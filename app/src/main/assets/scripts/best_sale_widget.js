// best_sale_widget.js
// Widget que muestra los mejores precios de USDT en diferentes exchanges
// 100% JavaScript puro usando httpGet

var TITLE = "Best Sale";
var TOP_COUNT = 3;
var ALLOWED_NAMES = ["Lemon", "Belo", "Buenbit", "Fiwind", "Binance"];

try {
    var response = httpGet("https://api.comparadolar.ar/crypto/usdt");
    if (response.startsWith("Error:")) throw new Error(response);
    var data = JSON.parse(response);
    var list = [];
    for (var k in data) {
        if (data.hasOwnProperty(k)) {
            var item = data[k];
            if (ALLOWED_NAMES.indexOf(item.prettyName) !== -1) {
                list.push(item);
            }
        }
    }
    if (!list.length) throw new Error("No se encontraron tus wallets en la API");
    // Ordenar por totalBid y tomar los top N
    list.sort(function(a, b) { return (b.totalBid || 0) - (a.totalBid || 0); });
    var topList = list.slice(0, TOP_COUNT);
    var body = topList.map(function(item, i) {
        return (i+1) + ". " + item.prettyName + ": $" + (item.totalBid || 0).toFixed(2);
    }).join("\n");
    ({
        title: TITLE,
        body: body,
        backgroundColor: "#1E2328",
        textColor: "#FFFFFF",
        textSize: 16,
        textAlign: "left"
    });
} catch (e) {
    ({
        title: TITLE,
        body: "Error: " + (e && e.message ? e.message.substring(0,80) : e),
        backgroundColor: "#1E2328",
        textColor: "#FF4444",
        textSize: 14,
        textAlign: "left"
    });
} 