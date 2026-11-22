package com.example.ads

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.File
import java.net.InetAddress

data class AdBlockerCheckResult(
    val isBlocked: Boolean,
    val isSimulated: Boolean = false,
    val hasInternet: Boolean = true,
    val privateDnsBlocked: Boolean = false,
    val privateDnsHost: String? = null,
    val vpnActive: Boolean = false,
    val dnsSinkholeDetected: Boolean = false,
    val hostsFileModified: Boolean = false,
    val probeDetails: List<String> = emptyList(),
    val summaryReason: String = "Ad network clear"
)

object AdBlockerDetector {

    private const val CACHE_TTL_MILLIS = 30_000L

    @Volatile
    private var cachedResult: AdBlockerCheckResult? = null

    @Volatile
    private var cachedAtMillis: Long = 0L

    private val knownBlockingDnsKeywords = listOf(
        "adguard",
        "nextdns",
        "controld",
        "mullvad",
        "pihole",
        "pi-hole",
        "rethinkdns",
        "cleanbrowsing",
        "ahadns",
        "blahdns",
        "dns.sb",
        "libredns",
        "adblock"
    )

    private val adProbeDomains = listOf(
        "pagead2.googlesyndication.com",
        "googleads.g.doubleclick.net",
        "adservice.google.com"
    )

    fun invalidateCache() {
        cachedResult = null
        cachedAtMillis = 0L
    }

    suspend fun detect(
        context: Context,
        simulateAdBlocker: Boolean = false,
        forceRefresh: Boolean = false
    ): AdBlockerCheckResult = withContext(Dispatchers.IO) {
        if (simulateAdBlocker) {
            return@withContext AdBlockerCheckResult(
                isBlocked = true,
                isSimulated = true,
                hasInternet = true,
                dnsSinkholeDetected = true,
                probeDetails = listOf(
                    "Simulated Test Mode: Ad blocker shield triggered",
                    "Probe pagead2.googlesyndication.com -> 0.0.0.0 (simulated)"
                ),
                summaryReason = "Simulated ad blocker active (Test Mode). Turn off simulation in Ads Lab to resume."
            )
        }

        val now = System.currentTimeMillis()
        val existingCache = cachedResult
        if (!forceRefresh && existingCache != null && (now - cachedAtMillis) < CACHE_TTL_MILLIS) {
            return@withContext existingCache
        }

        val details = mutableListOf<String>()
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = cm?.activeNetwork
        val capabilities = activeNetwork?.let { cm.getNetworkCapabilities(it) }
        val hasInternet = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        val vpnActive = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true

        // 1. Check Android Private DNS (API 28+)
        var privateDnsBlocked = false
        var privateDnsHost: String? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && activeNetwork != null) {
            val linkProps = cm.getLinkProperties(activeNetwork)
            privateDnsHost = linkProps?.privateDnsServerName
            if (!privateDnsHost.isNullOrBlank()) {
                val lowered = privateDnsHost.lowercase()
                if (knownBlockingDnsKeywords.any { lowered.contains(it) }) {
                    privateDnsBlocked = true
                    details.add("Private DNS matched ad-filter provider: $privateDnsHost")
                } else {
                    details.add("Private DNS configured: $privateDnsHost (allowed)")
                }
            } else {
                details.add("Private DNS: Automatic / Off")
            }
        }

        // 2. Check /system/etc/hosts for sinkholed ad domains
        var hostsFileModified = false
        try {
            val hostsFile = File("/system/etc/hosts")
            if (hostsFile.exists() && hostsFile.canRead()) {
                val adHostHits = hostsFile.bufferedReader().useLines { lines ->
                    lines.take(500).count { line ->
                        val trimmed = line.trim()
                        !trimmed.startsWith("#") &&
                            (trimmed.contains("googlesyndication.com") ||
                                trimmed.contains("doubleclick.net") ||
                                trimmed.contains("googleadservices.com") ||
                                trimmed.contains("adservice.google.com"))
                    }
                }
                if (adHostHits > 0) {
                    hostsFileModified = true
                    details.add("/system/etc/hosts contains $adHostHits ad-blocking entries")
                } else {
                    details.add("/system/etc/hosts: Clean")
                }
            }
        } catch (_: Exception) {
            details.add("/system/etc/hosts: Protected")
        }

        // 3. Parallel Live DNS Sinkhole Probe
        var dnsSinkholeDetected = false
        if (hasInternet) {
            val probeResults = coroutineScope {
                val neutralDeferred = async {
                    runCatching {
                        InetAddress.getAllByName("connectivitycheck.gstatic.com").isNotEmpty()
                    }.getOrDefault(false)
                }
                val domainDeferreds = adProbeDomains.map { domain ->
                    async {
                        domain to runCatching { InetAddress.getAllByName(domain) }
                    }
                }
                val neutralResolves = neutralDeferred.await()
                val resolvedDomains = domainDeferreds.awaitAll()
                neutralResolves to resolvedDomains
            }

            val neutralResolves = probeResults.first
            var blockedHostCount = 0
            for ((domain, result) in probeResults.second) {
                val addresses = result.getOrNull()
                if (addresses != null) {
                    val sinkholed = addresses.any { addr ->
                        val host = addr.hostAddress ?: ""
                        addr.isAnyLocalAddress ||
                            addr.isLoopbackAddress ||
                            host == "0.0.0.0" ||
                            host == "127.0.0.1" ||
                            host == "::" ||
                            host == "::1"
                    }
                    if (sinkholed) {
                        blockedHostCount++
                        details.add("DNS sinkhole on $domain -> ${addresses.firstOrNull()?.hostAddress}")
                    } else {
                        details.add("DNS OK: $domain -> ${addresses.firstOrNull()?.hostAddress}")
                    }
                } else {
                    if (neutralResolves) {
                        blockedHostCount++
                        details.add("DNS blocked for $domain while internet is active")
                    } else {
                        details.add("DNS skipped ($domain): offline or sandbox DNS")
                    }
                }
            }
            if (blockedHostCount > 0) {
                dnsSinkholeDetected = true
            }
        } else {
            details.add("Network offline or limited")
        }

        if (vpnActive) {
            details.add(
                if (dnsSinkholeDetected) "VPN active + ad domains blocked"
                else "VPN transport active (no ad domain blocking detected)"
            )
        }

        val isBlocked = privateDnsBlocked || hostsFileModified || dnsSinkholeDetected
        val summary = when {
            privateDnsBlocked -> "Ad-blocking Private DNS detected ($privateDnsHost)."
            hostsFileModified -> "Local hosts file ad blocker detected."
            dnsSinkholeDetected -> "Ad network domains are being blocked or sinkholed by a DNS/VPN ad blocker."
            else -> "Verified clean connection. Google AdMob ads are ready."
        }

        val computed = AdBlockerCheckResult(
            isBlocked = isBlocked,
            isSimulated = false,
            hasInternet = hasInternet,
            privateDnsBlocked = privateDnsBlocked,
            privateDnsHost = privateDnsHost,
            vpnActive = vpnActive,
            dnsSinkholeDetected = dnsSinkholeDetected,
            hostsFileModified = hostsFileModified,
            probeDetails = details,
            summaryReason = summary
        )
        cachedResult = computed
        cachedAtMillis = System.currentTimeMillis()
        computed
    }
}
