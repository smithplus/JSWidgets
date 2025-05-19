// btc_price_widget.js
// Widget que muestra el precio actual de Bitcoin en USD
// 100% JavaScript puro usando httpGet

try {
    var response = httpGet("https://api.coingecko.com/api/v3/simple/price?ids=bitcoin&vs_currencies=usd");
    if (response.startsWith("Error:")) throw new Error(response);
    var data = JSON.parse(response);
    var price = data.bitcoin && data.bitcoin.usd ? data.bitcoin.usd : null;
    if (!price) throw new Error("Datos de precio no disponibles");
    ({
        title: "Precio BTC",
        body: "USD " + price.toFixed(2),
        backgroundColor: "#1E2328",
        textColor: "#FFFFFF",
        textSize: 16,
        textAlign: "center"
    });
} catch (e) {
    ({
        title: "Precio BTC",
        body: "Error: " + (e && e.message ? e.message.substring(0,50) : e),
        backgroundColor: "#1E2328",
        textColor: "#FF4444",
        textSize: 14,
        textAlign: "center"
    });
} 