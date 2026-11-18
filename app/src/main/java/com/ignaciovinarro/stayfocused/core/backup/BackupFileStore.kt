package com.ignaciovinarro.stayfocused.core.backup

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Lee y escribe el archivo de respaldo por Uri, con el selector de archivos del sistema (no hace falta
// ningún permiso de almacenamiento). Envuelve ContentResolver para poder simularlo en las pruebas.
@Singleton
open class BackupFileStore @Inject constructor(
    @ApplicationContext private val context: Context
) {

    open suspend fun write(uri: Uri, text: String) = withContext(Dispatchers.IO) {
        // "wt" trunca: si el usuario elige un archivo existente, no quedan restos del contenido anterior.
        val stream = context.contentResolver.openOutputStream(uri, "wt")
            ?: throw java.io.IOException("No se pudo abrir el archivo para escribir")
        stream.bufferedWriter(Charsets.UTF_8).use { it.write(text) }
    }

    open suspend fun read(uri: Uri): String = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openInputStream(uri)
            ?: throw java.io.IOException("No se pudo abrir el archivo para leer")
        stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }
}
