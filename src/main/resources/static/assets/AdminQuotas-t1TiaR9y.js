import{j as e}from"./vendor-charts-ncBmr_CS.js";import{r as n}from"./vendor-three-DwQ_Ww00.js";import{A as O}from"./AdminLayout-BvqF5Eb9.js";import{a as d}from"./index-DK8Itq80.js";import{F as l,aj as g,ac as z,an as B,T as D,B as k,aK as V,K as A,N as s,C as c,x as S,a9 as Y,d as _,ab as C,j as N,t as q,M as J,I as G,aS as w,s as m,o as M,r as T,D as H,b1 as X}from"./vendor-antd-DhVej3TD.js";import"./vendor-react-DdkrXTat.js";const{Title:Z,Text:b}=D,le=()=>{const[u,x]=n.useState(!1),[v,K]=n.useState([]),[R,W]=n.useState([]),[$,h]=n.useState(!1),[y,E]=n.useState(null),[j]=l.useForm(),p=async()=>{x(!0);try{const[t,a]=await Promise.all([d.fetchWithAuth("/api/admin/quotas/config"),d.fetchWithAuth("/api/admin/quotas/usage")]),f=await t.json(),r=await a.json();if(f.success){const i=f.data,I=Object.keys(i.tierQuotas).map(o=>({tier:o,quota:i.tierQuotas[o],maxApis:i.tierMaxApis[o]||0,maxSimulator:i.tierMaxSimulatorInstalls[o]||0}));K(I)}r.success&&W(r.data||[])}catch(t){console.error("Failed to fetch quota data:",t),m.error("কোটা ডাটা লোড করতে সমস্যা হয়েছে")}finally{x(!1)}};n.useEffect(()=>{p()},[]);const Q=t=>{E(t),j.setFieldsValue(t),h(!0)},U=async t=>{x(!0);try{const r=(await(await d.fetchWithAuth("/api/admin/quotas/config")).json()).data,i={...r.tierQuotas,[t.tier]:t.quota},I={...r.tierMaxApis,[t.tier]:t.maxApis},o={...r.tierMaxSimulatorInstalls,[t.tier]:t.maxSimulator};(await(await d.fetchWithAuth("/api/admin/quotas/config",{method:"POST",body:JSON.stringify({tierQuotas:i,tierMaxApis:I,tierMaxSimulatorInstalls:o})})).json()).success&&(m.success(`${t.tier} টিয়ার আপডেট করা হয়েছে`),h(!1),p())}catch{m.error("আপডেট করতে ব্যর্থ হয়েছে")}finally{x(!1)}},F=async t=>{try{(await(await d.fetchWithAuth(`/api/admin/quotas/reset/${t}`,{method:"POST"})).json()).success&&(m.success("ইউসেজ রিসেট করা হয়েছে"),p())}catch{m.error("রিসেট করতে ব্যর্থ হয়েছে")}},L=[{title:"ইউজার টিয়ার",dataIndex:"tier",key:"tier",render:t=>e.jsx(M,{color:t==="ADMIN"?"gold":"blue",style:{fontWeight:700,borderRadius:6},children:t})},{title:"মাসিক কোটা (AI Calls)",dataIndex:"quota",key:"quota",render:t=>t===-1?"Unlimited":t.toLocaleString()},{title:"সর্বোচ্চ API Key",dataIndex:"maxApis",key:"maxApis"},{title:"সিমুলেটর লিমিট",dataIndex:"maxSimulator",key:"maxSimulator"},{title:"অ্যাকশন",key:"action",render:(t,a)=>e.jsx(k,{type:"text",icon:e.jsx(q,{}),onClick:()=>Q(a),style:{color:"#3b82f6"},children:"সম্পাদনা"})}],P=[{title:"ইউজার/API Key",dataIndex:"apiKey",key:"apiKey",render:(t,a)=>e.jsxs(T,{direction:"vertical",size:0,children:[e.jsx(b,{strong:!0,style:{color:"#fff"},children:a.userId}),e.jsx(b,{type:"secondary",style:{fontSize:10,fontFamily:"monospace"},children:t})]})},{title:"ব্যবহৃত (Usage)",dataIndex:"requestCount",key:"requestCount",render:t=>e.jsxs(T,{children:[e.jsx(H,{percent:Math.min(100,t/100*100),size:"small",showInfo:!1,strokeColor:t>80?"#ef4444":"#10b981",style:{width:50}}),e.jsx(b,{style:{color:"#fff"},children:t.toLocaleString()})]})},{title:"অবস্থা",dataIndex:"status",key:"status",render:t=>e.jsx(M,{color:t==="active"?"success":"error",children:t.toUpperCase()})},{title:"শেষ ব্যবহার",dataIndex:"lastUsed",key:"lastUsed",render:t=>t?new Date(t).toLocaleString("bn-BD"):"কখনো না"},{title:"অ্যাকশন",key:"action",render:(t,a)=>e.jsx(k,{type:"text",danger:!0,icon:e.jsx(X,{}),onClick:()=>F(a.apiKey),children:"রিসেট"})}];return e.jsxs(O,{title:"Quota Management",children:[e.jsxs("div",{className:"admin-header",children:[e.jsxs(g,{separator:">",style:{marginBottom:"var(--space-2)",opacity:.7},children:[e.jsxs(g.Item,{href:"",children:[e.jsx(z,{})," ড্যাশবোর্ড"]}),e.jsxs(g.Item,{children:[e.jsx(B,{})," সিস্টেম কন্ট্রোল"]}),e.jsx(g.Item,{children:"কোটা ম্যানেজমেন্ট"})]}),e.jsxs("div",{style:{display:"flex",justifyContent:"space-between",alignItems:"flex-end",flexWrap:"wrap",gap:"var(--space-4)"},children:[e.jsxs("div",{children:[e.jsxs(Z,{level:2,className:"admin-title",children:["কোটা ও লিমিট ম্যানেজমেন্ট ",e.jsx("span",{className:"admin-badge",children:"SYSTEM CONTROLS"})]}),e.jsx(b,{className:"admin-subtitle",children:"ব্যবহারকারীর টিয়ার ভিত্তিক সীমাবদ্ধতা এবং লাইভ ইউসেজ মনিটরিং"})]}),e.jsx(k,{type:"primary",icon:e.jsx(V,{}),onClick:p,loading:u,className:"admin-btn-primary",style:{background:"linear-gradient(135deg, #10b981 0%, #059669 100%)",border:"none"},children:"রিফ্রেশ করুন"})]})]}),e.jsxs(A,{gutter:[24,24],children:[e.jsx(s,{xs:24,lg:8,children:e.jsx(c,{className:"glass-card stat-card",style:{background:"linear-gradient(135deg, rgba(59,130,246,0.1) 0%, rgba(37,99,235,0.05) 100%)"},children:e.jsx(S,{title:e.jsx("span",{style:{color:"rgba(255,255,255,0.45)"},children:"মোট API ইউজার"}),value:R.length,prefix:e.jsx(Y,{style:{color:"#3b82f6"}}),valueStyle:{color:"#fff",fontWeight:800}})})}),e.jsx(s,{xs:24,lg:8,children:e.jsx(c,{className:"glass-card stat-card",style:{background:"linear-gradient(135deg, rgba(16,185,129,0.1) 0%, rgba(5,150,105,0.05) 100%)"},children:e.jsx(S,{title:e.jsx("span",{style:{color:"rgba(255,255,255,0.45)"},children:"সিস্টেম হেলথ"}),value:98.4,precision:1,suffix:"%",prefix:e.jsx(_,{style:{color:"#10b981"}}),valueStyle:{color:"#fff",fontWeight:800}})})}),e.jsx(s,{xs:24,lg:8,children:e.jsx(c,{className:"glass-card stat-card",style:{background:"linear-gradient(135deg, rgba(245,158,11,0.1) 0%, rgba(217,119,6,0.05) 100%)"},children:e.jsx(S,{title:e.jsx("span",{style:{color:"rgba(255,255,255,0.45)"},children:"গড় ইউসেজ"}),value:124,prefix:e.jsx(C,{style:{color:"#f59e0b"}}),valueStyle:{color:"#fff",fontWeight:800}})})}),e.jsx(s,{xs:24,children:e.jsx(c,{title:e.jsxs("span",{style:{color:"#fff"},children:[e.jsx(q,{})," টিয়ার কনফিগারেশন"]}),className:"glass-card",bodyStyle:{padding:0},children:e.jsx(N,{dataSource:v,columns:L,pagination:!1,loading:u,className:"admin-table-dark",rowKey:"tier"})})}),e.jsx(s,{xs:24,children:e.jsx(c,{title:e.jsxs("span",{style:{color:"#fff"},children:[e.jsx(C,{})," লাইভ ইউসেজ ট্র্যাকার"]}),className:"glass-card",bodyStyle:{padding:0},children:e.jsx(N,{dataSource:R,columns:P,pagination:{pageSize:10},loading:u,className:"admin-table-dark",rowKey:"apiKey"})})})]}),e.jsx(J,{title:`Edit Quota: ${y==null?void 0:y.tier}`,visible:$,onCancel:()=>h(!1),onOk:()=>j.submit(),confirmLoading:u,className:"premium-modal",okText:"আপডেট করুন",cancelText:"বাতিল",children:e.jsxs(l,{form:j,layout:"vertical",onFinish:U,children:[e.jsx(l.Item,{name:"tier",hidden:!0,children:e.jsx(G,{})}),e.jsx(l.Item,{name:"quota",label:"মাসিক কোটা (AI Calls)",rules:[{required:!0}],extra:"-১ মানে অসীম বা আনলিমিটেড",children:e.jsx(w,{style:{width:"100%"}})}),e.jsxs(A,{gutter:16,children:[e.jsx(s,{span:12,children:e.jsx(l.Item,{name:"maxApis",label:"সর্বোচ্চ API Keys",rules:[{required:!0}],children:e.jsx(w,{style:{width:"100%"}})})}),e.jsx(s,{span:12,children:e.jsx(l.Item,{name:"maxSimulator",label:"সিমুলেটর লিমিট",rules:[{required:!0}],children:e.jsx(w,{style:{width:"100%"}})})})]})]})}),e.jsx("style",{children:`
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
      `})]})};export{le as default};
