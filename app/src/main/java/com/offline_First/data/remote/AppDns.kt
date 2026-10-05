package com.offline_First.data.remote

import android.util.Log
import okhttp3.Dns
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URL
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap

/**
 * Intelligent Multi-Tier DNS Resolver for EduNova.
 *
 * Resolves hostnames through 4 tiers:
 * 1. System DNS (Android standard resolver)
 * 2. Direct UDP DNS to Google (8.8.8.8) and Cloudflare (1.1.1.1) on port 53.
 *    (Bypasses broken 10.0.2.3 emulator DNS on Windows)
 * 3. DNS-over-HTTPS (DoH) by direct IP (https://1.1.1.1/dns-query)
 * 4. Known bootstrap IPs for critical production domains (Render & Hugging Face)
 */
object AppDns : Dns {

    private const val TAG = "AppDns"
    private const val DNS_TIMEOUT_MS = 3500

    private val PUBLIC_DNS_SERVERS = listOf("8.8.8.8", "1.1.1.1", "8.8.4.4", "1.0.0.1")

    // In-memory cache with TTL (5 minutes)
    private val cache = ConcurrentHashMap<String, Pair<Long, List<InetAddress>>>()
    private const val CACHE_TTL_MS = 5 * 60 * 1000L

    // Bootstrap Anycast IPs for Render backend and Hugging Face infrastructure
    private val BOOTSTRAP_IPS = mapOf(
        "edunova-backend-9waj.onrender.com" to listOf(
            "216.24.57.16",
            "216.24.57.18"
        ),
        "huggingface.co" to listOf(
            "13.225.5.30",
            "13.225.5.26",
            "13.225.5.100",
            "13.225.5.95"
        )
    )

    override fun lookup(hostname: String): List<InetAddress> {
        // 0. Check cache
        val cached = cache[hostname]
        if (cached != null && System.currentTimeMillis() - cached.first < CACHE_TTL_MS) {
            return cached.second
        }

        // 1. Try system DNS first
        try {
            val systemResult = Dns.SYSTEM.lookup(hostname)
            if (systemResult.isNotEmpty()) {
                cache[hostname] = Pair(System.currentTimeMillis(), systemResult)
                return systemResult
            }
        } catch (e: Exception) {
            Log.w(TAG, "System DNS lookup failed for '$hostname': ${e.message}. Trying public DNS fallback...")
        }

        // 2. Try direct UDP DNS query to 8.8.8.8 / 1.1.1.1
        for (dnsServer in PUBLIC_DNS_SERVERS) {
            try {
                val udpResult = queryDnsUdp(hostname, dnsServer)
                if (udpResult.isNotEmpty()) {
                    Log.i(TAG, "Resolved '$hostname' via UDP DNS ($dnsServer): $udpResult")
                    cache[hostname] = Pair(System.currentTimeMillis(), udpResult)
                    return udpResult
                }
            } catch (e: Exception) {
                Log.d(TAG, "UDP DNS query to $dnsServer for '$hostname' failed: ${e.message}")
            }
        }

        // 3. Try DNS-over-HTTPS by raw IP to Cloudflare / Google (requires no DNS resolution)
        try {
            val dohResult = queryDnsOverHttps(hostname)
            if (dohResult.isNotEmpty()) {
                Log.i(TAG, "Resolved '$hostname' via DoH: $dohResult")
                cache[hostname] = Pair(System.currentTimeMillis(), dohResult)
                return dohResult
            }
        } catch (e: Exception) {
            Log.w(TAG, "DoH resolution for '$hostname' failed: ${e.message}")
        }

        // 4. Check bootstrap addresses
        val bootstrap = BOOTSTRAP_IPS[hostname]
        if (!bootstrap.isNullOrEmpty()) {
            val bootstrapAddresses = bootstrap.mapNotNull { ip ->
                runCatching { InetAddress.getByAddress(hostname, ipToBytes(ip)) }.getOrNull()
            }
            if (bootstrapAddresses.isNotEmpty()) {
                Log.w(TAG, "Using bootstrap addresses for '$hostname': $bootstrapAddresses")
                cache[hostname] = Pair(System.currentTimeMillis(), bootstrapAddresses)
                return bootstrapAddresses
            }
        }

        throw UnknownHostException("Unable to resolve host '$hostname' via system DNS, public DNS, or bootstrap cache.")
    }

