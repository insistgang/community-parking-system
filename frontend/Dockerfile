# ==========================================
# 慧停管家 · 前端 Vue 3 高性能生产 Dockerfile
# ==========================================
FROM nginx:alpine

# 复制已本地预编译的高性能生产静态文件 (秒级拉起，无需服务器重复编译)
COPY dist /usr/share/nginx/html

# 复制定制化 Nginx 配置 (支持 SPA 路由回退与 /api 反向代理)
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 暴露 HTTP 端口
EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
