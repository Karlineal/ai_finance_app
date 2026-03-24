# Actual Budget + AI 记账助手 完整部署指南

## 快速启动

```bash
# 1. 创建项目目录
mkdir ~/ai-finance-assistant
cd ~/ai-finance-assistant

# 2. 下载部署文件
curl -O https://raw.githubusercontent.com/your-repo/docker-compose.yml
curl -O https://raw.githubusercontent.com/your-repo/.env.example

# 3. 配置环境变量
cp .env.example .env
# 编辑 .env 文件，填入你的 API Keys

# 4. 启动服务
docker-compose up -d

# 5. 访问
# Actual Budget: http://localhost:5006
# Web 应用: http://localhost:3000
```

---

## 完整 docker-compose.yml

```yaml
version: '3.8'

services:
  # ==================== Actual Budget ====================
  actual-server:
    image: actualbudget/actual-server:latest
    container_name: actual-budget
    ports:
      - '5006:5006'
    volumes:
      - actual-data:/data
    environment:
      - ACTUAL_LOGIN_METHOD=password
      - ACTUAL_PASSWORD=${ACTUAL_PASSWORD:-changeme}
      - ACTUAL_DATA_DIR=/data
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:5006"]
      interval: 30s
      timeout: 10s
      retries: 3
    networks:
      - ai-finance-network

  # ==================== AI 服务 ====================
  ai-service:
    build:
      context: ./packages/ai-service
      dockerfile: Dockerfile
    container_name: ai-service
    environment:
      # Kimi Coding (默认OCR，免费)
      - KIMI_CODING_API_KEY=${KIMI_CODING_API_KEY}
      
      # Moonshot (备选OCR/分类)
      - MOONSHOT_API_KEY=${MOONSHOT_API_KEY}
      
      # OpenAI (备选)
      - OPENAI_API_KEY=${OPENAI_API_KEY}
      
      # Anthropic (备选)
      - ANTHROPIC_API_KEY=${ANTHROPIC_API_KEY}
      
      # 配置
      - DEFAULT_OCR_PROVIDER=${DEFAULT_OCR_PROVIDER:-kimi-coding}
      - DEFAULT_LLM_PROVIDER=${DEFAULT_LLM_PROVIDER:-openai}
      - ENABLE_OCR_FALLBACK=${ENABLE_OCR_FALLBACK:-true}
      
      # Actual Budget 连接
      - ACTUAL_URL=http://actual-server:5006
      - ACTUAL_PASSWORD=${ACTUAL_PASSWORD}
      
      - NODE_ENV=production
      - LOG_LEVEL=info
    volumes:
      - ai-service-logs:/app/logs
    depends_on:
      actual-server:
        condition: service_healthy
    restart: unless-stopped
    networks:
      - ai-finance-network

  # ==================== 导入服务 ====================
  import-service:
    build:
      context: ./packages/import-service
      dockerfile: Dockerfile
    container_name: import-service
    environment:
      - ACTUAL_URL=http://actual-server:5006
      - ACTUAL_PASSWORD=${ACTUAL_PASSWORD}
      - AI_SERVICE_URL=http://ai-service:3001
      - NODE_ENV=production
    volumes:
      - ./uploads:/app/uploads
      - import-logs:/app/logs
    depends_on:
      - actual-server
      - ai-service
    restart: unless-stopped
    networks:
      - ai-finance-network

  # ==================== Web 前端 ====================
  web-app:
    build:
      context: ./packages/web-app
      dockerfile: Dockerfile
    container_name: web-app
    ports:
      - '3000:80'
    environment:
      - VITE_API_URL=http://localhost:3001
      - VITE_ACTUAL_URL=http://localhost:5006
    depends_on:
      - ai-service
      - import-service
    restart: unless-stopped
    networks:
      - ai-finance-network

  # ==================== 可选：Nginx 反向代理 ====================
  nginx:
    image: nginx:alpine
    container_name: nginx
    ports:
      - '80:80'
      - '443:443'
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
      - ./nginx/ssl:/etc/nginx/ssl:ro
    depends_on:
      - web-app
      - actual-server
    restart: unless-stopped
    networks:
      - ai-finance-network
    profiles:
      - production  # 仅在 production profile 时启动

# ==================== 数据卷 ====================
volumes:
  actual-data:
    driver: local
  ai-service-logs:
    driver: local
  import-logs:
    driver: local

# ==================== 网络 ====================
networks:
  ai-finance-network:
    driver: bridge
```

