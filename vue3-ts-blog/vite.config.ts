import { fileURLToPath, URL } from 'node:url';
import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import { createSvg } from './src/utils/svgIcon.ts';
import { mockServer } from './src/utils/mockServer.ts';
import vueSetupExtend from 'unplugin-vue-setup-extend-plus/vite';
import fs from 'fs';

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
    createSvg('./src/assets/img/icons/'),
    vueSetupExtend({ /* options */ }),
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
                additionalData: '@import "@/style/mixin.scss";'
            }
        }
    }
});
