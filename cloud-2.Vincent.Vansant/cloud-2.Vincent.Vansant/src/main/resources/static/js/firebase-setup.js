import { initializeApp } from "https://www.gstatic.com/firebasejs/9.9.4/firebase-app.js";
import { getAuth, connectAuthEmulator, signOut } from "https://www.gstatic.com/firebasejs/9.9.4/firebase-auth.js";

// Initialize Firebase
let firebaseConfig;
if (location.hostname === "localhost") {
    firebaseConfig = {
        apiKey: "AIzaSyBoLKKR7OFL2ICE15Lc1-8czPtnbej0jWY",
        projectId: "demo-distributed-systems-kul",
    };
} else {
    firebaseConfig = {

            apiKey: "AIzaSyAsTP1Pbnpsdc1JoavOqI8vwquNYNT0G80",

            authDomain: "dappassignment2.firebaseapp.com",

            projectId: "dappassignment2",

            storageBucket: "dappassignment2.appspot.com",

            messagingSenderId: "182128216997",

            appId: "1:182128216997:web:70a3b2e8ac50edab99364c"

          };
}

const firebaseApp = initializeApp(firebaseConfig);
const auth = getAuth(firebaseApp);

if (location.hostname === "localhost") {
    connectAuthEmulator(auth, "http://localhost:8082", { disableWarnings: true });
}

function logoutUser() {
    signOut(auth).then(() => {
        window.location.href = 'index.html'; // Redirect to index.html after logout
    }).catch((error) => {
        console.error("Error logging out:", error);
    });
}

// Attach logout event listener
document.addEventListener('DOMContentLoaded', () => {
    const logoutButton = document.getElementById('btnLogout');
    if (logoutButton) {
        logoutButton.addEventListener('click', logoutUser);
    }
});

export { auth };