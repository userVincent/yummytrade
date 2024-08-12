import { auth } from './firebase-setup.js';
import { getAuth, onAuthStateChanged } from "https://www.gstatic.com/firebasejs/9.9.4/firebase-auth.js";

// Fetch stocks using the token from URL parameters
function fetchNYSEStocks() {
  fetch('/NYSEstocks')
    .then((response) => {
      return response.json();
    })
    .then((stocks) => {
      displayNYSEStocks(stocks);
    })
    .catch(function (error) {
      console.log(error);
    });
}

function fetchNASDAQStocks() {
  fetch('/NASDAQstocks')
    .then((response) => {
      return response.json();
    })
    .then((stocks) => {
      displayNASDAQStocks(stocks);
    })
    .catch(function (error) {
      console.log(error);
    });
}

function fetchIndexFunds() {
  fetch('/indexFunds')
    .then((response) => {
      return response.json();
    })
    .then((funds) => {
      displayIndexFunds(funds);
    })
    .catch(function (error) {
      console.log(error);
    });
}

// Display stocks on the home page
function displayNYSEStocks(stocks) {
  const stocksList = document.getElementById("NYSEstocksList");
  stocksList.innerHTML = ''; // Clear the list before updating
  stocks.forEach(stock => {
    const percentageChange = stock.percentagePriceChange;
    const stockRow = document.createElement('div');
    stockRow.className = 'stock-row';
    stockRow.onclick = () => {
      loadStockDetailPage(stock.symbol, "NYSE");
    };
    stockRow.innerHTML = `
      <div class="stock-symbol">${stock.symbol}</div>
      <div class="stock-name">${stock.name}</div>
      <div class="stock-price">$${stock.currentPrice.toFixed(2)}</div>
      <div class="stock-change ${percentageChange >= 0 ? 'positive' : 'negative'}">${percentageChange.toFixed(2)}%</div>
    `;
    stocksList.appendChild(stockRow);
  });
}

function displayNASDAQStocks(stocks) {
  const stocksList = document.getElementById("NASDAQstocksList");
  stocksList.innerHTML = ''; // Clear the list before updating
  stocks.forEach(stock => {
    const percentageChange = stock.percentagePriceChange;
    const stockRow = document.createElement('div');
    stockRow.className = 'stock-row';
    stockRow.onclick = () => {
      loadStockDetailPage(stock.symbol, "NASDAQ");
    };
    stockRow.innerHTML = `
      <div class="stock-symbol">${stock.symbol}</div>
      <div class="stock-name">${stock.name}</div>
      <div class="stock-price">$${stock.currentPrice.toFixed(2)}</div>
      <div class="stock-change ${percentageChange >= 0 ? 'positive' : 'negative'}">${percentageChange.toFixed(2)}%</div>
    `;
    stocksList.appendChild(stockRow);
  });
}

function displayIndexFunds(funds) {
  const fundsList = document.getElementById("indexFundsList");
  fundsList.innerHTML = ''; // Clear the list before updating
  funds.forEach(fund => {
    const percentageChange = fund.percentagePriceChange;
    const fundRow = document.createElement('div');
    fundRow.className = 'stock-row';
    fundRow.onclick = () => {
      window.location.href = `index-detail.html?symbol=${fund.symbol}`;
    };
    fundRow.innerHTML = `
      <div class="stock-symbol">${fund.symbol}</div>
      <div class="stock-name">${fund.name}</div>
      <div class="stock-price">$${fund.currentPrice.toFixed(2)}</div>
      <div class="stock-change ${percentageChange >= 0 ? 'positive' : 'negative'}">${percentageChange.toFixed(2)}%</div>
    `;
    fundsList.appendChild(fundRow);
  });
}

// Load stock detail page and pass symbol via URL parameters
function loadStockDetailPage(symbol, exchange) {
  window.location.href = `stock-detail.html?symbol=${symbol}&exchange=${exchange}`;
}

// Fetch past orders
function fetchPastOrders(token) {
  fetch('/api/orders', {
    headers: { 'Authorization': `Bearer ` + token }
  })
    .then((response) => {
      return response.json();
    })
    .then((orders) => {
      displayPastOrders(orders, token);
    })
    .catch(function (error) {
      console.log(error);
    });
}

// Display past orders
function displayPastOrders(orders, token) {
  const ordersList = document.getElementById("ordersList");
  ordersList.innerHTML = ''; // Clear the list before updating
  // Sort orders by date which is a string
  orders.sort((a, b) => {
    return new Date(b.dateTime) - new Date(a.dateTime);
  });
  orders.forEach(order => {
    const orderRow = document.createElement('div');
    orderRow.className = 'order-row';
    orderRow.innerHTML = `
      <div class="order-symbol">${order.symbol}</div>
      <button class="cancel-button" ${order.state === 'PENDING' ? 'style="visibility: visible;"' : 'style="visibility: hidden;"'}>Cancel</button>
      <div class="order-quantity">${order.amount}</div>
      <div class="order-price">$${order.price.toFixed(2)}</div>
      <div class="order-status ${order.state}">${order.state}</div>
      <div class="order-type ${order.type}">${order.type}</div>
      <div class="order-date">${order.dateTime}</div>
    `;
    if (order.state === 'PENDING') {
      orderRow.querySelector('.cancel-button').addEventListener('click', () => {
        cancelOrder(order.id, token);
      });
    }
    ordersList.appendChild(orderRow);
  });
}

// Cancel order function
function cancelOrder(orderId, token) {
  fetch(`/api/order/cancel/${orderId}`, {
    headers: { 'Authorization': `Bearer ` + token }
  })
    .then(response => {
      if (response.ok) {
        alert('Order canceled successfully');
        fetchPastOrders(token); // Refresh the order list
      } else {
        throw new Error('Failed to cancel order');
      }
    })
    .catch(error => {
      console.error('Error canceling order:', error);
      alert('Failed to cancel order');
    });
}

function fetchUserInfo(token) {
  fetch('/api/whoami', {
    headers: { 'Authorization': `Bearer ` + token }
  })
    .then((response) => response.json())
    .then((data) => {
      if (data.role === 'manager') {
        addManagerButton();
      }
    })
    .catch((error) => console.log(error));
}

// Add manager button to the page
function addManagerButton() {
  const managerButton = document.createElement('button');
  managerButton.id = 'managerButton';
  managerButton.className = 'manager-button';
  managerButton.innerText = 'Manager Page';
  managerButton.onclick = () => window.location.href = 'manager.html';
  document.body.appendChild(managerButton);
}

// Extract token from URL parameters and fetch stocks
function initHomePage() {
  fetchNYSEStocks();
  fetchNASDAQStocks();
  fetchIndexFunds();

  onAuthStateChanged(auth, (user) => {
    if (user) {
      user.getIdToken().then((token) => {
        fetchPastOrders(token);
        fetchUserInfo(token);
      });
    }
  });

  // Refresh stock data every 30 seconds
  setInterval(() => {
    fetchNYSEStocks();
    fetchNASDAQStocks();
    fetchIndexFunds();
    onAuthStateChanged(auth, (user) => {
      if (user) {
        user.getIdToken().then((token) => {
          fetchPastOrders(token);
          fetchUserInfo(token);
        });
      }
    });
  }, 1000);
}

document.addEventListener('DOMContentLoaded', () => {
  initHomePage();
});
