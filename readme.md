Este **PRD (Product Requirements Document)** describe JSWidgets, una aplicación Android inspirada en Scriptable. El documento cubre:

1.  Visión del producto
2.  Objetivos
3.  Estado Actual del Proyecto
4.  Inspiración: Adaptando la Experiencia Scriptable a Android
5.  Características clave
6.  Arquitectura funcional
7.  Casos de uso
8.  Requisitos técnicos
9.  Fases del desarrollo
10. Consideraciones futuras (General)
11. User Flow (Flujo de Usuario Actualizado)

---

### **📄 PRD – JSWidgets for Android**

---

#### **🧭 1. Visión del producto**

Crear una aplicación para Android que permita importar y ejecutar scripts en JavaScript para generar contenido dinámico y personalizable en widgets, inspirado en la experiencia que ofrece Scriptable en iOS.
Esta herramienta busca empoderar a usuarios técnicos con una forma flexible de convertir código en widgets funcionales directamente desde su Android.

---

#### **🎯 2. Objetivos**

* Permitir a los usuarios crear widgets visuales y personalizables basados en lógica propia escrita en JavaScript.
* Ofrecer una sandbox de ejecución de JavaScript (actualmente con Rhino) con acceso controlado a APIs de Android y Java para funcionalidades extendidas (ej. peticiones de red).
* Facilitar la gestión, creación y edición de scripts directamente en la aplicación, además de permitir la importación de archivos `.js` desde el almacenamiento del dispositivo.
* Proporcionar una experiencia de usuario fluida para la configuración y visualización de widgets en el homescreen.
* (Futuro) Ofrecer un preview de ejecución en tiempo real dentro del editor para acelerar el ciclo de edición-prueba.

---

#### **✨ 3. Estado Actual del Proyecto**

* **Interfaz de Usuario Principal:**
  * Pantalla principal con listado de scripts de usuario y ejemplos.
  * Diseño de items de script con colores cíclicos, icono personalizable (seleccionable por el usuario) y menú de acciones (Editar, Renombrar, Borrar).
  * Diálogos de confirmación para borrado y renombrado.
  * Editor de scripts integrado con resaltado de sintaxis (monoespaciado) y selector de iconos.
  * Funcionalidad de búsqueda de scripts (placeholder).
* **Gestión de Scripts:**
  * Creación, edición, renombrado y borrado de scripts.
  * Los scripts de usuario se guardan en el almacenamiento externo.
  * Carga automática de scripts de ejemplo desde los `assets` de la aplicación si no hay scripts de usuario.
* **Motor de JavaScript:**
  * Uso de Mozilla Rhino para ejecutar scripts.
  * `ContextFactory` personalizado que permite a los scripts acceder a clases Java específicas (ej. `java.net.URL`, `org.json.JSONObject`) mediante `applicationClassLoader`.
* **Widgets:**
  * Proveedor de widgets (`JSWidgetProvider`) que ejecuta el script seleccionado.
  * Actividad de configuración (`WidgetConfigActivity`) que permite al usuario elegir qué script asociar a una instancia de widget.
  * Los scripts retornan un objeto JSON con propiedades para personalizar el widget, incluyendo:
    * `title` (String, opcional)
    * `body` (String, contenido principal)
    * `backgroundColor` (String, color hexadecimal ej. "#FFFFFF", opcional)
    * `textColor` (String, color hexadecimal, opcional)
    * `textSize` (Number, tamaño de fuente en sp, opcional)
    * `textAlign` (String, "left", "center", "right", opcional)
    * `refreshInterval` (Number, minutos, opcional, mínimo 15)
  * Manejo básico de errores en widgets (muestra "Contenido no disponible" o "Script no encontrado").
  * Actualización periódica de widgets.
* **Permisos:**
  * `READ_EXTERNAL_STORAGE` para cargar scripts de usuario.
  * `INTERNET` para scripts que necesiten acceso a la red (ej. widget de precio de BTC).
  * `WRITE_EXTERNAL_STORAGE` (con `maxSdkVersion="28"`) para guardar scripts de usuario.

---

#### **🌟 4. Inspiración: Adaptando la Experiencia Scriptable a Android**

JSWidgets se inspira en la flexibilidad y potencia de Scriptable en iOS, buscando ofrecer una experiencia comparable adaptada al ecosistema Android:

