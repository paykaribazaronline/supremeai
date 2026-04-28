package com.supremeai.ide.learning

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import java.util.HashMap
import java.util.Map

/**
 * Listens for document changes to learn from user code edits.
 * Implements the "Eye" component of the Android Studio plugin learning system.
 * Tracks when users modify AI-generated code and sends the diff to backend for learning.
 */
class UserCodeChangeListener(private val project: Project, private val aiSuggestionMarker: String = "// AI-SUGGESTED") : DocumentListener {

    // Store original code when file is opened to compare against later edits
    private val originalCodeMap = HashMap<VirtualFile, String>()
    private val isProcessingChange = HashMap<VirtualFile, Boolean>()

    override fun documentChanged(e: DocumentEvent?) {
        val document = e?.document
        if (document == null) return

        // Avoid processing our own programmatic changes
        if (isProcessingChange[document.getUserData(FileDocumentManager.KEY) ?: return] == true) {
            return
        }

        val file = FileDocumentManager.getInstance().getFile(document)
        if (file == null) return

        // Get current code
        val currentCode = document.getText()

        // Check if we have original code stored for comparison
        val originalCode = originalCodeMap[file]
        if (originalCode == null) {
            // First time seeing this file - store as original
            originalCodeMap[file] = currentCode
            return
        }

        // Check if there was actually a change
        if (originalCode == currentCode) {
            return // No change
        }

        // Determine if this looks like user-edited AI code
        val isLikelyAICode = isLikelyAIGeneratedCode(originalCode, currentCode)
        if (!isLikelyAICode) {
            // Not AI-related code change, skip learning
            return
        }

        // Mark that we're processing to avoid feedback loops
        isProcessingChange[file] = true

        try {
            // Calculate diff and send to backend for learning
            val diff = generateDiff(originalCode, currentCode)
            
            // Send to backend in a separate thread to avoid blocking UI
            ApplicationManager.getApplication().executeOnPooledThread({
                try {
                    SupremeAILearningClient.sendCodeEditToBrain(
                        project,
                        file.nameWithoutExtension,
                        originalCode,
                        currentCode,
                        diff,
                        file.path
                    )
                } finally {
                    isProcessingChange[file] = false
                }
            })

            // Update stored original code to current for next comparison
            originalCodeMap[file] = currentCode
        } catch (ex: Exception) {
            isProcessingChange[file] = false
            ex.printStackTrace()
        }
    }

    /**
     * Determines if the code change is likely related to AI-generated code.
     * Looks for AI suggestion markers or common patterns.
     */
    private fun isLikelyAIGeneratedCode(originalCode: String, currentCode: String): Boolean {
        // Check for AI suggestion markers in either version
        val hasAIMarker = originalCode.contains(aiSuggestionMarker, ignoreCase = true) ||
                         currentCode.contains(aiSuggestionMarker, ignoreCase = true)

        // Additional heuristics could be added here
        // For example, checking if the change is in a template/generated file
        
        return hasAIMarker
    }

    /**
     * Generates a simple diff description between two code versions.
     * In a production implementation, you might want to use a proper diff library.
     */
    private fun generateDiff(original: String, current: String): String {
        if (original.length == current.length && original == current) {
            return "No changes"
        }

        val originalLines = original.linesToSequence().toList()
        val currentLines = current.linesToSequence().toList()

        val maxLines = Math.max(originalLines.size, currentLines.size)
        val diffBuilder = StringBuilder()

        for (i in 0 until maxLines) {
            val origLine = if (i < originalLines.size) originalLines[i] else null
            val currLine = if (i < currentLines.size) currentLines[i] else null

            if (origLine == null && currLine != null) {
                diffBuilder.append("+ ").append(currLine as String).append("\n")
            } else if (origLine != null && currLine == null) {
                diffBuilder.append("- ").append(origLine as String).append("\n")
            } else if (origLine != null && currLine != null && origLine != currLine) {
                diffBuilder.append("- ").append(origLine as String).append("\n")
                diffBuilder.append("+ ").append(currLine as String).append("\n")
            }
            // If lines are equal, no diff needed
        }

        return diffBuilder.toString().trim()
    }

    /**
     * Call this when opening a file to reset tracking for that file.
     */
    fun resetTrackingForFile(file: VirtualFile) {
        originalCodeMap.remove(file)
        isProcessingChange.remove(file)
    }
}