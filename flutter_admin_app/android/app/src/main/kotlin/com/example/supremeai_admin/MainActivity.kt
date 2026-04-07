package com.example.supremeai_admin

import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import kotlinx.coroutines.*

class MainActivity : FlutterActivity() {
    private val CHANNEL = "com.supremeai.admin/on_device_llm"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // MediaPipe LLM Inference Task handle (loaded at runtime)
    private var llmInference: Any? = null

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "getDeviceInfo" -> {
                    val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                    val memInfo = ActivityManager.MemoryInfo()
                    activityManager.getMemoryInfo(memInfo)
                    val totalRam = memInfo.totalMem / (1024 * 1024)
                    val availRam = memInfo.availMem / (1024 * 1024)

                    result.success(mapOf(
                        "totalRamMb" to totalRam,
                        "availableRamMb" to availRam,
                        "gpuSupported" to (Build.VERSION.SDK_INT >= 26),
                        "sdkVersion" to Build.VERSION.SDK_INT,
                        "device" to Build.MODEL
                    ))
                }

                "isModelDownloaded" -> {
                    val modelId = call.argument<String>("modelId") ?: ""
                    val modelFile = File(filesDir, "models/$modelId.bin")
                    result.success(modelFile.exists())
                }

                "downloadModel" -> {
                    val modelId = call.argument<String>("modelId") ?: ""
                    val url = call.argument<String>("url") ?: ""
                    val modelDir = File(filesDir, "models")
                    modelDir.mkdirs()
                    val modelFile = File(modelDir, "$modelId.bin")

                    scope.launch {
                        try {
                            val connection = URL(url).openConnection()
                            val totalSize = connection.contentLengthLong
                            val input = connection.getInputStream()
                            val output = FileOutputStream(modelFile)
                            val buffer = ByteArray(8192)
                            var downloaded = 0L
                            var bytesRead: Int

                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                downloaded += bytesRead
                                if (totalSize > 0) {
                                    val progress = downloaded.toDouble() / totalSize
                                    withContext(Dispatchers.Main) {
                                        flutterEngine.dartExecutor.binaryMessenger.let { messenger ->
                                            MethodChannel(messenger, CHANNEL).invokeMethod("downloadProgress", progress)
                                        }
                                    }
                                }
                            }
                            output.close()
                            input.close()
                            withContext(Dispatchers.Main) { result.success(true) }
                        } catch (e: Exception) {
                            modelFile.delete()
                            withContext(Dispatchers.Main) { result.success(false) }
                        }
                    }
                }

                "deleteModel" -> {
                    val modelId = call.argument<String>("modelId") ?: ""
                    val modelFile = File(filesDir, "models/$modelId.bin")
                    val deleted = if (modelFile.exists()) modelFile.delete() else true
                    llmInference = null
                    result.success(deleted)
                }

                "getStorageInfo" -> {
                    val modelDir = File(filesDir, "models")
                    val models = mutableMapOf<String, Long>()
                    var totalBytes = 0L
                    if (modelDir.exists()) {
                        modelDir.listFiles()?.forEach { file ->
                            models[file.nameWithoutExtension] = file.length()
                            totalBytes += file.length()
                        }
                    }
                    result.success(mapOf("totalBytes" to totalBytes, "models" to models))
                }

                "loadModel" -> {
                    val modelId = call.argument<String>("modelId") ?: ""
                    val modelFile = File(filesDir, "models/$modelId.bin")
                    if (!modelFile.exists()) {
                        result.success(false)
                        return@setMethodCallHandler
                    }

                    scope.launch {
                        try {
                            // Use MediaPipe LLM Inference API via reflection
                            // This allows the app to work even if MediaPipe isn't available yet
                            val llmInferenceClass = Class.forName("com.google.mediapipe.tasks.genai.llminference.LlmInference")
                            val optionsClass = Class.forName("com.google.mediapipe.tasks.genai.llminference.LlmInference\$LlmInferenceOptions")
                            val builderClass = Class.forName("com.google.mediapipe.tasks.genai.llminference.LlmInference\$LlmInferenceOptions\$Builder")

                            val builder = builderClass.getConstructor().newInstance()
                            builderClass.getMethod("setModelPath", String::class.java).invoke(builder, modelFile.absolutePath)
                            builderClass.getMethod("setMaxTokens", Int::class.java).invoke(builder, 1024)

                            val options = builderClass.getMethod("build").invoke(builder)
                            val inference = llmInferenceClass.getMethod("createFromOptions", Context::class.java, optionsClass)
                                .invoke(null, this@MainActivity, options)

                            llmInference = inference
                            withContext(Dispatchers.Main) { result.success(true) }
                        } catch (e: ClassNotFoundException) {
                            // MediaPipe not available — fallback placeholder
                            llmInference = modelFile.absolutePath
                            withContext(Dispatchers.Main) { result.success(true) }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) { result.success(false) }
                        }
                    }
                }

                "unloadModel" -> {
                    try {
                        if (llmInference != null && llmInference !is String) {
                            llmInference!!::class.java.getMethod("close").invoke(llmInference)
                        }
                    } catch (_: Exception) {}
                    llmInference = null
                    result.success(null)
                }

                "generateResponse" -> {
                    val prompt = call.argument<String>("prompt") ?: ""
                    if (llmInference == null) {
                        result.error("NO_MODEL", "No model loaded", null)
                        return@setMethodCallHandler
                    }

                    scope.launch {
                        try {
                            if (llmInference is String) {
                                // Fallback mode — simulate token streaming with placeholder
                                val response = "[Offline Mode] Model loaded from: ${llmInference}\n\n" +
                                    "MediaPipe LLM runtime not found in this build. " +
                                    "Add 'com.google.mediapipe:tasks-genai' dependency to enable real inference.\n\n" +
                                    "Your prompt was: $prompt"
                                for (word in response.split(" ")) {
                                    withContext(Dispatchers.Main) {
                                        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
                                            .invokeMethod("onToken", "$word ")
                                    }
                                }
                            } else {
                                // Real MediaPipe inference
                                val generateMethod = llmInference!!::class.java.getMethod("generateResponse", String::class.java)
                                val response = generateMethod.invoke(llmInference, prompt) as String
                                // Stream word by word
                                for (word in response.split(" ")) {
                                    withContext(Dispatchers.Main) {
                                        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
                                            .invokeMethod("onToken", "$word ")
                                    }
                                }
                            }
                            withContext(Dispatchers.Main) {
                                MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
                                    .invokeMethod("onComplete", null)
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
                                    .invokeMethod("onError", e.message ?: "Unknown error")
                            }
                        }
                    }
                    result.success(null)
                }

                "generateResponseFull" -> {
                    val prompt = call.argument<String>("prompt") ?: ""
                    if (llmInference == null) {
                        result.error("NO_MODEL", "No model loaded", null)
                        return@setMethodCallHandler
                    }

                    scope.launch {
                        try {
                            val response = if (llmInference is String) {
                                "[Offline Mode] Prompt: $prompt"
                            } else {
                                val method = llmInference!!::class.java.getMethod("generateResponse", String::class.java)
                                method.invoke(llmInference, prompt) as String
                            }
                            withContext(Dispatchers.Main) { result.success(response) }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) { result.error("INFERENCE_ERROR", e.message, null) }
                        }
                    }
                }

                else -> result.notImplemented()
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
