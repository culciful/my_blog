import axios from 'axios';

/* 模块扩展 */
export {};

declare module 'vue' {
    interface ComponentCustomProperties {
        $request: typeof axios
        // $translate: (key: string) => string
    }
}