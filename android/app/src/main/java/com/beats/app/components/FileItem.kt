package com.beats.app.components

import java.io.File

data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val file: File // This is the property causing the error
)