* **Scripts en JavaScript Puro (ES6 Soportado):** Al igual que Scriptable, JSWidgets utiliza archivos JavaScript (`.js`) estándar. El motor Rhino actual ofrece una amplia compatibilidad con ES6, permitiendo código moderno y legible. Los scripts se almacenan directamente en el sistema de archivos del dispositivo.
* **Integración con APIs Nativas de Android/Java:** Inspirado en el acceso nativo de Scriptable, JSWidgets busca ofrecer una integración controlada y segura con las APIs de Android y Java. Actualmente, esto permite a los scripts realizar operaciones como peticiones de red (`java.net.URL`) y manipulación de JSON (`org.json.JSONObject`). El objetivo es expandir estas capacidades para interactuar con más aspectos del sistema (sensores, archivos, calendario, etc.) de forma segura.
* **Automatización y Widgets Dinámicos (Equivalente Android a Atajos de Siri):** Mientras Scriptable se integra con Atajos de Siri, JSWidgets se enfoca en la creación de widgets de homescreen altamente personalizables. A futuro, se explorará la integración con herramientas de automatización de Android (ej. Tasker), App Shortcuts (atajos de aplicación) y Quick Settings Tiles (controles en panel de ajustes rápidos) para ejecutar scripts y presentar información de formas diversas y rápidas.
* **Documentación Clara y Accesible:** Un pilar de Scriptable es su documentación. JSWidgets aspira a proveer documentación detallada y accesible (idealmente offline) para todas las APIs nativas expuestas a JavaScript, facilitando la creación de scripts complejos.
* **Procesamiento de Datos desde Otras Apps (Equivalente Android a Share Sheet Extension):** (Futuro) Al igual que Scriptable puede procesar entradas desde la Share Sheet, se planea permitir que JSWidgets reciba datos de otras aplicaciones a través del sistema de "Compartir" de Android, permitiendo a los scripts actuar sobre texto, URLs, etc.
* **Integración con el Sistema de Archivos:** JSWidgets ya interactúa con el sistema de archivos para cargar y guardar scripts. Se contempla expandir las APIs para permitir a los scripts realizar operaciones de archivo más avanzadas directamente desde los mismos, similar a la integración de Files.app en Scriptable.
* **Editor Personalizable y Ejemplos:** JSWidgets incluye un editor de scripts y scripts de ejemplo para un inicio rápido. A futuro, se buscará que el editor sea más personalizable (temas, fuentes), siguiendo la línea de Scriptable.
* **Comunicación Inter-App (Intents de Android):** En Android, los Intents son el mecanismo principal para la comunicación entre apps. (Futuro) JSWidgets podría usar Intents para permitir que los scripts inicien acciones en otras apps o para que JSWidgets sea invocado y controlado por otras aplicaciones, de forma análoga a cómo Scriptable utiliza x-callback-url.

---

#### **🧩 5. Características clave**

##### **🗂️ Importador y Gestor de Scripts**

* Carga scripts `.js` desde el almacenamiento externo del dispositivo (directorio específico de la app y directorio público `Documents/JSWidgets`).
* Carga scripts de ejemplo empaquetados en los `assets`.
* Interfaz para listar, crear, editar (incluyendo icono), renombrar y eliminar scripts.
* Icono personalizable para cada script, visible en las listas y el editor.

##### **📜 Motor de Ejecución JS**

* Ejecuta scripts usando **Mozilla Rhino**.
* Contexto de ejecución configurado para permitir el uso de ciertas clases Java (ej. `java.net.URL`, `org.json.JSONObject`) para funcionalidades como peticiones HTTP y parseo de JSON.
* Los scripts son autocontenidos (no se soportan módulos `require()` al estilo Node.js).

##### **🧱 Widgets Conectados a Scripts**

* Cada instancia de widget en el homescreen está vinculada a un script JS específico seleccionado por el usuario.
* El script retorna un objeto JSON cuyas propiedades definen el contenido y apariencia del widget. Ejemplo de estructura retornada:

```javascript
return {
  title: "Bitcoin Price",
  body: "USD " + btcPrice,
  backgroundColor: "#263238",
  textColor: "#FFFFFF",
  textSize: 18,
  textAlign: "center",
  refreshInterval: 30 // minutos
};
```

##### **🖼️ Renderizado del Widget**

* El widget renderiza el contenido (título y cuerpo) basándose en las propiedades devueltas por el script.
* Soporta personalización básica de:
  * Color de fondo del widget.
  * Color del texto.
  * Tamaño del texto.
  * Alineación del texto.
* Diseño base con esquinas redondeadas.

##### **🔁 Actualización de Widget**

* Los widgets se actualizan periódicamente según el `refreshInterval` definido en el script (mínimo 15 minutos debido a restricciones de Android).
* Actualización al reconfigurar o añadir el widget.
* (Futuro) Posibilidad de actualización manual desde la app principal.

---

#### **📚 6. Arquitectura funcional**

**App (JSWidgets):**

