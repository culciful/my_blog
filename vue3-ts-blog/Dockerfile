# 多阶段构建：第一阶段用 vite 出静态产物，第二阶段用 nginx 只伺服这些静态文件 + 反代 API。

FROM node:20-alpine AS build
WORKDIR /build
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /build/dist /usr/share/nginx/html
COPY --from=build /build/node_modules/katex/dist/katex.min.js /usr/share/nginx/html/node_modules/katex/dist/katex.min.js
COPY --from=build /build/node_modules/mermaid/dist/mermaid.min.js /usr/share/nginx/html/node_modules/mermaid/dist/mermaid.min.js

EXPOSE 80
