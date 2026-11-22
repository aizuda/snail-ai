import{bt as N,b3 as E,S as v,Q as H,d as O,X as p,bu as V,bg as j,aa as A,a0 as L,a2 as w,h as k,ah as T,bv as S}from"./index-D0DF2D9J.js";let y=!1;function $(){if(N&&window.CSS&&!y&&(y=!0,"registerProperty"in window?.CSS))try{CSS.registerProperty({name:"--n-color-start",syntax:"<color>",inherits:!1,initialValue:"#0000"}),CSS.registerProperty({name:"--n-color-end",syntax:"<color>",inherits:!1,initialValue:"#0000"})}catch{}}function F(e){const{heightSmall:i,heightMedium:r,heightLarge:s,borderRadius:a}=e;return{color:"#eee",colorEnd:"#ddd",borderRadius:a,heightSmall:i,heightMedium:r,heightLarge:s}}const I={common:E,self:F},K=v([H("skeleton",`
 height: 1em;
 width: 100%;
 transition:
 --n-color-start .3s var(--n-bezier),
 --n-color-end .3s var(--n-bezier),
 background-color .3s var(--n-bezier);
 animation: 2s skeleton-loading infinite cubic-bezier(0.36, 0, 0.64, 1);
 background-color: var(--n-color-start);
 `),v("@keyframes skeleton-loading",`
 0% {
 background: var(--n-color-start);
 }
 40% {
 background: var(--n-color-end);
 }
 80% {
 background: var(--n-color-start);
 }
 100% {
 background: var(--n-color-start);
 }
 `)]),M=Object.assign(Object.assign({},w.props),{text:Boolean,round:Boolean,circle:Boolean,height:[String,Number],width:[String,Number],size:String,repeat:{type:Number,default:1},animated:{type:Boolean,default:!0},sharp:{type:Boolean,default:!0}}),W=O({name:"Skeleton",inheritAttrs:!1,props:M,setup(e){$();const{mergedClsPrefixRef:i,mergedComponentPropsRef:r}=L(e),s=k(()=>{var n,t;return e.size||((t=(n=r?.value)===null||n===void 0?void 0:n.Skeleton)===null||t===void 0?void 0:t.size)}),a=w("Skeleton","-skeleton",K,I,e,i);return{mergedClsPrefix:i,style:k(()=>{var n,t;const m=a.value,{common:{cubicBezierEaseInOut:z}}=m,h=m.self,{color:x,colorEnd:C,borderRadius:P}=h;let l;const{circle:d,sharp:B,round:R,width:o,height:c,text:f,animated:_}=e,b=s.value;b!==void 0&&(l=h[T("height",b)]);const u=d?(n=o??c)!==null&&n!==void 0?n:l:o,g=(t=d?o??c:c)!==null&&t!==void 0?t:l;return{display:f?"inline-block":"",verticalAlign:f?"-0.125em":"",borderRadius:d?"50%":R?"4096px":B?"":P,width:typeof u=="number"?S(u):u,height:typeof g=="number"?S(g):g,animation:_?"":"none","--n-bezier":z,"--n-color-start":x,"--n-color-end":C}})}},render(){const{repeat:e,style:i,mergedClsPrefix:r,$attrs:s}=this,a=p("div",V({class:`${r}-skeleton`,style:i},s));return e>1?p(A,null,j(e,null).map(n=>[a,`
`])):a}});export{W as N};
