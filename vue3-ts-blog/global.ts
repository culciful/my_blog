import request from './src/utils/request';

/* 模块扩展 */
export {};

declare module '@vue/runtime-core' {
    interface ComponentCustomProperties {
        $request: typeof request
        // $translate: (key: string) => string
    }
}