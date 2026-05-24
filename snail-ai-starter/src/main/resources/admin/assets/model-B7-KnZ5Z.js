import{K as Ge,bt as We,L as V,M as n,X as b,Y as j,bu as fe,aU as Je,aV as Qe,bv as Ze,d as eo,O as v,bw as oo,bx as to,aa as ao,by as no,bz as oe,T as ro,S as io,U as pe,bA as lo,I as k,bB as so,am as ve,aA as ee,an as co,ad as be,bC as uo,ag as ho,h as w,ah as O,bD as _,bE as K,aK as C}from"./index-2qcSDCiN.js";function fo(t){const i="rgba(0, 0, 0, .85)",x="0 2px 8px 0 rgba(0, 0, 0, 0.12)",{railColor:h,primaryColor:s,baseColor:d,cardColor:M,modalColor:R,popoverColor:L,borderRadius:X,fontSize:$,opacityDisabled:B}=t;return Object.assign(Object.assign({},We),{fontSize:$,markFontSize:$,railColor:h,railColorHover:h,fillColor:s,fillColorHover:s,opacityDisabled:B,handleColor:"#FFF",dotColor:M,dotColorModal:R,dotColorPopover:L,handleBoxShadow:"0 1px 4px 0 rgba(0, 0, 0, 0.3), inset 0 0 1px 0 rgba(0, 0, 0, 0.05)",handleBoxShadowHover:"0 1px 4px 0 rgba(0, 0, 0, 0.3), inset 0 0 1px 0 rgba(0, 0, 0, 0.05)",handleBoxShadowActive:"0 1px 4px 0 rgba(0, 0, 0, 0.3), inset 0 0 1px 0 rgba(0, 0, 0, 0.05)",handleBoxShadowFocus:"0 1px 4px 0 rgba(0, 0, 0, 0.3), inset 0 0 1px 0 rgba(0, 0, 0, 0.05)",indicatorColor:i,indicatorBoxShadow:x,indicatorTextColor:d,indicatorBorderRadius:X,dotBorder:`2px solid ${h}`,dotBorderActive:`2px solid ${s}`,dotBoxShadow:""})}const vo={common:Ge,self:fo},bo=V([n("slider",`
 display: block;
 padding: calc((var(--n-handle-size) - var(--n-rail-height)) / 2) 0;
 position: relative;
 z-index: 0;
 width: 100%;
 cursor: pointer;
 user-select: none;
 -webkit-user-select: none;
 `,[b("reverse",[n("slider-handles",[n("slider-handle-wrapper",`
 transform: translate(50%, -50%);
 `)]),n("slider-dots",[n("slider-dot",`
 transform: translateX(50%, -50%);
 `)]),b("vertical",[n("slider-handles",[n("slider-handle-wrapper",`
 transform: translate(-50%, -50%);
 `)]),n("slider-marks",[n("slider-mark",`
 transform: translateY(calc(-50% + var(--n-dot-height) / 2));
 `)]),n("slider-dots",[n("slider-dot",`
 transform: translateX(-50%) translateY(0);
 `)])])]),b("vertical",`
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
 `,[j("fill",`
 top: unset;
 right: 0;
 bottom: unset;
 left: 0;
 `)]),b("with-mark",`
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
 `)])]),b("disabled",`
 cursor: not-allowed;
 opacity: var(--n-opacity-disabled);
 `,[n("slider-handle",`
 cursor: not-allowed;
 `)]),b("with-mark",`
 width: 100%;
 margin: 8px 0 32px 0;
 `),V("&:hover",[n("slider-rail",{backgroundColor:"var(--n-rail-color-hover)"},[j("fill",{backgroundColor:"var(--n-fill-color-hover)"})]),n("slider-handle",{boxShadow:"var(--n-handle-box-shadow-hover)"})]),b("active",[n("slider-rail",{backgroundColor:"var(--n-rail-color-hover)"},[j("fill",{backgroundColor:"var(--n-fill-color-hover)"})]),n("slider-handle",{boxShadow:"var(--n-handle-box-shadow-hover)"})]),n("slider-marks",`
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
 `,[j("fill",`
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
 `,[V("&:hover",`
 box-shadow: var(--n-handle-box-shadow-hover);
 `)]),V("&:focus",[n("slider-handle",`
 box-shadow: var(--n-handle-box-shadow-focus);
 `,[V("&:hover",`
 box-shadow: var(--n-handle-box-shadow-active);
 `)])])])]),n("slider-dots",`
 position: absolute;
 top: 50%;
 left: calc(var(--n-handle-size) / 2);
 right: calc(var(--n-handle-size) / 2);
 `,[b("transition-disabled",[n("slider-dot","transition: none;")]),n("slider-dot",`
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
 `,[b("active","border: var(--n-dot-border-active);")])])]),n("slider-handle-indicator",`
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
 `,[b("top",`
 margin-bottom: 12px;
 `),b("right",`
 margin-left: 12px;
 `),b("bottom",`
 margin-top: 12px;
 `),b("left",`
 margin-right: 12px;
 `),fe()]),Je(n("slider",[n("slider-dot","background-color: var(--n-dot-color-modal);")])),Qe(n("slider",[n("slider-dot","background-color: var(--n-dot-color-popover);")]))]);function me(t){return window.TouchEvent&&t instanceof window.TouchEvent}function ge(){const t=new Map,i=x=>h=>{t.set(x,h)};return Ze(()=>{t.clear()}),[t,i]}const mo=0,go=Object.assign(Object.assign({},pe.props),{to:oe.propTo,defaultValue:{type:[Number,Array],default:0},marks:Object,disabled:{type:Boolean,default:void 0},formatTooltip:Function,keyboard:{type:Boolean,default:!0},min:{type:Number,default:0},max:{type:Number,default:100},step:{type:[Number,String],default:1},range:Boolean,value:[Number,Array],placement:String,showTooltip:{type:Boolean,default:void 0},tooltip:{type:Boolean,default:!0},vertical:Boolean,reverse:Boolean,"onUpdate:value":[Function,Array],onUpdateValue:[Function,Array],onDragstart:[Function],onDragend:[Function]}),wo=eo({name:"Slider",props:go,slots:Object,setup(t){const{mergedClsPrefixRef:i,namespaceRef:x,inlineThemeDisabled:h}=io(t),s=pe("Slider","-slider",bo,vo,t,i),d=k(null),[M,R]=ge(),[L,X]=ge(),$=k(new Set),B=lo(t),{mergedDisabledRef:F}=B,te=w(()=>{const{step:e}=t;if(Number(e)<=0||e==="mark")return 0;const o=e.toString();let a=0;return o.includes(".")&&(a=o.length-o.indexOf(".")-1),a}),Y=k(t.defaultValue),we=ho(t,"value"),q=so(we,Y),m=w(()=>{const{value:e}=q;return(t.range?e:[e]).map(de)}),ae=w(()=>m.value.length>2),xe=w(()=>t.placement===void 0?t.vertical?"right":"top":t.placement),ne=w(()=>{const{marks:e}=t;return e?Object.keys(e).map(Number.parseFloat):null}),g=k(-1),re=k(-1),S=k(-1),z=k(!1),I=k(!1),G=w(()=>{const{vertical:e,reverse:o}=t;return e?o?"top":"bottom":o?"right":"left"}),ye=w(()=>{if(ae.value)return;const e=m.value,o=H(t.range?Math.min(...e):t.min),a=H(t.range?Math.max(...e):e[0]),{value:r}=G;return t.vertical?{[r]:`${o}%`,height:`${a-o}%`}:{[r]:`${o}%`,width:`${a-o}%`}}),ke=w(()=>{const e=[],{marks:o}=t;if(o){const a=m.value.slice();a.sort((f,u)=>f-u);const{value:r}=G,{value:l}=ae,{range:c}=t,p=l?()=>!1:f=>c?f>=a[0]&&f<=a[a.length-1]:f<=a[0];for(const f of Object.keys(o)){const u=Number(f);e.push({active:p(u),key:u,label:o[f],style:{[r]:`${H(u)}%`}})}}return e});function Ce(e,o){const a=H(e),{value:r}=G;return{[r]:`${a}%`,zIndex:o===g.value?1:0}}function ie(e){return t.showTooltip||S.value===e||g.value===e&&z.value}function Re(e){return z.value?!(g.value===e&&re.value===e):!0}function Se(e){var o;~e&&(g.value=e,(o=M.get(e))===null||o===void 0||o.focus())}function ze(){L.forEach((e,o)=>{ie(o)&&e.syncPosition()})}function le(e){const{"onUpdate:value":o,onUpdateValue:a}=t,{nTriggerFormInput:r,nTriggerFormChange:l}=B;a&&O(a,e),o&&O(o,e),Y.value=e,r(),l()}function se(e){const{range:o}=t;if(o){if(Array.isArray(e)){const{value:a}=m;e.join()!==a.join()&&le(e)}}else Array.isArray(e)||m.value[0]!==e&&le(e)}function W(e,o){if(t.range){const a=m.value.slice();a.splice(o,1,e),se(a)}else se(e)}function J(e,o,a){const r=a!==void 0;a||(a=e-o>0?1:-1);const l=ne.value||[],{step:c}=t;if(c==="mark"){const u=A(e,l.concat(o),r?a:void 0);return u?u.value:o}if(c<=0)return o;const{value:p}=te;let f;if(r){const u=Number((o/c).toFixed(p)),y=Math.floor(u),Q=u>y?y:y-1,Z=u<y?y:y+1;f=A(o,[Number((Q*c).toFixed(p)),Number((Z*c).toFixed(p)),...l],a)}else{const u=Te(e);f=A(e,[...l,u])}return f?de(f.value):o}function de(e){return Math.min(t.max,Math.max(t.min,e))}function H(e){const{max:o,min:a}=t;return(e-a)/(o-a)*100}function Me(e){const{max:o,min:a}=t;return a+(o-a)*e}function Te(e){const{step:o,min:a}=t;if(Number(o)<=0||o==="mark")return e;const r=Math.round((e-a)/o)*o+a;return Number(r.toFixed(te.value))}function A(e,o=ne.value,a){if(!o?.length)return null;let r=null,l=-1;for(;++l<o.length;){const c=o[l]-e,p=Math.abs(c);(a===void 0||c*a>0)&&(r===null||p<r.distance)&&(r={index:l,distance:p,value:o[l]})}return r}function ce(e){const o=d.value;if(!o)return;const a=me(e)?e.touches[0]:e,r=o.getBoundingClientRect();let l;return t.vertical?l=(r.bottom-a.clientY)/r.height:l=(a.clientX-r.left)/r.width,t.reverse&&(l=1-l),Me(l)}function De(e){if(F.value||!t.keyboard)return;const{vertical:o,reverse:a}=t;switch(e.key){case"ArrowUp":e.preventDefault(),E(o&&a?-1:1);break;case"ArrowRight":e.preventDefault(),E(!o&&a?-1:1);break;case"ArrowDown":e.preventDefault(),E(o&&a?1:-1);break;case"ArrowLeft":e.preventDefault(),E(!o&&a?1:-1);break}}function E(e){const o=g.value;if(o===-1)return;const{step:a}=t,r=m.value[o],l=Number(a)<=0||a==="mark"?r:r+a*e;W(J(l,r,e>0?1:-1),o)}function Ve(e){var o,a;if(F.value||!me(e)&&e.button!==mo)return;const r=ce(e);if(r===void 0)return;const l=m.value.slice(),c=t.range?(a=(o=A(r,l))===null||o===void 0?void 0:o.index)!==null&&a!==void 0?a:-1:0;c!==-1&&(e.preventDefault(),Se(c),$e(),W(J(r,m.value[c]),c))}function $e(){z.value||(z.value=!0,t.onDragstart&&O(t.onDragstart),_("touchend",document,U),_("mouseup",document,U),_("touchmove",document,P),_("mousemove",document,P))}function N(){z.value&&(z.value=!1,t.onDragend&&O(t.onDragend),K("touchend",document,U),K("mouseup",document,U),K("touchmove",document,P),K("mousemove",document,P))}function P(e){const{value:o}=g;if(!z.value||o===-1){N();return}const a=ce(e);a!==void 0&&W(J(a,m.value[o]),o)}function U(){N()}function Be(e){g.value=e,F.value||(S.value=e)}function Fe(e){g.value===e&&(g.value=-1,N()),S.value===e&&(S.value=-1)}function Ie(e){S.value=e}function He(e){S.value===e&&(S.value=-1)}ve(g,(e,o)=>{ee(()=>re.value=o)}),ve(q,()=>{if(t.marks){if(I.value)return;I.value=!0,ee(()=>{I.value=!1})}ee(ze)}),co(()=>{N()});const ue=w(()=>{const{self:{markFontSize:e,railColor:o,railColorHover:a,fillColor:r,fillColorHover:l,handleColor:c,opacityDisabled:p,dotColor:f,dotColorModal:u,handleBoxShadow:y,handleBoxShadowHover:Q,handleBoxShadowActive:Z,handleBoxShadowFocus:Ae,dotBorder:Ee,dotBoxShadow:Ne,railHeight:Pe,railWidthVertical:Ue,handleSize:je,dotHeight:Oe,dotWidth:_e,dotBorderRadius:Ke,fontSize:Le,dotBorderActive:Xe,dotColorPopover:Ye},common:{cubicBezierEaseInOut:qe}}=s.value;return{"--n-bezier":qe,"--n-dot-border":Ee,"--n-dot-border-active":Xe,"--n-dot-border-radius":Ke,"--n-dot-box-shadow":Ne,"--n-dot-color":f,"--n-dot-color-modal":u,"--n-dot-color-popover":Ye,"--n-dot-height":Oe,"--n-dot-width":_e,"--n-fill-color":r,"--n-fill-color-hover":l,"--n-font-size":Le,"--n-handle-box-shadow":y,"--n-handle-box-shadow-active":Z,"--n-handle-box-shadow-focus":Ae,"--n-handle-box-shadow-hover":Q,"--n-handle-color":c,"--n-handle-size":je,"--n-opacity-disabled":p,"--n-rail-color":o,"--n-rail-color-hover":a,"--n-rail-height":Pe,"--n-rail-width-vertical":Ue,"--n-mark-font-size":e}}),T=h?be("slider",void 0,ue,t):void 0,he=w(()=>{const{self:{fontSize:e,indicatorColor:o,indicatorBoxShadow:a,indicatorTextColor:r,indicatorBorderRadius:l}}=s.value;return{"--n-font-size":e,"--n-indicator-border-radius":l,"--n-indicator-box-shadow":a,"--n-indicator-color":o,"--n-indicator-text-color":r}}),D=h?be("slider-indicator",void 0,he,t):void 0;return{mergedClsPrefix:i,namespace:x,uncontrolledValue:Y,mergedValue:q,mergedDisabled:F,mergedPlacement:xe,isMounted:uo(),adjustedTo:oe(t),dotTransitionDisabled:I,markInfos:ke,isShowTooltip:ie,shouldKeepTooltipTransition:Re,handleRailRef:d,setHandleRefs:R,setFollowerRefs:X,fillStyle:ye,getHandleStyle:Ce,activeIndex:g,arrifiedValues:m,followerEnabledIndexSet:$,handleRailMouseDown:Ve,handleHandleFocus:Be,handleHandleBlur:Fe,handleHandleMouseEnter:Ie,handleHandleMouseLeave:He,handleRailKeyDown:De,indicatorCssVars:h?void 0:he,indicatorThemeClass:D?.themeClass,indicatorOnRender:D?.onRender,cssVars:h?void 0:ue,themeClass:T?.themeClass,onRender:T?.onRender}},render(){var t;const{mergedClsPrefix:i,themeClass:x,formatTooltip:h}=this;return(t=this.onRender)===null||t===void 0||t.call(this),v("div",{class:[`${i}-slider`,x,{[`${i}-slider--disabled`]:this.mergedDisabled,[`${i}-slider--active`]:this.activeIndex!==-1,[`${i}-slider--with-mark`]:this.marks,[`${i}-slider--vertical`]:this.vertical,[`${i}-slider--reverse`]:this.reverse}],style:this.cssVars,onKeydown:this.handleRailKeyDown,onMousedown:this.handleRailMouseDown,onTouchstart:this.handleRailMouseDown},v("div",{class:`${i}-slider-rail`},v("div",{class:`${i}-slider-rail__fill`,style:this.fillStyle}),this.marks?v("div",{class:[`${i}-slider-dots`,this.dotTransitionDisabled&&`${i}-slider-dots--transition-disabled`]},this.markInfos.map(s=>v("div",{key:s.key,class:[`${i}-slider-dot`,{[`${i}-slider-dot--active`]:s.active}],style:s.style}))):null,v("div",{ref:"handleRailRef",class:`${i}-slider-handles`},this.arrifiedValues.map((s,d)=>{const M=this.isShowTooltip(d);return v(oo,null,{default:()=>[v(to,null,{default:()=>v("div",{ref:this.setHandleRefs(d),class:`${i}-slider-handle-wrapper`,tabindex:this.mergedDisabled?-1:0,role:"slider","aria-valuenow":s,"aria-valuemin":this.min,"aria-valuemax":this.max,"aria-orientation":this.vertical?"vertical":"horizontal","aria-disabled":this.disabled,style:this.getHandleStyle(s,d),onFocus:()=>{this.handleHandleFocus(d)},onBlur:()=>{this.handleHandleBlur(d)},onMouseenter:()=>{this.handleHandleMouseEnter(d)},onMouseleave:()=>{this.handleHandleMouseLeave(d)}},ao(this.$slots.thumb,()=>[v("div",{class:`${i}-slider-handle`})]))}),this.tooltip&&v(no,{ref:this.setFollowerRefs(d),show:M,to:this.adjustedTo,enabled:this.showTooltip&&!this.range||this.followerEnabledIndexSet.has(d),teleportDisabled:this.adjustedTo===oe.tdkey,placement:this.mergedPlacement,containerClass:this.namespace},{default:()=>v(ro,{name:"fade-in-scale-up-transition",appear:this.isMounted,css:this.shouldKeepTooltipTransition(d),onEnter:()=>{this.followerEnabledIndexSet.add(d)},onAfterLeave:()=>{this.followerEnabledIndexSet.delete(d)}},{default:()=>{var R;return M?((R=this.indicatorOnRender)===null||R===void 0||R.call(this),v("div",{class:[`${i}-slider-handle-indicator`,this.indicatorThemeClass,`${i}-slider-handle-indicator--${this.mergedPlacement}`],style:this.indicatorCssVars},typeof h=="function"?h(s):s)):null}})})]})})),this.marks?v("div",{class:`${i}-slider-marks`},this.markInfos.map(s=>v("div",{key:s.key,class:`${i}-slider-mark`,style:s.style},typeof s.label=="function"?s.label():s.label))):null))}});function xo(t){const{page:i,size:x,...h}=t,s={...h,pageNum:t.pageNum??i??1,pageSize:t.pageSize??x??10};return C({url:"/ai-model/configs",method:"get",params:s})}function yo(t){return C({url:"/ai-model/config",method:"post",data:t})}function ko(t,i){return C({url:`/ai-model/config/${t}`,method:"put",data:i})}function Co(t){return C({url:`/ai-model/config/${t}`,method:"delete"})}function Ro(t){return C({url:`/ai-model/by-type/${t}`,method:"get"})}function So(t){return C({url:`/ai-model/switch-default/${t}`,method:"put"})}function zo(t){return C({url:`/ai-model/config/${t}/enable`,method:"put"})}function Mo(t){return C({url:`/ai-model/config/${t}/disable`,method:"put"})}export{wo as N,yo as a,Co as b,xo as c,So as d,zo as e,ko as f,Mo as g,Ro as h};
