var _r={};/**
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
 */const rs=function(i){const e=[];let n=0;for(let r=0;r<i.length;r++){let o=i.charCodeAt(r);o<128?e[n++]=o:o<2048?(e[n++]=o>>6|192,e[n++]=o&63|128):(o&64512)===55296&&r+1<i.length&&(i.charCodeAt(r+1)&64512)===56320?(o=65536+((o&1023)<<10)+(i.charCodeAt(++r)&1023),e[n++]=o>>18|240,e[n++]=o>>12&63|128,e[n++]=o>>6&63|128,e[n++]=o&63|128):(e[n++]=o>>12|224,e[n++]=o>>6&63|128,e[n++]=o&63|128)}return e},Vo=function(i){const e=[];let n=0,r=0;for(;n<i.length;){const o=i[n++];if(o<128)e[r++]=String.fromCharCode(o);else if(o>191&&o<224){const c=i[n++];e[r++]=String.fromCharCode((o&31)<<6|c&63)}else if(o>239&&o<365){const c=i[n++],l=i[n++],p=i[n++],y=((o&7)<<18|(c&63)<<12|(l&63)<<6|p&63)-65536;e[r++]=String.fromCharCode(55296+(y>>10)),e[r++]=String.fromCharCode(56320+(y&1023))}else{const c=i[n++],l=i[n++];e[r++]=String.fromCharCode((o&15)<<12|(c&63)<<6|l&63)}}return e.join("")},ss={byteToCharMap_:null,charToByteMap_:null,byteToCharMapWebSafe_:null,charToByteMapWebSafe_:null,ENCODED_VALS_BASE:"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789",get ENCODED_VALS(){return this.ENCODED_VALS_BASE+"+/="},get ENCODED_VALS_WEBSAFE(){return this.ENCODED_VALS_BASE+"-_."},HAS_NATIVE_SUPPORT:typeof atob=="function",encodeByteArray(i,e){if(!Array.isArray(i))throw Error("encodeByteArray takes an array as a parameter");this.init_();const n=e?this.byteToCharMapWebSafe_:this.byteToCharMap_,r=[];for(let o=0;o<i.length;o+=3){const c=i[o],l=o+1<i.length,p=l?i[o+1]:0,y=o+2<i.length,E=y?i[o+2]:0,S=c>>2,b=(c&3)<<4|p>>4;let R=(p&15)<<2|E>>6,x=E&63;y||(x=64,l||(R=64)),r.push(n[S],n[b],n[R],n[x])}return r.join("")},encodeString(i,e){return this.HAS_NATIVE_SUPPORT&&!e?btoa(i):this.encodeByteArray(rs(i),e)},decodeString(i,e){return this.HAS_NATIVE_SUPPORT&&!e?atob(i):Vo(this.decodeStringToByteArray(i,e))},decodeStringToByteArray(i,e){this.init_();const n=e?this.charToByteMapWebSafe_:this.charToByteMap_,r=[];for(let o=0;o<i.length;){const c=n[i.charAt(o++)],p=o<i.length?n[i.charAt(o)]:0;++o;const E=o<i.length?n[i.charAt(o)]:64;++o;const b=o<i.length?n[i.charAt(o)]:64;if(++o,c==null||p==null||E==null||b==null)throw new Ho;const R=c<<2|p>>4;if(r.push(R),E!==64){const x=p<<4&240|E>>2;if(r.push(x),b!==64){const P=E<<6&192|b;r.push(P)}}}return r},init_(){if(!this.byteToCharMap_){this.byteToCharMap_={},this.charToByteMap_={},this.byteToCharMapWebSafe_={},this.charToByteMapWebSafe_={};for(let i=0;i<this.ENCODED_VALS.length;i++)this.byteToCharMap_[i]=this.ENCODED_VALS.charAt(i),this.charToByteMap_[this.byteToCharMap_[i]]=i,this.byteToCharMapWebSafe_[i]=this.ENCODED_VALS_WEBSAFE.charAt(i),this.charToByteMapWebSafe_[this.byteToCharMapWebSafe_[i]]=i,i>=this.ENCODED_VALS_BASE.length&&(this.charToByteMap_[this.ENCODED_VALS_WEBSAFE.charAt(i)]=i,this.charToByteMapWebSafe_[this.ENCODED_VALS.charAt(i)]=i)}}};class Ho extends Error{constructor(){super(...arguments),this.name="DecodeBase64StringError"}}const $o=function(i){const e=rs(i);return ss.encodeByteArray(e,!0)},en=function(i){return $o(i).replace(/\./g,"")},os=function(i){try{return ss.decodeString(i,!0)}catch(e){console.error("base64Decode failed: ",e)}return null};/**
 * @license
 * Copyright 2022 Google LLC
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
 */function Wo(){if(typeof self<"u")return self;if(typeof window<"u")return window;if(typeof global<"u")return global;throw new Error("Unable to locate global object.")}/**
 * @license
 * Copyright 2022 Google LLC
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
 */const zo=()=>Wo().__FIREBASE_DEFAULTS__,Go=()=>{if(typeof process>"u"||typeof _r>"u")return;const i=_r.__FIREBASE_DEFAULTS__;if(i)return JSON.parse(i)},Ko=()=>{if(typeof document>"u")return;let i;try{i=document.cookie.match(/__FIREBASE_DEFAULTS__=([^;]+)/)}catch{return}const e=i&&os(i[1]);return e&&JSON.parse(e)},ri=()=>{try{return zo()||Go()||Ko()}catch(i){console.info(`Unable to get __FIREBASE_DEFAULTS__ due to: ${i}`);return}},as=i=>{var e,n;return(n=(e=ri())===null||e===void 0?void 0:e.emulatorHosts)===null||n===void 0?void 0:n[i]},cs=i=>{const e=as(i);if(!e)return;const n=e.lastIndexOf(":");if(n<=0||n+1===e.length)throw new Error(`Invalid host ${e} with no separate hostname and port!`);const r=parseInt(e.substring(n+1),10);return e[0]==="["?[e.substring(1,n-1),r]:[e.substring(0,n),r]},hs=()=>{var i;return(i=ri())===null||i===void 0?void 0:i.config},ls=i=>{var e;return(e=ri())===null||e===void 0?void 0:e[`_${i}`]};/**
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
 */class qo{constructor(){this.reject=()=>{},this.resolve=()=>{},this.promise=new Promise((e,n)=>{this.resolve=e,this.reject=n})}wrapCallback(e){return(n,r)=>{n?this.reject(n):this.resolve(r),typeof e=="function"&&(this.promise.catch(()=>{}),e.length===1?e(n):e(n,r))}}}/**
 * @license
 * Copyright 2021 Google LLC
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
 */function Jo(i,e){if(i.uid)throw new Error('The "uid" field is no longer supported by mockUserToken. Please use "sub" instead for Firebase Auth User ID.');const n={alg:"none",type:"JWT"},r=e||"demo-project",o=i.iat||0,c=i.sub||i.user_id;if(!c)throw new Error("mockUserToken must contain 'sub' or 'user_id' field!");const l=Object.assign({iss:`https://securetoken.google.com/${r}`,aud:r,iat:o,exp:o+3600,auth_time:o,sub:c,user_id:c,firebase:{sign_in_provider:"custom",identities:{}}},i);return[en(JSON.stringify(n)),en(JSON.stringify(l)),""].join(".")}/**
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
 */function K(){return typeof navigator<"u"&&typeof navigator.userAgent=="string"?navigator.userAgent:""}function Xo(){return typeof window<"u"&&!!(window.cordova||window.phonegap||window.PhoneGap)&&/ios|iphone|ipod|ipad|android|blackberry|iemobile/i.test(K())}function Yo(){return typeof navigator<"u"&&navigator.userAgent==="Cloudflare-Workers"}function Qo(){const i=typeof chrome=="object"?chrome.runtime:typeof browser=="object"?browser.runtime:void 0;return typeof i=="object"&&i.id!==void 0}function Zo(){return typeof navigator=="object"&&navigator.product==="ReactNative"}function ea(){const i=K();return i.indexOf("MSIE ")>=0||i.indexOf("Trident/")>=0}function ta(){try{return typeof indexedDB=="object"}catch{return!1}}function na(){return new Promise((i,e)=>{try{let n=!0;const r="validate-browser-context-for-indexeddb-analytics-module",o=self.indexedDB.open(r);o.onsuccess=()=>{o.result.close(),n||self.indexedDB.deleteDatabase(r),i(!0)},o.onupgradeneeded=()=>{n=!1},o.onerror=()=>{var c;e(((c=o.error)===null||c===void 0?void 0:c.message)||"")}}catch(n){e(n)}})}/**
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
 */const ia="FirebaseError";class ie extends Error{constructor(e,n,r){super(n),this.code=e,this.customData=r,this.name=ia,Object.setPrototypeOf(this,ie.prototype),Error.captureStackTrace&&Error.captureStackTrace(this,bt.prototype.create)}}class bt{constructor(e,n,r){this.service=e,this.serviceName=n,this.errors=r}create(e,...n){const r=n[0]||{},o=`${this.service}/${e}`,c=this.errors[e],l=c?ra(c,r):"Error",p=`${this.serviceName}: ${l} (${o}).`;return new ie(o,p,r)}}function ra(i,e){return i.replace(sa,(n,r)=>{const o=e[r];return o!=null?String(o):`<${r}?>`})}const sa=/\{\$([^}]+)}/g;function oa(i){for(const e in i)if(Object.prototype.hasOwnProperty.call(i,e))return!1;return!0}function tn(i,e){if(i===e)return!0;const n=Object.keys(i),r=Object.keys(e);for(const o of n){if(!r.includes(o))return!1;const c=i[o],l=e[o];if(yr(c)&&yr(l)){if(!tn(c,l))return!1}else if(c!==l)return!1}for(const o of r)if(!n.includes(o))return!1;return!0}function yr(i){return i!==null&&typeof i=="object"}/**
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
 */function kt(i){const e=[];for(const[n,r]of Object.entries(i))Array.isArray(r)?r.forEach(o=>{e.push(encodeURIComponent(n)+"="+encodeURIComponent(o))}):e.push(encodeURIComponent(n)+"="+encodeURIComponent(r));return e.length?"&"+e.join("&"):""}function vt(i){const e={};return i.replace(/^\?/,"").split("&").forEach(r=>{if(r){const[o,c]=r.split("=");e[decodeURIComponent(o)]=decodeURIComponent(c)}}),e}function _t(i){const e=i.indexOf("?");if(!e)return"";const n=i.indexOf("#",e);return i.substring(e,n>0?n:void 0)}function aa(i,e){const n=new ca(i,e);return n.subscribe.bind(n)}class ca{constructor(e,n){this.observers=[],this.unsubscribes=[],this.observerCount=0,this.task=Promise.resolve(),this.finalized=!1,this.onNoObservers=n,this.task.then(()=>{e(this)}).catch(r=>{this.error(r)})}next(e){this.forEachObserver(n=>{n.next(e)})}error(e){this.forEachObserver(n=>{n.error(e)}),this.close(e)}complete(){this.forEachObserver(e=>{e.complete()}),this.close()}subscribe(e,n,r){let o;if(e===void 0&&n===void 0&&r===void 0)throw new Error("Missing Observer.");ha(e,["next","error","complete"])?o=e:o={next:e,error:n,complete:r},o.next===void 0&&(o.next=Vn),o.error===void 0&&(o.error=Vn),o.complete===void 0&&(o.complete=Vn);const c=this.unsubscribeOne.bind(this,this.observers.length);return this.finalized&&this.task.then(()=>{try{this.finalError?o.error(this.finalError):o.complete()}catch{}}),this.observers.push(o),c}unsubscribeOne(e){this.observers===void 0||this.observers[e]===void 0||(delete this.observers[e],this.observerCount-=1,this.observerCount===0&&this.onNoObservers!==void 0&&this.onNoObservers(this))}forEachObserver(e){if(!this.finalized)for(let n=0;n<this.observers.length;n++)this.sendOne(n,e)}sendOne(e,n){this.task.then(()=>{if(this.observers!==void 0&&this.observers[e]!==void 0)try{n(this.observers[e])}catch(r){typeof console<"u"&&console.error&&console.error(r)}})}close(e){this.finalized||(this.finalized=!0,e!==void 0&&(this.finalError=e),this.task.then(()=>{this.observers=void 0,this.onNoObservers=void 0}))}}function ha(i,e){if(typeof i!="object"||i===null)return!1;for(const n of e)if(n in i&&typeof i[n]=="function")return!0;return!1}function Vn(){}/**
 * @license
 * Copyright 2021 Google LLC
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
 */function ae(i){return i&&i._delegate?i._delegate:i}class Ce{constructor(e,n,r){this.name=e,this.instanceFactory=n,this.type=r,this.multipleInstances=!1,this.serviceProps={},this.instantiationMode="LAZY",this.onInstanceCreated=null}setInstantiationMode(e){return this.instantiationMode=e,this}setMultipleInstances(e){return this.multipleInstances=e,this}setServiceProps(e){return this.serviceProps=e,this}setInstanceCreatedCallback(e){return this.onInstanceCreated=e,this}}/**
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
 */const Ue="[DEFAULT]";/**
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
 */class la{constructor(e,n){this.name=e,this.container=n,this.component=null,this.instances=new Map,this.instancesDeferred=new Map,this.instancesOptions=new Map,this.onInitCallbacks=new Map}get(e){const n=this.normalizeInstanceIdentifier(e);if(!this.instancesDeferred.has(n)){const r=new qo;if(this.instancesDeferred.set(n,r),this.isInitialized(n)||this.shouldAutoInitialize())try{const o=this.getOrInitializeService({instanceIdentifier:n});o&&r.resolve(o)}catch{}}return this.instancesDeferred.get(n).promise}getImmediate(e){var n;const r=this.normalizeInstanceIdentifier(e==null?void 0:e.identifier),o=(n=e==null?void 0:e.optional)!==null&&n!==void 0?n:!1;if(this.isInitialized(r)||this.shouldAutoInitialize())try{return this.getOrInitializeService({instanceIdentifier:r})}catch(c){if(o)return null;throw c}else{if(o)return null;throw Error(`Service ${this.name} is not available`)}}getComponent(){return this.component}setComponent(e){if(e.name!==this.name)throw Error(`Mismatching Component ${e.name} for Provider ${this.name}.`);if(this.component)throw Error(`Component for ${this.name} has already been provided`);if(this.component=e,!!this.shouldAutoInitialize()){if(da(e))try{this.getOrInitializeService({instanceIdentifier:Ue})}catch{}for(const[n,r]of this.instancesDeferred.entries()){const o=this.normalizeInstanceIdentifier(n);try{const c=this.getOrInitializeService({instanceIdentifier:o});r.resolve(c)}catch{}}}}clearInstance(e=Ue){this.instancesDeferred.delete(e),this.instancesOptions.delete(e),this.instances.delete(e)}async delete(){const e=Array.from(this.instances.values());await Promise.all([...e.filter(n=>"INTERNAL"in n).map(n=>n.INTERNAL.delete()),...e.filter(n=>"_delete"in n).map(n=>n._delete())])}isComponentSet(){return this.component!=null}isInitialized(e=Ue){return this.instances.has(e)}getOptions(e=Ue){return this.instancesOptions.get(e)||{}}initialize(e={}){const{options:n={}}=e,r=this.normalizeInstanceIdentifier(e.instanceIdentifier);if(this.isInitialized(r))throw Error(`${this.name}(${r}) has already been initialized`);if(!this.isComponentSet())throw Error(`Component ${this.name} has not been registered yet`);const o=this.getOrInitializeService({instanceIdentifier:r,options:n});for(const[c,l]of this.instancesDeferred.entries()){const p=this.normalizeInstanceIdentifier(c);r===p&&l.resolve(o)}return o}onInit(e,n){var r;const o=this.normalizeInstanceIdentifier(n),c=(r=this.onInitCallbacks.get(o))!==null&&r!==void 0?r:new Set;c.add(e),this.onInitCallbacks.set(o,c);const l=this.instances.get(o);return l&&e(l,o),()=>{c.delete(e)}}invokeOnInitCallbacks(e,n){const r=this.onInitCallbacks.get(n);if(r)for(const o of r)try{o(e,n)}catch{}}getOrInitializeService({instanceIdentifier:e,options:n={}}){let r=this.instances.get(e);if(!r&&this.component&&(r=this.component.instanceFactory(this.container,{instanceIdentifier:ua(e),options:n}),this.instances.set(e,r),this.instancesOptions.set(e,n),this.invokeOnInitCallbacks(r,e),this.component.onInstanceCreated))try{this.component.onInstanceCreated(this.container,e,r)}catch{}return r||null}normalizeInstanceIdentifier(e=Ue){return this.component?this.component.multipleInstances?e:Ue:e}shouldAutoInitialize(){return!!this.component&&this.component.instantiationMode!=="EXPLICIT"}}function ua(i){return i===Ue?void 0:i}function da(i){return i.instantiationMode==="EAGER"}/**
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
 */class fa{constructor(e){this.name=e,this.providers=new Map}addComponent(e){const n=this.getProvider(e.name);if(n.isComponentSet())throw new Error(`Component ${e.name} has already been registered with ${this.name}`);n.setComponent(e)}addOrOverwriteComponent(e){this.getProvider(e.name).isComponentSet()&&this.providers.delete(e.name),this.addComponent(e)}getProvider(e){if(this.providers.has(e))return this.providers.get(e);const n=new la(e,this);return this.providers.set(e,n),n}getProviders(){return Array.from(this.providers.values())}}/**
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
 */var O;(function(i){i[i.DEBUG=0]="DEBUG",i[i.VERBOSE=1]="VERBOSE",i[i.INFO=2]="INFO",i[i.WARN=3]="WARN",i[i.ERROR=4]="ERROR",i[i.SILENT=5]="SILENT"})(O||(O={}));const pa={debug:O.DEBUG,verbose:O.VERBOSE,info:O.INFO,warn:O.WARN,error:O.ERROR,silent:O.SILENT},ga=O.INFO,ma={[O.DEBUG]:"log",[O.VERBOSE]:"log",[O.INFO]:"info",[O.WARN]:"warn",[O.ERROR]:"error"},va=(i,e,...n)=>{if(e<i.logLevel)return;const r=new Date().toISOString(),o=ma[e];if(o)console[o](`[${r}]  ${i.name}:`,...n);else throw new Error(`Attempted to log a message with an invalid logType (value: ${e})`)};class si{constructor(e){this.name=e,this._logLevel=ga,this._logHandler=va,this._userLogHandler=null}get logLevel(){return this._logLevel}set logLevel(e){if(!(e in O))throw new TypeError(`Invalid value "${e}" assigned to \`logLevel\``);this._logLevel=e}setLogLevel(e){this._logLevel=typeof e=="string"?pa[e]:e}get logHandler(){return this._logHandler}set logHandler(e){if(typeof e!="function")throw new TypeError("Value assigned to `logHandler` must be a function");this._logHandler=e}get userLogHandler(){return this._userLogHandler}set userLogHandler(e){this._userLogHandler=e}debug(...e){this._userLogHandler&&this._userLogHandler(this,O.DEBUG,...e),this._logHandler(this,O.DEBUG,...e)}log(...e){this._userLogHandler&&this._userLogHandler(this,O.VERBOSE,...e),this._logHandler(this,O.VERBOSE,...e)}info(...e){this._userLogHandler&&this._userLogHandler(this,O.INFO,...e),this._logHandler(this,O.INFO,...e)}warn(...e){this._userLogHandler&&this._userLogHandler(this,O.WARN,...e),this._logHandler(this,O.WARN,...e)}error(...e){this._userLogHandler&&this._userLogHandler(this,O.ERROR,...e),this._logHandler(this,O.ERROR,...e)}}const _a=(i,e)=>e.some(n=>i instanceof n);let wr,Ir;function ya(){return wr||(wr=[IDBDatabase,IDBObjectStore,IDBIndex,IDBCursor,IDBTransaction])}function wa(){return Ir||(Ir=[IDBCursor.prototype.advance,IDBCursor.prototype.continue,IDBCursor.prototype.continuePrimaryKey])}const us=new WeakMap,Jn=new WeakMap,ds=new WeakMap,Hn=new WeakMap,oi=new WeakMap;function Ia(i){const e=new Promise((n,r)=>{const o=()=>{i.removeEventListener("success",c),i.removeEventListener("error",l)},c=()=>{n(ke(i.result)),o()},l=()=>{r(i.error),o()};i.addEventListener("success",c),i.addEventListener("error",l)});return e.then(n=>{n instanceof IDBCursor&&us.set(n,i)}).catch(()=>{}),oi.set(e,i),e}function Ea(i){if(Jn.has(i))return;const e=new Promise((n,r)=>{const o=()=>{i.removeEventListener("complete",c),i.removeEventListener("error",l),i.removeEventListener("abort",l)},c=()=>{n(),o()},l=()=>{r(i.error||new DOMException("AbortError","AbortError")),o()};i.addEventListener("complete",c),i.addEventListener("error",l),i.addEventListener("abort",l)});Jn.set(i,e)}let Xn={get(i,e,n){if(i instanceof IDBTransaction){if(e==="done")return Jn.get(i);if(e==="objectStoreNames")return i.objectStoreNames||ds.get(i);if(e==="store")return n.objectStoreNames[1]?void 0:n.objectStore(n.objectStoreNames[0])}return ke(i[e])},set(i,e,n){return i[e]=n,!0},has(i,e){return i instanceof IDBTransaction&&(e==="done"||e==="store")?!0:e in i}};function Ta(i){Xn=i(Xn)}function Aa(i){return i===IDBDatabase.prototype.transaction&&!("objectStoreNames"in IDBTransaction.prototype)?function(e,...n){const r=i.call($n(this),e,...n);return ds.set(r,e.sort?e.sort():[e]),ke(r)}:wa().includes(i)?function(...e){return i.apply($n(this),e),ke(us.get(this))}:function(...e){return ke(i.apply($n(this),e))}}function Sa(i){return typeof i=="function"?Aa(i):(i instanceof IDBTransaction&&Ea(i),_a(i,ya())?new Proxy(i,Xn):i)}function ke(i){if(i instanceof IDBRequest)return Ia(i);if(Hn.has(i))return Hn.get(i);const e=Sa(i);return e!==i&&(Hn.set(i,e),oi.set(e,i)),e}const $n=i=>oi.get(i);function ba(i,e,{blocked:n,upgrade:r,blocking:o,terminated:c}={}){const l=indexedDB.open(i,e),p=ke(l);return r&&l.addEventListener("upgradeneeded",y=>{r(ke(l.result),y.oldVersion,y.newVersion,ke(l.transaction),y)}),n&&l.addEventListener("blocked",y=>n(y.oldVersion,y.newVersion,y)),p.then(y=>{c&&y.addEventListener("close",()=>c()),o&&y.addEventListener("versionchange",E=>o(E.oldVersion,E.newVersion,E))}).catch(()=>{}),p}const ka=["get","getKey","getAll","getAllKeys","count"],Pa=["put","add","delete","clear"],Wn=new Map;function Er(i,e){if(!(i instanceof IDBDatabase&&!(e in i)&&typeof e=="string"))return;if(Wn.get(e))return Wn.get(e);const n=e.replace(/FromIndex$/,""),r=e!==n,o=Pa.includes(n);if(!(n in(r?IDBIndex:IDBObjectStore).prototype)||!(o||ka.includes(n)))return;const c=async function(l,...p){const y=this.transaction(l,o?"readwrite":"readonly");let E=y.store;return r&&(E=E.index(p.shift())),(await Promise.all([E[n](...p),o&&y.done]))[0]};return Wn.set(e,c),c}Ta(i=>({...i,get:(e,n,r)=>Er(e,n)||i.get(e,n,r),has:(e,n)=>!!Er(e,n)||i.has(e,n)}));/**
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
 */class Ra{constructor(e){this.container=e}getPlatformInfoString(){return this.container.getProviders().map(n=>{if(Ca(n)){const r=n.getImmediate();return`${r.library}/${r.version}`}else return null}).filter(n=>n).join(" ")}}function Ca(i){const e=i.getComponent();return(e==null?void 0:e.type)==="VERSION"}const Yn="@firebase/app",Tr="0.10.13";/**
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
 */const me=new si("@firebase/app"),Oa="@firebase/app-compat",Da="@firebase/analytics-compat",Na="@firebase/analytics",La="@firebase/app-check-compat",Ma="@firebase/app-check",Ua="@firebase/auth",xa="@firebase/auth-compat",Fa="@firebase/database",ja="@firebase/data-connect",Ba="@firebase/database-compat",Va="@firebase/functions",Ha="@firebase/functions-compat",$a="@firebase/installations",Wa="@firebase/installations-compat",za="@firebase/messaging",Ga="@firebase/messaging-compat",Ka="@firebase/performance",qa="@firebase/performance-compat",Ja="@firebase/remote-config",Xa="@firebase/remote-config-compat",Ya="@firebase/storage",Qa="@firebase/storage-compat",Za="@firebase/firestore",ec="@firebase/vertexai-preview",tc="@firebase/firestore-compat",nc="firebase",ic="10.14.1";/**
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
 */const Qn="[DEFAULT]",rc={[Yn]:"fire-core",[Oa]:"fire-core-compat",[Na]:"fire-analytics",[Da]:"fire-analytics-compat",[Ma]:"fire-app-check",[La]:"fire-app-check-compat",[Ua]:"fire-auth",[xa]:"fire-auth-compat",[Fa]:"fire-rtdb",[ja]:"fire-data-connect",[Ba]:"fire-rtdb-compat",[Va]:"fire-fn",[Ha]:"fire-fn-compat",[$a]:"fire-iid",[Wa]:"fire-iid-compat",[za]:"fire-fcm",[Ga]:"fire-fcm-compat",[Ka]:"fire-perf",[qa]:"fire-perf-compat",[Ja]:"fire-rc",[Xa]:"fire-rc-compat",[Ya]:"fire-gcs",[Qa]:"fire-gcs-compat",[Za]:"fire-fst",[tc]:"fire-fst-compat",[ec]:"fire-vertex","fire-js":"fire-js",[nc]:"fire-js-all"};/**
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
 */const Et=new Map,sc=new Map,Zn=new Map;function Ar(i,e){try{i.container.addComponent(e)}catch(n){me.debug(`Component ${e.name} failed to register with FirebaseApp ${i.name}`,n)}}function xe(i){const e=i.name;if(Zn.has(e))return me.debug(`There were multiple attempts to register component ${e}.`),!1;Zn.set(e,i);for(const n of Et.values())Ar(n,i);for(const n of sc.values())Ar(n,i);return!0}function hn(i,e){const n=i.container.getProvider("heartbeat").getImmediate({optional:!0});return n&&n.triggerHeartbeat(),i.container.getProvider(e)}function de(i){return i.settings!==void 0}/**
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
 */const oc={"no-app":"No Firebase App '{$appName}' has been created - call initializeApp() first","bad-app-name":"Illegal App name: '{$appName}'","duplicate-app":"Firebase App named '{$appName}' already exists with different options or config","app-deleted":"Firebase App named '{$appName}' already deleted","server-app-deleted":"Firebase Server App has been deleted","no-options":"Need to provide options, when not being deployed to hosting via source.","invalid-app-argument":"firebase.{$appName}() takes either no argument or a Firebase App instance.","invalid-log-argument":"First argument to `onLog` must be null or a function.","idb-open":"Error thrown when opening IndexedDB. Original error: {$originalErrorMessage}.","idb-get":"Error thrown when reading from IndexedDB. Original error: {$originalErrorMessage}.","idb-set":"Error thrown when writing to IndexedDB. Original error: {$originalErrorMessage}.","idb-delete":"Error thrown when deleting from IndexedDB. Original error: {$originalErrorMessage}.","finalization-registry-not-supported":"FirebaseServerApp deleteOnDeref field defined but the JS runtime does not support FinalizationRegistry.","invalid-server-app-environment":"FirebaseServerApp is not for use in browser environments."},Pe=new bt("app","Firebase",oc);/**
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
 */class ac{constructor(e,n,r){this._isDeleted=!1,this._options=Object.assign({},e),this._config=Object.assign({},n),this._name=n.name,this._automaticDataCollectionEnabled=n.automaticDataCollectionEnabled,this._container=r,this.container.addComponent(new Ce("app",()=>this,"PUBLIC"))}get automaticDataCollectionEnabled(){return this.checkDestroyed(),this._automaticDataCollectionEnabled}set automaticDataCollectionEnabled(e){this.checkDestroyed(),this._automaticDataCollectionEnabled=e}get name(){return this.checkDestroyed(),this._name}get options(){return this.checkDestroyed(),this._options}get config(){return this.checkDestroyed(),this._config}get container(){return this._container}get isDeleted(){return this._isDeleted}set isDeleted(e){this._isDeleted=e}checkDestroyed(){if(this.isDeleted)throw Pe.create("app-deleted",{appName:this._name})}}/**
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
 */const Ye=ic;function fs(i,e={}){let n=i;typeof e!="object"&&(e={name:e});const r=Object.assign({name:Qn,automaticDataCollectionEnabled:!1},e),o=r.name;if(typeof o!="string"||!o)throw Pe.create("bad-app-name",{appName:String(o)});if(n||(n=hs()),!n)throw Pe.create("no-options");const c=Et.get(o);if(c){if(tn(n,c.options)&&tn(r,c.config))return c;throw Pe.create("duplicate-app",{appName:o})}const l=new fa(o);for(const y of Zn.values())l.addComponent(y);const p=new ac(n,r,l);return Et.set(o,p),p}function ln(i=Qn){const e=Et.get(i);if(!e&&i===Qn&&hs())return fs();if(!e)throw Pe.create("no-app",{appName:i});return e}function cc(){return Array.from(Et.values())}function re(i,e,n){var r;let o=(r=rc[i])!==null&&r!==void 0?r:i;n&&(o+=`-${n}`);const c=o.match(/\s|\//),l=e.match(/\s|\//);if(c||l){const p=[`Unable to register library "${o}" with version "${e}":`];c&&p.push(`library name "${o}" contains illegal characters (whitespace or "/")`),c&&l&&p.push("and"),l&&p.push(`version name "${e}" contains illegal characters (whitespace or "/")`),me.warn(p.join(" "));return}xe(new Ce(`${o}-version`,()=>({library:o,version:e}),"VERSION"))}/**
 * @license
 * Copyright 2021 Google LLC
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
 */const hc="firebase-heartbeat-database",lc=1,Tt="firebase-heartbeat-store";let zn=null;function ps(){return zn||(zn=ba(hc,lc,{upgrade:(i,e)=>{switch(e){case 0:try{i.createObjectStore(Tt)}catch(n){console.warn(n)}}}}).catch(i=>{throw Pe.create("idb-open",{originalErrorMessage:i.message})})),zn}async function uc(i){try{const n=(await ps()).transaction(Tt),r=await n.objectStore(Tt).get(gs(i));return await n.done,r}catch(e){if(e instanceof ie)me.warn(e.message);else{const n=Pe.create("idb-get",{originalErrorMessage:e==null?void 0:e.message});me.warn(n.message)}}}async function Sr(i,e){try{const r=(await ps()).transaction(Tt,"readwrite");await r.objectStore(Tt).put(e,gs(i)),await r.done}catch(n){if(n instanceof ie)me.warn(n.message);else{const r=Pe.create("idb-set",{originalErrorMessage:n==null?void 0:n.message});me.warn(r.message)}}}function gs(i){return`${i.name}!${i.options.appId}`}/**
 * @license
 * Copyright 2021 Google LLC
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
 */const dc=1024,fc=30*24*60*60*1e3;class pc{constructor(e){this.container=e,this._heartbeatsCache=null;const n=this.container.getProvider("app").getImmediate();this._storage=new mc(n),this._heartbeatsCachePromise=this._storage.read().then(r=>(this._heartbeatsCache=r,r))}async triggerHeartbeat(){var e,n;try{const o=this.container.getProvider("platform-logger").getImmediate().getPlatformInfoString(),c=br();return((e=this._heartbeatsCache)===null||e===void 0?void 0:e.heartbeats)==null&&(this._heartbeatsCache=await this._heartbeatsCachePromise,((n=this._heartbeatsCache)===null||n===void 0?void 0:n.heartbeats)==null)||this._heartbeatsCache.lastSentHeartbeatDate===c||this._heartbeatsCache.heartbeats.some(l=>l.date===c)?void 0:(this._heartbeatsCache.heartbeats.push({date:c,agent:o}),this._heartbeatsCache.heartbeats=this._heartbeatsCache.heartbeats.filter(l=>{const p=new Date(l.date).valueOf();return Date.now()-p<=fc}),this._storage.overwrite(this._heartbeatsCache))}catch(r){me.warn(r)}}async getHeartbeatsHeader(){var e;try{if(this._heartbeatsCache===null&&await this._heartbeatsCachePromise,((e=this._heartbeatsCache)===null||e===void 0?void 0:e.heartbeats)==null||this._heartbeatsCache.heartbeats.length===0)return"";const n=br(),{heartbeatsToSend:r,unsentEntries:o}=gc(this._heartbeatsCache.heartbeats),c=en(JSON.stringify({version:2,heartbeats:r}));return this._heartbeatsCache.lastSentHeartbeatDate=n,o.length>0?(this._heartbeatsCache.heartbeats=o,await this._storage.overwrite(this._heartbeatsCache)):(this._heartbeatsCache.heartbeats=[],this._storage.overwrite(this._heartbeatsCache)),c}catch(n){return me.warn(n),""}}}function br(){return new Date().toISOString().substring(0,10)}function gc(i,e=dc){const n=[];let r=i.slice();for(const o of i){const c=n.find(l=>l.agent===o.agent);if(c){if(c.dates.push(o.date),kr(n)>e){c.dates.pop();break}}else if(n.push({agent:o.agent,dates:[o.date]}),kr(n)>e){n.pop();break}r=r.slice(1)}return{heartbeatsToSend:n,unsentEntries:r}}class mc{constructor(e){this.app=e,this._canUseIndexedDBPromise=this.runIndexedDBEnvironmentCheck()}async runIndexedDBEnvironmentCheck(){return ta()?na().then(()=>!0).catch(()=>!1):!1}async read(){if(await this._canUseIndexedDBPromise){const n=await uc(this.app);return n!=null&&n.heartbeats?n:{heartbeats:[]}}else return{heartbeats:[]}}async overwrite(e){var n;if(await this._canUseIndexedDBPromise){const o=await this.read();return Sr(this.app,{lastSentHeartbeatDate:(n=e.lastSentHeartbeatDate)!==null&&n!==void 0?n:o.lastSentHeartbeatDate,heartbeats:e.heartbeats})}else return}async add(e){var n;if(await this._canUseIndexedDBPromise){const o=await this.read();return Sr(this.app,{lastSentHeartbeatDate:(n=e.lastSentHeartbeatDate)!==null&&n!==void 0?n:o.lastSentHeartbeatDate,heartbeats:[...o.heartbeats,...e.heartbeats]})}else return}}function kr(i){return en(JSON.stringify({version:2,heartbeats:i})).length}/**
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
 */function vc(i){xe(new Ce("platform-logger",e=>new Ra(e),"PRIVATE")),xe(new Ce("heartbeat",e=>new pc(e),"PRIVATE")),re(Yn,Tr,i),re(Yn,Tr,"esm2017"),re("fire-js","")}vc("");var _c="firebase",yc="10.14.1";/**
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
 */re(_c,yc,"app");function ai(i,e){var n={};for(var r in i)Object.prototype.hasOwnProperty.call(i,r)&&e.indexOf(r)<0&&(n[r]=i[r]);if(i!=null&&typeof Object.getOwnPropertySymbols=="function")for(var o=0,r=Object.getOwnPropertySymbols(i);o<r.length;o++)e.indexOf(r[o])<0&&Object.prototype.propertyIsEnumerable.call(i,r[o])&&(n[r[o]]=i[r[o]]);return n}function ms(){return{"dependent-sdk-initialized-before-auth":"Another Firebase SDK was initialized and is trying to use Auth before Auth is initialized. Please be sure to call `initializeAuth` or `getAuth` before starting any other Firebase SDK."}}const wc=ms,vs=new bt("auth","Firebase",ms());/**
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
 */const nn=new si("@firebase/auth");function Ic(i,...e){nn.logLevel<=O.WARN&&nn.warn(`Auth (${Ye}): ${i}`,...e)}function Xt(i,...e){nn.logLevel<=O.ERROR&&nn.error(`Auth (${Ye}): ${i}`,...e)}/**
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
 */function ne(i,...e){throw ci(i,...e)}function se(i,...e){return ci(i,...e)}function _s(i,e,n){const r=Object.assign(Object.assign({},wc()),{[e]:n});return new bt("auth","Firebase",r).create(e,{appName:i.name})}function Re(i){return _s(i,"operation-not-supported-in-this-environment","Operations that alter the current user are not supported in conjunction with FirebaseServerApp")}function ci(i,...e){if(typeof i!="string"){const n=e[0],r=[...e.slice(1)];return r[0]&&(r[0].appName=i.name),i._errorFactory.create(n,...r)}return vs.create(i,...e)}function A(i,e,...n){if(!i)throw ci(e,...n)}function fe(i){const e="INTERNAL ASSERTION FAILED: "+i;throw Xt(e),new Error(e)}function ve(i,e){i||fe(e)}/**
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
 */function ei(){var i;return typeof self<"u"&&((i=self.location)===null||i===void 0?void 0:i.href)||""}function Ec(){return Pr()==="http:"||Pr()==="https:"}function Pr(){var i;return typeof self<"u"&&((i=self.location)===null||i===void 0?void 0:i.protocol)||null}/**
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
 */function Tc(){return typeof navigator<"u"&&navigator&&"onLine"in navigator&&typeof navigator.onLine=="boolean"&&(Ec()||Qo()||"connection"in navigator)?navigator.onLine:!0}function Ac(){if(typeof navigator>"u")return null;const i=navigator;return i.languages&&i.languages[0]||i.language||null}/**
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
 */class Pt{constructor(e,n){this.shortDelay=e,this.longDelay=n,ve(n>e,"Short delay should be less than long delay!"),this.isMobile=Xo()||Zo()}get(){return Tc()?this.isMobile?this.longDelay:this.shortDelay:Math.min(5e3,this.shortDelay)}}/**
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
 */function hi(i,e){ve(i.emulator,"Emulator should always be set here");const{url:n}=i.emulator;return e?`${n}${e.startsWith("/")?e.slice(1):e}`:n}/**
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
 */class ys{static initialize(e,n,r){this.fetchImpl=e,n&&(this.headersImpl=n),r&&(this.responseImpl=r)}static fetch(){if(this.fetchImpl)return this.fetchImpl;if(typeof self<"u"&&"fetch"in self)return self.fetch;if(typeof globalThis<"u"&&globalThis.fetch)return globalThis.fetch;if(typeof fetch<"u")return fetch;fe("Could not find fetch implementation, make sure you call FetchProvider.initialize() with an appropriate polyfill")}static headers(){if(this.headersImpl)return this.headersImpl;if(typeof self<"u"&&"Headers"in self)return self.Headers;if(typeof globalThis<"u"&&globalThis.Headers)return globalThis.Headers;if(typeof Headers<"u")return Headers;fe("Could not find Headers implementation, make sure you call FetchProvider.initialize() with an appropriate polyfill")}static response(){if(this.responseImpl)return this.responseImpl;if(typeof self<"u"&&"Response"in self)return self.Response;if(typeof globalThis<"u"&&globalThis.Response)return globalThis.Response;if(typeof Response<"u")return Response;fe("Could not find Response implementation, make sure you call FetchProvider.initialize() with an appropriate polyfill")}}/**
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
 */const Sc={CREDENTIAL_MISMATCH:"custom-token-mismatch",MISSING_CUSTOM_TOKEN:"internal-error",INVALID_IDENTIFIER:"invalid-email",MISSING_CONTINUE_URI:"internal-error",INVALID_PASSWORD:"wrong-password",MISSING_PASSWORD:"missing-password",INVALID_LOGIN_CREDENTIALS:"invalid-credential",EMAIL_EXISTS:"email-already-in-use",PASSWORD_LOGIN_DISABLED:"operation-not-allowed",INVALID_IDP_RESPONSE:"invalid-credential",INVALID_PENDING_TOKEN:"invalid-credential",FEDERATED_USER_ID_ALREADY_LINKED:"credential-already-in-use",MISSING_REQ_TYPE:"internal-error",EMAIL_NOT_FOUND:"user-not-found",RESET_PASSWORD_EXCEED_LIMIT:"too-many-requests",EXPIRED_OOB_CODE:"expired-action-code",INVALID_OOB_CODE:"invalid-action-code",MISSING_OOB_CODE:"internal-error",CREDENTIAL_TOO_OLD_LOGIN_AGAIN:"requires-recent-login",INVALID_ID_TOKEN:"invalid-user-token",TOKEN_EXPIRED:"user-token-expired",USER_NOT_FOUND:"user-token-expired",TOO_MANY_ATTEMPTS_TRY_LATER:"too-many-requests",PASSWORD_DOES_NOT_MEET_REQUIREMENTS:"password-does-not-meet-requirements",INVALID_CODE:"invalid-verification-code",INVALID_SESSION_INFO:"invalid-verification-id",INVALID_TEMPORARY_PROOF:"invalid-credential",MISSING_SESSION_INFO:"missing-verification-id",SESSION_EXPIRED:"code-expired",MISSING_ANDROID_PACKAGE_NAME:"missing-android-pkg-name",UNAUTHORIZED_DOMAIN:"unauthorized-continue-uri",INVALID_OAUTH_CLIENT_ID:"invalid-oauth-client-id",ADMIN_ONLY_OPERATION:"admin-restricted-operation",INVALID_MFA_PENDING_CREDENTIAL:"invalid-multi-factor-session",MFA_ENROLLMENT_NOT_FOUND:"multi-factor-info-not-found",MISSING_MFA_ENROLLMENT_ID:"missing-multi-factor-info",MISSING_MFA_PENDING_CREDENTIAL:"missing-multi-factor-session",SECOND_FACTOR_EXISTS:"second-factor-already-in-use",SECOND_FACTOR_LIMIT_EXCEEDED:"maximum-second-factor-count-exceeded",BLOCKING_FUNCTION_ERROR_RESPONSE:"internal-error",RECAPTCHA_NOT_ENABLED:"recaptcha-not-enabled",MISSING_RECAPTCHA_TOKEN:"missing-recaptcha-token",INVALID_RECAPTCHA_TOKEN:"invalid-recaptcha-token",INVALID_RECAPTCHA_ACTION:"invalid-recaptcha-action",MISSING_CLIENT_TYPE:"missing-client-type",MISSING_RECAPTCHA_VERSION:"missing-recaptcha-version",INVALID_RECAPTCHA_VERSION:"invalid-recaptcha-version",INVALID_REQ_TYPE:"invalid-req-type"};/**
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
 */const bc=new Pt(3e4,6e4);function je(i,e){return i.tenantId&&!e.tenantId?Object.assign(Object.assign({},e),{tenantId:i.tenantId}):e}async function Oe(i,e,n,r,o={}){return ws(i,o,async()=>{let c={},l={};r&&(e==="GET"?l=r:c={body:JSON.stringify(r)});const p=kt(Object.assign({key:i.config.apiKey},l)).slice(1),y=await i._getAdditionalHeaders();y["Content-Type"]="application/json",i.languageCode&&(y["X-Firebase-Locale"]=i.languageCode);const E=Object.assign({method:e,headers:y},c);return Yo()||(E.referrerPolicy="no-referrer"),ys.fetch()(Is(i,i.config.apiHost,n,p),E)})}async function ws(i,e,n){i._canInitEmulator=!1;const r=Object.assign(Object.assign({},Sc),e);try{const o=new Pc(i),c=await Promise.race([n(),o.promise]);o.clearNetworkTimeout();const l=await c.json();if("needConfirmation"in l)throw qt(i,"account-exists-with-different-credential",l);if(c.ok&&!("errorMessage"in l))return l;{const p=c.ok?l.errorMessage:l.error.message,[y,E]=p.split(" : ");if(y==="FEDERATED_USER_ID_ALREADY_LINKED")throw qt(i,"credential-already-in-use",l);if(y==="EMAIL_EXISTS")throw qt(i,"email-already-in-use",l);if(y==="USER_DISABLED")throw qt(i,"user-disabled",l);const S=r[y]||y.toLowerCase().replace(/[_\s]+/g,"-");if(E)throw _s(i,S,E);ne(i,S)}}catch(o){if(o instanceof ie)throw o;ne(i,"network-request-failed",{message:String(o)})}}async function un(i,e,n,r,o={}){const c=await Oe(i,e,n,r,o);return"mfaPendingCredential"in c&&ne(i,"multi-factor-auth-required",{_serverResponse:c}),c}function Is(i,e,n,r){const o=`${e}${n}?${r}`;return i.config.emulator?hi(i.config,o):`${i.config.apiScheme}://${o}`}function kc(i){switch(i){case"ENFORCE":return"ENFORCE";case"AUDIT":return"AUDIT";case"OFF":return"OFF";default:return"ENFORCEMENT_STATE_UNSPECIFIED"}}class Pc{constructor(e){this.auth=e,this.timer=null,this.promise=new Promise((n,r)=>{this.timer=setTimeout(()=>r(se(this.auth,"network-request-failed")),bc.get())})}clearNetworkTimeout(){clearTimeout(this.timer)}}function qt(i,e,n){const r={appName:i.name};n.email&&(r.email=n.email),n.phoneNumber&&(r.phoneNumber=n.phoneNumber);const o=se(i,e,r);return o.customData._tokenResponse=n,o}function Rr(i){return i!==void 0&&i.enterprise!==void 0}class Rc{constructor(e){if(this.siteKey="",this.recaptchaEnforcementState=[],e.recaptchaKey===void 0)throw new Error("recaptchaKey undefined");this.siteKey=e.recaptchaKey.split("/")[3],this.recaptchaEnforcementState=e.recaptchaEnforcementState}getProviderEnforcementState(e){if(!this.recaptchaEnforcementState||this.recaptchaEnforcementState.length===0)return null;for(const n of this.recaptchaEnforcementState)if(n.provider&&n.provider===e)return kc(n.enforcementState);return null}isProviderEnabled(e){return this.getProviderEnforcementState(e)==="ENFORCE"||this.getProviderEnforcementState(e)==="AUDIT"}}async function Cc(i,e){return Oe(i,"GET","/v2/recaptchaConfig",je(i,e))}/**
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
 */async function Oc(i,e){return Oe(i,"POST","/v1/accounts:delete",e)}async function Es(i,e){return Oe(i,"POST","/v1/accounts:lookup",e)}/**
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
 */function yt(i){if(i)try{const e=new Date(Number(i));if(!isNaN(e.getTime()))return e.toUTCString()}catch{}}async function Ts(i,e=!1){const n=ae(i),r=await n.getIdToken(e),o=li(r);A(o&&o.exp&&o.auth_time&&o.iat,n.auth,"internal-error");const c=typeof o.firebase=="object"?o.firebase:void 0,l=c==null?void 0:c.sign_in_provider;return{claims:o,token:r,authTime:yt(Gn(o.auth_time)),issuedAtTime:yt(Gn(o.iat)),expirationTime:yt(Gn(o.exp)),signInProvider:l||null,signInSecondFactor:(c==null?void 0:c.sign_in_second_factor)||null}}function Gn(i){return Number(i)*1e3}function li(i){const[e,n,r]=i.split(".");if(e===void 0||n===void 0||r===void 0)return Xt("JWT malformed, contained fewer than 3 sections"),null;try{const o=os(n);return o?JSON.parse(o):(Xt("Failed to decode base64 JWT payload"),null)}catch(o){return Xt("Caught error parsing JWT payload as JSON",o==null?void 0:o.toString()),null}}function Cr(i){const e=li(i);return A(e,"internal-error"),A(typeof e.exp<"u","internal-error"),A(typeof e.iat<"u","internal-error"),Number(e.exp)-Number(e.iat)}/**
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
 */async function At(i,e,n=!1){if(n)return e;try{return await e}catch(r){throw r instanceof ie&&Dc(r)&&i.auth.currentUser===i&&await i.auth.signOut(),r}}function Dc({code:i}){return i==="auth/user-disabled"||i==="auth/user-token-expired"}/**
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
 */class Nc{constructor(e){this.user=e,this.isRunning=!1,this.timerId=null,this.errorBackoff=3e4}_start(){this.isRunning||(this.isRunning=!0,this.schedule())}_stop(){this.isRunning&&(this.isRunning=!1,this.timerId!==null&&clearTimeout(this.timerId))}getInterval(e){var n;if(e){const r=this.errorBackoff;return this.errorBackoff=Math.min(this.errorBackoff*2,96e4),r}else{this.errorBackoff=3e4;const o=((n=this.user.stsTokenManager.expirationTime)!==null&&n!==void 0?n:0)-Date.now()-3e5;return Math.max(0,o)}}schedule(e=!1){if(!this.isRunning)return;const n=this.getInterval(e);this.timerId=setTimeout(async()=>{await this.iteration()},n)}async iteration(){try{await this.user.getIdToken(!0)}catch(e){(e==null?void 0:e.code)==="auth/network-request-failed"&&this.schedule(!0);return}this.schedule()}}/**
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
 */class ti{constructor(e,n){this.createdAt=e,this.lastLoginAt=n,this._initializeTime()}_initializeTime(){this.lastSignInTime=yt(this.lastLoginAt),this.creationTime=yt(this.createdAt)}_copy(e){this.createdAt=e.createdAt,this.lastLoginAt=e.lastLoginAt,this._initializeTime()}toJSON(){return{createdAt:this.createdAt,lastLoginAt:this.lastLoginAt}}}/**
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
 */async function rn(i){var e;const n=i.auth,r=await i.getIdToken(),o=await At(i,Es(n,{idToken:r}));A(o==null?void 0:o.users.length,n,"internal-error");const c=o.users[0];i._notifyReloadListener(c);const l=!((e=c.providerUserInfo)===null||e===void 0)&&e.length?As(c.providerUserInfo):[],p=Mc(i.providerData,l),y=i.isAnonymous,E=!(i.email&&c.passwordHash)&&!(p!=null&&p.length),S=y?E:!1,b={uid:c.localId,displayName:c.displayName||null,photoURL:c.photoUrl||null,email:c.email||null,emailVerified:c.emailVerified||!1,phoneNumber:c.phoneNumber||null,tenantId:c.tenantId||null,providerData:p,metadata:new ti(c.createdAt,c.lastLoginAt),isAnonymous:S};Object.assign(i,b)}async function Lc(i){const e=ae(i);await rn(e),await e.auth._persistUserIfCurrent(e),e.auth._notifyListenersIfCurrent(e)}function Mc(i,e){return[...i.filter(r=>!e.some(o=>o.providerId===r.providerId)),...e]}function As(i){return i.map(e=>{var{providerId:n}=e,r=ai(e,["providerId"]);return{providerId:n,uid:r.rawId||"",displayName:r.displayName||null,email:r.email||null,phoneNumber:r.phoneNumber||null,photoURL:r.photoUrl||null}})}/**
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
 */async function Uc(i,e){const n=await ws(i,{},async()=>{const r=kt({grant_type:"refresh_token",refresh_token:e}).slice(1),{tokenApiHost:o,apiKey:c}=i.config,l=Is(i,o,"/v1/token",`key=${c}`),p=await i._getAdditionalHeaders();return p["Content-Type"]="application/x-www-form-urlencoded",ys.fetch()(l,{method:"POST",headers:p,body:r})});return{accessToken:n.access_token,expiresIn:n.expires_in,refreshToken:n.refresh_token}}async function xc(i,e){return Oe(i,"POST","/v2/accounts:revokeToken",je(i,e))}/**
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
 */class Ge{constructor(){this.refreshToken=null,this.accessToken=null,this.expirationTime=null}get isExpired(){return!this.expirationTime||Date.now()>this.expirationTime-3e4}updateFromServerResponse(e){A(e.idToken,"internal-error"),A(typeof e.idToken<"u","internal-error"),A(typeof e.refreshToken<"u","internal-error");const n="expiresIn"in e&&typeof e.expiresIn<"u"?Number(e.expiresIn):Cr(e.idToken);this.updateTokensAndExpiration(e.idToken,e.refreshToken,n)}updateFromIdToken(e){A(e.length!==0,"internal-error");const n=Cr(e);this.updateTokensAndExpiration(e,null,n)}async getToken(e,n=!1){return!n&&this.accessToken&&!this.isExpired?this.accessToken:(A(this.refreshToken,e,"user-token-expired"),this.refreshToken?(await this.refresh(e,this.refreshToken),this.accessToken):null)}clearRefreshToken(){this.refreshToken=null}async refresh(e,n){const{accessToken:r,refreshToken:o,expiresIn:c}=await Uc(e,n);this.updateTokensAndExpiration(r,o,Number(c))}updateTokensAndExpiration(e,n,r){this.refreshToken=n||null,this.accessToken=e||null,this.expirationTime=Date.now()+r*1e3}static fromJSON(e,n){const{refreshToken:r,accessToken:o,expirationTime:c}=n,l=new Ge;return r&&(A(typeof r=="string","internal-error",{appName:e}),l.refreshToken=r),o&&(A(typeof o=="string","internal-error",{appName:e}),l.accessToken=o),c&&(A(typeof c=="number","internal-error",{appName:e}),l.expirationTime=c),l}toJSON(){return{refreshToken:this.refreshToken,accessToken:this.accessToken,expirationTime:this.expirationTime}}_assign(e){this.accessToken=e.accessToken,this.refreshToken=e.refreshToken,this.expirationTime=e.expirationTime}_clone(){return Object.assign(new Ge,this.toJSON())}_performRefresh(){return fe("not implemented")}}/**
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
 */function Ee(i,e){A(typeof i=="string"||typeof i>"u","internal-error",{appName:e})}class pe{constructor(e){var{uid:n,auth:r,stsTokenManager:o}=e,c=ai(e,["uid","auth","stsTokenManager"]);this.providerId="firebase",this.proactiveRefresh=new Nc(this),this.reloadUserInfo=null,this.reloadListener=null,this.uid=n,this.auth=r,this.stsTokenManager=o,this.accessToken=o.accessToken,this.displayName=c.displayName||null,this.email=c.email||null,this.emailVerified=c.emailVerified||!1,this.phoneNumber=c.phoneNumber||null,this.photoURL=c.photoURL||null,this.isAnonymous=c.isAnonymous||!1,this.tenantId=c.tenantId||null,this.providerData=c.providerData?[...c.providerData]:[],this.metadata=new ti(c.createdAt||void 0,c.lastLoginAt||void 0)}async getIdToken(e){const n=await At(this,this.stsTokenManager.getToken(this.auth,e));return A(n,this.auth,"internal-error"),this.accessToken!==n&&(this.accessToken=n,await this.auth._persistUserIfCurrent(this),this.auth._notifyListenersIfCurrent(this)),n}getIdTokenResult(e){return Ts(this,e)}reload(){return Lc(this)}_assign(e){this!==e&&(A(this.uid===e.uid,this.auth,"internal-error"),this.displayName=e.displayName,this.photoURL=e.photoURL,this.email=e.email,this.emailVerified=e.emailVerified,this.phoneNumber=e.phoneNumber,this.isAnonymous=e.isAnonymous,this.tenantId=e.tenantId,this.providerData=e.providerData.map(n=>Object.assign({},n)),this.metadata._copy(e.metadata),this.stsTokenManager._assign(e.stsTokenManager))}_clone(e){const n=new pe(Object.assign(Object.assign({},this),{auth:e,stsTokenManager:this.stsTokenManager._clone()}));return n.metadata._copy(this.metadata),n}_onReload(e){A(!this.reloadListener,this.auth,"internal-error"),this.reloadListener=e,this.reloadUserInfo&&(this._notifyReloadListener(this.reloadUserInfo),this.reloadUserInfo=null)}_notifyReloadListener(e){this.reloadListener?this.reloadListener(e):this.reloadUserInfo=e}_startProactiveRefresh(){this.proactiveRefresh._start()}_stopProactiveRefresh(){this.proactiveRefresh._stop()}async _updateTokensIfNecessary(e,n=!1){let r=!1;e.idToken&&e.idToken!==this.stsTokenManager.accessToken&&(this.stsTokenManager.updateFromServerResponse(e),r=!0),n&&await rn(this),await this.auth._persistUserIfCurrent(this),r&&this.auth._notifyListenersIfCurrent(this)}async delete(){if(de(this.auth.app))return Promise.reject(Re(this.auth));const e=await this.getIdToken();return await At(this,Oc(this.auth,{idToken:e})),this.stsTokenManager.clearRefreshToken(),this.auth.signOut()}toJSON(){return Object.assign(Object.assign({uid:this.uid,email:this.email||void 0,emailVerified:this.emailVerified,displayName:this.displayName||void 0,isAnonymous:this.isAnonymous,photoURL:this.photoURL||void 0,phoneNumber:this.phoneNumber||void 0,tenantId:this.tenantId||void 0,providerData:this.providerData.map(e=>Object.assign({},e)),stsTokenManager:this.stsTokenManager.toJSON(),_redirectEventId:this._redirectEventId},this.metadata.toJSON()),{apiKey:this.auth.config.apiKey,appName:this.auth.name})}get refreshToken(){return this.stsTokenManager.refreshToken||""}static _fromJSON(e,n){var r,o,c,l,p,y,E,S;const b=(r=n.displayName)!==null&&r!==void 0?r:void 0,R=(o=n.email)!==null&&o!==void 0?o:void 0,x=(c=n.phoneNumber)!==null&&c!==void 0?c:void 0,P=(l=n.photoURL)!==null&&l!==void 0?l:void 0,U=(p=n.tenantId)!==null&&p!==void 0?p:void 0,L=(y=n._redirectEventId)!==null&&y!==void 0?y:void 0,ce=(E=n.createdAt)!==null&&E!==void 0?E:void 0,Y=(S=n.lastLoginAt)!==null&&S!==void 0?S:void 0,{uid:j,emailVerified:Z,isAnonymous:De,providerData:q,stsTokenManager:v}=n;A(j&&v,e,"internal-error");const u=Ge.fromJSON(this.name,v);A(typeof j=="string",e,"internal-error"),Ee(b,e.name),Ee(R,e.name),A(typeof Z=="boolean",e,"internal-error"),A(typeof De=="boolean",e,"internal-error"),Ee(x,e.name),Ee(P,e.name),Ee(U,e.name),Ee(L,e.name),Ee(ce,e.name),Ee(Y,e.name);const f=new pe({uid:j,auth:e,email:R,emailVerified:Z,displayName:b,isAnonymous:De,photoURL:P,phoneNumber:x,tenantId:U,stsTokenManager:u,createdAt:ce,lastLoginAt:Y});return q&&Array.isArray(q)&&(f.providerData=q.map(g=>Object.assign({},g))),L&&(f._redirectEventId=L),f}static async _fromIdTokenResponse(e,n,r=!1){const o=new Ge;o.updateFromServerResponse(n);const c=new pe({uid:n.localId,auth:e,stsTokenManager:o,isAnonymous:r});return await rn(c),c}static async _fromGetAccountInfoResponse(e,n,r){const o=n.users[0];A(o.localId!==void 0,"internal-error");const c=o.providerUserInfo!==void 0?As(o.providerUserInfo):[],l=!(o.email&&o.passwordHash)&&!(c!=null&&c.length),p=new Ge;p.updateFromIdToken(r);const y=new pe({uid:o.localId,auth:e,stsTokenManager:p,isAnonymous:l}),E={uid:o.localId,displayName:o.displayName||null,photoURL:o.photoUrl||null,email:o.email||null,emailVerified:o.emailVerified||!1,phoneNumber:o.phoneNumber||null,tenantId:o.tenantId||null,providerData:c,metadata:new ti(o.createdAt,o.lastLoginAt),isAnonymous:!(o.email&&o.passwordHash)&&!(c!=null&&c.length)};return Object.assign(y,E),y}}/**
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
 */const Or=new Map;function ge(i){ve(i instanceof Function,"Expected a class definition");let e=Or.get(i);return e?(ve(e instanceof i,"Instance stored in cache mismatched with class"),e):(e=new i,Or.set(i,e),e)}/**
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
 */class Ss{constructor(){this.type="NONE",this.storage={}}async _isAvailable(){return!0}async _set(e,n){this.storage[e]=n}async _get(e){const n=this.storage[e];return n===void 0?null:n}async _remove(e){delete this.storage[e]}_addListener(e,n){}_removeListener(e,n){}}Ss.type="NONE";const Dr=Ss;/**
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
 */function Yt(i,e,n){return`firebase:${i}:${e}:${n}`}class Ke{constructor(e,n,r){this.persistence=e,this.auth=n,this.userKey=r;const{config:o,name:c}=this.auth;this.fullUserKey=Yt(this.userKey,o.apiKey,c),this.fullPersistenceKey=Yt("persistence",o.apiKey,c),this.boundEventHandler=n._onStorageEvent.bind(n),this.persistence._addListener(this.fullUserKey,this.boundEventHandler)}setCurrentUser(e){return this.persistence._set(this.fullUserKey,e.toJSON())}async getCurrentUser(){const e=await this.persistence._get(this.fullUserKey);return e?pe._fromJSON(this.auth,e):null}removeCurrentUser(){return this.persistence._remove(this.fullUserKey)}savePersistenceForRedirect(){return this.persistence._set(this.fullPersistenceKey,this.persistence.type)}async setPersistence(e){if(this.persistence===e)return;const n=await this.getCurrentUser();if(await this.removeCurrentUser(),this.persistence=e,n)return this.setCurrentUser(n)}delete(){this.persistence._removeListener(this.fullUserKey,this.boundEventHandler)}static async create(e,n,r="authUser"){if(!n.length)return new Ke(ge(Dr),e,r);const o=(await Promise.all(n.map(async E=>{if(await E._isAvailable())return E}))).filter(E=>E);let c=o[0]||ge(Dr);const l=Yt(r,e.config.apiKey,e.name);let p=null;for(const E of n)try{const S=await E._get(l);if(S){const b=pe._fromJSON(e,S);E!==c&&(p=b),c=E;break}}catch{}const y=o.filter(E=>E._shouldAllowMigration);return!c._shouldAllowMigration||!y.length?new Ke(c,e,r):(c=y[0],p&&await c._set(l,p.toJSON()),await Promise.all(n.map(async E=>{if(E!==c)try{await E._remove(l)}catch{}})),new Ke(c,e,r))}}/**
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
 */function Nr(i){const e=i.toLowerCase();if(e.includes("opera/")||e.includes("opr/")||e.includes("opios/"))return"Opera";if(Rs(e))return"IEMobile";if(e.includes("msie")||e.includes("trident/"))return"IE";if(e.includes("edge/"))return"Edge";if(bs(e))return"Firefox";if(e.includes("silk/"))return"Silk";if(Os(e))return"Blackberry";if(Ds(e))return"Webos";if(ks(e))return"Safari";if((e.includes("chrome/")||Ps(e))&&!e.includes("edge/"))return"Chrome";if(Cs(e))return"Android";{const n=/([a-zA-Z\d\.]+)\/[a-zA-Z\d\.]*$/,r=i.match(n);if((r==null?void 0:r.length)===2)return r[1]}return"Other"}function bs(i=K()){return/firefox\//i.test(i)}function ks(i=K()){const e=i.toLowerCase();return e.includes("safari/")&&!e.includes("chrome/")&&!e.includes("crios/")&&!e.includes("android")}function Ps(i=K()){return/crios\//i.test(i)}function Rs(i=K()){return/iemobile/i.test(i)}function Cs(i=K()){return/android/i.test(i)}function Os(i=K()){return/blackberry/i.test(i)}function Ds(i=K()){return/webos/i.test(i)}function ui(i=K()){return/iphone|ipad|ipod/i.test(i)||/macintosh/i.test(i)&&/mobile/i.test(i)}function Fc(i=K()){var e;return ui(i)&&!!(!((e=window.navigator)===null||e===void 0)&&e.standalone)}function jc(){return ea()&&document.documentMode===10}function Ns(i=K()){return ui(i)||Cs(i)||Ds(i)||Os(i)||/windows phone/i.test(i)||Rs(i)}/**
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
 */function Ls(i,e=[]){let n;switch(i){case"Browser":n=Nr(K());break;case"Worker":n=`${Nr(K())}-${i}`;break;default:n=i}const r=e.length?e.join(","):"FirebaseCore-web";return`${n}/JsCore/${Ye}/${r}`}/**
 * @license
 * Copyright 2022 Google LLC
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
 */class Bc{constructor(e){this.auth=e,this.queue=[]}pushCallback(e,n){const r=c=>new Promise((l,p)=>{try{const y=e(c);l(y)}catch(y){p(y)}});r.onAbort=n,this.queue.push(r);const o=this.queue.length-1;return()=>{this.queue[o]=()=>Promise.resolve()}}async runMiddleware(e){if(this.auth.currentUser===e)return;const n=[];try{for(const r of this.queue)await r(e),r.onAbort&&n.push(r.onAbort)}catch(r){n.reverse();for(const o of n)try{o()}catch{}throw this.auth._errorFactory.create("login-blocked",{originalMessage:r==null?void 0:r.message})}}}/**
 * @license
 * Copyright 2023 Google LLC
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
 */async function Vc(i,e={}){return Oe(i,"GET","/v2/passwordPolicy",je(i,e))}/**
 * @license
 * Copyright 2023 Google LLC
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
 */const Hc=6;class $c{constructor(e){var n,r,o,c;const l=e.customStrengthOptions;this.customStrengthOptions={},this.customStrengthOptions.minPasswordLength=(n=l.minPasswordLength)!==null&&n!==void 0?n:Hc,l.maxPasswordLength&&(this.customStrengthOptions.maxPasswordLength=l.maxPasswordLength),l.containsLowercaseCharacter!==void 0&&(this.customStrengthOptions.containsLowercaseLetter=l.containsLowercaseCharacter),l.containsUppercaseCharacter!==void 0&&(this.customStrengthOptions.containsUppercaseLetter=l.containsUppercaseCharacter),l.containsNumericCharacter!==void 0&&(this.customStrengthOptions.containsNumericCharacter=l.containsNumericCharacter),l.containsNonAlphanumericCharacter!==void 0&&(this.customStrengthOptions.containsNonAlphanumericCharacter=l.containsNonAlphanumericCharacter),this.enforcementState=e.enforcementState,this.enforcementState==="ENFORCEMENT_STATE_UNSPECIFIED"&&(this.enforcementState="OFF"),this.allowedNonAlphanumericCharacters=(o=(r=e.allowedNonAlphanumericCharacters)===null||r===void 0?void 0:r.join(""))!==null&&o!==void 0?o:"",this.forceUpgradeOnSignin=(c=e.forceUpgradeOnSignin)!==null&&c!==void 0?c:!1,this.schemaVersion=e.schemaVersion}validatePassword(e){var n,r,o,c,l,p;const y={isValid:!0,passwordPolicy:this};return this.validatePasswordLengthOptions(e,y),this.validatePasswordCharacterOptions(e,y),y.isValid&&(y.isValid=(n=y.meetsMinPasswordLength)!==null&&n!==void 0?n:!0),y.isValid&&(y.isValid=(r=y.meetsMaxPasswordLength)!==null&&r!==void 0?r:!0),y.isValid&&(y.isValid=(o=y.containsLowercaseLetter)!==null&&o!==void 0?o:!0),y.isValid&&(y.isValid=(c=y.containsUppercaseLetter)!==null&&c!==void 0?c:!0),y.isValid&&(y.isValid=(l=y.containsNumericCharacter)!==null&&l!==void 0?l:!0),y.isValid&&(y.isValid=(p=y.containsNonAlphanumericCharacter)!==null&&p!==void 0?p:!0),y}validatePasswordLengthOptions(e,n){const r=this.customStrengthOptions.minPasswordLength,o=this.customStrengthOptions.maxPasswordLength;r&&(n.meetsMinPasswordLength=e.length>=r),o&&(n.meetsMaxPasswordLength=e.length<=o)}validatePasswordCharacterOptions(e,n){this.updatePasswordCharacterOptionsStatuses(n,!1,!1,!1,!1);let r;for(let o=0;o<e.length;o++)r=e.charAt(o),this.updatePasswordCharacterOptionsStatuses(n,r>="a"&&r<="z",r>="A"&&r<="Z",r>="0"&&r<="9",this.allowedNonAlphanumericCharacters.includes(r))}updatePasswordCharacterOptionsStatuses(e,n,r,o,c){this.customStrengthOptions.containsLowercaseLetter&&(e.containsLowercaseLetter||(e.containsLowercaseLetter=n)),this.customStrengthOptions.containsUppercaseLetter&&(e.containsUppercaseLetter||(e.containsUppercaseLetter=r)),this.customStrengthOptions.containsNumericCharacter&&(e.containsNumericCharacter||(e.containsNumericCharacter=o)),this.customStrengthOptions.containsNonAlphanumericCharacter&&(e.containsNonAlphanumericCharacter||(e.containsNonAlphanumericCharacter=c))}}/**
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
 */class Wc{constructor(e,n,r,o){this.app=e,this.heartbeatServiceProvider=n,this.appCheckServiceProvider=r,this.config=o,this.currentUser=null,this.emulatorConfig=null,this.operations=Promise.resolve(),this.authStateSubscription=new Lr(this),this.idTokenSubscription=new Lr(this),this.beforeStateQueue=new Bc(this),this.redirectUser=null,this.isProactiveRefreshEnabled=!1,this.EXPECTED_PASSWORD_POLICY_SCHEMA_VERSION=1,this._canInitEmulator=!0,this._isInitialized=!1,this._deleted=!1,this._initializationPromise=null,this._popupRedirectResolver=null,this._errorFactory=vs,this._agentRecaptchaConfig=null,this._tenantRecaptchaConfigs={},this._projectPasswordPolicy=null,this._tenantPasswordPolicies={},this.lastNotifiedUid=void 0,this.languageCode=null,this.tenantId=null,this.settings={appVerificationDisabledForTesting:!1},this.frameworks=[],this.name=e.name,this.clientVersion=o.sdkClientVersion}_initializeWithPersistence(e,n){return n&&(this._popupRedirectResolver=ge(n)),this._initializationPromise=this.queue(async()=>{var r,o;if(!this._deleted&&(this.persistenceManager=await Ke.create(this,e),!this._deleted)){if(!((r=this._popupRedirectResolver)===null||r===void 0)&&r._shouldInitProactively)try{await this._popupRedirectResolver._initialize(this)}catch{}await this.initializeCurrentUser(n),this.lastNotifiedUid=((o=this.currentUser)===null||o===void 0?void 0:o.uid)||null,!this._deleted&&(this._isInitialized=!0)}}),this._initializationPromise}async _onStorageEvent(){if(this._deleted)return;const e=await this.assertedPersistence.getCurrentUser();if(!(!this.currentUser&&!e)){if(this.currentUser&&e&&this.currentUser.uid===e.uid){this._currentUser._assign(e),await this.currentUser.getIdToken();return}await this._updateCurrentUser(e,!0)}}async initializeCurrentUserFromIdToken(e){try{const n=await Es(this,{idToken:e}),r=await pe._fromGetAccountInfoResponse(this,n,e);await this.directlySetCurrentUser(r)}catch(n){console.warn("FirebaseServerApp could not login user with provided authIdToken: ",n),await this.directlySetCurrentUser(null)}}async initializeCurrentUser(e){var n;if(de(this.app)){const l=this.app.settings.authIdToken;return l?new Promise(p=>{setTimeout(()=>this.initializeCurrentUserFromIdToken(l).then(p,p))}):this.directlySetCurrentUser(null)}const r=await this.assertedPersistence.getCurrentUser();let o=r,c=!1;if(e&&this.config.authDomain){await this.getOrInitRedirectPersistenceManager();const l=(n=this.redirectUser)===null||n===void 0?void 0:n._redirectEventId,p=o==null?void 0:o._redirectEventId,y=await this.tryRedirectSignIn(e);(!l||l===p)&&(y!=null&&y.user)&&(o=y.user,c=!0)}if(!o)return this.directlySetCurrentUser(null);if(!o._redirectEventId){if(c)try{await this.beforeStateQueue.runMiddleware(o)}catch(l){o=r,this._popupRedirectResolver._overrideRedirectResult(this,()=>Promise.reject(l))}return o?this.reloadAndSetCurrentUserOrClear(o):this.directlySetCurrentUser(null)}return A(this._popupRedirectResolver,this,"argument-error"),await this.getOrInitRedirectPersistenceManager(),this.redirectUser&&this.redirectUser._redirectEventId===o._redirectEventId?this.directlySetCurrentUser(o):this.reloadAndSetCurrentUserOrClear(o)}async tryRedirectSignIn(e){let n=null;try{n=await this._popupRedirectResolver._completeRedirectFn(this,e,!0)}catch{await this._setRedirectUser(null)}return n}async reloadAndSetCurrentUserOrClear(e){try{await rn(e)}catch(n){if((n==null?void 0:n.code)!=="auth/network-request-failed")return this.directlySetCurrentUser(null)}return this.directlySetCurrentUser(e)}useDeviceLanguage(){this.languageCode=Ac()}async _delete(){this._deleted=!0}async updateCurrentUser(e){if(de(this.app))return Promise.reject(Re(this));const n=e?ae(e):null;return n&&A(n.auth.config.apiKey===this.config.apiKey,this,"invalid-user-token"),this._updateCurrentUser(n&&n._clone(this))}async _updateCurrentUser(e,n=!1){if(!this._deleted)return e&&A(this.tenantId===e.tenantId,this,"tenant-id-mismatch"),n||await this.beforeStateQueue.runMiddleware(e),this.queue(async()=>{await this.directlySetCurrentUser(e),this.notifyAuthListeners()})}async signOut(){return de(this.app)?Promise.reject(Re(this)):(await this.beforeStateQueue.runMiddleware(null),(this.redirectPersistenceManager||this._popupRedirectResolver)&&await this._setRedirectUser(null),this._updateCurrentUser(null,!0))}setPersistence(e){return de(this.app)?Promise.reject(Re(this)):this.queue(async()=>{await this.assertedPersistence.setPersistence(ge(e))})}_getRecaptchaConfig(){return this.tenantId==null?this._agentRecaptchaConfig:this._tenantRecaptchaConfigs[this.tenantId]}async validatePassword(e){this._getPasswordPolicyInternal()||await this._updatePasswordPolicy();const n=this._getPasswordPolicyInternal();return n.schemaVersion!==this.EXPECTED_PASSWORD_POLICY_SCHEMA_VERSION?Promise.reject(this._errorFactory.create("unsupported-password-policy-schema-version",{})):n.validatePassword(e)}_getPasswordPolicyInternal(){return this.tenantId===null?this._projectPasswordPolicy:this._tenantPasswordPolicies[this.tenantId]}async _updatePasswordPolicy(){const e=await Vc(this),n=new $c(e);this.tenantId===null?this._projectPasswordPolicy=n:this._tenantPasswordPolicies[this.tenantId]=n}_getPersistence(){return this.assertedPersistence.persistence.type}_updateErrorMap(e){this._errorFactory=new bt("auth","Firebase",e())}onAuthStateChanged(e,n,r){return this.registerStateListener(this.authStateSubscription,e,n,r)}beforeAuthStateChanged(e,n){return this.beforeStateQueue.pushCallback(e,n)}onIdTokenChanged(e,n,r){return this.registerStateListener(this.idTokenSubscription,e,n,r)}authStateReady(){return new Promise((e,n)=>{if(this.currentUser)e();else{const r=this.onAuthStateChanged(()=>{r(),e()},n)}})}async revokeAccessToken(e){if(this.currentUser){const n=await this.currentUser.getIdToken(),r={providerId:"apple.com",tokenType:"ACCESS_TOKEN",token:e,idToken:n};this.tenantId!=null&&(r.tenantId=this.tenantId),await xc(this,r)}}toJSON(){var e;return{apiKey:this.config.apiKey,authDomain:this.config.authDomain,appName:this.name,currentUser:(e=this._currentUser)===null||e===void 0?void 0:e.toJSON()}}async _setRedirectUser(e,n){const r=await this.getOrInitRedirectPersistenceManager(n);return e===null?r.removeCurrentUser():r.setCurrentUser(e)}async getOrInitRedirectPersistenceManager(e){if(!this.redirectPersistenceManager){const n=e&&ge(e)||this._popupRedirectResolver;A(n,this,"argument-error"),this.redirectPersistenceManager=await Ke.create(this,[ge(n._redirectPersistence)],"redirectUser"),this.redirectUser=await this.redirectPersistenceManager.getCurrentUser()}return this.redirectPersistenceManager}async _redirectUserForId(e){var n,r;return this._isInitialized&&await this.queue(async()=>{}),((n=this._currentUser)===null||n===void 0?void 0:n._redirectEventId)===e?this._currentUser:((r=this.redirectUser)===null||r===void 0?void 0:r._redirectEventId)===e?this.redirectUser:null}async _persistUserIfCurrent(e){if(e===this.currentUser)return this.queue(async()=>this.directlySetCurrentUser(e))}_notifyListenersIfCurrent(e){e===this.currentUser&&this.notifyAuthListeners()}_key(){return`${this.config.authDomain}:${this.config.apiKey}:${this.name}`}_startProactiveRefresh(){this.isProactiveRefreshEnabled=!0,this.currentUser&&this._currentUser._startProactiveRefresh()}_stopProactiveRefresh(){this.isProactiveRefreshEnabled=!1,this.currentUser&&this._currentUser._stopProactiveRefresh()}get _currentUser(){return this.currentUser}notifyAuthListeners(){var e,n;if(!this._isInitialized)return;this.idTokenSubscription.next(this.currentUser);const r=(n=(e=this.currentUser)===null||e===void 0?void 0:e.uid)!==null&&n!==void 0?n:null;this.lastNotifiedUid!==r&&(this.lastNotifiedUid=r,this.authStateSubscription.next(this.currentUser))}registerStateListener(e,n,r,o){if(this._deleted)return()=>{};const c=typeof n=="function"?n:n.next.bind(n);let l=!1;const p=this._isInitialized?Promise.resolve():this._initializationPromise;if(A(p,this,"internal-error"),p.then(()=>{l||c(this.currentUser)}),typeof n=="function"){const y=e.addObserver(n,r,o);return()=>{l=!0,y()}}else{const y=e.addObserver(n);return()=>{l=!0,y()}}}async directlySetCurrentUser(e){this.currentUser&&this.currentUser!==e&&this._currentUser._stopProactiveRefresh(),e&&this.isProactiveRefreshEnabled&&e._startProactiveRefresh(),this.currentUser=e,e?await this.assertedPersistence.setCurrentUser(e):await this.assertedPersistence.removeCurrentUser()}queue(e){return this.operations=this.operations.then(e,e),this.operations}get assertedPersistence(){return A(this.persistenceManager,this,"internal-error"),this.persistenceManager}_logFramework(e){!e||this.frameworks.includes(e)||(this.frameworks.push(e),this.frameworks.sort(),this.clientVersion=Ls(this.config.clientPlatform,this._getFrameworks()))}_getFrameworks(){return this.frameworks}async _getAdditionalHeaders(){var e;const n={"X-Client-Version":this.clientVersion};this.app.options.appId&&(n["X-Firebase-gmpid"]=this.app.options.appId);const r=await((e=this.heartbeatServiceProvider.getImmediate({optional:!0}))===null||e===void 0?void 0:e.getHeartbeatsHeader());r&&(n["X-Firebase-Client"]=r);const o=await this._getAppCheckToken();return o&&(n["X-Firebase-AppCheck"]=o),n}async _getAppCheckToken(){var e;const n=await((e=this.appCheckServiceProvider.getImmediate({optional:!0}))===null||e===void 0?void 0:e.getToken());return n!=null&&n.error&&Ic(`Error while retrieving App Check token: ${n.error}`),n==null?void 0:n.token}}function Qe(i){return ae(i)}class Lr{constructor(e){this.auth=e,this.observer=null,this.addObserver=aa(n=>this.observer=n)}get next(){return A(this.observer,this.auth,"internal-error"),this.observer.next.bind(this.observer)}}/**
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
 */let dn={async loadJS(){throw new Error("Unable to load external scripts")},recaptchaV2Script:"",recaptchaEnterpriseScript:"",gapiScript:""};function zc(i){dn=i}function Ms(i){return dn.loadJS(i)}function Gc(){return dn.recaptchaEnterpriseScript}function Kc(){return dn.gapiScript}function qc(i){return`__${i}${Math.floor(Math.random()*1e6)}`}const Jc="recaptcha-enterprise",Xc="NO_RECAPTCHA";class Yc{constructor(e){this.type=Jc,this.auth=Qe(e)}async verify(e="verify",n=!1){async function r(c){if(!n){if(c.tenantId==null&&c._agentRecaptchaConfig!=null)return c._agentRecaptchaConfig.siteKey;if(c.tenantId!=null&&c._tenantRecaptchaConfigs[c.tenantId]!==void 0)return c._tenantRecaptchaConfigs[c.tenantId].siteKey}return new Promise(async(l,p)=>{Cc(c,{clientType:"CLIENT_TYPE_WEB",version:"RECAPTCHA_ENTERPRISE"}).then(y=>{if(y.recaptchaKey===void 0)p(new Error("recaptcha Enterprise site key undefined"));else{const E=new Rc(y);return c.tenantId==null?c._agentRecaptchaConfig=E:c._tenantRecaptchaConfigs[c.tenantId]=E,l(E.siteKey)}}).catch(y=>{p(y)})})}function o(c,l,p){const y=window.grecaptcha;Rr(y)?y.enterprise.ready(()=>{y.enterprise.execute(c,{action:e}).then(E=>{l(E)}).catch(()=>{l(Xc)})}):p(Error("No reCAPTCHA enterprise script loaded."))}return new Promise((c,l)=>{r(this.auth).then(p=>{if(!n&&Rr(window.grecaptcha))o(p,c,l);else{if(typeof window>"u"){l(new Error("RecaptchaVerifier is only supported in browser"));return}let y=Gc();y.length!==0&&(y+=p),Ms(y).then(()=>{o(p,c,l)}).catch(E=>{l(E)})}}).catch(p=>{l(p)})})}}async function Mr(i,e,n,r=!1){const o=new Yc(i);let c;try{c=await o.verify(n)}catch{c=await o.verify(n,!0)}const l=Object.assign({},e);return r?Object.assign(l,{captchaResp:c}):Object.assign(l,{captchaResponse:c}),Object.assign(l,{clientType:"CLIENT_TYPE_WEB"}),Object.assign(l,{recaptchaVersion:"RECAPTCHA_ENTERPRISE"}),l}async function Ur(i,e,n,r){var o;if(!((o=i._getRecaptchaConfig())===null||o===void 0)&&o.isProviderEnabled("EMAIL_PASSWORD_PROVIDER")){const c=await Mr(i,e,n,n==="getOobCode");return r(i,c)}else return r(i,e).catch(async c=>{if(c.code==="auth/missing-recaptcha-token"){console.log(`${n} is protected by reCAPTCHA Enterprise for this project. Automatically triggering the reCAPTCHA flow and restarting the flow.`);const l=await Mr(i,e,n,n==="getOobCode");return r(i,l)}else return Promise.reject(c)})}/**
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
 */function Qc(i,e){const n=hn(i,"auth");if(n.isInitialized()){const o=n.getImmediate(),c=n.getOptions();if(tn(c,e??{}))return o;ne(o,"already-initialized")}return n.initialize({options:e})}function Zc(i,e){const n=(e==null?void 0:e.persistence)||[],r=(Array.isArray(n)?n:[n]).map(ge);e!=null&&e.errorMap&&i._updateErrorMap(e.errorMap),i._initializeWithPersistence(r,e==null?void 0:e.popupRedirectResolver)}function eh(i,e,n){const r=Qe(i);A(r._canInitEmulator,r,"emulator-config-failed"),A(/^https?:\/\//.test(e),r,"invalid-emulator-scheme");const o=!1,c=Us(e),{host:l,port:p}=th(e),y=p===null?"":`:${p}`;r.config.emulator={url:`${c}//${l}${y}/`},r.settings.appVerificationDisabledForTesting=!0,r.emulatorConfig=Object.freeze({host:l,port:p,protocol:c.replace(":",""),options:Object.freeze({disableWarnings:o})}),nh()}function Us(i){const e=i.indexOf(":");return e<0?"":i.substr(0,e+1)}function th(i){const e=Us(i),n=/(\/\/)?([^?#/]+)/.exec(i.substr(e.length));if(!n)return{host:"",port:null};const r=n[2].split("@").pop()||"",o=/^(\[[^\]]+\])(:|$)/.exec(r);if(o){const c=o[1];return{host:c,port:xr(r.substr(c.length+1))}}else{const[c,l]=r.split(":");return{host:c,port:xr(l)}}}function xr(i){if(!i)return null;const e=Number(i);return isNaN(e)?null:e}function nh(){function i(){const e=document.createElement("p"),n=e.style;e.innerText="Running in emulator mode. Do not use with production credentials.",n.position="fixed",n.width="100%",n.backgroundColor="#ffffff",n.border=".1em solid #000000",n.color="#b50000",n.bottom="0px",n.left="0px",n.margin="0px",n.zIndex="10000",n.textAlign="center",e.classList.add("firebase-emulator-warning"),document.body.appendChild(e)}typeof console<"u"&&typeof console.info=="function"&&console.info("WARNING: You are using the Auth Emulator, which is intended for local testing only.  Do not use with production credentials."),typeof window<"u"&&typeof document<"u"&&(document.readyState==="loading"?window.addEventListener("DOMContentLoaded",i):i())}/**
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
 */class di{constructor(e,n){this.providerId=e,this.signInMethod=n}toJSON(){return fe("not implemented")}_getIdTokenResponse(e){return fe("not implemented")}_linkToIdToken(e,n){return fe("not implemented")}_getReauthenticationResolver(e){return fe("not implemented")}}async function ih(i,e){return Oe(i,"POST","/v1/accounts:signUp",e)}/**
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
 */async function rh(i,e){return un(i,"POST","/v1/accounts:signInWithPassword",je(i,e))}/**
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
 */async function sh(i,e){return un(i,"POST","/v1/accounts:signInWithEmailLink",je(i,e))}async function oh(i,e){return un(i,"POST","/v1/accounts:signInWithEmailLink",je(i,e))}/**
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
 */class St extends di{constructor(e,n,r,o=null){super("password",r),this._email=e,this._password=n,this._tenantId=o}static _fromEmailAndPassword(e,n){return new St(e,n,"password")}static _fromEmailAndCode(e,n,r=null){return new St(e,n,"emailLink",r)}toJSON(){return{email:this._email,password:this._password,signInMethod:this.signInMethod,tenantId:this._tenantId}}static fromJSON(e){const n=typeof e=="string"?JSON.parse(e):e;if(n!=null&&n.email&&(n!=null&&n.password)){if(n.signInMethod==="password")return this._fromEmailAndPassword(n.email,n.password);if(n.signInMethod==="emailLink")return this._fromEmailAndCode(n.email,n.password,n.tenantId)}return null}async _getIdTokenResponse(e){switch(this.signInMethod){case"password":const n={returnSecureToken:!0,email:this._email,password:this._password,clientType:"CLIENT_TYPE_WEB"};return Ur(e,n,"signInWithPassword",rh);case"emailLink":return sh(e,{email:this._email,oobCode:this._password});default:ne(e,"internal-error")}}async _linkToIdToken(e,n){switch(this.signInMethod){case"password":const r={idToken:n,returnSecureToken:!0,email:this._email,password:this._password,clientType:"CLIENT_TYPE_WEB"};return Ur(e,r,"signUpPassword",ih);case"emailLink":return oh(e,{idToken:n,email:this._email,oobCode:this._password});default:ne(e,"internal-error")}}_getReauthenticationResolver(e){return this._getIdTokenResponse(e)}}/**
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
 */async function qe(i,e){return un(i,"POST","/v1/accounts:signInWithIdp",je(i,e))}/**
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
 */const ah="http://localhost";class Fe extends di{constructor(){super(...arguments),this.pendingToken=null}static _fromParams(e){const n=new Fe(e.providerId,e.signInMethod);return e.idToken||e.accessToken?(e.idToken&&(n.idToken=e.idToken),e.accessToken&&(n.accessToken=e.accessToken),e.nonce&&!e.pendingToken&&(n.nonce=e.nonce),e.pendingToken&&(n.pendingToken=e.pendingToken)):e.oauthToken&&e.oauthTokenSecret?(n.accessToken=e.oauthToken,n.secret=e.oauthTokenSecret):ne("argument-error"),n}toJSON(){return{idToken:this.idToken,accessToken:this.accessToken,secret:this.secret,nonce:this.nonce,pendingToken:this.pendingToken,providerId:this.providerId,signInMethod:this.signInMethod}}static fromJSON(e){const n=typeof e=="string"?JSON.parse(e):e,{providerId:r,signInMethod:o}=n,c=ai(n,["providerId","signInMethod"]);if(!r||!o)return null;const l=new Fe(r,o);return l.idToken=c.idToken||void 0,l.accessToken=c.accessToken||void 0,l.secret=c.secret,l.nonce=c.nonce,l.pendingToken=c.pendingToken||null,l}_getIdTokenResponse(e){const n=this.buildRequest();return qe(e,n)}_linkToIdToken(e,n){const r=this.buildRequest();return r.idToken=n,qe(e,r)}_getReauthenticationResolver(e){const n=this.buildRequest();return n.autoCreate=!1,qe(e,n)}buildRequest(){const e={requestUri:ah,returnSecureToken:!0};if(this.pendingToken)e.pendingToken=this.pendingToken;else{const n={};this.idToken&&(n.id_token=this.idToken),this.accessToken&&(n.access_token=this.accessToken),this.secret&&(n.oauth_token_secret=this.secret),n.providerId=this.providerId,this.nonce&&!this.pendingToken&&(n.nonce=this.nonce),e.postBody=kt(n)}return e}}/**
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
 */function ch(i){switch(i){case"recoverEmail":return"RECOVER_EMAIL";case"resetPassword":return"PASSWORD_RESET";case"signIn":return"EMAIL_SIGNIN";case"verifyEmail":return"VERIFY_EMAIL";case"verifyAndChangeEmail":return"VERIFY_AND_CHANGE_EMAIL";case"revertSecondFactorAddition":return"REVERT_SECOND_FACTOR_ADDITION";default:return null}}function hh(i){const e=vt(_t(i)).link,n=e?vt(_t(e)).deep_link_id:null,r=vt(_t(i)).deep_link_id;return(r?vt(_t(r)).link:null)||r||n||e||i}class fi{constructor(e){var n,r,o,c,l,p;const y=vt(_t(e)),E=(n=y.apiKey)!==null&&n!==void 0?n:null,S=(r=y.oobCode)!==null&&r!==void 0?r:null,b=ch((o=y.mode)!==null&&o!==void 0?o:null);A(E&&S&&b,"argument-error"),this.apiKey=E,this.operation=b,this.code=S,this.continueUrl=(c=y.continueUrl)!==null&&c!==void 0?c:null,this.languageCode=(l=y.languageCode)!==null&&l!==void 0?l:null,this.tenantId=(p=y.tenantId)!==null&&p!==void 0?p:null}static parseLink(e){const n=hh(e);try{return new fi(n)}catch{return null}}}/**
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
 */class Ze{constructor(){this.providerId=Ze.PROVIDER_ID}static credential(e,n){return St._fromEmailAndPassword(e,n)}static credentialWithLink(e,n){const r=fi.parseLink(n);return A(r,"argument-error"),St._fromEmailAndCode(e,r.code,r.tenantId)}}Ze.PROVIDER_ID="password";Ze.EMAIL_PASSWORD_SIGN_IN_METHOD="password";Ze.EMAIL_LINK_SIGN_IN_METHOD="emailLink";/**
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
 */class xs{constructor(e){this.providerId=e,this.defaultLanguageCode=null,this.customParameters={}}setDefaultLanguage(e){this.defaultLanguageCode=e}setCustomParameters(e){return this.customParameters=e,this}getCustomParameters(){return this.customParameters}}/**
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
 */class Rt extends xs{constructor(){super(...arguments),this.scopes=[]}addScope(e){return this.scopes.includes(e)||this.scopes.push(e),this}getScopes(){return[...this.scopes]}}/**
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
 */class Te extends Rt{constructor(){super("facebook.com")}static credential(e){return Fe._fromParams({providerId:Te.PROVIDER_ID,signInMethod:Te.FACEBOOK_SIGN_IN_METHOD,accessToken:e})}static credentialFromResult(e){return Te.credentialFromTaggedObject(e)}static credentialFromError(e){return Te.credentialFromTaggedObject(e.customData||{})}static credentialFromTaggedObject({_tokenResponse:e}){if(!e||!("oauthAccessToken"in e)||!e.oauthAccessToken)return null;try{return Te.credential(e.oauthAccessToken)}catch{return null}}}Te.FACEBOOK_SIGN_IN_METHOD="facebook.com";Te.PROVIDER_ID="facebook.com";/**
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
 */class Ae extends Rt{constructor(){super("google.com"),this.addScope("profile")}static credential(e,n){return Fe._fromParams({providerId:Ae.PROVIDER_ID,signInMethod:Ae.GOOGLE_SIGN_IN_METHOD,idToken:e,accessToken:n})}static credentialFromResult(e){return Ae.credentialFromTaggedObject(e)}static credentialFromError(e){return Ae.credentialFromTaggedObject(e.customData||{})}static credentialFromTaggedObject({_tokenResponse:e}){if(!e)return null;const{oauthIdToken:n,oauthAccessToken:r}=e;if(!n&&!r)return null;try{return Ae.credential(n,r)}catch{return null}}}Ae.GOOGLE_SIGN_IN_METHOD="google.com";Ae.PROVIDER_ID="google.com";/**
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
 */class Se extends Rt{constructor(){super("github.com")}static credential(e){return Fe._fromParams({providerId:Se.PROVIDER_ID,signInMethod:Se.GITHUB_SIGN_IN_METHOD,accessToken:e})}static credentialFromResult(e){return Se.credentialFromTaggedObject(e)}static credentialFromError(e){return Se.credentialFromTaggedObject(e.customData||{})}static credentialFromTaggedObject({_tokenResponse:e}){if(!e||!("oauthAccessToken"in e)||!e.oauthAccessToken)return null;try{return Se.credential(e.oauthAccessToken)}catch{return null}}}Se.GITHUB_SIGN_IN_METHOD="github.com";Se.PROVIDER_ID="github.com";/**
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
 */class be extends Rt{constructor(){super("twitter.com")}static credential(e,n){return Fe._fromParams({providerId:be.PROVIDER_ID,signInMethod:be.TWITTER_SIGN_IN_METHOD,oauthToken:e,oauthTokenSecret:n})}static credentialFromResult(e){return be.credentialFromTaggedObject(e)}static credentialFromError(e){return be.credentialFromTaggedObject(e.customData||{})}static credentialFromTaggedObject({_tokenResponse:e}){if(!e)return null;const{oauthAccessToken:n,oauthTokenSecret:r}=e;if(!n||!r)return null;try{return be.credential(n,r)}catch{return null}}}be.TWITTER_SIGN_IN_METHOD="twitter.com";be.PROVIDER_ID="twitter.com";/**
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
 */class Je{constructor(e){this.user=e.user,this.providerId=e.providerId,this._tokenResponse=e._tokenResponse,this.operationType=e.operationType}static async _fromIdTokenResponse(e,n,r,o=!1){const c=await pe._fromIdTokenResponse(e,r,o),l=Fr(r);return new Je({user:c,providerId:l,_tokenResponse:r,operationType:n})}static async _forOperation(e,n,r){await e._updateTokensIfNecessary(r,!0);const o=Fr(r);return new Je({user:e,providerId:o,_tokenResponse:r,operationType:n})}}function Fr(i){return i.providerId?i.providerId:"phoneNumber"in i?"phone":null}/**
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
 */class sn extends ie{constructor(e,n,r,o){var c;super(n.code,n.message),this.operationType=r,this.user=o,Object.setPrototypeOf(this,sn.prototype),this.customData={appName:e.name,tenantId:(c=e.tenantId)!==null&&c!==void 0?c:void 0,_serverResponse:n.customData._serverResponse,operationType:r}}static _fromErrorAndOperation(e,n,r,o){return new sn(e,n,r,o)}}function Fs(i,e,n,r){return(e==="reauthenticate"?n._getReauthenticationResolver(i):n._getIdTokenResponse(i)).catch(c=>{throw c.code==="auth/multi-factor-auth-required"?sn._fromErrorAndOperation(i,c,e,r):c})}async function lh(i,e,n=!1){const r=await At(i,e._linkToIdToken(i.auth,await i.getIdToken()),n);return Je._forOperation(i,"link",r)}/**
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
 */async function uh(i,e,n=!1){const{auth:r}=i;if(de(r.app))return Promise.reject(Re(r));const o="reauthenticate";try{const c=await At(i,Fs(r,o,e,i),n);A(c.idToken,r,"internal-error");const l=li(c.idToken);A(l,r,"internal-error");const{sub:p}=l;return A(i.uid===p,r,"user-mismatch"),Je._forOperation(i,o,c)}catch(c){throw(c==null?void 0:c.code)==="auth/user-not-found"&&ne(r,"user-mismatch"),c}}/**
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
 */async function js(i,e,n=!1){if(de(i.app))return Promise.reject(Re(i));const r="signIn",o=await Fs(i,r,e),c=await Je._fromIdTokenResponse(i,r,o);return n||await i._updateCurrentUser(c.user),c}async function dh(i,e){return js(Qe(i),e)}/**
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
 */async function fh(i){const e=Qe(i);e._getPasswordPolicyInternal()&&await e._updatePasswordPolicy()}function ph(i,e,n){return de(i.app)?Promise.reject(Re(i)):dh(ae(i),Ze.credential(e,n)).catch(async r=>{throw r.code==="auth/password-does-not-meet-requirements"&&fh(i),r})}function gh(i,e,n,r){return ae(i).onIdTokenChanged(e,n,r)}function mh(i,e,n){return ae(i).beforeAuthStateChanged(e,n)}function vh(i){return ae(i).signOut()}const on="__sak";/**
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
 */class Bs{constructor(e,n){this.storageRetriever=e,this.type=n}_isAvailable(){try{return this.storage?(this.storage.setItem(on,"1"),this.storage.removeItem(on),Promise.resolve(!0)):Promise.resolve(!1)}catch{return Promise.resolve(!1)}}_set(e,n){return this.storage.setItem(e,JSON.stringify(n)),Promise.resolve()}_get(e){const n=this.storage.getItem(e);return Promise.resolve(n?JSON.parse(n):null)}_remove(e){return this.storage.removeItem(e),Promise.resolve()}get storage(){return this.storageRetriever()}}/**
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
 */const _h=1e3,yh=10;class Vs extends Bs{constructor(){super(()=>window.localStorage,"LOCAL"),this.boundEventHandler=(e,n)=>this.onStorageEvent(e,n),this.listeners={},this.localCache={},this.pollTimer=null,this.fallbackToPolling=Ns(),this._shouldAllowMigration=!0}forAllChangedKeys(e){for(const n of Object.keys(this.listeners)){const r=this.storage.getItem(n),o=this.localCache[n];r!==o&&e(n,o,r)}}onStorageEvent(e,n=!1){if(!e.key){this.forAllChangedKeys((l,p,y)=>{this.notifyListeners(l,y)});return}const r=e.key;n?this.detachListener():this.stopPolling();const o=()=>{const l=this.storage.getItem(r);!n&&this.localCache[r]===l||this.notifyListeners(r,l)},c=this.storage.getItem(r);jc()&&c!==e.newValue&&e.newValue!==e.oldValue?setTimeout(o,yh):o()}notifyListeners(e,n){this.localCache[e]=n;const r=this.listeners[e];if(r)for(const o of Array.from(r))o(n&&JSON.parse(n))}startPolling(){this.stopPolling(),this.pollTimer=setInterval(()=>{this.forAllChangedKeys((e,n,r)=>{this.onStorageEvent(new StorageEvent("storage",{key:e,oldValue:n,newValue:r}),!0)})},_h)}stopPolling(){this.pollTimer&&(clearInterval(this.pollTimer),this.pollTimer=null)}attachListener(){window.addEventListener("storage",this.boundEventHandler)}detachListener(){window.removeEventListener("storage",this.boundEventHandler)}_addListener(e,n){Object.keys(this.listeners).length===0&&(this.fallbackToPolling?this.startPolling():this.attachListener()),this.listeners[e]||(this.listeners[e]=new Set,this.localCache[e]=this.storage.getItem(e)),this.listeners[e].add(n)}_removeListener(e,n){this.listeners[e]&&(this.listeners[e].delete(n),this.listeners[e].size===0&&delete this.listeners[e]),Object.keys(this.listeners).length===0&&(this.detachListener(),this.stopPolling())}async _set(e,n){await super._set(e,n),this.localCache[e]=JSON.stringify(n)}async _get(e){const n=await super._get(e);return this.localCache[e]=JSON.stringify(n),n}async _remove(e){await super._remove(e),delete this.localCache[e]}}Vs.type="LOCAL";const wh=Vs;/**
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
 */class Hs extends Bs{constructor(){super(()=>window.sessionStorage,"SESSION")}_addListener(e,n){}_removeListener(e,n){}}Hs.type="SESSION";const $s=Hs;/**
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
 */function Ih(i){return Promise.all(i.map(async e=>{try{return{fulfilled:!0,value:await e}}catch(n){return{fulfilled:!1,reason:n}}}))}/**
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
 */class fn{constructor(e){this.eventTarget=e,this.handlersMap={},this.boundEventHandler=this.handleEvent.bind(this)}static _getInstance(e){const n=this.receivers.find(o=>o.isListeningto(e));if(n)return n;const r=new fn(e);return this.receivers.push(r),r}isListeningto(e){return this.eventTarget===e}async handleEvent(e){const n=e,{eventId:r,eventType:o,data:c}=n.data,l=this.handlersMap[o];if(!(l!=null&&l.size))return;n.ports[0].postMessage({status:"ack",eventId:r,eventType:o});const p=Array.from(l).map(async E=>E(n.origin,c)),y=await Ih(p);n.ports[0].postMessage({status:"done",eventId:r,eventType:o,response:y})}_subscribe(e,n){Object.keys(this.handlersMap).length===0&&this.eventTarget.addEventListener("message",this.boundEventHandler),this.handlersMap[e]||(this.handlersMap[e]=new Set),this.handlersMap[e].add(n)}_unsubscribe(e,n){this.handlersMap[e]&&n&&this.handlersMap[e].delete(n),(!n||this.handlersMap[e].size===0)&&delete this.handlersMap[e],Object.keys(this.handlersMap).length===0&&this.eventTarget.removeEventListener("message",this.boundEventHandler)}}fn.receivers=[];/**
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
 */function pi(i="",e=10){let n="";for(let r=0;r<e;r++)n+=Math.floor(Math.random()*10);return i+n}/**
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
 */class Eh{constructor(e){this.target=e,this.handlers=new Set}removeMessageHandler(e){e.messageChannel&&(e.messageChannel.port1.removeEventListener("message",e.onMessage),e.messageChannel.port1.close()),this.handlers.delete(e)}async _send(e,n,r=50){const o=typeof MessageChannel<"u"?new MessageChannel:null;if(!o)throw new Error("connection_unavailable");let c,l;return new Promise((p,y)=>{const E=pi("",20);o.port1.start();const S=setTimeout(()=>{y(new Error("unsupported_event"))},r);l={messageChannel:o,onMessage(b){const R=b;if(R.data.eventId===E)switch(R.data.status){case"ack":clearTimeout(S),c=setTimeout(()=>{y(new Error("timeout"))},3e3);break;case"done":clearTimeout(c),p(R.data.response);break;default:clearTimeout(S),clearTimeout(c),y(new Error("invalid_response"));break}}},this.handlers.add(l),o.port1.addEventListener("message",l.onMessage),this.target.postMessage({eventType:e,eventId:E,data:n},[o.port2])}).finally(()=>{l&&this.removeMessageHandler(l)})}}/**
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
 */function oe(){return window}function Th(i){oe().location.href=i}/**
 * @license
 * Copyright 2020 Google LLC.
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
 */function Ws(){return typeof oe().WorkerGlobalScope<"u"&&typeof oe().importScripts=="function"}async function Ah(){if(!(navigator!=null&&navigator.serviceWorker))return null;try{return(await navigator.serviceWorker.ready).active}catch{return null}}function Sh(){var i;return((i=navigator==null?void 0:navigator.serviceWorker)===null||i===void 0?void 0:i.controller)||null}function bh(){return Ws()?self:null}/**
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
 */const zs="firebaseLocalStorageDb",kh=1,an="firebaseLocalStorage",Gs="fbase_key";class Ct{constructor(e){this.request=e}toPromise(){return new Promise((e,n)=>{this.request.addEventListener("success",()=>{e(this.request.result)}),this.request.addEventListener("error",()=>{n(this.request.error)})})}}function pn(i,e){return i.transaction([an],e?"readwrite":"readonly").objectStore(an)}function Ph(){const i=indexedDB.deleteDatabase(zs);return new Ct(i).toPromise()}function ni(){const i=indexedDB.open(zs,kh);return new Promise((e,n)=>{i.addEventListener("error",()=>{n(i.error)}),i.addEventListener("upgradeneeded",()=>{const r=i.result;try{r.createObjectStore(an,{keyPath:Gs})}catch(o){n(o)}}),i.addEventListener("success",async()=>{const r=i.result;r.objectStoreNames.contains(an)?e(r):(r.close(),await Ph(),e(await ni()))})})}async function jr(i,e,n){const r=pn(i,!0).put({[Gs]:e,value:n});return new Ct(r).toPromise()}async function Rh(i,e){const n=pn(i,!1).get(e),r=await new Ct(n).toPromise();return r===void 0?null:r.value}function Br(i,e){const n=pn(i,!0).delete(e);return new Ct(n).toPromise()}const Ch=800,Oh=3;class Ks{constructor(){this.type="LOCAL",this._shouldAllowMigration=!0,this.listeners={},this.localCache={},this.pollTimer=null,this.pendingWrites=0,this.receiver=null,this.sender=null,this.serviceWorkerReceiverAvailable=!1,this.activeServiceWorker=null,this._workerInitializationPromise=this.initializeServiceWorkerMessaging().then(()=>{},()=>{})}async _openDb(){return this.db?this.db:(this.db=await ni(),this.db)}async _withRetries(e){let n=0;for(;;)try{const r=await this._openDb();return await e(r)}catch(r){if(n++>Oh)throw r;this.db&&(this.db.close(),this.db=void 0)}}async initializeServiceWorkerMessaging(){return Ws()?this.initializeReceiver():this.initializeSender()}async initializeReceiver(){this.receiver=fn._getInstance(bh()),this.receiver._subscribe("keyChanged",async(e,n)=>({keyProcessed:(await this._poll()).includes(n.key)})),this.receiver._subscribe("ping",async(e,n)=>["keyChanged"])}async initializeSender(){var e,n;if(this.activeServiceWorker=await Ah(),!this.activeServiceWorker)return;this.sender=new Eh(this.activeServiceWorker);const r=await this.sender._send("ping",{},800);r&&!((e=r[0])===null||e===void 0)&&e.fulfilled&&!((n=r[0])===null||n===void 0)&&n.value.includes("keyChanged")&&(this.serviceWorkerReceiverAvailable=!0)}async notifyServiceWorker(e){if(!(!this.sender||!this.activeServiceWorker||Sh()!==this.activeServiceWorker))try{await this.sender._send("keyChanged",{key:e},this.serviceWorkerReceiverAvailable?800:50)}catch{}}async _isAvailable(){try{if(!indexedDB)return!1;const e=await ni();return await jr(e,on,"1"),await Br(e,on),!0}catch{}return!1}async _withPendingWrite(e){this.pendingWrites++;try{await e()}finally{this.pendingWrites--}}async _set(e,n){return this._withPendingWrite(async()=>(await this._withRetries(r=>jr(r,e,n)),this.localCache[e]=n,this.notifyServiceWorker(e)))}async _get(e){const n=await this._withRetries(r=>Rh(r,e));return this.localCache[e]=n,n}async _remove(e){return this._withPendingWrite(async()=>(await this._withRetries(n=>Br(n,e)),delete this.localCache[e],this.notifyServiceWorker(e)))}async _poll(){const e=await this._withRetries(o=>{const c=pn(o,!1).getAll();return new Ct(c).toPromise()});if(!e)return[];if(this.pendingWrites!==0)return[];const n=[],r=new Set;if(e.length!==0)for(const{fbase_key:o,value:c}of e)r.add(o),JSON.stringify(this.localCache[o])!==JSON.stringify(c)&&(this.notifyListeners(o,c),n.push(o));for(const o of Object.keys(this.localCache))this.localCache[o]&&!r.has(o)&&(this.notifyListeners(o,null),n.push(o));return n}notifyListeners(e,n){this.localCache[e]=n;const r=this.listeners[e];if(r)for(const o of Array.from(r))o(n)}startPolling(){this.stopPolling(),this.pollTimer=setInterval(async()=>this._poll(),Ch)}stopPolling(){this.pollTimer&&(clearInterval(this.pollTimer),this.pollTimer=null)}_addListener(e,n){Object.keys(this.listeners).length===0&&this.startPolling(),this.listeners[e]||(this.listeners[e]=new Set,this._get(e)),this.listeners[e].add(n)}_removeListener(e,n){this.listeners[e]&&(this.listeners[e].delete(n),this.listeners[e].size===0&&delete this.listeners[e]),Object.keys(this.listeners).length===0&&this.stopPolling()}}Ks.type="LOCAL";const Dh=Ks;new Pt(3e4,6e4);/**
 * @license
 * Copyright 2021 Google LLC
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
 */function Nh(i,e){return e?ge(e):(A(i._popupRedirectResolver,i,"argument-error"),i._popupRedirectResolver)}/**
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
 */class gi extends di{constructor(e){super("custom","custom"),this.params=e}_getIdTokenResponse(e){return qe(e,this._buildIdpRequest())}_linkToIdToken(e,n){return qe(e,this._buildIdpRequest(n))}_getReauthenticationResolver(e){return qe(e,this._buildIdpRequest())}_buildIdpRequest(e){const n={requestUri:this.params.requestUri,sessionId:this.params.sessionId,postBody:this.params.postBody,tenantId:this.params.tenantId,pendingToken:this.params.pendingToken,returnSecureToken:!0,returnIdpCredential:!0};return e&&(n.idToken=e),n}}function Lh(i){return js(i.auth,new gi(i),i.bypassAuthState)}function Mh(i){const{auth:e,user:n}=i;return A(n,e,"internal-error"),uh(n,new gi(i),i.bypassAuthState)}async function Uh(i){const{auth:e,user:n}=i;return A(n,e,"internal-error"),lh(n,new gi(i),i.bypassAuthState)}/**
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
 */class qs{constructor(e,n,r,o,c=!1){this.auth=e,this.resolver=r,this.user=o,this.bypassAuthState=c,this.pendingPromise=null,this.eventManager=null,this.filter=Array.isArray(n)?n:[n]}execute(){return new Promise(async(e,n)=>{this.pendingPromise={resolve:e,reject:n};try{this.eventManager=await this.resolver._initialize(this.auth),await this.onExecution(),this.eventManager.registerConsumer(this)}catch(r){this.reject(r)}})}async onAuthEvent(e){const{urlResponse:n,sessionId:r,postBody:o,tenantId:c,error:l,type:p}=e;if(l){this.reject(l);return}const y={auth:this.auth,requestUri:n,sessionId:r,tenantId:c||void 0,postBody:o||void 0,user:this.user,bypassAuthState:this.bypassAuthState};try{this.resolve(await this.getIdpTask(p)(y))}catch(E){this.reject(E)}}onError(e){this.reject(e)}getIdpTask(e){switch(e){case"signInViaPopup":case"signInViaRedirect":return Lh;case"linkViaPopup":case"linkViaRedirect":return Uh;case"reauthViaPopup":case"reauthViaRedirect":return Mh;default:ne(this.auth,"internal-error")}}resolve(e){ve(this.pendingPromise,"Pending promise was never set"),this.pendingPromise.resolve(e),this.unregisterAndCleanUp()}reject(e){ve(this.pendingPromise,"Pending promise was never set"),this.pendingPromise.reject(e),this.unregisterAndCleanUp()}unregisterAndCleanUp(){this.eventManager&&this.eventManager.unregisterConsumer(this),this.pendingPromise=null,this.cleanUp()}}/**
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
 */const xh=new Pt(2e3,1e4);class ze extends qs{constructor(e,n,r,o,c){super(e,n,o,c),this.provider=r,this.authWindow=null,this.pollId=null,ze.currentPopupAction&&ze.currentPopupAction.cancel(),ze.currentPopupAction=this}async executeNotNull(){const e=await this.execute();return A(e,this.auth,"internal-error"),e}async onExecution(){ve(this.filter.length===1,"Popup operations only handle one event");const e=pi();this.authWindow=await this.resolver._openPopup(this.auth,this.provider,this.filter[0],e),this.authWindow.associatedEvent=e,this.resolver._originValidation(this.auth).catch(n=>{this.reject(n)}),this.resolver._isIframeWebStorageSupported(this.auth,n=>{n||this.reject(se(this.auth,"web-storage-unsupported"))}),this.pollUserCancellation()}get eventId(){var e;return((e=this.authWindow)===null||e===void 0?void 0:e.associatedEvent)||null}cancel(){this.reject(se(this.auth,"cancelled-popup-request"))}cleanUp(){this.authWindow&&this.authWindow.close(),this.pollId&&window.clearTimeout(this.pollId),this.authWindow=null,this.pollId=null,ze.currentPopupAction=null}pollUserCancellation(){const e=()=>{var n,r;if(!((r=(n=this.authWindow)===null||n===void 0?void 0:n.window)===null||r===void 0)&&r.closed){this.pollId=window.setTimeout(()=>{this.pollId=null,this.reject(se(this.auth,"popup-closed-by-user"))},8e3);return}this.pollId=window.setTimeout(e,xh.get())};e()}}ze.currentPopupAction=null;/**
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
 */const Fh="pendingRedirect",Qt=new Map;class jh extends qs{constructor(e,n,r=!1){super(e,["signInViaRedirect","linkViaRedirect","reauthViaRedirect","unknown"],n,void 0,r),this.eventId=null}async execute(){let e=Qt.get(this.auth._key());if(!e){try{const r=await Bh(this.resolver,this.auth)?await super.execute():null;e=()=>Promise.resolve(r)}catch(n){e=()=>Promise.reject(n)}Qt.set(this.auth._key(),e)}return this.bypassAuthState||Qt.set(this.auth._key(),()=>Promise.resolve(null)),e()}async onAuthEvent(e){if(e.type==="signInViaRedirect")return super.onAuthEvent(e);if(e.type==="unknown"){this.resolve(null);return}if(e.eventId){const n=await this.auth._redirectUserForId(e.eventId);if(n)return this.user=n,super.onAuthEvent(e);this.resolve(null)}}async onExecution(){}cleanUp(){}}async function Bh(i,e){const n=$h(e),r=Hh(i);if(!await r._isAvailable())return!1;const o=await r._get(n)==="true";return await r._remove(n),o}function Vh(i,e){Qt.set(i._key(),e)}function Hh(i){return ge(i._redirectPersistence)}function $h(i){return Yt(Fh,i.config.apiKey,i.name)}async function Wh(i,e,n=!1){if(de(i.app))return Promise.reject(Re(i));const r=Qe(i),o=Nh(r,e),l=await new jh(r,o,n).execute();return l&&!n&&(delete l.user._redirectEventId,await r._persistUserIfCurrent(l.user),await r._setRedirectUser(null,e)),l}/**
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
 */const zh=10*60*1e3;class Gh{constructor(e){this.auth=e,this.cachedEventUids=new Set,this.consumers=new Set,this.queuedRedirectEvent=null,this.hasHandledPotentialRedirect=!1,this.lastProcessedEventTime=Date.now()}registerConsumer(e){this.consumers.add(e),this.queuedRedirectEvent&&this.isEventForConsumer(this.queuedRedirectEvent,e)&&(this.sendToConsumer(this.queuedRedirectEvent,e),this.saveEventToCache(this.queuedRedirectEvent),this.queuedRedirectEvent=null)}unregisterConsumer(e){this.consumers.delete(e)}onEvent(e){if(this.hasEventBeenHandled(e))return!1;let n=!1;return this.consumers.forEach(r=>{this.isEventForConsumer(e,r)&&(n=!0,this.sendToConsumer(e,r),this.saveEventToCache(e))}),this.hasHandledPotentialRedirect||!Kh(e)||(this.hasHandledPotentialRedirect=!0,n||(this.queuedRedirectEvent=e,n=!0)),n}sendToConsumer(e,n){var r;if(e.error&&!Js(e)){const o=((r=e.error.code)===null||r===void 0?void 0:r.split("auth/")[1])||"internal-error";n.onError(se(this.auth,o))}else n.onAuthEvent(e)}isEventForConsumer(e,n){const r=n.eventId===null||!!e.eventId&&e.eventId===n.eventId;return n.filter.includes(e.type)&&r}hasEventBeenHandled(e){return Date.now()-this.lastProcessedEventTime>=zh&&this.cachedEventUids.clear(),this.cachedEventUids.has(Vr(e))}saveEventToCache(e){this.cachedEventUids.add(Vr(e)),this.lastProcessedEventTime=Date.now()}}function Vr(i){return[i.type,i.eventId,i.sessionId,i.tenantId].filter(e=>e).join("-")}function Js({type:i,error:e}){return i==="unknown"&&(e==null?void 0:e.code)==="auth/no-auth-event"}function Kh(i){switch(i.type){case"signInViaRedirect":case"linkViaRedirect":case"reauthViaRedirect":return!0;case"unknown":return Js(i);default:return!1}}/**
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
 */async function qh(i,e={}){return Oe(i,"GET","/v1/projects",e)}/**
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
 */const Jh=/^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$/,Xh=/^https?/;async function Yh(i){if(i.config.emulator)return;const{authorizedDomains:e}=await qh(i);for(const n of e)try{if(Qh(n))return}catch{}ne(i,"unauthorized-domain")}function Qh(i){const e=ei(),{protocol:n,hostname:r}=new URL(e);if(i.startsWith("chrome-extension://")){const l=new URL(i);return l.hostname===""&&r===""?n==="chrome-extension:"&&i.replace("chrome-extension://","")===e.replace("chrome-extension://",""):n==="chrome-extension:"&&l.hostname===r}if(!Xh.test(n))return!1;if(Jh.test(i))return r===i;const o=i.replace(/\./g,"\\.");return new RegExp("^(.+\\."+o+"|"+o+")$","i").test(r)}/**
 * @license
 * Copyright 2020 Google LLC.
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
 */const Zh=new Pt(3e4,6e4);function Hr(){const i=oe().___jsl;if(i!=null&&i.H){for(const e of Object.keys(i.H))if(i.H[e].r=i.H[e].r||[],i.H[e].L=i.H[e].L||[],i.H[e].r=[...i.H[e].L],i.CP)for(let n=0;n<i.CP.length;n++)i.CP[n]=null}}function el(i){return new Promise((e,n)=>{var r,o,c;function l(){Hr(),gapi.load("gapi.iframes",{callback:()=>{e(gapi.iframes.getContext())},ontimeout:()=>{Hr(),n(se(i,"network-request-failed"))},timeout:Zh.get()})}if(!((o=(r=oe().gapi)===null||r===void 0?void 0:r.iframes)===null||o===void 0)&&o.Iframe)e(gapi.iframes.getContext());else if(!((c=oe().gapi)===null||c===void 0)&&c.load)l();else{const p=qc("iframefcb");return oe()[p]=()=>{gapi.load?l():n(se(i,"network-request-failed"))},Ms(`${Kc()}?onload=${p}`).catch(y=>n(y))}}).catch(e=>{throw Zt=null,e})}let Zt=null;function tl(i){return Zt=Zt||el(i),Zt}/**
 * @license
 * Copyright 2020 Google LLC.
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
 */const nl=new Pt(5e3,15e3),il="__/auth/iframe",rl="emulator/auth/iframe",sl={style:{position:"absolute",top:"-100px",width:"1px",height:"1px"},"aria-hidden":"true",tabindex:"-1"},ol=new Map([["identitytoolkit.googleapis.com","p"],["staging-identitytoolkit.sandbox.googleapis.com","s"],["test-identitytoolkit.sandbox.googleapis.com","t"]]);function al(i){const e=i.config;A(e.authDomain,i,"auth-domain-config-required");const n=e.emulator?hi(e,rl):`https://${i.config.authDomain}/${il}`,r={apiKey:e.apiKey,appName:i.name,v:Ye},o=ol.get(i.config.apiHost);o&&(r.eid=o);const c=i._getFrameworks();return c.length&&(r.fw=c.join(",")),`${n}?${kt(r).slice(1)}`}async function cl(i){const e=await tl(i),n=oe().gapi;return A(n,i,"internal-error"),e.open({where:document.body,url:al(i),messageHandlersFilter:n.iframes.CROSS_ORIGIN_IFRAMES_FILTER,attributes:sl,dontclear:!0},r=>new Promise(async(o,c)=>{await r.restyle({setHideOnLeave:!1});const l=se(i,"network-request-failed"),p=oe().setTimeout(()=>{c(l)},nl.get());function y(){oe().clearTimeout(p),o(r)}r.ping(y).then(y,()=>{c(l)})}))}/**
 * @license
 * Copyright 2020 Google LLC.
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
 */const hl={location:"yes",resizable:"yes",statusbar:"yes",toolbar:"no"},ll=500,ul=600,dl="_blank",fl="http://localhost";class $r{constructor(e){this.window=e,this.associatedEvent=null}close(){if(this.window)try{this.window.close()}catch{}}}function pl(i,e,n,r=ll,o=ul){const c=Math.max((window.screen.availHeight-o)/2,0).toString(),l=Math.max((window.screen.availWidth-r)/2,0).toString();let p="";const y=Object.assign(Object.assign({},hl),{width:r.toString(),height:o.toString(),top:c,left:l}),E=K().toLowerCase();n&&(p=Ps(E)?dl:n),bs(E)&&(e=e||fl,y.scrollbars="yes");const S=Object.entries(y).reduce((R,[x,P])=>`${R}${x}=${P},`,"");if(Fc(E)&&p!=="_self")return gl(e||"",p),new $r(null);const b=window.open(e||"",p,S);A(b,i,"popup-blocked");try{b.focus()}catch{}return new $r(b)}function gl(i,e){const n=document.createElement("a");n.href=i,n.target=e;const r=document.createEvent("MouseEvent");r.initMouseEvent("click",!0,!0,window,1,0,0,0,0,!1,!1,!1,!1,1,null),n.dispatchEvent(r)}/**
 * @license
 * Copyright 2021 Google LLC
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
 */const ml="__/auth/handler",vl="emulator/auth/handler",_l=encodeURIComponent("fac");async function Wr(i,e,n,r,o,c){A(i.config.authDomain,i,"auth-domain-config-required"),A(i.config.apiKey,i,"invalid-api-key");const l={apiKey:i.config.apiKey,appName:i.name,authType:n,redirectUrl:r,v:Ye,eventId:o};if(e instanceof xs){e.setDefaultLanguage(i.languageCode),l.providerId=e.providerId||"",oa(e.getCustomParameters())||(l.customParameters=JSON.stringify(e.getCustomParameters()));for(const[S,b]of Object.entries({}))l[S]=b}if(e instanceof Rt){const S=e.getScopes().filter(b=>b!=="");S.length>0&&(l.scopes=S.join(","))}i.tenantId&&(l.tid=i.tenantId);const p=l;for(const S of Object.keys(p))p[S]===void 0&&delete p[S];const y=await i._getAppCheckToken(),E=y?`#${_l}=${encodeURIComponent(y)}`:"";return`${yl(i)}?${kt(p).slice(1)}${E}`}function yl({config:i}){return i.emulator?hi(i,vl):`https://${i.authDomain}/${ml}`}/**
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
 */const Kn="webStorageSupport";class wl{constructor(){this.eventManagers={},this.iframes={},this.originValidationPromises={},this._redirectPersistence=$s,this._completeRedirectFn=Wh,this._overrideRedirectResult=Vh}async _openPopup(e,n,r,o){var c;ve((c=this.eventManagers[e._key()])===null||c===void 0?void 0:c.manager,"_initialize() not called before _openPopup()");const l=await Wr(e,n,r,ei(),o);return pl(e,l,pi())}async _openRedirect(e,n,r,o){await this._originValidation(e);const c=await Wr(e,n,r,ei(),o);return Th(c),new Promise(()=>{})}_initialize(e){const n=e._key();if(this.eventManagers[n]){const{manager:o,promise:c}=this.eventManagers[n];return o?Promise.resolve(o):(ve(c,"If manager is not set, promise should be"),c)}const r=this.initAndGetManager(e);return this.eventManagers[n]={promise:r},r.catch(()=>{delete this.eventManagers[n]}),r}async initAndGetManager(e){const n=await cl(e),r=new Gh(e);return n.register("authEvent",o=>(A(o==null?void 0:o.authEvent,e,"invalid-auth-event"),{status:r.onEvent(o.authEvent)?"ACK":"ERROR"}),gapi.iframes.CROSS_ORIGIN_IFRAMES_FILTER),this.eventManagers[e._key()]={manager:r},this.iframes[e._key()]=n,r}_isIframeWebStorageSupported(e,n){this.iframes[e._key()].send(Kn,{type:Kn},o=>{var c;const l=(c=o==null?void 0:o[0])===null||c===void 0?void 0:c[Kn];l!==void 0&&n(!!l),ne(e,"internal-error")},gapi.iframes.CROSS_ORIGIN_IFRAMES_FILTER)}_originValidation(e){const n=e._key();return this.originValidationPromises[n]||(this.originValidationPromises[n]=Yh(e)),this.originValidationPromises[n]}get _shouldInitProactively(){return Ns()||ks()||ui()}}const Il=wl;var zr="@firebase/auth",Gr="1.7.9";/**
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
 */class El{constructor(e){this.auth=e,this.internalListeners=new Map}getUid(){var e;return this.assertAuthConfigured(),((e=this.auth.currentUser)===null||e===void 0?void 0:e.uid)||null}async getToken(e){return this.assertAuthConfigured(),await this.auth._initializationPromise,this.auth.currentUser?{accessToken:await this.auth.currentUser.getIdToken(e)}:null}addAuthTokenListener(e){if(this.assertAuthConfigured(),this.internalListeners.has(e))return;const n=this.auth.onIdTokenChanged(r=>{e((r==null?void 0:r.stsTokenManager.accessToken)||null)});this.internalListeners.set(e,n),this.updateProactiveRefresh()}removeAuthTokenListener(e){this.assertAuthConfigured();const n=this.internalListeners.get(e);n&&(this.internalListeners.delete(e),n(),this.updateProactiveRefresh())}assertAuthConfigured(){A(this.auth._initializationPromise,"dependent-sdk-initialized-before-auth")}updateProactiveRefresh(){this.internalListeners.size>0?this.auth._startProactiveRefresh():this.auth._stopProactiveRefresh()}}/**
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
 */function Tl(i){switch(i){case"Node":return"node";case"ReactNative":return"rn";case"Worker":return"webworker";case"Cordova":return"cordova";case"WebExtension":return"web-extension";default:return}}function Al(i){xe(new Ce("auth",(e,{options:n})=>{const r=e.getProvider("app").getImmediate(),o=e.getProvider("heartbeat"),c=e.getProvider("app-check-internal"),{apiKey:l,authDomain:p}=r.options;A(l&&!l.includes(":"),"invalid-api-key",{appName:r.name});const y={apiKey:l,authDomain:p,clientPlatform:i,apiHost:"identitytoolkit.googleapis.com",tokenApiHost:"securetoken.googleapis.com",apiScheme:"https",sdkClientVersion:Ls(i)},E=new Wc(r,o,c,y);return Zc(E,n),E},"PUBLIC").setInstantiationMode("EXPLICIT").setInstanceCreatedCallback((e,n,r)=>{e.getProvider("auth-internal").initialize()})),xe(new Ce("auth-internal",e=>{const n=Qe(e.getProvider("auth").getImmediate());return(r=>new El(r))(n)},"PRIVATE").setInstantiationMode("EXPLICIT")),re(zr,Gr,Tl(i)),re(zr,Gr,"esm2017")}/**
 * @license
 * Copyright 2021 Google LLC
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
 */const Sl=5*60,bl=ls("authIdTokenMaxAge")||Sl;let Kr=null;const kl=i=>async e=>{const n=e&&await e.getIdTokenResult(),r=n&&(new Date().getTime()-Date.parse(n.issuedAtTime))/1e3;if(r&&r>bl)return;const o=n==null?void 0:n.token;Kr!==o&&(Kr=o,await fetch(i,{method:o?"POST":"DELETE",headers:o?{Authorization:`Bearer ${o}`}:{}}))};function Pl(i=ln()){const e=hn(i,"auth");if(e.isInitialized())return e.getImmediate();const n=Qc(i,{popupRedirectResolver:Il,persistence:[Dh,wh,$s]}),r=ls("authTokenSyncURL");if(r&&typeof isSecureContext=="boolean"&&isSecureContext){const c=new URL(r,location.origin);if(location.origin===c.origin){const l=kl(c.toString());mh(n,l,()=>l(n.currentUser)),gh(n,p=>l(p))}}const o=as("auth");return o&&eh(n,`http://${o}`),n}function Rl(){var i,e;return(e=(i=document.getElementsByTagName("head"))===null||i===void 0?void 0:i[0])!==null&&e!==void 0?e:document}zc({loadJS(i){return new Promise((e,n)=>{const r=document.createElement("script");r.setAttribute("src",i),r.onload=e,r.onerror=o=>{const c=se("internal-error");c.customData=o,n(c)},r.type="text/javascript",r.charset="UTF-8",Rl().appendChild(r)})},gapiScript:"https://apis.google.com/js/api.js",recaptchaV2Script:"https://www.google.com/recaptcha/api.js",recaptchaEnterpriseScript:"https://www.google.com/recaptcha/enterprise.js?render="});Al("Browser");var qr=typeof globalThis<"u"?globalThis:typeof window<"u"?window:typeof global<"u"?global:typeof self<"u"?self:{};/** @license
Copyright The Closure Library Authors.
SPDX-License-Identifier: Apache-2.0
*/var Xs;(function(){var i;/** @license

 Copyright The Closure Library Authors.
 SPDX-License-Identifier: Apache-2.0
*/function e(v,u){function f(){}f.prototype=u.prototype,v.D=u.prototype,v.prototype=new f,v.prototype.constructor=v,v.C=function(g,m,w){for(var d=Array(arguments.length-2),he=2;he<arguments.length;he++)d[he-2]=arguments[he];return u.prototype[m].apply(g,d)}}function n(){this.blockSize=-1}function r(){this.blockSize=-1,this.blockSize=64,this.g=Array(4),this.B=Array(this.blockSize),this.o=this.h=0,this.s()}e(r,n),r.prototype.s=function(){this.g[0]=1732584193,this.g[1]=4023233417,this.g[2]=2562383102,this.g[3]=271733878,this.o=this.h=0};function o(v,u,f){f||(f=0);var g=Array(16);if(typeof u=="string")for(var m=0;16>m;++m)g[m]=u.charCodeAt(f++)|u.charCodeAt(f++)<<8|u.charCodeAt(f++)<<16|u.charCodeAt(f++)<<24;else for(m=0;16>m;++m)g[m]=u[f++]|u[f++]<<8|u[f++]<<16|u[f++]<<24;u=v.g[0],f=v.g[1],m=v.g[2];var w=v.g[3],d=u+(w^f&(m^w))+g[0]+3614090360&4294967295;u=f+(d<<7&4294967295|d>>>25),d=w+(m^u&(f^m))+g[1]+3905402710&4294967295,w=u+(d<<12&4294967295|d>>>20),d=m+(f^w&(u^f))+g[2]+606105819&4294967295,m=w+(d<<17&4294967295|d>>>15),d=f+(u^m&(w^u))+g[3]+3250441966&4294967295,f=m+(d<<22&4294967295|d>>>10),d=u+(w^f&(m^w))+g[4]+4118548399&4294967295,u=f+(d<<7&4294967295|d>>>25),d=w+(m^u&(f^m))+g[5]+1200080426&4294967295,w=u+(d<<12&4294967295|d>>>20),d=m+(f^w&(u^f))+g[6]+2821735955&4294967295,m=w+(d<<17&4294967295|d>>>15),d=f+(u^m&(w^u))+g[7]+4249261313&4294967295,f=m+(d<<22&4294967295|d>>>10),d=u+(w^f&(m^w))+g[8]+1770035416&4294967295,u=f+(d<<7&4294967295|d>>>25),d=w+(m^u&(f^m))+g[9]+2336552879&4294967295,w=u+(d<<12&4294967295|d>>>20),d=m+(f^w&(u^f))+g[10]+4294925233&4294967295,m=w+(d<<17&4294967295|d>>>15),d=f+(u^m&(w^u))+g[11]+2304563134&4294967295,f=m+(d<<22&4294967295|d>>>10),d=u+(w^f&(m^w))+g[12]+1804603682&4294967295,u=f+(d<<7&4294967295|d>>>25),d=w+(m^u&(f^m))+g[13]+4254626195&4294967295,w=u+(d<<12&4294967295|d>>>20),d=m+(f^w&(u^f))+g[14]+2792965006&4294967295,m=w+(d<<17&4294967295|d>>>15),d=f+(u^m&(w^u))+g[15]+1236535329&4294967295,f=m+(d<<22&4294967295|d>>>10),d=u+(m^w&(f^m))+g[1]+4129170786&4294967295,u=f+(d<<5&4294967295|d>>>27),d=w+(f^m&(u^f))+g[6]+3225465664&4294967295,w=u+(d<<9&4294967295|d>>>23),d=m+(u^f&(w^u))+g[11]+643717713&4294967295,m=w+(d<<14&4294967295|d>>>18),d=f+(w^u&(m^w))+g[0]+3921069994&4294967295,f=m+(d<<20&4294967295|d>>>12),d=u+(m^w&(f^m))+g[5]+3593408605&4294967295,u=f+(d<<5&4294967295|d>>>27),d=w+(f^m&(u^f))+g[10]+38016083&4294967295,w=u+(d<<9&4294967295|d>>>23),d=m+(u^f&(w^u))+g[15]+3634488961&4294967295,m=w+(d<<14&4294967295|d>>>18),d=f+(w^u&(m^w))+g[4]+3889429448&4294967295,f=m+(d<<20&4294967295|d>>>12),d=u+(m^w&(f^m))+g[9]+568446438&4294967295,u=f+(d<<5&4294967295|d>>>27),d=w+(f^m&(u^f))+g[14]+3275163606&4294967295,w=u+(d<<9&4294967295|d>>>23),d=m+(u^f&(w^u))+g[3]+4107603335&4294967295,m=w+(d<<14&4294967295|d>>>18),d=f+(w^u&(m^w))+g[8]+1163531501&4294967295,f=m+(d<<20&4294967295|d>>>12),d=u+(m^w&(f^m))+g[13]+2850285829&4294967295,u=f+(d<<5&4294967295|d>>>27),d=w+(f^m&(u^f))+g[2]+4243563512&4294967295,w=u+(d<<9&4294967295|d>>>23),d=m+(u^f&(w^u))+g[7]+1735328473&4294967295,m=w+(d<<14&4294967295|d>>>18),d=f+(w^u&(m^w))+g[12]+2368359562&4294967295,f=m+(d<<20&4294967295|d>>>12),d=u+(f^m^w)+g[5]+4294588738&4294967295,u=f+(d<<4&4294967295|d>>>28),d=w+(u^f^m)+g[8]+2272392833&4294967295,w=u+(d<<11&4294967295|d>>>21),d=m+(w^u^f)+g[11]+1839030562&4294967295,m=w+(d<<16&4294967295|d>>>16),d=f+(m^w^u)+g[14]+4259657740&4294967295,f=m+(d<<23&4294967295|d>>>9),d=u+(f^m^w)+g[1]+2763975236&4294967295,u=f+(d<<4&4294967295|d>>>28),d=w+(u^f^m)+g[4]+1272893353&4294967295,w=u+(d<<11&4294967295|d>>>21),d=m+(w^u^f)+g[7]+4139469664&4294967295,m=w+(d<<16&4294967295|d>>>16),d=f+(m^w^u)+g[10]+3200236656&4294967295,f=m+(d<<23&4294967295|d>>>9),d=u+(f^m^w)+g[13]+681279174&4294967295,u=f+(d<<4&4294967295|d>>>28),d=w+(u^f^m)+g[0]+3936430074&4294967295,w=u+(d<<11&4294967295|d>>>21),d=m+(w^u^f)+g[3]+3572445317&4294967295,m=w+(d<<16&4294967295|d>>>16),d=f+(m^w^u)+g[6]+76029189&4294967295,f=m+(d<<23&4294967295|d>>>9),d=u+(f^m^w)+g[9]+3654602809&4294967295,u=f+(d<<4&4294967295|d>>>28),d=w+(u^f^m)+g[12]+3873151461&4294967295,w=u+(d<<11&4294967295|d>>>21),d=m+(w^u^f)+g[15]+530742520&4294967295,m=w+(d<<16&4294967295|d>>>16),d=f+(m^w^u)+g[2]+3299628645&4294967295,f=m+(d<<23&4294967295|d>>>9),d=u+(m^(f|~w))+g[0]+4096336452&4294967295,u=f+(d<<6&4294967295|d>>>26),d=w+(f^(u|~m))+g[7]+1126891415&4294967295,w=u+(d<<10&4294967295|d>>>22),d=m+(u^(w|~f))+g[14]+2878612391&4294967295,m=w+(d<<15&4294967295|d>>>17),d=f+(w^(m|~u))+g[5]+4237533241&4294967295,f=m+(d<<21&4294967295|d>>>11),d=u+(m^(f|~w))+g[12]+1700485571&4294967295,u=f+(d<<6&4294967295|d>>>26),d=w+(f^(u|~m))+g[3]+2399980690&4294967295,w=u+(d<<10&4294967295|d>>>22),d=m+(u^(w|~f))+g[10]+4293915773&4294967295,m=w+(d<<15&4294967295|d>>>17),d=f+(w^(m|~u))+g[1]+2240044497&4294967295,f=m+(d<<21&4294967295|d>>>11),d=u+(m^(f|~w))+g[8]+1873313359&4294967295,u=f+(d<<6&4294967295|d>>>26),d=w+(f^(u|~m))+g[15]+4264355552&4294967295,w=u+(d<<10&4294967295|d>>>22),d=m+(u^(w|~f))+g[6]+2734768916&4294967295,m=w+(d<<15&4294967295|d>>>17),d=f+(w^(m|~u))+g[13]+1309151649&4294967295,f=m+(d<<21&4294967295|d>>>11),d=u+(m^(f|~w))+g[4]+4149444226&4294967295,u=f+(d<<6&4294967295|d>>>26),d=w+(f^(u|~m))+g[11]+3174756917&4294967295,w=u+(d<<10&4294967295|d>>>22),d=m+(u^(w|~f))+g[2]+718787259&4294967295,m=w+(d<<15&4294967295|d>>>17),d=f+(w^(m|~u))+g[9]+3951481745&4294967295,v.g[0]=v.g[0]+u&4294967295,v.g[1]=v.g[1]+(m+(d<<21&4294967295|d>>>11))&4294967295,v.g[2]=v.g[2]+m&4294967295,v.g[3]=v.g[3]+w&4294967295}r.prototype.u=function(v,u){u===void 0&&(u=v.length);for(var f=u-this.blockSize,g=this.B,m=this.h,w=0;w<u;){if(m==0)for(;w<=f;)o(this,v,w),w+=this.blockSize;if(typeof v=="string"){for(;w<u;)if(g[m++]=v.charCodeAt(w++),m==this.blockSize){o(this,g),m=0;break}}else for(;w<u;)if(g[m++]=v[w++],m==this.blockSize){o(this,g),m=0;break}}this.h=m,this.o+=u},r.prototype.v=function(){var v=Array((56>this.h?this.blockSize:2*this.blockSize)-this.h);v[0]=128;for(var u=1;u<v.length-8;++u)v[u]=0;var f=8*this.o;for(u=v.length-8;u<v.length;++u)v[u]=f&255,f/=256;for(this.u(v),v=Array(16),u=f=0;4>u;++u)for(var g=0;32>g;g+=8)v[f++]=this.g[u]>>>g&255;return v};function c(v,u){var f=p;return Object.prototype.hasOwnProperty.call(f,v)?f[v]:f[v]=u(v)}function l(v,u){this.h=u;for(var f=[],g=!0,m=v.length-1;0<=m;m--){var w=v[m]|0;g&&w==u||(f[m]=w,g=!1)}this.g=f}var p={};function y(v){return-128<=v&&128>v?c(v,function(u){return new l([u|0],0>u?-1:0)}):new l([v|0],0>v?-1:0)}function E(v){if(isNaN(v)||!isFinite(v))return b;if(0>v)return L(E(-v));for(var u=[],f=1,g=0;v>=f;g++)u[g]=v/f|0,f*=4294967296;return new l(u,0)}function S(v,u){if(v.length==0)throw Error("number format error: empty string");if(u=u||10,2>u||36<u)throw Error("radix out of range: "+u);if(v.charAt(0)=="-")return L(S(v.substring(1),u));if(0<=v.indexOf("-"))throw Error('number format error: interior "-" character');for(var f=E(Math.pow(u,8)),g=b,m=0;m<v.length;m+=8){var w=Math.min(8,v.length-m),d=parseInt(v.substring(m,m+w),u);8>w?(w=E(Math.pow(u,w)),g=g.j(w).add(E(d))):(g=g.j(f),g=g.add(E(d)))}return g}var b=y(0),R=y(1),x=y(16777216);i=l.prototype,i.m=function(){if(U(this))return-L(this).m();for(var v=0,u=1,f=0;f<this.g.length;f++){var g=this.i(f);v+=(0<=g?g:4294967296+g)*u,u*=4294967296}return v},i.toString=function(v){if(v=v||10,2>v||36<v)throw Error("radix out of range: "+v);if(P(this))return"0";if(U(this))return"-"+L(this).toString(v);for(var u=E(Math.pow(v,6)),f=this,g="";;){var m=Z(f,u).g;f=ce(f,m.j(u));var w=((0<f.g.length?f.g[0]:f.h)>>>0).toString(v);if(f=m,P(f))return w+g;for(;6>w.length;)w="0"+w;g=w+g}},i.i=function(v){return 0>v?0:v<this.g.length?this.g[v]:this.h};function P(v){if(v.h!=0)return!1;for(var u=0;u<v.g.length;u++)if(v.g[u]!=0)return!1;return!0}function U(v){return v.h==-1}i.l=function(v){return v=ce(this,v),U(v)?-1:P(v)?0:1};function L(v){for(var u=v.g.length,f=[],g=0;g<u;g++)f[g]=~v.g[g];return new l(f,~v.h).add(R)}i.abs=function(){return U(this)?L(this):this},i.add=function(v){for(var u=Math.max(this.g.length,v.g.length),f=[],g=0,m=0;m<=u;m++){var w=g+(this.i(m)&65535)+(v.i(m)&65535),d=(w>>>16)+(this.i(m)>>>16)+(v.i(m)>>>16);g=d>>>16,w&=65535,d&=65535,f[m]=d<<16|w}return new l(f,f[f.length-1]&-2147483648?-1:0)};function ce(v,u){return v.add(L(u))}i.j=function(v){if(P(this)||P(v))return b;if(U(this))return U(v)?L(this).j(L(v)):L(L(this).j(v));if(U(v))return L(this.j(L(v)));if(0>this.l(x)&&0>v.l(x))return E(this.m()*v.m());for(var u=this.g.length+v.g.length,f=[],g=0;g<2*u;g++)f[g]=0;for(g=0;g<this.g.length;g++)for(var m=0;m<v.g.length;m++){var w=this.i(g)>>>16,d=this.i(g)&65535,he=v.i(m)>>>16,et=v.i(m)&65535;f[2*g+2*m]+=d*et,Y(f,2*g+2*m),f[2*g+2*m+1]+=w*et,Y(f,2*g+2*m+1),f[2*g+2*m+1]+=d*he,Y(f,2*g+2*m+1),f[2*g+2*m+2]+=w*he,Y(f,2*g+2*m+2)}for(g=0;g<u;g++)f[g]=f[2*g+1]<<16|f[2*g];for(g=u;g<2*u;g++)f[g]=0;return new l(f,0)};function Y(v,u){for(;(v[u]&65535)!=v[u];)v[u+1]+=v[u]>>>16,v[u]&=65535,u++}function j(v,u){this.g=v,this.h=u}function Z(v,u){if(P(u))throw Error("division by zero");if(P(v))return new j(b,b);if(U(v))return u=Z(L(v),u),new j(L(u.g),L(u.h));if(U(u))return u=Z(v,L(u)),new j(L(u.g),u.h);if(30<v.g.length){if(U(v)||U(u))throw Error("slowDivide_ only works with positive integers.");for(var f=R,g=u;0>=g.l(v);)f=De(f),g=De(g);var m=q(f,1),w=q(g,1);for(g=q(g,2),f=q(f,2);!P(g);){var d=w.add(g);0>=d.l(v)&&(m=m.add(f),w=d),g=q(g,1),f=q(f,1)}return u=ce(v,m.j(u)),new j(m,u)}for(m=b;0<=v.l(u);){for(f=Math.max(1,Math.floor(v.m()/u.m())),g=Math.ceil(Math.log(f)/Math.LN2),g=48>=g?1:Math.pow(2,g-48),w=E(f),d=w.j(u);U(d)||0<d.l(v);)f-=g,w=E(f),d=w.j(u);P(w)&&(w=R),m=m.add(w),v=ce(v,d)}return new j(m,v)}i.A=function(v){return Z(this,v).h},i.and=function(v){for(var u=Math.max(this.g.length,v.g.length),f=[],g=0;g<u;g++)f[g]=this.i(g)&v.i(g);return new l(f,this.h&v.h)},i.or=function(v){for(var u=Math.max(this.g.length,v.g.length),f=[],g=0;g<u;g++)f[g]=this.i(g)|v.i(g);return new l(f,this.h|v.h)},i.xor=function(v){for(var u=Math.max(this.g.length,v.g.length),f=[],g=0;g<u;g++)f[g]=this.i(g)^v.i(g);return new l(f,this.h^v.h)};function De(v){for(var u=v.g.length+1,f=[],g=0;g<u;g++)f[g]=v.i(g)<<1|v.i(g-1)>>>31;return new l(f,v.h)}function q(v,u){var f=u>>5;u%=32;for(var g=v.g.length-f,m=[],w=0;w<g;w++)m[w]=0<u?v.i(w+f)>>>u|v.i(w+f+1)<<32-u:v.i(w+f);return new l(m,v.h)}r.prototype.digest=r.prototype.v,r.prototype.reset=r.prototype.s,r.prototype.update=r.prototype.u,l.prototype.add=l.prototype.add,l.prototype.multiply=l.prototype.j,l.prototype.modulo=l.prototype.A,l.prototype.compare=l.prototype.l,l.prototype.toNumber=l.prototype.m,l.prototype.toString=l.prototype.toString,l.prototype.getBits=l.prototype.i,l.fromNumber=E,l.fromString=S,Xs=l}).apply(typeof qr<"u"?qr:typeof self<"u"?self:typeof window<"u"?window:{});var Jt=typeof globalThis<"u"?globalThis:typeof window<"u"?window:typeof global<"u"?global:typeof self<"u"?self:{};(function(){var i,e=typeof Object.defineProperties=="function"?Object.defineProperty:function(t,s,a){return t==Array.prototype||t==Object.prototype||(t[s]=a.value),t};function n(t){t=[typeof globalThis=="object"&&globalThis,t,typeof window=="object"&&window,typeof self=="object"&&self,typeof Jt=="object"&&Jt];for(var s=0;s<t.length;++s){var a=t[s];if(a&&a.Math==Math)return a}throw Error("Cannot find global object")}var r=n(this);function o(t,s){if(s)e:{var a=r;t=t.split(".");for(var h=0;h<t.length-1;h++){var _=t[h];if(!(_ in a))break e;a=a[_]}t=t[t.length-1],h=a[t],s=s(h),s!=h&&s!=null&&e(a,t,{configurable:!0,writable:!0,value:s})}}function c(t,s){t instanceof String&&(t+="");var a=0,h=!1,_={next:function(){if(!h&&a<t.length){var I=a++;return{value:s(I,t[I]),done:!1}}return h=!0,{done:!0,value:void 0}}};return _[Symbol.iterator]=function(){return _},_}o("Array.prototype.values",function(t){return t||function(){return c(this,function(s,a){return a})}});/** @license

 Copyright The Closure Library Authors.
 SPDX-License-Identifier: Apache-2.0
*/var l=l||{},p=this||self;function y(t){var s=typeof t;return s=s!="object"?s:t?Array.isArray(t)?"array":s:"null",s=="array"||s=="object"&&typeof t.length=="number"}function E(t){var s=typeof t;return s=="object"&&t!=null||s=="function"}function S(t,s,a){return t.call.apply(t.bind,arguments)}function b(t,s,a){if(!t)throw Error();if(2<arguments.length){var h=Array.prototype.slice.call(arguments,2);return function(){var _=Array.prototype.slice.call(arguments);return Array.prototype.unshift.apply(_,h),t.apply(s,_)}}return function(){return t.apply(s,arguments)}}function R(t,s,a){return R=Function.prototype.bind&&Function.prototype.bind.toString().indexOf("native code")!=-1?S:b,R.apply(null,arguments)}function x(t,s){var a=Array.prototype.slice.call(arguments,1);return function(){var h=a.slice();return h.push.apply(h,arguments),t.apply(this,h)}}function P(t,s){function a(){}a.prototype=s.prototype,t.aa=s.prototype,t.prototype=new a,t.prototype.constructor=t,t.Qb=function(h,_,I){for(var T=Array(arguments.length-2),D=2;D<arguments.length;D++)T[D-2]=arguments[D];return s.prototype[_].apply(h,T)}}function U(t){const s=t.length;if(0<s){const a=Array(s);for(let h=0;h<s;h++)a[h]=t[h];return a}return[]}function L(t,s){for(let a=1;a<arguments.length;a++){const h=arguments[a];if(y(h)){const _=t.length||0,I=h.length||0;t.length=_+I;for(let T=0;T<I;T++)t[_+T]=h[T]}else t.push(h)}}class ce{constructor(s,a){this.i=s,this.j=a,this.h=0,this.g=null}get(){let s;return 0<this.h?(this.h--,s=this.g,this.g=s.next,s.next=null):s=this.i(),s}}function Y(t){return/^[\s\xa0]*$/.test(t)}function j(){var t=p.navigator;return t&&(t=t.userAgent)?t:""}function Z(t){return Z[" "](t),t}Z[" "]=function(){};var De=j().indexOf("Gecko")!=-1&&!(j().toLowerCase().indexOf("webkit")!=-1&&j().indexOf("Edge")==-1)&&!(j().indexOf("Trident")!=-1||j().indexOf("MSIE")!=-1)&&j().indexOf("Edge")==-1;function q(t,s,a){for(const h in t)s.call(a,t[h],h,t)}function v(t,s){for(const a in t)s.call(void 0,t[a],a,t)}function u(t){const s={};for(const a in t)s[a]=t[a];return s}const f="constructor hasOwnProperty isPrototypeOf propertyIsEnumerable toLocaleString toString valueOf".split(" ");function g(t,s){let a,h;for(let _=1;_<arguments.length;_++){h=arguments[_];for(a in h)t[a]=h[a];for(let I=0;I<f.length;I++)a=f[I],Object.prototype.hasOwnProperty.call(h,a)&&(t[a]=h[a])}}function m(t){var s=1;t=t.split(":");const a=[];for(;0<s&&t.length;)a.push(t.shift()),s--;return t.length&&a.push(t.join(":")),a}function w(t){p.setTimeout(()=>{throw t},0)}function d(){var t=gn;let s=null;return t.g&&(s=t.g,t.g=t.g.next,t.g||(t.h=null),s.next=null),s}class he{constructor(){this.h=this.g=null}add(s,a){const h=et.get();h.set(s,a),this.h?this.h.next=h:this.g=h,this.h=h}}var et=new ce(()=>new io,t=>t.reset());class io{constructor(){this.next=this.g=this.h=null}set(s,a){this.h=s,this.g=a,this.next=null}reset(){this.next=this.g=this.h=null}}let tt,nt=!1,gn=new he,wi=()=>{const t=p.Promise.resolve(void 0);tt=()=>{t.then(ro)}};var ro=()=>{for(var t;t=d();){try{t.h.call(t.g)}catch(a){w(a)}var s=et;s.j(t),100>s.h&&(s.h++,t.next=s.g,s.g=t)}nt=!1};function _e(){this.s=this.s,this.C=this.C}_e.prototype.s=!1,_e.prototype.ma=function(){this.s||(this.s=!0,this.N())},_e.prototype.N=function(){if(this.C)for(;this.C.length;)this.C.shift()()};function B(t,s){this.type=t,this.g=this.target=s,this.defaultPrevented=!1}B.prototype.h=function(){this.defaultPrevented=!0};var so=function(){if(!p.addEventListener||!Object.defineProperty)return!1;var t=!1,s=Object.defineProperty({},"passive",{get:function(){t=!0}});try{const a=()=>{};p.addEventListener("test",a,s),p.removeEventListener("test",a,s)}catch{}return t}();function it(t,s){if(B.call(this,t?t.type:""),this.relatedTarget=this.g=this.target=null,this.button=this.screenY=this.screenX=this.clientY=this.clientX=0,this.key="",this.metaKey=this.shiftKey=this.altKey=this.ctrlKey=!1,this.state=null,this.pointerId=0,this.pointerType="",this.i=null,t){var a=this.type=t.type,h=t.changedTouches&&t.changedTouches.length?t.changedTouches[0]:null;if(this.target=t.target||t.srcElement,this.g=s,s=t.relatedTarget){if(De){e:{try{Z(s.nodeName);var _=!0;break e}catch{}_=!1}_||(s=null)}}else a=="mouseover"?s=t.fromElement:a=="mouseout"&&(s=t.toElement);this.relatedTarget=s,h?(this.clientX=h.clientX!==void 0?h.clientX:h.pageX,this.clientY=h.clientY!==void 0?h.clientY:h.pageY,this.screenX=h.screenX||0,this.screenY=h.screenY||0):(this.clientX=t.clientX!==void 0?t.clientX:t.pageX,this.clientY=t.clientY!==void 0?t.clientY:t.pageY,this.screenX=t.screenX||0,this.screenY=t.screenY||0),this.button=t.button,this.key=t.key||"",this.ctrlKey=t.ctrlKey,this.altKey=t.altKey,this.shiftKey=t.shiftKey,this.metaKey=t.metaKey,this.pointerId=t.pointerId||0,this.pointerType=typeof t.pointerType=="string"?t.pointerType:oo[t.pointerType]||"",this.state=t.state,this.i=t,t.defaultPrevented&&it.aa.h.call(this)}}P(it,B);var oo={2:"touch",3:"pen",4:"mouse"};it.prototype.h=function(){it.aa.h.call(this);var t=this.i;t.preventDefault?t.preventDefault():t.returnValue=!1};var Dt="closure_listenable_"+(1e6*Math.random()|0),ao=0;function co(t,s,a,h,_){this.listener=t,this.proxy=null,this.src=s,this.type=a,this.capture=!!h,this.ha=_,this.key=++ao,this.da=this.fa=!1}function Nt(t){t.da=!0,t.listener=null,t.proxy=null,t.src=null,t.ha=null}function Lt(t){this.src=t,this.g={},this.h=0}Lt.prototype.add=function(t,s,a,h,_){var I=t.toString();t=this.g[I],t||(t=this.g[I]=[],this.h++);var T=vn(t,s,h,_);return-1<T?(s=t[T],a||(s.fa=!1)):(s=new co(s,this.src,I,!!h,_),s.fa=a,t.push(s)),s};function mn(t,s){var a=s.type;if(a in t.g){var h=t.g[a],_=Array.prototype.indexOf.call(h,s,void 0),I;(I=0<=_)&&Array.prototype.splice.call(h,_,1),I&&(Nt(s),t.g[a].length==0&&(delete t.g[a],t.h--))}}function vn(t,s,a,h){for(var _=0;_<t.length;++_){var I=t[_];if(!I.da&&I.listener==s&&I.capture==!!a&&I.ha==h)return _}return-1}var _n="closure_lm_"+(1e6*Math.random()|0),yn={};function Ii(t,s,a,h,_){if(Array.isArray(s)){for(var I=0;I<s.length;I++)Ii(t,s[I],a,h,_);return null}return a=Ai(a),t&&t[Dt]?t.K(s,a,E(h)?!!h.capture:!1,_):ho(t,s,a,!1,h,_)}function ho(t,s,a,h,_,I){if(!s)throw Error("Invalid event type");var T=E(_)?!!_.capture:!!_,D=In(t);if(D||(t[_n]=D=new Lt(t)),a=D.add(s,a,h,T,I),a.proxy)return a;if(h=lo(),a.proxy=h,h.src=t,h.listener=a,t.addEventListener)so||(_=T),_===void 0&&(_=!1),t.addEventListener(s.toString(),h,_);else if(t.attachEvent)t.attachEvent(Ti(s.toString()),h);else if(t.addListener&&t.removeListener)t.addListener(h);else throw Error("addEventListener and attachEvent are unavailable.");return a}function lo(){function t(a){return s.call(t.src,t.listener,a)}const s=uo;return t}function Ei(t,s,a,h,_){if(Array.isArray(s))for(var I=0;I<s.length;I++)Ei(t,s[I],a,h,_);else h=E(h)?!!h.capture:!!h,a=Ai(a),t&&t[Dt]?(t=t.i,s=String(s).toString(),s in t.g&&(I=t.g[s],a=vn(I,a,h,_),-1<a&&(Nt(I[a]),Array.prototype.splice.call(I,a,1),I.length==0&&(delete t.g[s],t.h--)))):t&&(t=In(t))&&(s=t.g[s.toString()],t=-1,s&&(t=vn(s,a,h,_)),(a=-1<t?s[t]:null)&&wn(a))}function wn(t){if(typeof t!="number"&&t&&!t.da){var s=t.src;if(s&&s[Dt])mn(s.i,t);else{var a=t.type,h=t.proxy;s.removeEventListener?s.removeEventListener(a,h,t.capture):s.detachEvent?s.detachEvent(Ti(a),h):s.addListener&&s.removeListener&&s.removeListener(h),(a=In(s))?(mn(a,t),a.h==0&&(a.src=null,s[_n]=null)):Nt(t)}}}function Ti(t){return t in yn?yn[t]:yn[t]="on"+t}function uo(t,s){if(t.da)t=!0;else{s=new it(s,this);var a=t.listener,h=t.ha||t.src;t.fa&&wn(t),t=a.call(h,s)}return t}function In(t){return t=t[_n],t instanceof Lt?t:null}var En="__closure_events_fn_"+(1e9*Math.random()>>>0);function Ai(t){return typeof t=="function"?t:(t[En]||(t[En]=function(s){return t.handleEvent(s)}),t[En])}function V(){_e.call(this),this.i=new Lt(this),this.M=this,this.F=null}P(V,_e),V.prototype[Dt]=!0,V.prototype.removeEventListener=function(t,s,a,h){Ei(this,t,s,a,h)};function W(t,s){var a,h=t.F;if(h)for(a=[];h;h=h.F)a.push(h);if(t=t.M,h=s.type||s,typeof s=="string")s=new B(s,t);else if(s instanceof B)s.target=s.target||t;else{var _=s;s=new B(h,t),g(s,_)}if(_=!0,a)for(var I=a.length-1;0<=I;I--){var T=s.g=a[I];_=Mt(T,h,!0,s)&&_}if(T=s.g=t,_=Mt(T,h,!0,s)&&_,_=Mt(T,h,!1,s)&&_,a)for(I=0;I<a.length;I++)T=s.g=a[I],_=Mt(T,h,!1,s)&&_}V.prototype.N=function(){if(V.aa.N.call(this),this.i){var t=this.i,s;for(s in t.g){for(var a=t.g[s],h=0;h<a.length;h++)Nt(a[h]);delete t.g[s],t.h--}}this.F=null},V.prototype.K=function(t,s,a,h){return this.i.add(String(t),s,!1,a,h)},V.prototype.L=function(t,s,a,h){return this.i.add(String(t),s,!0,a,h)};function Mt(t,s,a,h){if(s=t.i.g[String(s)],!s)return!0;s=s.concat();for(var _=!0,I=0;I<s.length;++I){var T=s[I];if(T&&!T.da&&T.capture==a){var D=T.listener,F=T.ha||T.src;T.fa&&mn(t.i,T),_=D.call(F,h)!==!1&&_}}return _&&!h.defaultPrevented}function Si(t,s,a){if(typeof t=="function")a&&(t=R(t,a));else if(t&&typeof t.handleEvent=="function")t=R(t.handleEvent,t);else throw Error("Invalid listener argument");return 2147483647<Number(s)?-1:p.setTimeout(t,s||0)}function bi(t){t.g=Si(()=>{t.g=null,t.i&&(t.i=!1,bi(t))},t.l);const s=t.h;t.h=null,t.m.apply(null,s)}class fo extends _e{constructor(s,a){super(),this.m=s,this.l=a,this.h=null,this.i=!1,this.g=null}j(s){this.h=arguments,this.g?this.i=!0:bi(this)}N(){super.N(),this.g&&(p.clearTimeout(this.g),this.g=null,this.i=!1,this.h=null)}}function rt(t){_e.call(this),this.h=t,this.g={}}P(rt,_e);var ki=[];function Pi(t){q(t.g,function(s,a){this.g.hasOwnProperty(a)&&wn(s)},t),t.g={}}rt.prototype.N=function(){rt.aa.N.call(this),Pi(this)},rt.prototype.handleEvent=function(){throw Error("EventHandler.handleEvent not implemented")};var Tn=p.JSON.stringify,po=p.JSON.parse,go=class{stringify(t){return p.JSON.stringify(t,void 0)}parse(t){return p.JSON.parse(t,void 0)}};function An(){}An.prototype.h=null;function Ri(t){return t.h||(t.h=t.i())}function mo(){}var st={OPEN:"a",kb:"b",Ja:"c",wb:"d"};function Sn(){B.call(this,"d")}P(Sn,B);function bn(){B.call(this,"c")}P(bn,B);var Be={},Ci=null;function kn(){return Ci=Ci||new V}Be.La="serverreachability";function Oi(t){B.call(this,Be.La,t)}P(Oi,B);function ot(t){const s=kn();W(s,new Oi(s))}Be.STAT_EVENT="statevent";function Di(t,s){B.call(this,Be.STAT_EVENT,t),this.stat=s}P(Di,B);function z(t){const s=kn();W(s,new Di(s,t))}Be.Ma="timingevent";function Ni(t,s){B.call(this,Be.Ma,t),this.size=s}P(Ni,B);function at(t,s){if(typeof t!="function")throw Error("Fn must not be null and must be a function");return p.setTimeout(function(){t()},s)}function ct(){this.g=!0}ct.prototype.xa=function(){this.g=!1};function vo(t,s,a,h,_,I){t.info(function(){if(t.g)if(I)for(var T="",D=I.split("&"),F=0;F<D.length;F++){var C=D[F].split("=");if(1<C.length){var H=C[0];C=C[1];var $=H.split("_");T=2<=$.length&&$[1]=="type"?T+(H+"="+C+"&"):T+(H+"=redacted&")}}else T=null;else T=I;return"XMLHTTP REQ ("+h+") [attempt "+_+"]: "+s+`
`+a+`
`+T})}function _o(t,s,a,h,_,I,T){t.info(function(){return"XMLHTTP RESP ("+h+") [ attempt "+_+"]: "+s+`
`+a+`
`+I+" "+T})}function Ve(t,s,a,h){t.info(function(){return"XMLHTTP TEXT ("+s+"): "+wo(t,a)+(h?" "+h:"")})}function yo(t,s){t.info(function(){return"TIMEOUT: "+s})}ct.prototype.info=function(){};function wo(t,s){if(!t.g)return s;if(!s)return null;try{var a=JSON.parse(s);if(a){for(t=0;t<a.length;t++)if(Array.isArray(a[t])){var h=a[t];if(!(2>h.length)){var _=h[1];if(Array.isArray(_)&&!(1>_.length)){var I=_[0];if(I!="noop"&&I!="stop"&&I!="close")for(var T=1;T<_.length;T++)_[T]=""}}}}return Tn(a)}catch{return s}}var Pn={NO_ERROR:0,TIMEOUT:8},Io={},Rn;function Ut(){}P(Ut,An),Ut.prototype.g=function(){return new XMLHttpRequest},Ut.prototype.i=function(){return{}},Rn=new Ut;function ye(t,s,a,h){this.j=t,this.i=s,this.l=a,this.R=h||1,this.U=new rt(this),this.I=45e3,this.H=null,this.o=!1,this.m=this.A=this.v=this.L=this.F=this.S=this.B=null,this.D=[],this.g=null,this.C=0,this.s=this.u=null,this.X=-1,this.J=!1,this.O=0,this.M=null,this.W=this.K=this.T=this.P=!1,this.h=new Li}function Li(){this.i=null,this.g="",this.h=!1}var Mi={},Cn={};function On(t,s,a){t.L=1,t.v=Bt(le(s)),t.m=a,t.P=!0,Ui(t,null)}function Ui(t,s){t.F=Date.now(),xt(t),t.A=le(t.v);var a=t.A,h=t.R;Array.isArray(h)||(h=[String(h)]),Xi(a.i,"t",h),t.C=0,a=t.j.J,t.h=new Li,t.g=pr(t.j,a?s:null,!t.m),0<t.O&&(t.M=new fo(R(t.Y,t,t.g),t.O)),s=t.U,a=t.g,h=t.ca;var _="readystatechange";Array.isArray(_)||(_&&(ki[0]=_.toString()),_=ki);for(var I=0;I<_.length;I++){var T=Ii(a,_[I],h||s.handleEvent,!1,s.h||s);if(!T)break;s.g[T.key]=T}s=t.H?u(t.H):{},t.m?(t.u||(t.u="POST"),s["Content-Type"]="application/x-www-form-urlencoded",t.g.ea(t.A,t.u,t.m,s)):(t.u="GET",t.g.ea(t.A,t.u,null,s)),ot(),vo(t.i,t.u,t.A,t.l,t.R,t.m)}ye.prototype.ca=function(t){t=t.target;const s=this.M;s&&ue(t)==3?s.j():this.Y(t)},ye.prototype.Y=function(t){try{if(t==this.g)e:{const $=ue(this.g);var s=this.g.Ba();const We=this.g.Z();if(!(3>$)&&($!=3||this.g&&(this.h.h||this.g.oa()||ir(this.g)))){this.J||$!=4||s==7||(s==8||0>=We?ot(3):ot(2)),Dn(this);var a=this.g.Z();this.X=a;t:if(xi(this)){var h=ir(this.g);t="";var _=h.length,I=ue(this.g)==4;if(!this.h.i){if(typeof TextDecoder>"u"){Ne(this),ht(this);var T="";break t}this.h.i=new p.TextDecoder}for(s=0;s<_;s++)this.h.h=!0,t+=this.h.i.decode(h[s],{stream:!(I&&s==_-1)});h.length=0,this.h.g+=t,this.C=0,T=this.h.g}else T=this.g.oa();if(this.o=a==200,_o(this.i,this.u,this.A,this.l,this.R,$,a),this.o){if(this.T&&!this.K){t:{if(this.g){var D,F=this.g;if((D=F.g?F.g.getResponseHeader("X-HTTP-Initial-Response"):null)&&!Y(D)){var C=D;break t}}C=null}if(a=C)Ve(this.i,this.l,a,"Initial handshake response via X-HTTP-Initial-Response"),this.K=!0,Nn(this,a);else{this.o=!1,this.s=3,z(12),Ne(this),ht(this);break e}}if(this.P){a=!0;let ee;for(;!this.J&&this.C<T.length;)if(ee=Eo(this,T),ee==Cn){$==4&&(this.s=4,z(14),a=!1),Ve(this.i,this.l,null,"[Incomplete Response]");break}else if(ee==Mi){this.s=4,z(15),Ve(this.i,this.l,T,"[Invalid Chunk]"),a=!1;break}else Ve(this.i,this.l,ee,null),Nn(this,ee);if(xi(this)&&this.C!=0&&(this.h.g=this.h.g.slice(this.C),this.C=0),$!=4||T.length!=0||this.h.h||(this.s=1,z(16),a=!1),this.o=this.o&&a,!a)Ve(this.i,this.l,T,"[Invalid Chunked Response]"),Ne(this),ht(this);else if(0<T.length&&!this.W){this.W=!0;var H=this.j;H.g==this&&H.ba&&!H.M&&(H.j.info("Great, no buffering proxy detected. Bytes received: "+T.length),jn(H),H.M=!0,z(11))}}else Ve(this.i,this.l,T,null),Nn(this,T);$==4&&Ne(this),this.o&&!this.J&&($==4?lr(this.j,this):(this.o=!1,xt(this)))}else jo(this.g),a==400&&0<T.indexOf("Unknown SID")?(this.s=3,z(12)):(this.s=0,z(13)),Ne(this),ht(this)}}}catch{}finally{}};function xi(t){return t.g?t.u=="GET"&&t.L!=2&&t.j.Ca:!1}function Eo(t,s){var a=t.C,h=s.indexOf(`
`,a);return h==-1?Cn:(a=Number(s.substring(a,h)),isNaN(a)?Mi:(h+=1,h+a>s.length?Cn:(s=s.slice(h,h+a),t.C=h+a,s)))}ye.prototype.cancel=function(){this.J=!0,Ne(this)};function xt(t){t.S=Date.now()+t.I,Fi(t,t.I)}function Fi(t,s){if(t.B!=null)throw Error("WatchDog timer not null");t.B=at(R(t.ba,t),s)}function Dn(t){t.B&&(p.clearTimeout(t.B),t.B=null)}ye.prototype.ba=function(){this.B=null;const t=Date.now();0<=t-this.S?(yo(this.i,this.A),this.L!=2&&(ot(),z(17)),Ne(this),this.s=2,ht(this)):Fi(this,this.S-t)};function ht(t){t.j.G==0||t.J||lr(t.j,t)}function Ne(t){Dn(t);var s=t.M;s&&typeof s.ma=="function"&&s.ma(),t.M=null,Pi(t.U),t.g&&(s=t.g,t.g=null,s.abort(),s.ma())}function Nn(t,s){try{var a=t.j;if(a.G!=0&&(a.g==t||Ln(a.h,t))){if(!t.K&&Ln(a.h,t)&&a.G==3){try{var h=a.Da.g.parse(s)}catch{h=null}if(Array.isArray(h)&&h.length==3){var _=h;if(_[0]==0){e:if(!a.u){if(a.g)if(a.g.F+3e3<t.F)Gt(a),Wt(a);else break e;Fn(a),z(18)}}else a.za=_[1],0<a.za-a.T&&37500>_[2]&&a.F&&a.v==0&&!a.C&&(a.C=at(R(a.Za,a),6e3));if(1>=Vi(a.h)&&a.ca){try{a.ca()}catch{}a.ca=void 0}}else Me(a,11)}else if((t.K||a.g==t)&&Gt(a),!Y(s))for(_=a.Da.g.parse(s),s=0;s<_.length;s++){let C=_[s];if(a.T=C[0],C=C[1],a.G==2)if(C[0]=="c"){a.K=C[1],a.ia=C[2];const H=C[3];H!=null&&(a.la=H,a.j.info("VER="+a.la));const $=C[4];$!=null&&(a.Aa=$,a.j.info("SVER="+a.Aa));const We=C[5];We!=null&&typeof We=="number"&&0<We&&(h=1.5*We,a.L=h,a.j.info("backChannelRequestTimeoutMs_="+h)),h=a;const ee=t.g;if(ee){const Kt=ee.g?ee.g.getResponseHeader("X-Client-Wire-Protocol"):null;if(Kt){var I=h.h;I.g||Kt.indexOf("spdy")==-1&&Kt.indexOf("quic")==-1&&Kt.indexOf("h2")==-1||(I.j=I.l,I.g=new Set,I.h&&(Mn(I,I.h),I.h=null))}if(h.D){const Bn=ee.g?ee.g.getResponseHeader("X-HTTP-Session-Id"):null;Bn&&(h.ya=Bn,N(h.I,h.D,Bn))}}a.G=3,a.l&&a.l.ua(),a.ba&&(a.R=Date.now()-t.F,a.j.info("Handshake RTT: "+a.R+"ms")),h=a;var T=t;if(h.qa=fr(h,h.J?h.ia:null,h.W),T.K){Hi(h.h,T);var D=T,F=h.L;F&&(D.I=F),D.B&&(Dn(D),xt(D)),h.g=T}else cr(h);0<a.i.length&&zt(a)}else C[0]!="stop"&&C[0]!="close"||Me(a,7);else a.G==3&&(C[0]=="stop"||C[0]=="close"?C[0]=="stop"?Me(a,7):xn(a):C[0]!="noop"&&a.l&&a.l.ta(C),a.v=0)}}ot(4)}catch{}}var To=class{constructor(t,s){this.g=t,this.map=s}};function ji(t){this.l=t||10,p.PerformanceNavigationTiming?(t=p.performance.getEntriesByType("navigation"),t=0<t.length&&(t[0].nextHopProtocol=="hq"||t[0].nextHopProtocol=="h2")):t=!!(p.chrome&&p.chrome.loadTimes&&p.chrome.loadTimes()&&p.chrome.loadTimes().wasFetchedViaSpdy),this.j=t?this.l:1,this.g=null,1<this.j&&(this.g=new Set),this.h=null,this.i=[]}function Bi(t){return t.h?!0:t.g?t.g.size>=t.j:!1}function Vi(t){return t.h?1:t.g?t.g.size:0}function Ln(t,s){return t.h?t.h==s:t.g?t.g.has(s):!1}function Mn(t,s){t.g?t.g.add(s):t.h=s}function Hi(t,s){t.h&&t.h==s?t.h=null:t.g&&t.g.has(s)&&t.g.delete(s)}ji.prototype.cancel=function(){if(this.i=$i(this),this.h)this.h.cancel(),this.h=null;else if(this.g&&this.g.size!==0){for(const t of this.g.values())t.cancel();this.g.clear()}};function $i(t){if(t.h!=null)return t.i.concat(t.h.D);if(t.g!=null&&t.g.size!==0){let s=t.i;for(const a of t.g.values())s=s.concat(a.D);return s}return U(t.i)}function Ao(t){if(t.V&&typeof t.V=="function")return t.V();if(typeof Map<"u"&&t instanceof Map||typeof Set<"u"&&t instanceof Set)return Array.from(t.values());if(typeof t=="string")return t.split("");if(y(t)){for(var s=[],a=t.length,h=0;h<a;h++)s.push(t[h]);return s}s=[],a=0;for(h in t)s[a++]=t[h];return s}function So(t){if(t.na&&typeof t.na=="function")return t.na();if(!t.V||typeof t.V!="function"){if(typeof Map<"u"&&t instanceof Map)return Array.from(t.keys());if(!(typeof Set<"u"&&t instanceof Set)){if(y(t)||typeof t=="string"){var s=[];t=t.length;for(var a=0;a<t;a++)s.push(a);return s}s=[],a=0;for(const h in t)s[a++]=h;return s}}}function Wi(t,s){if(t.forEach&&typeof t.forEach=="function")t.forEach(s,void 0);else if(y(t)||typeof t=="string")Array.prototype.forEach.call(t,s,void 0);else for(var a=So(t),h=Ao(t),_=h.length,I=0;I<_;I++)s.call(void 0,h[I],a&&a[I],t)}var zi=RegExp("^(?:([^:/?#.]+):)?(?://(?:([^\\\\/?#]*)@)?([^\\\\/?#]*?)(?::([0-9]+))?(?=[\\\\/?#]|$))?([^?#]+)?(?:\\?([^#]*))?(?:#([\\s\\S]*))?$");function bo(t,s){if(t){t=t.split("&");for(var a=0;a<t.length;a++){var h=t[a].indexOf("="),_=null;if(0<=h){var I=t[a].substring(0,h);_=t[a].substring(h+1)}else I=t[a];s(I,_?decodeURIComponent(_.replace(/\+/g," ")):"")}}}function Le(t){if(this.g=this.o=this.j="",this.s=null,this.m=this.l="",this.h=!1,t instanceof Le){this.h=t.h,Ft(this,t.j),this.o=t.o,this.g=t.g,jt(this,t.s),this.l=t.l;var s=t.i,a=new dt;a.i=s.i,s.g&&(a.g=new Map(s.g),a.h=s.h),Gi(this,a),this.m=t.m}else t&&(s=String(t).match(zi))?(this.h=!1,Ft(this,s[1]||"",!0),this.o=lt(s[2]||""),this.g=lt(s[3]||"",!0),jt(this,s[4]),this.l=lt(s[5]||"",!0),Gi(this,s[6]||"",!0),this.m=lt(s[7]||"")):(this.h=!1,this.i=new dt(null,this.h))}Le.prototype.toString=function(){var t=[],s=this.j;s&&t.push(ut(s,Ki,!0),":");var a=this.g;return(a||s=="file")&&(t.push("//"),(s=this.o)&&t.push(ut(s,Ki,!0),"@"),t.push(encodeURIComponent(String(a)).replace(/%25([0-9a-fA-F]{2})/g,"%$1")),a=this.s,a!=null&&t.push(":",String(a))),(a=this.l)&&(this.g&&a.charAt(0)!="/"&&t.push("/"),t.push(ut(a,a.charAt(0)=="/"?Ro:Po,!0))),(a=this.i.toString())&&t.push("?",a),(a=this.m)&&t.push("#",ut(a,Oo)),t.join("")};function le(t){return new Le(t)}function Ft(t,s,a){t.j=a?lt(s,!0):s,t.j&&(t.j=t.j.replace(/:$/,""))}function jt(t,s){if(s){if(s=Number(s),isNaN(s)||0>s)throw Error("Bad port number "+s);t.s=s}else t.s=null}function Gi(t,s,a){s instanceof dt?(t.i=s,Do(t.i,t.h)):(a||(s=ut(s,Co)),t.i=new dt(s,t.h))}function N(t,s,a){t.i.set(s,a)}function Bt(t){return N(t,"zx",Math.floor(2147483648*Math.random()).toString(36)+Math.abs(Math.floor(2147483648*Math.random())^Date.now()).toString(36)),t}function lt(t,s){return t?s?decodeURI(t.replace(/%25/g,"%2525")):decodeURIComponent(t):""}function ut(t,s,a){return typeof t=="string"?(t=encodeURI(t).replace(s,ko),a&&(t=t.replace(/%25([0-9a-fA-F]{2})/g,"%$1")),t):null}function ko(t){return t=t.charCodeAt(0),"%"+(t>>4&15).toString(16)+(t&15).toString(16)}var Ki=/[#\/\?@]/g,Po=/[#\?:]/g,Ro=/[#\?]/g,Co=/[#\?@]/g,Oo=/#/g;function dt(t,s){this.h=this.g=null,this.i=t||null,this.j=!!s}function we(t){t.g||(t.g=new Map,t.h=0,t.i&&bo(t.i,function(s,a){t.add(decodeURIComponent(s.replace(/\+/g," ")),a)}))}i=dt.prototype,i.add=function(t,s){we(this),this.i=null,t=He(this,t);var a=this.g.get(t);return a||this.g.set(t,a=[]),a.push(s),this.h+=1,this};function qi(t,s){we(t),s=He(t,s),t.g.has(s)&&(t.i=null,t.h-=t.g.get(s).length,t.g.delete(s))}function Ji(t,s){return we(t),s=He(t,s),t.g.has(s)}i.forEach=function(t,s){we(this),this.g.forEach(function(a,h){a.forEach(function(_){t.call(s,_,h,this)},this)},this)},i.na=function(){we(this);const t=Array.from(this.g.values()),s=Array.from(this.g.keys()),a=[];for(let h=0;h<s.length;h++){const _=t[h];for(let I=0;I<_.length;I++)a.push(s[h])}return a},i.V=function(t){we(this);let s=[];if(typeof t=="string")Ji(this,t)&&(s=s.concat(this.g.get(He(this,t))));else{t=Array.from(this.g.values());for(let a=0;a<t.length;a++)s=s.concat(t[a])}return s},i.set=function(t,s){return we(this),this.i=null,t=He(this,t),Ji(this,t)&&(this.h-=this.g.get(t).length),this.g.set(t,[s]),this.h+=1,this},i.get=function(t,s){return t?(t=this.V(t),0<t.length?String(t[0]):s):s};function Xi(t,s,a){qi(t,s),0<a.length&&(t.i=null,t.g.set(He(t,s),U(a)),t.h+=a.length)}i.toString=function(){if(this.i)return this.i;if(!this.g)return"";const t=[],s=Array.from(this.g.keys());for(var a=0;a<s.length;a++){var h=s[a];const I=encodeURIComponent(String(h)),T=this.V(h);for(h=0;h<T.length;h++){var _=I;T[h]!==""&&(_+="="+encodeURIComponent(String(T[h]))),t.push(_)}}return this.i=t.join("&")};function He(t,s){return s=String(s),t.j&&(s=s.toLowerCase()),s}function Do(t,s){s&&!t.j&&(we(t),t.i=null,t.g.forEach(function(a,h){var _=h.toLowerCase();h!=_&&(qi(this,h),Xi(this,_,a))},t)),t.j=s}function No(t,s){const a=new ct;if(p.Image){const h=new Image;h.onload=x(Ie,a,"TestLoadImage: loaded",!0,s,h),h.onerror=x(Ie,a,"TestLoadImage: error",!1,s,h),h.onabort=x(Ie,a,"TestLoadImage: abort",!1,s,h),h.ontimeout=x(Ie,a,"TestLoadImage: timeout",!1,s,h),p.setTimeout(function(){h.ontimeout&&h.ontimeout()},1e4),h.src=t}else s(!1)}function Lo(t,s){const a=new ct,h=new AbortController,_=setTimeout(()=>{h.abort(),Ie(a,"TestPingServer: timeout",!1,s)},1e4);fetch(t,{signal:h.signal}).then(I=>{clearTimeout(_),I.ok?Ie(a,"TestPingServer: ok",!0,s):Ie(a,"TestPingServer: server error",!1,s)}).catch(()=>{clearTimeout(_),Ie(a,"TestPingServer: error",!1,s)})}function Ie(t,s,a,h,_){try{_&&(_.onload=null,_.onerror=null,_.onabort=null,_.ontimeout=null),h(a)}catch{}}function Mo(){this.g=new go}function Uo(t,s,a){const h=a||"";try{Wi(t,function(_,I){let T=_;E(_)&&(T=Tn(_)),s.push(h+I+"="+encodeURIComponent(T))})}catch(_){throw s.push(h+"type="+encodeURIComponent("_badmap")),_}}function Vt(t){this.l=t.Ub||null,this.j=t.eb||!1}P(Vt,An),Vt.prototype.g=function(){return new Ht(this.l,this.j)},Vt.prototype.i=function(t){return function(){return t}}({});function Ht(t,s){V.call(this),this.D=t,this.o=s,this.m=void 0,this.status=this.readyState=0,this.responseType=this.responseText=this.response=this.statusText="",this.onreadystatechange=null,this.u=new Headers,this.h=null,this.B="GET",this.A="",this.g=!1,this.v=this.j=this.l=null}P(Ht,V),i=Ht.prototype,i.open=function(t,s){if(this.readyState!=0)throw this.abort(),Error("Error reopening a connection");this.B=t,this.A=s,this.readyState=1,pt(this)},i.send=function(t){if(this.readyState!=1)throw this.abort(),Error("need to call open() first. ");this.g=!0;const s={headers:this.u,method:this.B,credentials:this.m,cache:void 0};t&&(s.body=t),(this.D||p).fetch(new Request(this.A,s)).then(this.Sa.bind(this),this.ga.bind(this))},i.abort=function(){this.response=this.responseText="",this.u=new Headers,this.status=0,this.j&&this.j.cancel("Request was aborted.").catch(()=>{}),1<=this.readyState&&this.g&&this.readyState!=4&&(this.g=!1,ft(this)),this.readyState=0},i.Sa=function(t){if(this.g&&(this.l=t,this.h||(this.status=this.l.status,this.statusText=this.l.statusText,this.h=t.headers,this.readyState=2,pt(this)),this.g&&(this.readyState=3,pt(this),this.g)))if(this.responseType==="arraybuffer")t.arrayBuffer().then(this.Qa.bind(this),this.ga.bind(this));else if(typeof p.ReadableStream<"u"&&"body"in t){if(this.j=t.body.getReader(),this.o){if(this.responseType)throw Error('responseType must be empty for "streamBinaryChunks" mode responses.');this.response=[]}else this.response=this.responseText="",this.v=new TextDecoder;Yi(this)}else t.text().then(this.Ra.bind(this),this.ga.bind(this))};function Yi(t){t.j.read().then(t.Pa.bind(t)).catch(t.ga.bind(t))}i.Pa=function(t){if(this.g){if(this.o&&t.value)this.response.push(t.value);else if(!this.o){var s=t.value?t.value:new Uint8Array(0);(s=this.v.decode(s,{stream:!t.done}))&&(this.response=this.responseText+=s)}t.done?ft(this):pt(this),this.readyState==3&&Yi(this)}},i.Ra=function(t){this.g&&(this.response=this.responseText=t,ft(this))},i.Qa=function(t){this.g&&(this.response=t,ft(this))},i.ga=function(){this.g&&ft(this)};function ft(t){t.readyState=4,t.l=null,t.j=null,t.v=null,pt(t)}i.setRequestHeader=function(t,s){this.u.append(t,s)},i.getResponseHeader=function(t){return this.h&&this.h.get(t.toLowerCase())||""},i.getAllResponseHeaders=function(){if(!this.h)return"";const t=[],s=this.h.entries();for(var a=s.next();!a.done;)a=a.value,t.push(a[0]+": "+a[1]),a=s.next();return t.join(`\r
`)};function pt(t){t.onreadystatechange&&t.onreadystatechange.call(t)}Object.defineProperty(Ht.prototype,"withCredentials",{get:function(){return this.m==="include"},set:function(t){this.m=t?"include":"same-origin"}});function Qi(t){let s="";return q(t,function(a,h){s+=h,s+=":",s+=a,s+=`\r
`}),s}function Un(t,s,a){e:{for(h in a){var h=!1;break e}h=!0}h||(a=Qi(a),typeof t=="string"?a!=null&&encodeURIComponent(String(a)):N(t,s,a))}function M(t){V.call(this),this.headers=new Map,this.o=t||null,this.h=!1,this.v=this.g=null,this.D="",this.m=0,this.l="",this.j=this.B=this.u=this.A=!1,this.I=null,this.H="",this.J=!1}P(M,V);var xo=/^https?$/i,Fo=["POST","PUT"];i=M.prototype,i.Ha=function(t){this.J=t},i.ea=function(t,s,a,h){if(this.g)throw Error("[goog.net.XhrIo] Object is active with another request="+this.D+"; newUri="+t);s=s?s.toUpperCase():"GET",this.D=t,this.l="",this.m=0,this.A=!1,this.h=!0,this.g=this.o?this.o.g():Rn.g(),this.v=this.o?Ri(this.o):Ri(Rn),this.g.onreadystatechange=R(this.Ea,this);try{this.B=!0,this.g.open(s,String(t),!0),this.B=!1}catch(I){Zi(this,I);return}if(t=a||"",a=new Map(this.headers),h)if(Object.getPrototypeOf(h)===Object.prototype)for(var _ in h)a.set(_,h[_]);else if(typeof h.keys=="function"&&typeof h.get=="function")for(const I of h.keys())a.set(I,h.get(I));else throw Error("Unknown input type for opt_headers: "+String(h));h=Array.from(a.keys()).find(I=>I.toLowerCase()=="content-type"),_=p.FormData&&t instanceof p.FormData,!(0<=Array.prototype.indexOf.call(Fo,s,void 0))||h||_||a.set("Content-Type","application/x-www-form-urlencoded;charset=utf-8");for(const[I,T]of a)this.g.setRequestHeader(I,T);this.H&&(this.g.responseType=this.H),"withCredentials"in this.g&&this.g.withCredentials!==this.J&&(this.g.withCredentials=this.J);try{nr(this),this.u=!0,this.g.send(t),this.u=!1}catch(I){Zi(this,I)}};function Zi(t,s){t.h=!1,t.g&&(t.j=!0,t.g.abort(),t.j=!1),t.l=s,t.m=5,er(t),$t(t)}function er(t){t.A||(t.A=!0,W(t,"complete"),W(t,"error"))}i.abort=function(t){this.g&&this.h&&(this.h=!1,this.j=!0,this.g.abort(),this.j=!1,this.m=t||7,W(this,"complete"),W(this,"abort"),$t(this))},i.N=function(){this.g&&(this.h&&(this.h=!1,this.j=!0,this.g.abort(),this.j=!1),$t(this,!0)),M.aa.N.call(this)},i.Ea=function(){this.s||(this.B||this.u||this.j?tr(this):this.bb())},i.bb=function(){tr(this)};function tr(t){if(t.h&&typeof l<"u"&&(!t.v[1]||ue(t)!=4||t.Z()!=2)){if(t.u&&ue(t)==4)Si(t.Ea,0,t);else if(W(t,"readystatechange"),ue(t)==4){t.h=!1;try{const T=t.Z();e:switch(T){case 200:case 201:case 202:case 204:case 206:case 304:case 1223:var s=!0;break e;default:s=!1}var a;if(!(a=s)){var h;if(h=T===0){var _=String(t.D).match(zi)[1]||null;!_&&p.self&&p.self.location&&(_=p.self.location.protocol.slice(0,-1)),h=!xo.test(_?_.toLowerCase():"")}a=h}if(a)W(t,"complete"),W(t,"success");else{t.m=6;try{var I=2<ue(t)?t.g.statusText:""}catch{I=""}t.l=I+" ["+t.Z()+"]",er(t)}}finally{$t(t)}}}}function $t(t,s){if(t.g){nr(t);const a=t.g,h=t.v[0]?()=>{}:null;t.g=null,t.v=null,s||W(t,"ready");try{a.onreadystatechange=h}catch{}}}function nr(t){t.I&&(p.clearTimeout(t.I),t.I=null)}i.isActive=function(){return!!this.g};function ue(t){return t.g?t.g.readyState:0}i.Z=function(){try{return 2<ue(this)?this.g.status:-1}catch{return-1}},i.oa=function(){try{return this.g?this.g.responseText:""}catch{return""}},i.Oa=function(t){if(this.g){var s=this.g.responseText;return t&&s.indexOf(t)==0&&(s=s.substring(t.length)),po(s)}};function ir(t){try{if(!t.g)return null;if("response"in t.g)return t.g.response;switch(t.H){case"":case"text":return t.g.responseText;case"arraybuffer":if("mozResponseArrayBuffer"in t.g)return t.g.mozResponseArrayBuffer}return null}catch{return null}}function jo(t){const s={};t=(t.g&&2<=ue(t)&&t.g.getAllResponseHeaders()||"").split(`\r
`);for(let h=0;h<t.length;h++){if(Y(t[h]))continue;var a=m(t[h]);const _=a[0];if(a=a[1],typeof a!="string")continue;a=a.trim();const I=s[_]||[];s[_]=I,I.push(a)}v(s,function(h){return h.join(", ")})}i.Ba=function(){return this.m},i.Ka=function(){return typeof this.l=="string"?this.l:String(this.l)};function gt(t,s,a){return a&&a.internalChannelParams&&a.internalChannelParams[t]||s}function rr(t){this.Aa=0,this.i=[],this.j=new ct,this.ia=this.qa=this.I=this.W=this.g=this.ya=this.D=this.H=this.m=this.S=this.o=null,this.Ya=this.U=0,this.Va=gt("failFast",!1,t),this.F=this.C=this.u=this.s=this.l=null,this.X=!0,this.za=this.T=-1,this.Y=this.v=this.B=0,this.Ta=gt("baseRetryDelayMs",5e3,t),this.cb=gt("retryDelaySeedMs",1e4,t),this.Wa=gt("forwardChannelMaxRetries",2,t),this.wa=gt("forwardChannelRequestTimeoutMs",2e4,t),this.pa=t&&t.xmlHttpFactory||void 0,this.Xa=t&&t.Tb||void 0,this.Ca=t&&t.useFetchStreams||!1,this.L=void 0,this.J=t&&t.supportsCrossDomainXhr||!1,this.K="",this.h=new ji(t&&t.concurrentRequestLimit),this.Da=new Mo,this.P=t&&t.fastHandshake||!1,this.O=t&&t.encodeInitMessageHeaders||!1,this.P&&this.O&&(this.O=!1),this.Ua=t&&t.Rb||!1,t&&t.xa&&this.j.xa(),t&&t.forceLongPolling&&(this.X=!1),this.ba=!this.P&&this.X&&t&&t.detectBufferingProxy||!1,this.ja=void 0,t&&t.longPollingTimeout&&0<t.longPollingTimeout&&(this.ja=t.longPollingTimeout),this.ca=void 0,this.R=0,this.M=!1,this.ka=this.A=null}i=rr.prototype,i.la=8,i.G=1,i.connect=function(t,s,a,h){z(0),this.W=t,this.H=s||{},a&&h!==void 0&&(this.H.OSID=a,this.H.OAID=h),this.F=this.X,this.I=fr(this,null,this.W),zt(this)};function xn(t){if(sr(t),t.G==3){var s=t.U++,a=le(t.I);if(N(a,"SID",t.K),N(a,"RID",s),N(a,"TYPE","terminate"),mt(t,a),s=new ye(t,t.j,s),s.L=2,s.v=Bt(le(a)),a=!1,p.navigator&&p.navigator.sendBeacon)try{a=p.navigator.sendBeacon(s.v.toString(),"")}catch{}!a&&p.Image&&(new Image().src=s.v,a=!0),a||(s.g=pr(s.j,null),s.g.ea(s.v)),s.F=Date.now(),xt(s)}dr(t)}function Wt(t){t.g&&(jn(t),t.g.cancel(),t.g=null)}function sr(t){Wt(t),t.u&&(p.clearTimeout(t.u),t.u=null),Gt(t),t.h.cancel(),t.s&&(typeof t.s=="number"&&p.clearTimeout(t.s),t.s=null)}function zt(t){if(!Bi(t.h)&&!t.s){t.s=!0;var s=t.Ga;tt||wi(),nt||(tt(),nt=!0),gn.add(s,t),t.B=0}}function Bo(t,s){return Vi(t.h)>=t.h.j-(t.s?1:0)?!1:t.s?(t.i=s.D.concat(t.i),!0):t.G==1||t.G==2||t.B>=(t.Va?0:t.Wa)?!1:(t.s=at(R(t.Ga,t,s),ur(t,t.B)),t.B++,!0)}i.Ga=function(t){if(this.s)if(this.s=null,this.G==1){if(!t){this.U=Math.floor(1e5*Math.random()),t=this.U++;const _=new ye(this,this.j,t);let I=this.o;if(this.S&&(I?(I=u(I),g(I,this.S)):I=this.S),this.m!==null||this.O||(_.H=I,I=null),this.P)e:{for(var s=0,a=0;a<this.i.length;a++){t:{var h=this.i[a];if("__data__"in h.map&&(h=h.map.__data__,typeof h=="string")){h=h.length;break t}h=void 0}if(h===void 0)break;if(s+=h,4096<s){s=a;break e}if(s===4096||a===this.i.length-1){s=a+1;break e}}s=1e3}else s=1e3;s=ar(this,_,s),a=le(this.I),N(a,"RID",t),N(a,"CVER",22),this.D&&N(a,"X-HTTP-Session-Id",this.D),mt(this,a),I&&(this.O?s="headers="+encodeURIComponent(String(Qi(I)))+"&"+s:this.m&&Un(a,this.m,I)),Mn(this.h,_),this.Ua&&N(a,"TYPE","init"),this.P?(N(a,"$req",s),N(a,"SID","null"),_.T=!0,On(_,a,null)):On(_,a,s),this.G=2}}else this.G==3&&(t?or(this,t):this.i.length==0||Bi(this.h)||or(this))};function or(t,s){var a;s?a=s.l:a=t.U++;const h=le(t.I);N(h,"SID",t.K),N(h,"RID",a),N(h,"AID",t.T),mt(t,h),t.m&&t.o&&Un(h,t.m,t.o),a=new ye(t,t.j,a,t.B+1),t.m===null&&(a.H=t.o),s&&(t.i=s.D.concat(t.i)),s=ar(t,a,1e3),a.I=Math.round(.5*t.wa)+Math.round(.5*t.wa*Math.random()),Mn(t.h,a),On(a,h,s)}function mt(t,s){t.H&&q(t.H,function(a,h){N(s,h,a)}),t.l&&Wi({},function(a,h){N(s,h,a)})}function ar(t,s,a){a=Math.min(t.i.length,a);var h=t.l?R(t.l.Na,t.l,t):null;e:{var _=t.i;let I=-1;for(;;){const T=["count="+a];I==-1?0<a?(I=_[0].g,T.push("ofs="+I)):I=0:T.push("ofs="+I);let D=!0;for(let F=0;F<a;F++){let C=_[F].g;const H=_[F].map;if(C-=I,0>C)I=Math.max(0,_[F].g-100),D=!1;else try{Uo(H,T,"req"+C+"_")}catch{h&&h(H)}}if(D){h=T.join("&");break e}}}return t=t.i.splice(0,a),s.D=t,h}function cr(t){if(!t.g&&!t.u){t.Y=1;var s=t.Fa;tt||wi(),nt||(tt(),nt=!0),gn.add(s,t),t.v=0}}function Fn(t){return t.g||t.u||3<=t.v?!1:(t.Y++,t.u=at(R(t.Fa,t),ur(t,t.v)),t.v++,!0)}i.Fa=function(){if(this.u=null,hr(this),this.ba&&!(this.M||this.g==null||0>=this.R)){var t=2*this.R;this.j.info("BP detection timer enabled: "+t),this.A=at(R(this.ab,this),t)}},i.ab=function(){this.A&&(this.A=null,this.j.info("BP detection timeout reached."),this.j.info("Buffering proxy detected and switch to long-polling!"),this.F=!1,this.M=!0,z(10),Wt(this),hr(this))};function jn(t){t.A!=null&&(p.clearTimeout(t.A),t.A=null)}function hr(t){t.g=new ye(t,t.j,"rpc",t.Y),t.m===null&&(t.g.H=t.o),t.g.O=0;var s=le(t.qa);N(s,"RID","rpc"),N(s,"SID",t.K),N(s,"AID",t.T),N(s,"CI",t.F?"0":"1"),!t.F&&t.ja&&N(s,"TO",t.ja),N(s,"TYPE","xmlhttp"),mt(t,s),t.m&&t.o&&Un(s,t.m,t.o),t.L&&(t.g.I=t.L);var a=t.g;t=t.ia,a.L=1,a.v=Bt(le(s)),a.m=null,a.P=!0,Ui(a,t)}i.Za=function(){this.C!=null&&(this.C=null,Wt(this),Fn(this),z(19))};function Gt(t){t.C!=null&&(p.clearTimeout(t.C),t.C=null)}function lr(t,s){var a=null;if(t.g==s){Gt(t),jn(t),t.g=null;var h=2}else if(Ln(t.h,s))a=s.D,Hi(t.h,s),h=1;else return;if(t.G!=0){if(s.o)if(h==1){a=s.m?s.m.length:0,s=Date.now()-s.F;var _=t.B;h=kn(),W(h,new Ni(h,a)),zt(t)}else cr(t);else if(_=s.s,_==3||_==0&&0<s.X||!(h==1&&Bo(t,s)||h==2&&Fn(t)))switch(a&&0<a.length&&(s=t.h,s.i=s.i.concat(a)),_){case 1:Me(t,5);break;case 4:Me(t,10);break;case 3:Me(t,6);break;default:Me(t,2)}}}function ur(t,s){let a=t.Ta+Math.floor(Math.random()*t.cb);return t.isActive()||(a*=2),a*s}function Me(t,s){if(t.j.info("Error code "+s),s==2){var a=R(t.fb,t),h=t.Xa;const _=!h;h=new Le(h||"//www.google.com/images/cleardot.gif"),p.location&&p.location.protocol=="http"||Ft(h,"https"),Bt(h),_?No(h.toString(),a):Lo(h.toString(),a)}else z(2);t.G=0,t.l&&t.l.sa(s),dr(t),sr(t)}i.fb=function(t){t?(this.j.info("Successfully pinged google.com"),z(2)):(this.j.info("Failed to ping google.com"),z(1))};function dr(t){if(t.G=0,t.ka=[],t.l){const s=$i(t.h);(s.length!=0||t.i.length!=0)&&(L(t.ka,s),L(t.ka,t.i),t.h.i.length=0,U(t.i),t.i.length=0),t.l.ra()}}function fr(t,s,a){var h=a instanceof Le?le(a):new Le(a);if(h.g!="")s&&(h.g=s+"."+h.g),jt(h,h.s);else{var _=p.location;h=_.protocol,s=s?s+"."+_.hostname:_.hostname,_=+_.port;var I=new Le(null);h&&Ft(I,h),s&&(I.g=s),_&&jt(I,_),a&&(I.l=a),h=I}return a=t.D,s=t.ya,a&&s&&N(h,a,s),N(h,"VER",t.la),mt(t,h),h}function pr(t,s,a){if(s&&!t.J)throw Error("Can't create secondary domain capable XhrIo object.");return s=t.Ca&&!t.pa?new M(new Vt({eb:a})):new M(t.pa),s.Ha(t.J),s}i.isActive=function(){return!!this.l&&this.l.isActive(this)};function gr(){}i=gr.prototype,i.ua=function(){},i.ta=function(){},i.sa=function(){},i.ra=function(){},i.isActive=function(){return!0},i.Na=function(){};function Q(t,s){V.call(this),this.g=new rr(s),this.l=t,this.h=s&&s.messageUrlParams||null,t=s&&s.messageHeaders||null,s&&s.clientProtocolHeaderRequired&&(t?t["X-Client-Protocol"]="webchannel":t={"X-Client-Protocol":"webchannel"}),this.g.o=t,t=s&&s.initMessageHeaders||null,s&&s.messageContentType&&(t?t["X-WebChannel-Content-Type"]=s.messageContentType:t={"X-WebChannel-Content-Type":s.messageContentType}),s&&s.va&&(t?t["X-WebChannel-Client-Profile"]=s.va:t={"X-WebChannel-Client-Profile":s.va}),this.g.S=t,(t=s&&s.Sb)&&!Y(t)&&(this.g.m=t),this.v=s&&s.supportsCrossDomainXhr||!1,this.u=s&&s.sendRawJson||!1,(s=s&&s.httpSessionIdParam)&&!Y(s)&&(this.g.D=s,t=this.h,t!==null&&s in t&&(t=this.h,s in t&&delete t[s])),this.j=new $e(this)}P(Q,V),Q.prototype.m=function(){this.g.l=this.j,this.v&&(this.g.J=!0),this.g.connect(this.l,this.h||void 0)},Q.prototype.close=function(){xn(this.g)},Q.prototype.o=function(t){var s=this.g;if(typeof t=="string"){var a={};a.__data__=t,t=a}else this.u&&(a={},a.__data__=Tn(t),t=a);s.i.push(new To(s.Ya++,t)),s.G==3&&zt(s)},Q.prototype.N=function(){this.g.l=null,delete this.j,xn(this.g),delete this.g,Q.aa.N.call(this)};function mr(t){Sn.call(this),t.__headers__&&(this.headers=t.__headers__,this.statusCode=t.__status__,delete t.__headers__,delete t.__status__);var s=t.__sm__;if(s){e:{for(const a in s){t=a;break e}t=void 0}(this.i=t)&&(t=this.i,s=s!==null&&t in s?s[t]:void 0),this.data=s}else this.data=t}P(mr,Sn);function vr(){bn.call(this),this.status=1}P(vr,bn);function $e(t){this.g=t}P($e,gr),$e.prototype.ua=function(){W(this.g,"a")},$e.prototype.ta=function(t){W(this.g,new mr(t))},$e.prototype.sa=function(t){W(this.g,new vr)},$e.prototype.ra=function(){W(this.g,"b")},Q.prototype.send=Q.prototype.o,Q.prototype.open=Q.prototype.m,Q.prototype.close=Q.prototype.close,Pn.NO_ERROR=0,Pn.TIMEOUT=8,Pn.HTTP_ERROR=6,Io.COMPLETE="complete",mo.EventType=st,st.OPEN="a",st.CLOSE="b",st.ERROR="c",st.MESSAGE="d",V.prototype.listen=V.prototype.K,M.prototype.listenOnce=M.prototype.L,M.prototype.getLastError=M.prototype.Ka,M.prototype.getLastErrorCode=M.prototype.Ba,M.prototype.getStatus=M.prototype.Z,M.prototype.getResponseJson=M.prototype.Oa,M.prototype.getResponseText=M.prototype.oa,M.prototype.send=M.prototype.ea,M.prototype.setWithCredentials=M.prototype.Ha}).apply(typeof Jt<"u"?Jt:typeof self<"u"?self:typeof window<"u"?window:{});const Jr="@firebase/firestore";/**
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
 */class G{constructor(e){this.uid=e}isAuthenticated(){return this.uid!=null}toKey(){return this.isAuthenticated()?"uid:"+this.uid:"anonymous-user"}isEqual(e){return e.uid===this.uid}}G.UNAUTHENTICATED=new G(null),G.GOOGLE_CREDENTIALS=new G("google-credentials-uid"),G.FIRST_PARTY=new G("first-party-uid"),G.MOCK_USER=new G("mock-user");/**
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
 */let Ot="10.14.0";/**
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
 */const Xe=new si("@firebase/firestore");function te(i,...e){if(Xe.logLevel<=O.DEBUG){const n=e.map(mi);Xe.debug(`Firestore (${Ot}): ${i}`,...n)}}function Ys(i,...e){if(Xe.logLevel<=O.ERROR){const n=e.map(mi);Xe.error(`Firestore (${Ot}): ${i}`,...n)}}function Cl(i,...e){if(Xe.logLevel<=O.WARN){const n=e.map(mi);Xe.warn(`Firestore (${Ot}): ${i}`,...n)}}function mi(i){if(typeof i=="string")return i;try{/**
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
*/return function(n){return JSON.stringify(n)}(i)}catch{return i}}/**
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
 */function vi(i="Unexpected state"){const e=`FIRESTORE (${Ot}) INTERNAL ASSERTION FAILED: `+i;throw Ys(e),new Error(e)}function wt(i,e){i||vi()}/**
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
 */const J={CANCELLED:"cancelled",INVALID_ARGUMENT:"invalid-argument",FAILED_PRECONDITION:"failed-precondition"};class X extends ie{constructor(e,n){super(e,n),this.code=e,this.message=n,this.toString=()=>`${this.name}: [code=${this.code}]: ${this.message}`}}/**
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
 */class It{constructor(){this.promise=new Promise((e,n)=>{this.resolve=e,this.reject=n})}}/**
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
 */class Qs{constructor(e,n){this.user=n,this.type="OAuth",this.headers=new Map,this.headers.set("Authorization",`Bearer ${e}`)}}class Ol{getToken(){return Promise.resolve(null)}invalidateToken(){}start(e,n){e.enqueueRetryable(()=>n(G.UNAUTHENTICATED))}shutdown(){}}class Dl{constructor(e){this.token=e,this.changeListener=null}getToken(){return Promise.resolve(this.token)}invalidateToken(){}start(e,n){this.changeListener=n,e.enqueueRetryable(()=>n(this.token.user))}shutdown(){this.changeListener=null}}class Nl{constructor(e){this.t=e,this.currentUser=G.UNAUTHENTICATED,this.i=0,this.forceRefresh=!1,this.auth=null}start(e,n){wt(this.o===void 0);let r=this.i;const o=y=>this.i!==r?(r=this.i,n(y)):Promise.resolve();let c=new It;this.o=()=>{this.i++,this.currentUser=this.u(),c.resolve(),c=new It,e.enqueueRetryable(()=>o(this.currentUser))};const l=()=>{const y=c;e.enqueueRetryable(async()=>{await y.promise,await o(this.currentUser)})},p=y=>{te("FirebaseAuthCredentialsProvider","Auth detected"),this.auth=y,this.o&&(this.auth.addAuthTokenListener(this.o),l())};this.t.onInit(y=>p(y)),setTimeout(()=>{if(!this.auth){const y=this.t.getImmediate({optional:!0});y?p(y):(te("FirebaseAuthCredentialsProvider","Auth not yet detected"),c.resolve(),c=new It)}},0),l()}getToken(){const e=this.i,n=this.forceRefresh;return this.forceRefresh=!1,this.auth?this.auth.getToken(n).then(r=>this.i!==e?(te("FirebaseAuthCredentialsProvider","getToken aborted due to token change."),this.getToken()):r?(wt(typeof r.accessToken=="string"),new Qs(r.accessToken,this.currentUser)):null):Promise.resolve(null)}invalidateToken(){this.forceRefresh=!0}shutdown(){this.auth&&this.o&&this.auth.removeAuthTokenListener(this.o),this.o=void 0}u(){const e=this.auth&&this.auth.getUid();return wt(e===null||typeof e=="string"),new G(e)}}class Ll{constructor(e,n,r){this.l=e,this.h=n,this.P=r,this.type="FirstParty",this.user=G.FIRST_PARTY,this.I=new Map}T(){return this.P?this.P():null}get headers(){this.I.set("X-Goog-AuthUser",this.l);const e=this.T();return e&&this.I.set("Authorization",e),this.h&&this.I.set("X-Goog-Iam-Authorization-Token",this.h),this.I}}class Ml{constructor(e,n,r){this.l=e,this.h=n,this.P=r}getToken(){return Promise.resolve(new Ll(this.l,this.h,this.P))}start(e,n){e.enqueueRetryable(()=>n(G.FIRST_PARTY))}shutdown(){}invalidateToken(){}}class Ul{constructor(e){this.value=e,this.type="AppCheck",this.headers=new Map,e&&e.length>0&&this.headers.set("x-firebase-appcheck",this.value)}}class xl{constructor(e){this.A=e,this.forceRefresh=!1,this.appCheck=null,this.R=null}start(e,n){wt(this.o===void 0);const r=c=>{c.error!=null&&te("FirebaseAppCheckTokenProvider",`Error getting App Check token; using placeholder token instead. Error: ${c.error.message}`);const l=c.token!==this.R;return this.R=c.token,te("FirebaseAppCheckTokenProvider",`Received ${l?"new":"existing"} token.`),l?n(c.token):Promise.resolve()};this.o=c=>{e.enqueueRetryable(()=>r(c))};const o=c=>{te("FirebaseAppCheckTokenProvider","AppCheck detected"),this.appCheck=c,this.o&&this.appCheck.addTokenListener(this.o)};this.A.onInit(c=>o(c)),setTimeout(()=>{if(!this.appCheck){const c=this.A.getImmediate({optional:!0});c?o(c):te("FirebaseAppCheckTokenProvider","AppCheck not yet detected")}},0)}getToken(){const e=this.forceRefresh;return this.forceRefresh=!1,this.appCheck?this.appCheck.getToken(e).then(n=>n?(wt(typeof n.token=="string"),this.R=n.token,new Ul(n.token)):null):Promise.resolve(null)}invalidateToken(){this.forceRefresh=!0}shutdown(){this.appCheck&&this.o&&this.appCheck.removeTokenListener(this.o),this.o=void 0}}function Fl(i){return i.name==="IndexedDbTransactionError"}class cn{constructor(e,n){this.projectId=e,this.database=n||"(default)"}static empty(){return new cn("","")}get isDefaultDatabase(){return this.database==="(default)"}isEqual(e){return e instanceof cn&&e.projectId===this.projectId&&e.database===this.database}}/**
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
 */var Xr,k;(k=Xr||(Xr={}))[k.OK=0]="OK",k[k.CANCELLED=1]="CANCELLED",k[k.UNKNOWN=2]="UNKNOWN",k[k.INVALID_ARGUMENT=3]="INVALID_ARGUMENT",k[k.DEADLINE_EXCEEDED=4]="DEADLINE_EXCEEDED",k[k.NOT_FOUND=5]="NOT_FOUND",k[k.ALREADY_EXISTS=6]="ALREADY_EXISTS",k[k.PERMISSION_DENIED=7]="PERMISSION_DENIED",k[k.UNAUTHENTICATED=16]="UNAUTHENTICATED",k[k.RESOURCE_EXHAUSTED=8]="RESOURCE_EXHAUSTED",k[k.FAILED_PRECONDITION=9]="FAILED_PRECONDITION",k[k.ABORTED=10]="ABORTED",k[k.OUT_OF_RANGE=11]="OUT_OF_RANGE",k[k.UNIMPLEMENTED=12]="UNIMPLEMENTED",k[k.INTERNAL=13]="INTERNAL",k[k.UNAVAILABLE=14]="UNAVAILABLE",k[k.DATA_LOSS=15]="DATA_LOSS";/**
 * @license
 * Copyright 2022 Google LLC
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
 */new Xs([4294967295,4294967295],0);function qn(){return typeof document<"u"?document:null}/**
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
 */class jl{constructor(e,n,r=1e3,o=1.5,c=6e4){this.ui=e,this.timerId=n,this.ko=r,this.qo=o,this.Qo=c,this.Ko=0,this.$o=null,this.Uo=Date.now(),this.reset()}reset(){this.Ko=0}Wo(){this.Ko=this.Qo}Go(e){this.cancel();const n=Math.floor(this.Ko+this.zo()),r=Math.max(0,Date.now()-this.Uo),o=Math.max(0,n-r);o>0&&te("ExponentialBackoff",`Backing off for ${o} ms (base delay: ${this.Ko} ms, delay with jitter: ${n} ms, last attempt: ${r} ms ago)`),this.$o=this.ui.enqueueAfterDelay(this.timerId,o,()=>(this.Uo=Date.now(),e())),this.Ko*=this.qo,this.Ko<this.ko&&(this.Ko=this.ko),this.Ko>this.Qo&&(this.Ko=this.Qo)}jo(){this.$o!==null&&(this.$o.skipDelay(),this.$o=null)}cancel(){this.$o!==null&&(this.$o.cancel(),this.$o=null)}zo(){return(Math.random()-.5)*this.Ko}}/**
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
 */class _i{constructor(e,n,r,o,c){this.asyncQueue=e,this.timerId=n,this.targetTimeMs=r,this.op=o,this.removalCallback=c,this.deferred=new It,this.then=this.deferred.promise.then.bind(this.deferred.promise),this.deferred.promise.catch(l=>{})}get promise(){return this.deferred.promise}static createAndSchedule(e,n,r,o,c){const l=Date.now()+r,p=new _i(e,n,l,o,c);return p.start(r),p}start(e){this.timerHandle=setTimeout(()=>this.handleDelayElapsed(),e)}skipDelay(){return this.handleDelayElapsed()}cancel(e){this.timerHandle!==null&&(this.clearTimeout(),this.deferred.reject(new X(J.CANCELLED,"Operation cancelled"+(e?": "+e:""))))}handleDelayElapsed(){this.asyncQueue.enqueueAndForget(()=>this.timerHandle!==null?(this.clearTimeout(),this.op().then(e=>this.deferred.resolve(e))):Promise.resolve())}clearTimeout(){this.timerHandle!==null&&(this.removalCallback(this),clearTimeout(this.timerHandle),this.timerHandle=null)}}var Yr,Qr;(Qr=Yr||(Yr={})).ea="default",Qr.Cache="cache";/**
 * @license
 * Copyright 2023 Google LLC
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
 */function Bl(i){const e={};return i.timeoutSeconds!==void 0&&(e.timeoutSeconds=i.timeoutSeconds),e}/**
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
 */const Zr=new Map;function Vl(i,e,n,r){if(e===!0&&r===!0)throw new X(J.INVALID_ARGUMENT,`${i} and ${n} cannot be used together.`)}function Hl(i){if(i===void 0)return"undefined";if(i===null)return"null";if(typeof i=="string")return i.length>20&&(i=`${i.substring(0,20)}...`),JSON.stringify(i);if(typeof i=="number"||typeof i=="boolean")return""+i;if(typeof i=="object"){if(i instanceof Array)return"an array";{const e=function(r){return r.constructor?r.constructor.name:null}(i);return e?`a custom ${e} object`:"an object"}}return typeof i=="function"?"a function":vi()}function $l(i,e){if("_delegate"in i&&(i=i._delegate),!(i instanceof e)){if(e.name===i.constructor.name)throw new X(J.INVALID_ARGUMENT,"Type does not match the expected instance. Did you pass a reference from a different Firestore SDK?");{const n=Hl(i);throw new X(J.INVALID_ARGUMENT,`Expected type '${e.name}', but it was: ${n}`)}}return i}/**
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
 */class es{constructor(e){var n,r;if(e.host===void 0){if(e.ssl!==void 0)throw new X(J.INVALID_ARGUMENT,"Can't provide ssl option if host option is not set");this.host="firestore.googleapis.com",this.ssl=!0}else this.host=e.host,this.ssl=(n=e.ssl)===null||n===void 0||n;if(this.credentials=e.credentials,this.ignoreUndefinedProperties=!!e.ignoreUndefinedProperties,this.localCache=e.localCache,e.cacheSizeBytes===void 0)this.cacheSizeBytes=41943040;else{if(e.cacheSizeBytes!==-1&&e.cacheSizeBytes<1048576)throw new X(J.INVALID_ARGUMENT,"cacheSizeBytes must be at least 1048576");this.cacheSizeBytes=e.cacheSizeBytes}Vl("experimentalForceLongPolling",e.experimentalForceLongPolling,"experimentalAutoDetectLongPolling",e.experimentalAutoDetectLongPolling),this.experimentalForceLongPolling=!!e.experimentalForceLongPolling,this.experimentalForceLongPolling?this.experimentalAutoDetectLongPolling=!1:e.experimentalAutoDetectLongPolling===void 0?this.experimentalAutoDetectLongPolling=!0:this.experimentalAutoDetectLongPolling=!!e.experimentalAutoDetectLongPolling,this.experimentalLongPollingOptions=Bl((r=e.experimentalLongPollingOptions)!==null&&r!==void 0?r:{}),function(c){if(c.timeoutSeconds!==void 0){if(isNaN(c.timeoutSeconds))throw new X(J.INVALID_ARGUMENT,`invalid long polling timeout: ${c.timeoutSeconds} (must not be NaN)`);if(c.timeoutSeconds<5)throw new X(J.INVALID_ARGUMENT,`invalid long polling timeout: ${c.timeoutSeconds} (minimum allowed value is 5)`);if(c.timeoutSeconds>30)throw new X(J.INVALID_ARGUMENT,`invalid long polling timeout: ${c.timeoutSeconds} (maximum allowed value is 30)`)}}(this.experimentalLongPollingOptions),this.useFetchStreams=!!e.useFetchStreams}isEqual(e){return this.host===e.host&&this.ssl===e.ssl&&this.credentials===e.credentials&&this.cacheSizeBytes===e.cacheSizeBytes&&this.experimentalForceLongPolling===e.experimentalForceLongPolling&&this.experimentalAutoDetectLongPolling===e.experimentalAutoDetectLongPolling&&function(r,o){return r.timeoutSeconds===o.timeoutSeconds}(this.experimentalLongPollingOptions,e.experimentalLongPollingOptions)&&this.ignoreUndefinedProperties===e.ignoreUndefinedProperties&&this.useFetchStreams===e.useFetchStreams}}class Zs{constructor(e,n,r,o){this._authCredentials=e,this._appCheckCredentials=n,this._databaseId=r,this._app=o,this.type="firestore-lite",this._persistenceKey="(lite)",this._settings=new es({}),this._settingsFrozen=!1,this._terminateTask="notTerminated"}get app(){if(!this._app)throw new X(J.FAILED_PRECONDITION,"Firestore was not initialized using the Firebase SDK. 'app' is not available");return this._app}get _initialized(){return this._settingsFrozen}get _terminated(){return this._terminateTask!=="notTerminated"}_setSettings(e){if(this._settingsFrozen)throw new X(J.FAILED_PRECONDITION,"Firestore has already been started and its settings can no longer be changed. You can only modify settings before calling any other methods on a Firestore object.");this._settings=new es(e),e.credentials!==void 0&&(this._authCredentials=function(r){if(!r)return new Ol;switch(r.type){case"firstParty":return new Ml(r.sessionIndex||"0",r.iamToken||null,r.authTokenFactory||null);case"provider":return r.client;default:throw new X(J.INVALID_ARGUMENT,"makeAuthCredentialsProvider failed due to invalid credential type")}}(e.credentials))}_getSettings(){return this._settings}_freezeSettings(){return this._settingsFrozen=!0,this._settings}_delete(){return this._terminateTask==="notTerminated"&&(this._terminateTask=this._terminate()),this._terminateTask}async _restart(){this._terminateTask==="notTerminated"?await this._terminate():this._terminateTask="notTerminated"}toJSON(){return{app:this._app,databaseId:this._databaseId,settings:this._settings}}_terminate(){return function(n){const r=Zr.get(n);r&&(te("ComponentProvider","Removing Datastore"),Zr.delete(n),r.terminate())}(this),Promise.resolve()}}function Wl(i,e,n,r={}){var o;const c=(i=$l(i,Zs))._getSettings(),l=`${e}:${n}`;if(c.host!=="firestore.googleapis.com"&&c.host!==l&&Cl("Host has been set in both settings() and connectFirestoreEmulator(), emulator host will be used."),i._setSettings(Object.assign(Object.assign({},c),{host:l,ssl:!1})),r.mockUserToken){let p,y;if(typeof r.mockUserToken=="string")p=r.mockUserToken,y=G.MOCK_USER;else{p=Jo(r.mockUserToken,(o=i._app)===null||o===void 0?void 0:o.options.projectId);const E=r.mockUserToken.sub||r.mockUserToken.user_id;if(!E)throw new X(J.INVALID_ARGUMENT,"mockUserToken must contain 'sub' or 'user_id' field!");y=new G(E)}i._authCredentials=new Dl(new Qs(p,y))}}/**
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
 */class ts{constructor(e=Promise.resolve()){this.Pu=[],this.Iu=!1,this.Tu=[],this.Eu=null,this.du=!1,this.Au=!1,this.Ru=[],this.t_=new jl(this,"async_queue_retry"),this.Vu=()=>{const r=qn();r&&te("AsyncQueue","Visibility state changed to "+r.visibilityState),this.t_.jo()},this.mu=e;const n=qn();n&&typeof n.addEventListener=="function"&&n.addEventListener("visibilitychange",this.Vu)}get isShuttingDown(){return this.Iu}enqueueAndForget(e){this.enqueue(e)}enqueueAndForgetEvenWhileRestricted(e){this.fu(),this.gu(e)}enterRestrictedMode(e){if(!this.Iu){this.Iu=!0,this.Au=e||!1;const n=qn();n&&typeof n.removeEventListener=="function"&&n.removeEventListener("visibilitychange",this.Vu)}}enqueue(e){if(this.fu(),this.Iu)return new Promise(()=>{});const n=new It;return this.gu(()=>this.Iu&&this.Au?Promise.resolve():(e().then(n.resolve,n.reject),n.promise)).then(()=>n.promise)}enqueueRetryable(e){this.enqueueAndForget(()=>(this.Pu.push(e),this.pu()))}async pu(){if(this.Pu.length!==0){try{await this.Pu[0](),this.Pu.shift(),this.t_.reset()}catch(e){if(!Fl(e))throw e;te("AsyncQueue","Operation failed with retryable error: "+e)}this.Pu.length>0&&this.t_.Go(()=>this.pu())}}gu(e){const n=this.mu.then(()=>(this.du=!0,e().catch(r=>{this.Eu=r,this.du=!1;const o=function(l){let p=l.message||"";return l.stack&&(p=l.stack.includes(l.message)?l.stack:l.message+`
`+l.stack),p}(r);throw Ys("INTERNAL UNHANDLED ERROR: ",o),r}).then(r=>(this.du=!1,r))));return this.mu=n,n}enqueueAfterDelay(e,n,r){this.fu(),this.Ru.indexOf(e)>-1&&(n=0);const o=_i.createAndSchedule(this,e,n,r,c=>this.yu(c));return this.Tu.push(o),o}fu(){this.Eu&&vi()}verifyOperationInProgress(){}async wu(){let e;do e=this.mu,await e;while(e!==this.mu)}Su(e){for(const n of this.Tu)if(n.timerId===e)return!0;return!1}bu(e){return this.wu().then(()=>{this.Tu.sort((n,r)=>n.targetTimeMs-r.targetTimeMs);for(const n of this.Tu)if(n.skipDelay(),e!=="all"&&n.timerId===e)break;return this.wu()})}Du(e){this.Ru.push(e)}yu(e){const n=this.Tu.indexOf(e);this.Tu.splice(n,1)}}class zl extends Zs{constructor(e,n,r,o){super(e,n,r,o),this.type="firestore",this._queue=new ts,this._persistenceKey=(o==null?void 0:o.name)||"[DEFAULT]"}async _terminate(){if(this._firestoreClient){const e=this._firestoreClient.terminate();this._queue=new ts(e),this._firestoreClient=void 0,await e}}}function Gl(i,e){const n=typeof i=="object"?i:ln(),r=typeof i=="string"?i:"(default)",o=hn(n,"firestore").getImmediate({identifier:r});if(!o._initialized){const c=cs("firestore");c&&Wl(o,...c)}return o}(function(e,n=!0){(function(o){Ot=o})(Ye),xe(new Ce("firestore",(r,{instanceIdentifier:o,options:c})=>{const l=r.getProvider("app").getImmediate(),p=new zl(new Nl(r.getProvider("auth-internal")),new xl(r.getProvider("app-check-internal")),function(E,S){if(!Object.prototype.hasOwnProperty.apply(E.options,["projectId"]))throw new X(J.INVALID_ARGUMENT,'"projectId" not provided in firebase.initializeApp.');return new cn(E.options.projectId,S)}(l,o),l);return c=Object.assign({useFetchStreams:n},c),p._setSettings(c),p},"PUBLIC").setMultipleInstances(!0)),re(Jr,"4.7.3",e),re(Jr,"4.7.3","esm2017")})();/**
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
 */const eo="functions";/**
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
 */class Kl{constructor(e,n,r){this.auth=null,this.messaging=null,this.appCheck=null,this.auth=e.getImmediate({optional:!0}),this.messaging=n.getImmediate({optional:!0}),this.auth||e.get().then(o=>this.auth=o,()=>{}),this.messaging||n.get().then(o=>this.messaging=o,()=>{}),this.appCheck||r.get().then(o=>this.appCheck=o,()=>{})}async getAuthToken(){if(this.auth)try{const e=await this.auth.getToken();return e==null?void 0:e.accessToken}catch{return}}async getMessagingToken(){if(!(!this.messaging||!("Notification"in self)||Notification.permission!=="granted"))try{return await this.messaging.getToken()}catch{return}}async getAppCheckToken(e){if(this.appCheck){const n=e?await this.appCheck.getLimitedUseToken():await this.appCheck.getToken();return n.error?null:n.token}return null}async getContext(e){const n=await this.getAuthToken(),r=await this.getMessagingToken(),o=await this.getAppCheckToken(e);return{authToken:n,messagingToken:r,appCheckToken:o}}}/**
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
 */const ii="us-central1";class ql{constructor(e,n,r,o,c=ii,l){this.app=e,this.fetchImpl=l,this.emulatorOrigin=null,this.contextProvider=new Kl(n,r,o),this.cancelAllRequests=new Promise(p=>{this.deleteService=()=>Promise.resolve(p())});try{const p=new URL(c);this.customDomain=p.origin+(p.pathname==="/"?"":p.pathname),this.region=ii}catch{this.customDomain=null,this.region=c}}_delete(){return this.deleteService()}_url(e){const n=this.app.options.projectId;return this.emulatorOrigin!==null?`${this.emulatorOrigin}/${n}/${this.region}/${e}`:this.customDomain!==null?`${this.customDomain}/${e}`:`https://${this.region}-${n}.cloudfunctions.net/${e}`}}function Jl(i,e,n){i.emulatorOrigin=`http://${e}:${n}`}const ns="@firebase/functions",is="0.11.8";/**
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
 */const Xl="auth-internal",Yl="app-check-internal",Ql="messaging-internal";function Zl(i,e){const n=(r,{instanceIdentifier:o})=>{const c=r.getProvider("app").getImmediate(),l=r.getProvider(Xl),p=r.getProvider(Ql),y=r.getProvider(Yl);return new ql(c,l,p,y,o,i)};xe(new Ce(eo,n,"PUBLIC").setMultipleInstances(!0)),re(ns,is,e),re(ns,is,"esm2017")}/**
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
 */function eu(i=ln(),e=ii){const r=hn(ae(i),eo).getImmediate({identifier:e}),o=cs("functions");return o&&tu(r,...o),r}function tu(i,e,n){Jl(ae(i),e,n)}Zl(fetch.bind(self));const nu={apiKey:"AIzaSyCib1UPogwLoAshIWm9YQJB_RR0UxC07i8",authDomain:"supremeai-a.firebaseapp.com",databaseURL:"https://supremeai-a-default-rtdb.asia-southeast1.firebasedatabase.app/",projectId:"supremeai-a",storageBucket:"supremeai-a.firebasestorage.app",messagingSenderId:"565236080752",appId:"1:565236080752:web:572bb9313db9afb355d4b5"},yi=cc().length?ln():fs(nu),to=Pl(yi),iu=Gl(yi),ru=eu(yi);window.addEventListener("unhandledrejection",i=>{const e=i.reason,n=e instanceof Error?e.message:String(e);console.error("[GlobalErrorHandler] Unhandled async error:",n)});window.addEventListener("error",i=>{const e=i.message??"Unknown runtime error",n=i.filename??"unknown",r=i.lineno??0;console.error(`[GlobalErrorHandler] Runtime error at ${n}:${r}:`,e)});function no(i){return i?{"auth/invalid-email":"The email address is not valid. Please check and try again.","auth/user-disabled":"This account has been disabled. Please contact support.","auth/user-not-found":"No account found with this email address.","auth/wrong-password":"The password is incorrect. Please try again.","auth/too-many-requests":"Too many failed attempts. Please wait a moment before trying again.","auth/network-request-failed":"Unable to connect. Please check your internet connection.","auth/invalid-credential":"The credentials are invalid. Please log in again.","auth/expired-action-code":"This link has expired. Please request a new one.","auth/invalid-action-code":"This link is invalid or has already been used.","auth/email-already-in-use":"An account with this email already exists.","auth/weak-password":"The password is too weak. Please use at least 6 characters.","auth/requires-recent-login":"Please log out and log in again to perform this action."}[i]??"An authentication error occurred. Please try again.":"An unexpected authentication error occurred."}async function su(i,e){try{const n=await ph(to,i,e),o=(await Ts(n.user)).token,c=await fetch("/api/auth/firebase-login",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({idToken:o})});if(!c.ok){const y=await c.json().catch(()=>({}));throw new Error(y.message??"Firebase token exchange failed")}const l=await c.json();if(!l.success||!l.data)throw new Error(l.error??"Authentication failed");const p=l.data;return localStorage.setItem("supremeai_token",p.token),localStorage.setItem("supremeai_refresh_token",p.refreshToken),p}catch(n){throw n instanceof ie?new Error(no(n.code)):n}}async function ou(){const i=localStorage.getItem("supremeai_refresh_token");if(!i)throw new Error("No refresh token available");const e=await fetch("/api/auth/refresh",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({refreshToken:i})});if(!e.ok){const o=await e.json().catch(()=>({}));throw new Error(o.message??"Token refresh failed")}const n=await e.json();if(!n.success||!n.data)throw new Error(n.error??"Token refresh failed");const r=n.data;return localStorage.setItem("supremeai_token",r.token),r.token}async function au(){try{await vh(to)}catch(i){i instanceof ie&&console.warn("Firebase sign-out warning:",no(i.code))}}export{to as firebaseAuth,su as firebaseSignIn,au as firebaseSignOutFn,iu as firestore,ru as functions,ou as refreshAccessToken};
