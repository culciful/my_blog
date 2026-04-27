import { fileURLToPath, URL } from 'node:url';
import { defineConfig, loadEnv } from 'vite';
import vue from '@vitejs/plugin-vue';
import { insertSvg } from './src/utils/insertSvg';
import { mockServer } from './src/utils/mockServer';
/* 允许setup内设置组件name */
import vueSetupExtend from 'unplugin-vue-setup-extend-plus/vite';
import {prismjsPlugin} from 'vite-plugin-prismjs';

export default defineConfig(({ mode }) => {
    const env = loadEnv(mode, process.cwd(), 'VITE_');
    const useMock = env.VITE_USE_MOCK === 'true';
    const proxyTarget = env.VITE_PROXY_TARGET || 'http://127.0.0.1:8080';

    const plugins = [
        vue(),
        insertSvg(),
        vueSetupExtend({ /* options */ }),
        prismjsPlugin({
            languages: [
                'json',
                'html',
                'css',
                'js',
                'ts',
                'powershell',
                'java',
                'sass',
                'sql',
                'vim',
                'git',
                'yaml',
                'cpp',
                'c'
            ]
        })
    ];
    if (useMock && mode === 'development') {
        plugins.push(mockServer());
    }

    return {
        plugins,
        server: {
            proxy: {
                '/api': { target: proxyTarget, changeOrigin: true },
                '/user': { target: proxyTarget, changeOrigin: true },
                '/article': { target: proxyTarget, changeOrigin: true },
                '/comment': { target: proxyTarget, changeOrigin: true }
            }
        },
        resolve: {
            alias: {
                '@': fileURLToPath(new URL('./src', import.meta.url))
            }
        },
        css: {
            preprocessorOptions: {
                scss: {
                    additionalData: '@import "@/style/_mixin.scss";'
                }
            }
        }
    };
});
