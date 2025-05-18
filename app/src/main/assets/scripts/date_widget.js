// date_widget.js
return {
    title: "Fecha Actual",
    body: new Date().toLocaleDateString('es-ES', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })
}; 