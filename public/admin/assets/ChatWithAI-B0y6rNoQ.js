import{j as e}from"./vendor-charts-CgurTGG5.js";import{r as o}from"./vendor-i18n-f43QMM9M.js";import{a as m}from"./index-BvbR3CK1.js";import{s as l,B as ue,ab as be,o as R,ac as fe,ad as me,c as he,ae as ye,M as H,af as we,ag as G,ah as ve,u as Q,I as q,ai as je,aj as ke,ak as Se,_ as X,p as Ne}from"./vendor-antd-cihozudQ.js";import"./vendor-three-D9kAsY74.js";import"./vendor-react-IK9anZxV.js";const We=({chatFont:Ie="font-mono"})=>{const[b,g]=o.useState([]),[c,S]=o.useState(null),[Z,C]=o.useState(!1),[P,ee]=o.useState(null),[A,O]=o.useState(""),[z,T]=o.useState(""),[w,U]=o.useState(!1),[D,te]=o.useState("all"),[ae,F]=o.useState([]),[N,_]=o.useState({rules:[],plans:[],actions:[]}),Y=o.useRef(null),[v,j]=o.useState(!1),[I,ne]=o.useState(null),[h,W]=o.useState(null),[J,$]=o.useState("");o.useEffect(()=>{const t=window.SpeechRecognition||window.webkitSpeechRecognition;if(t){const a=new t;a.continuous=!0,a.interimResults=!0,a.lang="bn-BD",a.onresult=n=>{let r="",x="";for(let s=n.resultIndex;s<n.results.length;++s)n.results[s].isFinal?x+=n.results[s][0].transcript:r+=n.results[s][0].transcript;const d=x||r;d.trim()&&T(s=>s.endsWith(" ")||s===""?s+d:s+" "+d)},a.onerror=n=>{console.error("Speech recognition error:",n.error),n.error==="not-allowed"?l.error("মাইক্রোফোন অ্যাক্সেসের অনুমতি নেই।"):l.error("ভয়েস সনাক্তকরণে সমস্যা হয়েছে: "+n.error),j(!1)},a.onend=()=>{j(!1)},ne(a)}},[]);const re=()=>{if(!I){l.warning("আপনার ব্রাউজার ভয়েস সনাক্তকরণ সমর্থন করে না। Google Chrome ব্যবহার করার চেষ্টা করুন।");return}if(v)I.stop(),j(!1),l.info("ভয়েস ইনপুট বন্ধ করা হয়েছে।");else try{I.start(),j(!0),l.success("ভয়েস ইনপুট সক্রিয় হয়েছে... কথা বলুন।")}catch(t){console.error(t),I.stop(),j(!1)}},se=t=>{var r;const a=(r=t.target.files)==null?void 0:r[0];if(!a)return;if(a.size>4*1024*1024){l.error("ফাইলের সাইজ ৪ মেগাবাইটের কম হতে হবে।");return}const n=new FileReader;n.onload=()=>{typeof n.result=="string"&&(W(n.result),$(a.name),l.success("ছবি সংযুক্ত করা হয়েছে।"))},n.onerror=()=>{l.error("ছবি প্রসেস করতে ত্রুটি হয়েছে।")},n.readAsDataURL(a)},oe=()=>{W(null),$(""),l.info("সংযুক্ত ছবি মুছে ফেলা হয়েছে।")};o.useEffect(()=>{const t=localStorage.getItem("supremeai_chat_sessions");if(t)try{const a=JSON.parse(t);g(a),a.length>0?S(a[0].id):k()}catch{console.error("Failed to parse saved sessions"),k()}else k();de(),pe()},[]),o.useEffect(()=>{b.length>=0&&localStorage.setItem("supremeai_chat_sessions",JSON.stringify(b))},[b]);const y=b.find(t=>t.id===c),M=(y==null?void 0:y.messages)||[],k=()=>{const t={id:Date.now().toString(),name:"নতুন চ্যাট সেশন",messages:[],createdAt:new Date().toISOString()};g(a=>[t,...a]),S(t.id)},ie=(t,a)=>{a.stopPropagation();const n=b.filter(r=>r.id!==t);g(n),c===t&&(S(n.length>0?n[0].id:null),n.length===0&&setTimeout(()=>{n.length===0&&k()},0)),l.success("চ্যাট সেশন মুছে ফেলা হয়েছে")},le=(t,a)=>{a.stopPropagation(),ee(t),O(t.name),C(!0)},V=()=>{P&&A.trim()&&(g(t=>t.map(a=>a.id===P.id?{...a,name:A.trim()}:a)),C(!1),l.success("চ্যাটের নাম পরিবর্তন করা হয়েছে"))},ce=()=>{var t;(t=Y.current)==null||t.scrollIntoView({behavior:"smooth"})};o.useEffect(()=>{ce()},[M]);const de=async()=>{var t;try{if(!m.isAuthenticated()){F([]);return}const n=await m.fetchWithAuth("/api/admin/providers/configured");if(n.ok){const r=await n.json(),d=(((t=r.data)==null?void 0:t.providers)||(Array.isArray(r.data)?r.data:[])).map(s=>({id:s.id,name:s.name,status:s.status||"online",type:s.type||"llm"}));F(d)}}catch{console.error("Failed to fetch agents")}},pe=async()=>{try{if(!m.isAuthenticated()){_({rules:[],plans:[],actions:[]});return}const[a,n,r]=await Promise.all([m.fetchWithAuth("/api/admin/rules").catch(()=>null),m.fetchWithAuth("/api/admin/plans").catch(()=>null),m.fetchWithAuth("/api/admin/chat/actions/pending").catch(()=>null)]),x=a!=null&&a.ok?await a.json():[],d=n!=null&&n.ok?await n.json():[],s=r!=null&&r.ok?await r.json():[];_({rules:Array.isArray(x)?x.slice(0,5):[],plans:Array.isArray(d)?d.slice(0,5):[],actions:Array.isArray(s)?s.slice(0,5):[]})}catch{console.error("Failed to fetch knowledge context")}},K=async t=>{if(!t.trim()&&!h||w||!c)return;const a={id:Date.now().toString(),sender:"user",agent:"You",content:t,image:h||void 0,timestamp:new Date().toLocaleTimeString([],{hour:"2-digit",minute:"2-digit"}),status:"completed"};let n=[...b];const r=n.findIndex(s=>s.id===c);if(r!==-1){if(n[r].messages.push(a),n[r].messages.length===1||n[r].name==="New Chat"||n[r].name==="নতুন চ্যাট সেশন"){const s=t.trim()?t.split(" "):["সংযুক্ত ছবি"];n[r].name=s.slice(0,4).join(" ")+(s.length>4?"...":"")}g(n)}const x=h,d=J;T(""),W(null),$(""),U(!0);try{const s=b.find(i=>i.id===c),B=s?s.messages:[],E=x?`${t}

[সংযুক্ত ছবি: ${d}]
![${d}](${x})`:t,f=await m.fetchWithAuth("/api/chat/send",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({message:E,agentId:D==="all"?null:D,sessionId:c,messages:B})});if(f.ok){const i=await f.json(),L={id:(Date.now()+1).toString(),sender:"ai",agent:i.agent_name||"Neural Core",content:i.message||"Processing optimized.",timestamp:new Date().toLocaleTimeString([],{hour:"2-digit",minute:"2-digit"}),confidence:i.confidence?Math.round(i.confidence*100):void 0,intent:i.intent||"NORMAL",status:"completed",type:i.type,options:i.options||[]};g(p=>p.map(u=>u.id===c?{...u,messages:[...u.messages,L]}:u))}else{let i="সিস্টেম প্রক্রিয়াকরণে সমস্যা হয়েছে। অনুগ্রহ করে আবার চেষ্টা করুন বা ব্যাকএন্ড লগ পরীক্ষা করুন।";try{const p=await f.json();p&&p.message?i=p.message:p&&p.error&&(i=p.error)}catch{}const L={id:(Date.now()+1).toString(),sender:"ai",agent:"সিস্টেম রেসপন্স",content:i,timestamp:new Date().toLocaleTimeString([],{hour:"2-digit",minute:"2-digit"}),status:"completed"};g(p=>p.map(u=>u.id===c?{...u,messages:[...u.messages,L]}:u))}}catch(s){const B={id:(Date.now()+1).toString(),sender:"ai",agent:"লোকাল নেটওয়ার্ক",content:`সার্ভারের সাথে সংযোগ স্থাপন করা যাচ্ছে না। ত্রুটি: ${s.message||"Unknown network error"}`,timestamp:new Date().toLocaleTimeString([],{hour:"2-digit",minute:"2-digit"}),status:"completed"};g(E=>E.map(f=>f.id===c?{...f,messages:[...f.messages,B]}:f))}finally{U(!1)}},ge=async t=>{t.preventDefault(),await K(z)},xe=async t=>{try{const a=await m.fetchWithAuth("/api/voicebox/speak",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({text:t,profile:"default"})});if(a.ok){const n=await a.json();if(n&&n.audio_url)new Audio(n.audio_url).play();else if(n&&n.audio)new Audio(`data:audio/wav;base64,${n.audio}`).play();else{const r=new SpeechSynthesisUtterance(t);r.lang="bn-BD",window.speechSynthesis.speak(r)}}else{const n=new SpeechSynthesisUtterance(t);n.lang="bn-BD",window.speechSynthesis.speak(n)}}catch{const n=new SpeechSynthesisUtterance(t);n.lang="bn-BD",window.speechSynthesis.speak(n)}};return e.jsxs("div",{className:"glass-panel text-white neural-chat-container",children:[e.jsxs("div",{className:"neural-chat-sidebar",children:[e.jsx("div",{className:"sidebar-header",children:e.jsx(ue,{type:"primary",icon:e.jsx(be,{}),block:!0,onClick:k,className:"new-session-glow-btn",style:{background:"linear-gradient(135deg, var(--neon-blue), var(--neon-purple))",border:"none",height:"46px",borderRadius:"12px",fontWeight:800,fontSize:"11px",letterSpacing:"1px",textTransform:"uppercase",color:"#ffffff",display:"flex",alignItems:"center",justifyContent:"center",gap:"8px",boxShadow:"0 0 15px rgba(0, 243, 255, 0.25)"},children:"নতুন নিউরাল চ্যাট"})}),e.jsx("div",{className:"sessions-list custom-scrollbar",children:b.map(t=>e.jsxs("div",{onClick:()=>S(t.id),className:`session-item ${c===t.id?"active":""}`,children:[e.jsxs("div",{className:"session-name-container",children:[e.jsx("div",{className:"session-dot"}),e.jsx("span",{className:"session-name-text",children:t.name})]}),e.jsxs("div",{className:"session-actions",children:[e.jsx(R,{title:"নাম পরিবর্তন",children:e.jsx(fe,{className:"action-icon",onClick:a=>le(t,a)})}),e.jsx(R,{title:"মুছে ফেলুন",children:e.jsx(me,{className:"action-icon delete",onClick:a=>ie(t.id,a)})})]})]},t.id))})]}),e.jsxs("div",{className:"neural-chat-content",children:[e.jsxs("div",{className:"chat-content-header",children:[e.jsxs("div",{className:"header-info-container",children:[e.jsx("div",{className:"header-icon-wrapper",children:e.jsx(he,{style:{color:"var(--neon-blue)",fontSize:"20px"}})}),e.jsxs("div",{children:[e.jsx("h3",{className:"header-title-text",children:(y==null?void 0:y.name)||"নিউরাল চ্যাট (Neural Chat)"}),e.jsx("span",{className:"header-subtitle-text",children:"SUPREMEAI NEURAL CORE SYSTEM"})]})]}),e.jsxs("div",{style:{display:"flex",alignItems:"center",gap:"16px"},children:[e.jsx("span",{style:{fontSize:"11px",color:"rgba(255, 255, 255, 0.4)",fontWeight:600,textTransform:"uppercase",letterSpacing:"1px"},children:"মডেল সিলেক্ট করুন:"}),e.jsxs("select",{value:D,onChange:t=>te(t.target.value),className:"custom-agent-select",children:[e.jsx("option",{value:"all",children:"Dynamic Routing (All Models)"}),ae.map(t=>e.jsx("option",{value:t.id,children:t.name},t.id))]})]})]}),e.jsx("div",{className:"chat-messages-area custom-scrollbar",children:M.length===0?e.jsxs("div",{style:{height:"60vh",display:"flex",flexDirection:"column",alignItems:"center",justifyContent:"center",color:"rgba(255,255,255,0.15)"},children:[e.jsxs("div",{style:{width:"80px",height:"80px",background:"rgba(0, 243, 255, 0.05)",borderRadius:"24px",display:"flex",alignItems:"center",justifyContent:"center",border:"1px solid rgba(0, 243, 255, 0.15)",boxShadow:"0 0 30px rgba(0, 243, 255, 0.05)",marginBottom:"20px",position:"relative"},children:[e.jsx(ye,{style:{fontSize:"32px",color:"var(--neon-blue)"}}),e.jsx("div",{style:{position:"absolute",inset:0,border:"1.5px solid var(--neon-blue)",borderRadius:"24px",opacity:.3,animation:"pulse 2s infinite ease-in-out"}})]}),e.jsx("span",{style:{fontSize:"10px",fontWeight:800,letterSpacing:"4px",textTransform:"uppercase",color:"var(--neon-blue)",opacity:.6},children:"নিউরাল সিঙ্ক সেশন চালু হয়েছে"})]}):e.jsxs("div",{className:"messages-max-width-wrapper",children:[M.map(t=>e.jsx("div",{className:`message-row ${t.sender==="user"?"user":"ai"}`,children:e.jsxs("div",{className:"message-bubble-wrapper",children:[e.jsx("div",{className:`message-meta-header ${t.sender==="user"?"user":"ai"}`,children:e.jsxs("span",{className:"meta-sender-name",children:[t.sender==="ai"?t.agent:"অনুমোদিত অপারেটর (Operator)"," • ",t.timestamp]})}),e.jsxs("div",{className:`message-bubble ${t.sender==="user"?"user":"ai"}`,children:[t.image&&e.jsx("div",{style:{marginBottom:"12px",borderRadius:"12px",overflow:"hidden",border:"1px solid rgba(255, 255, 255, 0.1)",maxWidth:"320px",cursor:"pointer"},children:e.jsx("img",{src:t.image,alt:"Attached file",style:{width:"100%",height:"auto",display:"block",transition:"transform 0.3s"},onClick:()=>{H.info({title:e.jsx("span",{style:{color:"white",fontWeight:700,textTransform:"uppercase",letterSpacing:"1px"},children:"সংযুক্ত ছবি প্রিভিউ"}),icon:null,width:800,centered:!0,content:e.jsx("div",{style:{display:"flex",justifyContent:"center",padding:"12px",background:"#08080f",borderRadius:"12px",border:"1px solid rgba(255, 255, 255, 0.1)",marginTop:"16px"},children:e.jsx("img",{src:t.image,alt:"Preview",style:{maxWidth:"100%",maxHeight:"70vh",borderRadius:"8px",objectFit:"contain"}})}),okText:"বন্ধ করুন",okButtonProps:{style:{background:"var(--neon-blue)",border:"none",color:"#000",fontWeight:"bold"}},styles:{body:{backgroundColor:"#05050a",color:"white"}},className:"dark-modal"})}})}),e.jsx("div",{style:{wordBreak:"break-word",whiteSpace:"pre-wrap"},children:t.content}),t.type==="CLARIFICATION_REQUIRED"&&t.options&&t.options.length>0&&e.jsxs("div",{style:{marginTop:"16px",display:"flex",flexDirection:"column",gap:"10px"},children:[e.jsx("span",{style:{fontSize:"12px",fontWeight:600,color:"rgba(255,255,255,0.6)"},children:"সম্ভাব্য অপশনগুলো থেকে বেছে নিন:"}),e.jsx("div",{style:{display:"flex",flexWrap:"wrap",gap:"8px"},children:t.options.map((a,n)=>e.jsx("button",{onClick:()=>K(a),style:{background:"rgba(0, 243, 255, 0.05)",border:"1px solid rgba(0, 243, 255, 0.2)",color:"var(--neon-blue)",padding:"8px 14px",borderRadius:"8px",fontSize:"13px",cursor:"pointer",transition:"all 0.2s",fontWeight:500},onMouseEnter:r=>{r.currentTarget.style.background="rgba(0, 243, 255, 0.15)",r.currentTarget.style.borderColor="var(--neon-blue)",r.currentTarget.style.boxShadow="0 0 10px rgba(0, 243, 255, 0.3)"},onMouseLeave:r=>{r.currentTarget.style.background="rgba(0, 243, 255, 0.05)",r.currentTarget.style.borderColor="rgba(0, 243, 255, 0.2)",r.currentTarget.style.boxShadow="none"},children:a},n))}),e.jsx("span",{style:{fontSize:"11px",color:"rgba(255,255,255,0.4)",fontStyle:"italic",marginTop:"4px"},children:"*অথবা নিচের ইনপুট বক্সে আপনার নিজের মতো করে কাস্টম উত্তর টাইপ করুন।"})]}),t.sender==="ai"&&e.jsxs("div",{className:"message-bubble-footer",children:[e.jsxs("div",{className:"bubble-footer-actions",children:[e.jsxs("button",{onClick:()=>{navigator.clipboard.writeText(t.content),l.success("টেক্সট কপি করা হয়েছে")},className:"footer-action-btn",children:[e.jsx(we,{})," কপি করুন"]}),e.jsxs("button",{type:"button",onClick:()=>xe(t.content),className:"footer-action-btn",children:[e.jsx(G,{})," শুনুন"]})]}),t.confidence&&e.jsxs("div",{className:"ai-confidence-badge",children:[e.jsx("div",{className:"ai-confidence-dot"}),e.jsxs("span",{className:"ai-confidence-text",children:[t.confidence,"% Confidence"]})]})]})]})]})},t.id)),e.jsx("div",{ref:Y})]})}),e.jsx("div",{className:"chat-input-wrapper",children:e.jsxs("div",{className:"input-max-width-wrapper",children:[h&&e.jsxs("div",{className:"image-preview-panel",children:[e.jsxs("div",{className:"preview-thumb-container",children:[e.jsx("img",{src:h,alt:"Upload preview",className:"preview-thumbnail"}),e.jsxs("div",{className:"preview-file-details",children:[e.jsx("span",{className:"preview-filename",children:J||"image.png"}),e.jsx("span",{className:"preview-badge",children:"সংযুক্ত ছবি প্রস্তুত"})]})]}),e.jsx("button",{type:"button",onClick:oe,style:{background:"transparent",border:"none",color:"rgba(255, 255, 255, 0.4)",cursor:"pointer",fontSize:"16px"},children:e.jsx(ve,{style:{color:"var(--error)"}})})]}),e.jsxs("form",{onSubmit:ge,className:"input-container-row",children:[e.jsx("button",{type:"button",onClick:()=>g(t=>t.map(a=>a.id===c?{...a,messages:[]}:a)),className:"reset-context-btn",title:"চ্যাট রিসেট করুন (Reset Context)",children:e.jsx(Q,{style:{fontSize:"20px"}})}),e.jsxs("div",{className:"main-input-capsule",children:[e.jsx("input",{type:"file",id:"chat-image-upload",accept:"image/*",onChange:se,style:{display:"none"}}),e.jsx(q,{placeholder:v?"ভয়েস রেকর্ড করা হচ্ছে... কথা বলুন...":"নিউরাল কমান্ড টাইপ করুন...",value:z,onChange:t=>T(t.target.value),disabled:w,className:"chat-styled-input",prefix:e.jsxs("div",{className:"chat-input-actions-prefix",children:[e.jsx(R,{title:"ছবি সংযুক্ত করুন",children:e.jsx("button",{type:"button",onClick:()=>{var t;return(t=document.getElementById("chat-image-upload"))==null?void 0:t.click()},className:"prefix-action-btn",children:e.jsx(je,{style:{fontSize:"16px"}})})}),e.jsx(R,{title:v?"রেকর্ডিং বন্ধ করুন":"ভয়েস ইনপুট",children:e.jsx("button",{type:"button",onClick:re,className:`prefix-action-btn ${v?"recording":""}`,children:v?e.jsx(ke,{style:{fontSize:"16px"}}):e.jsx(G,{style:{fontSize:"16px"}})})})]})}),e.jsx("div",{className:"input-submit-wrapper",children:e.jsxs("button",{type:"submit",disabled:w||!z.trim()&&!h,className:"chat-send-btn",children:[w?e.jsx(Q,{spin:!0}):e.jsx(Se,{}),e.jsx("span",{children:w?"প্রসেস...":"পাঠান"})]})})]})]}),e.jsxs("div",{style:{display:"flex",alignItems:"center",justifyContent:"space-between",marginTop:"12px",padding:"0 8px"},children:[e.jsxs("div",{style:{display:"flex",alignItems:"center",gap:"8px"},children:[e.jsx("div",{className:"session-dot",style:{background:"var(--success)",boxShadow:"0 0 6px var(--success)",width:"5px",height:"5px"}}),e.jsx("span",{style:{fontSize:"9px",fontWeight:800,color:"rgba(255, 255, 255, 0.25)",textTransform:"uppercase",letterSpacing:"1.5px"},children:"Security Level: High (Alpha-1)"})]}),e.jsx("span",{style:{fontSize:"9px",fontWeight:800,color:"rgba(255, 255, 255, 0.15)",textTransform:"uppercase",letterSpacing:"1px"},children:"AI-Driven Autonomy System • Core v6.0 Stable"})]})]})})]}),e.jsxs("div",{className:"neural-chat-knowledge-pane",children:[e.jsxs("div",{className:"knowledge-pane-title-row",children:[e.jsx(X,{style:{color:"var(--neon-blue)",fontSize:"14px"}}),e.jsx("h4",{style:{fontSize:"11px",fontWeight:800,color:"#ffffff",textTransform:"uppercase",letterSpacing:"2px",margin:0},children:"সিস্টেম কনটেক্সট (Context)"})]}),e.jsx("div",{style:{flex:1,overflowY:"auto"},className:"custom-scrollbar",children:N.rules&&N.rules.length>0?e.jsxs("div",{style:{marginBottom:"24px"},children:[e.jsxs("div",{className:"knowledge-section-header",children:[e.jsx("span",{className:"knowledge-section-title",children:"সক্রিয় রুলস (Rules)"}),e.jsx(Ne,{count:N.rules.length,style:{backgroundColor:"var(--neon-blue)",fontSize:"9px",fontWeight:800,color:"#000",border:"none"}})]}),e.jsx("div",{className:"knowledge-cards-stack",children:N.rules.map((t,a)=>e.jsx("div",{className:"knowledge-rule-card",children:t.content||t.message},a))})]}):e.jsxs("div",{style:{display:"flex",flexDirection:"column",alignItems:"center",justifyContent:"center",padding:"40px 0",opacity:.15},children:[e.jsx(X,{style:{fontSize:"28px",marginBottom:"12px"}}),e.jsx("span",{style:{fontSize:"9px",fontWeight:700,textTransform:"uppercase",letterSpacing:"1px"},children:"কোন রুলস সক্রিয় নেই"})]})})]}),e.jsx(H,{title:e.jsx("span",{style:{color:"white",fontWeight:800,textTransform:"uppercase",letterSpacing:"1px"},children:"চ্যাট সেশনের নাম পরিবর্তন"}),open:Z,onOk:V,onCancel:()=>C(!1),okText:"নাম পরিবর্তন করুন",cancelText:"বাতিল",centered:!0,className:"dark-modal",styles:{body:{backgroundColor:"#0a0a0a",borderBottomLeftRadius:"12px",borderBottomRightRadius:"12px"}},children:e.jsxs("div",{style:{padding:"16px 0"},children:[e.jsx("label",{style:{display:"block",fontSize:"9px",fontWeight:800,color:"rgba(255, 255, 255, 0.3)",textTransform:"uppercase",letterSpacing:"2px",marginBottom:"8px"},children:"নতুন নাম লিখুন"}),e.jsx(q,{value:A,onChange:t=>O(t.target.value),style:{background:"rgba(255, 255, 255, 0.05)",border:"1px solid rgba(255, 255, 255, 0.1)",color:"white",height:"46px",borderRadius:"12px"},placeholder:"চ্যাটের নতুন নাম...",onPressEnter:V,autoFocus:!0})]})}),e.jsx("style",{children:`
                .neural-chat-container {
                    display: flex;
                    flex: 1;
                    height: 100%;
                    overflow: hidden;
                    border-radius: var(--radius-lg);
                    background: var(--cyber-dark);
                }

                .neural-chat-sidebar {
                    width: 280px;
                    background: rgba(2, 2, 5, 0.65);
                    border-right: 1px solid rgba(0, 243, 255, 0.15);
                    display: flex;
                    flex-direction: column;
                    backdrop-filter: blur(20px);
                    flex-shrink: 0;
                }

                .sidebar-header {
                    padding: var(--space-3);
                    border-bottom: 1px solid rgba(0, 243, 255, 0.1);
                }

                .new-session-glow-btn:hover {
                    box-shadow: 0 0 25px rgba(0, 243, 255, 0.5) !important;
                    transform: translateY(-1px);
                }

                .sessions-list {
                    flex: 1;
                    overflow-y: auto;
                    padding: var(--space-2);
                    display: flex;
                    flex-direction: column;
                    gap: 8px;
                }

                .session-item {
                    display: flex;
                    align-items: center;
                    justify-content: space-between;
                    padding: 12px 16px;
                    border-radius: 12px;
                    cursor: pointer;
                    transition: all 0.3s cubic-bezier(0.2, 0.8, 0.2, 1);
                    border: 1px solid transparent;
                    position: relative;
                    overflow: hidden;
                }

                .session-item.active {
                    background: linear-gradient(90deg, rgba(0, 243, 255, 0.12), rgba(188, 19, 254, 0.03));
                    border-color: rgba(0, 243, 255, 0.3);
                    box-shadow: 0 0 15px rgba(0, 243, 255, 0.05);
                }

                .session-item:not(.active):hover {
                    background: rgba(255, 255, 255, 0.03);
                    border-color: rgba(255, 255, 255, 0.05);
                }

                .session-name-container {
                    display: flex;
                    align-items: center;
                    gap: 12px;
                    flex: 1;
                    overflow: hidden;
                }

                .session-dot {
                    width: 6px;
                    height: 6px;
                    border-radius: 50%;
                    background: rgba(255, 255, 255, 0.2);
                    transition: all 0.3s ease;
                }

                .session-item.active .session-dot {
                    background: var(--neon-blue);
                    box-shadow: 0 0 8px var(--neon-blue);
                }

                .session-name-text {
                    font-size: 13px;
                    font-weight: 600;
                    color: rgba(255, 255, 255, 0.5);
                    text-transform: uppercase;
                    letter-spacing: 0.5px;
                    white-space: nowrap;
                    overflow: hidden;
                    text-overflow: ellipsis;
                    transition: all 0.3s ease;
                }

                .session-item.active .session-name-text {
                    color: #ffffff;
                }

                .session-actions {
                    display: flex;
                    align-items: center;
                    gap: 10px;
                    opacity: 0;
                    transition: opacity 0.25s ease;
                    padding-left: 8px;
                }

                .session-item:hover .session-actions {
                    opacity: 1;
                }

                .action-icon {
                    font-size: 13px;
                    color: rgba(255, 255, 255, 0.4);
                    transition: color 0.2s ease;
                    cursor: pointer;
                }

                .action-icon:hover {
                    color: var(--neon-blue);
                }

                .action-icon.delete:hover {
                    color: var(--error);
                }

                /* Chat Layout Content Pane */
                .neural-chat-content {
                    display: flex;
                    flex-direction: column;
                    flex: 1;
                    position: relative;
                    background: #030307;
                }

                .chat-content-header {
                    padding: 16px 24px;
                    background: rgba(2, 2, 5, 0.4);
                    border-bottom: 1px solid rgba(0, 243, 255, 0.15);
                    display: flex;
                    align-items: center;
                    justify-content: space-between;
                    backdrop-filter: blur(10px);
                }

                .header-info-container {
                    display: flex;
                    align-items: center;
                    gap: 16px;
                }

                .header-icon-wrapper {
                    padding: 10px;
                    background: rgba(0, 243, 255, 0.1);
                    border-radius: 12px;
                    border: 1px solid rgba(0, 243, 255, 0.2);
                    display: flex;
                    align-items: center;
                    justify-content: center;
                }

                .header-title-text {
                    font-size: 15px;
                    font-weight: 700;
                    color: #ffffff;
                    margin: 0 0 2px 0;
                }

                .header-subtitle-text {
                    font-size: 9px;
                    color: var(--neon-blue);
                    font-weight: 800;
                    text-transform: uppercase;
                    letter-spacing: 2px;
                }

                .custom-agent-select {
                    background: rgba(8, 8, 16, 0.8);
                    border: 1px solid rgba(0, 243, 255, 0.25);
                    color: rgba(255, 255, 255, 0.85);
                    font-size: 12px;
                    font-family: var(--font-mono);
                    padding: 8px 16px;
                    border-radius: 8px;
                    outline: none;
                    cursor: pointer;
                    transition: all 0.3s ease;
                }

                .custom-agent-select:hover {
                    border-color: rgba(0, 243, 255, 0.5);
                    box-shadow: 0 0 10px rgba(0, 243, 255, 0.15);
                }

                /* Chat Messages Display Area */
                .chat-messages-area {
                    flex: 1;
                    overflow-y: auto;
                    padding: 24px;
                }

                .messages-max-width-wrapper {
                    max-width: 800px;
                    margin: 0 auto;
                    width: 100%;
                    display: flex;
                    flex-direction: column;
                    gap: 24px;
                }

                .message-row {
                    display: flex;
                    width: 100%;
                }

                .message-row.user {
                    justify-content: flex-end;
                }

                .message-row.ai {
                    justify-content: flex-start;
                }

                .message-bubble-wrapper {
                    max-width: 80%;
                    display: flex;
                    flex-direction: column;
                }

                .message-meta-header {
                    display: flex;
                    align-items: center;
                    gap: 10px;
                    margin-bottom: 6px;
                    padding: 0 4px;
                }

                .message-meta-header.user {
                    justify-content: flex-end;
                }

                .message-meta-header.ai {
                    justify-content: flex-start;
                }

                .meta-sender-name {
                    font-size: 10px;
                    font-weight: 800;
                    color: rgba(255, 255, 255, 0.35);
                    text-transform: uppercase;
                    letter-spacing: 1.5px;
                }

                .message-bubble {
                    padding: 16px 20px;
                    border-radius: 16px;
                    font-size: 14px;
                    line-height: 1.6;
                    box-shadow: 0 10px 25px -10px rgba(0, 0, 0, 0.5);
                    transition: all 0.3s ease;
                }

                .message-bubble.user {
                    background: linear-gradient(135deg, rgba(0, 243, 255, 0.15), rgba(188, 19, 254, 0.04));
                    border: 1px solid rgba(0, 243, 255, 0.25);
                    color: #ffffff;
                    border-top-right-radius: 2px;
                }

                .message-bubble.user:hover {
                    border-color: rgba(0, 243, 255, 0.4);
                    box-shadow: 0 10px 30px -10px rgba(0, 243, 255, 0.15);
                }

                .message-bubble.ai {
                    background: rgba(255, 255, 255, 0.03);
                    border: 1px solid rgba(255, 255, 255, 0.06);
                    color: rgba(255, 255, 255, 0.9);
                    border-top-left-radius: 2px;
                    backdrop-filter: blur(10px);
                }

                .message-bubble.ai:hover {
                    border-color: rgba(0, 243, 255, 0.15);
                    background: rgba(255, 255, 255, 0.04);
                }

                .message-bubble-footer {
                    display: flex;
                    align-items: center;
                    justify-content: space-between;
                    margin-top: 12px;
                    padding-top: 10px;
                    border-top: 1px solid rgba(255, 255, 255, 0.05);
                }

                .bubble-footer-actions {
                    display: flex;
                    gap: 16px;
                }

                .footer-action-btn {
                    background: transparent;
                    border: none;
                    color: rgba(255, 255, 255, 0.35);
                    font-size: 10px;
                    font-weight: 700;
                    text-transform: uppercase;
                    letter-spacing: 1px;
                    cursor: pointer;
                    display: flex;
                    align-items: center;
                    gap: 6px;
                    transition: all 0.2s ease;
                    padding: 2px 0;
                }

                .footer-action-btn:hover {
                    color: var(--neon-blue);
                }

                .ai-confidence-badge {
                    display: flex;
                    align-items: center;
                    gap: 6px;
                    padding: 3px 8px;
                    background: rgba(0, 243, 255, 0.08);
                    border: 1px solid rgba(0, 243, 255, 0.2);
                    border-radius: 6px;
                }

                .ai-confidence-dot {
                    width: 5px;
                    height: 5px;
                    border-radius: 50%;
                    background: var(--neon-blue);
                    box-shadow: 0 0 6px var(--neon-blue);
                }

                .ai-confidence-text {
                    font-size: 9px;
                    font-weight: 800;
                    color: var(--neon-blue);
                    text-transform: uppercase;
                    letter-spacing: 0.5px;
                }

                /* Chat Input Styling */
                .chat-input-wrapper {
                    padding: 24px;
                    background: linear-gradient(180deg, transparent, rgba(2, 2, 5, 0.95));
                    border-top: 1px solid rgba(255, 255, 255, 0.05);
                    position: relative;
                    z-index: 10;
                }

                .input-max-width-wrapper {
                    max-width: 800px;
                    margin: 0 auto;
                    width: 100%;
                }

                .image-preview-panel {
                    background: rgba(8, 8, 16, 0.85);
                    border: 1px solid rgba(0, 243, 255, 0.2);
                    border-radius: 12px;
                    padding: 12px 16px;
                    margin-bottom: 12px;
                    display: flex;
                    align-items: center;
                    justify-content: space-between;
                    backdrop-filter: blur(10px);
                }

                .preview-thumb-container {
                    display: flex;
                    align-items: center;
                    gap: 12px;
                }

                .preview-thumbnail {
                    width: 44px;
                    height: 44px;
                    border-radius: 8px;
                    object-fit: cover;
                    border: 1px solid rgba(255, 255, 255, 0.1);
                }

                .preview-file-details {
                    display: flex;
                    flex-direction: column;
                }

                .preview-filename {
                    font-size: 12px;
                    font-weight: 700;
                    color: #ffffff;
                }

                .preview-badge {
                    font-size: 8px;
                    font-weight: 800;
                    color: var(--neon-blue);
                    letter-spacing: 1.5px;
                    margin-top: 2px;
                }

                .input-container-row {
                    display: flex;
                    gap: 12px;
                    align-items: center;
                }

                .reset-context-btn {
                    width: 56px;
                    height: 56px;
                    border-radius: 14px;
                    background: rgba(188, 19, 254, 0.1);
                    border: 1px solid rgba(188, 19, 254, 0.3);
                    color: var(--neon-purple);
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    cursor: pointer;
                    transition: all 0.3s ease;
                    flex-shrink: 0;
                }

                .reset-context-btn:hover {
                    background: rgba(188, 19, 254, 0.25);
                    border-color: var(--neon-purple);
                    box-shadow: 0 0 15px rgba(188, 19, 254, 0.2);
                    color: #ffffff;
                }

                .main-input-capsule {
                    flex: 1;
                    position: relative;
                    display: flex;
                    align-items: center;
                }

                .chat-styled-input {
                    background: rgba(2, 2, 5, 0.8) !important;
                    border: 1px solid rgba(0, 243, 255, 0.2) !important;
                    border-radius: 16px !important;
                    height: 56px !important;
                    font-size: 13px !important;
                    font-family: var(--font-mono) !important;
                    color: #ffffff !important;
                    padding-left: 100px !important;
                    padding-right: 140px !important;
                    transition: all 0.3s ease !important;
                }

                .chat-styled-input:focus, .chat-styled-input:hover {
                    border-color: rgba(0, 243, 255, 0.5) !important;
                    box-shadow: 0 0 15px rgba(0, 243, 255, 0.15) !important;
                }

                .chat-input-actions-prefix {
                    position: absolute;
                    left: 8px;
                    top: 50%;
                    transform: translateY(-50%);
                    display: flex;
                    align-items: center;
                    gap: 4px;
                    z-index: 5;
                    border-right: 1px solid rgba(0, 243, 255, 0.15);
                    padding-right: 8px;
                }

                .prefix-action-btn {
                    background: transparent;
                    border: none;
                    width: 36px;
                    height: 36px;
                    border-radius: 10px;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    color: rgba(0, 243, 255, 0.5);
                    cursor: pointer;
                    transition: all 0.2s ease;
                }

                .prefix-action-btn:hover {
                    background: rgba(0, 243, 255, 0.1);
                    color: var(--neon-blue);
                }

                .prefix-action-btn.recording {
                    background: rgba(239, 68, 68, 0.15);
                    color: #ef4444;
                    animation: recordingPulse 1.5s infinite ease-in-out;
                }

                @keyframes recordingPulse {
                    0%, 100% { opacity: 0.8; }
                    50% { opacity: 1; box-shadow: 0 0 10px rgba(239, 68, 68, 0.2); }
                }

                .input-submit-wrapper {
                    position: absolute;
                    right: 8px;
                    top: 50%;
                    transform: translateY(-50%);
                    display: flex;
                    align-items: center;
                    gap: 8px;
                    z-index: 5;
                }

                .chat-send-btn {
                    height: 40px;
                    padding: 0 20px;
                    background: var(--neon-blue);
                    border: none;
                    border-radius: 10px;
                    color: #020205;
                    font-weight: 800;
                    font-size: 11px;
                    text-transform: uppercase;
                    letter-spacing: 1.5px;
                    cursor: pointer;
                    display: flex;
                    align-items: center;
                    gap: 8px;
                    transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
                }

                .chat-send-btn:hover:not(:disabled) {
                    background: #ffffff;
                    box-shadow: 0 0 15px rgba(0, 243, 255, 0.4);
                    transform: translateY(-1px);
                }

                .chat-send-btn:disabled {
                    background: rgba(255, 255, 255, 0.05);
                    color: rgba(255, 255, 255, 0.15);
                    cursor: not-allowed;
                }

                /* Right Sidebar: Knowledge Context styling */
                .neural-chat-knowledge-pane {
                    width: 320px;
                    background: rgba(2, 2, 5, 0.3);
                    border-left: 1px solid rgba(0, 243, 255, 0.15);
                    display: flex;
                    flex-direction: column;
                    padding: 24px;
                    flex-shrink: 0;
                }

                .knowledge-pane-title-row {
                    display: flex;
                    align-items: center;
                    gap: 12px;
                    margin-bottom: 24px;
                }

                .knowledge-section-header {
                    display: flex;
                    align-items: center;
                    justify-content: space-between;
                    margin-bottom: 16px;
                }

                .knowledge-section-title {
                    font-size: 10px;
                    font-weight: 800;
                    color: rgba(255, 255, 255, 0.35);
                    text-transform: uppercase;
                    letter-spacing: 2px;
                }

                .knowledge-cards-stack {
                    display: flex;
                    flex-direction: column;
                    gap: 12px;
                }

                .knowledge-rule-card {
                    background: rgba(255, 255, 255, 0.02);
                    border: 1px solid rgba(255, 255, 255, 0.05);
                    border-radius: 12px;
                    padding: 14px;
                    font-size: 11.5px;
                    line-height: 1.5;
                    color: rgba(255, 255, 255, 0.65);
                    transition: all 0.25s ease;
                }

                .knowledge-rule-card:hover {
                    background: rgba(255, 255, 255, 0.04);
                    border-color: rgba(0, 243, 255, 0.15);
                    color: #ffffff;
                }

                .custom-scrollbar::-webkit-scrollbar {
                    width: 4px;
                }
                .custom-scrollbar::-webkit-scrollbar-track {
                    background: transparent;
                }
                .custom-scrollbar::-webkit-scrollbar-thumb {
                    background: rgba(255, 255, 255, 0.05);
                    border-radius: 10px;
                }
                .custom-scrollbar::-webkit-scrollbar-thumb:hover {
                    background: rgba(0, 243, 255, 0.2);
                }
                .dark-modal .ant-modal-content {
                    background-color: #05050a !important;
                    border: 1px solid rgba(255, 255, 255, 0.1) !important;
                    border-radius: 16px !important;
                    overflow: hidden !important;
                }
                .dark-modal .ant-modal-header {
                    background-color: #05050a !important;
                    border-bottom: 1px solid rgba(255, 255, 255, 0.1) !important;
                }
                .dark-modal .ant-modal-title {
                    color: white !important;
                }
                .dark-modal .ant-modal-close-x {
                    color: rgba(255, 255, 255, 0.4) !important;
                }
                .dark-modal .ant-btn-primary {
                    background-color: var(--neon-blue) !important;
                    color: #000 !important;
                    font-weight: bold !important;
                    border: none !important;
                }
                .dark-modal .ant-btn-default {
                    background-color: transparent !important;
                    border: 1px solid rgba(255, 255, 255, 0.1) !important;
                    color: white !important;
                }
            `})]})};export{We as default};
