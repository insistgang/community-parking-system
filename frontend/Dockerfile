# ==========================================
# 慧停管家 · 前端 Vue 3 多阶段构建 Dockerfile
# ==========================================

# 阶段 1: 依赖安装与静态编译
FROM node:20-alpine AS builder

WORKDIR /app

# 先复制包管理清单利用缓存
COPY package*.json ./
RUN npm install

# 复制源码并构建打包
COPY . .
RUN npm run build

# 阶段 2: Nginx 高性能生产服务器
FROM nginx:alpine

# 复制构建产物到 Nginx 网页根目录
COPY --from=builder /app/dist /usr/share/nginx/html

# 复制定制化 Nginx 配置 (支持 SPA 路由回退与 /api 反代)
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 暴露 HTTP 端口
EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
