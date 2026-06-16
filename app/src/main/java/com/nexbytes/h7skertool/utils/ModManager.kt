package com.nexbytes.h7skertool.utils

import android.content.Context
import android.util.Log
import java.io.File

object ModManager {
    private const val TAG = "ModManager"
    private const val MODS_DIR = ".mods"
    private const val GAME_PACKAGE = "com.dts.freefireth"

    fun getModsDir(context: Context): File {
        val gamePath = File("/sdcard/Android/data/$GAME_PACKAGE/files/$MODS_DIR")
        if (gamePath.parentFile?.exists() == true) { gamePath.mkdirs(); return gamePath }
        val fallback = File(context.filesDir, MODS_DIR); fallback.mkdirs(); return fallback
    }

    fun saveMod(context: Context, name: String, content: String, type: ModType = ModType.RESPONSE): Boolean = try {
        val file = File(getModsDir(context), "${name.sanitize()}.modz")
        val wrapper = ModFile(name = name, type = type.name, content = content, createdAt = System.currentTimeMillis())
        file.writeText(com.google.gson.Gson().toJson(wrapper), Charsets.UTF_8)
        Log.i(TAG, "Saved mod: ${file.absolutePath}"); true
    } catch (e: Exception) { Log.e(TAG, "saveMod failed: ${e.message}"); false }

    fun loadMods(context: Context): List<ModFile> = try {
        val gson = com.google.gson.Gson()
        getModsDir(context).listFiles { f -> f.name.endsWith(".modz") }
            ?.mapNotNull { f -> try { gson.fromJson(f.readText(), ModFile::class.java) } catch (_: Exception) { null } }
            ?.sortedByDescending { it.createdAt } ?: emptyList()
    } catch (e: Exception) { Log.e(TAG, "loadMods error: ${e.message}"); emptyList() }

    fun deleteMod(context: Context, name: String): Boolean = try {
        File(getModsDir(context), "${name.sanitize()}.modz").delete()
    } catch (_: Exception) { false }

    private fun String.sanitize(): String = replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
}

enum class ModType { REQUEST, RESPONSE, HEADER }

data class ModFile(
    val name: String = "",
    val type: String = ModType.RESPONSE.name,
    val content: String = "",
    val createdAt: Long = 0L,
    val version: Int = 1
)
