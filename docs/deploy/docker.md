# Docker 部署

本文介绍如何基于 `script/docker/docker-compose.yml` 部署 Snail AI 完整服务，包括 Server、Agent、nginx 反向代理和前端。

## 目录结构

```
script/docker/
├── docker-compose.yml          # 生产环境 Compose 文件
├── .env                        # 构建路径变量（本地配置，不提交）
├── .env.example                # 构建路径变量模板
└── nginx/
    ├── conf/
    │   ├── nginx.conf
    │   └── vhost/
    │       ├── snailai-admin.opensnail.com.conf   # 管理端前端
    │       ├── snailai-server.opensnail.com.conf  # 后端 API
    │       ├── snailai-chat.opensnail.com.conf    # 对话页面
    │       └── snailai.opensnail.com.conf         # 主站
    ├── cert/                   # SSL 证书目录（需自行放入）
    └── html/                   # 前端静态文件目录
```

## 前提条件

| 软件 | 最低版本 |
|------|----------|
| JDK | 21+ |
| Maven | 3.8+ |
| Docker | 20.10+ |
| Docker Compose | v2+ |
| Node.js | 20+（仅前端构建需要） |
| pnpm | 10+（仅前端构建需要） |

## 快速开始（无脑操作）

适用于新服务器，从头到尾一键部署。

### 1. 安装环境

```bash
sudo bash script/deploy/ci-setup.sh
```

安装 JDK 21 + Maven + Git + Node.js + pnpm。已有环境可跳过。

### 2. 拉取源码

```bash
mkdir -p /workspace
cd /workspace
git clone https://gitee.com/opensnail/snail-ai.git
cd snail-ai
```

### 3. 复制 docker 目录到 /docker

```bash
cp -r script/docker /docker
```

此后所有 docker 命令都在 `/docker` 下执行，简洁直观。

### 4. 配置 .env（构建路径）

编辑 `/docker/.env`：

```env
SNAIL_AI_STARTER_PATH=/workspace/snail-ai/snail-ai-starter
SNAIL_AI_AGENT_EXAMPLE_PATH=/workspace/snail-ai/snail-ai-agent-example
```

这是 Docker 构建时的上下文路径，指向你的源码目录。

### 5. 配置 docker-compose.yml

编辑 `/docker/docker-compose.yml`，按需修改：

**数据库连接**（`snail-ai-server` 服务的 `JAVA_OPTS`）：

```yaml
-Dspring.datasource.driver-class-name=org.postgresql.Driver
-Dspring.datasource.url=jdbc:postgresql://localhost:5432/snail_ai
-Dspring.datasource.username=root
-Dspring.datasource.password=你的密码
```

**Agent 凭证**（`snail-ai-agent-example` 服务的 `JAVA_OPTS`）：

```yaml
-Dsnail-ai.app-id=snail-ai-agent-demo
-Dsnail-ai.token=你的token
```

> token 需与数据库 `sai_app` 表中一致。默认数据见 SQL 初始化脚本。

### 6. 配置 nginx 域名

编辑 `/docker/nginx/conf/vhost/` 下的配置文件，将 `opensnail.com` 替换为你自己的域名。

将 SSL 证书放入 `/docker/nginx/cert/`：

```
/docker/nginx/cert/你的域名.pem
/docker/nginx/cert/你的域名.key
```

修改配置中的 `ssl_certificate` 和 `ssl_certificate_key` 路径。

### 7. 构建源码

```bash
# 构建 Server
cd /workspace/snail-ai
mvn clean package -DskipTests -T 1C

# 构建 Agent
cd /workspace/snail-ai/snail-ai-agent-example
mvn clean package -DskipTests -T 1C
```

### 8. 初始化数据库

#### PostgreSQL

```bash
# 创建数据库
docker exec -i postgres psql -U root -c "CREATE DATABASE snail_ai;"

# 导入表结构（含默认管理员和演示应用）
docker exec -i postgres psql -U root -d snail_ai < /workspace/snail-ai/script/sql/snail_ai_schema_pgsql.sql
```

