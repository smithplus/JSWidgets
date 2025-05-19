package com.jswidgets.android

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.Text
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.jswidgets.android.model.Widget
import com.jswidgets.android.ui.theme.JSWidgetsTheme
import com.jswidgets.android.viewmodel.WidgetViewModel
import java.io.File
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.AlertDialog
import androidx.compose.ui.platform.LocalContext
import android.widget.RemoteViews
import com.jswidgets.android.R
import org.mozilla.javascript.ContextFactory
import org.mozilla.javascript.Scriptable
import androidx.compose.ui.unit.sp
import com.jswidgets.android.widget.AndroidContextFactory
import org.mozilla.javascript.BaseFunction
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Lista de colores para los items de script
val scriptColors = listOf(
    Color(0xFF4CAF50), // Green
    Color(0xFF3F51B5), // Indigo
    Color(0xFFFFC107), // Amber
    Color(0xFFE91E63), // Pink
    Color(0xFF009688), // Teal
    Color(0xFF795548), // Brown
    Color(0xFF9C27B0), // Purple
    Color(0xFF03A9F4), // Light Blue
    Color(0xFFCDDC39), // Lime
    Color(0xFFFF5722)  // Deep Orange
)

class MainActivity : ComponentActivity() {
    private val viewModel: WidgetViewModel by viewModels()
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val readGranted = permissions.getOrDefault(Manifest.permission.READ_EXTERNAL_STORAGE, false)

        if (readGranted) {
            Log.d("MainActivity", "READ_EXTERNAL_STORAGE permission granted via dialog")
            viewModel.loadWidgets(this)
        } else {
            Log.d("MainActivity", "READ_EXTERNAL_STORAGE permission denied via dialog")
            Toast.makeText(this, "Permiso de lectura denegado. No se podrán cargar widgets de usuario desde el almacenamiento. Se usarán los ejemplos.", Toast.LENGTH_LONG).show()
            viewModel.loadWidgets(this) // Cargar ejemplos si se deniega
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Primero, configurar la UI
        setContent {
            JSWidgetsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    val widgets by viewModel.widgets.collectAsState()
                    val currentWidget by viewModel.currentWidget.collectAsState()

                    when {
                        currentWidget != null -> {
                            WidgetEditorScreen(
                                widget = currentWidget!!,
                                onSave = { name, content ->
                                    viewModel.updateWidget(this, Widget(name, name, content))
                                    viewModel.setCurrentWidget(null)
                                },
                                onCancel = {
                                    viewModel.setCurrentWidget(null)
                                }
                            )
                        }
                        else -> {
                            WidgetListScreen(
                                widgets = widgets,
                                onAddWidget = { viewModel.setCurrentWidget(Widget("", "", "")) },
                                onEditWidget = { widget -> viewModel.setCurrentWidget(widget) },
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
        
        // Luego, verificar permisos y cargar datos
        checkAndRequestStoragePermission()
    }

    private fun checkAndRequestStoragePermission() {
        val permissionsToRequest = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        
        val allPermissionsGranted = permissionsToRequest.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allPermissionsGranted) {
            Log.d("MainActivity", "READ_EXTERNAL_STORAGE permission already granted")
            viewModel.loadWidgets(this) // Cargar widgets si el permiso ya está concedido
            return
        }
        
        Log.d("MainActivity", "READ_EXTERNAL_STORAGE permission not granted, requesting...")
        requestPermissionLauncher.launch(permissionsToRequest)
    }
}

@Composable
fun WidgetListScreen(
    widgets: List<Widget>,
    onAddWidget: () -> Unit,
    onEditWidget: (Widget) -> Unit,
    viewModel: WidgetViewModel
) {
    var showRenameDialog by remember { mutableStateOf<Widget?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Widget?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scripts", color = MaterialTheme.colors.onPrimary) },
                navigationIcon = {
                    IconButton(onClick = { /* TODO: Acción de configuración */ }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Configuración", tint = MaterialTheme.colors.onPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = onAddWidget) {
                        Icon(Icons.Filled.Add, contentDescription = "Agregar Widget", tint = MaterialTheme.colors.onPrimary)
                    }
                },
                backgroundColor = MaterialTheme.colors.primary // O un color específico
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Campo de búsqueda (placeholder)
            OutlinedTextField(
                value = "", 
                onValueChange = {},
                label = { Text("Search") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            if (widgets.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No hay scripts. ¡Crea uno nuevo!")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    itemsIndexed(widgets) { index, widget ->
                        WidgetItem(
                            widget = widget,
                            color = scriptColors[index % scriptColors.size],
                            onItemClick = { onEditWidget(widget) },
                            onEditActionClick = { onEditWidget(widget) },
                            onRenameActionClick = { showRenameDialog = widget },
                            onDeleteActionClick = { showDeleteDialog = widget }
                        )
                    }
                }
            }
        }
    }

    // Diálogos
    showRenameDialog?.let { widgetToRename ->
        RenameDialog(
            currentName = widgetToRename.name,
            onConfirm = { newName ->
                viewModel.renameWidget(context, widgetToRename, newName)
                showRenameDialog = null
            },
            onDismiss = { showRenameDialog = null }
        )
    }

    showDeleteDialog?.let { widgetToDelete ->
        ConfirmDeleteDialog(
            widgetName = widgetToDelete.name,
            onConfirm = {
                viewModel.deleteWidget(context, widgetToDelete)
                showDeleteDialog = null
            },
            onDismiss = { showDeleteDialog = null }
        )
    }
}

@Composable
fun WidgetItem(
    widget: Widget,
    color: Color,
    onItemClick: () -> Unit,
    onEditActionClick: () -> Unit,
    onRenameActionClick: () -> Unit,
    onDeleteActionClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color)
            .clickable(onClick = onItemClick)
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.Star, 
            contentDescription = "Script Icon",
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = widget.name,
            style = MaterialTheme.typography.h6.copy(color = Color.White),
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(16.dp))
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = "Opciones",
                    tint = Color.White
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(onClick = {
                    onEditActionClick()
                    menuExpanded = false
                }) {
                    Text("Editar")
                }
                DropdownMenuItem(onClick = {
                    onRenameActionClick()
                    menuExpanded = false
                }) {
                    Text("Renombrar")
                }
                DropdownMenuItem(onClick = {
                    onDeleteActionClick()
                    menuExpanded = false
                }) {
                    Text("Borrar")
                }
            }
        }
    }
}

