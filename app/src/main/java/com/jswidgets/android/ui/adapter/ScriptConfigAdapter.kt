package com.jswidgets.android.ui.adapter // Nueva ubicación para el adapter

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.jswidgets.android.R
import com.jswidgets.android.model.Widget

// Colores para los items, similar a MainActivity pero en formato Int para facilidad de uso en el adapter tradicional
val SCRIPT_CONFIG_ITEM_COLORS = listOf(
    0xFF4CAF50, // Green
    0xFF3F51B5, // Indigo
    0xFFFFC107, // Amber
    0xFFE91E63, // Pink
    0xFF009688, // Teal
    0xFF795548, // Brown
    0xFF9C27B0, // Purple
    0xFF03A9F4, // Light Blue
    0xFFCDDC39, // Lime
    0xFFFF5722  // Deep Orange
).map { it.toInt() } // Convertir a Int

class ScriptConfigAdapter(context: Context, private val widgets: List<Widget>) :
    ArrayAdapter<Widget>(context, 0, widgets) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.list_item_script_config, parent, false)

        val widget = getItem(position)

        val colorIndicator = view.findViewById<View>(R.id.script_color_indicator)
        val scriptIcon = view.findViewById<ImageView>(R.id.script_icon)
        val scriptNameText = view.findViewById<TextView>(R.id.script_name_text)

        if (widget != null) {
            scriptNameText.text = widget.name
            
            val colorRes:
            Int
            if (widget.isExample) {
                // Usar un color de la lista de ejemplos, ciclando según su índice entre los ejemplos
                val exampleIndex = widgets.filter { it.isExample }.indexOf(widget)
                colorRes = if (exampleIndex != -1) {
                    SCRIPT_CONFIG_ITEM_COLORS[exampleIndex % SCRIPT_CONFIG_ITEM_COLORS.size]
                } else {
                    SCRIPT_CONFIG_ITEM_COLORS[position % SCRIPT_CONFIG_ITEM_COLORS.size] // Fallback al comportamiento anterior
                }
                scriptIcon.setImageResource(android.R.drawable.btn_star_big_on) // Icono lleno para ejemplos (placeholder)
            } else {
                // Color por defecto para scripts de usuario (puedes definir uno específico o usar uno de la lista)
                colorRes = SCRIPT_CONFIG_ITEM_COLORS[0] // Verde por defecto para usuarios
                scriptIcon.setImageResource(android.R.drawable.btn_star_big_off) // Icono diferente para usuario (placeholder)
            }
            colorIndicator.setBackgroundColor(colorRes)
            scriptIcon.colorFilter = PorterDuffColorFilter(colorRes, PorterDuff.Mode.SRC_IN)
            // scriptNameText.setTextColor(ContextCompat.getColor(context, R.color.default_text_color)) // Asegurar color de texto legible
            // Usar un color de texto estándar del sistema para mayor compatibilidad inicial
            val typedValue = android.util.TypedValue()
            context.theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
            // scriptNameText.setTextColor(typedValue.data) // Comentado temporalmente
            scriptNameText.setTextColor(android.graphics.Color.BLACK) // Prueba de depuración

        } else {
            scriptNameText.text = "Error: Widget no encontrado"
        }

        return view
    }
} 