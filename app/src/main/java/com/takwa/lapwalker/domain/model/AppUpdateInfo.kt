package com.takwa.lapwalker.domain.model

data class AppUpdateInfo(
    val versionName: String,
    val releaseTitle: String,
    val changelog: String,
    val downloadUrl: String,
    val fileSizeBytes: Long,
    val isUpdateAvailable: Boolean
)