@Composable
fun RenameDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Renombrar Script") },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Nuevo nombre") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newName.isNotBlank()) {
                        onConfirm(newName)
                    }
                }
            ) {
                Text("Renombrar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun ConfirmDeleteDialog(
    widgetName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirmar Borrado") },
        text = { Text("¿Estás seguro de que quieres borrar el script \"$widgetName\"? Esta acción no se puede deshacer.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.error)
            ) {
                Text("Borrar", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun WidgetEditorScreen(
    widget: Widget,
    onSave: (String, String) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(widget.name) }
    var scriptContent by remember { mutableStateOf(widget.scriptContent) }
    var showError by remember { mutableStateOf(false) }
    var showPreview by remember { mutableStateOf(false) }
    var previewResult by remember { mutableStateOf<org.mozilla.javascript.Scriptable?>(null) }
    var previewError by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (widget.id.isEmpty()) "Nuevo Script" else "Editar Script",
                        color = MaterialTheme.colors.onPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Filled.Close, contentDescription = "Cancelar", tint = MaterialTheme.colors.onPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        // Ejecutar el script y mostrar el preview en background
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val result = executeJsScriptPreview(scriptContent)
                                previewResult = result
                                previewError = null
                            } catch (e: Exception) {
                                previewResult = null
                                previewError = e.message
                            }
                            showPreview = true
                        }
                    }) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Preview", tint = MaterialTheme.colors.onPrimary)
                    }
                    TextButton(
                        onClick = {
                            if (name.isNotBlank() && scriptContent.isNotBlank()) {
                                onSave(name, scriptContent)
                            } else {
                                showError = true
                            }
                        }
                    ) {
                        Text("GUARDAR", color = MaterialTheme.colors.onPrimary)
                    }
                },
                backgroundColor = MaterialTheme.colors.primary
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre del Script") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                singleLine = true
            )

            if (showError) {
                Text(
                    text = "El nombre y el contenido del script no pueden estar vacíos.",
                    color = MaterialTheme.colors.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            OutlinedTextField(
                value = scriptContent,
                onValueChange = { newScriptContent: String -> scriptContent = newScriptContent },
                label = { Text("Código JavaScript") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                textStyle = TextStyle(fontFamily = FontFamily.Monospace)
            )
        }

        if (showPreview) {
            PreviewWidgetDialog(
                scriptName = name,
                result = previewResult,
                error = previewError,
                onDismiss = { showPreview = false }
            )
        }
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
 * 
 * Ejemplo de script válido:
 * ```
 * ({
 *   title: "Mi Widget",
 *   body: "Contenido del widget",
 *   backgroundColor: "#263238",
 *   textColor: "#FFFFFF",
 *   textSize: 16,
 *   textAlign: "center"
 * })
 * ```
 */
fun executeJsScriptPreview(script: String): Scriptable? {
    try {
        if (!ContextFactory.hasExplicitGlobal()) {
            ContextFactory.initGlobal(AndroidContextFactory())
        }
        var lastResult: Any? = null
        var lastError: String? = null
        val result = ContextFactory.getGlobal().call { rhino ->
            rhino.optimizationLevel = -1
            val scope: Scriptable = rhino.initStandardObjects()
            // Inyectar variable para indicar que es preview
            scope.put("IS_PREVIEW", scope, true)
            // Inyectar función httpGet para peticiones HTTP desde JS puro
            scope.put("httpGet", scope, object : BaseFunction() {
                override fun call(cx: org.mozilla.javascript.Context?, scope: Scriptable?, thisObj: Scriptable?, args: Array<out Any>?): Any {
                    val urlString = args?.getOrNull(0)?.toString() ?: return "Error: url vacío"
                    return try {
                        val url = URL(urlString)
                        val connection = url.openConnection() as HttpURLConnection
                        connection.requestMethod = "GET"
                        connection.connectTimeout = 8000
                        connection.readTimeout = 8000
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
            
            try {
                lastResult = rhino.evaluateString(scope, script, "JSWidgetScriptPreview", 1, null)
                lastResult
            } catch (e: Exception) {
                lastError = e.message
                null
            }
        }
        if (result is Scriptable) {
            return result
        } else {
            if (lastError != null) throw Exception(lastError)
            throw Exception("El script debe retornar un objeto. Ejemplo:\n({ title: 'Mi Widget', body: 'Contenido' })")
        }
    } catch (e: Exception) {
        throw e
    }
}

@Composable
fun PreviewWidgetDialog(
    scriptName: String,
    result: Scriptable?,
    error: String?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Vista Previa del Widget") },
        text = {
            if (error != null) {
                Text("Error al ejecutar el script:\n$error")
            } else if (result != null) {
                val jsTitle = result.get("title", result)
                val jsBody = result.get("body", result)
                val jsBgColor = result.get("backgroundColor", result)
                val jsTextColor = result.get("textColor", result)
                val jsTextSize = result.get("textSize", result)
                val jsTextAlign = result.get("textAlign", result)

                val finalTitle = if (jsTitle != null && jsTitle != Scriptable.NOT_FOUND) jsTitle.toString() else scriptName
                val finalBody = if (jsBody != null && jsBody != Scriptable.NOT_FOUND) jsBody.toString() else "Contenido no disponible"
                val backgroundColor = try { Color(android.graphics.Color.parseColor(jsBgColor?.toString() ?: "#263238")) } catch (_: Exception) { Color(0xFF263238) }
                val textColor = try { Color(android.graphics.Color.parseColor(jsTextColor?.toString() ?: "#FFFFFF")) } catch (_: Exception) { Color.White }
                val textSize = (jsTextSize?.toString()?.toFloatOrNull() ?: 16f)
                val textAlign = when (jsTextAlign?.toString()?.lowercase()) {
                    "center" -> androidx.compose.ui.text.style.TextAlign.Center
                    "right" -> androidx.compose.ui.text.style.TextAlign.Right
                    else -> androidx.compose.ui.text.style.TextAlign.Left
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(backgroundColor, shape = RoundedCornerShape(16.dp))
                        .padding(20.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            finalTitle,
                            style = MaterialTheme.typography.h6.copy(
                                color = textColor,
                                fontSize = androidx.compose.ui.unit.TextUnit.Unspecified
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = textAlign
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            finalBody,
                            color = textColor,
                            fontSize = textSize.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = textAlign
                        )
                    }
                }
            } else {
                Text("No hay resultado para mostrar.")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
} 