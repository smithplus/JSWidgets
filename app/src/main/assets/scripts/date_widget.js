// date_widget.js
({
    title: "Fecha Actual",
    body: new Date().toLocaleDateString('es-ES', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })
}) 