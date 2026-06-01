import{_ as y,g as m,a as A,b as f,c as I,C as P,r as d,d as E,e as v,f as _,F as k,h as C,i as S,j as F,k as N}from"./vendor-firebase-CZDHtMTl.js";/**
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
 */const w="functions";/**
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
 */class ${constructor(t,e,a){this.auth=null,this.messaging=null,this.appCheck=null,this.auth=t.getImmediate({optional:!0}),this.messaging=e.getImmediate({optional:!0}),this.auth||t.get().then(s=>this.auth=s,()=>{}),this.messaging||e.get().then(s=>this.messaging=s,()=>{}),this.appCheck||a.get().then(s=>this.appCheck=s,()=>{})}async getAuthToken(){if(this.auth)try{const t=await this.auth.getToken();return t==null?void 0:t.accessToken}catch{return}}async getMessagingToken(){if(!(!this.messaging||!("Notification"in self)||Notification.permission!=="granted"))try{return await this.messaging.getToken()}catch{return}}async getAppCheckToken(t){if(this.appCheck){const e=t?await this.appCheck.getLimitedUseToken():await this.appCheck.getToken();return e.error?null:e.token}return null}async getContext(t){const e=await this.getAuthToken(),a=await this.getMessagingToken(),s=await this.getAppCheckToken(t);return{authToken:e,messagingToken:a,appCheckToken:s}}}/**
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
 */const h="us-central1";class R{constructor(t,e,a,s,r=h,o){this.app=t,this.fetchImpl=o,this.emulatorOrigin=null,this.contextProvider=new $(e,a,s),this.cancelAllRequests=new Promise(i=>{this.deleteService=()=>Promise.resolve(i())});try{const i=new URL(r);this.customDomain=i.origin+(i.pathname==="/"?"":i.pathname),this.region=h}catch{this.customDomain=null,this.region=r}}_delete(){return this.deleteService()}_url(t){const e=this.app.options.projectId;return this.emulatorOrigin!==null?`${this.emulatorOrigin}/${e}/${this.region}/${t}`:this.customDomain!==null?`${this.customDomain}/${t}`:`https://${this.region}-${e}.cloudfunctions.net/${t}`}}function U(n,t,e){n.emulatorOrigin=`http://${t}:${e}`}const g="@firebase/functions",p="0.11.8";/**
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
 */const L="auth-internal",j="app-check-internal",M="messaging-internal";function O(n,t){const e=(a,{instanceIdentifier:s})=>{const r=a.getProvider("app").getImmediate(),o=a.getProvider(L),i=a.getProvider(M),c=a.getProvider(j);return new R(r,o,i,c,s,n)};I(new P(w,e,"PUBLIC").setMultipleInstances(!0)),d(g,p,t),d(g,p,"esm2017")}/**
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
 */function x(n=f(),t=h){const a=y(m(n),w).getImmediate({identifier:t}),s=A("functions");return s&&D(a,...s),a}function D(n,t,e){U(m(n),t,e)}O(fetch.bind(self));const B={apiKey:"AIzaSyCib1UPogwLoAshIWm9YQJB_RR0UxC07i8",authDomain:"supremeai-a.firebaseapp.com",databaseURL:"https://supremeai-a-default-rtdb.asia-southeast1.firebasedatabase.app/",projectId:"supremeai-a",storageBucket:"supremeai-a.firebasestorage.app",messagingSenderId:"565236080752",appId:"1:565236080752:web:572bb9313db9afb355d4b5"},l=F().length?f():N(B),b=E(l),z=S(l),G=x(l);window.addEventListener("unhandledrejection",n=>{const t=n.reason,e=t instanceof Error?t.message:String(t);console.error("[GlobalErrorHandler] Unhandled async error:",e)});window.addEventListener("error",n=>{const t=n.message??"Unknown runtime error",e=n.filename??"unknown",a=n.lineno??0;console.error(`[GlobalErrorHandler] Runtime error at ${e}:${a}:`,t)});function T(n){return n?{"auth/invalid-email":"The email address is not valid. Please check and try again.","auth/user-disabled":"This account has been disabled. Please contact support.","auth/user-not-found":"No account found with this email address.","auth/wrong-password":"The password is incorrect. Please try again.","auth/too-many-requests":"Too many failed attempts. Please wait a moment before trying again.","auth/network-request-failed":"Unable to connect. Please check your internet connection.","auth/invalid-credential":"The credentials are invalid. Please log in again.","auth/expired-action-code":"This link has expired. Please request a new one.","auth/invalid-action-code":"This link is invalid or has already been used.","auth/email-already-in-use":"An account with this email already exists.","auth/weak-password":"The password is too weak. Please use at least 6 characters.","auth/requires-recent-login":"Please log out and log in again to perform this action.","auth/operation-not-allowed":"Email/Password sign-in is disabled. Enable it in Firebase Console > Authentication > Sign-in method.","auth/unauthorized-domain":"This domain (supremeai-a.web.app) is not authorized. Add it to Authorized domains in Firebase Console > Authentication > Settings.","auth/invalid-api-key":"Firebase API key is invalid. Check your Firebase project configuration.","auth/missing-api-key":"Firebase API key is missing. Check your environment configuration.","auth/invalid-app-id":"Firebase App ID is invalid. Verify your Firebase configuration."}[n]??"An authentication error occurred. Please try again.":"An unexpected authentication error occurred."}async function H(n,t){try{const e=await v(b,n,t),s=(await _(e.user)).token,r="";try{const i=await fetch(`${r}/api/auth/firebase-login`,{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({idToken:s})});if(i.ok){const c=await i.json();if(c.success&&c.data){const u=c.data;return localStorage.setItem("supremeai_token",u.token),localStorage.setItem("supremeai_refresh_token",u.refreshToken),u}}}catch{}const o={token:s,refreshToken:"dev-refresh-token",user:{uid:e.user.uid,email:e.user.email||"",displayName:e.user.displayName||"",photoURL:e.user.photoURL||"",role:"admin"}};return localStorage.setItem("supremeai_token",o.token),localStorage.setItem("supremeai_refresh_token",o.refreshToken),o}catch(e){if(e instanceof k){const a=T(e.code);throw console.error("[Firebase Auth Error]",{code:e.code,message:e.message,fullError:e}),new Error(a)}throw console.error("[Unexpected Auth Error]",e),e}}async function J(){const n=localStorage.getItem("supremeai_refresh_token");if(!n)throw new Error("No refresh token available");const e=await fetch("/api/auth/refresh",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({refreshToken:n})});if(!e.ok){const r=await e.json().catch(()=>({}));throw new Error(r.message??"Token refresh failed")}const a=await e.json();if(!a.success||!a.data)throw new Error(a.error??"Token refresh failed");const s=a.data;return localStorage.setItem("supremeai_token",s.token),s.token}async function K(){try{await C(b)}catch(n){n instanceof k&&console.warn("Firebase sign-out warning:",T(n.code))}}export{b as auth,H as firebaseSignIn,K as firebaseSignOutFn,z as firestore,G as functions,J as refreshAccessToken};
