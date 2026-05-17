import{_ as T,g as p,a as y,b as f,c as A,C as P,r as l,d as E,e as I,f as v,F as m,h as C,i as F,j as S,k as _}from"./vendor-firebase-CZDHtMTl.js";/**
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
 */const k="functions";/**
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
 */class N{constructor(e,t,a){this.auth=null,this.messaging=null,this.appCheck=null,this.auth=e.getImmediate({optional:!0}),this.messaging=t.getImmediate({optional:!0}),this.auth||e.get().then(i=>this.auth=i,()=>{}),this.messaging||t.get().then(i=>this.messaging=i,()=>{}),this.appCheck||a.get().then(i=>this.appCheck=i,()=>{})}async getAuthToken(){if(this.auth)try{const e=await this.auth.getToken();return e==null?void 0:e.accessToken}catch{return}}async getMessagingToken(){if(!(!this.messaging||!("Notification"in self)||Notification.permission!=="granted"))try{return await this.messaging.getToken()}catch{return}}async getAppCheckToken(e){if(this.appCheck){const t=e?await this.appCheck.getLimitedUseToken():await this.appCheck.getToken();return t.error?null:t.token}return null}async getContext(e){const t=await this.getAuthToken(),a=await this.getMessagingToken(),i=await this.getAppCheckToken(e);return{authToken:t,messagingToken:a,appCheckToken:i}}}/**
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
 */const u="us-central1";class ${constructor(e,t,a,i,r=u,o){this.app=e,this.fetchImpl=o,this.emulatorOrigin=null,this.contextProvider=new N(t,a,i),this.cancelAllRequests=new Promise(s=>{this.deleteService=()=>Promise.resolve(s())});try{const s=new URL(r);this.customDomain=s.origin+(s.pathname==="/"?"":s.pathname),this.region=u}catch{this.customDomain=null,this.region=r}}_delete(){return this.deleteService()}_url(e){const t=this.app.options.projectId;return this.emulatorOrigin!==null?`${this.emulatorOrigin}/${t}/${this.region}/${e}`:this.customDomain!==null?`${this.customDomain}/${e}`:`https://${this.region}-${t}.cloudfunctions.net/${e}`}}function R(n,e,t){n.emulatorOrigin=`http://${e}:${t}`}const d="@firebase/functions",g="0.11.8";/**
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
 */const U="auth-internal",j="app-check-internal",x="messaging-internal";function L(n,e){const t=(a,{instanceIdentifier:i})=>{const r=a.getProvider("app").getImmediate(),o=a.getProvider(U),s=a.getProvider(x),c=a.getProvider(j);return new $(r,o,s,c,i,n)};A(new P(k,t,"PUBLIC").setMultipleInstances(!0)),l(d,g,e),l(d,g,"esm2017")}/**
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
 */function M(n=f(),e=u){const a=T(p(n),k).getImmediate({identifier:e}),i=y("functions");return i&&O(a,...i),a}function O(n,e,t){R(p(n),e,t)}L(fetch.bind(self));const D={apiKey:"AIzaSyCib1UPogwLoAshIWm9YQJB_RR0UxC07i8",authDomain:"supremeai-a.firebaseapp.com",databaseURL:"https://supremeai-a-default-rtdb.asia-southeast1.firebasedatabase.app/",projectId:"supremeai-a",storageBucket:"supremeai-a.firebasestorage.app",messagingSenderId:"565236080752",appId:"1:565236080752:web:572bb9313db9afb355d4b5"},h=S().length?f():_(D),w=E(h),z=F(h),G=M(h);window.addEventListener("unhandledrejection",n=>{const e=n.reason,t=e instanceof Error?e.message:String(e);console.error("[GlobalErrorHandler] Unhandled async error:",t)});window.addEventListener("error",n=>{const e=n.message??"Unknown runtime error",t=n.filename??"unknown",a=n.lineno??0;console.error(`[GlobalErrorHandler] Runtime error at ${t}:${a}:`,e)});function b(n){return n?{"auth/invalid-email":"The email address is not valid. Please check and try again.","auth/user-disabled":"This account has been disabled. Please contact support.","auth/user-not-found":"No account found with this email address.","auth/wrong-password":"The password is incorrect. Please try again.","auth/too-many-requests":"Too many failed attempts. Please wait a moment before trying again.","auth/network-request-failed":"Unable to connect. Please check your internet connection.","auth/invalid-credential":"The credentials are invalid. Please log in again.","auth/expired-action-code":"This link has expired. Please request a new one.","auth/invalid-action-code":"This link is invalid or has already been used.","auth/email-already-in-use":"An account with this email already exists.","auth/weak-password":"The password is too weak. Please use at least 6 characters.","auth/requires-recent-login":"Please log out and log in again to perform this action.","auth/operation-not-allowed":"Email/Password sign-in is disabled. Enable it in Firebase Console > Authentication > Sign-in method.","auth/unauthorized-domain":"This domain (supremeai-a.web.app) is not authorized. Add it to Authorized domains in Firebase Console > Authentication > Settings.","auth/invalid-api-key":"Firebase API key is invalid. Check your Firebase project configuration.","auth/missing-api-key":"Firebase API key is missing. Check your environment configuration.","auth/invalid-app-id":"Firebase App ID is invalid. Verify your Firebase configuration."}[n]??"An authentication error occurred. Please try again.":"An unexpected authentication error occurred."}async function H(n,e){try{const t=await I(w,n,e),i=(await v(t.user)).token,r=await fetch("/api/auth/firebase-login",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({idToken:i})});if(!r.ok){const c=await r.json().catch(()=>({}));throw new Error(c.message??"Firebase token exchange failed")}const o=await r.json();if(!o.success||!o.data)throw new Error(o.error??"Authentication failed");const s=o.data;return localStorage.setItem("supremeai_token",s.token),localStorage.setItem("supremeai_refresh_token",s.refreshToken),s}catch(t){if(t instanceof m){const a=b(t.code);throw console.error("[Firebase Auth Error]",{code:t.code,message:t.message,fullError:t}),new Error(a)}throw console.error("[Unexpected Auth Error]",t),t}}async function B(){const n=localStorage.getItem("supremeai_refresh_token");if(!n)throw new Error("No refresh token available");const e=await fetch("/api/auth/refresh",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({refreshToken:n})});if(!e.ok){const i=await e.json().catch(()=>({}));throw new Error(i.message??"Token refresh failed")}const t=await e.json();if(!t.success||!t.data)throw new Error(t.error??"Token refresh failed");const a=t.data;return localStorage.setItem("supremeai_token",a.token),a.token}async function J(){try{await C(w)}catch(n){n instanceof m&&console.warn("Firebase sign-out warning:",b(n.code))}}export{w as auth,H as firebaseSignIn,J as firebaseSignOutFn,z as firestore,G as functions,B as refreshAccessToken};
