package com.supremeai.ide.metrics

import com.google.gson.Gson
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.messaging.converter.StringMessageConverter
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.messaging.simp.stomp.StompSession
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.messaging.WebSocketStompClient
import org.springframework.web.socket.sockjs.client.SockJsClient
import org.springframework.web.socket.sockjs.client.Transport
import org.springframework.web.socket.sockjs.client.WebSocketTransport
import java.lang.reflect.Type

@Service(Service.Level.PROJECT)
class SupremeAIMetricsService(private val scope: CoroutineScope) {
    private val log = logger<SupremeAIMetricsService>()
    private var stompSession: StompSession? = null
    private val gson = Gson()

    init {
        connect()
    }

    private fun connect() {
        scope.launch(Dispatchers.IO) {
            try {
                val client = StandardWebSocketClient()
                val transports = listOf<Transport>(WebSocketTransport(client))
                val sockJsClient = SockJsClient(transports)
                val stompClient = WebSocketStompClient(sockJsClient)
                stompClient.messageConverter = StringMessageConverter()

                val settings = SupremeAISettings.getInstance()
                val baseUrl = settings.apiEndpoint.takeIf { it.isNotBlank() } ?: "https://supremeai-a.web.app"
                val url = baseUrl.replace("https://", "wss://").replace("http://", "ws://") + "/ws-supreme"
                val handler = object : StompSessionHandlerAdapter() {
                    override fun afterConnected(session: StompSession, connectedHeaders: StompHeaders) {
                        log.info("Connected to SupremeAI Dashboard")
                        stompSession = session
                    }

                    override fun handleException(session: StompSession, command: org.springframework.messaging.simp.stomp.StompCommand?, headers: StompHeaders, payload: ByteArray, exception: Throwable) {
                        log.error("STOMP Error: ${exception.message}", exception)
                    }
                }

                stompClient.connectAsync(url, handler)
            } catch (e: Exception) {
                log.error("Failed to connect to metrics dashboard: ${e.message}")
            }
        }
    }

    fun sendMetrics(data: Any) {
        val session = stompSession
        if (session != null && session.isConnected) {
            scope.launch(Dispatchers.IO) {
                try {
                    val json = gson.toJson(data)
                    session.send("/app/metrics", json)
                } catch (e: Exception) {
                    log.error("Error sending metrics: ${e.message}")
                }
            }
        }
    }

    companion object {
        fun getInstance(project: com.intellij.openapi.project.Project) = project.service<SupremeAIMetricsService>()
    }
}
