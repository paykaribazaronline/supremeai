import{j as e}from"./vendor-charts-ncBmr_CS.js";import{r as d,a as B}from"./vendor-three-DwQ_Ww00.js";import{f as j}from"./index-BqLVfG_f.js";import{F as p,al as x,ae as F,c as L,T as O,B as b,i as $,k as M,I as g,f as S,aM as q,r as C,e as T,g as U,h as G,C as Y,j as H,M as J,aU as K,s as n,o as _,P as Q,u as X}from"./vendor-antd-DjZVmnz2.js";import"./vendor-react-DdkrXTat.js";const{Title:Z,Text:o}=O,{Option:h}=T,oe=()=>{const[k,v]=d.useState(!0),[N,I]=d.useState([]),[z,u]=d.useState(!1),[f]=p.useForm(),[y,R]=d.useState(""),[c,P]=d.useState("name"),[i,V]=d.useState("ascend"),m=async()=>{var a;v(!0);try{const r=await j("/api/admin/vpn");if(r.ok){const l=await r.json();I(((a=l.data)==null?void 0:a.connections)||[])}else n.error("VPN কানেকশন লোড করতে ব্যর্থ হয়েছে")}catch(r){console.error("Error fetching VPNs:",r),n.error("সার্ভারের সাথে যোগাযোগ বিচ্ছিন্ন")}finally{v(!1)}},E=B.useMemo(()=>{let a=N.filter(r=>{var t,s,w;const l=y.toLowerCase();return((t=r.name)==null?void 0:t.toLowerCase().includes(l))||((s=r.host)==null?void 0:s.toLowerCase().includes(l))||((w=r.username)==null?void 0:w.toLowerCase().includes(l))});return c&&a.sort((r,l)=>{const t=r[c]??"",s=l[c]??"";return c==="createdAt"?i==="ascend"?new Date(t).getTime()-new Date(s).getTime():new Date(s).getTime()-new Date(t).getTime():typeof t=="string"&&typeof s=="string"?i==="ascend"?t.localeCompare(s):s.localeCompare(t):typeof t=="number"&&typeof s=="number"?i==="ascend"?t-s:s-t:0}),a},[N,y,c,i]);d.useEffect(()=>{m()},[]);const W=async a=>{try{(await j("/api/admin/vpn",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify(a)})).ok?(n.success("VPN কানেকশন তৈরি হয়েছে"),u(!1),f.resetFields(),m()):n.error("VPN তৈরি করতে ব্যর্থ হয়েছে")}catch{n.error("সার্ভার ত্রুটি")}},D=async a=>{try{(await j(`/api/admin/vpn/${a}`,{method:"DELETE"})).ok?(n.success("VPN ডিলিট করা হয়েছে"),m()):n.error("ডিলিট করতে ব্যর্থ হয়েছে")}catch{n.error("সার্ভার ত্রুটি")}},A=[{title:e.jsx("span",{className:"text-[11px] uppercase tracking-wider opacity-60",children:"নাম"}),dataIndex:"name",key:"name",render:a=>e.jsx(o,{strong:!0,style:{color:"rgba(255,255,255,0.9)"},children:a})},{title:e.jsx("span",{className:"text-[11px] uppercase tracking-wider opacity-60",children:"সার্ভার হোস্ট"}),dataIndex:"host",key:"host",render:(a,r)=>e.jsxs("div",{style:{display:"flex",flexDirection:"column"},children:[e.jsx(o,{style:{color:"#60a5fa",fontFamily:"monospace",fontSize:"12px"},children:a}),e.jsxs(o,{style:{color:"rgba(255,255,255,0.2)",fontSize:"10px"},children:["পোর্ট: ",r.port]})]})},{title:e.jsx("span",{className:"text-[11px] uppercase tracking-wider opacity-60",children:"স্ট্যাটাস"}),dataIndex:"status",key:"status",render:a=>e.jsx(_,{color:a==="CONNECTED"?"green":"default",style:{borderRadius:"20px",padding:"0 12px",border:"none",background:a==="CONNECTED"?"rgba(16, 185, 129, 0.1)":"rgba(255,255,255,0.05)"},children:a||"IDLE"})},{title:e.jsx("span",{className:"text-[11px] uppercase tracking-wider opacity-60",children:"তৈরির তারিখ"}),dataIndex:"createdAt",key:"createdAt",render:a=>e.jsx("span",{style:{color:"rgba(255,255,255,0.4)",fontFamily:"monospace",fontSize:"11px"},children:a?new Date(a).toLocaleString():"N/A"})},{title:e.jsx("span",{className:"text-[11px] uppercase tracking-wider opacity-60 text-right",children:"অ্যাকশন"}),key:"actions",align:"right",render:(a,r)=>e.jsx(C,{children:e.jsx(Q,{title:"আপনি কি নিশ্চিত যে আপনি এই VPN কানেকশনটি ডিলিট করতে চান?",onConfirm:()=>r.id&&D(r.id),okText:"হ্যাঁ",cancelText:"না",overlayClassName:"dark-popconfirm",children:e.jsx(b,{type:"text",size:"small",icon:e.jsx(X,{}),danger:!0,style:{display:"flex",alignItems:"center",justifyContent:"center"}})})})}];return e.jsxs("div",{className:"admin-page",children:[e.jsxs("div",{className:"admin-header",children:[e.jsxs(x,{separator:">",style:{marginBottom:"var(--space-3)",opacity:.7},children:[e.jsxs(x.Item,{href:"",children:[e.jsx(F,{})," ড্যাশবোর্ড"]}),e.jsxs(x.Item,{children:[e.jsx(L,{})," ইনফ্রাস্ট্রাকচার"]}),e.jsx(x.Item,{children:"VPN গেটওয়ে"})]}),e.jsxs("div",{style:{display:"flex",justifyContent:"space-between",alignItems:"flex-end",flexWrap:"wrap",gap:"var(--space-4)"},children:[e.jsxs("div",{children:[e.jsxs(Z,{level:2,style:{margin:0,color:"#fff",fontWeight:800,fontSize:"var(--title-size)",letterSpacing:"-0.5px"},children:["VPN ম্যানেজমেন্ট ",e.jsx("span",{className:"admin-badge",children:"SECURE GATEWAY"})]}),e.jsx(o,{className:"admin-subtitle",children:"নিরাপদ নেটওয়ার্ক ইনফ্রাস্ট্রাকচার এবং VPN কানেকশন নিয়ন্ত্রণ করুন"})]}),e.jsx(b,{type:"primary",icon:e.jsx($,{}),onClick:()=>u(!0),className:"admin-btn-primary",style:{background:"linear-gradient(135deg, #3b82f6 0%, #2563eb 100%)",border:"none",fontWeight:600,boxShadow:"0 4px clamp(12px, 2vw, 20px) rgba(59, 130, 246, 0.3)"},children:"নতুন VPN যোগ করুন"})]})]}),e.jsxs("div",{className:"glass-panel admin-toolbar",children:[e.jsxs("div",{className:"toolbar-section",children:[e.jsx("div",{style:{background:"rgba(59, 130, 246, 0.1)",padding:"var(--space-2)",borderRadius:"var(--radius-md)",border:"1px solid rgba(59, 130, 246, 0.2)",display:"flex",alignItems:"center"},children:e.jsx(M,{style:{color:"#3b82f6",fontSize:"var(--text-base)"}})}),e.jsx(g,{placeholder:"নাম বা হোস্ট দিয়ে খুঁজুন...",value:y,onChange:a=>R(a.target.value),variant:"borderless",className:"admin-search dark-input-minimal"})]}),e.jsxs("div",{className:"toolbar-section",children:[e.jsx(S,{title:"রিফ্রেশ করুন",children:e.jsx(b,{icon:e.jsx(q,{}),onClick:m,loading:k,className:"admin-btn-icon hover-bright",style:{borderRadius:"var(--radius-md)",background:"rgba(255, 255, 255, 0.05)",border:"1px solid rgba(255, 255, 255, 0.1)",color:"#fff"}})}),e.jsx("div",{className:"toolbar-divider"}),e.jsxs(C,{size:"middle",children:[e.jsxs("div",{style:{display:"flex",alignItems:"center",gap:"var(--space-2)"},children:[e.jsx(o,{style:{color:"rgba(255,255,255,0.35)",fontSize:"var(--text-xs)",textTransform:"uppercase",letterSpacing:"1px",fontWeight:700},children:"সর্টিং"}),e.jsxs(T,{value:c,onChange:a=>P(a),style:{width:"clamp(140px, 15vw, 180px)"},className:"premium-select",dropdownClassName:"premium-dropdown",children:[e.jsx(h,{value:"name",children:"নাম"}),e.jsx(h,{value:"status",children:"স্ট্যাটাস"}),e.jsx(h,{value:"host",children:"সার্ভার হোস্ট"}),e.jsx(h,{value:"createdAt",children:"তৈরির তারিখ"})]})]}),e.jsx(S,{title:i==="ascend"?"ক্রমানুসারে":"বিপরীত ক্রমানুসারে",children:e.jsx(b,{onClick:()=>V(i==="ascend"?"descend":"ascend"),icon:i==="ascend"?e.jsx(U,{}):e.jsx(G,{}),className:"admin-btn-icon hover-bright",style:{borderRadius:"var(--radius-md)",background:"rgba(255, 255, 255, 0.05)",border:"1px solid rgba(255, 255, 255, 0.1)",color:"#fff"}})})]})]})]}),e.jsx(Y,{className:"glass-card",style:{borderRadius:"var(--radius-xl)",background:"rgba(255,255,255,0.02)",border:"1px solid rgba(255,255,255,0.08)",marginBottom:"var(--space-4)",boxShadow:"0 clamp(16px, 2.5vw, 32px) clamp(32px, 4vw, 64px) rgba(0, 0, 0, 0.3)",overflow:"hidden"},bodyStyle:{padding:0},children:e.jsx(H,{columns:A,dataSource:E,rowKey:"id",loading:k,pagination:{pageSize:10,className:"admin-pagination p-4"},size:"middle",className:"admin-table-dark"})}),e.jsx(J,{title:e.jsxs("div",{style:{display:"flex",flexDirection:"column"},children:[e.jsx("span",{style:{color:"#fff",fontWeight:800,fontSize:"var(--text-lg)"},children:"নতুন VPN কানেকশন"}),e.jsx("span",{style:{color:"rgba(255,255,255,0.2)",fontSize:"var(--text-xs)",fontWeight:700,textTransform:"uppercase"},children:"Secure Node Configuration"})]}),open:z,onCancel:()=>u(!1),onOk:()=>f.submit(),okText:"সেভ করুন",cancelText:"বাতিল",className:"admin-modal dark-modal",styles:{body:{maxWidth:"calc(100vw - var(--space-6))",width:"clamp(400px, 50vw, 600px)"}},centered:!0,okButtonProps:{style:{background:"#2563eb",border:"none",height:"40px",padding:"0 24px",borderRadius:"8px",fontWeight:700}},cancelButtonProps:{style:{background:"rgba(255,255,255,0.05)",border:"1px solid rgba(255,255,255,0.1)",color:"rgba(255,255,255,0.6)",height:"40px",padding:"0 24px",borderRadius:"8px"}},children:e.jsxs(p,{form:f,layout:"vertical",onFinish:W,style:{marginTop:24},children:[e.jsx(p.Item,{name:"name",label:e.jsx(o,{style:{color:"rgba(255,255,255,0.6)",fontSize:"12px",fontWeight:700},children:"কানেকশন নাম"}),rules:[{required:!0}],children:e.jsx(g,{placeholder:"e.g. SG-HighSpeed-01",className:"dark-input"})}),e.jsxs("div",{style:{display:"grid",gridTemplateColumns:"1fr 1fr",gap:"16px"},children:[e.jsx(p.Item,{name:"host",label:e.jsx(o,{style:{color:"rgba(255,255,255,0.6)",fontSize:"12px",fontWeight:700},children:"সার্ভার হোস্ট"}),rules:[{required:!0}],children:e.jsx(g,{placeholder:"1.2.3.4",className:"dark-input",style:{fontFamily:"monospace"}})}),e.jsx(p.Item,{name:"port",label:e.jsx(o,{style:{color:"rgba(255,255,255,0.6)",fontSize:"12px",fontWeight:700},children:"পোর্ট"}),rules:[{required:!0}],initialValue:443,children:e.jsx(K,{style:{width:"100%"},className:"dark-input"})})]}),e.jsx(p.Item,{name:"username",label:e.jsx(o,{style:{color:"rgba(255,255,255,0.6)",fontSize:"12px",fontWeight:700},children:"ইউজারনেম (অপশনাল)"}),children:e.jsx(g,{placeholder:"admin",className:"dark-input"})})]})}),e.jsx("style",{children:`
        .glass-card {
          backdrop-filter: blur(20px);
          -webkit-backdrop-filter: blur(20px);
        }
        
        .dark-input-minimal::placeholder {
          color: rgba(255,255,255,0.2) !important;
        }
        
        .toolbar-separator {
          height: 24px;
          width: 1px;
          background: rgba(255,255,255,0.08);
          margin: 0 8px;
        }

        .premium-select .ant-select-selector {
          background: rgba(255,255,255,0.05) !important;
          border: 1px solid rgba(255,255,255,0.1) !important;
          border-radius: 12px !important;
          height: 42px !important;
          display: flex !important;
          align-items: center !important;
          color: #fff !important;
        }

        .premium-dropdown {
          background: #141414 !important;
          border: 1px solid rgba(255,255,255,0.1) !important;
          border-radius: 12px !important;
        }

        .premium-dropdown .ant-select-item {
          color: rgba(255,255,255,0.65) !important;
        }

        .hover-bright:hover {
          background: rgba(255,255,255,0.1) !important;
          border-color: rgba(255,255,255,0.2) !important;
          transform: translateY(-1px);
        }

        /* Table Style Unified */
        .admin-table-dark .ant-table {
          background: transparent !important;
          color: #fff !important;
        }
        .admin-table-dark .ant-table-thead > tr > th {
          background: rgba(255,255,255,0.02) !important;
          color: rgba(255,255,255,0.4) !important;
          border-bottom: 1px solid rgba(255,255,255,0.05) !important;
          font-size: 11px !important;
          text-transform: uppercase !important;
          letter-spacing: 1px !important;
          font-weight: 700 !important;
          padding: 16px 24px !important;
        }
        .admin-table-dark .ant-table-tbody > tr > td {
          border-bottom: 1px solid rgba(255,255,255,0.02) !important;
          padding: 16px 24px !important;
        }
        .admin-table-dark .ant-table-tbody > tr:hover > td {
          background: rgba(255,255,255,0.02) !important;
        }

        .dark-input {
          background: rgba(255,255,255,0.05) !important;
          border: 1px solid rgba(255,255,255,0.1) !important;
          border-radius: 10px !important;
          color: #fff !important;
          height: 42px !important;
        }
        .dark-input:focus {
          border-color: #3b82f6 !important;
          box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.1) !important;
        }

        .dark-modal .ant-modal-content {
          background: #141414 !important;
          border: 1px solid rgba(255,255,255,0.08) !important;
          border-radius: 20px !important;
        }
        .dark-modal .ant-modal-header {
          background: transparent !important;
          border-bottom: 1px solid rgba(255,255,255,0.05) !important;
        }
      `})]})};export{oe as default};
