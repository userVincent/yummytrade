import { getAuth, onAuthStateChanged } from "https://www.gstatic.com/firebasejs/9.9.4/firebase-auth.js";
import { auth } from './firebase-setup.js';

let tickBuffer = []; // Buffer to store the latest ticks for calculating the current candle
const CANDLE_INTERVAL = 10; // Interval for candle calculation in seconds
let allCandles = []; // Array to store all candles
let firstTimeFetchingNewTicks = true;

// Fetch index fund details using the symbol from URL parameters
function fetchIndexFundDetails(symbol) {
    fetch(`/indexFunds/ticks/${symbol}`)
        .then((response) => response.json())
        .then((ticks) => {
            tickBuffer = ticks.slice(findLastIntervalStartIndex(ticks, CANDLE_INTERVAL), 1000);
            allCandles = calculateCandles(ticks, CANDLE_INTERVAL);
            displayFundChart(symbol, allCandles);
            fetchNewTicks(symbol); // Start fetching new ticks every second

            // Fetch additional fund information
            setInterval(() => {
                fetch(`/indexFunds/${symbol}`)
                    .then((response) => response.json())
                    .then((fund) => {
                        displayFundInfo(fund);
                        fetchComponentStockDetails(fund.componentStocks, fund.exchanges);
                    })
                    .catch((error) => {
                        console.log("Error fetching fund details:", error);
                    });
            }, 1000);
        })
        .catch((error) => {
            console.log("Error fetching ticks:", error);
        });
}

// Fetch new ticks and update the chart every second
function fetchNewTicks(symbol) {
    setInterval(() => {
        fetch(`/indexFunds/ticks/${symbol}`)
            .then((response) => response.json())
            .then((newTicks) => {
                const prevTickBuffer = tickBuffer;
                tickBuffer = newTicks.slice(findLastIntervalStartIndex(newTicks, CANDLE_INTERVAL), 1000);
                const currentCandle = calculateCurrentCandle(tickBuffer);

                if (tickBuffer.length === 1) {
                    const combinedBuffer = prevTickBuffer.concat(tickBuffer);
                    const combinedCandle = calculateCurrentCandle(combinedBuffer);
                    allCandles[allCandles.length - 1] = combinedCandle;
                    allCandles.push(currentCandle);
                    allCandles.shift();
                }

                if (firstTimeFetchingNewTicks) {
                    allCandles.push(currentCandle);
                    firstTimeFetchingNewTicks = false;
                }
                allCandles[allCandles.length - 1] = currentCandle;
                updateChart(symbol, allCandles);
            })
            .catch((error) => {
                console.log("Error fetching new ticks:", error);
            });
    }, 1000);
}

// Calculate candles from ticks for the given interval (in seconds)
function calculateCandles(ticks, interval) {
    const candles = [];
    let startIndex = findFirstIntervalStartIndex(ticks, interval);
    let endIndex = startIndex + interval;

    while (endIndex <= ticks.length - 1) {
        const candleTicks = ticks.slice(startIndex, endIndex + 1);
        const candle = createCandle(candleTicks);
        candles.push(candle);
        startIndex = endIndex;
        endIndex = startIndex + interval;
    }

    return candles;
}

// Find the starting index for the first interval
function findFirstIntervalStartIndex(ticks, interval) {
    const firstTickTime = new Date(ticks[0].timestamp);
    const firstTickSeconds = firstTickTime.getSeconds();
    const offset = interval - (firstTickSeconds % interval);
    return Math.min(offset, ticks.length - interval);
}

// Find the starting index for the last interval
function findLastIntervalStartIndex(ticks, interval) {
    for (let i = ticks.length - 1; i >= 0; i--) {
        const seconds = ticks[i].timestamp.slice(-2);
        if (seconds % interval === 0) {
            return i;
        }
    }
}

