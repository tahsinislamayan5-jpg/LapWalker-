package com.takwa.lapwalker.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.takwa.lapwalker.BuildConfig
import com.takwa.lapwalker.domain.model.AppUpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class UpdateRepository(private val context: Context) {

    companion object {
        private const val GITHUB_REPO_OWNER = "tahsinislamayan5-jpg"
        private const val GITHUB_REPO_NAME = "LapWalker-"
        private const val API_URL = "https://api.github.com/repos/$GITHUB_REPO_OWNER/$GITHUB_REPO_NAME/releases/latest"
        private const val UPDATE_FILE_NAME = "LapWalker_latest.apk"
    }

    suspend fun checkForUpdate(): Result<AppUpdateInfo?> = withContext(Dispatchers.IO) {
        runCatching {
            try {
                val url = URL(API_URL)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/vnd.github.v3+json")
                    setRequestProperty("User-Agent", "LapWalker-Android-App")
                    connectTimeout = 8000
                    readTimeout = 8000
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    return@runCatching null
                }

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val jsonText = connection.inputStream.bufferedReader().use { it.readText() }
                    val releaseJson = JSONObject(jsonText)

                    val rawTagName = releaseJson.optString("tag_name", "")
                    val cleanRemoteVersion = rawTagName.trimStart('v', 'V').trim()
                    val releaseTitle = releaseJson.optString("name", "LapWalker $cleanRemoteVersion")
                    val changelog = releaseJson.optString("body", "Bug fixes and performance improvements.")

                    var downloadUrl = ""
                    var fileSizeBytes = 0L

                    val assetsArray = releaseJson.optJSONArray("assets")
                    if (assetsArray != null) {
                        for (i in 0 until assetsArray.length()) {
                            val asset = assetsArray.getJSONObject(i)
                            val assetName = asset.optString("name", "")
                            if (assetName.endsWith(".apk", ignoreCase = true)) {
                                downloadUrl = asset.optString("browser_download_url", "")
                                fileSizeBytes = asset.optLong("size", 0L)
                                break
                            }
                        }
                    }

                    if (downloadUrl.isEmpty()) {
                        downloadUrl = "https://github.com/$GITHUB_REPO_OWNER/$GITHUB_REPO_NAME/releases/download/$rawTagName/LapWalker.apk"
                    }

                    val currentVersion = BuildConfig.VERSION_NAME
                    val isNewer = isRemoteVersionNewer(cleanRemoteVersion, currentVersion)

                    return@runCatching AppUpdateInfo(
                        versionName = cleanRemoteVersion.ifEmpty { rawTagName },
                        releaseTitle = releaseTitle,
                        changelog = changelog,
                        downloadUrl = downloadUrl,
                        fileSizeBytes = fileSizeBytes,
                        isUpdateAvailable = isNewer
                    )
                }
            } catch (_: Exception) {
                // Fallback to web redirect if API fails or is rate-limited (HTTP 403)
            }

            // Fallback: Bypass GitHub API rate-limits via public redirect
            checkViaWebRedirect()
        }
    }

    private fun checkViaWebRedirect(): AppUpdateInfo? {
        val webUrl = "https://github.com/$GITHUB_REPO_OWNER/$GITHUB_REPO_NAME/releases/latest"
        val connection = (URL(webUrl).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = false
            requestMethod = "GET"
            setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile)")
            connectTimeout = 10000
            readTimeout = 10000
        }
        val responseCode = connection.responseCode
        if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
            responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
            responseCode == 307 || responseCode == 308
        ) {
            val location = connection.getHeaderField("Location") ?: return null
            val rawTag = location.substringAfterLast("/")
            val cleanRemoteVersion = rawTag.trimStart('v', 'V').trim()
            val currentVersion = BuildConfig.VERSION_NAME
            val isNewer = isRemoteVersionNewer(cleanRemoteVersion, currentVersion)
            val downloadUrl = "https://github.com/$GITHUB_REPO_OWNER/$GITHUB_REPO_NAME/releases/download/$rawTag/LapWalker.apk"

            return AppUpdateInfo(
                versionName = cleanRemoteVersion.ifEmpty { rawTag },
                releaseTitle = "LapWalker $cleanRemoteVersion",
                changelog = "• A new version ($cleanRemoteVersion) is available on GitHub!\n• Tap 'Update Now' below to download and install automatically.",
                downloadUrl = downloadUrl,
                fileSizeBytes = 0L,
                isUpdateAvailable = isNewer
            )
        }
        return null
    }

    suspend fun downloadApk(
        downloadUrl: String,
        onProgress: (percent: Int, downloaded: Long, total: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val destinationFile = File(context.cacheDir, UPDATE_FILE_NAME)
            if (destinationFile.exists()) {
                destinationFile.delete()
            }

            var currentConnection = (URL(downloadUrl).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                connectTimeout = 15000
                readTimeout = 30000
            }

            var redirects = 0
            while (redirects < 5) {
                val code = currentConnection.responseCode
                if (code == HttpURLConnection.HTTP_MOVED_PERM ||
                    code == HttpURLConnection.HTTP_MOVED_TEMP ||
                    code == 307 || code == 308
                ) {
                    val redirectUrl = currentConnection.getHeaderField("Location") ?: break
                    currentConnection.disconnect()
                    currentConnection = (URL(redirectUrl).openConnection() as HttpURLConnection).apply {
                        setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                        connectTimeout = 15000
                        readTimeout = 30000
                    }
                    redirects++
                } else {
                    break
                }
            }

            val totalLength = currentConnection.contentLengthLong
            var downloadedBytes = 0L

            currentConnection.inputStream.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        if (totalLength > 0) {
                            val percent = ((downloadedBytes * 100) / totalLength).toInt()
                            onProgress(percent, downloadedBytes, totalLength)
                        } else {
                            onProgress(-1, downloadedBytes, -1)
                        }
                    }
                    output.flush()
                }
            }

            destinationFile
        }
    }

    fun getInstallIntent(apkFile: File): Intent {
        val apkUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
    }

    private fun isRemoteVersionNewer(remote: String, local: String): Boolean {
        if (remote.isBlank() || local.isBlank()) return false
        val remoteParts = remote.split(".").mapNotNull { it.filter { ch -> ch.isDigit() }.toIntOrNull() }
        val localParts = local.split(".").mapNotNull { it.filter { ch -> ch.isDigit() }.toIntOrNull() }

        val maxLen = maxOf(remoteParts.size, localParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val l = localParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }
}
