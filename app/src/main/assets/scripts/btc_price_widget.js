// btc_price_widget.js
(function() {
    var title = "Precio BTC";
    var body = "Cargando..."; // Valor inicial mientras se obtiene el precio

    try {
        // Hacemos la petición HTTP usando clases Java ya que Rhino no tiene fetch/XHR nativo
        var url = new java.net.URL("https://api.coingecko.com/api/v3/simple/price?ids=bitcoin&vs_currencies=usd");
        var connection = url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(8000); // Timeout de conexión 8 segundos
        connection.setReadTimeout(8000);    // Timeout de lectura 8 segundos
        
        // Rhino en Android generalmente permite el uso de org.json
        // por lo que no necesitamos un import explícito aquí, se resuelve en el contexto de ejecución.
        // Es importante notar que si org.json no estuviera disponible, este script fallaría.

        var responseCode = connection.getResponseCode();

        if (responseCode == 200) { // HTTP OK
            var inputStream = connection.getInputStream();
            var reader = new java.io.BufferedReader(new java.io.InputStreamReader(inputStream, "UTF-8"));
            var line;
            var responseText = new java.lang.StringBuilder();
            while ((line = reader.readLine()) != null) {
                responseText.append(line);
            }
            reader.close();
            inputStream.close();
            
            // Parsear el JSON. Rhino no tiene JSON.parse() por defecto.
            // Se espera que el entorno Android provea org.json.JSONObject.
            var json = new org.json.JSONObject(responseText.toString());
            if (json.has("bitcoin") && json.getJSONObject("bitcoin").has("usd")) {
                var price = json.getJSONObject("bitcoin").getDouble("usd");
                // Formatear el precio a dos decimales.
                // Rhino podría no tener toFixed() en Number.prototype, así que usamos String.format de Java.
                body = java.lang.String.format("USD %.2f", price);
            } else {
                body = "Respuesta inválida de API";
            }
        } else {
            body = "Error API: " + responseCode;
        }
        connection.disconnect();

    } catch (e) {
        // e puede ser una instancia de org.mozilla.javascript.JavaScriptException
        // o una excepción de Java directamente.
        var errorMessage = "Error script: ";
        if (e.getMessage) { // Para excepciones de Java
            errorMessage += e.getMessage();
        } else if (e.message) { // Para JavaScriptException
             errorMessage += e.message;
        } else {
            errorMessage += "Desconocido";
        }
        body = errorMessage.substring(0, Math.min(errorMessage.length(), 50)); // Limitar longitud
    }

    return {
        title: title,
        body: body
    };
})(); 