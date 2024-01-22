import {createI18n} from 'vue-i18n';
import {LANG} from '@/utils/localStoreItem';
import en from './en';
import zh from './zh';

export const messages = {
    'zh-CN': zh,
    'en-US': en
};

export const languageOptions = {
    'zh-CN': '简体中文',
    'en-US': 'English'
};

export const defaultLanguage = 'zh-CN';

const getLocale = () => {
    let locale: string | null = localStorage.getItem(LANG);
    if(locale == null) {
        locale = defaultLanguage;
        localStorage.setItem(LANG, locale);
    }
    document.documentElement.lang = locale;
    return locale;
};

const i18n = createI18n({
    /* 支持 composition Api使用 t */
    legacy: false,
    allowComposition: true,
    globalInjection: true,
    locale: getLocale(),
    messages
});
export default i18n;