import './style/global.scss';
// vite-plugin-svg-icons 的虚拟模块：副作用 import，运行时把生成好的雪碧图塞进 <body>，
// 取代原来 vite.config.ts 里手写的 insertSvg() transformIndexHtml
import 'virtual:svg-icons-register';
import { createApp } from 'vue';
import { createPinia } from 'pinia';
import App from './App.vue';
import router from './router';
import {LANG} from '@/utils/localStoreItem';
/* ElementPlus */
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import zhCn from 'element-plus/dist/locale/zh-cn.mjs';
import en from 'element-plus/dist/locale/en.mjs';
/* utils */
import SvgIcon from './components/svgIcon/index.vue';
import i18n from '@/language/i18n';
import {defaultLanguage} from '@/language/i18n';
import request from '@/utils/request';
import '@/utils/date';
/* markdown */
import VueMarkdownEditor from '@kangc/v-md-editor';
import '@kangc/v-md-editor/lib/style/base-editor.css';
import vuepressTheme from '@kangc/v-md-editor/lib/theme/vuepress.js';
import '@kangc/v-md-editor/lib/theme/style/vuepress.css';
import createTipPlugin from '@kangc/v-md-editor/lib/plugins/tip/index';
import '@kangc/v-md-editor/lib/plugins/tip/tip.css';
import createEmojiPlugin from '@kangc/v-md-editor/lib/plugins/emoji/index';
import '@kangc/v-md-editor/lib/plugins/emoji/emoji.css';
import createKatexPlugin from '@kangc/v-md-editor/lib/plugins/katex/cdn';
import createMermaidPlugin from '@kangc/v-md-editor/lib/plugins/mermaid/cdn';
import '@kangc/v-md-editor/lib/plugins/mermaid/mermaid.css';
import createTodoListPlugin from '@kangc/v-md-editor/lib/plugins/todo-list/index';
import '@kangc/v-md-editor/lib/plugins/todo-list/todo-list.css';
import createLineNumberPlugin from '@kangc/v-md-editor/lib/plugins/line-number/index';
import createHighlightLinesPlugin from '@kangc/v-md-editor/lib/plugins/highlight-lines/index';
import '@kangc/v-md-editor/lib/plugins/highlight-lines/highlight-lines.css';
import createCopyCodePlugin from '@kangc/v-md-editor/lib/plugins/copy-code/index';
import '@kangc/v-md-editor/lib/plugins/copy-code/copy-code.css';
import createAlignPlugin from '@kangc/v-md-editor/lib/plugins/align';
import Prism from 'prismjs';
import enUS from '@kangc/v-md-editor/lib/lang/en-US';

const lang = localStorage.getItem(LANG);
VueMarkdownEditor.use(vuepressTheme, {
    codeHighlightExtensionMap: {
        vue: 'html'
    },
    Prism
});
VueMarkdownEditor.use(createTipPlugin());
VueMarkdownEditor.use(createEmojiPlugin());
VueMarkdownEditor.use(createKatexPlugin());
// securityLevel 默认是 'loose'：mermaid 会在 js-xss 过滤之后直接建 SVG DOM，
// 节点标签里的 <img onerror>/click 指令能绕过 v-md-editor 自带的 xss 白名单执行。
// 收紧成 'strict'：禁用 htmlLabels 和 click/href，评论/正文里的图表只是图表。
VueMarkdownEditor.use(createMermaidPlugin({
    mermaidInitializeOptions: {
        securityLevel: 'strict',
        htmlLabels: false,
        flowchart: { htmlLabels: false }
    }
}));
VueMarkdownEditor.use(createTodoListPlugin());
VueMarkdownEditor.use(createLineNumberPlugin());
VueMarkdownEditor.use(createHighlightLinesPlugin());
VueMarkdownEditor.use(createCopyCodePlugin());
VueMarkdownEditor.use(createAlignPlugin());

// v-md-editor 内置 js-xss 白名单为了让 KaTeX 自己画的 sqrt/大括号等 <svg><path> 能过滤器，
// 默认放行了一整套 svg 标签，里面混进了 foreignObject/use/image/feImage/animate*/set/cursor ——
// 这些标签会"渲染"自己 href 指向的内容，而 href/xlink:href 在这里只做了转义、没做协议/内容过滤。
// 典型绕过：<svg><use href="data:image/svg+xml,<svg onload=alert(1)>..."/></svg>，
// payload 整个藏在 data: URI 里，过滤器扫不到里面的 onload。评论支持 markdown、任何登录用户都能发，
// 相当于对全站开放存储型 XSS。KaTeX 实际只用得到 svg/path/g/line/rect 等纯图形标签，删掉上面这批
// "会取资源/执行内容"的标签，公式渲染不受影响，mermaid 走的是另一条不经过这个白名单的路径（见上）。
// js-xss 内部把 whiteList 的 key 统一转成小写了（SVG 标签本身是 camelCase，比如 foreignObject/
// feImage/animateTransform）—— 这里必须用小写 key 删，用原始大小写删是静默无效的（踩过一次坑）。
['foreignobject', 'use', 'image', 'feimage', 'animate', 'animatecolor', 'animatemotion', 'animatetransform', 'set', 'cursor']
    .forEach(tag => { delete VueMarkdownEditor.xss.options.whiteList[tag]; });

if(lang !== defaultLanguage) {
    VueMarkdownEditor.lang.use('en-US', enUS);
}

const pinia = createPinia();
const app = createApp(App);

app.use(pinia);
app.use(router);
app.use(i18n, {});
app.use(ElementPlus, {locale: lang === defaultLanguage ? zhCn : en});
app.use(VueMarkdownEditor);

app.component('svg-icon', SvgIcon);
/* 全局注册 */
app.config.globalProperties.$request = request;
// app.config.globalProperties.$i18n = i18n.global;
app.mount('#app');