// Ejemplo de widget usando JavaScript puro
// Este script muestra cómo crear un widget con diferentes estilos

// Función para obtener un color aleatorio en formato hex
function getRandomColor() {
    const letters = '0123456789ABCDEF';
    let color = '#';
    for (let i = 0; i < 6; i++) {
        color += letters[Math.floor(Math.random() * 16)];
    }
    return color;
}

// Función para obtener la hora actual formateada
function getCurrentTime() {
    const now = new Date();
    return now.toLocaleTimeString();
}

// Retornamos un objeto con la configuración del widget
({
    // Título del widget
    title: "Mi Widget Dinámico",
    
    // Cuerpo del widget con la hora actual
    body: `La hora actual es: ${getCurrentTime()}`,
    
    // Color de fondo aleatorio
    backgroundColor: getRandomColor(),
    
    // Color del texto (blanco o negro según el fondo)
    textColor: "#FFFFFF",
    
    // Tamaño del texto
    textSize: 16,
    
    // Alineación del texto
    textAlign: "center"
}) 