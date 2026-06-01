import{_ as b,g as m,a as y,b as f,i as I,p as E,c as P,C as v,r as d,d as C,e as S,s as _,f as F,F as k,h as N,j as $,k as R,l as U}from"./vendor-firebase-xxLNFf6Y.js";/**
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
 */class L{constructor(t,e,s,a){this.app=t,this.auth=null,this.messaging=null,this.appCheck=null,this.serverAppAppCheckToken=null,C(t)&&t.settings.appCheckToken&&(this.serverAppAppCheckToken=t.settings.appCheckToken),this.auth=e.getImmediate({optional:!0}),this.messaging=s.getImmediate({optional:!0}),this.auth||e.get().then(i=>this.auth=i,()=>{}),this.messaging||s.get().then(i=>this.messaging=i,()=>{}),this.appCheck||a==null||a.get().then(i=>this.appCheck=i,()=>{})}async getAuthToken(){if(this.auth)try{const t=await this.auth.getToken();return t==null?void 0:t.accessToken}catch{return}}async getMessagingToken(){if(!(!this.messaging||!("Notification"in self)||Notification.permission!=="granted"))try{return await this.messaging.getToken()}catch{return}}async getAppCheckToken(t){if(this.serverAppAppCheckToken)return this.serverAppAppCheckToken;if(this.appCheck){const e=t?await this.appCheck.getLimitedUseToken():await this.appCheck.getToken();return e.error?null:e.token}return null}async getContext(t){const e=await this.getAuthToken(),s=await this.getMessagingToken(),a=await this.getAppCheckToken(t);return{authToken:e,messagingToken:s,appCheckToken:a}}}/**
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
 */const h="us-central1";class O{constructor(t,e,s,a,i=h,o=(...r)=>fetch(...r)){this.app=t,this.fetchImpl=o,this.emulatorOrigin=null,this.contextProvider=new L(t,e,s,a),this.cancelAllRequests=new Promise(r=>{this.deleteService=()=>Promise.resolve(r())});try{const r=new URL(i);this.customDomain=r.origin+(r.pathname==="/"?"":r.pathname),this.region=h}catch{this.customDomain=null,this.region=i}}_delete(){return this.deleteService()}_url(t){const e=this.app.options.projectId;return this.emulatorOrigin!==null?`${this.emulatorOrigin}/${e}/${this.region}/${t}`:this.customDomain!==null?`${this.customDomain}/${t}`:`https://${this.region}-${e}.cloudfunctions.net/${t}`}}function j(n,t,e){const s=I(t);n.emulatorOrigin=`http${s?"s":""}://${t}:${e}`,s&&E(n.emulatorOrigin+"/backends")}const p="@firebase/functions",g="0.13.5";/**
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
 */const M="auth-internal",x="app-check-internal",D="messaging-internal";function B(n){const t=(e,{instanceIdentifier:s})=>{const a=e.getProvider("app").getImmediate(),i=e.getProvider(M),o=e.getProvider(D),r=e.getProvider(x);return new O(a,i,o,r,s)};P(new v(w,t,"PUBLIC").setMultipleInstances(!0)),d(p,g,n),d(p,g,"esm2020")}/**
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
 */function q(n=f(),t=h){const s=b(m(n),w).getImmediate({identifier:t}),a=y("functions");return a&&z(s,...a),s}function z(n,t,e){j(m(n),t,e)}B();const G={apiKey:"AIzaSyCib1UPogwLoAshIWm9YQJB_RR0UxC07i8",authDomain:"supremeai-a.firebaseapp.com",databaseURL:"https://supremeai-a-default-rtdb.asia-southeast1.firebasedatabase.app/",projectId:"supremeai-a",storageBucket:"supremeai-a.firebasestorage.app",messagingSenderId:"565236080752",appId:"1:565236080752:web:572bb9313db9afb355d4b5"},l=R().length?f():U(G),A=S(l),J=$(l),W=q(l);window.addEventListener("unhandledrejection",n=>{const t=n.reason,e=t instanceof Error?t.message:String(t);console.error("[GlobalErrorHandler] Unhandled async error:",e)});window.addEventListener("error",n=>{const t=n.message??"Unknown runtime error",e=n.filename??"unknown",s=n.lineno??0;console.error(`[GlobalErrorHandler] Runtime error at ${e}:${s}:`,t)});function T(n){return n?{"auth/invalid-email":"The email address is not valid. Please check and try again.","auth/user-disabled":"This account has been disabled. Please contact support.","auth/user-not-found":"No account found with this email address.","auth/wrong-password":"The password is incorrect. Please try again.","auth/too-many-requests":"Too many failed attempts. Please wait a moment before trying again.","auth/network-request-failed":"Unable to connect. Please check your internet connection.","auth/invalid-credential":"The credentials are invalid. Please log in again.","auth/expired-action-code":"This link has expired. Please request a new one.","auth/invalid-action-code":"This link is invalid or has already been used.","auth/email-already-in-use":"An account with this email already exists.","auth/weak-password":"The password is too weak. Please use at least 6 characters.","auth/requires-recent-login":"Please log out and log in again to perform this action.","auth/operation-not-allowed":"Email/Password sign-in is disabled. Enable it in Firebase Console > Authentication > Sign-in method.","auth/unauthorized-domain":"This domain (supremeai-a.web.app) is not authorized. Add it to Authorized domains in Firebase Console > Authentication > Settings.","auth/invalid-api-key":"Firebase API key is invalid. Check your Firebase project configuration.","auth/missing-api-key":"Firebase API key is missing. Check your environment configuration.","auth/invalid-app-id":"Firebase App ID is invalid. Verify your Firebase configuration."}[n]??"An authentication error occurred. Please try again.":"An unexpected authentication error occurred."}async function K(n,t){try{const e=await _(A,n,t),a=(await F(e.user)).token,i="";try{const r=await fetch(`${i}/api/auth/firebase-login`,{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({idToken:a})});if(r.ok){const c=await r.json();if(c.success&&c.data){const u=c.data;return localStorage.setItem("supremeai_token",u.token),localStorage.setItem("supremeai_refresh_token",u.refreshToken),u}}}catch{}const o={token:a,refreshToken:"dev-refresh-token",user:{uid:e.user.uid,email:e.user.email||"",displayName:e.user.displayName||"",photoURL:e.user.photoURL||"",role:"admin"}};return localStorage.setItem("supremeai_token",o.token),localStorage.setItem("supremeai_refresh_token",o.refreshToken),o}catch(e){if(e instanceof k){const s=T(e.code);throw console.error("[Firebase Auth Error]",{code:e.code,message:e.message,fullError:e}),new Error(s)}throw console.error("[Unexpected Auth Error]",e),e}}async function V(){const n=localStorage.getItem("supremeai_refresh_token");if(!n)throw new Error("No refresh token available");const e=await fetch("/api/auth/refresh",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({refreshToken:n})});if(!e.ok){const i=await e.json().catch(()=>({}));throw new Error(i.message??"Token refresh failed")}const s=await e.json();if(!s.success||!s.data)throw new Error(s.error??"Token refresh failed");const a=s.data;return localStorage.setItem("supremeai_token",a.token),a.token}async function Y(){try{await N(A)}catch(n){n instanceof k&&console.warn("Firebase sign-out warning:",T(n.code))}}export{A as auth,K as firebaseSignIn,Y as firebaseSignOutFn,J as firestore,W as functions,V as refreshAccessToken};