* Administra los scripts (CRUD).
* Proporciona un editor de texto para los scripts.
* Lanza la actividad de configuración de widgets.
* (Futuro) Previsualiza la ejecución del script.

**Motor JS (Rhino integrado):**

* Evalúa el código JavaScript del script seleccionado.
* Proporciona un entorno de ejecución con acceso limitado y controlado a funcionalidades del sistema/Java.
* Devuelve el objeto JSON resultante de la ejecución del script.

**WidgetProvider (`JSWidgetProvider`):**

* Asociado con las instancias de widgets en el homescreen.
* Recupera el script asociado a un widget (vía `SharedPreferences` pobladas por `WidgetConfigActivity`).
* Invoca al motor JS para ejecutar el script.
* Renderiza el widget (actualiza las `RemoteViews`) con la información obtenida del script.
* Maneja los ciclos de actualización del widget (`onUpdate`, `onDeleted`).
* Gestiona la persistencia de qué script está asociado a qué `appWidgetId`.

**Actividad de Configuración (`WidgetConfigActivity`):**

* Se lanza cuando el usuario añade un nuevo widget al homescreen.
* Muestra la lista de scripts disponibles (de usuario y ejemplos).
* Permite al usuario seleccionar un script para la instancia del widget.
* Guarda la asociación script-widgetId y fuerza la primera actualización del widget.

---

#### **👤 7. Casos de uso**

1. **Crear un nuevo script:**
   * El usuario abre la app JSWidgets.
   * Pulsa el botón "+" para crear un nuevo script.
   * Ingresa un nombre para el script y escribe/pega el código JavaScript en el editor.
   * Guarda el script.
2. **Añadir un widget al homescreen:**
   * El usuario añade un widget de JSWidgets desde el selector de widgets de Android.
   * Se abre la `WidgetConfigActivity` mostrando la lista de scripts.
   * El usuario selecciona el script deseado.
   * El widget aparece en el homescreen mostrando el contenido generado por el script.
3. **Editar un script existente:**
   * El usuario abre la app y selecciona un script de la lista.
   * Modifica el código en el editor y guarda los cambios.
   * Los widgets que usan ese script se actualizarán con la nueva lógica en su próximo ciclo de actualización.
4. **Usar un script con acceso a internet (ej. precio de BTC):**
   * El usuario selecciona o crea un script que realiza una petición HTTP (ej. a CoinGecko).
   * El script utiliza `java.net.URL` para la petición y `org.json.JSONObject` para parsear la respuesta.
   * El widget muestra el precio de Bitcoin actualizado.
5. (Futuro) El usuario descarga un script `.js` de una fuente externa, lo guarda en su dispositivo. La app (mediante una función de importación o detectándolo en una carpeta específica) lo añade a su lista de scripts disponibles.

---

#### **⚙️ 8. Requisitos técnicos**

* **Lenguaje base:** Kotlin.
* **Motor JS:** Mozilla Rhino.
* **Compatibilidad mínima:** Android 8.0+ (API nivel 26+).
* **Permisos requeridos:**
  * `android.permission.INTERNET` (para scripts que acceden a la red).
  * `android.permission.READ_EXTERNAL_STORAGE` (para cargar scripts de usuario).
  * `android.permission.WRITE_EXTERNAL_STORAGE` (con `android:maxSdkVersion="28"`, para guardar scripts de usuario en versiones antiguas de Android; en versiones nuevas se usa el almacenamiento específico de la app que no requiere este permiso explícito de la misma manera).
* **Restricciones y Consideraciones de Seguridad:**
  * Los scripts se ejecutan en un entorno con acceso limitado.
  * Se debe tener cuidado con los scripts de fuentes no confiables.
  * (Futuro) Implementar timeouts más estrictos para la ejecución de scripts para evitar bloqueos del widget o consumo excesivo de batería.
  * Manejo de errores: los scripts con errores de sintaxis o excepciones durante la ejecución actualmente muestran un mensaje genérico en el widget ("Contenido no disponible"). Esto debe mejorarse para ofrecer feedback más específico.

---

#### **🗺️ 9. Fases del desarrollo**

##### **📦 Fase 1 (MVP - Mayormente Completada)**

* Ejecutar script JS desde archivo (assets y almacenamiento externo, incluyendo `Documents/JSWidgets`). ✔️
* Retornar una estructura JSON con datos para el widget. ✔️
* Mostrar contenido y aplicar estilos básicos (texto, color de fondo/texto, tamaño, alineación) en el widget. ✔️
* Actualización de widgets por intervalo y al configurar. ✔️
* Editor visual interno para scripts, con selector de iconos. ✔️
* Actividad de configuración para seleccionar script al añadir widget (mostrando iconos y colores). ✔️
* Funcionalidades básicas de gestión de scripts (crear, listar, editar [incluyendo selección de icono], renombrar, borrar). ✔️
* Permisos de internet y almacenamiento. ✔️
* Acceso a APIs Java limitadas desde JS (URL, JSONObject). ✔️
* Visualización de iconos personalizados en las listas de scripts. ✔️

