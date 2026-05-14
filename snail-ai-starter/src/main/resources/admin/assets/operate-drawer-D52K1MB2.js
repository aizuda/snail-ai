import{L as v,M as e,a0 as K,X as M,Y as F,aU as Q,aV as X,d as T,a1 as Y,O as l,Q as J,a2 as Z,S as ee,U as H,b1 as te,ad as oe,h as B,b2 as re,V,aG as ne,b3 as se,s as le,I as ie,aA as ae,y as de,o as D,c as E,g as j,aD as ce,w as I,e as pe,aB as be,b4 as he,b5 as L,b as W,t as ue,a as me,b6 as ge,b7 as ve,E as fe,aH as G,r as we,q as Se}from"./index-0ChY-CaP.js";function U(r,u="default",s=[]){const{children:d}=r;if(d!==null&&typeof d=="object"&&!Array.isArray(d)){const i=d[u];if(typeof i=="function")return i()}return s}const xe=v([e("descriptions",{fontSize:"var(--n-font-size)"},[e("descriptions-separator",`
 display: inline-block;
 margin: 0 8px 0 2px;
 `),e("descriptions-table-wrapper",[e("descriptions-table",[e("descriptions-table-row",[e("descriptions-table-header",{padding:"var(--n-th-padding)"}),e("descriptions-table-content",{padding:"var(--n-td-padding)"})])])]),K("bordered",[e("descriptions-table-wrapper",[e("descriptions-table",[e("descriptions-table-row",[v("&:last-child",[e("descriptions-table-content",{paddingBottom:0})])])])])]),M("left-label-placement",[e("descriptions-table-content",[v("> *",{verticalAlign:"top"})])]),M("left-label-align",[v("th",{textAlign:"left"})]),M("center-label-align",[v("th",{textAlign:"center"})]),M("right-label-align",[v("th",{textAlign:"right"})]),M("bordered",[e("descriptions-table-wrapper",`
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
 `,[F("content",`
 transition: color .3s var(--n-bezier);
 display: inline-block;
 color: var(--n-td-text-color);
 `)]),F("label",`
 font-weight: var(--n-th-font-weight);
 transition: color .3s var(--n-bezier);
 display: inline-block;
 margin-right: 14px;
 color: var(--n-th-text-color);
 `)])])])]),e("descriptions-table-wrapper",`
 --n-merged-th-color: var(--n-th-color);
 --n-merged-td-color: var(--n-td-color);
 --n-merged-border-color: var(--n-border-color);
 `),Q(e("descriptions-table-wrapper",`
 --n-merged-th-color: var(--n-th-color-modal);
 --n-merged-td-color: var(--n-td-color-modal);
 --n-merged-border-color: var(--n-border-color-modal);
 `)),X(e("descriptions-table-wrapper",`
 --n-merged-th-color: var(--n-th-color-popover);
 --n-merged-td-color: var(--n-td-color-popover);
 --n-merged-border-color: var(--n-border-color-popover);
 `))]),q="DESCRIPTION_ITEM_FLAG";function ye(r){return typeof r=="object"&&r&&!Array.isArray(r)?r.type&&r.type[q]:!1}const ze=Object.assign(Object.assign({},H.props),{title:String,column:{type:Number,default:3},columns:Number,labelPlacement:{type:String,default:"top"},labelAlign:{type:String,default:"left"},separator:{type:String,default:":"},size:String,bordered:Boolean,labelClass:String,labelStyle:[Object,String],contentClass:String,contentStyle:[Object,String]}),ke=T({name:"Descriptions",props:ze,slots:Object,setup(r){const{mergedClsPrefixRef:u,inlineThemeDisabled:s,mergedComponentPropsRef:d}=ee(r),i=B(()=>{var n,c;return r.size||((c=(n=d?.value)===null||n===void 0?void 0:n.Descriptions)===null||c===void 0?void 0:c.size)||"medium"}),f=H("Descriptions","-descriptions",xe,te,r,u),w=B(()=>{const{bordered:n}=r,c=i.value,{common:{cubicBezierEaseInOut:S},self:{titleTextColor:o,thColor:p,thColorModal:b,thColorPopover:h,thTextColor:m,thFontWeight:z,tdTextColor:O,tdColor:t,tdColorModal:C,tdColorPopover:A,borderColor:g,borderColorModal:x,borderColorPopover:$,borderRadius:_,lineHeight:y,[V("fontSize",c)]:P,[V(n?"thPaddingBordered":"thPadding",c)]:k,[V(n?"tdPaddingBordered":"tdPadding",c)]:R}}=f.value;return{"--n-title-text-color":o,"--n-th-padding":k,"--n-td-padding":R,"--n-font-size":P,"--n-bezier":S,"--n-th-font-weight":z,"--n-line-height":y,"--n-th-text-color":m,"--n-td-text-color":O,"--n-th-color":p,"--n-th-color-modal":b,"--n-th-color-popover":h,"--n-td-color":t,"--n-td-color-modal":C,"--n-td-color-popover":A,"--n-border-radius":_,"--n-border-color":g,"--n-border-color-modal":x,"--n-border-color-popover":$}}),a=s?oe("descriptions",B(()=>{let n="";const{bordered:c}=r;return c&&(n+="a"),n+=i.value[0],n}),w,r):void 0;return{mergedClsPrefix:u,cssVars:s?void 0:w,themeClass:a?.themeClass,onRender:a?.onRender,compitableColumn:re(r,["columns","column"]),inlineThemeDisabled:s,mergedSize:i}},render(){const r=this.$slots.default,u=r?Y(r()):[];u.length;const{contentClass:s,labelClass:d,compitableColumn:i,labelPlacement:f,labelAlign:w,mergedSize:a,bordered:n,title:c,cssVars:S,mergedClsPrefix:o,separator:p,onRender:b}=this;b?.();const h=u.filter(t=>ye(t)),m={span:0,row:[],secondRow:[],rows:[]},O=h.reduce((t,C,A)=>{const g=C.props||{},x=h.length-1===A,$=["label"in g?g.label:U(C,"label")],_=[U(C)],y=g.span||1,P=t.span;t.span+=y;const k=g.labelStyle||g["label-style"]||this.labelStyle,R=g.contentStyle||g["content-style"]||this.contentStyle;if(f==="left")n?t.row.push(l("th",{class:[`${o}-descriptions-table-header`,d],colspan:1,style:k},$),l("td",{class:[`${o}-descriptions-table-content`,s],colspan:x?(i-P)*2+1:y*2-1,style:R},_)):t.row.push(l("td",{class:`${o}-descriptions-table-content`,colspan:x?(i-P)*2:y*2},l("span",{class:[`${o}-descriptions-table-content__label`,d],style:k},[...$,p&&l("span",{class:`${o}-descriptions-separator`},p)]),l("span",{class:[`${o}-descriptions-table-content__content`,s],style:R},_)));else{const N=x?(i-P)*2:y*2;t.row.push(l("th",{class:[`${o}-descriptions-table-header`,d],colspan:N,style:k},$)),t.secondRow.push(l("td",{class:[`${o}-descriptions-table-content`,s],colspan:N,style:R},_))}return(t.span>=i||x)&&(t.span=0,t.row.length&&(t.rows.push(t.row),t.row=[]),f!=="left"&&t.secondRow.length&&(t.rows.push(t.secondRow),t.secondRow=[])),t},m).rows.map(t=>l("tr",{class:`${o}-descriptions-table-row`},t));return l("div",{style:S,class:[`${o}-descriptions`,this.themeClass,`${o}-descriptions--${f}-label-placement`,`${o}-descriptions--${w}-label-align`,`${o}-descriptions--${a}-size`,n&&`${o}-descriptions--bordered`]},c||this.$slots.header?l("div",{class:`${o}-descriptions-header`},c||Z(this,"header")):null,l("div",{class:`${o}-descriptions-table-wrapper`},l("table",{class:`${o}-descriptions-table`},l("tbody",null,f==="top"&&l("tr",{class:`${o}-descriptions-table-row`,style:{visibility:"collapse"}},J(i*2,l("td",null))),O))))}}),Ce={label:String,span:{type:Number,default:1},labelClass:String,labelStyle:[Object,String],contentClass:String,contentStyle:[Object,String]},Re=T({name:"DescriptionsItem",[q]:!0,props:Ce,slots:Object,render(){return null}}),$e={class:"flex items-center justify-between w-full"},_e=T({name:"OperateDrawer",__name:"operate-drawer",props:G({title:{},minSize:{default:360},maxSize:{default:void 0},defaultFullscreen:{type:Boolean,default:!1}},{modelValue:{type:Boolean,default:!1},modelModifiers:{}}),emits:G(["update:modelValue"],["update:modelValue"]),setup(r,{emit:u}){const s=r,d=u,i=ne(r,"modelValue"),f=se(),w=le(),a=we({width:0}),n=ie(s.defaultFullscreen),c=B(()=>{const p=s.minSize,b=Math.max(s.maxSize??s.minSize+240,s.minSize,600);if(w.isMobile)return a.width*.9>=p?`${p}px`:"90%";let h=a.width*.3>=p?`${p}px`:"30%";h=a.width<=420?"90%":h;let m=a.width*.85>=b?`${b}px`:"85%";return m=a.width<=420?"90%":m,n.value?m:h}),S=()=>{a.width=document.documentElement.clientWidth};ae(()=>{S(),window.addEventListener("resize",S)}),de(()=>{window.removeEventListener("resize",S)});const o=p=>{d("update:modelValue",p)};return(p,b)=>{const h=ge,m=ve;return D(),E(j(ce),{show:i.value,"onUpdate:show":[b[1]||(b[1]=z=>i.value=z),o],"display-directive":"if",width:c.value},{default:I(()=>[pe(j(be),{title:s.title,"native-scrollbar":!1,closable:"","header-class":"operate-drawer-header"},he({header:I(()=>[W("div",$e,[W("span",null,ue(s.title),1),j(w).isMobile?fe("",!0):(D(),me("div",{key:0,class:"fullscreen text-18px color-#6a6a6a",onClick:b[0]||(b[0]=z=>n.value=!n.value)},[n.value?(D(),E(h,{key:0})):(D(),E(m,{key:1}))]))])]),default:I(()=>[L(p.$slots,"default",{},void 0,!0)]),_:2},[f.footer?{name:"footer",fn:I(()=>[L(p.$slots,"footer",{},void 0,!0)]),key:"0"}:void 0]),1032,["title"])]),_:3},8,["show","width"])}}}),Me=Se(_e,[["__scopeId","data-v-5428dfd3"]]);export{ke as N,Me as O,Re as a};
