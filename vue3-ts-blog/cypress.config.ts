import { defineConfig } from 'cypress'

export default defineConfig({
  e2e: {
    specPattern: 'cypress/e2e/**/*.{cy,spec}.{js,jsx,ts,tsx}',
    // 之前指向 4173（vite preview，生产构建）：生产构建走的是 .env.production，不是本地
    // 联调用的 .env.development.local（VITE_USE_MOCK=false + 真实后端代理），实测这条路径
    // 连登录页的表单都渲染不出来。改成本地正常跑的开发服务器端口——运行 e2e 前先按
    // 本地运行与联调.md 把后端 + `npm run dev` 起起来（跟平时联调开发是同一套，不需要
    // 额外构建 / 起新进程）。
    baseUrl: 'http://localhost:5173'
  },
  component: {
    specPattern: 'src/**/__tests__/*.{cy,spec}.{js,ts,jsx,tsx}',
    devServer: {
      framework: 'vue',
      bundler: 'vite'
    }
  }
})
