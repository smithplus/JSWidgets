package com.jswidgets.android.model

import java.io.File
import java.util.UUID

/**
 * Represents a user-defined widget.
 *
 * @param id Unique identifier for the widget. Defaults to a random UUID.
 * @param name User-defined name for the widget.
 * @param scriptContent The JavaScript content that defines the widget's behavior and appearance.
 * @param isExample Indicates whether the widget is an example or created by the user.
 */
data class Widget(
    val id: String = UUID.randomUUID().toString(), // Unique ID for each widget
    var name: String, // Cambiado a var para que coincida con el uso en updateWidget y fromFile
    var scriptContent: String, // Cambiado a var
    val isExample: Boolean = false // Indica si el widget es de los ejemplos o creado por el usuario
) {
    companion object {
        fun fromFile(file: File): Widget {
            val name = file.nameWithoutExtension
            val scriptContent = file.readText()
            // Usamos el nombre del archivo como ID también para consistencia con cómo se guardan
            // Los widgets cargados desde archivo nunca son ejemplos por defecto
            return Widget(id = name, name = name, scriptContent = scriptContent, isExample = false)
        }
    }
} 