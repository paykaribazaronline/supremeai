import{j as e}from"./vendor-charts-DNQHmZIt.js";import{r as n}from"./vendor-i18n-CqRa-TkV.js";import{A as O}from"./AdminLayout-BnwaPKrj.js";import{a as d}from"./index-Dsj7lm7a.js";import{a9 as l,k as g,r as B,q as z,T as D,B as I,as as V,aj as A,ak as s,Q as c,aB as S,a as J,h as Y,J as q,ai as C,$ as N,a2 as Z,a5 as _,aK as w,Z as m,d as M,b as T,P as G,aZ as H}from"./vendor-antd-Ds3tdPxY.js";import"./vendor-three-DJcAVArf.js";import"./vendor-react-B41qB032.js";const{Title:X,Text:h}=D,de=()=>{const[u,x]=n.useState(!1),[v,$]=n.useState([]),[R,K]=n.useState([]),[Q,b]=n.useState(!1),[y,W]=n.useState(null),[j]=l.useForm(),p=async()=>{x(!0);try{const[t,a]=await Promise.all([d.fetchWithAuth("/api/admin/quotas/config"),d.fetchWithAuth("/api/admin/quotas/usage")]),f=await t.json(),r=await a.json();if(f.success){const i=f.data,k=Object.keys(i.tierQuotas).map(o=>({tier:o,quota:i.tierQuotas[o],maxApis:i.tierMaxApis[o]||0,maxSimulator:i.tierMaxSimulatorInstalls[o]||0}));$(k)}r.success&&K(r.data||[])}catch(t){console.error("Failed to fetch quota data:",t),m.error("কোটা ডাটা লোড করতে সমস্যা হয়েছে")}finally{x(!1)}};n.useEffect(()=>{p()},[]);const E=t=>{W(t),j.setFieldsValue(t),b(!0)},P=async t=>{x(!0);try{const r=(await(await d.fetchWithAuth("/api/admin/quotas/config")).json()).data,i={...r.tierQuotas,[t.tier]:t.quota},k={...r.tierMaxApis,[t.tier]:t.maxApis},o={...r.tierMaxSimulatorInstalls,[t.tier]:t.maxSimulator};(await(await d.fetchWithAuth("/api/admin/quotas/config",{method:"POST",body:JSON.stringify({tierQuotas:i,tierMaxApis:k,tierMaxSimulatorInstalls:o})})).json()).success&&(m.success(`${t.tier} টিয়ার আপডেট করা হয়েছে`),b(!1),p())}catch{m.error("আপডেট করতে ব্যর্থ হয়েছে")}finally{x(!1)}},U=async t=>{try{(await(await d.fetchWithAuth(`/api/admin/quotas/reset/${t}`,{method:"POST"})).json()).success&&(m.success("ইউসেজ রিসেট করা হয়েছে"),p())}catch{m.error("রিসেট করতে ব্যর্থ হয়েছে")}},L=[{title:"ইউজার টিয়ার",dataIndex:"tier",key:"tier",render:t=>e.jsx(M,{color:t==="ADMIN"?"gold":"blue",style:{fontWeight:700,borderRadius:6},children:t})},{title:"মাসিক কোটা (AI Calls)",dataIndex:"quota",key:"quota",render:t=>t===-1?"Unlimited":t.toLocaleString()},{title:"সর্বোচ্চ API Key",dataIndex:"maxApis",key:"maxApis"},{title:"সিমুলেটর লিমিট",dataIndex:"maxSimulator",key:"maxSimulator"},{title:"অ্যাকশন",key:"action",render:(t,a)=>e.jsx(I,{type:"text",icon:e.jsx(N,{}),onClick:()=>E(a),style:{color:"#3b82f6"},children:"সম্পাদনা"})}],F=[{title:"ইউজার/API Key",dataIndex:"apiKey",key:"apiKey",render:(t,a)=>e.jsxs(T,{direction:"vertical",size:0,children:[e.jsx(h,{strong:!0,style:{color:"#fff"},children:a.userId}),e.jsx(h,{type:"secondary",style:{fontSize:10,fontFamily:"monospace"},children:t})]})},{title:"ব্যবহৃত (Usage)",dataIndex:"requestCount",key:"requestCount",render:t=>e.jsxs(T,{children:[e.jsx(G,{percent:Math.min(100,t/100*100),size:"small",showInfo:!1,strokeColor:t>80?"#ef4444":"#10b981",style:{width:50}}),e.jsx(h,{style:{color:"#fff"},children:t.toLocaleString()})]})},{title:"অবস্থা",dataIndex:"status",key:"status",render:t=>e.jsx(M,{color:t==="active"?"success":"error",children:t.toUpperCase()})},{title:"শেষ ব্যবহার",dataIndex:"lastUsed",key:"lastUsed",render:t=>t?new Date(t).toLocaleString("bn-BD"):"কখনো না"},{title:"অ্যাকশন",key:"action",render:(t,a)=>e.jsx(I,{type:"text",danger:!0,icon:e.jsx(H,{}),onClick:()=>U(a.apiKey),children:"রিসেট"})}];return e.jsxs(O,{title:"Quota Management",children:[e.jsxs("div",{className:"admin-header",children:[e.jsxs(g,{separator:">",style:{marginBottom:"var(--space-2)",opacity:.7},children:[e.jsxs(g.Item,{href:"",children:[e.jsx(B,{})," ড্যাশবোর্ড"]}),e.jsxs(g.Item,{children:[e.jsx(z,{})," সিস্টেম কন্ট্রোল"]}),e.jsx(g.Item,{children:"কোটা ম্যানেজমেন্ট"})]}),e.jsxs("div",{style:{display:"flex",justifyContent:"space-between",alignItems:"flex-end",flexWrap:"wrap",gap:"var(--space-4)"},children:[e.jsxs("div",{children:[e.jsxs(X,{level:2,className:"admin-title",children:["কোটা ও লিমিট ম্যানেজমেন্ট ",e.jsx("span",{className:"admin-badge",children:"SYSTEM CONTROLS"})]}),e.jsx(h,{className:"admin-subtitle",children:"ব্যবহারকারীর টিয়ার ভিত্তিক সীমাবদ্ধতা এবং লাইভ ইউসেজ মনিটরিং"})]}),e.jsx(I,{type:"primary",icon:e.jsx(V,{}),onClick:p,loading:u,className:"admin-btn-primary",style:{background:"linear-gradient(135deg, #10b981 0%, #059669 100%)",border:"none"},children:"রিফ্রেশ করুন"})]})]}),e.jsxs(A,{gutter:[24,24],children:[e.jsx(s,{xs:24,lg:8,children:e.jsx(c,{className:"glass-card stat-card",style:{background:"linear-gradient(135deg, rgba(59,130,246,0.1) 0%, rgba(37,99,235,0.05) 100%)"},children:e.jsx(S,{title:e.jsx("span",{style:{color:"rgba(255,255,255,0.45)"},children:"মোট API ইউজার"}),value:R.length,prefix:e.jsx(J,{style:{color:"#3b82f6"}}),valueStyle:{color:"#fff",fontWeight:800}})})}),e.jsx(s,{xs:24,lg:8,children:e.jsx(c,{className:"glass-card stat-card",style:{background:"linear-gradient(135deg, rgba(16,185,129,0.1) 0%, rgba(5,150,105,0.05) 100%)"},children:e.jsx(S,{title:e.jsx("span",{style:{color:"rgba(255,255,255,0.45)"},children:"সিস্টেম হেলথ"}),value:98.4,precision:1,suffix:"%",prefix:e.jsx(Y,{style:{color:"#10b981"}}),valueStyle:{color:"#fff",fontWeight:800}})})}),e.jsx(s,{xs:24,lg:8,children:e.jsx(c,{className:"glass-card stat-card",style:{background:"linear-gradient(135deg, rgba(245,158,11,0.1) 0%, rgba(217,119,6,0.05) 100%)"},children:e.jsx(S,{title:e.jsx("span",{style:{color:"rgba(255,255,255,0.45)"},children:"গড় ইউসেজ"}),value:124,prefix:e.jsx(q,{style:{color:"#f59e0b"}}),valueStyle:{color:"#fff",fontWeight:800}})})}),e.jsx(s,{xs:24,children:e.jsx(c,{title:e.jsxs("span",{style:{color:"#fff"},children:[e.jsx(N,{})," টিয়ার কনফিগারেশন"]}),className:"glass-card",bodyStyle:{padding:0},children:e.jsx(C,{dataSource:v,columns:L,pagination:!1,loading:u,className:"admin-table-dark",rowKey:"tier"})})}),e.jsx(s,{xs:24,children:e.jsx(c,{title:e.jsxs("span",{style:{color:"#fff"},children:[e.jsx(q,{})," লাইভ ইউসেজ ট্র্যাকার"]}),className:"glass-card",bodyStyle:{padding:0},children:e.jsx(C,{dataSource:R,columns:F,pagination:{pageSize:10},loading:u,className:"admin-table-dark",rowKey:"apiKey"})})})]}),e.jsx(Z,{title:`Edit Quota: ${y==null?void 0:y.tier}`,visible:Q,onCancel:()=>b(!1),onOk:()=>j.submit(),confirmLoading:u,className:"premium-modal",okText:"আপডেট করুন",cancelText:"বাতিল",children:e.jsxs(l,{form:j,layout:"vertical",onFinish:P,children:[e.jsx(l.Item,{name:"tier",hidden:!0,children:e.jsx(_,{})}),e.jsx(l.Item,{name:"quota",label:"মাসিক কোটা (AI Calls)",rules:[{required:!0}],extra:"-১ মানে অসীম বা আনলিমিটেড",children:e.jsx(w,{style:{width:"100%"}})}),e.jsxs(A,{gutter:16,children:[e.jsx(s,{span:12,children:e.jsx(l.Item,{name:"maxApis",label:"সর্বোচ্চ API Keys",rules:[{required:!0}],children:e.jsx(w,{style:{width:"100%"}})})}),e.jsx(s,{span:12,children:e.jsx(l.Item,{name:"maxSimulator",label:"সিমুলেটর লিমিট",rules:[{required:!0}],children:e.jsx(w,{style:{width:"100%"}})})})]})]})}),e.jsx("style",{children:`
        .glass-card {
          backdrop-filter: blur(20px);
          -webkit-backdrop-filter: blur(20px);
          border-radius: 20px;
          background: rgba(255, 255, 255, 0.02);
          border: 1px solid rgba(255, 255, 255, 0.08);
          box-shadow: 0 15px 35px rgba(0,0,0,0.2);
        }

        .stat-card {
          padding: 10px;
          transition: transform 0.3s ease;
        }

        .stat-card:hover {
          transform: translateY(-5px);
        }

        .admin-table-dark .ant-table {
          background: transparent !important;
        }

        .admin-table-dark .ant-table-thead > tr > th {
          background: rgba(255,255,255,0.02) !important;
          color: rgba(255,255,255,0.45) !important;
          border-bottom: 1px solid rgba(255,255,255,0.05) !important;
          font-size: 11px !important;
          text-transform: uppercase !important;
          letter-spacing: 1px !important;
          font-weight: 700 !important;
        }

        .admin-table-dark .ant-table-tbody > tr > td {
          border-bottom: 1px solid rgba(255,255,255,0.03) !important;
          color: rgba(255,255,255,0.85) !important;
        }

        .admin-table-dark .ant-table-tbody > tr:hover > td {
          background: rgba(255,255,255,0.03) !important;
        }

        .premium-modal .ant-modal-content {
          background: #141414 !important;
          border: 1px solid rgba(255,255,255,0.1);
          border-radius: 24px;
        }

        .premium-modal .ant-modal-header {
          background: transparent !important;
          border-bottom: 1px solid rgba(255,255,255,0.05);
        }

        .premium-modal .ant-modal-title {
          color: #fff !important;
        }

        .ant-form-item-label > label {
          color: rgba(255,255,255,0.65) !important;
        }

        .ant-input-number {
          background: rgba(255,255,255,0.05) !important;
          border: 1px solid rgba(255,255,255,0.1) !important;
          color: #fff !important;
          border-radius: 12px;
        }
      `})]})};export{de as default};
