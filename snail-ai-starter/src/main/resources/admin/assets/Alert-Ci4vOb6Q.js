import{b3 as F,b$ as M,c0 as u,c1 as v,Q as I,U as i,R as y,bQ as N,S as V,d as Q,X as l,bu as D,c2 as K,a9 as U,a6 as X,bP as q,a0 as G,a2 as E,a1 as J,ad as Y,h as $,a8 as Z,c3 as oo,c4 as eo,c5 as ro,c6 as no,bR as so,ah as c,r as lo}from"./index-D0DF2D9J.js";function to(r){const{lineHeight:o,borderRadius:d,fontWeightStrong:f,baseColor:t,dividerColor:b,actionColor:S,textColor1:h,textColor2:s,closeColorHover:g,closeColorPressed:C,closeIconColor:m,closeIconColorHover:p,closeIconColorPressed:n,infoColor:e,successColor:x,warningColor:z,errorColor:P,fontSize:T}=r;return Object.assign(Object.assign({},M),{fontSize:T,lineHeight:o,titleFontWeight:f,borderRadius:d,border:`1px solid ${b}`,color:S,titleTextColor:h,iconColor:s,contentTextColor:s,closeBorderRadius:d,closeColorHover:g,closeColorPressed:C,closeIconColor:m,closeIconColorHover:p,closeIconColorPressed:n,borderInfo:`1px solid ${u(t,v(e,{alpha:.25}))}`,colorInfo:u(t,v(e,{alpha:.08})),titleTextColorInfo:h,iconColorInfo:e,contentTextColorInfo:s,closeColorHoverInfo:g,closeColorPressedInfo:C,closeIconColorInfo:m,closeIconColorHoverInfo:p,closeIconColorPressedInfo:n,borderSuccess:`1px solid ${u(t,v(x,{alpha:.25}))}`,colorSuccess:u(t,v(x,{alpha:.08})),titleTextColorSuccess:h,iconColorSuccess:x,contentTextColorSuccess:s,closeColorHoverSuccess:g,closeColorPressedSuccess:C,closeIconColorSuccess:m,closeIconColorHoverSuccess:p,closeIconColorPressedSuccess:n,borderWarning:`1px solid ${u(t,v(z,{alpha:.33}))}`,colorWarning:u(t,v(z,{alpha:.08})),titleTextColorWarning:h,iconColorWarning:z,contentTextColorWarning:s,closeColorHoverWarning:g,closeColorPressedWarning:C,closeIconColorWarning:m,closeIconColorHoverWarning:p,closeIconColorPressedWarning:n,borderError:`1px solid ${u(t,v(P,{alpha:.25}))}`,colorError:u(t,v(P,{alpha:.08})),titleTextColorError:h,iconColorError:P,contentTextColorError:s,closeColorHoverError:g,closeColorPressedError:C,closeIconColorError:m,closeIconColorHoverError:p,closeIconColorPressedError:n})}const io={common:F,self:to},ao=I("alert",`
 line-height: var(--n-line-height);
 border-radius: var(--n-border-radius);
 position: relative;
 transition: background-color .3s var(--n-bezier);
 background-color: var(--n-color);
 text-align: start;
 word-break: break-word;
`,[i("border",`
 border-radius: inherit;
 position: absolute;
 left: 0;
 right: 0;
 top: 0;
 bottom: 0;
 transition: border-color .3s var(--n-bezier);
 border: var(--n-border);
 pointer-events: none;
 `),y("closable",[I("alert-body",[i("title",`
 padding-right: 24px;
 `)])]),i("icon",{color:"var(--n-icon-color)"}),I("alert-body",{padding:"var(--n-padding)"},[i("title",{color:"var(--n-title-text-color)"}),i("content",{color:"var(--n-content-text-color)"})]),N({originalTransition:"transform .3s var(--n-bezier)",enterToProps:{transform:"scale(1)"},leaveToProps:{transform:"scale(0.9)"}}),i("icon",`
 position: absolute;
 left: 0;
 top: 0;
 align-items: center;
 justify-content: center;
 display: flex;
 width: var(--n-icon-size);
 height: var(--n-icon-size);
 font-size: var(--n-icon-size);
 margin: var(--n-icon-margin);
 `),i("close",`
 transition:
 color .3s var(--n-bezier),
 background-color .3s var(--n-bezier);
 position: absolute;
 right: 0;
 top: 0;
 margin: var(--n-close-margin);
 `),y("show-icon",[I("alert-body",{paddingLeft:"calc(var(--n-icon-margin-left) + var(--n-icon-size) + var(--n-icon-margin-right))"})]),y("right-adjust",[I("alert-body",{paddingRight:"calc(var(--n-close-size) + var(--n-padding) + 2px)"})]),I("alert-body",`
 border-radius: var(--n-border-radius);
 transition: border-color .3s var(--n-bezier);
 `,[i("title",`
 transition: color .3s var(--n-bezier);
 font-size: 16px;
 line-height: 19px;
 font-weight: var(--n-title-font-weight);
 `,[V("& +",[i("content",{marginTop:"9px"})])]),i("content",{transition:"color .3s var(--n-bezier)",fontSize:"var(--n-font-size)"})]),i("icon",{transition:"color .3s var(--n-bezier)"})]),co=Object.assign(Object.assign({},E.props),{title:String,showIcon:{type:Boolean,default:!0},type:{type:String,default:"default"},bordered:{type:Boolean,default:!0},closable:Boolean,onClose:Function,onAfterLeave:Function,onAfterHide:Function}),go=Q({name:"Alert",inheritAttrs:!1,props:co,slots:Object,setup(r){const{mergedClsPrefixRef:o,mergedBorderedRef:d,inlineThemeDisabled:f,mergedRtlRef:t}=G(r),b=E("Alert","-alert",ao,io,r,o),S=J("Alert",t,o),h=$(()=>{const{common:{cubicBezierEaseInOut:n},self:e}=b.value,{fontSize:x,borderRadius:z,titleFontWeight:P,lineHeight:T,iconSize:H,iconMargin:R,iconMarginRtl:W,closeIconSize:w,closeBorderRadius:A,closeSize:B,closeMargin:_,closeMarginRtl:j,padding:k}=e,{type:a}=r,{left:L,right:O}=so(R);return{"--n-bezier":n,"--n-color":e[c("color",a)],"--n-close-icon-size":w,"--n-close-border-radius":A,"--n-close-color-hover":e[c("closeColorHover",a)],"--n-close-color-pressed":e[c("closeColorPressed",a)],"--n-close-icon-color":e[c("closeIconColor",a)],"--n-close-icon-color-hover":e[c("closeIconColorHover",a)],"--n-close-icon-color-pressed":e[c("closeIconColorPressed",a)],"--n-icon-color":e[c("iconColor",a)],"--n-border":e[c("border",a)],"--n-title-text-color":e[c("titleTextColor",a)],"--n-content-text-color":e[c("contentTextColor",a)],"--n-line-height":T,"--n-border-radius":z,"--n-font-size":x,"--n-title-font-weight":P,"--n-icon-size":H,"--n-icon-margin":R,"--n-icon-margin-rtl":W,"--n-close-size":B,"--n-close-margin":_,"--n-close-margin-rtl":j,"--n-padding":k,"--n-icon-margin-left":L,"--n-icon-margin-right":O}}),s=f?Y("alert",$(()=>r.type[0]),h,r):void 0,g=lo(!0),C=()=>{const{onAfterLeave:n,onAfterHide:e}=r;n&&n(),e&&e()};return{rtlEnabled:S,mergedClsPrefix:o,mergedBordered:d,visible:g,handleCloseClick:()=>{var n;Promise.resolve((n=r.onClose)===null||n===void 0?void 0:n.call(r)).then(e=>{e!==!1&&(g.value=!1)})},handleAfterLeave:()=>{C()},mergedTheme:b,cssVars:f?void 0:h,themeClass:s?.themeClass,onRender:s?.onRender}},render(){var r;return(r=this.onRender)===null||r===void 0||r.call(this),l(q,{onAfterLeave:this.handleAfterLeave},{default:()=>{const{mergedClsPrefix:o,$slots:d}=this,f={class:[`${o}-alert`,this.themeClass,this.closable&&`${o}-alert--closable`,this.showIcon&&`${o}-alert--show-icon`,!this.title&&this.closable&&`${o}-alert--right-adjust`,this.rtlEnabled&&`${o}-alert--rtl`],style:this.cssVars,role:"alert"};return this.visible?l("div",Object.assign({},D(this.$attrs,f)),this.closable&&l(K,{clsPrefix:o,class:`${o}-alert__close`,onClick:this.handleCloseClick}),this.bordered&&l("div",{class:`${o}-alert__border`}),this.showIcon&&l("div",{class:`${o}-alert__icon`,"aria-hidden":"true"},U(d.icon,()=>[l(Z,{clsPrefix:o},{default:()=>{switch(this.type){case"success":return l(no,null);case"info":return l(ro,null);case"warning":return l(eo,null);case"error":return l(oo,null);default:return null}}})])),l("div",{class:[`${o}-alert-body`,this.mergedBordered&&`${o}-alert-body--bordered`]},X(d.header,t=>{const b=t||this.title;return b?l("div",{class:`${o}-alert-body__title`},b):null}),d.default&&l("div",{class:`${o}-alert-body__content`},d))):null}})}});export{go as N};
