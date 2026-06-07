import{i as W,o as M}from"./utils-CpJzjMdW.js";import{a6 as N,P as T,aV as V,O as E,aW as _,Z as k,d as K,aa as A,L as G,K as y,aX as U,U as X,ab as H,V as $,aY as Y,aZ as Z,ad as D,h as O,a_ as q,I as p,x as J,a$ as Q,an as ee,am as re,W as oe}from"./index-Cq6_y1za.js";const te=N("n-avatar-group"),ne=T("avatar",`
 width: var(--n-merged-size);
 height: var(--n-merged-size);
 color: #FFF;
 font-size: var(--n-font-size);
 display: inline-flex;
 position: relative;
 overflow: hidden;
 text-align: center;
 border: var(--n-border);
 border-radius: var(--n-border-radius);
 --n-merged-color: var(--n-color);
 background-color: var(--n-merged-color);
 transition:
 border-color .3s var(--n-bezier),
 background-color .3s var(--n-bezier),
 color .3s var(--n-bezier);
`,[V(E("&","--n-merged-color: var(--n-color-modal);")),_(E("&","--n-merged-color: var(--n-color-popover);")),E("img",`
 width: 100%;
 height: 100%;
 `),k("text",`
 white-space: nowrap;
 display: inline-block;
 position: absolute;
 left: 50%;
 top: 50%;
 `),T("icon",`
 vertical-align: bottom;
 font-size: calc(var(--n-merged-size) - 6px);
 `),k("text","line-height: 1.25")]),ae=Object.assign(Object.assign({},$.props),{size:[String,Number],src:String,circle:{type:Boolean,default:void 0},objectFit:String,round:{type:Boolean,default:void 0},bordered:{type:Boolean,default:void 0},onError:Function,fallbackSrc:String,intersectionObserverOptions:Object,lazy:Boolean,onLoad:Function,renderPlaceholder:Function,renderFallback:Function,imgProps:Object,color:String}),le=K({name:"Avatar",props:ae,slots:Object,setup(o){const{mergedClsPrefixRef:l,inlineThemeDisabled:m}=X(o),g=p(!1);let d=null;const c=p(null),s=p(null),R=()=>{const{value:e}=c;if(e&&(d===null||d!==e.innerHTML)){d=e.innerHTML;const{value:r}=s;if(r){const{offsetWidth:t,offsetHeight:a}=r,{offsetWidth:n,offsetHeight:j}=e,x=.9,P=Math.min(t/n*x,a/j*x,1);e.style.transform=`translateX(-50%) translateY(-50%) scale(${P})`}}},b=H(te,null),i=O(()=>{const{size:e}=o;if(e)return e;const{size:r}=b||{};return r||"medium"}),u=$("Avatar","-avatar",ne,Y,o,l),z=H(Z,null),f=O(()=>{if(b)return!0;const{round:e,circle:r}=o;return e!==void 0||r!==void 0?e||r:z?z.roundRef.value:!1}),v=O(()=>b?!0:o.bordered||!1),F=O(()=>{const e=i.value,r=f.value,t=v.value,{color:a}=o,{self:{borderRadius:n,fontSize:j,color:x,border:P,colorModal:w,colorPopover:B},common:{cubicBezierEaseInOut:I}}=u.value;let S;return typeof e=="number"?S=`${e}px`:S=u.value.self[oe("height",e)],{"--n-font-size":j,"--n-border":t?P:"none","--n-border-radius":r?"50%":n,"--n-color":a||x,"--n-color-modal":a||w,"--n-color-popover":a||B,"--n-bezier":I,"--n-merged-size":`var(--n-avatar-size-override, ${S})`}}),h=m?D("avatar",O(()=>{const e=i.value,r=f.value,t=v.value,{color:a}=o;let n="";return e&&(typeof e=="number"?n+=`a${e}`:n+=e[0]),r&&(n+="b"),t&&(n+="c"),a&&(n+=q(a)),n}),F,o):void 0,L=p(!o.lazy);J(()=>{if(o.lazy&&o.intersectionObserverOptions){let e;const r=Q(()=>{e?.(),e=void 0,o.lazy&&(e=M(s.value,o.intersectionObserverOptions,L))});ee(()=>{r(),e?.()})}}),re(()=>{var e;return o.src||((e=o.imgProps)===null||e===void 0?void 0:e.src)},()=>{g.value=!1});const C=p(!o.lazy);return{textRef:c,selfRef:s,mergedRoundRef:f,mergedClsPrefix:l,fitTextTransform:R,cssVars:m?void 0:F,themeClass:h?.themeClass,onRender:h?.onRender,hasLoadError:g,shouldStartLoading:L,loaded:C,mergedOnError:e=>{if(!L.value)return;g.value=!0;const{onError:r,imgProps:{onError:t}={}}=o;r?.(e),t?.(e)},mergedOnLoad:e=>{const{onLoad:r,imgProps:{onLoad:t}={}}=o;r?.(e),t?.(e),C.value=!0}}},render(){var o,l;const{$slots:m,src:g,mergedClsPrefix:d,lazy:c,onRender:s,loaded:R,hasLoadError:b,imgProps:i={}}=this;s?.();let u;const z=!R&&!b&&(this.renderPlaceholder?this.renderPlaceholder():(l=(o=this.$slots).placeholder)===null||l===void 0?void 0:l.call(o));return this.hasLoadError?u=this.renderFallback?this.renderFallback():A(m.fallback,()=>[y("img",{src:this.fallbackSrc,style:{objectFit:this.objectFit}})]):u=G(m.default,f=>{if(f)return y(U,{onResize:this.fitTextTransform},{default:()=>y("span",{ref:"textRef",class:`${d}-avatar__text`},f)});if(g||i.src){const v=this.src||i.src;return y("img",Object.assign(Object.assign({},i),{loading:W&&!this.intersectionObserverOptions&&c?"lazy":"eager",src:c&&this.intersectionObserverOptions?this.shouldStartLoading?v:void 0:v,"data-image-src":v,onLoad:this.mergedOnLoad,onError:this.mergedOnError,style:[i.style||"",{objectFit:this.objectFit},z?{height:"0",width:"0",visibility:"hidden",position:"absolute"}:""]}))}}),y("span",{ref:"selfRef",class:[`${d}-avatar`,this.themeClass],style:this.cssVars},u,c&&z)}});export{le as N};
