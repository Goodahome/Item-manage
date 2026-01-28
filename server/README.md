# Item Reminder Tool - 服务端

Node.js + Express + MySQL + Prisma 后端服务

## 功能特性

- ✅ JWT 认证（注册/登录/刷新/登出）
- ✅ 物品管理（CRUD + 分页/搜索/过滤）
- ✅ 分类管理（CRUD）
- ✅ 容器管理（CRUD）
- ✅ 购物清单管理（CRUD）
- ✅ 提醒管理（CRUD）
- ✅ 动态事件（只读）
- ✅ 删除记录（同步用）
- ✅ 统一的 API 响应格式
- ✅ 数据校验（Zod）
- ✅ 请求日志（Morgan）

## 环境要求

- Node.js 18+
- MySQL 8.0+
- npm 或 yarn

## 快速开始

### 1. 安装依赖

```bash
cd server
npm install
```

### 2. 配置环境变量 ⭐

**方法 1：使用自动化脚本（推荐）**

Windows 命令提示符 (CMD)：
```cmd
CREATE_ENV.bat
```

或 PowerShell：
```powershell
.\CREATE_ENV.ps1
```

**方法 2：手动创建**

在 `server` 目录下创建 `.env` 文件，内容如下：

```env
# 数据库配置
DATABASE_URL="mysql://root:123456@localhost:3306/itemreminder"

# JWT 配置（开发环境示例，生产环境务必修改）
JWT_SECRET="dev_secret_key_change_this_in_production_32chars"
JWT_EXPIRES_IN="7d"

# 服务器配置
PORT=3000
NODE_ENV=development
```

**根据你的 MySQL 配置修改**:
- 用户名（示例: `root`）
- 密码（示例: `123456`）
- 数据库名（默认: `itemreminder`）

📖 **详细配置说明**: 查看 [ENV_SETUP.md](ENV_SETUP.md)

### 3. 初始化数据库

```bash
# 生成 Prisma Client
npm run prisma:generate

# 创建数据库表（开发环境）
npm run prisma:push

# 或者使用迁移（生产环境推荐）
npm run prisma:migrate
```

### 4. 启动服务器

开发模式（支持热重载）：

```bash
npm run dev
```

生产模式：

```bash
npm run build
npm start
```

服务器将在 `http://localhost:3000` 启动。

### 5. 健康检查

访问 `http://localhost:3000/health` 检查服务器状态。

## API 文档

### 认证接口

- `POST /api/auth/register` - 注册新用户
- `POST /api/auth/login` - 用户登录
- `POST /api/auth/refresh` - 刷新 token（需要认证）
- `POST /api/auth/logout` - 登出（需要认证）

### 物品接口

- `GET /api/items` - 获取物品列表（支持分页、搜索、过滤）
- `GET /api/items/:uuid` - 获取单个物品
- `POST /api/items` - 创建或更新物品（upsert）
- `PUT /api/items/:uuid` - 更新物品
- `DELETE /api/items/:uuid` - 删除物品

### 分类接口

- `GET /api/categories` - 获取分类列表
- `GET /api/categories/:uuid` - 获取单个分类
- `POST /api/categories` - 创建或更新分类
- `PUT /api/categories/:uuid` - 更新分类
- `DELETE /api/categories/:uuid` - 删除分类

### 容器接口

- `GET /api/warehouses` - 获取容器列表
- `GET /api/warehouses/:uuid` - 获取单个容器
- `POST /api/warehouses` - 创建或更新容器
- `PUT /api/warehouses/:uuid` - 更新容器
- `DELETE /api/warehouses/:uuid` - 删除容器

### 购物清单接口

- `GET /api/shopping-items` - 获取购物清单
- `GET /api/shopping-items/:uuid` - 获取单个购物项
- `POST /api/shopping-items` - 创建或更新购物项
- `DELETE /api/shopping-items/:uuid` - 删除购物项

### 提醒接口

- `GET /api/reminders` - 获取提醒列表
- `GET /api/reminders/:uuid` - 获取单个提醒
- `POST /api/reminders` - 创建或更新提醒
- `DELETE /api/reminders/:uuid` - 删除提醒

### 动态事件接口

- `GET /api/activity-events` - 获取动态事件列表（分页）
- `GET /api/activity-events/:uuid` - 获取单个动态事件

### 删除记录接口

- `GET /api/deleted-records` - 获取删除记录（用于同步）

## 统一响应格式

### 成功响应

```json
{
  "success": true,
  "data": {
    // 响应数据
  }
}
```

### 错误响应

```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "错误信息",
    "details": {}
  }
}
```

## 认证机制

所有需要认证的接口都需要在请求头中添加 JWT token：

```
Authorization: Bearer <token>
```

## 数据模型

所有实体都包含以下字段：

- `id` - 自增主键（服务端使用）
- `uuid` - UUID（跨端唯一标识，客户端使用）
- `userId` - 用户 ID（外键）
- `createdAt` - 创建时间
- `updatedAt` - 更新时间

## 开发说明

### 项目结构

```
server/
├── src/
│   ├── controllers/      # 控制器（业务逻辑）
│   ├── routes/           # 路由定义
│   ├── middleware/       # 中间件（认证、错误处理）
│   ├── validators/       # 数据校验（Zod）
│   ├── utils/            # 工具函数
│   ├── types/            # TypeScript 类型定义
│   ├── prisma.ts         # Prisma 客户端
│   └── index.ts          # 应用入口
├── prisma/
│   └── schema.prisma     # 数据库模型定义
├── package.json
├── tsconfig.json
└── .env
```

