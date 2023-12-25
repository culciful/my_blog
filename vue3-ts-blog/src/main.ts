import './style/global.scss';
import { createApp } from 'vue';
import { createPinia } from 'pinia';
import App from './App.vue';
import router from './router';
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import * as ElementPlusIconsVue from '@element-plus/icons-vue';
import SvgIcon from './components/svgIcon/index.vue';
import {i18n} from "@/language/i18n";
import request from "@/utils/request";

const pinia = createPinia();
const app = createApp(App);

app.use(pinia);
app.use(router);
app.use(i18n);
app.use(ElementPlus);

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component);
}

app.component('svg-icon', SvgIcon);
/* 全局注册 */
app.config.globalProperties.$request = request;
app.mount('#app');