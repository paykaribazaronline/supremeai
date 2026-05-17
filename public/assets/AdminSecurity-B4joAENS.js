import{j as e}from"./vendor-charts-ncBmr_CS.js";import{r as n}from"./vendor-three-DwQ_Ww00.js";import{m as pe,f as x}from"./index-C3YYadhl.js";import{C as j,x as R,aW as xe,D as ue,a4 as L,T as v,K as Y,N as g,r as f,I as G,B as h,d as ge,o as m,O as be,X as me,_ as fe,aa as ye,v as J,q as V,ap as U,a5 as he,as as je,L as z,z as k,S as ve,al as A,ae as Se,a as Te,aM as F,p as we,s as d}from"./vendor-antd-DjZVmnz2.js";import"./vendor-react-DdkrXTat.js";const{Text:Me}=v,ke=({healthScore:t,healthStatus:a,healthReason:i})=>e.jsxs(j,{bordered:!1,className:"glass-card",style:{height:"100%",background:"rgba(255,255,255,0.02)",border:"1px solid rgba(255,255,255,0.1)"},children:[e.jsx(R,{title:e.jsx("span",{style:{color:"rgba(255,255,255,0.45)"},children:"সিস্টেম হেলথ স্কোর"}),value:t,suffix:"/ 100",prefix:e.jsx(xe,{style:{color:a==="healthy"?"#10b981":"#ef4444"}}),valueStyle:{color:"#fff",fontSize:"24px"}}),e.jsx("div",{style:{marginTop:20,textAlign:"center"},children:e.jsx(ue,{type:"dashboard",percent:t,strokeColor:{"0%":"#ef4444","50%":"#f59e0b","100%":"#10b981"},trailColor:"rgba(255,255,255,0.05)"})}),e.jsx("div",{style:{marginTop:16},children:e.jsx(L,{message:i||"All systems operational",type:a==="healthy"?"success":"warning",showIcon:!0,style:{borderRadius:8,background:"rgba(255,255,255,0.05)",border:"none"}})})]}),{Title:Ae,Text:Ie}=v,{TextArea:Ce}=G,Re=({healingStatus:t,testError:a,setTestError:i,handleTestFix:u,fixing:s,fixResult:c})=>{var l;return e.jsxs(j,{title:e.jsxs("span",{style:{color:"#fff"},children:[e.jsx(be,{})," Self-Healing System Status"]}),bordered:!1,className:"glass-card",style:{background:"rgba(255,255,255,0.02)",border:"1px solid rgba(255,255,255,0.1)"},children:[e.jsxs(Y,{gutter:16,children:[e.jsx(g,{span:8,children:e.jsx(R,{title:e.jsx("span",{style:{color:"rgba(255,255,255,0.45)"},children:"স্ট্যাটাস"}),value:((l=t==null?void 0:t.status)==null?void 0:l.toUpperCase())||"ACTIVE",valueStyle:{color:"#10b981",fontSize:"18px"}})}),e.jsx(g,{span:8,children:e.jsx(R,{title:e.jsx("span",{style:{color:"rgba(255,255,255,0.45)"},children:"অটো-হিলিং"}),value:(t==null?void 0:t.autoHealing)||"Enabled",valueStyle:{color:"#3b82f6",fontSize:"18px"}})}),e.jsx(g,{span:8,children:e.jsx(R,{title:e.jsx("span",{style:{color:"rgba(255,255,255,0.45)"},children:"ইনফিনিট লুপ"}),value:(t==null?void 0:t.infiniteLoop)||"Active",valueStyle:{color:"#f59e0b",fontSize:"18px"}})})]}),e.jsxs("div",{style:{marginTop:24},children:[e.jsx(Ae,{level:5,style:{color:"#fff",marginBottom:16},children:"ইন্টারেক্টিভ এরর সিমুলেটর"}),e.jsxs(f,{direction:"vertical",style:{width:"100%"},children:[e.jsx(Ce,{placeholder:"একটি এরর মেসেজ লিখুন (যেমন: Connection timeout to provider X)...",rows:3,value:a,onChange:p=>i(p.target.value),style:{background:"rgba(0,0,0,0.2)",color:"#fff",borderColor:"rgba(255,255,255,0.1)"}}),e.jsx(h,{type:"primary",icon:e.jsx(ge,{}),onClick:u,loading:s,block:!0,style:{background:"linear-gradient(135deg, #8b5cf6 0%, #6d28d9 100%)",border:"none",height:"40px"},children:"ডিটেক্ট এবং অটো-ফিক্স পরীক্ষা করুন"})]}),c&&e.jsx("div",{style:{marginTop:16},children:e.jsx(L,{message:"অটো-ফিক্স বিশ্লেষণ ফলাফল",description:e.jsxs("div",{style:{marginTop:8},children:[e.jsx(Ie,{style:{color:"rgba(255,255,255,0.8)"},children:c.summary||c.status||"প্রক্রিয়াটি সফলভাবে সম্পন্ন হয়েছে।"}),e.jsx("br",{}),e.jsxs(f,{style:{marginTop:8},children:[e.jsxs(m,{color:"green",children:["Action: ",c.actionTaken||"Analyzed"]}),e.jsxs(m,{color:"blue",children:["Confidence: ",c.confidence||"High"]})]})]}),type:"info",style:{background:"rgba(139, 92, 246, 0.1)",border:"1px solid rgba(139, 92, 246, 0.2)"}})})]})]})},{Title:Ee,Text:I}=v,ze=({learnTopic:t,setLearnTopic:a,onStartLearning:i,learning:u,cyberSkills:s,autonomousLearningEnabled:c,onToggleAutonomous:l})=>e.jsxs(j,{title:e.jsxs("div",{style:{display:"flex",justifyContent:"space-between",alignItems:"center",width:"100%"},children:[e.jsxs("span",{style:{color:"#fff"},children:[e.jsx(ye,{})," Autonomous Cyber Learning"]}),e.jsxs(f,{children:[e.jsxs(I,{style:{color:"rgba(255,255,255,0.45)",fontSize:"12px"},children:[e.jsx(J,{})," Autonomous Mode"]}),e.jsx(V,{size:"small",checked:c,onChange:l})]})]}),bordered:!1,className:"glass-card",style:{background:"rgba(255,255,255,0.02)",border:"1px solid rgba(255,255,255,0.1)"},children:[e.jsxs(f,{direction:"vertical",style:{width:"100%"},children:[e.jsx(I,{style:{color:"rgba(255,255,255,0.6)"},children:"নিজে নিজে হ্যাকিং স্কিল শিখে সিস্টেমকে নিরাপদ রাখার মডিউল।"}),e.jsx(G,{placeholder:"Vulnerability Topic (e.g., SQL Injection, Zero-day)...",value:t,onChange:p=>a(p.target.value),style:{background:"rgba(0,0,0,0.2)",color:"#fff"}}),e.jsx(h,{type:"primary",icon:e.jsx(me,{}),onClick:i,loading:u,block:!0,children:"Initiate Learning Cycle"})]}),e.jsx(fe,{style:{borderColor:"rgba(255,255,255,0.05)"}}),e.jsxs(Ee,{level:5,style:{color:"#fff"},children:["Mastered Skills (",s.length,")"]}),e.jsx("div",{style:{maxHeight:200,overflowY:"auto"},children:s.map(p=>e.jsxs("div",{style:{marginBottom:12,padding:8,background:"rgba(255,255,255,0.03)",borderRadius:8},children:[e.jsxs("div",{style:{display:"flex",justifyContent:"space-between"},children:[e.jsx(I,{strong:!0,style:{color:"#00f3ff"},children:p.name}),e.jsx(m,{color:"cyan",children:p.status})]}),e.jsx(I,{style:{fontSize:12,color:"rgba(255,255,255,0.45)"},children:p.description})]},p.id))})]}),{Title:$e,Text:C}=v,Le=({onRunAudit:t,auditing:a,auditReport:i,protections:u,autonomousAuditEnabled:s,onToggleAutonomous:c})=>e.jsxs(j,{title:e.jsxs("div",{style:{display:"flex",justifyContent:"space-between",alignItems:"center",width:"100%"},children:[e.jsxs("span",{style:{color:"#fff"},children:[e.jsx(je,{})," System Self-Audit (Red Team)"]}),e.jsxs(f,{children:[e.jsxs(C,{style:{color:"rgba(255,255,255,0.45)",fontSize:"12px"},children:[e.jsx(J,{})," Autonomous Mode"]}),e.jsx(V,{size:"small",checked:s,onChange:c})]})]}),bordered:!1,className:"glass-card",style:{background:"rgba(255,255,255,0.02)",border:"1px solid rgba(255,255,255,0.1)"},children:[e.jsxs("div",{style:{textAlign:"center",padding:"20px 0"},children:[e.jsx(h,{size:"large",danger:!0,icon:e.jsx(U,{}),onClick:t,loading:a,style:{height:60,fontSize:18,padding:"0 40px",borderRadius:30},children:"Run System Self-Audit"}),e.jsx("p",{style:{marginTop:16,color:"rgba(255,255,255,0.45)"},children:"সিস্টেম নিজের ওপর একটি নিয়ন্ত্রিত সাইবার অ্যাটাক সিমুলেট করবে রেজিলিয়েন্স পরীক্ষার জন্য।"})]}),i&&e.jsx(pe.div,{initial:{opacity:0},animate:{opacity:1},children:e.jsx(L,{message:"Self-Audit Result",description:e.jsxs("div",{style:{marginTop:8},children:[e.jsxs(C,{strong:!0,style:{color:"#fff"},children:["Score: ",(i.resilienceScore*100).toFixed(1),"%"]}),e.jsx("br",{}),e.jsx(C,{style:{color:"rgba(255,255,255,0.8)"},children:i.summary}),e.jsxs("div",{style:{marginTop:12},children:[e.jsxs(m,{color:"green",children:["Vulnerabilities: ",i.vulnerabilitiesFound]}),e.jsxs(m,{color:"blue",children:["Audit ID: ",i.auditId.substring(0,8)]})]})]}),type:"success",showIcon:!0,style:{background:"rgba(82, 196, 26, 0.1)",border:"1px solid rgba(82, 196, 26, 0.2)"}})}),e.jsxs("div",{style:{marginTop:24},children:[e.jsx($e,{level:5,style:{color:"#fff"},children:"Active Dynamic Protections"}),u.length===0?e.jsx(C,{type:"secondary",children:"No active shields generated yet."}):e.jsx("div",{style:{display:"flex",flexWrap:"wrap",gap:8},children:u.map(l=>e.jsxs(m,{color:"processing",icon:e.jsx(he,{}),children:[l.protectionType," (ID: ",l.targetId.substring(0,5),")"]},l.targetId))})]})]}),Ne=()=>{const t=[{title:"Firewall Monitoring",status:"Online",icon:e.jsx(k,{style:{color:"#10b981"}})},{title:"Intrusion Detection System",status:"Active",icon:e.jsx(k,{style:{color:"#10b981"}})},{title:"API Security Layer",status:"Secured",icon:e.jsx(k,{style:{color:"#10b981"}})},{title:"Database Encryption",status:"AES-256 Enabled",icon:e.jsx(k,{style:{color:"#10b981"}})}];return e.jsx(j,{title:e.jsxs("span",{style:{color:"#fff"},children:[e.jsx(U,{})," Cyber Guard Active Surveillance"]}),bordered:!1,className:"glass-card",style:{background:"rgba(255,255,255,0.02)",border:"1px solid rgba(255,255,255,0.1)"},children:e.jsx(z,{itemLayout:"horizontal",dataSource:t,renderItem:a=>e.jsxs(z.Item,{style:{borderBottom:"1px solid rgba(255,255,255,0.05)"},children:[e.jsx(z.Item.Meta,{avatar:a.icon,title:e.jsx("span",{style:{color:"#fff"},children:a.title}),description:e.jsx("span",{style:{color:"rgba(255,255,255,0.45)"},children:a.status})}),e.jsx(m,{color:"success",children:"OPERATIONAL"})]})})})},{Title:Pe,Text:$}=v,Fe=()=>{const[t,a]=n.useState(!0),[i,u]=n.useState(null),[s,c]=n.useState(null),[l,p]=n.useState(""),[X,N]=n.useState(!1),[q,K]=n.useState(null),[Q,Z]=n.useState([]),[_,ee]=n.useState([]),[te,P]=n.useState(!1),[re,se]=n.useState(null),[ae,O]=n.useState(!1),[S,B]=n.useState(""),[D,H]=n.useState({autonomousLearningEnabled:!1,autonomousAuditEnabled:!1}),y=async()=>{var r;a(!0);try{const[o,T,E,w]=await Promise.all([x("/api/self-healing/status"),x("/api/admin/dashboard/contract"),x("/api/admin/security/cyber/skills"),x("/api/admin/security/cyber/protections")]);if(o.ok&&u(await o.json()),T.ok){const b=await T.json();c((r=b.data)==null?void 0:r.stats)}if(E.ok){const b=await E.json();Z(b.data||[])}if(w.ok){const b=await w.json();ee(b.data||[])}const M=await x("/api/admin/security/cyber/config");if(M.ok){const b=await M.json();H(b.data)}}catch(o){console.error("Error fetching security data:",o),d.error("ডাটা লোড করতে ব্যর্থ হয়েছে")}finally{a(!1)}};n.useEffect(()=>{y();const r=setInterval(y,3e4);return()=>clearInterval(r)},[]);const oe=async()=>{if(!l.trim()){d.warning("অনুগ্রহ করে একটি এরর মেসেজ লিখুন");return}N(!0);try{const r=await x("/api/self-healing/detect",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({error:l})});if(r.ok){const o=await r.json();K(o),d.success("সিস্টেম এররটি বিশ্লেষণ করেছে")}else d.error("এরর ডিটেকশন ব্যর্থ হয়েছে")}catch{d.error("সার্ভার ত্রুটি")}finally{N(!1)}},ne=async()=>{P(!0);try{const r=await x("/api/admin/security/cyber/audit",{method:"POST"});if(r.ok){const o=await r.json();se(o.data),d.success("সেলফ-অডিট সফলভাবে সম্পন্ন হয়েছে")}}catch{d.error("অডিট ব্যর্থ হয়েছে")}finally{P(!1)}},ie=async()=>{if(S.trim()){O(!0);try{(await x("/api/admin/security/cyber/learn",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({topic:S})})).ok&&(d.success(`সিস্টেম ডিফেন্স লার্নিং শুরু করেছে: ${S}`),B(""),y())}catch{d.error("লার্নিং সাইকেল ব্যর্থ হয়েছে")}finally{O(!1)}}},W=async(r,o)=>{try{(await x("/api/admin/security/cyber/config",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify(r==="learning"?{autonomousLearningEnabled:o}:{autonomousAuditEnabled:o})})).ok&&(H(w=>({...w,...r==="learning"?{autonomousLearningEnabled:o}:{autonomousAuditEnabled:o}})),d.success(`${r==="learning"?"Autonomous Learning":"Autonomous Audit"} ${o?"সক্রিয়":"নিষ্ক্রিয়"} করা হয়েছে`))}catch{d.error("কনফিগারেশন আপডেট করতে ব্যর্থ হয়েছে")}};if(t&&!s)return e.jsx("div",{className:"loading-fallback",children:e.jsx(ve,{size:"large",tip:"সিকিউরিটি ডাটা লোড হচ্ছে..."})});const le=(s==null?void 0:s.systemHealthScore)||100,ce=(s==null?void 0:s.systemHealthStatus)||"healthy",de=(s==null?void 0:s.systemHealthReason)||"All systems operational";return e.jsxs("div",{className:"admin-page",children:[e.jsxs("div",{className:"admin-header",children:[e.jsxs(A,{separator:">",style:{marginBottom:"var(--space-2)",opacity:.7},children:[e.jsxs(A.Item,{href:"",children:[e.jsx(Se,{})," ড্যাশবোর্ড"]}),e.jsxs(A.Item,{children:[e.jsx(Te,{})," সিস্টেম প্রোটেকশন"]}),e.jsx(A.Item,{children:"সিকিউরিটি & রেজিলিয়েন্স"})]}),e.jsxs("div",{style:{display:"flex",justifyContent:"space-between",alignItems:"flex-end",flexWrap:"wrap",gap:"var(--space-4)"},children:[e.jsxs("div",{children:[e.jsxs(Pe,{level:2,className:"admin-title",children:["নিরাপত্তা ও স্থিতিশীলতা ",e.jsx("span",{className:"admin-badge",style:{background:"rgba(16, 185, 129, 0.1)",color:"var(--success)",borderColor:"rgba(16, 185, 129, 0.3)"},children:"CYBER GUARD"})]}),e.jsx($,{className:"admin-subtitle",children:"AI-চালিত স্ব-সura classifiers এবং রেজিলিয়েন্সoutes"})]}),e.jsx(h,{type:"primary",icon:e.jsx(F,{}),onClick:y,loading:t,className:"admin-btn-primary",style:{background:"linear-gradient(135deg, #10b981 0%, #059669 100%)",border:"none",fontWeight:600,boxShadow:"0 4px clamp(12px, 2vw, 20px) rgba(16, 185, 129, 0.3)"},children:"রিফ্রেশ"})]})]}),e.jsxs("div",{style:{display:"flex",alignItems:"center",justifyContent:"space-between",marginBottom:"var(--space-4)",flexWrap:"wrap",gap:"var(--space-3)"},children:[e.jsx($,{style:{color:"rgba(255,255,255,0.45)",fontSize:"var(--text-sm)"},children:"সিস্টেমের নিরাপত্তা স্তর মনিটর করুন এবং অটোমেটেড ডিফেন্স পরিচালনা করুন"}),e.jsxs(f,{children:[e.jsx(we,{status:"processing",color:"#10b981",text:e.jsx($,{style:{color:"#10b981",fontWeight:600,fontSize:"var(--text-xs)"},children:"Cyber Guard Active"})}),e.jsx(h,{type:"primary",icon:e.jsx(F,{}),onClick:y,loading:t,className:"admin-btn-primary",style:{background:"linear-gradient(135deg, #3b82f6 0%, #2563eb 100%)",border:"none",fontWeight:600,boxShadow:"0 4px clamp(12px, 2vw, 20px) rgba(59, 130, 246, 0.3)"},children:"রিফ্রেশ ডাটা"})]})]}),e.jsxs(Y,{gutter:[16,16],style:{marginTop:"var(--space-5)"},children:[e.jsx(g,{xs:24,lg:8,children:e.jsx(ke,{healthScore:le,healthStatus:ce,healthReason:de})}),e.jsx(g,{xs:24,lg:16,children:e.jsx(Re,{healingStatus:i,testError:l,setTestError:p,handleTestFix:oe,fixing:X,fixResult:q})}),e.jsx(g,{xs:24,lg:12,children:e.jsx(ze,{learnTopic:S,setLearnTopic:B,onStartLearning:ie,learning:ae,cyberSkills:Q,autonomousLearningEnabled:D.autonomousLearningEnabled,onToggleAutonomous:r=>W("learning",r)})}),e.jsx(g,{xs:24,lg:12,children:e.jsx(Le,{onRunAudit:ne,auditing:te,auditReport:re,protections:_,autonomousAuditEnabled:D.autonomousAuditEnabled,onToggleAutonomous:r=>W("audit",r)})}),e.jsx(g,{xs:24,children:e.jsx(Ne,{})})]}),e.jsx("style",{children:`
        .glass-card {
          border-radius: 24px !important;
          background: rgba(255,255,255,0.02) !important;
          border: 1px solid rgba(255,255,255,0.08) !important;
          backdrop-filter: blur(20px);
          -webkit-backdrop-filter: blur(20px);
          box-shadow: 0 10px 30px rgba(0,0,0,0.2) !important;
          transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
          overflow: hidden;
        }
        
        .glass-card:hover {
          transform: translateY(-4px);
          background: rgba(255,255,255,0.04) !important;
          border-color: rgba(255,255,255,0.15) !important;
          box-shadow: 0 20px 40px rgba(0,0,0,0.4) !important;
        }

        .ant-breadcrumb-link {
          color: rgba(255,255,255,0.45) !important;
          font-size: 13px !important;
        }
        
        .ant-breadcrumb-link a {
          color: rgba(255,255,255,0.45) !important;
        }
        
        .ant-breadcrumb-link a:hover {
          color: #3b82f6 !important;
        }

        .ant-breadcrumb-separator {
          color: rgba(255,255,255,0.2) !important;
        }

        .ant-typography {
          color: #fff !important;
        }

        /* Sub-component style overrides */
        .ant-card-head {
          border-bottom: 1px solid rgba(255,255,255,0.05) !important;
          padding: 16px 24px !important;
        }

        .ant-card-head-title {
          font-size: 16px !important;
          font-weight: 700 !important;
          letter-spacing: 0.5px !important;
          text-transform: uppercase !important;
        }

        .ant-statistic-title {
          color: rgba(255,255,255,0.45) !important;
          font-size: 12px !important;
          text-transform: uppercase !important;
          letter-spacing: 1px !important;
          font-weight: 700 !important;
          margin-bottom: 12px !important;
        }

        .ant-input, .ant-input-affix-wrapper, .ant-input-number {
          background: rgba(255,255,255,0.05) !important;
          border: 1px solid rgba(255,255,255,0.1) !important;
          border-radius: 12px !important;
          color: #fff !important;
          padding: 8px 16px !important;
          transition: all 0.3s ease !important;
        }

        .ant-input:focus, .ant-input-affix-wrapper-focused {
          border-color: #3b82f6 !important;
          box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.2) !important;
          background: rgba(255,255,255,0.08) !important;
        }

        .ant-btn-primary {
          border-radius: 12px !important;
          font-weight: 600 !important;
          transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
        }

        .ant-btn-primary:hover {
          transform: translateY(-2px);
          box-shadow: 0 8px 20px rgba(59, 130, 246, 0.4) !important;
        }
      `})]})};export{Fe as default};
