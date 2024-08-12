import { initializeApp } from "https://www.gstatic.com/firebasejs/9.9.4/firebase-app.js";
import { getAuth, connectAuthEmulator, onAuthStateChanged, createUserWithEmailAndPassword, signInWithEmailAndPassword } from "https://www.gstatic.com/firebasejs/9.9.4/firebase-auth.js";
import { auth } from './firebase-setup.js';

// Initialize Firebase
function setupAuth() {
    let firebaseConfig;
    if (location.hostname === "localhost") {
        firebaseConfig = {
            apiKey: "AIzaSyBoLKKR7OFL2ICE15Lc1-8czPtnbej0jWY",
            projectId: "demo-distributed-systems-kul",
        };
    } else {
        firebaseConfig = {
            // TODO: for level 2, paste your config here
        };
    }

    const firebaseApp = initializeApp(firebaseConfig);
    // const auth = getAuth(firebaseApp);
    try {
        auth.signOut();
    } catch (err) {}

    if (location.hostname === "localhost") {
        connectAuthEmulator(auth, "http://localhost:8082", { disableWarnings: true });
    }
}

// Add event listeners to authentication buttons
function wireGuiUpEvents() {
    const email = document.getElementById("email");
    const password = document.getElementById("password");
    const signInButton = document.getElementById("btnSignIn");
    const signUpButton = document.getElementById("btnSignUp");

    signInButton.addEventListener("click", () => {
        signInWithEmailAndPassword(getAuth(), email.value, password.value)
            .then(() => {
                console.log("signedin");
                auth.currentUser.getIdToken().then((token) => {
                  addUserToDatabase(token);
                });
            })
            .catch((error) => {
                console.log("error signInWithEmailAndPassword:", error.message);
                alert(error.message);
            });
    });

    signUpButton.addEventListener("click", () => {
        createUserWithEmailAndPassword(getAuth(), email.value, password.value)
            .then(() => {
                console.log("created");
                auth.currentUser.getIdToken().then((token) => {
                  addUserToDatabase(token);
                });
            })
            .catch((error) => {
                console.log("error createUserWithEmailAndPassword:", error.message);
                alert(error.message);
            });
    });
}

function addUserToDatabase(token) {
  fetch('/api/hello', {
    headers: { 'Authorization': `Bearer ` + token }
  })
    .then(() => {
      console.log("added user to database");
    })
    .catch(function (error) {
      console.log(error);
    });
}

// Handle authentication state changes
function wireUpAuthChange() {
    // const auth = getAuth();
    onAuthStateChanged(auth, (user) => {
        if (user == null || auth.currentUser == null) {
            showUnAuthenticated();
            return;
        }

        auth.currentUser.getIdToken().then((token) => {
            showAuthenticated(auth.currentUser.email);
            loadHomePage();
        });
    });
}

function showAuthenticated(username) {
    document.getElementById("namediv").innerHTML = "Hello " + username;
    document.getElementById("logindiv").style.display = "none";
    document.getElementById("contentdiv").style.display = "block";
}

function showUnAuthenticated() {
    document.getElementById("namediv").innerHTML = "";
    document.getElementById("email").value = "";
    document.getElementById("password").value = "";
    document.getElementById("logindiv").style.display = "block";
    document.getElementById("contentdiv").style.display = "none";
}

// Load home page and pass token via URL parameters
function loadHomePage() {
    window.location.href = `home.html`;
}

// Initialize authentication
// setupAuth();
wireGuiUpEvents();
wireUpAuthChange();
