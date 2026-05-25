import{_ as A,g as f,a as E,b as k,c as I,C as P,r as g,d as v,e as _,f as C,F as w,h as F,i as S,j as N,k as $,l as R,m as U}from"./vendor-firebase-VaeWqIGB.js";/**
 * @license
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */const b="functions";/**
 * @license
 * Copyright 2017 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */class L{constructor(t,e,a){this.auth=null,this.messaging=null,this.appCheck=null,this.auth=t.getImmediate({optional:!0}),this.messaging=e.getImmediate({optional:!0}),this.auth||t.get().then(s=>this.auth=s,()=>{}),this.messaging||e.get().then(s=>this.messaging=s,()=>{}),this.appCheck||a.get().then(s=>this.appCheck=s,()=>{})}async getAuthToken(){if(this.auth)try{const t=await this.auth.getToken();return t==null?void 0:t.accessToken}catch{return}}async getMessagingToken(){if(!(!this.messaging||!("Notification"in self)||Notification.permission!=="granted"))try{return await this.messaging.getToken()}catch{return}}async getAppCheckToken(t){if(this.appCheck){const e=t?await this.appCheck.getLimitedUseToken():await this.appCheck.getToken();return e.error?null:e.token}return null}async getContext(t){const e=await this.getAuthToken(),a=await this.getMessagingToken(),s=await this.getAppCheckToken(t);return{authToken:e,messagingToken:a,appCheckToken:s}}}/**
 * @license
 * Copyright 2017 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */const h="us-central1";class j{constructor(t,e,a,s,r=h,i){this.app=t,this.fetchImpl=i,this.emulatorOrigin=null,this.contextProvider=new L(e,a,s),this.cancelAllRequests=new Promise(o=>{this.deleteService=()=>Promise.resolve(o())});try{const o=new URL(r);this.customDomain=o.origin+(o.pathname==="/"?"":o.pathname),this.region=h}catch{this.customDomain=null,this.region=r}}_delete(){return this.deleteService()}_url(t){const e=this.app.options.projectId;return this.emulatorOrigin!==null?`${this.emulatorOrigin}/${e}/${this.region}/${t}`:this.customDomain!==null?`${this.customDomain}/${t}`:`https://${this.region}-${e}.cloudfunctions.net/${t}`}}function M(n,t,e){n.emulatorOrigin=`http://${t}:${e}`}const p="@firebase/functions",m="0.11.8";/**
 * @license
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */const O="auth-internal",x="app-check-internal",D="messaging-internal";function B(n,t){const e=(a,{instanceIdentifier:s})=>{const r=a.getProvider("app").getImmediate(),i=a.getProvider(O),o=a.getProvider(D),c=a.getProvider(x);return new j(r,i,o,c,s,n)};I(new P(b,e,"PUBLIC").setMultipleInstances(!0)),g(p,m,t),g(p,m,"esm2017")}/**
 * @license
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */function q(n=k(),t=h){const a=A(f(n),b).getImmediate({identifier:t}),s=E("functions");return s&&T(a,...s),a}function T(n,t,e){M(f(n),t,e)}B(fetch.bind(self));const z={apiKey:"AIzaSyCib1UPogwLoAshIWm9YQJB_RR0UxC07i8",authDomain:"supremeai-a.firebaseapp.com",databaseURL:"https://supremeai-a-default-rtdb.asia-southeast1.firebasedatabase.app/",projectId:"supremeai-a",storageBucket:"supremeai-a.firebasestorage.app",messagingSenderId:"565236080752",appId:"1:565236080752:web:572bb9313db9afb355d4b5"},l=N().length?k():$(z),d=v(l),G=S(l),H=q(l);try{R(d,"http://localhost:9099",{disableWarnings:!0}),U(G,"localhost",8081),T(H,"localhost",5003),console.log("🚀 Firebase Emulators (Auth, Firestore, Functions) connected")}catch(n){console.error("⚠️ Firebase Emulator connection error:",n)}window.addEventListener("unhandledrejection",n=>{const t=n.reason,e=t instanceof Error?t.message:String(t);console.error("[GlobalErrorHandler] Unhandled async error:",e)});window.addEventListener("error",n=>{const t=n.message??"Unknown runtime error",e=n.filename??"unknown",a=n.lineno??0;console.error(`[GlobalErrorHandler] Runtime error at ${e}:${a}:`,t)});function y(n){return n?{"auth/invalid-email":"The email address is not valid. Please check and try again.","auth/user-disabled":"This account has been disabled. Please contact support.","auth/user-not-found":"No account found with this email address.","auth/wrong-password":"The password is incorrect. Please try again.","auth/too-many-requests":"Too many failed attempts. Please wait a moment before trying again.","auth/network-request-failed":"Unable to connect. Please check your internet connection.","auth/invalid-credential":"The credentials are invalid. Please log in again.","auth/expired-action-code":"This link has expired. Please request a new one.","auth/invalid-action-code":"This link is invalid or has already been used.","auth/email-already-in-use":"An account with this email already exists.","auth/weak-password":"The password is too weak. Please use at least 6 characters.","auth/requires-recent-login":"Please log out and log in again to perform this action.","auth/operation-not-allowed":"Email/Password sign-in is disabled. Enable it in Firebase Console > Authentication > Sign-in method.","auth/unauthorized-domain":"This domain (supremeai-a.web.app) is not authorized. Add it to Authorized domains in Firebase Console > Authentication > Settings.","auth/invalid-api-key":"Firebase API key is invalid. Check your Firebase project configuration.","auth/missing-api-key":"Firebase API key is missing. Check your environment configuration.","auth/invalid-app-id":"Firebase App ID is invalid. Verify your Firebase configuration."}[n]??"An authentication error occurred. Please try again.":"An unexpected authentication error occurred."}async function W(n,t){try{const e=await _(d,n,t),s=(await C(e.user)).token,r="http://localhost:8080";try{const o=await fetch(`${r}/api/auth/firebase-login`,{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({idToken:s})});if(o.ok){const c=await o.json();if(c.success&&c.data){const u=c.data;return localStorage.setItem("supremeai_token",u.token),localStorage.setItem("supremeai_refresh_token",u.refreshToken),u}}}catch{}const i={token:s,refreshToken:"dev-refresh-token",user:{uid:e.user.uid,email:e.user.email||"",displayName:e.user.displayName||"",photoURL:e.user.photoURL||"",role:"admin"}};return localStorage.setItem("supremeai_token",i.token),localStorage.setItem("supremeai_refresh_token",i.refreshToken),i}catch(e){if(e instanceof w){const a=y(e.code);throw console.error("[Firebase Auth Error]",{code:e.code,message:e.message,fullError:e}),new Error(a)}throw console.error("[Unexpected Auth Error]",e),e}}async function K(){const n=localStorage.getItem("supremeai_refresh_token");if(!n)throw new Error("No refresh token available");const e=await fetch("http://localhost:8080/api/auth/refresh",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({refreshToken:n})});if(!e.ok){const r=await e.json().catch(()=>({}));throw new Error(r.message??"Token refresh failed")}const a=await e.json();if(!a.success||!a.data)throw new Error(a.error??"Token refresh failed");const s=a.data;return localStorage.setItem("supremeai_token",s.token),s.token}async function V(){try{await F(d)}catch(n){n instanceof w&&console.warn("Firebase sign-out warning:",y(n.code))}}export{d as auth,W as firebaseSignIn,V as firebaseSignOutFn,G as firestore,H as functions,K as refreshAccessToken};
