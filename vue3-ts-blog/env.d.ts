/// <reference types="vite/client" />
/// <reference types="vite-plugin-svg-icons/client" />

/** src/utils/date.ts 在 Date.prototype 上挂了 format，这里补全局声明 */
interface Date {
    format(fmt?: string): string;
}

interface ImportMetaEnv {
    readonly VITE_USE_MOCK?: string;
    readonly VITE_API_BASE?: string;
    readonly VITE_PROXY_TARGET?: string;
}