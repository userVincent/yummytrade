import { getAuth, onAuthStateChanged } from "https://www.gstatic.com/firebasejs/9.9.4/firebase-auth.js";
import { auth } from './firebase-setup.js';

document.addEventListener('DOMContentLoaded', () => {
    initManagerPage();
});

function initManagerPage() {
    onAuthStateChanged(auth, (user) => {
        if (user) {
            user.getIdToken().then((token) => {
                fetchUsers(token);
                fetchAllOrders(token);
            });
        } else {
            window.location.href = 'index.html';
        }
    });
}

function fetchUsers(token) {
    fetch('/api/getAllCustomers', {  // Ensure this endpoint returns all users
        headers: { 'Authorization': `Bearer ` + token }
    })
    .then((response) => response.json())
    .then((users) => {
        displayUsers(users);
    })
    .catch((error) => {
        console.log(error);
    });
}

function displayUsers(users) {
    const usersList = document.getElementById("usersList");
    users.forEach(user => {
        const userRow = document.createElement('div');
        userRow.className = 'user-row';
        userRow.innerHTML = `
            <div class="user-email">${user.email}</div>
            <div class="user-role">${user.role}</div>
        `;
        usersList.appendChild(userRow);
    });
}

function fetchAllOrders(token) {
    fetch('/api/getAllOrders', {
        headers: { 'Authorization': `Bearer ` + token }
    })
    .then((response) => response.json())
    .then((orders) => {
        displayAllOrders(orders);
    })
    .catch((error) => {
        console.log(error);
    });
}

function displayAllOrders(orders) {
    const ordersList = document.getElementById("allOrdersList");
    orders.sort((a, b) => {
        return new Date(b.dateTime) - new Date(a.dateTime);
      });
    orders.forEach(order => {
        const orderRow = document.createElement('div');
        orderRow.className = 'order-row';
        orderRow.innerHTML = `
            <div class="order-symbol">${order.symbol}</div>
            <div class="order-quantity">${order.amount}</div>
            <div class="order-price">$${order.price.toFixed(2)}</div>
            <div class="order-status ${order.state}">${order.state}</div>
            <div class="order-type ${order.type}">${order.type}</div>
            <div class="order-email">${order.email}</div>
            <div class="order-date">${order.dateTime}</div>
        `;
        ordersList.appendChild(orderRow);
    });
}
