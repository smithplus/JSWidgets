package com.jswidgets.android.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.jswidgets.android.R // Importar R
import com.jswidgets.android.WidgetConfigActivity // Importar para SharedPreferences
import org.mozilla.javascript.Context as RhinoContext
import org.mozilla.javascript.ContextFactory
import org.mozilla.javascript.Scriptable
import java.io.File
import java.io.IOException

// ContextFactory personalizado para establecer el ClassLoader
class AndroidContextFactory : ContextFactory() {
    override fun onContextCreated(cx: RhinoContext) {
        super.onContextCreated(cx)
        cx.applicationClassLoader = RhinoContext::class.java.classLoader
    }
}

class JSWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        // Cuando se elimina un widget, borrar su preferencia de script
        appWidgetIds.forEach {
            WidgetConfigActivity.deleteScriptPref(context, it)
        }
    }

    companion object {
        internal fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val scriptName = WidgetConfigActivity.loadScriptPref(context, appWidgetId)
            val views = RemoteViews(context.packageName, R.layout.widget_content_layout) // Nuevo layout para contenido

            if (scriptName == null) {
                views.setTextViewText(R.id.widget_title, "Error")
                views.setTextViewText(R.id.widget_body, "Widget no configurado. Por favor, elimínelo y vuelva a añadirlo.")
                appWidgetManager.updateAppWidget(appWidgetId, views)
                return
            }

            try {
                // Intentar cargar primero desde los scripts de usuario (almacenamiento externo privado)
                val userWidgetsDir = File(context.getExternalFilesDir(null), "widgets")
                val userScriptFile = File(userWidgetsDir, scriptName)
                var scriptContent: String? = null
                var scriptSource = ""

                if (userScriptFile.exists()) {
                    scriptContent = userScriptFile.readText()
                    scriptSource = "user script"
                } else {
                    // Si no está en los scripts de usuario, intentar cargarlo desde assets (ejemplos)
                    try {
                        val assetManager = context.assets
                        val inputStream = assetManager.open("scripts/$scriptName")
                        scriptContent = inputStream.bufferedReader().use { it.readText() }
                        scriptSource = "asset script"
                    } catch (e: IOException) {
                        Log.w("JSWidgetProvider", "Script '$scriptName' no encontrado en assets ni en scripts de usuario.")
                    }
                }

                if (scriptContent != null) {
                    Log.i("JSWidgetProvider", "Ejecutando script '$scriptName' (desde $scriptSource) para widget $appWidgetId")
                    Log.d("JSWidgetProvider", "Contenido del script $scriptName:\n$scriptContent")
                    val result = executeJsScript(scriptContent) 
                    Log.d("JSWidgetProvider", "Resultado de executeJsScript para $scriptName: $result")

                    // Asumimos que el resultado es un objeto con title y body
                    val jsTitle = result?.get("title", result)
                    val jsBody = result?.get("body", result)

                    val finalTitle = if (jsTitle != null && jsTitle != Scriptable.NOT_FOUND) jsTitle.toString() else scriptName.removeSuffix(".js")
                    val finalBody = if (jsBody != null && jsBody != Scriptable.NOT_FOUND) jsBody.toString() else "Contenido no disponible"
                    
                    views.setTextViewText(R.id.widget_title, finalTitle)
                    views.setTextViewText(R.id.widget_body, finalBody)
                } else {
                    views.setTextViewText(R.id.widget_title, scriptName.removeSuffix(".js"))
                    views.setTextViewText(R.id.widget_body, "Script no encontrado")
                }
            } catch (e: Exception) {
                Log.e("JSWidgetProvider", "Error al actualizar widget $appWidgetId con script $scriptName", e)
                views.setTextViewText(R.id.widget_title, scriptName.removeSuffix(".js"))
                views.setTextViewText(R.id.widget_body, "Error al ejecutar script: ${e.message?.take(50)}")
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun ensureRhinoContextFactoryInitialized() {
            if (!ContextFactory.hasExplicitGlobal()) {
                ContextFactory.initGlobal(AndroidContextFactory())
            }
        }

        private fun executeJsScript(script: String): Scriptable? = runCatching {
            ensureRhinoContextFactoryInitialized()
            ContextFactory.getGlobal().call { rhino ->
                rhino.optimizationLevel = -1 // Necesario para Android
                val scope: Scriptable = rhino.initStandardObjects()
                // Evalúa el script. El resultado de la última expresión es devuelto.
                val result = rhino.evaluateString(scope, script, "JSWidgetScript", 1, null)
                if (result is Scriptable) {
                    result // Devuelve el objeto Scriptable si el script retorna un objeto
                } else {
                    // Si el script no devuelve un objeto explícitamente (ej. solo un string o número),
                    // no podemos hacer get("title"). En este MVP, esperamos un objeto.
                    // Podrías envolver el resultado si no es Scriptable, o manejarlo diferente.
                    Log.w("JSWidgetProvider", "El resultado del script no fue un objeto Scriptable: ${result?.javaClass?.name}")
                    null 
                }
            }
        }.onFailure { e -> 
            Log.e("JSWidgetProvider", "Error detallado en ejecución de Rhino:", e) // Loguear excepción completa
        }.getOrNull()

        // La extensión 'use' ya no es estrictamente necesaria si usamos ContextFactory.getGlobal().call
        // Pero la mantenemos por si se usa en otros sitios o para futura referencia.
        private fun <T> RhinoContext.use(block: (RhinoContext) -> T): T {
            // Esta función ya no maneja el ContextFactory.initGlobal(), se hace en executeJsScript
            try {
                return block(this)
            } finally {
                RhinoContext.exit() // Asegurar que se llama exit si el contexto se entró manualmente
            }
        }
    }
} 