#### MySQL

```bash
docker exec -i mysql mysql -uroot -p123456 -e "CREATE DATABASE snail_ai DEFAULT CHARACTER SET utf8mb4;"
docker exec -i mysql mysql -uroot -p123456 snail_ai < /workspace/snail-ai/script/sql/snail_ai_schema.sql
```

### 9. 依次启动服务

```bash
cd /docker

# 1. 先启动数据库
docker compose up -d postgres

# 2. 启动 Server
docker compose up -d --build snail-ai-server

# 3. 启动 Agent
docker compose up -d --build snail-ai-agent-example

# 4. 最后启动 nginx
docker compose up -d nginx
```

### 10. 部署前端

```bash
cd /workspace/snail-ai-admin

# 安装依赖
pnpm install --frozen-lockfile

# 构建（读取 .env.deploy，使用绝对后端地址）
pnpm build:deploy

# 拷贝到 nginx 静态文件目录
rm -rf /docker/nginx/html/snailai-admin
mkdir -p /docker/nginx/html/snailai-admin
cp -r dist/* /docker/nginx/html/snailai-admin/
```

> 前端源码独立仓库，需单独 clone。`.env.deploy` 中的 `VITE_SERVICE_BASE_URL` 配置后端完整地址。

## 更新服务

三种更新脚本位于 `script/deploy/`，每个脚本顶部有配置区，修改路径后即可使用。

### update-server.sh

```bash
bash script/deploy/update-server.sh
```

流程：Maven 构建 → 停容器 → 删镜像 → 重建启动。

### update-agent.sh

```bash
bash script/deploy/update-agent.sh
```

流程同上。

### update-admin.sh

```bash
bash script/deploy/update-admin.sh
```

流程：pnpm install → build:deploy → 清 nginx 旧包 → 拷贝新包。

### 脚本配置

每个脚本顶部有配置区，复制到任意位置，修改后直接执行：

```bash
# update-server.sh / update-agent.sh
PROJECT_DIR="/workspace/snail-ai"                  # 源码目录
COMPOSE_DIR="/docker"                              # docker-compose.yml 所在目录
SERVICE_NAME="snail-ai-server"                     # 服务名
MVN_OPTS="-DskipTests -T 1C"                       # Maven 参数

# update-admin.sh
FRONTEND_DIR="/workspace/snail-ai-admin"           # 前端源码目录
NGINX_HTML_DIR="/docker/nginx/html/snailai-admin"  # nginx 静态文件目录
BUILD_CMD="pnpm build:deploy"                      # 构建命令
```

> 已有 Docker 环境但不需要完整构建链的用户，可以跳过 Maven/Node.js 安装，直接使用预编译的 JAR 或镜像。

## 演示模式

编辑 `/docker/nginx/conf/vhost/snailai-server.opensnail.com.conf`，取消注释：

```nginx
# if ($request_method !~ ^(GET|HEAD|OPTIONS)$) {
#     return 403;
# }
```

改为：

```nginx
if ($request_method !~ ^(GET|HEAD|OPTIONS)$) {
    return 403;
}
```

重启 nginx 后，所有 POST/PUT/DELETE 请求返回 `{"status":0,"message":"演示模式不允许操作"}`。

## 常用运维命令

```bash
cd /docker

# 查看所有服务状态
docker compose ps

# 查看日志
docker compose logs -f snail-ai-server

# 重启某个服务
docker compose restart nginx

# 停止所有服务
docker compose down
```

## 默认账号

| 账号 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | `admin` | `admin123` |

## 相关文件

- `script/docker/docker-compose.yml`
- `script/docker/nginx/conf/vhost/`
- `script/deploy/ci-setup.sh`
- `script/deploy/update-server.sh`
- `script/deploy/update-agent.sh`
- `script/deploy/update-admin.sh`
- `script/sql/snail_ai_schema.sql`
- `script/sql/snail_ai_schema_pgsql.sql`