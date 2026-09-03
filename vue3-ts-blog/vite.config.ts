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

    /**
     * /user、/article 前缀下同时存在 SPA 路由（/user/userCenter…）和 API（/user/getMyProfile…）。
     * 浏览器 HTML 导航（Accept: text/html，直接打开或刷新）交回 SPA 处理，
     * 只有 XHR/fetch（Accept 非 text/html）才转发到后端。
     */
    const apiProxy = {
        target: proxyTarget,
        changeOrigin: true,
        bypass(req: { headers: Record<string, string | undefined> }) {
            const accept = req.headers.accept || '';
            if (accept.includes('text/html')) {
                return '/index.html';
            }
        }
    };

    return {
        plugins,
        server: {
            proxy: {
                '/api': apiProxy,
                '/user': apiProxy,
                '/article': apiProxy,
                '/comment': apiProxy,
                // 后端本地上传目录（头像 / 正文图片）
                '/uploads': { target: proxyTarget, changeOrigin: true }
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