##### **🛠️ Fase 2 (En progreso y Próximos Pasos)**

* **Mejoras en el Editor:**
  * Resaltado de sintaxis más avanzado y específico para el contexto JSWidgets.
  * Sugerencias/autocompletado para APIs expuestas.
  * Preview en tiempo real del widget dentro del editor.
* **Expansión del API JS:**
  * Acceso a más información del dispositivo (ej. nivel de batería, estado de red, info de calendario básica).
  * Funciones de utilidad (ej. formateo de fechas/horas).
  * (Considerar) API para mostrar imágenes simples o iconos en el widget.
* **Mejoras en UI/UX de la App Principal:**
  * Implementar funcionalidad de búsqueda de scripts.
  * Interfaz para gestionar ajustes de la app (ej. carpeta de scripts por defecto).
  * Posibilidad de duplicar scripts.
  * Importación explícita de archivos `.js` desde el explorador de archivos.
* **Mejoras en Widgets:**
  * Renderizado más rico (si es posible sin afectar demasiado el rendimiento/batería).
  * Mejor feedback de errores directamente en el widget.
  * Interacción con el widget (ej. `onTap` para abrir una URL o refrescar).
* **Estabilidad y Rendimiento:**
  * Optimización de la ejecución de Rhino.
  * Manejo robusto de errores y excepciones en scripts y en la app.

##### **🚀 Fase 3 (Futuro Lejano)**

* Galería/Repositorio de scripts (online o curado).
* Compartir scripts entre usuarios.
* Disparadores de actualización de widgets más avanzados (ej. por evento del sistema, ubicación).
* Soporte para múltiples tamaños de widgets o layouts dinámicos.
* (Evaluar) Alternativas a Rhino si se encuentran limitaciones significativas (ej. QuickJS, Duktape).

---

#### **🧠 10. Consideraciones futuras (General)**

* **Seguridad:** Fortalecer la sandbox de ejecución de scripts. Validación y saneamiento de lo que los scripts pueden hacer.
* **Internacionalización (i18n) y Localización (l10n)** de la aplicación.
* **Backup y Restauración** de scripts (local o en la nube).
* **Documentación exhaustiva** para desarrolladores de scripts (API disponible, ejemplos, buenas prácticas).
* **Monetización:** Si el proyecto crece, explorar opciones (ej. versión Pro con características avanzadas, galería premium de scripts).

---

#### **🧑‍💻 11. User Flow (Flujo de Usuario Actualizado)**

1. **Abrir la App:** El usuario abre JSWidgets. Se muestra la `MainActivity` con una lista de sus scripts y los ejemplos.
2. **Crear Script:**
   * El usuario pulsa el icono "+" en la `TopAppBar`.
   * Se navega a `WidgetEditorScreen` para un nuevo script.
   * El usuario introduce el nombre y el código JavaScript.
   * Pulsa "GUARDAR". El script se guarda en el almacenamiento y se actualiza la lista.
3. **Editar Script:**
   * En la lista, el usuario pulsa sobre un script o selecciona "Editar" en su menú contextual.
   * Se navega a `WidgetEditorScreen` con los datos del script cargados.
   * Modifica el nombre o el contenido.
   * Pulsa "GUARDAR".
4. **Renombrar/Borrar Script:**
   * El usuario usa las opciones "Renombrar" o "Borrar" del menú contextual de un script.
   * Aparecen diálogos de confirmación/entrada. La acción se refleja en el almacenamiento y en la UI.
5. **Añadir Widget al Homescreen:**
   * El usuario realiza una pulsación larga en su homescreen y selecciona "Widgets".
   * Busca y selecciona el widget de "JSWidgets".
   * Se lanza `WidgetConfigActivity`, mostrando la lista de todos los scripts disponibles.
   * El usuario selecciona el script que desea asociar a esta instancia del widget.
   * El widget se añade al homescreen y ejecuta el script seleccionado para mostrar su contenido inicial.
6. **Actualización del Widget:**
   * El widget se actualiza automáticamente según el `refreshInterval` (o el valor por defecto de Android) o cuando su configuración cambia.
   * Los cambios en un script desde la app principal se reflejarán en las instancias de widget asociadas en su próximo ciclo de actualización.

---

Este documento sirve como una guía viva para el desarrollo de JSWidgets. Se actualizará a medida que el proyecto evolucione.
