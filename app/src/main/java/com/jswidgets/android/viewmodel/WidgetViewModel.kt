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
            val userWidgetsDir = File(context.getExternalFilesDir(null), "widgets")
            val loadedWidgets = mutableListOf<Widget>()
            var loadedFromUserFiles = false

            if (userWidgetsDir.exists() && userWidgetsDir.isDirectory) {
                userWidgetsDir.listFiles { _, name -> name.endsWith(".js") }?.forEach { file ->
                    try {
                        val content = file.readText()
                        // Usamos el nombre del archivo (sin .js) como ID y nombre inicial
                        val widgetName = file.nameWithoutExtension
                        loadedWidgets.add(Widget(id = widgetName, name = widgetName, scriptContent = content))
                        loadedFromUserFiles = true
                    } catch (e: IOException) {
                        Log.e("WidgetViewModel", "Error leyendo el script de usuario: ${file.name}", e)
                    }
                }
            }

            if (!loadedFromUserFiles && loadedWidgets.isEmpty()) {
                Log.i("WidgetViewModel", "No user widgets found or loaded, loading example widgets from assets.")
                try {
                    val assetManager = context.assets
                    val exampleScripts = listOf("time_widget.js", "date_widget.js", "greeting_widget.js", "btc_price_widget.js")
                    exampleScripts.forEach { scriptFileName ->
                        try {
                            val inputStream = assetManager.open("scripts/$scriptFileName")
                            val content = inputStream.bufferedReader().use { it.readText() }
                            val widgetName = scriptFileName.removeSuffix(".js")
                            loadedWidgets.add(Widget(id = widgetName, name = widgetName, scriptContent = content, isExample = true))
                        } catch (e: IOException) {
                            Log.e("WidgetViewModel", "Error leyendo el script de asset: $scriptFileName", e)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("WidgetViewModel", "Error cargando scripts de ejemplo desde assets", e)
                }
            }
            _widgets.value = loadedWidgets
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

    fun updateWidget(context: Context, widget: Widget) {
        // Para los scripts de usuario, el nombre es el ID. Si el nombre cambia, es un "renombrar".
        // Si el id original (nombre de archivo antiguo) es diferente al nuevo nombre, tratamos como guardar y borrar el antiguo.
        // Por ahora, updateWidget asume que el ID (nombre) no ha cambiado, solo el contenido.
        // Si el widget era un ejemplo, guardarlo lo convierte en un widget de usuario.

        val originalWidget = _widgets.value.find { it.id == widget.id }
        
        saveWidget(context, widget.copy(id = widget.name, isExample = false)) // Guardar siempre como widget de usuario

        // Si el nombre original (ID) es diferente al nuevo nombre y el original no era un ejemplo,
        // significa que se renombró un widget de usuario, así que borramos el archivo antiguo.
        if (originalWidget != null && originalWidget.name != widget.name && !originalWidget.isExample) {
             val userWidgetsDir = File(context.getExternalFilesDir(null), "widgets")
             val oldFile = File(userWidgetsDir, "${originalWidget.name}.js")
             if (oldFile.exists()) {
                 if (!oldFile.delete()) {
                     Log.w("WidgetViewModel", "No se pudo borrar el archivo del widget antiguo tras renombrar: ${originalWidget.name}.js")
                 }
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
} 