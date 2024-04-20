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
VueMarkdownEditor.use(createMermaidPlugin());
VueMarkdownEditor.use(createTodoListPlugin());
VueMarkdownEditor.use(createLineNumberPlugin());
VueMarkdownEditor.use(createHighlightLinesPlugin());
VueMarkdownEditor.use(createCopyCodePlugin());
VueMarkdownEditor.use(createAlignPlugin());
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