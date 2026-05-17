import{j as t}from"./vendor-motion-DuGy6Wfu.js";import{r as s,a as B}from"./vendor-three-t7wuiRj2.js";import{A as z}from"./AdminLayout-l2e4v5VS.js";import{a as D}from"./index-BecHtTlp.js";import{ab as b,ag as W,Q as O,T as F,B as h,aG as $,$ as U,C as G,k as P,I as M,e as f,f as V,g as K,h as Q,j as X,s as Y,o as S}from"./vendor-antd-D0GHnncR.js";import"./vendor-react-8ayQL58t.js";import"./vendor-icons-CyxgcXk7.js";const{Option:i}=f,{Title:q,Text:c}=F,et=()=>{const[y,R]=s.useState([]),[v,k]=s.useState(!1),[j,w]=s.useState(null),[g,N]=s.useState(""),[x,C]=s.useState(""),[d,T]=s.useState("timestamp"),[l,A]=s.useState("descend"),u=async()=>{var a;k(!0),w(null);try{const r=await D.fetchWithAuth("/api/logs?severity="+x);if(!r.ok)throw new Error("Failed to fetch logs");const e=await r.json();console.log("[AdminLogs] API Result:",e);const o=((a=e.data)==null?void 0:a.logs)||(Array.isArray(e.data)?e.data:[]);R(o)}catch(r){w(r instanceof Error?r.message:"Failed to fetch logs"),Y.error("লগ ডাটা লোড করতে সমস্যা হয়েছে")}finally{k(!1)}},I={ERROR:4,WARN:3,INFO:2,DEBUG:1},L=B.useMemo(()=>{let a=y.filter(r=>{var o,n,p,m;const e=g.toLowerCase();return((o=r.user)==null?void 0:o.toLowerCase().includes(e))||((n=r.action)==null?void 0:n.toLowerCase().includes(e))||((p=r.category)==null?void 0:p.toLowerCase().includes(e))||((m=r.outcome)==null?void 0:m.toLowerCase().includes(e))});return d&&a.sort((r,e)=>{let o=r[d]??"",n=e[d]??"";if(d==="severity"){const p=I[r.severity]||0,m=I[e.severity]||0;return l==="ascend"?p-m:m-p}return d==="timestamp"?l==="ascend"?new Date(o).getTime()-new Date(n).getTime():new Date(n).getTime()-new Date(o).getTime():typeof o=="string"&&typeof n=="string"?l==="ascend"?o.localeCompare(n):n.localeCompare(o):0}),a},[y,g,d,l]);s.useEffect(()=>{u()},[x]);const E=[{title:"সময়কাল (Timestamp)",dataIndex:"timestamp",key:"timestamp",width:220,render:a=>t.jsx(c,{style:{color:"rgba(255,255,255,0.65)",fontFamily:"monospace"},children:a})},{title:"ইউজার",dataIndex:"user",key:"user",render:a=>t.jsx(c,{strong:!0,style:{color:"#fff"},children:a})},{title:"অ্যাকশন",dataIndex:"action",key:"action"},{title:"ক্যাটাগরি",dataIndex:"category",key:"category",render:a=>t.jsx(S,{style:{background:"rgba(255,255,255,0.05)",border:"1px solid rgba(255,255,255,0.1)",color:"rgba(255,255,255,0.45)"},children:a})},{title:"সেভেরিটি",dataIndex:"severity",key:"severity",render:a=>{let r="#3b82f6",e="rgba(59, 130, 246, 0.1)";return a==="ERROR"&&(r="#ef4444",e="rgba(239, 68, 68, 0.1)"),a==="WARN"&&(r="#f59e0b",e="rgba(245, 158, 11, 0.1)"),a==="DEBUG"&&(r="#10b981",e="rgba(16, 185, 129, 0.1)"),t.jsx(S,{style:{background:e,border:`1px solid ${r}44`,color:r,fontWeight:600},children:a})}},{title:"ফলাফল",dataIndex:"outcome",key:"outcome"}];return t.jsxs(z,{title:"System Activity Logs",children:[t.jsxs("div",{className:"admin-header",children:[t.jsxs(b,{separator:">",style:{marginBottom:"var(--space-2)",opacity:.7},children:[t.jsxs(b.Item,{href:"",children:[t.jsx(W,{})," ড্যাশবোর্ড"]}),t.jsxs(b.Item,{children:[t.jsx(O,{})," সিস্টেম অডিট"]}),t.jsx(b.Item,{children:"অ্যাক্টিভিটি লগস"})]}),t.jsxs("div",{style:{display:"flex",justifyContent:"space-between",alignItems:"flex-end",flexWrap:"wrap",gap:"var(--space-4)"},children:[t.jsxs("div",{children:[t.jsxs(q,{level:2,className:"admin-title",children:["সিস্টেম অ্যাক্টিভিটি লগস ",t.jsx("span",{className:"admin-badge",children:"LIVE AUDIT"})]}),t.jsx(c,{className:"admin-subtitle",children:"অডিট ট্রেইল এবং সিস্টেম ইভেন্ট রিয়েল-টাইম মনিটরিং"})]}),t.jsx(h,{type:"primary",icon:t.jsx($,{}),onClick:u,loading:v,className:"admin-btn-primary",style:{background:"linear-gradient(135deg, #3b82f6 0%, #2563eb 100%)",border:"none",fontWeight:600,boxShadow:"0 4px clamp(12px, 2vw, 20px) rgba(59, 130, 246, 0.3)"},children:"রিফ্রেশ করুন"})]})]}),j&&t.jsx(U,{type:"error",message:j,action:t.jsx(h,{onClick:u,children:"Retry"}),className:"admin-empty",style:{marginBottom:"var(--space-3)"}}),t.jsxs(G,{className:"glass-card",style:{borderRadius:"var(--radius-xl)",background:"rgba(255,255,255,0.02)",border:"1px solid rgba(255,255,255,0.08)",marginBottom:"var(--space-4)",boxShadow:"0 clamp(16px, 2.5vw, 32px) clamp(32px, 4vw, 64px) rgba(0, 0, 0, 0.3)",overflow:"hidden"},bodyStyle:{padding:0},children:[t.jsxs("div",{className:"admin-toolbar",children:[t.jsxs("div",{className:"toolbar-section",children:[t.jsx("div",{style:{background:"rgba(59, 130, 246, 0.1)",padding:"var(--space-2)",borderRadius:"var(--radius-md)",border:"1px solid rgba(59, 130, 246, 0.2)",display:"flex",alignItems:"center"},children:t.jsx(P,{style:{color:"#3b82f6",fontSize:"var(--text-base)"}})}),t.jsx(M,{placeholder:"ইউজার বা অ্যাকশন দিয়ে খুঁজুন...",value:g,onChange:a=>N(a.target.value),variant:"borderless",className:"admin-search dark-input-minimal"})]}),t.jsxs("div",{className:"toolbar-section",children:[t.jsxs("div",{style:{display:"flex",alignItems:"center",gap:"var(--space-2)"},children:[t.jsx(c,{style:{color:"rgba(255,255,255,0.35)",fontSize:"var(--text-xs)",textTransform:"uppercase",letterSpacing:"1px",fontWeight:700},children:"সেভেরিটি"}),t.jsxs(f,{placeholder:"সবগুলো",value:x||void 0,onChange:a=>C(a||""),style:{width:140},allowClear:!0,className:"premium-select",dropdownClassName:"premium-dropdown",children:[t.jsx(i,{value:"INFO",children:"ইনফো (Info)"}),t.jsx(i,{value:"WARN",children:"ওয়ার্নিং (Warn)"}),t.jsx(i,{value:"ERROR",children:"এরর (Error)"}),t.jsx(i,{value:"DEBUG",children:"ডিবাগ (Debug)"})]})]}),t.jsxs("div",{style:{display:"flex",alignItems:"center",gap:"var(--space-2)"},children:[t.jsx(c,{style:{color:"rgba(255,255,255,0.35)",fontSize:"var(--text-xs)",textTransform:"uppercase",letterSpacing:"1px",fontWeight:700},children:"সর্ট করুন"}),t.jsxs(f,{value:d,onChange:a=>T(a),style:{width:160},className:"premium-select",dropdownClassName:"premium-dropdown",children:[t.jsx(i,{value:"timestamp",children:"সময় (Time)"}),t.jsx(i,{value:"severity",children:"সেভেরিটি"}),t.jsx(i,{value:"user",children:"ইউজার"}),t.jsx(i,{value:"action",children:"অ্যাকশন"}),t.jsx(i,{value:"category",children:"ক্যাটাগরি"})]})]}),t.jsx(V,{title:l==="ascend"?"ক্রমানুসারে":"বিপরীত ক্রমানুসারে",children:t.jsx(h,{onClick:()=>A(l==="ascend"?"descend":"ascend"),icon:l==="ascend"?t.jsx(K,{}):t.jsx(Q,{}),className:"admin-btn-icon hover-bright",style:{borderRadius:"var(--radius-md)",background:"rgba(255, 255, 255, 0.05)",border:"1px solid rgba(255, 255, 255, 0.1)",color:"#fff"}})})]})]}),t.jsx("div",{style:{overflowX:"auto"},children:t.jsx(X,{columns:E,dataSource:L,rowKey:a=>a.id||a.timestamp||Math.random().toString(),loading:v,pagination:{pageSize:15,showSizeChanger:!0,pageSizeOptions:["15","30","50","100"]},className:"admin-table-dark"})})]}),t.jsx("style",{children:`
        .glass-card {
          backdrop-filter: blur(20px);
          -webkit-backdrop-filter: blur(20px);
        }
        
        .dark-input-minimal::placeholder {
          color: rgba(255,255,255,0.2) !important;
        }

        .premium-select .ant-select-selector {
          background: rgba(255,255,255,0.05) !important;
          border: 1px solid rgba(255,255,255,0.1) !important;
          border-radius: 12px !important;
          height: 42px !important;
          display: flex !important;
          align-items: center !important;
          color: #fff !important;
          transition: all 0.3s ease !important;
        }

        .premium-select:hover .ant-select-selector {
          border-color: rgba(59, 130, 246, 0.5) !important;
          background: rgba(255,255,255,0.08) !important;
        }

        .premium-dropdown {
          background: #141414 !important;
          border: 1px solid rgba(255,255,255,0.1) !important;
          border-radius: 12px !important;
          padding: 8px !important;
          box-shadow: 0 10px 30px rgba(0,0,0,0.5) !important;
        }

        .premium-dropdown .ant-select-item {
          border-radius: 8px !important;
          padding: 8px 12px !important;
        }

        .premium-dropdown .ant-select-item-option-selected {
          background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%) !important;
          color: white !important;
          font-weight: 600;
        }

        .premium-dropdown .ant-select-item-option-active:not(.ant-select-item-option-selected) {
          background: rgba(255,255,255,0.05) !important;
        }

        .admin-table-dark .ant-table {
          background: transparent !important;
        }

        .admin-table-dark .ant-table-thead > tr > th {
          background: rgba(255,255,255,0.02) !important;
          color: rgba(255,255,255,0.4) !important;
          border-bottom: 1px solid rgba(255,255,255,0.05) !important;
          font-size: var(--text-xs) !important;
          text-transform: uppercase !important;
          letter-spacing: 0.5px !important;
          font-weight: 700 !important;
          padding: var(--space-3) !important;
        }

        .admin-table-dark .ant-table-tbody > tr > td {
          border-bottom: 1px solid rgba(255,255,255,0.03) !important;
          padding: var(--space-3) !important;
          color: rgba(255,255,255,0.85) !important;
        }

        .admin-table-dark .ant-table-tbody > tr:hover > td {
          background: rgba(255,255,255,0.02) !important;
        }

        .admin-table-dark .ant-pagination-item {
          background: rgba(255,255,255,0.05) !important;
          border: 1px solid rgba(255,255,255,0.1) !important;
          border-radius: var(--radius-md) !important;
        }

        .admin-table-dark .ant-pagination-item-active {
          background: var(--neon-blue) !important;
          border-color: var(--neon-blue) !important;
        }

        /* Admin button fixes */
        .admin-btn-primary {
          height: clamp(36px, 5vh, 48px) !important;
          padding: 0 clamp(16px, 2vw, 24px) !important;
          font-size: var(--text-sm) !important;
          border-radius: var(--radius-md) !important;
        }

        .admin-btn-icon {
          height: clamp(36px, 5vh, 48px) !important;
          width: clamp(36px, 5vh, 48px) !important;
          border-radius: var(--radius-md) !important;
          display: flex !important;
          align-items: center !important;
          justify-content: center !important;
          padding: 0 !important;
        }

        .glass-card {
          backdrop-filter: blur(20px);
          -webkit-backdrop-filter: blur(20px);
        }
        
        .dark-input-minimal::placeholder {
          color: rgba(255,255,255,0.2) !important;
        }

        .premium-select .ant-select-selector {
          background: rgba(255,255,255,0.05) !important;
          border: 1px solid rgba(255,255,255,0.1) !important;
          border-radius: 12px !important;
          height: 42px !important;
          display: flex !important;
          align-items: center !important;
          color: #fff !important;
          transition: all 0.3s ease !important;
        }

        .premium-select:hover .ant-select-selector {
          border-color: rgba(59, 130, 246, 0.5) !important;
          background: rgba(255,255,255,0.08) !important;
        }

        .premium-dropdown {
          background: #141414 !important;
          border: 1px solid rgba(255,255,255,0.1) !important;
          border-radius: 12px !important;
          padding: 8px !important;
          box-shadow: 0 10px 30px rgba(0,0,0,0.5) !important;
        }

        .premium-dropdown .ant-select-item {
          color: rgba(255,255,255,0.65) !important;
          border-radius: 8px !important;
          margin-bottom: 4px !important;
        }

        .premium-dropdown .ant-select-item-option-selected {
          background: rgba(59, 130, 246, 0.15) !important;
          color: #3b82f6 !important;
        }

        .hover-bright:hover {
          background: rgba(255,255,255,0.1) !important;
          transform: translateY(-1px);
        }

        /* Table Customizations */
        .admin-table-dark .ant-table {
          background: transparent !important;
        }

        .admin-table-dark .ant-table-thead > tr > th {
          background: rgba(255,255,255,0.02) !important;
          color: rgba(255,255,255,0.4) !important;
          border-bottom: 1px solid rgba(255,255,255,0.05) !important;
          font-size: 12px !important;
          text-transform: uppercase !important;
          letter-spacing: 1px !important;
          font-weight: 700 !important;
          padding: 16px 24px !important;
        }

        .admin-table-dark .ant-table-tbody > tr > td {
          border-bottom: 1px solid rgba(255,255,255,0.03) !important;
          padding: 16px 24px !important;
          color: rgba(255,255,255,0.85) !important;
        }

        .admin-table-dark .ant-table-tbody > tr:hover > td {
          background: rgba(255,255,255,0.02) !important;
        }

        .admin-table-dark .ant-pagination-item {
          background: rgba(255,255,255,0.05) !important;
          border: 1px solid rgba(255,255,255,0.1) !important;
          border-radius: 8px !important;
        }

        .admin-table-dark .ant-pagination-item-active {
          background: #3b82f6 !important;
          border-color: #3b82f6 !important;
        }

        .admin-table-dark .ant-pagination-item a {
          color: rgba(255,255,255,0.65) !important;
        }
      `})]})};export{et as default};
