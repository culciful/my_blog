import { fileURLToPath, URL } from 'node:url';
import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import { insertSvg } from './src/utils/insertSvg.ts';
import { mockServer } from './src/utils/mockServer.ts';
/* 允许setup内设置组件name */
import vueSetupExtend from 'unplugin-vue-setup-extend-plus/vite';
import fs from 'fs';
import {prismjsPlugin} from 'vite-plugin-prismjs';

const mockFilePath = 'mock.ts';
if(!fs.existsSync(mockFilePath)) {
    fs.writeFileSync(mockFilePath, 'export default {\n' +
      '    baseURL: \'\',\n' +
      '    type: \'local\', // local or proxy    \n' +
      '    useMock: true\n' +
      '};');
}

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
const mockConfig = require('./mock');
if(mockConfig.default.useMock && process.env.NODE_ENV !== 'production') plugins.push(mockServer());
// https://vitejs.dev/config/
export default defineConfig({
    plugins,
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
});
