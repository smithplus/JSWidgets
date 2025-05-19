package com.jswidgets.android

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity // Usar AppCompatActivity para temas, etc.
import com.jswidgets.android.R // Import explícito de R

// Nombre del archivo de SharedPreferences
private const val PREFS_NAME = "com.jswidgets.android.JSWidgetProvider"
private const val PREF_PREFIX_KEY = "widget_script_"

class WidgetConfigActivity : AppCompatActivity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var scriptsListView: ListView
    private lateinit var exampleScripts: Array<String>

    public override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        setResult(Activity.RESULT_CANCELED)
        setContentView(R.layout.widget_config_activity) // Necesitaremos este layout

        scriptsListView = findViewById<ListView>(R.id.scripts_list_view) // Necesitaremos este ID en el layout

        // Leer automáticamente todos los scripts .js en assets/scripts
        exampleScripts = try {
            assets.list("scripts")?.filter { it.endsWith(".js") }?.toTypedArray() ?: emptyArray()
        } catch (e: Exception) {
            emptyArray()
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, exampleScripts)
        scriptsListView.adapter = adapter

        scriptsListView.setOnItemClickListener { _, _, position, _ ->
            val selectedScript = exampleScripts[position]
            configureWidget(selectedScript)
        }

        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
    }

    private fun configureWidget(scriptName: String) {
        val context: Context = this.applicationContext
        saveScriptPref(context, appWidgetId, scriptName)

        // Forzar actualización del widget
        // Aquí se llamaría a una función de actualización en JSWidgetProvider si fuera necesario
        // Por ahora, JSWidgetProvider leerá la preferencia en su onUpdate
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE, null, context, com.jswidgets.android.widget.JSWidgetProvider::class.java)
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
        sendBroadcast(intent)

        val resultValue = Intent()
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(Activity.RESULT_OK, resultValue)
        finish()
    }

    companion object {
        fun saveScriptPref(context: Context, appWidgetId: Int, scriptName: String) {
            val prefs = context.getSharedPreferences(PREFS_NAME, 0).edit()
            prefs.putString(PREF_PREFIX_KEY + appWidgetId, scriptName)
            prefs.apply()
        }

        fun loadScriptPref(context: Context, appWidgetId: Int): String? {
            val prefs = context.getSharedPreferences(PREFS_NAME, 0)
            return prefs.getString(PREF_PREFIX_KEY + appWidgetId, null)
        }

        fun deleteScriptPref(context: Context, appWidgetId: Int) {
            val prefs = context.getSharedPreferences(PREFS_NAME, 0).edit()
            prefs.remove(PREF_PREFIX_KEY + appWidgetId)
            prefs.apply()
        }
    }
} 