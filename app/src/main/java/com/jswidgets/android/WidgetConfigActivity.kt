package com.jswidgets.android

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity // Usar AppCompatActivity para temas, etc.
import com.jswidgets.android.R // Import explícito de R
import java.io.File
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.util.Log
import android.provider.Settings
import androidx.activity.viewModels // Para obtener ViewModel
import com.jswidgets.android.model.Widget // Importar Widget
import com.jswidgets.android.viewmodel.WidgetViewModel // Importar ViewModel
import androidx.lifecycle.lifecycleScope // Importación para lifecycleScope
import kotlinx.coroutines.launch // Importación para launch
import com.jswidgets.android.ui.adapter.ScriptConfigAdapter // Importar el nuevo adapter

// Nombre del archivo de SharedPreferences
private const val PREFS_NAME = "com.jswidgets.android.JSWidgetProvider"
private const val PREF_PREFIX_KEY = "widget_script_"

class WidgetConfigActivity : AppCompatActivity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var scriptsListView: ListView
    private lateinit var permissionDeniedContainer: LinearLayout
    private lateinit var openSettingsButton: Button
    private val viewModel: WidgetViewModel by viewModels()
    private var allWidgetsFromVM: List<Widget> = emptyList()

    public override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        setResult(Activity.RESULT_CANCELED)
        setContentView(R.layout.widget_config_activity)

        // Obtener el appWidgetId del intent
        val intentExtras = intent.extras
        if (intentExtras != null) {
            appWidgetId = intentExtras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            Log.e("WidgetConfigActivity", "AppWidget ID inválido, finalizando actividad.")
            finish()
            return // Salir de onCreate si el ID no es válido
        }

        scriptsListView = findViewById<ListView>(R.id.scripts_list_view)
        permissionDeniedContainer = findViewById<LinearLayout>(R.id.permission_denied_container)
        openSettingsButton = findViewById<Button>(R.id.open_settings_button)

        // Configurar el botón de configuración
        openSettingsButton.setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.fromParts("package", packageName, null)
            }
            startActivity(intent)
        }

        // Solicitar permiso si no está concedido
        val permission = android.Manifest.permission.READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            Log.w("WidgetConfigActivity", "Permiso READ_EXTERNAL_STORAGE no concedido, solicitando...")
            ActivityCompat.requestPermissions(this, arrayOf(permission), 123)
        }

        // Observar los widgets del ViewModel usando lifecycleScope
        lifecycleScope.launch {
            viewModel.widgets.collect { widgets ->
                allWidgetsFromVM = widgets
                displayScripts()
            }
        }
        viewModel.loadWidgets(this) // Cargar los widgets
    }

    private fun displayScripts() {
        // Mapear a nombres, indicando si es un ejemplo. El nombre ya no necesita el .js aquí.
        // val scriptDisplayNames = allWidgetsFromVM.map { 
        //     val type = if (it.isExample) " (ejemplo)" else ""
        //     it.name + type 
        // }

        // Usar el nuevo ScriptConfigAdapter
        val adapter = ScriptConfigAdapter(this, allWidgetsFromVM)
        scriptsListView.adapter = adapter

        scriptsListView.setOnItemClickListener { _, _, position, _ ->
            if (position < allWidgetsFromVM.size) {
                val selectedWidget = allWidgetsFromVM[position]
                // Guardar la preferencia con el nombre del widget (que es el nombre del archivo sin .js)
                // JSWidgetProvider añadirá .js al leerlo si es necesario, o buscará por ese nombre en assets.
                // Para consistencia, pasamos el nombre del script SIN .js, ya que así se guarda en el Widget object.
                // El WidgetConfigActivity.saveScriptPref espera el nombre del script (que puede ser de asset o de usuario)
                // Y JSWidgetProvider espera ese mismo nombre para buscar en assets o en archivos de usuario.
                // Si el scriptName en loadScriptPref (y por tanto en saveScriptPref) es el nombre base (sin .js),
                // entonces JSWidgetProvider deberá añadir .js al formar el File para scripts de usuario,
                // o usarlo tal cual para buscar en assets.
                // Revisando JSWidgetProvider, espera el nombre completo con .js.
                // Así que, al configurar, debemos pasar widget.name + ".js".
                configureWidget("${selectedWidget.name}.js")
            } else {
                Log.e("WidgetConfigActivity", "Posición de click inválida: $position, tamaño de lista: ${allWidgetsFromVM.size}")
            }
        }

        val hasPermission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        val hasExampleScripts = allWidgetsFromVM.any { it.isExample }
        val hasUserScripts = allWidgetsFromVM.any { !it.isExample }

        if (!hasPermission && hasUserScripts && !hasExampleScripts) {
             // Si no hay permiso Y hay scripts de usuario Y no hay ejemplos -> Mostrar mensaje de permiso
            permissionDeniedContainer.visibility = View.VISIBLE
        } else if (!hasPermission && !hasUserScripts && !hasExampleScripts){
            // Si no hay permiso Y no hay ningun script -> Mostrar mensaje de permiso (quizas el usuario espera ver los suyos)
            permissionDeniedContainer.visibility = View.VISIBLE
        } else {
            permissionDeniedContainer.visibility = View.GONE
        }
    }

    private fun configureWidget(scriptFileNameWithJs: String) {
        val context: Context = this.applicationContext
        // Guardar la preferencia con el nombre del archivo completo (con .js)
        saveScriptPref(context, appWidgetId, scriptFileNameWithJs)

        val updateIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE, null, context, com.jswidgets.android.widget.JSWidgetProvider::class.java)
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
        sendBroadcast(updateIntent)

        val resultValue = Intent()
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(Activity.RESULT_OK, resultValue)
        finish()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 123) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i("WidgetConfigActivity", "Permiso READ_EXTERNAL_STORAGE concedido, ViewModel recargará.")
                viewModel.loadWidgets(this)
            } else {
                Log.e("WidgetConfigActivity", "Permiso READ_EXTERNAL_STORAGE denegado.")
                viewModel.loadWidgets(this) // Recargar igual para que actualice la lista (sin scripts de usuario)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Recargar scripts cuando se vuelve a la actividad (por ejemplo, después de cambiar permisos o si MainActivity actualizó widgets)
        Log.d("WidgetConfigActivity", "onResume, llamando a viewModel.loadWidgets")
        viewModel.loadWidgets(this)
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