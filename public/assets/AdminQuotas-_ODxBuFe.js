import{j as e}from"./vendor-motion-DuGy6Wfu.js";import{r as d,a as K}from"./vendor-three-t7wuiRj2.js";import{f as g}from"./index-BecHtTlp.js";import{K as Y,N as C,C as S,x as $,a6 as G,ao as J,aV as X,j as _,r as B,T as A,e as I,o as U,D as H,p as Z,f as Q,P as L,B as h,aG as E,aW as ee,$ as te,s as c,aI as ae,ab as k,ag as re,a as se,k as oe,I as ne,g as ie,h as le}from"./vendor-antd-D0GHnncR.js";import"./vendor-react-8ayQL58t.js";import"./vendor-icons-CyxgcXk7.js";const ce=({stats:l})=>e.jsxs(Y,{gutter:[24,24],style:{marginBottom:24},children:[e.jsx(C,{xs:24,sm:8,children:e.jsx(S,{bordered:!1,className:"glass-card",style:{background:"rgba(255,255,255,0.03)",border:"1px solid rgba(255,255,255,0.1)"},children:e.jsx($,{title:e.jsx("span",{style:{color:"rgba(255,255,255,0.45)"},children:"মোট ইউজার"}),value:l.totalUsers,prefix:e.jsx(G,{style:{color:"#3b82f6"}}),valueStyle:{color:"#fff"}})})}),e.jsx(C,{xs:24,sm:8,children:e.jsx(S,{bordered:!1,className:"glass-card",style:{background:"rgba(255,255,255,0.03)",border:"1px solid rgba(255,255,255,0.1)"},children:e.jsx($,{title:e.jsx("span",{style:{color:"rgba(255,255,255,0.45)"},children:"অ্যাক্টিভ কোটা (ব্যবহারকারী)"}),value:l.activeQuotas,prefix:e.jsx(J,{style:{color:"#10b981"}}),valueStyle:{color:"#fff"}})})}),e.jsx(C,{xs:24,sm:8,children:e.jsx(S,{bordered:!1,className:"glass-card",style:{background:"rgba(255,255,255,0.03)",border:"1px solid rgba(255,255,255,0.1)"},children:e.jsx($,{title:e.jsx("span",{style:{color:"rgba(255,255,255,0.45)"},children:"কোটা লিমিট অতিক্রম"}),value:l.overLimit,prefix:e.jsx(X,{style:{color:"#ef4444"}}),valueStyle:{color:"#fff"}})})})]}),{Text:u}=A,{Option:z}=I,de=({users:l,loading:f,onReset:y,onTierUpdate:N,onDeactivate:R})=>{const T=[{title:"ইউজার",dataIndex:"email",key:"email",render:(r,o)=>e.jsxs(B,{direction:"vertical",size:0,children:[e.jsx(u,{strong:!0,style:{color:"#fff"},children:o.displayName||"No Name"}),e.jsx(u,{type:"secondary",style:{fontSize:"12px"},children:r})]})},{title:"টিয়ার ম্যানেজমেন্ট",dataIndex:"tier",key:"tier",width:150,render:(r,o)=>e.jsxs(I,{defaultValue:r,style:{width:"100%"},onChange:p=>N(o.uid,p),size:"small",dropdownStyle:{borderRadius:8,background:"#1a1a1a",border:"1px solid rgba(255,255,255,0.1)"},children:[e.jsx(z,{value:"FREE",children:e.jsx(U,{color:"default",children:"FREE"})}),e.jsx(z,{value:"PRO",children:e.jsx(U,{color:"blue",children:"PRO"})}),e.jsx(z,{value:"ADMIN",children:e.jsx(U,{color:"gold",children:"ADMIN"})})]})},{title:"API কল (ব্যবহার)",dataIndex:"currentUsage",key:"currentUsage",render:(r,o)=>{const p=Math.min(100,Math.round(r/o.monthlyQuota*100));return e.jsxs("div",{style:{width:150},children:[e.jsxs("div",{style:{display:"flex",justifyContent:"space-between",marginBottom:4},children:[e.jsx(u,{style:{fontSize:"12px",color:"rgba(255,255,255,0.85)"},children:r.toLocaleString()}),e.jsxs(u,{style:{fontSize:"12px"},type:"secondary",children:["/ ",o.monthlyQuota.toLocaleString()]})]}),e.jsx(H,{percent:p,size:"small",status:r>=o.monthlyQuota?"exception":"active",strokeColor:r>=o.monthlyQuota?"#ef4444":r>o.monthlyQuota*.8?"#f59e0b":"#10b981"})]})}},{title:"স্ট্যাটাস",dataIndex:"isActive",key:"isActive",render:r=>e.jsx(Z,{status:r?"success":"error",text:e.jsx(u,{style:{color:r?"#10b981":"#ef4444"},children:r?"Active":"Inactive"})})},{title:"অ্যাকশন",key:"actions",render:(r,o)=>e.jsxs(B,{children:[e.jsx(Q,{title:"রিসেট কোটা",children:e.jsx(L,{title:"আপনি কি নিশ্চিত যে আপনি এই ইউজারের কোটা রিসেট করতে চান?",onConfirm:()=>y(o.uid),okText:"হ্যাঁ",cancelText:"না",children:e.jsx(h,{size:"small",shape:"circle",icon:e.jsx(E,{}),style:{background:"rgba(255,255,255,0.05)",border:"none",color:"#fff"}})})}),o.isActive&&e.jsx(Q,{title:"ডিঅ্যাক্টিভ করুন",children:e.jsx(L,{title:"অ্যাকাউন্ট ডিঅ্যাক্টিভ করতে চান?",onConfirm:()=>R(o.uid),okText:"হ্যাঁ",cancelText:"না",okButtonProps:{danger:!0},children:e.jsx(h,{size:"small",shape:"circle",danger:!0,icon:e.jsx(ee,{}),style:{border:"none"}})})})]})}];return e.jsx(_,{columns:T,dataSource:l,rowKey:"uid",loading:f,pagination:{pageSize:10,showSizeChanger:!0},size:"middle",className:"admin-table-dark",style:{padding:"8px"}})},pe=({warningsCount:l})=>l===0?null:e.jsx(te,{message:"কোটা সতর্কতা",description:`${l} জন ইউজার তাদের মাসিক কোটা সীমার কাছাকাছি পৌঁছেছেন।`,type:"warning",showIcon:!0,icon:e.jsx(ae,{}),style:{marginBottom:24,borderRadius:12,background:"rgba(245, 158, 11, 0.1)",border:"1px solid rgba(245, 158, 11, 0.2)",color:"#f59e0b"},action:e.jsx(h,{size:"small",ghost:!0,onClick:()=>c.info("সতর্কতা তালিকা চেক করুন"),children:"বিস্তারিত"})}),{Title:me,Text:P}=A,{Option:w}=I,ye=()=>{const[l,f]=d.useState(!0),[y,N]=d.useState([]),[R,T]=d.useState([]),[r,o]=d.useState({totalUsers:0,activeQuotas:0,overLimit:0}),[p,O]=d.useState(""),[m,M]=d.useState("currentUsage"),[x,D]=d.useState("descend"),b=async()=>{f(!0);try{const[t,a]=await Promise.all([g("/api/accounts"),g("/api/quota/warnings")]);if(t.ok){const s=await t.json();N(s);const n=s.length,i=s.filter(v=>v.currentUsage>0).length,j=s.filter(v=>v.currentUsage>=v.monthlyQuota).length;o({totalUsers:n,activeQuotas:i,overLimit:j})}if(a.ok){const s=await a.json();T(s.warnings||[])}}catch(t){console.error("Error fetching data:",t),c.error("সার্ভারের সাথে যোগাযোগ বিচ্ছিন্ন")}finally{f(!1)}},W=K.useMemo(()=>{let t=y.filter(a=>{var n,i,j;const s=p.toLowerCase();return((n=a.displayName)==null?void 0:n.toLowerCase().includes(s))||((i=a.email)==null?void 0:i.toLowerCase().includes(s))||((j=a.uid)==null?void 0:j.toLowerCase().includes(s))});return m&&t.sort((a,s)=>{let n=a[m],i=s[m];return m==="usagePercent"&&(n=a.currentUsage/a.monthlyQuota,i=s.currentUsage/s.monthlyQuota),typeof n=="string"&&typeof i=="string"?x==="ascend"?n.localeCompare(i):i.localeCompare(n):typeof n=="number"&&typeof i=="number"?x==="ascend"?n-i:i-n:0}),t},[y,p,m,x]);d.useEffect(()=>{b()},[]);const V=async t=>{try{(await g(`/api/quota/${t}/reset`,{method:"POST"})).ok?(c.success("কোটা সফলভাবে রিসেট করা হয়েছে"),b()):c.error("কোটা রিসেট করতে ব্যর্থ হয়েছে")}catch{c.error("সার্ভার ত্রুটি")}},q=async(t,a)=>{try{(await g(`/api/accounts/${t}/tier`,{method:"PUT",headers:{"Content-Type":"application/json"},body:JSON.stringify({tier:a})})).ok?(c.success(`ইউজার টিয়ার ${a} এ আপডেট করা হয়েছে`),b()):c.error("টিয়ার আপডেট করতে ব্যর্থ হয়েছে")}catch{c.error("সার্ভার ত্রুটি")}},F=async t=>{try{(await g(`/api/accounts/${t}/deactivate`,{method:"PUT"})).ok?(c.warning("ইউজার অ্যাকাউন্ট ডিঅ্যাক্টিভ করা হয়েছে"),b()):c.error("অ্যাকাউন্ট ডিঅ্যাক্টিভ করতে ব্যর্থ হয়েছে")}catch{c.error("সার্ভার ত্রুটি")}};return e.jsxs("div",{className:"admin-page",children:[e.jsxs("div",{className:"admin-header",children:[e.jsxs(k,{separator:">",style:{marginBottom:"var(--space-2)",opacity:.7},children:[e.jsxs(k.Item,{href:"",children:[e.jsx(re,{})," ড্যাশবোর্ড"]}),e.jsxs(k.Item,{children:[e.jsx(se,{})," রিসোর্স ম্যানেজমেন্ট"]}),e.jsx(k.Item,{children:"কোটা কন্ট্রোল"})]}),e.jsxs("div",{style:{display:"flex",justifyContent:"space-between",alignItems:"flex-end",flexWrap:"wrap",gap:"var(--space-4)"},children:[e.jsxs("div",{children:[e.jsxs(me,{level:2,className:"admin-title",children:["কোটা ম্যানেজমেন্ট ",e.jsx("span",{className:"admin-badge",children:"SYSTEM LIMITS"})]}),e.jsx(P,{className:"admin-subtitle",children:"ইউজারদের রিসোর্স ব্যবহার এবং লিমিটেশন নিয়ন্ত্রণ করুন"})]}),e.jsx(h,{type:"primary",icon:e.jsx(E,{}),onClick:b,loading:l,className:"admin-btn-primary",style:{background:"linear-gradient(135deg, #3b82f6 0%, #2563eb 100%)",border:"none",fontWeight:600,boxShadow:"0 4px clamp(12px, 2vw, 20px) rgba(59, 130, 246, 0.3)"},children:"রিফ্রেশ ডাটা"})]})]}),e.jsx(pe,{warningsCount:R.length}),e.jsxs("div",{className:"admin-toolbar",children:[e.jsxs("div",{className:"toolbar-section",children:[e.jsx("div",{style:{background:"rgba(59, 130, 246, 0.1)",padding:"var(--space-2)",borderRadius:"var(--radius-md)",border:"1px solid rgba(59, 130, 246, 0.2)",display:"flex",alignItems:"center"},children:e.jsx(oe,{style:{color:"#3b82f6",fontSize:"var(--text-base)"}})}),e.jsx(ne,{placeholder:"ইউজার ইমেইল বা নাম দিয়ে খুঁজুন...",value:p,onChange:t=>O(t.target.value),variant:"borderless",className:"admin-search dark-input-minimal"})]}),e.jsxs("div",{className:"toolbar-section",children:[e.jsxs("div",{style:{display:"flex",alignItems:"center",gap:"var(--space-2)"},children:[e.jsx(P,{style:{color:"rgba(255,255,255,0.35)",fontSize:"var(--text-xs)",textTransform:"uppercase",letterSpacing:"1px",fontWeight:700},children:"সর্ট"}),e.jsxs(I,{value:m,onChange:t=>M(t),style:{width:160},className:"premium-select",dropdownClassName:"premium-dropdown",children:[e.jsx(w,{value:"currentUsage",children:"ব্যবহার"}),e.jsx(w,{value:"monthlyQuota",children:"কোটা"}),e.jsx(w,{value:"displayName",children:"নাম"}),e.jsx(w,{value:"tier",children:"টিয়ার"})]})]}),e.jsx(Q,{title:x==="ascend"?"ক্রমানুসারে":"বিপরীত ক্রমানুসারে",children:e.jsx(h,{onClick:()=>D(x==="ascend"?"descend":"ascend"),icon:x==="ascend"?e.jsx(ie,{}):e.jsx(le,{}),className:"admin-btn-icon",style:{borderRadius:"var(--radius-md)",background:"rgba(255, 255, 255, 0.05)",border:"1px solid rgba(255, 255, 255, 0.1)",color:"#fff"}})})]})]}),e.jsx(ce,{stats:r}),e.jsx(S,{className:"glass-card",style:{borderRadius:"var(--radius-xl)",background:"rgba(255,255,255,0.02)",border:"1px solid rgba(255,255,255,0.08)",marginBottom:"var(--space-4)",overflow:"hidden"},bodyStyle:{padding:0},children:e.jsx("div",{style:{overflowX:"auto"},children:e.jsx(de,{users:W,loading:l,onReset:V,onTierUpdate:q,onDeactivate:F})})}),e.jsx("style",{children:`
        .glass-card {
          backdrop-filter: blur(20px);
          -webkit-backdrop-filter: blur(20px);
        }
        
        .main-quota-card .ant-card-body {
          background: linear-gradient(180deg, rgba(255,255,255,0.01) 0%, rgba(255,255,255,0) 100%);
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
          transition: all 0.3s ease !important;
        }

        .premium-select:hover .ant-select-selector {
          border-color: rgba(59, 130, 246, 0.5) !important;
          background: rgba(255,255,255,0.08) !important;
        }

        .premium-select .ant-select-selection-item {
          color: #fff !important;
          font-weight: 500 !important;
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
          transition: all 0.2s ease !important;
        }

        .premium-dropdown .ant-select-item-option-selected {
          background: rgba(59, 130, 246, 0.15) !important;
          color: #3b82f6 !important;
        }

        .premium-dropdown .ant-select-item-option-active {
          background: rgba(255,255,255,0.05) !important;
          color: #fff !important;
        }

        .hover-bright:hover {
          background: rgba(255,255,255,0.1) !important;
          border-color: rgba(255,255,255,0.2) !important;
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

        .admin-table-dark .ant-pagination-item-active a {
          color: #fff !important;
        }
      `})]})};export{ye as default};
