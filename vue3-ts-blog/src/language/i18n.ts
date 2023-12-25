import {createI18n} from "vue-i18n";
import en from './en';
import zh from './zh';

export const messages = {
    en,
    zh
};

export const languageOptions = {
    zh: '简体中文',
    en: 'English'
};
const defaultLanguage = 'zh';

const getLocale = () => {
    let locale: string | null = localStorage.getItem('locale');
    if(locale == null) {
        locale = defaultLanguage;
        localStorage.setItem('lang', locale);
    }
    return locale;
};

export const i18n = createI18n({
    /* 支持 composition Api使用 t */
    legacy: false,
    allowComposition: true,
    locale: getLocale(),
    messages
});