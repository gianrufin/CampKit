package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class SemVer(
    val major: Int,
    val minor: Int,
    val patch: Int
) : Comparable<SemVer> {
    override fun compareTo(other: SemVer): Int {
        if (major != other.major) return major.compareTo(other.major)
        if (minor != other.minor) return minor.compareTo(other.minor)
        return patch.compareTo(other.patch)
    }

    override fun toString(): String = "$major.$minor.$patch"

    companion object {
        private val SEMVER_REGEX = Regex("""(\d+)\.(\d+)\.(\d+)""")

        fun parse(raw: String?): SemVer? {
            if (raw.isNullOrBlank()) return null
            val match = SEMVER_REGEX.find(raw.trim()) ?: return null
            val (maj, min, pat) = match.destructured
            return SemVer(
                major = maj.toIntOrNull() ?: return null,
                minor = min.toIntOrNull() ?: return null,
                patch = pat.toIntOrNull() ?: return null
            )
        }
    }
}

data class ReleaseInfo(
    val version: String,
    val tag: String,
    val notes: String,
    val downloadUrl: String,
    val releasePageUrl: String
)

sealed interface UpdateCheckResult {
    data class UpToDate(val currentVersion: String, val latestVersion: String) : UpdateCheckResult
    data class UpdateAvailable(val currentVersion: String, val release: ReleaseInfo) : UpdateCheckResult
    data class Error(val message: String) : UpdateCheckResult
}

class UpdateChecker(
    private val repoOwner: String = "gianrufin",
    private val repoName: String = "CampKit"
) {
    suspend fun checkForUpdate(currentVersionName: String): UpdateCheckResult = withContext(Dispatchers.IO) {
        val currentSemVer = SemVer.parse(currentVersionName) ?: SemVer(2, 0, 1)

        // 1. Check GitHub Releases API (/releases/latest)
        val releaseResult = fetchLatestGitHubRelease()
        if (releaseResult != null) {
            val remoteSemVer = SemVer.parse(releaseResult.version)
            if (remoteSemVer != null && remoteSemVer > currentSemVer) {
                return@withContext UpdateCheckResult.UpdateAvailable(currentSemVer.toString(), releaseResult)
            }
            return@withContext UpdateCheckResult.UpToDate(
                currentVersion = currentSemVer.toString(),
                latestVersion = remoteSemVer?.toString() ?: releaseResult.version
            )
        }

        // 2. Fallback to repo version.json on main branch
        val manifestResult = fetchVersionJson()
        if (manifestResult != null) {
            val remoteSemVer = SemVer.parse(manifestResult.version)
            if (remoteSemVer != null && remoteSemVer > currentSemVer) {
                return@withContext UpdateCheckResult.UpdateAvailable(currentSemVer.toString(), manifestResult)
            }
            return@withContext UpdateCheckResult.UpToDate(
                currentVersion = currentSemVer.toString(),
                latestVersion = remoteSemVer?.toString() ?: manifestResult.version
            )
        }

        // 3. Fallback to GitHub Tags API
        val tagResult = fetchLatestGitHubTag()
        if (tagResult != null) {
            val remoteSemVer = SemVer.parse(tagResult.version)
            if (remoteSemVer != null && remoteSemVer > currentSemVer) {
                return@withContext UpdateCheckResult.UpdateAvailable(currentSemVer.toString(), tagResult)
            }
            return@withContext UpdateCheckResult.UpToDate(
                currentVersion = currentSemVer.toString(),
                latestVersion = remoteSemVer?.toString() ?: tagResult.version
            )
        }

        UpdateCheckResult.Error("Unable to reach GitHub repository ($repoOwner/$repoName). Check your connection and try again.")
    }

    private fun fetchLatestGitHubRelease(): ReleaseInfo? {
        val body = httpGet("https://api.github.com/repos/$repoOwner/$repoName/releases/latest") ?: return null
        return try {
            val json = JSONObject(body)
            val tag = json.optString("tag_name").takeIf { it.isNotBlank() } ?: return null
            val htmlUrl = json.optString("html_url").ifBlank {
                "https://github.com/$repoOwner/$repoName/releases/tag/$tag"
            }
            val notes = json.optString("body").ifBlank { "New CampKit release $tag is available." }
            val assets = json.optJSONArray("assets")
            var apkUrl: String? = null
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.optJSONObject(i) ?: continue
                    val name = asset.optString("name")
                    val url = asset.optString("browser_download_url")
                    if (name.endsWith(".apk", ignoreCase = true) && url.isNotBlank()) {
                        apkUrl = url
                        break
                    }
                }
            }
            val version = SemVer.parse(tag)?.toString() ?: tag.removePrefix("v")
            ReleaseInfo(
                version = version,
                tag = tag,
                notes = notes,
                downloadUrl = apkUrl ?: htmlUrl,
                releasePageUrl = htmlUrl
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchVersionJson(): ReleaseInfo? {
        val body = httpGet("https://raw.githubusercontent.com/$repoOwner/$repoName/main/version.json") ?: return null
        return try {
            val json = JSONObject(body)
            val versionRaw = json.optString("version").takeIf { it.isNotBlank() } ?: return null
            val tag = json.optString("tag").ifBlank { "v$versionRaw" }
            val downloadUrl = json.optString("downloadUrl").ifBlank {
                "https://github.com/$repoOwner/$repoName/releases/tag/$tag"
            }
            val releaseUrl = json.optString("releaseUrl").ifBlank { downloadUrl }
            val notes = json.optString("notes").ifBlank { "New CampKit release $tag is available." }
            ReleaseInfo(
                version = SemVer.parse(versionRaw)?.toString() ?: versionRaw,
                tag = tag,
                notes = notes,
                downloadUrl = downloadUrl,
                releasePageUrl = releaseUrl
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchLatestGitHubTag(): ReleaseInfo? {
        val body = httpGet("https://api.github.com/repos/$repoOwner/$repoName/tags") ?: return null
        return try {
            val array = JSONArray(body)
            var bestTag: String? = null
            var bestVer: SemVer? = null
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val name = obj.optString("name")
                val ver = SemVer.parse(name) ?: continue
                if (bestVer == null || ver > bestVer) {
                    bestVer = ver
                    bestTag = name
                }
            }
            if (bestTag != null && bestVer != null) {
                val releaseUrl = "https://github.com/$repoOwner/$repoName/releases/tag/$bestTag"
                ReleaseInfo(
                    version = bestVer.toString(),
                    tag = bestTag,
                    notes = "Release $bestTag found in $repoOwner/$repoName.",
                    downloadUrl = "https://github.com/$repoOwner/$repoName/raw/main/releases/CampKit-v${bestVer}.apk",
                    releasePageUrl = releaseUrl
                )
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun httpGet(urlString: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "CampKit-Android")
            }
            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }
}
