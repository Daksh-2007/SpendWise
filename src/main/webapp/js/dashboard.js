(() => {
"use strict";
const $ = id => document.getElementById(id);
const base = (() => { const p = (location.pathname || "/").replace(/\/[^/]*\.[^/]*$/, "").replace(/\/+$/, ""); return p === "/" ? "" : p; })();
const url = path => `${base}${path}`;
const money = n => new Intl.NumberFormat("en-IN",{style:"currency",currency:"INR"}).format(Number(n||0));
const isoToday = () => { const d=new Date(); return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,"0")}-${String(d.getDate()).padStart(2,"0")}`; };
const monthNow = () => isoToday().slice(0,7);
let categories=[], categoryChart=null, barChart=null, homeExpenses=[], editExpenses=new Map(), editingExpense=null;
const deletingExpenseIds=new Set();
let lastExpensesQuery=null, inFlightExpensesQuery=null, expensesAbortController=null, expensesReqId=0, reportsReqId=0, debouncedSearchExpenses=null;
const CATEGORY_PALETTE=[
 {bg:"#159b70",border:"#0f7554"},
 {bg:"#2563eb",border:"#1d4ed8"},
 {bg:"#f59e0b",border:"#d97706"},
 {bg:"#e11d48",border:"#be123c"},
 {bg:"#7c3aed",border:"#6d28d9"},
 {bg:"#0891b2",border:"#0e7490"},
 {bg:"#ea580c",border:"#c2410c"},
 {bg:"#16a34a",border:"#15803d"},
 {bg:"#db2777",border:"#be185d"},
 {bg:"#4f46e5",border:"#4338ca"},
 {bg:"#ca8a04",border:"#a16207"},
 {bg:"#0d9488",border:"#0f766e"}
];
const categoryColorMap=new Map();
function colorForIndex(i){if(i<CATEGORY_PALETTE.length)return CATEGORY_PALETTE[i];const h=(i*137.508)%360;return{bg:`hsl(${h.toFixed(1)}, 68%, 48%)`,border:`hsl(${h.toFixed(1)}, 72%, 34%)`};}
function getCategoryColor(id){const key=Number(id);if(!categoryColorMap.has(key))categoryColorMap.set(key,colorForIndex(categoryColorMap.size));return categoryColorMap.get(key);}

async function api(path, options={}) {
    const response=await fetch(url(path),{credentials:"same-origin",...options});
    if(response.redirected && response.url.includes("/login")) { location.href=url("/login.html"); throw new Error("Session expired. Please log in again."); }
    const raw=await response.text();
    if(!response.ok) { let msg=`Request failed (${response.status})`; try{msg=JSON.parse(raw).message||msg;}catch(e){} throw new Error(msg); }
    try{return raw?JSON.parse(raw):null;}catch(e){throw new Error("Unexpected server response. Check your login session.");}
}
function esc(s){return String(s??"").replace(/[&<>"']/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[c]));}
function dateText(s){if(!s)return "—";const [y,m,d]=String(s).slice(0,10).split("-");return `${d}-${m}-${y}`;}
function dateForApi(s){if(!s.trim())return "";if(!/^\d{2}-\d{2}-\d{4}$/.test(s.trim()))throw new Error("Use DD-MM-YYYY date format.");const [d,m,y]=s.trim().split("-").map(Number);const test=new Date(y,m-1,d);if(test.getFullYear()!==y||test.getMonth()!==m-1||test.getDate()!==d)throw new Error("Enter a valid date.");return `${String(d).padStart(2,"0")}-${String(m).padStart(2,"0")}-${y}`;}
function message(msg,type="danger"){const el=$("message");el.className=`alert alert-${type}`;el.textContent=msg;el.classList.remove("d-none");}
function hideMessage(){$("message").classList.add("d-none");}
function fillSelect(el,all=false){el.innerHTML=(all?'<option value="">All categories</option>':'<option value="">Select category</option>')+categories.map(c=>`<option value="${Number(c.id)}">${esc(c.name)}</option>`).join("");}
async function loadCategories(){categories=await api("/api/categories");categories.forEach(c=>getCategoryColor(c.id));fillSelect($("filterCategory"),true);fillSelect($("reportCategory"),true);fillSelect($("expenseCategory"));}
function query(values){const p=new URLSearchParams();Object.entries(values).forEach(([k,v])=>{if(v!==""&&v!=null)p.set(k,v)});return p.toString();}
function switchPage(name){hideMessage();document.querySelectorAll(".page").forEach(p=>p.classList.toggle("active",p.id===`page-${name}`));document.querySelectorAll("[data-page]").forEach(b=>b.classList.toggle("active",b.dataset.page===name));$("pageTitle").textContent=name==="home"?"Dashboard":name==="visual"?"Visual Reports":"Add / Edit Expenses";$("sidebar").classList.remove("open");if(name==="home")loadHome();if(name==="visual")loadReports();if(name==="manage")loadEditList();}
async function loadHome(){
 try{
  hideMessage();
  const month=$("homeMonth").value||monthNow();$("homeMonth").value=month;$("filterMonth").value=month;
  const s=await api(`/api/dashboard?month=${encodeURIComponent(month)}`);
  $("sumTotal").textContent=money(s.totalAmount);$("sumCount").textContent=Number(s.transactionCount||0).toLocaleString("en-IN");$("sumAverage").textContent=money(s.averageAmount);
  await loadExpenses(true);
 }catch(e){message(e.message);}
}
async function loadExpenses(force=false){
 if(debouncedSearchExpenses)debouncedSearchExpenses.cancel();
 const search=$("filterSearch").value.trim();
 const q=query({month:$("filterMonth").value,categoryId:$("filterCategory").value,sort:$("filterSort").value,search,date:dateForApi($("filterDate").value)});
 if(!force){
  if(q===inFlightExpensesQuery)return;
  if(q===lastExpensesQuery){
   if(expensesAbortController){expensesAbortController.abort();expensesAbortController=null;inFlightExpensesQuery=null;expensesReqId++;}
   return;
  }
 }
 if(expensesAbortController)expensesAbortController.abort();
 const controller=new AbortController();
 expensesAbortController=controller;
 inFlightExpensesQuery=q;
 const reqId=++expensesReqId;
 try{
  const rows=await api(`/api/expenses${q?`?${q}`:""}`,{signal:controller.signal});
  if(reqId!==expensesReqId)return;
  lastExpensesQuery=q;
  homeExpenses=rows;
  $("filteredTotal").textContent=money(rows.reduce((a,x)=>a+Number(x.amount||0),0));
  $("expenseCount").textContent=`${rows.length} expense${rows.length===1?"":"s"} found`;
  $("tableSubtitle").textContent=search?`Matches for “${search}”`:"Your recorded transactions";
  $("expenseRows").innerHTML=rows.length?rows.map(x=>`<tr><td>${dateText(x.expenseDate)}</td><td><strong>${esc(x.description)}</strong></td><td><span class="category-tag">${esc(x.categoryName)}</span></td><td class="text-end">${money(x.amount)}</td></tr>`).join(""):'<tr><td colspan="4" class="empty">No expenses found for these filters.</td></tr>';
 }catch(e){
  if(e.name==="AbortError")return;
  throw e;
 }finally{
  if(expensesAbortController===controller){expensesAbortController=null;inFlightExpensesQuery=null;}
 }
}
async function refreshExpenses(force=false){
 try{
  hideMessage();
  await loadExpenses(force);
 }catch(e){
  message(e.message);
 }
}
async function loadReports(){
 const reqId=++reportsReqId;
 try{
  hideMessage();
  if(!categories.length)await loadCategories();
  const q=query({month:$("reportMonth").value||monthNow(),categoryId:$("reportCategory").value,search:$("reportSearch").value.trim()});
  const rows=await api(`/api/expenses?${q}`);
  if(reqId!==reportsReqId)return;
  const map=new Map(categories.map(c=>[Number(c.id),{id:Number(c.id),name:c.name,amount:0,count:0}]));
  rows.forEach(x=>{const id=Number(x.categoryId);if(!map.has(id))map.set(id,{id,name:x.categoryName,amount:0,count:0});const r=map.get(id);r.amount+=Number(x.amount||0);r.count++;});
  const data=[...map.values()],total=rows.reduce((a,x)=>a+Number(x.amount||0),0),top=[...data].filter(x=>x.count).sort((a,b)=>b.amount-a.amount)[0];
  $("reportTotal").textContent=money(total);$("reportCount").textContent=rows.length;$("topCategory").textContent=top?top.name:"—";
  const sorted=[...data].sort((a,b)=>b.amount-a.amount||a.id-b.id);
  $("categoryRows").innerHTML=sorted.map(x=>`<tr><td>${esc(x.name)}</td><td>${x.count}</td><td>${total?(x.amount/total*100).toFixed(2):"0.00"}%</td><td class="text-end">${money(x.amount)}</td></tr>`).join("");
  if(categoryChart){categoryChart.destroy();categoryChart=null;}
  if(barChart){barChart.destroy();barChart=null;}
  if(typeof Chart.getChart==="function"){Chart.getChart($("donutChart"))?.destroy();Chart.getChart($("barChart"))?.destroy();}
  const labels=data.map(x=>x.name),values=data.map(x=>Number(x.amount.toFixed(2)));
  const colors=data.map(x=>getCategoryColor(x.id));
  const bgColors=colors.map(c=>c.bg),borderColors=colors.map(c=>c.border);
  categoryChart=new Chart($("donutChart"),{
   type:"doughnut",
   data:{labels,datasets:[{label:"Spending (₹)",data:values,backgroundColor:bgColors,borderColor:borderColors,borderWidth:2,hoverOffset:6}]},
   options:{responsive:true,maintainAspectRatio:false,cutout:"62%",plugins:{legend:{display:true,position:"bottom",labels:{usePointStyle:true,pointStyle:"circle",boxWidth:9,padding:10,font:{size:11}}},tooltip:{callbacks:{label:ctx=>` ${ctx.label}: ${money(ctx.raw)}`}}}}
  });
  barChart=new Chart($("barChart"),{
   type:"bar",
   data:{labels,datasets:[{label:"Amount (₹)",data:values,backgroundColor:bgColors,borderColor:borderColors,borderWidth:2,borderRadius:5,maxBarThickness:32}]},
   options:{responsive:true,maintainAspectRatio:false,plugins:{legend:{display:true,position:"bottom",labels:{usePointStyle:true,pointStyle:"rectRounded",boxWidth:9,padding:10,font:{size:11},generateLabels:chart=>chart.data.labels.map((label,i)=>({text:label,fillStyle:bgColors[i],strokeStyle:borderColors[i],lineWidth:1.5,hidden:!chart.getDataVisibility(i),index:i}))},onClick:(e,item,legend)=>{legend.chart.toggleDataVisibility(item.index);legend.chart.update();}},tooltip:{callbacks:{label:ctx=>` ${ctx.label}: ${money(ctx.raw)}`}}},scales:{x:{grid:{display:false}},y:{beginAtZero:true}}}
  });
 }catch(e){if(reqId===reportsReqId)message(e.message);}
}
async function loadEditList(){
 try{
  const q=query({month:$("editMonth").value,search:$("editSearch").value.trim()});
  const rows=await api(`/api/expenses${q?`?${q}`:""}`);
  editExpenses=new Map(rows.map(x=>[Number(x.id),x]));
  $("editRows").innerHTML=rows.length?rows.map(x=>`<div class="edit-row"><div><div class="edit-desc">${esc(x.description)}</div><div class="edit-meta">${dateText(x.expenseDate)} · ${esc(x.categoryName)}</div></div><div class="edit-right"><span class="edit-amount">${money(x.amount)}</span><div class="edit-actions"><button type="button" class="edit-btn" data-edit="${Number(x.id)}"><i class="bi bi-pencil"></i> Edit</button><button type="button" class="delete-btn" data-delete="${Number(x.id)}"><i class="bi bi-trash"></i> Delete</button></div></div></div>`).join(""):'<div class="empty">No matching expenses.</div>';
 }catch(e){$("editRows").textContent=e.message;}
}
async function beginEdit(id){
 try{
  const rows=await api("/api/expenses");const x=rows.find(r=>Number(r.id)===Number(id));if(!x)throw new Error("Expense not found.");
  editingExpense=x;
  $("expenseId").value=x.id;$("expenseDate").value=String(x.expenseDate).slice(0,10);$("expenseAmount").value=x.amount;$("expenseCategory").value=x.categoryId;$("expenseDescription").value=x.description;
  $("formTitle").textContent="Edit expense";$("formSubtitle").textContent=`Update transaction #${x.id}`;$("saveExpense").textContent="Update expense";$("deleteExpense").classList.remove("d-none");$("cancelEdit").classList.remove("d-none");$("formMessage").textContent="";$("expenseForm").scrollIntoView({behavior:"smooth"});
 }catch(e){$("formMessage").textContent=e.message;$("formMessage").className="form-message error";}
}
function resetForm(){$("expenseForm").reset();editingExpense=null;$("expenseId").value="";$("expenseDate").value=isoToday();$("formTitle").textContent="Add new expense";$("formSubtitle").textContent="Enter your transaction details.";$("saveExpense").textContent="Save expense";$("deleteExpense").classList.add("d-none");$("cancelEdit").classList.add("d-none");$("formMessage").textContent="";}
async function deleteExpense(rawId, triggerBtn=null){
 const id=Number(rawId);
 if(!Number.isInteger(id)||id<=0){
  const errMsg="Invalid expense ID.";
  $("formMessage").textContent=errMsg;$("formMessage").className="form-message error";
  message(errMsg,"danger");
  return;
 }
 if(deletingExpenseIds.has(id))return;
 const exp=editExpenses.get(id)||(editingExpense&&Number(editingExpense.id)===id?editingExpense:null);
 const desc=exp?exp.description:$("expenseDescription").value.trim()||`Expense #${id}`;
 const amt=exp?exp.amount:$("expenseAmount").value;
 const confirmed=confirm(`Are you sure you want to delete this expense?\n\nDescription: ${desc}\nAmount: ${money(amt)}`);
 if(!confirmed)return;
 deletingExpenseIds.add(id);
 if(triggerBtn)triggerBtn.disabled=true;
 try{
  const result=await api(`/api/expenses?id=${encodeURIComponent(id)}`,{method:"DELETE"});
  if(!result||result.success!==true)throw new Error((result&&result.message)||"Could not delete expense.");
  const successMsg=result.message||"Expense deleted successfully.";
  if(Number($("expenseId").value)===id)resetForm();
  await Promise.all([loadEditList(),loadHome(),loadReports()]);
  $("formMessage").textContent=successMsg;$("formMessage").className="form-message success";
  message(successMsg,"success");
 }catch(err){
  const errMsg=err.message||"Could not delete expense.";
  $("formMessage").textContent=errMsg;$("formMessage").className="form-message error";
  message(errMsg,"danger");
 }finally{
  deletingExpenseIds.delete(id);
  if(triggerBtn)triggerBtn.disabled=false;
 }
}
async function saveExpense(e){
 e.preventDefault();const id=$("expenseId").value;const body=new URLSearchParams({amount:$("expenseAmount").value,categoryId:$("expenseCategory").value,description:$("expenseDescription").value.trim(),expenseDate:$("expenseDate").value});if(id)body.set("id",id);
 try{
  const response=await fetch(url("/api/expenses"),{method:id?"PUT":"POST",credentials:"same-origin",headers:{"Content-Type":"application/x-www-form-urlencoded;charset=UTF-8"},body:body.toString()});
  const raw=await response.text();if(!response.ok)throw new Error((()=>{try{return JSON.parse(raw).message||"Could not save expense."}catch(e){return "Could not save expense."}})());
  $("formMessage").textContent=id?"Expense updated successfully.":"Expense added successfully.";$("formMessage").className="form-message success";resetForm();await loadEditList();await loadHome();
 }catch(err){$("formMessage").textContent=err.message;$("formMessage").className="form-message error";}
}
function debounce(fn,ms=300){let t;const d=(...args)=>{clearTimeout(t);t=setTimeout(()=>{t=null;fn(...args);},ms);};d.cancel=()=>{clearTimeout(t);t=null;};return d;}
document.addEventListener("DOMContentLoaded",async()=>{
 $("todayLabel").textContent=new Intl.DateTimeFormat("en-IN",{day:"numeric",month:"short",year:"numeric"}).format(new Date());
 ["homeMonth","filterMonth","reportMonth","editMonth"].forEach(id=>$(id).value=monthNow());$("expenseDate").value=isoToday();
 document.querySelectorAll("[data-page]").forEach(b=>b.addEventListener("click",()=>switchPage(b.dataset.page)));
 $("menuBtn").addEventListener("click",()=>$("sidebar").classList.toggle("open"));
 $("homeMonth").addEventListener("change",()=>{ $("filterMonth").value=$("homeMonth").value;loadHome();});
 const applyFiltersBtn=$("applyFilters");if(applyFiltersBtn)applyFiltersBtn.remove();
 debouncedSearchExpenses=debounce(()=>refreshExpenses(),300);
 ["filterMonth","filterCategory","filterSort","filterDate"].forEach(id=>$(id).addEventListener("change",()=>refreshExpenses()));
 $("filterSearch").addEventListener("input",debouncedSearchExpenses);
 $("filterDate").addEventListener("input",()=>{const v=$("filterDate").value.trim();if(v===""||/^\d{2}-\d{2}-\d{4}$/.test(v))refreshExpenses();});
 $("resetFilters").addEventListener("click",()=>{$("filterMonth").value=$("homeMonth").value;$("filterCategory").value="";$("filterSort").value="";$("filterSearch").value="";$("filterDate").value="";refreshExpenses();});
 ["filterSearch","filterDate"].forEach(id=>$(id).addEventListener("keydown",e=>{if(e.key==="Enter")refreshExpenses();}));
 $("reportMonth").addEventListener("change",loadReports);$("applyReport").addEventListener("click",loadReports);$("reportSearch").addEventListener("keydown",e=>{if(e.key==="Enter")loadReports()});
 $("editSearch").addEventListener("input",debounce(loadEditList));$("editMonth").addEventListener("change",loadEditList);
 $("editRows").addEventListener("click",e=>{const editBtn=e.target.closest("[data-edit]");if(editBtn){beginEdit(editBtn.dataset.edit);return;}const delBtn=e.target.closest("[data-delete]");if(delBtn)deleteExpense(delBtn.dataset.delete,delBtn);});
 $("expenseForm").addEventListener("submit",saveExpense);$("deleteExpense").addEventListener("click",()=>{const id=$("expenseId").value;if(id)deleteExpense(id,$("deleteExpense"));});$("cancelEdit").addEventListener("click",resetForm);
 $("logout").addEventListener("click",e=>{e.preventDefault();location.href=url("/logout")});
 try{await loadCategories();await loadHome()}catch(e){message(e.message)}
});
})();