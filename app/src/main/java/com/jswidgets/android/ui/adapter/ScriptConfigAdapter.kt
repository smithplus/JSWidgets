package com.jswidgets.android.ui.adapter

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.jswidgets.android.R
import com.jswidgets.android.model.Widget

val SCRIPT_CONFIG_ITEM_COLORS = listOf(
    0xFF4CAF50, 0xFF3F51B5, 0xFFFFC107, 0xFFE91E63, 0xFF009688, 
    0xFF795548, 0xFF9C27B0, 0xFF03A9F4, 0xFFCDDC39, 0xFFFF5722
).map { it.toInt() }

// Mapeo de iconName (String) a android.R.drawable (Int)
fun getDrawableResForIconName(iconName: String?): Int {
    return when (iconName) {
        "Star" -> android.R.drawable.btn_star_big_on
        "Favorite" -> android.R.drawable.star_on // Similar, no exacto
        "Settings" -> android.R.drawable.ic_menu_preferences
        "Info" -> android.R.drawable.ic_menu_info_details
        "Home" -> android.R.drawable.ic_menu_myplaces // Similar
        "AccountCircle" -> android.R.drawable.ic_menu_myplaces // Placeholder, no hay un buen análogo directo
        "Search" -> android.R.drawable.ic_menu_search
        "Delete" -> android.R.drawable.ic_menu_delete
        "AddCircle" -> android.R.drawable.ic_input_add
        "CheckCircle" -> android.R.drawable.checkbox_on_background // Similar
        "Warning" -> android.R.drawable.ic_dialog_alert
        "Place" -> android.R.drawable.ic_menu_mapmode
        "DateRange" -> android.R.drawable.ic_menu_month
        "Build" -> android.R.drawable.ic_menu_manage
        "Call" -> android.R.drawable.ic_menu_call
        "Email" -> android.R.drawable.ic_dialog_email
        "ShoppingCart" -> android.R.drawable.ic_menu_search // Placeholder
        "ThumbUp" -> android.R.drawable.btn_star_big_on // Placeholder
        "Visibility" -> android.R.drawable.ic_menu_view
        "Cloud" -> android.R.drawable.stat_sys_upload // Similar
        "Lightbulb" -> android.R.drawable.ic_menu_help // Similar
        "Lock" -> android.R.drawable.ic_lock_lock
        "Notifications" -> android.R.drawable.stat_notify_chat // Similar
        "Share" -> android.R.drawable.ic_menu_share
        else -> android.R.drawable.btn_star // Icono por defecto
    }
}

class ScriptConfigAdapter(context: Context, private val widgets: List<Widget>) :
    ArrayAdapter<Widget>(context, 0, widgets) {

    private fun getTextColorForBackground(backgroundColor: Int): Int {
        return if (ColorUtils.calculateLuminance(backgroundColor) > 0.5) Color.BLACK else Color.WHITE
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.list_item_script_config, parent, false)

        val widget = getItem(position)
        val rootLayout = view.findViewById<View>(R.id.script_item_root)
        val scriptIcon = view.findViewById<ImageView>(R.id.script_icon)
        val scriptNameText = view.findViewById<TextView>(R.id.script_name_text)

        if (widget != null) {
            scriptNameText.text = widget.name
            val colorRes: Int
            val iconRes: Int

            if (widget.isExample) {
                val exampleIndex = widgets.filter { it.isExample }.indexOf(widget)
                colorRes = if (exampleIndex != -1) {
                    SCRIPT_CONFIG_ITEM_COLORS[exampleIndex % SCRIPT_CONFIG_ITEM_COLORS.size]
                } else {
                    SCRIPT_CONFIG_ITEM_COLORS[position % SCRIPT_CONFIG_ITEM_COLORS.size]
                }
                iconRes = getDrawableResForIconName(widget.iconName ?: "Star")
            } else {
                colorRes = SCRIPT_CONFIG_ITEM_COLORS[0]
                iconRes = getDrawableResForIconName(widget.iconName ?: "Star")
            }
            
            scriptIcon.setImageResource(iconRes)
            rootLayout.setBackgroundColor(colorRes)
            val textColor = getTextColorForBackground(colorRes)
            scriptNameText.setTextColor(textColor)
            scriptIcon.clearColorFilter()
            scriptIcon.setColorFilter(textColor, PorterDuff.Mode.SRC_IN)

        } else {
            scriptNameText.text = context.getString(R.string.widget_load_error)
            rootLayout.setBackgroundColor(Color.LTGRAY)
            scriptNameText.setTextColor(Color.BLACK)
            scriptIcon.setImageResource(android.R.drawable.ic_dialog_alert)
            scriptIcon.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN)
        }
        return view
    }
} 