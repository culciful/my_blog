import './style/global.scss';
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

// v-md-editor 渲染前会跑一遍自带的 js-xss 白名单（剥 <script>/on* 等），但它的白名单
// 默认放行所有标签的 style/class/id。评论区开放注册，放行 style 就能被人塞
// `<p style="position:fixed;inset:0;z-index:9999">假登录框</p>` 这种全屏钓鱼遮罩；
// class/id 则能让注入内容套用本站自己的 UI 样式。这里把这三个属性从白名单摘掉，
// 其余（align、data-*、svg/katex 需要的属性）仍交回原逻辑处理。
const xssFilter: any = (VueMarkdownEditor as any).xss;
if (xssFilter?.options) {
    const fallback = xssFilter.options.onIgnoreTagAttr;
    xssFilter.options.onIgnoreTagAttr = (tag: string, name: string, value: string, isWhiteAttr: boolean) => {
        // 返回 undefined = 跟 onerror 等一样，整个属性丢掉
        if (name === 'style' || name === 'class' || name === 'id') return undefined;
        return fallback ? fallback(tag, name, value, isWhiteAttr) : undefined;
    };
} else {
    console.warn('[xss] v-md-editor 内置过滤器结构变了，style/class/id 收紧未生效');
}

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