### 添加新接口

1. 在 `prisma/schema.prisma` 中定义数据模型
2. 运行 `npm run prisma:generate` 生成类型
3. 在 `src/controllers/` 创建控制器
4. 在 `src/routes/` 创建路由
5. 在 `src/index.ts` 中注册路由

## 部署

### Docker 部署（推荐）

TODO: 添加 Dockerfile 和 docker-compose.yml

### 传统部署

1. 在服务器上安装 Node.js 和 MySQL
2. 克隆代码并安装依赖
3. 配置环境变量
4. 运行数据库迁移
5. 使用 PM2 或 systemd 管理进程

```bash
# 使用 PM2
npm install -g pm2
pm2 start dist/index.js --name itemreminder
```

## MinIO 配置

### 基本配置

MinIO 用于存储图片文件。基本配置在 `.env` 文件中：

```env
STORAGE_ENDPOINT=http://182.140.144.150:21152  # MinIO API 端点
STORAGE_BUCKET=itemremindertool                 # Bucket 名称
STORAGE_ACCESS_KEY=minioadmin                  # 访问密钥
STORAGE_SECRET_KEY=minioadmin                  # 秘密密钥
STORAGE_REGION=auto                            # 区域
STORAGE_FORCE_PATH_STYLE=true                  # 使用路径样式
```

### 服务器端加密（SSE）配置

#### 选项 1：禁用加密（推荐用于开发环境）

如果 MinIO 未配置 KMS，请禁用服务器端加密：

```env
# 注释掉或删除 STORAGE_SSE
# STORAGE_SSE=AES256
```

#### 选项 2：使用 MinIO KMS（生产环境）

如果需要启用服务器端加密，需要先配置 MinIO KMS：

**1. 安装 MinIO KMS**

MinIO KMS 是一个独立的密钥管理服务器。安装方式：

- **Docker 方式（推荐用于开发/测试）：**
  ```bash
  docker pull minio/kms:latest
  docker run -d \
    --name minio-kms \
    -p 7373:7373 \
    -v /path/to/kms/config:/etc/minkms \
    -v /path/to/kms/data:/mnt/minio-kms \
    minio/kms:latest
  ```

- **Linux 二进制方式（生产环境）：**
  参考官方文档：https://docs.min.io/enterprise/minio-kms/installation/linux/

**2. 配置 MinIO 使用 KMS**

在 MinIO 启动时添加环境变量：

```bash
export MINIO_KMS_SERVER="http://kms-host:7373"
export MINIO_KMS_SSE_KEY="my-encryption-key"
export MINIO_KMS_ENCLAVE="my-enclave"
export MINIO_KMS_API_KEY="your-api-key"

minio server /path/to/data --console-address ":21152" --address ":21152"
```

**3. 配置应用使用 KMS**

在 `.env` 文件中：

```env
# 使用 KMS 加密
STORAGE_SSE=aws:kms
STORAGE_SSE_KMS_KEY=my-encryption-key  # KMS 密钥名称
```

或者使用 AES256（如果 MinIO 支持）：

```env
STORAGE_SSE=AES256
```

**4. 验证配置**

重启服务器后，查看日志应该显示：
```
[mediaController] STORAGE_SSE="AES256", 服务器端加密: 启用
```

### MinIO 启动命令示例

```bash
# API 端口和 Console 端口可以相同或不同
minio.exe server E:\heji_server\minio\data \
  --console-address "0.0.0.0:21152" \
  --address "0.0.0.0:21152"
```

**注意：**
- `--address` 指定 API 端口（S3 兼容接口）
- `--console-address` 指定 Web Console 端口
- 两个端口可以相同，也可以不同
- 确保 `.env` 中的 `STORAGE_ENDPOINT` 指向 API 端口

### 常见问题

**Q: 上传失败，返回 "KMS not configured" 错误**

A: MinIO 未配置 KMS 但启用了服务器端加密。解决方案：
- 禁用加密：在 `.env` 中注释掉 `STORAGE_SSE`
- 或配置 MinIO KMS（见上方说明）

**Q: 上传返回 HTML 错误页面**

A: 签名 URL 可能指向了错误的端点。检查：
- `STORAGE_ENDPOINT` 是否指向 API 端口（不是 Console 端口）
- 服务器端日志中的签名 URL 是否正确

**Q: 如何验证 MinIO 配置是否正确？**

A: 使用 MinIO 客户端测试：
```bash
mc alias set myminio http://182.140.144.150:21152 minioadmin minioadmin
mc ls myminio/itemremindertool
```

## 故障排除

### 数据库连接失败

- 检查 DATABASE_URL 配置是否正确
- 确认 MySQL 服务是否运行
- 检查防火墙设置

### 认证失败

- 检查 JWT_SECRET 配置
- 确认 token 格式正确（Bearer <token>）
- 检查 token 是否过期

### MinIO 上传失败

- 检查 `STORAGE_ENDPOINT` 是否正确指向 MinIO API 端口
- 确认 MinIO 服务是否运行
- 检查访问密钥和秘密密钥是否正确
- 如果启用了加密，确认 MinIO KMS 已配置
- 查看服务器端日志中的详细错误信息

## License

MIT
