import{O as v,P as e,a1 as q,Y as I,Z as W,aV as Y,aW as Z,d as T,a2 as J,K as l,R as Q,a3 as X,U as ee,V as H,b2 as te,ad as oe,h as D,b3 as re,W as A,aH as ne,b4 as se,s as le,I as ie,aB as ae,y as de,o as M,c as E,g as j,aE as ce,w as B,e as pe,aC as be,b5 as he,b6 as F,b as L,t as ue,a as me,b7 as ge,b8 as ve,E as fe,aI as G,r as we,q as Se}from"./index-Cfzqw6bG.js";function U(r,u="default",s=[]){const{children:d}=r;if(d!==null&&typeof d=="object"&&!Array.isArray(d)){const i=d[u];if(typeof i=="function")return i()}return s}const xe=v([e("descriptions",{fontSize:"var(--n-font-size)"},[e("descriptions-separator",`
 display: inline-block;
 margin: 0 8px 0 2px;
 `),e("descriptions-table-wrapper",[e("descriptions-table",[e("descriptions-table-row",[e("descriptions-table-header",{padding:"var(--n-th-padding)"}),e("descriptions-table-content",{padding:"var(--n-td-padding)"})])])]),q("bordered",[e("descriptions-table-wrapper",[e("descriptions-table",[e("descriptions-table-row",[v("&:last-child",[e("descriptions-table-content",{paddingBottom:0})])])])])]),I("left-label-placement",[e("descriptions-table-content",[v("> *",{verticalAlign:"top"})])]),I("left-label-align",[v("th",{textAlign:"left"})]),I("center-label-align",[v("th",{textAlign:"center"})]),I("right-label-align",[v("th",{textAlign:"right"})]),I("bordered",[e("descriptions-table-wrapper",`
 border-radius: var(--n-border-radius);
 overflow: hidden;
 background: var(--n-merged-td-color);
 border: 1px solid var(--n-merged-border-color);
 `,[e("descriptions-table",[e("descriptions-table-row",[v("&:not(:last-child)",[e("descriptions-table-content",{borderBottom:"1px solid var(--n-merged-border-color)"}),e("descriptions-table-header",{borderBottom:"1px solid var(--n-merged-border-color)"})]),e("descriptions-table-header",`
 font-weight: 400;
 background-clip: padding-box;
 background-color: var(--n-merged-th-color);
 `,[v("&:not(:last-child)",{borderRight:"1px solid var(--n-merged-border-color)"})]),e("descriptions-table-content",[v("&:not(:last-child)",{borderRight:"1px solid var(--n-merged-border-color)"})])])])])]),e("descriptions-header",`
 font-weight: var(--n-th-font-weight);
 font-size: 18px;
 transition: color .3s var(--n-bezier);
 line-height: var(--n-line-height);
 margin-bottom: 16px;
 color: var(--n-title-text-color);
 `),e("descriptions-table-wrapper",`
 transition:
 background-color .3s var(--n-bezier),
 border-color .3s var(--n-bezier);
 `,[e("descriptions-table",`
 width: 100%;
 border-collapse: separate;
 border-spacing: 0;
 box-sizing: border-box;
 `,[e("descriptions-table-row",`
 box-sizing: border-box;
 transition: border-color .3s var(--n-bezier);
 `,[e("descriptions-table-header",`
 font-weight: var(--n-th-font-weight);
 line-height: var(--n-line-height);
 display: table-cell;
 box-sizing: border-box;
 color: var(--n-th-text-color);
 transition:
 color .3s var(--n-bezier),
 background-color .3s var(--n-bezier),
 border-color .3s var(--n-bezier);
 `),e("descriptions-table-content",`
 vertical-align: top;
 line-height: var(--n-line-height);
 display: table-cell;
 box-sizing: border-box;
 color: var(--n-td-text-color);
 transition:
 color .3s var(--n-bezier),
 background-color .3s var(--n-bezier),
 border-color .3s var(--n-bezier);
 `,[W("content",`
 transition: color .3s var(--n-bezier);
 display: inline-block;
 color: var(--n-td-text-color);
 `)]),W("label",`
 font-weight: var(--n-th-font-weight);
 transition: color .3s var(--n-bezier);
 display: inline-block;
 margin-right: 14px;
 color: var(--n-th-text-color);
 `)])])])]),e("descriptions-table-wrapper",`
 --n-merged-th-color: var(--n-th-color);
 --n-merged-td-color: var(--n-td-color);
 --n-merged-border-color: var(--n-border-color);
 `),Y(e("descriptions-table-wrapper",`
 --n-merged-th-color: var(--n-th-color-modal);
 --n-merged-td-color: var(--n-td-color-modal);
 --n-merged-border-color: var(--n-border-color-modal);
 `)),Z(e("descriptions-table-wrapper",`
 --n-merged-th-color: var(--n-th-color-popover);
 --n-merged-td-color: var(--n-td-color-popover);
 --n-merged-border-color: var(--n-border-color-popover);
 `))]),K="DESCRIPTION_ITEM_FLAG";function ye(r){return typeof r=="object"&&r&&!Array.isArray(r)?r.type&&r.type[K]:!1}const Ce=Object.assign(Object.assign({},H.props),{title:String,column:{type:Number,default:3},columns:Number,labelPlacement:{type:String,default:"top"},labelAlign:{type:String,default:"left"},separator:{type:String,default:":"},size:String,bordered:Boolean,labelClass:String,labelStyle:[Object,String],contentClass:String,contentStyle:[Object,String]}),Re=T({name:"Descriptions",props:Ce,slots:Object,setup(r){const{mergedClsPrefixRef:u,inlineThemeDisabled:s,mergedComponentPropsRef:d}=ee(r),i=D(()=>{var n,c;return r.size||((c=(n=d?.value)===null||n===void 0?void 0:n.Descriptions)===null||c===void 0?void 0:c.size)||"medium"}),f=H("Descriptions","-descriptions",xe,te,r,u),w=D(()=>{const{bordered:n}=r,c=i.value,{common:{cubicBezierEaseInOut:S},self:{titleTextColor:o,thColor:p,thColorModal:b,thColorPopover:h,thTextColor:m,thFontWeight:C,tdTextColor:O,tdColor:t,tdColorModal:z,tdColorPopover:V,borderColor:g,borderColorModal:x,borderColorPopover:$,borderRadius:_,lineHeight:y,[A("fontSize",c)]:P,[A(n?"thPaddingBordered":"thPadding",c)]:R,[A(n?"tdPaddingBordered":"tdPadding",c)]:k}}=f.value;return{"--n-title-text-color":o,"--n-th-padding":R,"--n-td-padding":k,"--n-font-size":P,"--n-bezier":S,"--n-th-font-weight":C,"--n-line-height":y,"--n-th-text-color":m,"--n-td-text-color":O,"--n-th-color":p,"--n-th-color-modal":b,"--n-th-color-popover":h,"--n-td-color":t,"--n-td-color-modal":z,"--n-td-color-popover":V,"--n-border-radius":_,"--n-border-color":g,"--n-border-color-modal":x,"--n-border-color-popover":$}}),a=s?oe("descriptions",D(()=>{let n="";const{bordered:c}=r;return c&&(n+="a"),n+=i.value[0],n}),w,r):void 0;return{mergedClsPrefix:u,cssVars:s?void 0:w,themeClass:a?.themeClass,onRender:a?.onRender,compitableColumn:re(r,["columns","column"]),inlineThemeDisabled:s,mergedSize:i}},render(){const r=this.$slots.default,u=r?J(r()):[];u.length;const{contentClass:s,labelClass:d,compitableColumn:i,labelPlacement:f,labelAlign:w,mergedSize:a,bordered:n,title:c,cssVars:S,mergedClsPrefix:o,separator:p,onRender:b}=this;b?.();const h=u.filter(t=>ye(t)),m={span:0,row:[],secondRow:[],rows:[]},O=h.reduce((t,z,V)=>{const g=z.props||{},x=h.length-1===V,$=["label"in g?g.label:U(z,"label")],_=[U(z)],y=g.span||1,P=t.span;t.span+=y;const R=g.labelStyle||g["label-style"]||this.labelStyle,k=g.contentStyle||g["content-style"]||this.contentStyle;if(f==="left")n?t.row.push(l("th",{class:[`${o}-descriptions-table-header`,d],colspan:1,style:R},$),l("td",{class:[`${o}-descriptions-table-content`,s],colspan:x?(i-P)*2+1:y*2-1,style:k},_)):t.row.push(l("td",{class:`${o}-descriptions-table-content`,colspan:x?(i-P)*2:y*2},l("span",{class:[`${o}-descriptions-table-content__label`,d],style:R},[...$,p&&l("span",{class:`${o}-descriptions-separator`},p)]),l("span",{class:[`${o}-descriptions-table-content__content`,s],style:k},_)));else{const N=x?(i-P)*2:y*2;t.row.push(l("th",{class:[`${o}-descriptions-table-header`,d],colspan:N,style:R},$)),t.secondRow.push(l("td",{class:[`${o}-descriptions-table-content`,s],colspan:N,style:k},_))}return(t.span>=i||x)&&(t.span=0,t.row.length&&(t.rows.push(t.row),t.row=[]),f!=="left"&&t.secondRow.length&&(t.rows.push(t.secondRow),t.secondRow=[])),t},m).rows.map(t=>l("tr",{class:`${o}-descriptions-table-row`},t));return l("div",{style:S,class:[`${o}-descriptions`,this.themeClass,`${o}-descriptions--${f}-label-placement`,`${o}-descriptions--${w}-label-align`,`${o}-descriptions--${a}-size`,n&&`${o}-descriptions--bordered`]},c||this.$slots.header?l("div",{class:`${o}-descriptions-header`},c||X(this,"header")):null,l("div",{class:`${o}-descriptions-table-wrapper`},l("table",{class:`${o}-descriptions-table`},l("tbody",null,f==="top"&&l("tr",{class:`${o}-descriptions-table-row`,style:{visibility:"collapse"}},Q(i*2,l("td",null))),O))))}}),ze={label:String,span:{type:Number,default:1},labelClass:String,labelStyle:[Object,String],contentClass:String,contentStyle:[Object,String]},ke=T({name:"DescriptionsItem",[K]:!0,props:ze,slots:Object,render(){return null}}),$e={class:"flex items-center justify-between w-full"},_e=T({name:"OperateDrawer",__name:"operate-drawer",props:G({title:{},minSize:{default:360},maxSize:{default:void 0},defaultFullscreen:{type:Boolean,default:!1}},{modelValue:{type:Boolean,default:!1},modelModifiers:{}}),emits:G(["update:modelValue"],["update:modelValue"]),setup(r,{emit:u}){const s=r,d=u,i=ne(r,"modelValue"),f=se(),w=le(),a=we({width:0}),n=ie(s.defaultFullscreen),c=D(()=>{const p=s.minSize,b=Math.max(s.maxSize??s.minSize+240,s.minSize,600);if(w.isMobile)return a.width*.9>=p?`${p}px`:"90%";let h=a.width*.3>=p?`${p}px`:"30%";h=a.width<=420?"90%":h;let m=a.width*.85>=b?`${b}px`:"85%";return m=a.width<=420?"90%":m,n.value?m:h}),S=()=>{a.width=document.documentElement.clientWidth};ae(()=>{S(),window.addEventListener("resize",S)}),de(()=>{window.removeEventListener("resize",S)});const o=p=>{d("update:modelValue",p)};return(p,b)=>{const h=ge,m=ve;return M(),E(j(ce),{show:i.value,"onUpdate:show":[b[1]||(b[1]=C=>i.value=C),o],"display-directive":"if",width:c.value},{default:B(()=>[pe(j(be),{title:s.title,"native-scrollbar":!1,closable:"","header-class":"operate-drawer-header"},he({header:B(()=>[L("div",$e,[L("span",null,ue(s.title),1),j(w).isMobile?fe("",!0):(M(),me("div",{key:0,class:"fullscreen text-18px color-#6a6a6a",onClick:b[0]||(b[0]=C=>n.value=!n.value)},[n.value?(M(),E(h,{key:0})):(M(),E(m,{key:1}))]))])]),default:B(()=>[F(p.$slots,"default",{},void 0,!0)]),_:2},[f.footer?{name:"footer",fn:B(()=>[F(p.$slots,"footer",{},void 0,!0)]),key:"0"}:void 0]),1032,["title"])]),_:3},8,["show","width"])}}}),Ie=Se(_e,[["__scopeId","data-v-5428dfd3"]]);export{Re as N,Ie as O,ke as a};
