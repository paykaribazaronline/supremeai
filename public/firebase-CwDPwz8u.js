import{_ as y,g as m,a as b,b as f,i as I,p as E,c as v,C as P,r as d,d as C,e as S,s as _,f as F,F as k,h as N,j as $,k as R,l as U}from"./vendor-firebase-xxLNFf6Y.js";/**
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
 */class L{constructor(t,e,s,i){this.app=t,this.auth=null,this.messaging=null,this.appCheck=null,this.serverAppAppCheckToken=null,C(t)&&t.settings.appCheckToken&&(this.serverAppAppCheckToken=t.settings.appCheckToken),this.auth=e.getImmediate({optional:!0}),this.messaging=s.getImmediate({optional:!0}),this.auth||e.get().then(r=>this.auth=r,()=>{}),this.messaging||s.get().then(r=>this.messaging=r,()=>{}),this.appCheck||i==null||i.get().then(r=>this.appCheck=r,()=>{})}async getAuthToken(){if(this.auth)try{const t=await this.auth.getToken();return t==null?void 0:t.accessToken}catch{return}}async getMessagingToken(){if(!(!this.messaging||!("Notification"in self)||Notification.permission!=="granted"))try{return await this.messaging.getToken()}catch{return}}async getAppCheckToken(t){if(this.serverAppAppCheckToken)return this.serverAppAppCheckToken;if(this.appCheck){const e=t?await this.appCheck.getLimitedUseToken():await this.appCheck.getToken();return e.error?null:e.token}return null}async getContext(t){const e=await this.getAuthToken(),s=await this.getMessagingToken(),i=await this.getAppCheckToken(t);return{authToken:e,messagingToken:s,appCheckToken:i}}}/**
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
 */const h="us-central1";class O{constructor(t,e,s,i,r=h,o=(...a)=>fetch(...a)){this.app=t,this.fetchImpl=o,this.emulatorOrigin=null,this.contextProvider=new L(t,e,s,i),this.cancelAllRequests=new Promise(a=>{this.deleteService=()=>Promise.resolve(a())});try{const a=new URL(r);this.customDomain=a.origin+(a.pathname==="/"?"":a.pathname),this.region=h}catch{this.customDomain=null,this.region=r}}_delete(){return this.deleteService()}_url(t){const e=this.app.options.projectId;return this.emulatorOrigin!==null?`${this.emulatorOrigin}/${e}/${this.region}/${t}`:this.customDomain!==null?`${this.customDomain}/${t}`:`https://${this.region}-${e}.cloudfunctions.net/${t}`}}function j(n,t,e){const s=I(t);n.emulatorOrigin=`http${s?"s":""}://${t}:${e}`,s&&E(n.emulatorOrigin+"/backends")}const g="@firebase/functions",p="0.13.5";/**
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
 */const M="auth-internal",x="app-check-internal",D="messaging-internal";function q(n){const t=(e,{instanceIdentifier:s})=>{const i=e.getProvider("app").getImmediate(),r=e.getProvider(M),o=e.getProvider(D),a=e.getProvider(x);return new O(i,r,o,a,s)};v(new P(w,t,"PUBLIC").setMultipleInstances(!0)),d(g,p,n),d(g,p,"esm2020")}/**
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
 */function B(n=f(),t=h){const s=y(m(n),w).getImmediate({identifier:t}),i=b("functions");return i&&G(s,...i),s}function G(n,t,e){j(m(n),t,e)}q();const H={apiKey:"dummy-key-for-development",authDomain:window.location.host,databaseURL:"",projectId:"supremeai-a",storageBucket:"",messagingSenderId:"123456789",appId:"1:123456789:web:abcdef"},l=R().length?f():U(H),T=S(l),J=$(l),K=B(l);window.addEventListener("unhandledrejection",n=>{const t=n.reason,e=t instanceof Error?t.message:String(t);console.error("[GlobalErrorHandler] Unhandled async error:",e)});window.addEventListener("error",n=>{const t=n.message??"Unknown runtime error",e=n.filename??"unknown",s=n.lineno??0;console.error(`[GlobalErrorHandler] Runtime error at ${e}:${s}:`,t)});function A(n){return n?{"auth/invalid-email":"The email address is not valid. Please check and try again.","auth/user-disabled":"This account has been disabled. Please contact support.","auth/user-not-found":"No account found with this email address.","auth/wrong-password":"The password is incorrect. Please try again.","auth/too-many-requests":"Too many failed attempts. Please wait a moment before trying again.","auth/network-request-failed":"Unable to connect. Please check your internet connection.","auth/invalid-credential":"The credentials are invalid. Please log in again.","auth/expired-action-code":"This link has expired. Please request a new one.","auth/invalid-action-code":"This link is invalid or has already been used.","auth/email-already-in-use":"An account with this email already exists.","auth/weak-password":"The password is too weak. Please use at least 6 characters.","auth/requires-recent-login":"Please log out and log in again to perform this action.","auth/operation-not-allowed":"Email/Password sign-in is disabled. Enable it in Firebase Console > Authentication > Sign-in method.","auth/unauthorized-domain":"This domain (supremeai-a.web.app) is not authorized. Add it to Authorized domains in Firebase Console > Authentication > Settings.","auth/invalid-api-key":"Firebase API key is invalid. Check your Firebase project configuration.","auth/missing-api-key":"Firebase API key is missing. Check your environment configuration.","auth/invalid-app-id":"Firebase App ID is invalid. Verify your Firebase configuration."}[n]??"An authentication error occurred. Please try again.":"An unexpected authentication error occurred."}async function V(n,t){try{const e=await _(T,n,t),i=(await F(e.user)).token,r="";try{const a=await fetch(`${r}/api/auth/firebase-login`,{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({idToken:i})});if(a.ok){const c=await a.json();if(c.success&&c.data){const u=c.data;return localStorage.setItem("supremeai_token",u.token),localStorage.setItem("supremeai_refresh_token",u.refreshToken),u}}}catch{}const o={token:i,refreshToken:"dev-refresh-token",user:{uid:e.user.uid,email:e.user.email||"",displayName:e.user.displayName||"",photoURL:e.user.photoURL||"",role:"admin"}};return localStorage.setItem("supremeai_token",o.token),localStorage.setItem("supremeai_refresh_token",o.refreshToken),o}catch(e){if(e instanceof k){const s=A(e.code);throw console.error("[Firebase Auth Error]",{code:e.code,message:e.message,fullError:e}),new Error(s)}throw console.error("[Unexpected Auth Error]",e),e}}async function W(){const n=localStorage.getItem("supremeai_refresh_token");if(!n)throw new Error("No refresh token available");const e=await fetch("/api/auth/refresh",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({refreshToken:n})});if(!e.ok){const r=await e.json().catch(()=>({}));throw new Error(r.message??"Token refresh failed")}const s=await e.json();if(!s.success||!s.data)throw new Error(s.error??"Token refresh failed");const i=s.data;return localStorage.setItem("supremeai_token",i.token),i.token}async function Y(){try{await N(T)}catch(n){n instanceof k&&console.warn("Firebase sign-out warning:",A(n.code))}}export{T as auth,V as firebaseSignIn,Y as firebaseSignOutFn,J as firestore,K as functions,W as refreshAccessToken};
