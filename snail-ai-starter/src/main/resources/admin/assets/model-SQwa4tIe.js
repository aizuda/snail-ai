import{aX as We,bs as qe,L as $,J as n,K as m,M as U,bt as fe,b6 as Je,b7 as Qe,bu as Ze,d as eo,Q as v,bv as oo,bw as to,a3 as ao,bx as no,by as oe,T as ro,U as io,W as pe,bz as lo,I as C,a$ as so,ak as ve,ay as ee,al as co,a7 as me,bA as uo,aa as ho,h as x,ac as O,bB as _,bC as L,ad as p}from"./index-W5wXddbt.js";function fo(t){const i="rgba(0, 0, 0, .85)",y="0 2px 8px 0 rgba(0, 0, 0, 0.12)",{railColor:h,primaryColor:s,baseColor:d,cardColor:M,modalColor:R,popoverColor:K,borderRadius:X,fontSize:B,opacityDisabled:V}=t;return Object.assign(Object.assign({},qe),{fontSize:B,markFontSize:B,railColor:h,railColorHover:h,fillColor:s,fillColorHover:s,opacityDisabled:V,handleColor:"#FFF",dotColor:M,dotColorModal:R,dotColorPopover:K,handleBoxShadow:"0 1px 4px 0 rgba(0, 0, 0, 0.3), inset 0 0 1px 0 rgba(0, 0, 0, 0.05)",handleBoxShadowHover:"0 1px 4px 0 rgba(0, 0, 0, 0.3), inset 0 0 1px 0 rgba(0, 0, 0, 0.05)",handleBoxShadowActive:"0 1px 4px 0 rgba(0, 0, 0, 0.3), inset 0 0 1px 0 rgba(0, 0, 0, 0.05)",handleBoxShadowFocus:"0 1px 4px 0 rgba(0, 0, 0, 0.3), inset 0 0 1px 0 rgba(0, 0, 0, 0.05)",indicatorColor:i,indicatorBoxShadow:y,indicatorTextColor:d,indicatorBorderRadius:X,dotBorder:`2px solid ${h}`,dotBorderActive:`2px solid ${s}`,dotBoxShadow:""})}const vo={common:We,self:fo},mo=$([n("slider",`
 display: block;
 padding: calc((var(--n-handle-size) - var(--n-rail-height)) / 2) 0;
 position: relative;
 z-index: 0;
 width: 100%;
 cursor: pointer;
 user-select: none;
 -webkit-user-select: none;
 `,[m("reverse",[n("slider-handles",[n("slider-handle-wrapper",`
 transform: translate(50%, -50%);
 `)]),n("slider-dots",[n("slider-dot",`
 transform: translateX(50%, -50%);
 `)]),m("vertical",[n("slider-handles",[n("slider-handle-wrapper",`
 transform: translate(-50%, -50%);
 `)]),n("slider-marks",[n("slider-mark",`
 transform: translateY(calc(-50% + var(--n-dot-height) / 2));
 `)]),n("slider-dots",[n("slider-dot",`
 transform: translateX(-50%) translateY(0);
 `)])])]),m("vertical",`
 box-sizing: content-box;
 padding: 0 calc((var(--n-handle-size) - var(--n-rail-height)) / 2);
 width: var(--n-rail-width-vertical);
 height: 100%;
 `,[n("slider-handles",`
 top: calc(var(--n-handle-size) / 2);
 right: 0;
 bottom: calc(var(--n-handle-size) / 2);
 left: 0;
 `,[n("slider-handle-wrapper",`
 top: unset;
 left: 50%;
 transform: translate(-50%, 50%);
 `)]),n("slider-rail",`
 height: 100%;
 `,[U("fill",`
 top: unset;
 right: 0;
 bottom: unset;
 left: 0;
 `)]),m("with-mark",`
 width: var(--n-rail-width-vertical);
 margin: 0 32px 0 8px;
 `),n("slider-marks",`
 top: calc(var(--n-handle-size) / 2);
 right: unset;
 bottom: calc(var(--n-handle-size) / 2);
 left: 22px;
 font-size: var(--n-mark-font-size);
 `,[n("slider-mark",`
 transform: translateY(50%);
 white-space: nowrap;
 `)]),n("slider-dots",`
 top: calc(var(--n-handle-size) / 2);
 right: unset;
 bottom: calc(var(--n-handle-size) / 2);
 left: 50%;
 `,[n("slider-dot",`
 transform: translateX(-50%) translateY(50%);
 `)])]),m("disabled",`
 cursor: not-allowed;
 opacity: var(--n-opacity-disabled);
 `,[n("slider-handle",`
 cursor: not-allowed;
 `)]),m("with-mark",`
 width: 100%;
 margin: 8px 0 32px 0;
 `),$("&:hover",[n("slider-rail",{backgroundColor:"var(--n-rail-color-hover)"},[U("fill",{backgroundColor:"var(--n-fill-color-hover)"})]),n("slider-handle",{boxShadow:"var(--n-handle-box-shadow-hover)"})]),m("active",[n("slider-rail",{backgroundColor:"var(--n-rail-color-hover)"},[U("fill",{backgroundColor:"var(--n-fill-color-hover)"})]),n("slider-handle",{boxShadow:"var(--n-handle-box-shadow-hover)"})]),n("slider-marks",`
 position: absolute;
 top: 18px;
 left: calc(var(--n-handle-size) / 2);
 right: calc(var(--n-handle-size) / 2);
 `,[n("slider-mark",`
 position: absolute;
 transform: translateX(-50%);
 white-space: nowrap;
 `)]),n("slider-rail",`
 width: 100%;
 position: relative;
 height: var(--n-rail-height);
 background-color: var(--n-rail-color);
 transition: background-color .3s var(--n-bezier);
 border-radius: calc(var(--n-rail-height) / 2);
 `,[U("fill",`
 position: absolute;
 top: 0;
 bottom: 0;
 border-radius: calc(var(--n-rail-height) / 2);
 transition: background-color .3s var(--n-bezier);
 background-color: var(--n-fill-color);
 `)]),n("slider-handles",`
 position: absolute;
 top: 0;
 right: calc(var(--n-handle-size) / 2);
 bottom: 0;
 left: calc(var(--n-handle-size) / 2);
 `,[n("slider-handle-wrapper",`
 outline: none;
 position: absolute;
 top: 50%;
 transform: translate(-50%, -50%);
 cursor: pointer;
 display: flex;
 `,[n("slider-handle",`
 height: var(--n-handle-size);
 width: var(--n-handle-size);
 border-radius: 50%;
 overflow: hidden;
 transition: box-shadow .2s var(--n-bezier), background-color .3s var(--n-bezier);
 background-color: var(--n-handle-color);
 box-shadow: var(--n-handle-box-shadow);
 `,[$("&:hover",`
 box-shadow: var(--n-handle-box-shadow-hover);
 `)]),$("&:focus",[n("slider-handle",`
 box-shadow: var(--n-handle-box-shadow-focus);
 `,[$("&:hover",`
 box-shadow: var(--n-handle-box-shadow-active);
 `)])])])]),n("slider-dots",`
 position: absolute;
 top: 50%;
 left: calc(var(--n-handle-size) / 2);
 right: calc(var(--n-handle-size) / 2);
 `,[m("transition-disabled",[n("slider-dot","transition: none;")]),n("slider-dot",`
 transition:
 border-color .3s var(--n-bezier),
 box-shadow .3s var(--n-bezier),
 background-color .3s var(--n-bezier);
 position: absolute;
 transform: translate(-50%, -50%);
 height: var(--n-dot-height);
 width: var(--n-dot-width);
 border-radius: var(--n-dot-border-radius);
 overflow: hidden;
 box-sizing: border-box;
 border: var(--n-dot-border);
 background-color: var(--n-dot-color);
 `,[m("active","border: var(--n-dot-border-active);")])])]),n("slider-handle-indicator",`
 font-size: var(--n-font-size);
 padding: 6px 10px;
 border-radius: var(--n-indicator-border-radius);
 color: var(--n-indicator-text-color);
 background-color: var(--n-indicator-color);
 box-shadow: var(--n-indicator-box-shadow);
 `,[fe()]),n("slider-handle-indicator",`
 font-size: var(--n-font-size);
 padding: 6px 10px;
 border-radius: var(--n-indicator-border-radius);
 color: var(--n-indicator-text-color);
 background-color: var(--n-indicator-color);
 box-shadow: var(--n-indicator-box-shadow);
 `,[m("top",`
 margin-bottom: 12px;
 `),m("right",`
 margin-left: 12px;
 `),m("bottom",`
 margin-top: 12px;
 `),m("left",`
 margin-right: 12px;
 `),fe()]),Je(n("slider",[n("slider-dot","background-color: var(--n-dot-color-modal);")])),Qe(n("slider",[n("slider-dot","background-color: var(--n-dot-color-popover);")]))]);function be(t){return window.TouchEvent&&t instanceof window.TouchEvent}function ge(){const t=new Map,i=y=>h=>{t.set(y,h)};return Ze(()=>{t.clear()}),[t,i]}const bo=0,go=Object.assign(Object.assign({},pe.props),{to:oe.propTo,defaultValue:{type:[Number,Array],default:0},marks:Object,disabled:{type:Boolean,default:void 0},formatTooltip:Function,keyboard:{type:Boolean,default:!0},min:{type:Number,default:0},max:{type:Number,default:100},step:{type:[Number,String],default:1},range:Boolean,value:[Number,Array],placement:String,showTooltip:{type:Boolean,default:void 0},tooltip:{type:Boolean,default:!0},vertical:Boolean,reverse:Boolean,"onUpdate:value":[Function,Array],onUpdateValue:[Function,Array],onDragstart:[Function],onDragend:[Function]}),wo=eo({name:"Slider",props:go,slots:Object,setup(t){const{mergedClsPrefixRef:i,namespaceRef:y,inlineThemeDisabled:h}=io(t),s=pe("Slider","-slider",mo,vo,t,i),d=C(null),[M,R]=ge(),[K,X]=ge(),B=C(new Set),V=lo(t),{mergedDisabledRef:F}=V,te=x(()=>{const{step:e}=t;if(Number(e)<=0||e==="mark")return 0;const o=e.toString();let a=0;return o.includes(".")&&(a=o.length-o.indexOf(".")-1),a}),Y=C(t.defaultValue),we=ho(t,"value"),G=so(we,Y),b=x(()=>{const{value:e}=G;return(t.range?e:[e]).map(de)}),ae=x(()=>b.value.length>2),xe=x(()=>t.placement===void 0?t.vertical?"right":"top":t.placement),ne=x(()=>{const{marks:e}=t;return e?Object.keys(e).map(Number.parseFloat):null}),g=C(-1),re=C(-1),z=C(-1),S=C(!1),I=C(!1),W=x(()=>{const{vertical:e,reverse:o}=t;return e?o?"top":"bottom":o?"right":"left"}),ye=x(()=>{if(ae.value)return;const e=b.value,o=H(t.range?Math.min(...e):t.min),a=H(t.range?Math.max(...e):e[0]),{value:r}=W;return t.vertical?{[r]:`${o}%`,height:`${a-o}%`}:{[r]:`${o}%`,width:`${a-o}%`}}),ke=x(()=>{const e=[],{marks:o}=t;if(o){const a=b.value.slice();a.sort((f,u)=>f-u);const{value:r}=W,{value:l}=ae,{range:c}=t,w=l?()=>!1:f=>c?f>=a[0]&&f<=a[a.length-1]:f<=a[0];for(const f of Object.keys(o)){const u=Number(f);e.push({active:w(u),key:u,label:o[f],style:{[r]:`${H(u)}%`}})}}return e});function Ce(e,o){const a=H(e),{value:r}=W;return{[r]:`${a}%`,zIndex:o===g.value?1:0}}function ie(e){return t.showTooltip||z.value===e||g.value===e&&S.value}function Re(e){return S.value?!(g.value===e&&re.value===e):!0}function ze(e){var o;~e&&(g.value=e,(o=M.get(e))===null||o===void 0||o.focus())}function Se(){K.forEach((e,o)=>{ie(o)&&e.syncPosition()})}function le(e){const{"onUpdate:value":o,onUpdateValue:a}=t,{nTriggerFormInput:r,nTriggerFormChange:l}=V;a&&O(a,e),o&&O(o,e),Y.value=e,r(),l()}function se(e){const{range:o}=t;if(o){if(Array.isArray(e)){const{value:a}=b;e.join()!==a.join()&&le(e)}}else Array.isArray(e)||b.value[0]!==e&&le(e)}function q(e,o){if(t.range){const a=b.value.slice();a.splice(o,1,e),se(a)}else se(e)}function J(e,o,a){const r=a!==void 0;a||(a=e-o>0?1:-1);const l=ne.value||[],{step:c}=t;if(c==="mark"){const u=A(e,l.concat(o),r?a:void 0);return u?u.value:o}if(c<=0)return o;const{value:w}=te;let f;if(r){const u=Number((o/c).toFixed(w)),k=Math.floor(u),Q=u>k?k:k-1,Z=u<k?k:k+1;f=A(o,[Number((Q*c).toFixed(w)),Number((Z*c).toFixed(w)),...l],a)}else{const u=Te(e);f=A(e,[...l,u])}return f?de(f.value):o}function de(e){return Math.min(t.max,Math.max(t.min,e))}function H(e){const{max:o,min:a}=t;return(e-a)/(o-a)*100}function Me(e){const{max:o,min:a}=t;return a+(o-a)*e}function Te(e){const{step:o,min:a}=t;if(Number(o)<=0||o==="mark")return e;const r=Math.round((e-a)/o)*o+a;return Number(r.toFixed(te.value))}function A(e,o=ne.value,a){if(!o?.length)return null;let r=null,l=-1;for(;++l<o.length;){const c=o[l]-e,w=Math.abs(c);(a===void 0||c*a>0)&&(r===null||w<r.distance)&&(r={index:l,distance:w,value:o[l]})}return r}function ce(e){const o=d.value;if(!o)return;const a=be(e)?e.touches[0]:e,r=o.getBoundingClientRect();let l;return t.vertical?l=(r.bottom-a.clientY)/r.height:l=(a.clientX-r.left)/r.width,t.reverse&&(l=1-l),Me(l)}function De(e){if(F.value||!t.keyboard)return;const{vertical:o,reverse:a}=t;switch(e.key){case"ArrowUp":e.preventDefault(),E(o&&a?-1:1);break;case"ArrowRight":e.preventDefault(),E(!o&&a?-1:1);break;case"ArrowDown":e.preventDefault(),E(o&&a?1:-1);break;case"ArrowLeft":e.preventDefault(),E(!o&&a?1:-1);break}}function E(e){const o=g.value;if(o===-1)return;const{step:a}=t,r=b.value[o],l=Number(a)<=0||a==="mark"?r:r+a*e;q(J(l,r,e>0?1:-1),o)}function $e(e){var o,a;if(F.value||!be(e)&&e.button!==bo)return;const r=ce(e);if(r===void 0)return;const l=b.value.slice(),c=t.range?(a=(o=A(r,l))===null||o===void 0?void 0:o.index)!==null&&a!==void 0?a:-1:0;c!==-1&&(e.preventDefault(),ze(c),Be(),q(J(r,b.value[c]),c))}function Be(){S.value||(S.value=!0,t.onDragstart&&O(t.onDragstart),_("touchend",document,j),_("mouseup",document,j),_("touchmove",document,P),_("mousemove",document,P))}function N(){S.value&&(S.value=!1,t.onDragend&&O(t.onDragend),L("touchend",document,j),L("mouseup",document,j),L("touchmove",document,P),L("mousemove",document,P))}function P(e){const{value:o}=g;if(!S.value||o===-1){N();return}const a=ce(e);a!==void 0&&q(J(a,b.value[o]),o)}function j(){N()}function Ve(e){g.value=e,F.value||(z.value=e)}function Fe(e){g.value===e&&(g.value=-1,N()),z.value===e&&(z.value=-1)}function Ie(e){z.value=e}function He(e){z.value===e&&(z.value=-1)}ve(g,(e,o)=>{ee(()=>re.value=o)}),ve(G,()=>{if(t.marks){if(I.value)return;I.value=!0,ee(()=>{I.value=!1})}ee(Se)}),co(()=>{N()});const ue=x(()=>{const{self:{markFontSize:e,railColor:o,railColorHover:a,fillColor:r,fillColorHover:l,handleColor:c,opacityDisabled:w,dotColor:f,dotColorModal:u,handleBoxShadow:k,handleBoxShadowHover:Q,handleBoxShadowActive:Z,handleBoxShadowFocus:Ae,dotBorder:Ee,dotBoxShadow:Ne,railHeight:Pe,railWidthVertical:je,handleSize:Ue,dotHeight:Oe,dotWidth:_e,dotBorderRadius:Le,fontSize:Ke,dotBorderActive:Xe,dotColorPopover:Ye},common:{cubicBezierEaseInOut:Ge}}=s.value;return{"--n-bezier":Ge,"--n-dot-border":Ee,"--n-dot-border-active":Xe,"--n-dot-border-radius":Le,"--n-dot-box-shadow":Ne,"--n-dot-color":f,"--n-dot-color-modal":u,"--n-dot-color-popover":Ye,"--n-dot-height":Oe,"--n-dot-width":_e,"--n-fill-color":r,"--n-fill-color-hover":l,"--n-font-size":Ke,"--n-handle-box-shadow":k,"--n-handle-box-shadow-active":Z,"--n-handle-box-shadow-focus":Ae,"--n-handle-box-shadow-hover":Q,"--n-handle-color":c,"--n-handle-size":Ue,"--n-opacity-disabled":w,"--n-rail-color":o,"--n-rail-color-hover":a,"--n-rail-height":Pe,"--n-rail-width-vertical":je,"--n-mark-font-size":e}}),T=h?me("slider",void 0,ue,t):void 0,he=x(()=>{const{self:{fontSize:e,indicatorColor:o,indicatorBoxShadow:a,indicatorTextColor:r,indicatorBorderRadius:l}}=s.value;return{"--n-font-size":e,"--n-indicator-border-radius":l,"--n-indicator-box-shadow":a,"--n-indicator-color":o,"--n-indicator-text-color":r}}),D=h?me("slider-indicator",void 0,he,t):void 0;return{mergedClsPrefix:i,namespace:y,uncontrolledValue:Y,mergedValue:G,mergedDisabled:F,mergedPlacement:xe,isMounted:uo(),adjustedTo:oe(t),dotTransitionDisabled:I,markInfos:ke,isShowTooltip:ie,shouldKeepTooltipTransition:Re,handleRailRef:d,setHandleRefs:R,setFollowerRefs:X,fillStyle:ye,getHandleStyle:Ce,activeIndex:g,arrifiedValues:b,followerEnabledIndexSet:B,handleRailMouseDown:$e,handleHandleFocus:Ve,handleHandleBlur:Fe,handleHandleMouseEnter:Ie,handleHandleMouseLeave:He,handleRailKeyDown:De,indicatorCssVars:h?void 0:he,indicatorThemeClass:D?.themeClass,indicatorOnRender:D?.onRender,cssVars:h?void 0:ue,themeClass:T?.themeClass,onRender:T?.onRender}},render(){var t;const{mergedClsPrefix:i,themeClass:y,formatTooltip:h}=this;return(t=this.onRender)===null||t===void 0||t.call(this),v("div",{class:[`${i}-slider`,y,{[`${i}-slider--disabled`]:this.mergedDisabled,[`${i}-slider--active`]:this.activeIndex!==-1,[`${i}-slider--with-mark`]:this.marks,[`${i}-slider--vertical`]:this.vertical,[`${i}-slider--reverse`]:this.reverse}],style:this.cssVars,onKeydown:this.handleRailKeyDown,onMousedown:this.handleRailMouseDown,onTouchstart:this.handleRailMouseDown},v("div",{class:`${i}-slider-rail`},v("div",{class:`${i}-slider-rail__fill`,style:this.fillStyle}),this.marks?v("div",{class:[`${i}-slider-dots`,this.dotTransitionDisabled&&`${i}-slider-dots--transition-disabled`]},this.markInfos.map(s=>v("div",{key:s.key,class:[`${i}-slider-dot`,{[`${i}-slider-dot--active`]:s.active}],style:s.style}))):null,v("div",{ref:"handleRailRef",class:`${i}-slider-handles`},this.arrifiedValues.map((s,d)=>{const M=this.isShowTooltip(d);return v(oo,null,{default:()=>[v(to,null,{default:()=>v("div",{ref:this.setHandleRefs(d),class:`${i}-slider-handle-wrapper`,tabindex:this.mergedDisabled?-1:0,role:"slider","aria-valuenow":s,"aria-valuemin":this.min,"aria-valuemax":this.max,"aria-orientation":this.vertical?"vertical":"horizontal","aria-disabled":this.disabled,style:this.getHandleStyle(s,d),onFocus:()=>{this.handleHandleFocus(d)},onBlur:()=>{this.handleHandleBlur(d)},onMouseenter:()=>{this.handleHandleMouseEnter(d)},onMouseleave:()=>{this.handleHandleMouseLeave(d)}},ao(this.$slots.thumb,()=>[v("div",{class:`${i}-slider-handle`})]))}),this.tooltip&&v(no,{ref:this.setFollowerRefs(d),show:M,to:this.adjustedTo,enabled:this.showTooltip&&!this.range||this.followerEnabledIndexSet.has(d),teleportDisabled:this.adjustedTo===oe.tdkey,placement:this.mergedPlacement,containerClass:this.namespace},{default:()=>v(ro,{name:"fade-in-scale-up-transition",appear:this.isMounted,css:this.shouldKeepTooltipTransition(d),onEnter:()=>{this.followerEnabledIndexSet.add(d)},onAfterLeave:()=>{this.followerEnabledIndexSet.delete(d)}},{default:()=>{var R;return M?((R=this.indicatorOnRender)===null||R===void 0||R.call(this),v("div",{class:[`${i}-slider-handle-indicator`,this.indicatorThemeClass,`${i}-slider-handle-indicator--${this.mergedPlacement}`],style:this.indicatorCssVars},typeof h=="function"?h(s):s)):null}})})]})})),this.marks?v("div",{class:`${i}-slider-marks`},this.markInfos.map(s=>v("div",{key:s.key,class:`${i}-slider-mark`,style:s.style},typeof s.label=="function"?s.label():s.label))):null))}});function xo(t){const{page:i,size:y,...h}=t,s={...h,pageNum:t.pageNum??i??1,pageSize:t.pageSize??y??10};return p({url:"/ai-model/configs",method:"get",params:s})}function yo(t){return p({url:`/ai-model/config/${t}`,method:"get"})}function ko(t){return p({url:"/ai-model/adapters",method:"get",params:{modelType:t}})}function Co(t){return p({url:"/ai-model/config",method:"post",data:t})}function Ro(t,i){return p({url:`/ai-model/config/${t}`,method:"put",data:i})}function zo(t){return p({url:`/ai-model/config/${t}`,method:"delete"})}function So(t){return p({url:`/ai-model/by-type/${t}`,method:"get"})}function Mo(t){return p({url:`/ai-model/switch-default/${t}`,method:"put"})}function To(t){return p({url:`/ai-model/config/${t}/enable`,method:"put"})}function Do(t){return p({url:`/ai-model/config/${t}/disable`,method:"put"})}function $o(t){return p({url:`/ai-model/config/${t}/test`,method:"post"})}export{wo as N,$o as a,Ro as b,Co as c,zo as d,xo as e,ko as f,Mo as g,To as h,Do as i,yo as j,So as k};
