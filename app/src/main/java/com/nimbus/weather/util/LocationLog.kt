package com.nimbus.weather.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LocationLog {

    private const val TAG = "LocationSearchVM"
    private const val FILE_NAME = "nimbus_location.log"
    private const val MAX_LINES = 200

    fun log(context: Context, message: String) {
        Log.w(TAG, message)
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        try {
            val line = "[$stamp] $message\n"
            appendLine(context, line)
        } catch (e: Exception) {
            Log.w(TAG, "failed to write location log: ${e.message}")
        }
    }

    private fun appendLine(context: Context, line: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appendViaMediaStore(context, line)
        } else {
            appendToFile(context, line)
        }
    }

    private fun appendViaMediaStore(context: Context, line: String) {
        val resolver = context.contentResolver
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME}=? AND " +
            "${MediaStore.MediaColumns.RELATIVE_PATH}=?"
        val selectionArgs = arrayOf(FILE_NAME, Environment.DIRECTORY_DOWNLOADS + "/")
        val uri = resolver.query(
            collection,
            arrayOf(MediaStore.MediaColumns._ID),
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                android.content.ContentUris.withAppendedId(
                    collection,
                    cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                )
            } else {
                null
            }
        }

        val target = uri ?: run {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, FILE_NAME)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/")
            }
            resolver.insert(collection, values)
        } ?: throw IllegalStateException("MediaStore insert failed")

        resolver.openFileDescriptor(target, "wa").use { pfd ->
            FileOutputStream(pfd!!.fileDescriptor).use { it.write(line.toByteArray()) }
        }
    }

    private fun appendToFile(context: Context, line: String) {
        val dir = context.getExternalFilesDir(null) ?: return
        val file = File(dir, FILE_NAME)
        val lines = if (file.exists()) file.readLines().toMutableList() else mutableListOf()
        lines.add(line.trimEnd())
        while (lines.size > MAX_LINES) {
            lines.removeAt(0)
        }
        FileOutputStream(file, false).use {
            it.write(lines.joinToString("\n").toByteArray())
            it.write("\n".toByteArray())
        }
    }
}