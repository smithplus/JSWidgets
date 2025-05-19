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
import org.mozilla.javascript.BaseFunction
import java.net.HttpURLConnection
import java.net.URL
import android.os.Handler
import android.os.Looper

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
            Thread {
                val scriptName = WidgetConfigActivity.loadScriptPref(context, appWidgetId)
                val views = RemoteViews(context.packageName, R.layout.widget_content_layout)

                if (scriptName == null) {
                    views.setTextViewText(R.id.widget_title, "Error")
                    views.setTextViewText(R.id.widget_body, "Widget no configurado. Por favor, elimínelo y vuelva a añadirlo.")
                    Handler(Looper.getMainLooper()).post {
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                    return@Thread
                }

                try {
                    val userWidgetsDir = File(context.getExternalFilesDir(null), "JSWidgets")
                    val userScriptFile = File(userWidgetsDir, scriptName)
                    lateinit var scriptContent: String
                    lateinit var scriptSource: String

                    if (userScriptFile.exists()) {
                        try {
                            scriptContent = userScriptFile.readText()
                            scriptSource = "user script"
                            Log.d("JSWidgetProvider", "Script cargado exitosamente desde almacenamiento de usuario")
                        } catch (e: SecurityException) {
                            Log.e("JSWidgetProvider", "Error de permisos al leer script de usuario: ", e)
                            views.setTextViewText(R.id.widget_title, "Error de Permisos")
                            views.setTextViewText(R.id.widget_body, "No se puede acceder al script. Verifica los permisos de la app.")
                            Handler(Looper.getMainLooper()).post {
                                appWidgetManager.updateAppWidget(appWidgetId, views)
                            }
                            return@Thread
                        }
                    } else {
                        try {
                            val assetManager = context.assets
                            val inputStream = assetManager.open("scripts/$scriptName")
                            scriptContent = inputStream.bufferedReader().use { it.readText() }
                            scriptSource = "asset script"
                            Log.d("JSWidgetProvider", "Script cargado exitosamente desde assets")
                        } catch (e: IOException) {
                            Log.e("JSWidgetProvider", "Error al leer script desde assets: ", e)
                            views.setTextViewText(R.id.widget_title, scriptName.removeSuffix(".js"))
                            views.setTextViewText(R.id.widget_body, "Script no encontrado en assets")
                            Handler(Looper.getMainLooper()).post {
                                appWidgetManager.updateAppWidget(appWidgetId, views)
                            }
                            return@Thread
                        } catch (e: SecurityException) {
                            Log.e("JSWidgetProvider", "Error de permisos al leer script desde assets: ", e)
                            views.setTextViewText(R.id.widget_title, "Error de Permisos")
                            views.setTextViewText(R.id.widget_body, "No se puede acceder al script. Verifica los permisos de la app.")
                            Handler(Looper.getMainLooper()).post {
                                appWidgetManager.updateAppWidget(appWidgetId, views)
                            }
                            return@Thread
                        }
                    }

                    Log.i("JSWidgetProvider", "Ejecutando script '$scriptName' (desde $scriptSource) para widget $appWidgetId")
                    Log.d("JSWidgetProvider", "Contenido del script $scriptName:\n$scriptContent")
                    val result = executeJsScript(scriptContent)
                    Log.d("JSWidgetProvider", "Resultado de executeJsScript para $scriptName: $result")

                    val jsTitle = result?.get("title", result)
                    val jsBody = result?.get("body", result)

                    Log.d("JSWidgetProvider", "jsTitle: $jsTitle, jsBody: $jsBody")

                    val finalTitle = if (jsTitle != null && jsTitle != Scriptable.NOT_FOUND) jsTitle.toString() else scriptName.removeSuffix(".js")
                    val finalBody = if (jsBody != null && jsBody != Scriptable.NOT_FOUND) jsBody.toString() else "Contenido no disponible"

                    Log.d("JSWidgetProvider", "finalTitle: $finalTitle, finalBody: $finalBody")

                    views.setTextViewText(R.id.widget_title, finalTitle)
                    views.setTextViewText(R.id.widget_body, finalBody)
                } catch (e: Exception) {
                    val errorMsg = e.message?.take(80) ?: "Error desconocido"
                    Log.e("JSWidgetProvider", "Error ejecutando script $scriptName para widget $appWidgetId", e)
                    views.setTextViewText(R.id.widget_title, scriptName.removeSuffix(".js"))
                    views.setTextViewText(R.id.widget_body, "Error: $errorMsg")
                }
                Handler(Looper.getMainLooper()).post {
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }.start()
        }

        private fun ensureRhinoContextFactoryInitialized() {
            if (!ContextFactory.hasExplicitGlobal()) {
                ContextFactory.initGlobal(AndroidContextFactory())
            }
        }

        /**
         * Ejecuta un script JavaScript puro y retorna el resultado.
         * Los scripts deben retornar un objeto con las siguientes propiedades opcionales:
         * - title: string (título del widget)
         * - body: string (contenido del widget)
         * - backgroundColor: string (color de fondo en formato hex, ej: "#FF0000")
         * - textColor: string (color del texto en formato hex)
         * - textSize: number (tamaño del texto en sp)
         * - textAlign: string ("left", "center", "right")
         */
        private fun executeJsScript(script: String): Scriptable? = runCatching {
            ensureRhinoContextFactoryInitialized()
            ContextFactory.getGlobal().call { rhino ->
                rhino.optimizationLevel = -1 // Necesario para Android
                val scope: Scriptable = rhino.initStandardObjects()
                // Inyectar variable para indicar que NO es preview
                scope.put("IS_PREVIEW", scope, false)
                // Inyectar función httpGet para peticiones HTTP desde JS puro
                scope.put("httpGet", scope, object : BaseFunction() {
                    override fun call(cx: org.mozilla.javascript.Context?, scope: Scriptable?, thisObj: Scriptable?, args: Array<out Any>?): Any {
                        if (args == null || args.isEmpty()) {
                            return "Error: url es requerida"
                        }
                        val firstArg: Any? = args[0]
                        val urlString = firstArg?.toString() ?: return "Error: url inválida (null o no string)"
                        val headersObj = if (args.size > 1) args[1] else null
                        return try {
                            val url = URL(urlString)
                            val connection = url.openConnection() as HttpURLConnection
                            connection.requestMethod = "GET"
                            connection.connectTimeout = 8000
                            connection.readTimeout = 8000
                            // Si hay headers, agregarlos
                            if (headersObj is Scriptable) {
                                val ids = headersObj.ids
                                for (id in ids) {
                                    val key = id.toString()
                                    val value = headersObj.get(key, headersObj)?.toString() ?: continue
                                    connection.setRequestProperty(key, value)
                                }
                            }
                            val responseCode = connection.responseCode
                            if (responseCode == 200) {
                                connection.inputStream.bufferedReader().use { it.readText() }
                            } else {
                                "Error: HTTP $responseCode"
                            }
                        } catch (e: Exception) {
                            Log.e("JSWidgets-httpGet", "Error en httpGet para url: $urlString", e)
                            "Error: ${e.message ?: e.javaClass.simpleName}"
                        }
                    }
                })
                // Deshabilitar acceso a clases Java
                rhino.setClassShutter { _ -> false }
                
                Log.d("JSWidgetProvider", "Contexto de Rhino inicializado. Ejecutando script...")
                // Evalúa el script. El resultado de la última expresión es devuelto.
                val result = rhino.evaluateString(scope, script, "JSWidgetScript", 1, null)
                Log.d("JSWidgetProvider", "Resultado de evaluateString: $result (tipo: ${result?.javaClass?.name})")
                if (result is Scriptable) {
                    Log.d("JSWidgetProvider", "El resultado es un objeto Scriptable")
                    result // Devuelve el objeto Scriptable si el script retorna un objeto
                } else {
                    // Si el script no devuelve un objeto explícitamente, mostramos un error
                    Log.w("JSWidgetProvider", "El script debe retornar un objeto. Ejemplo: ({ title: 'Mi Widget', body: 'Contenido' })")
                    null 
                }
            }
        }.onFailure { e -> 
            Log.e("JSWidgetProvider", "Error detallado en ejecución de Rhino:", e)
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