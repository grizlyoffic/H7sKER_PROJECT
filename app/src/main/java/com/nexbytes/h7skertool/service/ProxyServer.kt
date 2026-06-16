package com.nexbytes.h7skertool.service

import android.util.Log
import com.nexbytes.h7skertool.model.CapturedRequest
import com.nexbytes.h7skertool.model.CapturedResponse
import com.nexbytes.h7skertool.utils.HexUtils
import com.nexbytes.h7skertool.utils.ProtoModifier
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class ProxyServer(
    private val clientBaseUrl: String,
    private val scope: CoroutineScope,
    private val savedMods: Map<String, String>,
    private val onCapture: (CapturedRequest, CapturedResponse) -> Unit,
    private val onLog: (String) -> Unit
) : NanoHTTPD("127.0.0.1", 8080) {

    private val TAG = "ProxyServer"

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .connectionPool(okhttp3.ConnectionPool(5, 30, TimeUnit.SECONDS))
        .retryOnConnectionFailure(true)
        .build()

    override fun serve(session: IHTTPSession): Response {
        val method = session.method.name
        val path = session.uri
        val endpoint = extractEndpoint(path)
        val start = System.currentTimeMillis()
        onLog("→ $method $endpoint")

        val reqHeaders = session.headers.toMutableMap()

        // Read body efficiently
        val bodyBytes: ByteArray? = try {
            val len = reqHeaders["content-length"]?.toLongOrNull() ?: 0L
            if (len > 0L && len < 10_000_000L) {
                ByteArray(len.toInt()).also { buf ->
                    var offset = 0
                    while (offset < buf.size) {
                        val read = session.inputStream.read(buf, offset, buf.size - offset)
                        if (read == -1) break
                        offset += read
                    }
                }
            } else null
        } catch (_: Exception) { null }

        // Apply request/header modifications
        val finalBody = applyRequestMod(endpoint, bodyBytes)
        val modHeaders = applyHeaderMod(endpoint, reqHeaders)

        val capturedReq = CapturedRequest(
            method = method,
            url = "$clientBaseUrl$path",
            endpoint = endpoint,
            headers = modHeaders,
            body = finalBody,
            bodyText = finalBody?.let { runCatching { String(it, Charsets.UTF_8) }.getOrNull() },
            bodyHex = HexUtils.toHexDump(finalBody)
        )

        return try {
            val realResp = forwardRequest(method, path, modHeaders, finalBody)
            val duration = System.currentTimeMillis() - start
            val result = applyResponseMod(endpoint, realResp)

            val capturedRes = CapturedResponse(
                requestId = capturedReq.id,
                statusCode = realResp.code,
                statusMessage = realResp.message,
                endpoint = endpoint,
                headers = result.headers,
                body = result.bytes,
                bodyText = result.text,
                bodyHex = result.hex,
                durationMs = duration
            )

            onLog("← ${realResp.code} $endpoint (${duration}ms)")
            scope.launch { onCapture(capturedReq, capturedRes) }

            val mime = result.headers["content-type"] ?: "application/octet-stream"
            val response = newFixedLengthResponse(
                Response.Status.lookup(realResp.code), mime,
                result.bytes?.inputStream(), (result.bytes?.size ?: 0).toLong()
            )
            result.headers.forEach { (k, v) ->
                if (!k.equals("content-length", true) && !k.equals("transfer-encoding", true)) response.addHeader(k, v)
            }
            realResp.close()
            response
        } catch (e: IOException) {
            onLog("✗ Error: $endpoint — ${e.message}")
            val errRes = CapturedResponse(requestId = capturedReq.id, statusCode = 503, statusMessage = "Proxy Error", endpoint = endpoint, headers = emptyMap(), body = null, bodyText = e.message, bodyHex = null, durationMs = -1)
            scope.launch { onCapture(capturedReq, errRes) }
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Proxy error: ${e.message}")
        }
    }

    private fun forwardRequest(method: String, path: String, headers: Map<String, String>, body: ByteArray?): okhttp3.Response {
        val url = "$clientBaseUrl$path"
        val ct = headers["content-type"]?.toMediaTypeOrNull()
        val reqBody = when {
            body != null && method !in listOf("GET", "HEAD") -> body.toRequestBody(ct)
            method !in listOf("GET", "HEAD") -> ByteArray(0).toRequestBody(ct)
            else -> null
        }
        val builder = Request.Builder().url(url)
        val host = clientBaseUrl.removePrefix("https://").removePrefix("http://").split("/").first()
        headers.forEach { (k, v) ->
            if (k.lowercase() !in listOf("host", "connection", "transfer-encoding", "content-length", "keep-alive", "proxy-connection"))
                runCatching { builder.addHeader(k, v) }
        }
        builder.header("Host", host)
        return http.newCall(builder.method(method, reqBody).build()).execute()
    }

    private fun applyRequestMod(endpoint: String, body: ByteArray?): ByteArray? {
        val mod = savedMods[endpoint] ?: return body
        if (body == null) return null
        return try {
            val fields = ProtoModifier.parseModFields(mod)
            if (fields.isNotEmpty()) ProtoModifier.modifyProtoBytes(body, fields)
            else mod.toByteArray(Charsets.UTF_8)
        } catch (_: Exception) { runCatching { mod.toByteArray(Charsets.UTF_8) }.getOrDefault(body) }
    }

    private fun applyHeaderMod(endpoint: String, headers: MutableMap<String, String>): MutableMap<String, String> {
        val headerMod = savedMods["${endpoint}_headers"]
        if (headerMod.isNullOrEmpty()) return headers
        val modified = headers.toMutableMap()
        try {
            headerMod.lines().forEach { line ->
                val trimmed = line.trim(); if (trimmed.isEmpty()) return@forEach
                val parts = trimmed.split(":", limit = 2); if (parts.size == 2) {
                    val k = parts[0].trim(); val v = parts[1].trim()
                    if (v.isEmpty() || v.equals("null", ignoreCase = true)) modified.remove(k)
                    else { modified[k] = v; onLog("  ✏️ Header: $k: $v") }
                }
            }
        } catch (e: Exception) { onLog("  ⚠️ Header mod error: ${e.message}") }
        return modified
    }

    private fun applyResponseMod(endpoint: String, response: okhttp3.Response): ResponseModResult {
        val mod = savedMods["${endpoint}_response"]
        val originalBytes = try { response.body?.bytes() } catch (_: Exception) { null }
        val headers = mutableMapOf<String, String>()
        response.headers.forEach { (k, v) -> headers[k] = v }
        if (originalBytes == null) return ResponseModResult(null, null, null, headers)
        if (mod.isNullOrEmpty()) return ResponseModResult(originalBytes, runCatching { String(originalBytes, Charsets.UTF_8) }.getOrNull(), HexUtils.toHexDump(originalBytes), headers)
        val modifiedBytes = try {
            val fields = ProtoModifier.parseModFields(mod)
            if (fields.isNotEmpty()) ProtoModifier.modifyProtoBytes(originalBytes, fields) else mod.toByteArray(Charsets.UTF_8)
        } catch (_: Exception) { runCatching { mod.toByteArray(Charsets.UTF_8) }.getOrDefault(originalBytes) }
        headers["content-length"] = modifiedBytes.size.toString()
        onLog("  ✏️ Response modified for $endpoint (${originalBytes.size} → ${modifiedBytes.size} bytes)")
        return ResponseModResult(modifiedBytes, runCatching { String(modifiedBytes, Charsets.UTF_8) }.getOrNull(), HexUtils.toHexDump(modifiedBytes), headers)
    }

    private fun extractEndpoint(path: String): String {
        val clean = path.split("?").first().trimStart('/')
        val first = clean.split("/").firstOrNull { it.isNotEmpty() } ?: return path
        return "/$first"
    }

    data class ResponseModResult(val bytes: ByteArray?, val text: String?, val hex: String?, val headers: MutableMap<String, String>)
}
