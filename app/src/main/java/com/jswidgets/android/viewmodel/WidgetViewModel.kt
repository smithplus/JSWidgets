package com.jswidgets.android.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jswidgets.android.model.Widget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.util.UUID
import android.os.Build
import android.os.Environment
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

class WidgetViewModel : ViewModel() {
    private val _widgets = MutableStateFlow<List<Widget>>(emptyList())
    val widgets: StateFlow<List<Widget>> = _widgets.asStateFlow()

    private val _currentWidget = MutableStateFlow<Widget?>(null)
    val currentWidget: StateFlow<Widget?> = _currentWidget.asStateFlow()

    fun setCurrentWidget(widget: Widget?) {
        _currentWidget.value = widget
    }

    fun loadWidgets(context: Context) {
        viewModelScope.launch {
            val userWidgets = loadUserWidgets(context)
            val exampleWidgets = loadExampleScripts(context)
            // No duplicar por nombre
            val allWidgets = userWidgets + exampleWidgets.filter { ex -> userWidgets.none { it.name == ex.name } }
            _widgets.value = allWidgets
        }
    }

    fun saveWidget(context: Context, widget: Widget) {
        viewModelScope.launch {
            val userWidgetsDir = File(context.getExternalFilesDir(null), "widgets")
            if (!userWidgetsDir.exists()) {
                if (!userWidgetsDir.mkdirs()) {
                    Log.e("WidgetViewModel", "Error al crear el directorio de widgets de usuario.")
                    return@launch
                }
            }
            val widgetFile = File(userWidgetsDir, "${widget.name}.js") // El nombre del archivo es el nombre del widget
            try {
                widgetFile.writeText(widget.scriptContent)
                // Actualizar lista: añadir si es nuevo, o reemplazar si ya existe por nombre (id)
                _widgets.update {
                    val existing = it.find { w -> w.name == widget.name }
                    if (existing != null) {
                        it.map { w -> if (w.name == widget.name) widget.copy(isExample = false) else w } 
                    } else {
                        it + widget.copy(isExample = false)
                    }
                }
            } catch (e: IOException) {
                Log.e("WidgetViewModel", "Error al guardar el widget: ${widget.name}", e)
            }
        }
    }

    fun updateWidget(context: Context, widgetWithPotentialUpdates: Widget) {
        // widgetWithPotentialUpdates contiene: id (original), name (posiblemente nuevo), scriptContent (posiblemente nuevo)
        viewModelScope.launch {
            val originalWidgetInList = _widgets.value.find { it.id == widgetWithPotentialUpdates.id }

            // El widget que representa el estado final a persistir y mostrar en la lista.
            // Su id y name deben ser el nuevo nombre del script.
            val finalWidgetState = widgetWithPotentialUpdates.copy(
                id = widgetWithPotentialUpdates.name, // El id ahora es el nuevo nombre
                isExample = false // Al guardar/actualizar, deja de ser un ejemplo
            )

            // 1. Guardar el archivo del script (con el nuevo nombre si cambió)
            val fileSaved = saveUserScriptFile(context, finalWidgetState.name, finalWidgetState.scriptContent)

            if (fileSaved) {
                // 2. Si el archivo se guardó correctamente, proceder a borrar el archivo antiguo (si hubo renombramiento de un script de usuario)
                if (originalWidgetInList != null && originalWidgetInList.name != finalWidgetState.name && !originalWidgetInList.isExample) {
                    val userWidgetsDir = File(context.getExternalFilesDir(null), "widgets")
                    val oldFile = File(userWidgetsDir, "${originalWidgetInList.name}.js")
                    if (oldFile.exists()) {
                        if (oldFile.delete()) {
                            Log.d("WidgetViewModel", "Archivo antiguo ${originalWidgetInList.name}.js borrado tras renombrar a ${finalWidgetState.name}.js")
                        } else {
                            Log.w("WidgetViewModel", "No se pudo borrar el archivo del widget antiguo: ${originalWidgetInList.name}.js")
                        }
                    }
                }

                // 3. Actualizar la lista _widgets en memoria
                _widgets.update { currentList ->
                    val listAfterRemoval = if (originalWidgetInList != null) {
                        currentList.filterNot { it.id == originalWidgetInList.id }
                    } else {
                        currentList
                    }
                    listAfterRemoval + finalWidgetState // Añadir el estado final del widget
                }
                Log.d("WidgetViewModel", "Widget ${finalWidgetState.name} actualizado en la lista y archivo.")
            } else {
                Log.e("WidgetViewModel", "No se pudo guardar el archivo para ${finalWidgetState.name}, la lista de widgets no se actualizará.")
                // Considerar notificar al usuario del error de guardado.
            }
        }
    }

