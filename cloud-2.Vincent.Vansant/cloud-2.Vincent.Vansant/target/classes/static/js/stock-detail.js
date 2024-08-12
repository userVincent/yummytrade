import { getAuth, onAuthStateChanged } from "https://www.gstatic.com/firebasejs/9.9.4/firebase-auth.js";
import { auth } from './firebase-setup.js';

let chartInstance = null; // Variable to keep track of the current chart instance
let tickBuffer = []; // Buffer to store the latest ticks for calculating the current candle
const CANDLE_INTERVAL = 10; // Interval for candle calculation in seconds
let allCandles = []; // Array to store all candles
let currentCandle = null;
let stompClient = null;

// Fetch stock details using the symbol from URL parameters
function fetchStockDetails(symbol, exchange) {
    fetch(`/${exchange}stocks/ticks/${symbol}`)
        .then((response) => response.json())
        .then((ticks) => {
            const lastTickTime = new Date(ticks[ticks.length - 1].timestamp);
            const lastTickSeconds = lastTickTime.getSeconds();
            const roundInterval = lastTickSeconds - (lastTickSeconds % CANDLE_INTERVAL);
            const startIndex = ticks.findIndex(tick => new Date(tick.timestamp).getSeconds() === roundInterval);

            if (startIndex === -1) {
                tickBuffer = ticks.slice(-CANDLE_INTERVAL);
            } else {
                tickBuffer = ticks.slice(startIndex);
                if (tickBuffer.length < CANDLE_INTERVAL) {
                    tickBuffer = ticks.slice(startIndex - (CANDLE_INTERVAL - tickBuffer.length), startIndex).concat(tickBuffer);
                }
            }

            allCandles = calculateCandles(ticks, CANDLE_INTERVAL);
            currentCandle = allCandles.pop(); // Remove and save the last candle if it's not complete
            displayStockChart(symbol, allCandles);
            connectToWebSocket(symbol); // Connect to WebSocket for live updates
        })
        .catch((error) => {
            console.log("Error fetching stock details:", error);
        });
}

// Connect to WebSocket endpoint and subscribe to tick updates
function connectToWebSocket(symbol) {
    var socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);

    stompClient.connect({}, function (frame) {
        console.log('Connected: ' + frame);
        stompClient.subscribe('/topic/ticks/' + symbol, function (message) {
            const tick = JSON.parse(message.body);
            processTick(tick);
        });
    }, function (error) {
        console.error('WebSocket connection error:', error);
    });
}

// Process incoming tick data
function processTick(tick) {
    const tickTime = new Date(tick.timestamp);
    const tickSeconds = tickTime.getSeconds();

    if (currentCandle && tickTime.getTime() - currentCandle.x.getTime() < CANDLE_INTERVAL * 1000) {
        // Add tick to current candle
        updateCurrentCandle(tick);
    } else {
        // Update the previous candle's closing price to match the new opening price
        if (currentCandle) {
            currentCandle.close = tick.price;
            allCandles.push(currentCandle);
        }

        // Create a new candle
        const newCandle = createCandle([tick]);

        // Add new candle to allCandles
        allCandles.push(newCandle);

        // Remove the first element if the allCandles array exceeds the desired length
        if (allCandles.length > CANDLE_INTERVAL) {
            allCandles.shift();
        }

        currentCandle = newCandle;
    }

    updateChart(tick.symbol, allCandles);
}

// Update the current candle with the latest tick data
function updateCurrentCandle(tick) {
    currentCandle.high = Math.max(currentCandle.high, tick.price);
    currentCandle.low = Math.min(currentCandle.low, tick.price);
    currentCandle.close = tick.price;
    currentCandle.volume += tick.volume; // Assuming tick contains a volume field
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
        candle.volume += tick.volume; // Assuming tick contains a volume field
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

// Display stock price chart
function displayStockChart(symbol, candles) {
    const data = [{
        x: candles.map(c => c.x),
        close: candles.map(c => c.close),
        decreasing: {line: {color: 'red'}},
        high: candles.map(c => c.high),
        increasing: {line: {color: 'green'}},
        line: {color: 'rgba(31,119,180,1)'},
        low: candles.map(c => c.low),
        open: candles.map(c => c.open),
        type: 'candlestick',
        xaxis: 'x',
        yaxis: 'y'
    }];

    const layout = {
        title: `${symbol} Stock Price`,
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

    Plotly.newPlot('chartContainer', data, layout);
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

    Plotly.react('chartContainer', [{
        x: candles.map(c => c.x),
        close: candles.map(c => c.close),
        decreasing: {line: {color: 'red'}},
        high: candles.map(c => c.high),
        increasing: {line: {color: 'green'}},
        line: {color: 'rgba(31,119,180,1)'},
        low: candles.map(c => c.low),
        open: candles.map(c => c.open),
        type: 'candlestick',
        xaxis: 'x',
        yaxis: 'y'
    }], {
        title: `${symbol} Stock Price`,
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
            zerolinecolor: '#cccccc' // Lighter zero line
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

// Handle buy/sell actions
function handleTrade(action, exchange) {
    const user = auth.currentUser;
    if (!user) {
        alert("You need to be logged in to trade.");
        return;
    }

    const symbol = document.getElementById('stockSymbol').value;
    const quantity = document.getElementById('quantity').value;
    const price = document.getElementById('strikePrice').value;

    if (!symbol || !quantity || quantity <= 0 || price < 0) {
        alert("Please enter a valid stock symbol and quantity.");
        return;
    }

    const endpoint = action === 'buy' ? 'buy' : 'sell';
    const url = new URL(`/api/${exchange}/${endpoint}/${symbol}`, window.location.origin);
    url.searchParams.append('amount', quantity);
    if (price) {
        url.searchParams.append('price', price);
    }

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

// Extract symbol from URL parameters and fetch stock details
function initStockDetailPage() {
    const urlParams = new URLSearchParams(window.location.search);
    const symbol = urlParams.get('symbol');
    const exchange = urlParams.get('exchange');
    document.getElementById('stockSymbol').value = symbol;
    document.getElementById('tradeTitle').innerText = `Buy/Sell ${symbol}`;

    // Fetch stock details initially
    fetchStockDetails(symbol, exchange);

    // Add event listeners for buy and sell buttons
    document.getElementById('buyButton').addEventListener('click', () => handleTrade('buy', exchange));
    document.getElementById('sellButton').addEventListener('click', () => handleTrade('sell', exchange));
}

initStockDetailPage();