---

## 环境变量配置 (.env)

```bash
# 复制示例配置文件
cp .env.example .env

# 然后编辑 .env 文件
```

### .env.example

```bash
# ============================================
# AI 记账助手 - 环境变量配置
# ============================================

# ------------ Actual Budget 配置 ------------
# 登录密码（必填）
ACTUAL_PASSWORD=YourSecurePassword123!

# ------------ Kimi Coding 配置（默认OCR）------------
# 免费使用，优先用于发票识别
# 申请地址：https://platform.moonshot.cn
KIMI_CODING_API_KEY=sk-kimi-your-key-here

# ------------ Moonshot 配置（备选）------------
# 付费，普通 Kimi API
# 用于 AI 分类、预算预测等
MOONSHOT_API_KEY=sk-your-moonshot-key

# ------------ OpenAI 配置（备选）------------
# 用于 AI 分类、预算预测
OPENAI_API_KEY=sk-your-openai-key

# ------------ Anthropic 配置（备选）------------
# Claude 3，用于 OCR 备选
ANTHROPIC_API_KEY=sk-ant-your-anthropic-key

# ------------ AI 服务配置 ------------
# 默认 OCR 服务商: kimi-coding | moonshot | openai | claude
DEFAULT_OCR_PROVIDER=kimi-coding

# 默认 LLM 服务商: openai | moonshot
DEFAULT_LLM_PROVIDER=openai

# OCR 失败时是否自动降级到其他服务商
ENABLE_OCR_FALLBACK=true

# ------------ 应用配置 ------------
NODE_ENV=production
LOG_LEVEL=info

# ------------ 端口配置（可选）------------
# 如果端口被占用，可以修改
ACTUAL_PORT=5006
WEB_PORT=3000
```

---

## 目录结构

```
ai-finance-assistant/
├── docker-compose.yml          # Docker 编排配置
├── .env                        # 环境变量（不要提交到Git）
├── .env.example                # 环境变量示例
├── packages/
│   ├── ai-service/             # AI 服务代码
│   ├── import-service/         # 导入服务代码
│   └── web-app/                # Web 前端代码
├── nginx/                      # Nginx 配置（可选）
│   ├── nginx.conf
│   └── ssl/                    # SSL 证书
├── data/                       # Actual Budget 数据（自动创建）
├── uploads/                    # 上传文件目录
└── logs/                       # 日志目录
```

---

## AI 服务 Dockerfile

### packages/ai-service/Dockerfile

```dockerfile
# 构建阶段
FROM node:20-alpine AS builder

WORKDIR /app

# 安装依赖
COPY package*.json ./
COPY tsconfig.json ./
RUN npm ci

# 复制源代码
COPY src ./src

# 构建
RUN npm run build

# 运行阶段
FROM node:20-alpine

WORKDIR /app

# 安装生产依赖
COPY package*.json ./
RUN npm ci --only=production

# 复制构建产物
COPY --from=builder /app/dist ./dist

# 创建日志目录
RUN mkdir -p /app/logs

EXPOSE 3001

CMD ["node", "dist/index.js"]
```

---

## 导入服务 Dockerfile

### packages/import-service/Dockerfile

```dockerfile
FROM node:20-alpine

WORKDIR /app

COPY package*.json ./
RUN npm ci --only=production

COPY dist ./dist

# 创建上传目录
RUN mkdir -p /app/uploads /app/logs

EXPOSE 3002

CMD ["node", "dist/index.js"]
```

