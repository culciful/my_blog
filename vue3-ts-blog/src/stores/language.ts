import { defineStore } from 'pinia';

const languageOptions = {
    zh: 'zh-cn',
    en: 'en'
};

export const useLanguageStore = defineStore('language', {
    state: () => ({ 
        language: languageOptions.zh,
        languageOptions
    }),
    getters: {
        // language: (state) => state.language,
    },
    actions: {
        switch() {
            this.language = this.language === languageOptions.zh ? languageOptions.en : languageOptions.zh;
        },
    },
});