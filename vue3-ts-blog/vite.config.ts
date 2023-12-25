import { fileURLToPath, URL } from 'node:url';

import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import { createSvg } from './src/utils/svgIcon.ts';
import vueSetupExtend from 'unplugin-vue-setup-extend-plus/vite';

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    createSvg('./src/assets/img/icons/'),
    vueSetupExtend({ /* options */ }),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  css: {
    preprocessorOptions: {
      scss: {
        additionalData: `@import "@/style/mixin.scss";`
      }
    }
  }
});