    fun renameWidget(context: Context, widgetToRename: Widget, newName: String) {
        if (newName.isBlank() || newName == widgetToRename.name) return
        viewModelScope.launch {
            val userWidgetsDir = File(context.getExternalFilesDir(null), "widgets")
            val newFile = File(userWidgetsDir, "$newName.js")

            if (newFile.exists()) {
                Log.e("WidgetViewModel", "Error al renombrar: ya existe un script con el nombre $newName")
                // Aquí podrías notificar al usuario, e.g., con un Toast o un StateFlow de error.
                return@launch
            }

            try {
                // Si es un script de usuario, renombrar archivo.
                if (!widgetToRename.isExample) {
                    val oldFile = File(userWidgetsDir, "${widgetToRename.name}.js")
                    if (oldFile.exists()) {
                        if (!oldFile.renameTo(newFile)) {
                            Log.e("WidgetViewModel", "Error al renombrar el archivo de ${widgetToRename.name} a $newName")
                            return@launch
                        }
                    }
                } else {
                    // Si es un script de ejemplo, simplemente se guarda como nuevo script de usuario con nuevo nombre.
                     if (!userWidgetsDir.exists()) userWidgetsDir.mkdirs()
                    newFile.writeText(widgetToRename.scriptContent)
                }

                _widgets.update {
                    it.map {
                        if (it.id == widgetToRename.id) {
                            it.copy(id = newName, name = newName, isExample = false)
                        } else {
                            it
                        }
                    }
                }
            } catch (e: IOException) {
                Log.e("WidgetViewModel", "Error de IO al renombrar widget: ${widgetToRename.name} a $newName", e)
            }
        }
    }

    fun deleteWidget(context: Context, widgetToDelete: Widget) {
        viewModelScope.launch {
            // Si es un script de usuario, borrar el archivo.
            if (!widgetToDelete.isExample) {
                val userWidgetsDir = File(context.getExternalFilesDir(null), "widgets")
                val fileToDelete = File(userWidgetsDir, "${widgetToDelete.name}.js")
                if (fileToDelete.exists()) {
                    if (!fileToDelete.delete()) {
                        Log.e("WidgetViewModel", "Error al borrar el archivo del script: ${widgetToDelete.name}")
                        // Podrías no continuar y no quitarlo de la lista si el archivo no se pudo borrar
                    }
                }
            }
            // Quitar de la lista en cualquier caso (sea de ejemplo o de usuario cuyo archivo se borró o no existía)
            _widgets.update { list -> list.filterNot { it.id == widgetToDelete.id } }
        }
    }

    fun getUserScriptsDir(context: Context): File {
        val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val jsWidgetsDir = File(publicDir, "JSWidgets")
        if (!jsWidgetsDir.exists()) {
            jsWidgetsDir.mkdirs()
        }
        return jsWidgetsDir
    }

    fun loadUserScripts(context: Context): List<File> {
        val dir = getUserScriptsDir(context)
        return dir.listFiles { file -> file.extension == "js" }?.toList() ?: emptyList()
    }

    fun saveUserScript(context: Context, name: String, content: String) {
        val dir = getUserScriptsDir(context)
        val file = File(dir, name)
        file.writeText(content)
    }

    fun loadExampleScripts(context: Context): List<Widget> {
        val widgets = mutableListOf<Widget>()
        try {
            val assetManager = context.assets
            val scriptFiles = assetManager.list("scripts")?.filter { it.endsWith(".js") } ?: emptyList()
            Log.d("WidgetViewModel", "Scripts encontrados en assets/scripts/: $scriptFiles")
            
            scriptFiles.forEach { scriptFileName ->
                val widgetName = scriptFileName.removeSuffix(".js")
                try {
                    val inputStream = assetManager.open("scripts/$scriptFileName")
                    val content = inputStream.bufferedReader().use { it.readText() }
                    widgets.add(Widget(id = widgetName, name = widgetName, scriptContent = content, isExample = true))
                    Log.d("WidgetViewModel", "Script cargado exitosamente: $scriptFileName")
                } catch (e: IOException) {
                    Log.e("WidgetViewModel", "Error leyendo el script de asset: $scriptFileName", e)
                }
            }
        } catch (e: Exception) {
            Log.e("WidgetViewModel", "Error cargando scripts de ejemplo desde assets", e)
        }
        Log.d("WidgetViewModel", "Total de scripts de ejemplo cargados: ${widgets.size}")
        return widgets
    }

    fun loadUserWidgets(context: Context): List<Widget> {
        val dir = getUserScriptsDir(context)
        return dir.listFiles { file -> file.extension == "js" }?.map { file ->
            Widget(
                id = file.nameWithoutExtension,
                name = file.nameWithoutExtension,
                scriptContent = file.readText(),
                isExample = false
            )
        } ?: emptyList()
    }

    // saveWidget ahora solo guarda el archivo y no modifica _widgets
    fun saveUserScriptFile(context: Context, scriptName: String, scriptContent: String): Boolean {
        return try {
            val userWidgetsDir = File(context.getExternalFilesDir(null), "widgets")
            if (!userWidgetsDir.exists()) {
                if (!userWidgetsDir.mkdirs()) {
                    Log.e("WidgetViewModel", "Error al crear el directorio de widgets de usuario para guardar archivo.")
                    return false
                }
            }
            val widgetFile = File(userWidgetsDir, "$scriptName.js")
            widgetFile.writeText(scriptContent)
            Log.d("WidgetViewModel", "Archivo $scriptName.js guardado.")
            true
        } catch (e: IOException) {
            Log.e("WidgetViewModel", "Error al guardar el archivo del widget: $scriptName.js", e)
            false
        }
    }
} 