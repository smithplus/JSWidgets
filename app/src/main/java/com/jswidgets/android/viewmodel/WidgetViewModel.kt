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

    fun updateWidget(context: Context, widgetWithPotentialUpdates: Widget) {
        viewModelScope.launch {
            val originalWidgetInList = _widgets.value.find { it.id == widgetWithPotentialUpdates.id }

            val finalWidgetState = widgetWithPotentialUpdates.copy(
                id = widgetWithPotentialUpdates.name, 
                isExample = false,
                iconName = widgetWithPotentialUpdates.iconName // Asegurar que iconName se propaga
            )

            val fileSaved = saveUserScriptFile(context, finalWidgetState.name, finalWidgetState.scriptContent)

            if (fileSaved) {
                if (originalWidgetInList != null && originalWidgetInList.name != finalWidgetState.name && !originalWidgetInList.isExample) {
                    val userScriptsDir = getUserScriptsDir(context)
                    val oldFile = File(userScriptsDir, "${originalWidgetInList.name}.js")
                    if (oldFile.exists()) {
                        if (oldFile.delete()) {
                            Log.d("WidgetViewModel", "Archivo antiguo ${originalWidgetInList.name}.js borrado tras renombrar a ${finalWidgetState.name}.js")
                        } else {
                            Log.w("WidgetViewModel", "No se pudo borrar el archivo del widget antiguo: ${originalWidgetInList.name}.js")
                        }
                    }
                }
                _widgets.update { currentList ->
                    val listAfterRemoval = if (originalWidgetInList != null) {
                        currentList.filterNot { it.id == originalWidgetInList.id }
                    } else {
                        currentList
                    }
                    listAfterRemoval + finalWidgetState
                }
                Log.d("WidgetViewModel", "Widget ${finalWidgetState.name} actualizado, iconName: ${finalWidgetState.iconName}")
            } else {
                Log.e("WidgetViewModel", "No se pudo guardar el archivo para ${finalWidgetState.name}")
            }
        }
    }

    fun renameWidget(context: Context, widgetToRename: Widget, newName: String) {
        if (newName.isBlank() || newName == widgetToRename.name) return
        viewModelScope.launch {
            val userScriptsDir = getUserScriptsDir(context)
            val newFile = File(userScriptsDir, "$newName.js")

            if (newFile.exists()) {
                Log.e("WidgetViewModel", "Error al renombrar: ya existe un script con el nombre $newName")
                // Aquí podrías notificar al usuario, e.g., con un Toast o un StateFlow de error.
                return@launch
            }

            try {
                // Si es un script de usuario, renombrar archivo.
                if (!widgetToRename.isExample) {
                    val oldFile = File(userScriptsDir, "${widgetToRename.name}.js")
                    if (oldFile.exists()) {
                        if (!oldFile.renameTo(newFile)) {
                            Log.e("WidgetViewModel", "Error al renombrar el archivo de ${widgetToRename.name} a $newName")
                            return@launch
                        }
                    }
                } else {
                    // Si es un script de ejemplo, simplemente se guarda como nuevo script de usuario con nuevo nombre.
                    if (!userScriptsDir.exists()) userScriptsDir.mkdirs()
                    newFile.writeText(widgetToRename.scriptContent)
                }

                _widgets.update {
                    it.map {
                        if (it.id == widgetToRename.id) {
                            it.copy(id = newName, name = newName, isExample = false, iconName = widgetToRename.iconName) // Mantener iconName
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
                val userScriptsDir = getUserScriptsDir(context)
                val fileToDelete = File(userScriptsDir, "${widgetToDelete.name}.js")
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
        val appSpecificDir = context.getExternalFilesDir(null)
        val jsWidgetsDir = File(appSpecificDir, "JSWidgets")
        if (!jsWidgetsDir.exists()) {
            jsWidgetsDir.mkdirs()
        }
        return jsWidgetsDir
    }

    private fun getPublicUserScriptsDir(): File? {
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        if (documentsDir == null) {
            Log.e("WidgetViewModel", "Directorio Documents no disponible.")
            return null
        }
        val jsWidgetsPublicDir = File(documentsDir, "JSWidgets")
        if (!jsWidgetsPublicDir.exists()) {
            // No crearla si no existe, el usuario debe crearla o la app la usará si existe
            Log.i("WidgetViewModel", "Directorio público Documents/JSWidgets/ no encontrado. El usuario puede crearlo.")
            return null
        }
        return jsWidgetsPublicDir
    }

    fun loadUserScripts(context: Context): List<File> {
        val scriptFiles = mutableListOf<File>()
        
        // 1. Cargar desde el directorio específico de la app
        val appSpecificDir = getUserScriptsDir(context)
        Log.d("WidgetViewModel", "Cargando scripts de usuario desde dir específico de app: ${appSpecificDir.absolutePath}")
        appSpecificDir.listFiles { file -> file.extension == "js" }?.let { files ->
            Log.d("WidgetViewModel", "Archivos encontrados en dir específico de app: ${files.map { it.name }.joinToString()}")
            scriptFiles.addAll(files)
        }

        // 2. Cargar desde el directorio público Documents/JSWidgets/
        // Esto requiere permiso READ_EXTERNAL_STORAGE, que se maneja en la UI (WidgetConfigActivity)
        val publicDir = getPublicUserScriptsDir()
        if (publicDir != null && publicDir.canRead()) {
            Log.d("WidgetViewModel", "Cargando scripts de usuario desde dir público: ${publicDir.absolutePath}")
            publicDir.listFiles { file -> file.extension == "js" }?.let { files ->
                Log.d("WidgetViewModel", "Archivos encontrados en dir público: ${files.map { it.name }.joinToString()}")
                // Añadir solo si no existe ya uno con el mismo nombre del dir de la app (priorizar el de la app si hay conflicto en esta simple unión)
                // Una lógica más avanzada podría comparar fechas de modificación.
                // O, alternativamente, priorizar el público:
                files.forEach { publicFile ->
                    if (scriptFiles.none { appSpecificFile -> appSpecificFile.name == publicFile.name }) {
                        scriptFiles.add(publicFile)
                    }
                }
            }
        } else {
            if (publicDir == null) {
                 Log.i("WidgetViewModel", "Directorio público no disponible o no encontrado.")
            } else {
                 Log.w("WidgetViewModel", "No se puede leer del directorio público: ${publicDir.absolutePath}. ¿Permiso READ_EXTERNAL_STORAGE concedido?")
            }
        }
        
        if (scriptFiles.isEmpty()) {
            Log.d("WidgetViewModel", "No se encontraron archivos de usuario en ninguna ubicación.")
        }
        return scriptFiles.distinctBy { it.name } // Asegurar unicidad por nombre, priorizando los primeros añadidos (app-specific)
    }

    fun loadUserWidgets(context: Context): List<Widget> {
        val scriptFiles = loadUserScripts(context)
        Log.d("WidgetViewModel", "Total de archivos de script de usuario (después de posible fusión): ${scriptFiles.size} nombres: ${scriptFiles.map {it.name}.joinToString()}")
        return scriptFiles.mapNotNull { file ->
            try {
                val widgetName = file.nameWithoutExtension
                val scriptContent = file.readText()
                Widget(
                    id = widgetName, // ID es el nombre para scripts de usuario
                    name = widgetName,
                    scriptContent = scriptContent,
                    isExample = false,
                    iconName = null // O un nombre de icono por defecto para usuarios: "Star", "AccountCircle", etc.
                                  // Si se quiere persistir el iconName para scripts de usuario, se necesitaría un archivo .meta o codificarlo en el nombre del archivo.
                                  // Por ahora, los scripts de usuario no tendrán un icono específico persistente de esta carga.
                )
            } catch (e: IOException) {
                Log.e("WidgetViewModel", "Error al leer el script de usuario: ${file.name}", e)
                null
            }
        }
    }

    fun loadExampleScripts(context: Context): List<Widget> {
        val widgets = mutableListOf<Widget>()
        try {
            val assetManager = context.assets
            val scriptFiles = assetManager.list("scripts")?.filter { it.endsWith(".js") } ?: emptyList()
            Log.d("WidgetViewModel", "Scripts encontrados en assets/scripts/: $scriptFiles")
            
            // Nombres de iconos de ejemplo para rotar (usar nombres de Material Icons)
            val exampleIconNames = listOf(
                "Favorite", "Info", "Settings", "Build", "Place", "DateRange", "Cloud", "Search", "Call", "Email"
            )

            scriptFiles.forEachIndexed { index, scriptFileName ->
                val widgetName = scriptFileName.removeSuffix(".js")
                try {
                    val scriptContent = assetManager.open("scripts/$scriptFileName").bufferedReader().use { it.readText() }
                    val iconNameForExample = exampleIconNames[index % exampleIconNames.size]
                    widgets.add(
                        Widget(
                            id = widgetName, // ID es el nombre para scripts de ejemplo
                            name = widgetName,
                            scriptContent = scriptContent,
                            isExample = true,
                            iconName = iconNameForExample // Asignar nombre de icono
                        )
                    )
                    Log.d("WidgetViewModel", "Script cargado exitosamente: $scriptFileName, Icono: $iconNameForExample")
                } catch (e: IOException) {
                    Log.e("WidgetViewModel", "Error al cargar el script de ejemplo: $scriptFileName", e)
                }
            }
            Log.d("WidgetViewModel", "Total de scripts de ejemplo cargados: ${widgets.size}")
        } catch (e: Exception) {
            Log.e("WidgetViewModel", "No se pudieron listar los scripts de assets/scripts", e)
        }
        return widgets
    }

    private fun saveUserScriptFile(context: Context, widgetName: String, scriptContent: String): Boolean {
        val userScriptsDir = getUserScriptsDir(context)
        // Asegurarse de que el nombre del archivo siempre tenga .js
        val fileName = if (widgetName.endsWith(".js")) widgetName else "${widgetName}.js"
        val widgetFile = File(userScriptsDir, fileName)
        return try {
            widgetFile.writeText(scriptContent)
            Log.d("WidgetViewModel", "Script guardado en: ${widgetFile.absolutePath}")
            true
        } catch (e: IOException) {
            Log.e("WidgetViewModel", "Error al guardar el script en archivo: $fileName", e)
            false
        }
    }
} 