    /**
     * Resolves IPv4 A-records for a hostname via raw UDP query to a DNS server IP.
     */
    private fun queryDnsUdp(hostname: String, dnsServerIp: String): List<InetAddress> {
        val queryPacket = buildDnsQuery(hostname)
        DatagramSocket().use { socket ->
            socket.soTimeout = DNS_TIMEOUT_MS
            val dest = InetSocketAddress(dnsServerIp, 53)
            val sendPacket = DatagramPacket(queryPacket, queryPacket.size, dest)
            socket.send(sendPacket)

            val buffer = ByteArray(512)
            val receivePacket = DatagramPacket(buffer, buffer.size)
            socket.receive(receivePacket)

            return parseDnsResponse(buffer, receivePacket.length, hostname)
        }
    }

    private fun buildDnsQuery(hostname: String): ByteArray {
        val baos = ByteArrayOutputStream()
        // Header
        baos.write(byteArrayOf(0x12, 0x34)) // Transaction ID
        baos.write(byteArrayOf(0x01, 0x00)) // Flags: Standard query, recursion desired
        baos.write(byteArrayOf(0x00, 0x01)) // QDCOUNT = 1
        baos.write(byteArrayOf(0x00, 0x00)) // ANCOUNT = 0
        baos.write(byteArrayOf(0x00, 0x00)) // NSCOUNT = 0
        baos.write(byteArrayOf(0x00, 0x00)) // ARCOUNT = 0

        // Question: Labels
        for (part in hostname.split('.')) {
            val bytes = part.toByteArray(Charsets.US_ASCII)
            baos.write(bytes.size)
            baos.write(bytes)
        }
        baos.write(0) // End of name

        baos.write(byteArrayOf(0x00, 0x01)) // Type = A (1)
        baos.write(byteArrayOf(0x00, 0x01)) // Class = IN (1)
        return baos.toByteArray()
    }

    private fun parseDnsResponse(data: ByteArray, length: Int, hostname: String): List<InetAddress> {
        if (length < 12) return emptyList()
        val ancount = ((data[6].toInt() and 0xFF) shl 8) or (data[7].toInt() and 0xFF)
        if (ancount == 0) return emptyList()

        var offset = 12
        // Skip Question section
        while (offset < length && data[offset].toInt() != 0) {
            val len = data[offset].toInt() and 0xFF
            if ((len and 0xC0) == 0xC0) {
                offset += 2
                break
            } else {
                offset += 1 + len
            }
        }
        if (offset < length && data[offset].toInt() == 0) offset++ // null byte
        offset += 4 // Skip Type & Class

        val addresses = mutableListOf<InetAddress>()
        for (i in 0 until ancount) {
            if (offset >= length) break
            // Read Name (handle compression pointer)
            if ((data[offset].toInt() and 0xC0) == 0xC0) {
                offset += 2
            } else {
                while (offset < length && data[offset].toInt() != 0) {
                    val len = data[offset].toInt() and 0xFF
                    offset += 1 + len
                }
                if (offset < length && data[offset].toInt() == 0) offset++
            }
            if (offset + 10 > length) break
            val type = ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)
            val rdlength = ((data[offset + 8].toInt() and 0xFF) shl 8) or (data[offset + 9].toInt() and 0xFF)
            offset += 10
            if (type == 1 && rdlength == 4 && offset + 4 <= length) {
                val ipBytes = data.copyOfRange(offset, offset + 4)
                runCatching {
                    addresses.add(InetAddress.getByAddress(hostname, ipBytes))
                }
            }
            offset += rdlength
        }
        return addresses
    }

    /**
     * Resolves IPv4 A-records via Cloudflare DoH (https://1.1.1.1/dns-query) using direct IP.
     */
    private fun queryDnsOverHttps(hostname: String): List<InetAddress> {
        val url = URL("https://1.1.1.1/dns-query?name=$hostname&type=A")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = DNS_TIMEOUT_MS
            readTimeout = DNS_TIMEOUT_MS
            requestMethod = "GET"
            setRequestProperty("Accept", "application/dns-json")
            setRequestProperty("User-Agent", "EduNova-Android")
        }
        try {
            if (conn.responseCode !in 200..299) return emptyList()
            val text = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }
            val json = JSONObject(text)
            val answers = json.optJSONArray("Answer") ?: return emptyList()
            val addresses = mutableListOf<InetAddress>()
            for (i in 0 until answers.length()) {
                val obj = answers.getJSONObject(i)
                if (obj.optInt("type") == 1) { // Type A
                    val ip = obj.optString("data")
                    if (ip.isNotEmpty() && !ip.contains(':')) {
                        runCatching {
                            addresses.add(InetAddress.getByAddress(hostname, ipToBytes(ip)))
                        }
                    }
                }
            }
            return addresses
        } finally {
            conn.disconnect()
        }
    }

    private fun ipToBytes(ip: String): ByteArray {
        val parts = ip.split('.')
        require(parts.size == 4) { "Invalid IPv4: $ip" }
        return ByteArray(4) { idx -> parts[idx].toInt().toByte() }
    }
}
