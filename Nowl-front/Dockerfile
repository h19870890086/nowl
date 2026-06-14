# 构建阶段
FROM node:20-alpine AS builder
WORKDIR /app

# 先复制依赖文件进行缓存
COPY package.json package-lock.json ./

# 安装依赖
RUN npm install --legacy-peer-deps

# 复制源代码并构建
COPY . .
RUN npm run build-only

# 运行阶段 —— Nginx 托管静态文件
FROM nginx:1.27-alpine
WORKDIR /usr/share/nginx/html

# 复制构建产物
COPY --from=builder /app/dist .

# 复制 Nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 替换默认 nginx 配置中的端口
RUN rm /etc/nginx/conf.d/default.conf.bak 2>/dev/null; true

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
