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
import Prism from 'prismjs';
import enUS from '@kangc/v-md-editor/lib/lang/en-US';

const lang = localStorage.getItem(LANG);
VueMarkdownEditor.use(vuepressTheme, {
    Prism,
});
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