---

## Web 前端 Dockerfile

### packages/web-app/Dockerfile

```dockerfile
# 构建阶段
FROM node:20-alpine AS builder

WORKDIR /app

COPY package*.json ./
COPY vite.config.ts ./
COPY tsconfig.json ./
COPY index.html ./

RUN npm ci

COPY src ./src
COPY public ./public

RUN npm run build

# 运行阶段（Nginx）
FROM nginx:alpine

COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
```

### packages/web-app/nginx.conf

```nginx
server {
    listen 80;
    server_name localhost;
    root /usr/share/nginx/html;
    index index.html;

    # 前端路由支持
    location / {
        try_files $uri $uri/ /index.html;
    }

    # API 代理
    location /api/ {
        proxy_pass http://ai-service:3001/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_cache_bypass $http_upgrade;
    }

    # 静态文件缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
```

---

## 启动步骤

### 1. 初始化项目

```bash
# 创建目录
mkdir -p ~/ai-finance-assistant
cd ~/ai-finance-assistant

# 创建必要的子目录
mkdir -p packages/ai-service packages/import-service packages/web-app
mkdir -p nginx/ssl data uploads logs
```

### 2. 配置环境变量

```bash
# 创建 .env 文件
cat > .env << 'EOF'
ACTUAL_PASSWORD=YourSecurePassword123!
KIMI_CODING_API_KEY=sk-kimi-your-key-here
MOONSHOT_API_KEY=sk-your-moonshot-key
OPENAI_API_KEY=sk-your-openai-key
ANTHROPIC_API_KEY=sk-ant-your-anthropic-key
DEFAULT_OCR_PROVIDER=kimi-coding
DEFAULT_LLM_PROVIDER=openai
ENABLE_OCR_FALLBACK=true
NODE_ENV=production
LOG_LEVEL=info
EOF

# 编辑 .env，填入你的真实 API Keys
nano .env
```

### 3. 启动服务

```bash
# 开发模式（只启动核心服务）
docker-compose up -d

# 生产模式（包含 Nginx）
docker-compose --profile production up -d
```

### 4. 验证部署

```bash
# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f actual-server
docker-compose logs -f ai-service

# 测试 Actual Budget
curl http://localhost:5006

# 测试 AI 服务
curl http://localhost:3001/health
```

---

## 常用命令

```bash
# 启动
docker-compose up -d

# 停止
docker-compose down

# 重启
docker-compose restart

# 查看日志
docker-compose logs -f [service-name]

# 更新镜像
docker-compose pull
docker-compose up -d

# 进入容器
docker-compose exec actual-server sh

# 备份数据
docker-compose exec actual-server tar czf /data/backup-$(date +%Y%m%d).tar.gz /data

# 查看资源使用
docker-compose stats
```

---

## 故障排查

### 问题：服务启动失败

```bash
# 查看详细日志
docker-compose logs [service-name]

# 检查端口占用
netstat -tlnp | grep 5006

# 修改端口（编辑 docker-compose.yml）
ports:
  - '8080:5006'  # 改为 8080 端口
```

### 问题：AI 服务连接失败

```bash
# 检查 API Key 是否正确
docker-compose exec ai-service env | grep API_KEY

# 测试 Kimi Coding API
curl -H "Authorization: Bearer $KIMI_CODING_API_KEY" \
  https://api.kimi.com/coding/v1/models
```

### 问题：数据丢失

```bash
# 检查数据卷
docker volume ls
docker volume inspect ai-finance-assistant_actual-data

# 从备份恢复
cp -r data-backup-20240115/* data/
docker-compose restart actual-server
```

---

## 下一步

部署完成后：
1. 访问 http://localhost:5006 创建预算文件
2. 访问 http://localhost:3000 使用 AI 记账助手
3. 开始导入支付宝/微信账单

**需要我帮你写 Phase 1 的代码吗？**