// Create a candle from a set of ticks
function createCandle(tickBuffer) {
    const candle = { open: null, high: null, low: null, close: null, volume: 0, time: null };

    tickBuffer.forEach((tick, index) => {
        const tickTime = new Date(tick.timestamp);

        if (candle.open === null) {
            candle.open = tick.price;
            candle.high = tick.price;
            candle.low = tick.price;
            candle.time = tickTime;
        }

        candle.high = Math.max(candle.high, tick.price);
        candle.low = Math.min(candle.low, tick.price);
        candle.close = tick.price;
        candle.volume += tick.volume;
    });

    return {
        x: candle.time,
        open: candle.open,
        high: candle.high,
        low: candle.low,
        close: candle.close,
        volume: candle.volume
    };
}

// Calculate the current candle from the tick buffer
function calculateCurrentCandle(tickBuffer) {
    return createCandle(tickBuffer);
}

// Display fund price chart
function displayFundChart(symbol, candles) {
    const data = [{
        x: candles.map(c => c.x),
        close: candles.map(c => c.close),
        decreasing: { line: { color: '#d9534f' } }, // Red for decreasing
        high: candles.map(c => c.high),
        increasing: { line: { color: '#5cb85c' } }, // Green for increasing
        line: { color: '#337ab7' },
        low: candles.map(c => c.low),
        open: candles.map(c => c.open),
        type: 'candlestick',
        xaxis: 'x',
        yaxis: 'y'
    }];

    const layout = {
        title: `${symbol} Fund Price`,
        dragmode: 'zoom',
        showlegend: false,
        plot_bgcolor: "#ffffff",  // White background color for the plot area
        paper_bgcolor: "#ffffff", // White background color for the entire chart
        font: {
            color: '#333333'  // Dark text color
        },
        xaxis: {
            rangeslider: {
                visible: false
            },
            color: '#333333', // Dark color for x-axis text and lines
            gridcolor: '#cccccc', // Lighter grid lines
            zerolinecolor: '#cccccc', // Lighter zero line
        },
        yaxis: {
            autorange: true,
            fixedrange: false,
            color: '#333333', // Dark color for y-axis text and lines
            gridcolor: '#cccccc', // Lighter grid lines
            zerolinecolor: '#cccccc' // Lighter zero line
        }
    };

    Plotly.newPlot('fundChart', data, layout);
}

// Update the chart with the current candle
function updateChart(symbol, candles) {
    const update = {
        x: [candles.map(c => c.x)],
        open: [candles.map(c => c.open)],
        high: [candles.map(c => c.high)],
        low: [candles.map(c => c.low)],
        close: [candles.map(c => c.close)]
    };

    Plotly.react('fundChart', [{
        x: candles.map(c => c.x),
        close: candles.map(c => c.close),
        decreasing: { line: { color: '#d9534f' } }, // Red for decreasing
        high: candles.map(c => c.high),
        increasing: { line: { color: '#5cb85c' } }, // Green for increasing
        line: { color: '#337ab7' },
        low: candles.map(c => c.low),
        open: candles.map(c => c.open),
        type: 'candlestick',
        xaxis: 'x',
        yaxis: 'y'
    }], {
        title: `${symbol} Fund Price`,
        dragmode: 'zoom',
        showlegend: false,
        plot_bgcolor: "#ffffff",  // White background color for the plot area
        paper_bgcolor: "#ffffff", // White background color for the entire chart
        font: {
            color: '#333333'  // Dark text color
        },
        xaxis: {
            rangeslider: {
                visible: false
            },
            color: '#333333', // Dark color for x-axis text and lines
            gridcolor: '#cccccc', // Lighter grid lines
            zerolinecolor: '#cccccc', // Lighter zero line
        },
        yaxis: {
            autorange: true,
            fixedrange: false,
            color: '#333333', // Dark color for y-axis text and lines
            gridcolor: '#cccccc', // Lighter grid lines
            zerolinecolor: '#cccccc' // Lighter zero line
        }
    });
}

// Display fund information
function displayFundInfo(fund) {
    document.getElementById('fundName').innerText = fund.name;
    document.getElementById('currentPrice').innerText = fund.currentPrice.toFixed(2);
    const percentagePriceChange = document.getElementById('percentagePriceChange');
    percentagePriceChange.innerText = fund.percentagePriceChange.toFixed(2);
    percentagePriceChange.className = fund.percentagePriceChange >= 0 ? 'positive' : 'negative';
    document.getElementById('fundSymbol').value = fund.symbol;
    document.getElementById('fundTitle').innerText = fund.symbol;
}

