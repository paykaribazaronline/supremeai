const firebase = require('firebase/compat/app');
require('firebase/compat/auth');
firebase.initializeApp({ apiKey: "test", projectId: "test" });
const authObj = firebase.auth();
console.log("authObj is:", authObj !== undefined ? "defined" : "undefined");
console.log("has signIn:", typeof authObj?.signInWithEmailAndPassword);