// Fetch component stock details from NYSE and NASDAQ
function fetchComponentStockDetails(stocks, exchanges) {
    fetch(`/NYSEstocks`)
        .then(response => response.json())
        .then(nyseStocks => {
            fetch(`/NASDAQstocks`)
                .then(response => response.json())
                .then(nasdaqStocks => {
                    displayComponentStockDetails(stocks, exchanges, nyseStocks, nasdaqStocks);
                });
        })
        .catch((error) => {
            console.log("Error fetching component stock details:", error);
        });
}

// Display component stock details
function displayComponentStockDetails(stocks, exchanges, nyseStocks, nasdaqStocks) {
    const componentStocksDetails = document.getElementById('componentStocksDetails');
    componentStocksDetails.innerHTML = ''; // Clear existing content

    stocks.forEach((stock, index) => {
        let stockDetails = nyseStocks.find(s => s.symbol === stock) || nasdaqStocks.find(s => s.symbol === stock);
        let exchange = exchanges[index];

        if (stockDetails) {
            const stockDetailDiv = document.createElement('div');
            stockDetailDiv.className = 'stock-detail';
            stockDetailDiv.onclick = () => {
                loadStockDetailPage(stockDetails.symbol, exchange);
            };
            stockDetailDiv.innerHTML = `
                <div class="stock-symbol">${stockDetails.symbol}</div>
                <div class="stock-exchange">${exchange}</div>
                <div class="stock-name">${stockDetails.name}</div>
                <div class="stock-price">$${stockDetails.currentPrice.toFixed(2)}</div>
                <div class="stock-change ${stockDetails.percentagePriceChange >= 0 ? 'positive' : 'negative'}">${stockDetails.percentagePriceChange.toFixed(2)}%</div>
            `;
            componentStocksDetails.appendChild(stockDetailDiv);
        }
    });
}

// Load stock detail page and pass symbol and exchange via URL parameters
function loadStockDetailPage(symbol, exchange) {
    window.location.href = `stock-detail.html?symbol=${symbol}&exchange=${exchange}`;
}

// Handle buy/sell actions
function handleTrade(action) {
    const user = auth.currentUser;
    if (!user) {
        alert("You need to be logged in to trade.");
        return;
    }

    const symbol = document.getElementById('fundSymbol').value;
    const quantity = document.getElementById('quantity').value;

    if (!symbol || !quantity || quantity <= 0) {
        alert("Please enter a valid fund symbol and quantity.");
        return;
    }

    const endpoint = action === 'buy' ? 'buy' : 'sell';
    const url = new URL(`/api/indexFund/${endpoint}/${symbol}`, window.location.origin);
    url.searchParams.append('amount', quantity);

    user.getIdToken().then((token) => {
        fetch(url, {
            headers: { 'Authorization': `Bearer ` + token }
        })
            .then(response => response.json())
            .then(data => {
                if (data.error) {
                    throw new Error(data.error);
                }
                alert(`${action.charAt(0).toUpperCase() + action.slice(1)} order placed successfully!`);
                console.log(data);
            })
            .catch(error => {
                alert(`Error placing ${action} order: ${error.message}`);
                console.error(error);
            });
    });
}

// Extract symbol from URL parameters and fetch index fund details
function initIndexFundDetailPage() {
    const urlParams = new URLSearchParams(window.location.search);
    const symbol = urlParams.get('symbol');
    document.getElementById('fundSymbol').value = symbol;

    // Fetch index fund details initially with default time period
    fetchIndexFundDetails(symbol);

    // Add event listeners for buy and sell buttons
    document.getElementById('buyButton').addEventListener('click', () => handleTrade('buy'));
    document.getElementById('sellButton').addEventListener('click', () => handleTrade('sell'));
}

initIndexFundDetailPage();
