# 环境变量配置指南

## 📋 说明

`.env` 文件包含敏感配置信息，需要手动创建。本文档提供详细的配置步骤。

## 🚀 快速开始

### 1. 创建 .env 文件

在 `server` 目录下创建一个名为 `.env` 的文件（注意：文件名以点开头）。

### 2. 复制以下内容到 .env 文件

```env
# 数据库连接 URL
# 格式: mysql://用户名:密码@主机:端口/数据库名
DATABASE_URL="mysql://root:123456@localhost:3306/itemreminder"

# JWT 密钥（开发环境示例，生产环境务必修改）
JWT_SECRET="dev_secret_key_change_this_in_production_32chars"

# JWT 过期时间（7天）
JWT_EXPIRES_IN="7d"

# 服务器端口
PORT=3000

# 运行环境
NODE_ENV="development"
```

### 3. 修改配置

根据你的实际情况修改以下配置项：

#### DATABASE_URL
- **用户名**: 默认 `root`，改为你的 MySQL 用户名
- **密码**: 默认 `123456`，改为你的 MySQL 密码
- **主机**: 默认 `localhost`，如果 MySQL 在其他服务器上，改为对应 IP
- **端口**: 默认 `3306`，如果修改过 MySQL 端口，改为实际端口
- **数据库名**: 默认 `itemreminder`，如果使用其他名称，改为实际名称

#### JWT_SECRET
- **开发环境**: 可以使用示例密钥
- **生产环境**: 必须使用强随机密钥（至少 32 个字符）

## 📝 详细配置说明

### DATABASE_URL（数据库连接字符串）

```env
DATABASE_URL="mysql://用户名:密码@主机:端口/数据库名"
```

**示例**:
```env
# 本地开发
DATABASE_URL="mysql://root:mypassword@localhost:3306/itemreminder"

# 远程数据库
DATABASE_URL="mysql://appuser:secretpass@192.168.1.100:3306/itemreminder"

# 云数据库
DATABASE_URL="mysql://admin:password123@db.example.com:3306/itemreminder_prod"
```

### JWT_SECRET（JWT 签名密钥）

用于签名和验证 JWT token 的密钥。

**开发环境**:
```env
JWT_SECRET="dev_secret_key_change_this_in_production_32chars"
```

**生产环境**（必须使用强随机密钥）:

方法 1 - 使用 Node.js 生成:
```bash
node -e "console.log(require('crypto').randomBytes(32).toString('hex'))"
```

方法 2 - 在线生成:
访问 https://www.random.org/strings/ 生成随机字符串

示例:
```env
JWT_SECRET="a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3x4y5z6"
```

### JWT_EXPIRES_IN（Token 过期时间）

控制 JWT token 的有效期。

```env
# 1 小时
JWT_EXPIRES_IN="1h"

# 7 天（推荐）
JWT_EXPIRES_IN="7d"

# 30 天
JWT_EXPIRES_IN="30d"

# 90 天
JWT_EXPIRES_IN="90d"
```

### PORT（服务器端口）

服务器监听的端口号。

```env
# 默认端口
PORT=3000

# 其他端口
PORT=8080
PORT=5000
```

### NODE_ENV（运行环境）

```env
# 开发环境
NODE_ENV="development"

# 生产环境
NODE_ENV="production"

# 测试环境
NODE_ENV="test"
```

**影响**:
- `development`: 详细日志、允许 HTTP
- `production`: 简化日志、强制 HTTPS、更严格的错误处理

## 🔐 生产环境配置示例

为生产环境创建单独的 `.env.production` 文件：

```env
# 生产数据库（使用强密码）
DATABASE_URL="mysql://prod_user:VeryStr0ng!Pa$$w0rd@prod-db.example.com:3306/itemreminder_prod"

# 强随机 JWT 密钥（64 个字符）
JWT_SECRET="f7a9c8b2e1d4g6h5i3j7k9l2m4n6o8p1q3r5s7t9u2v4w6x8y1z3a5"

# Token 过期时间
JWT_EXPIRES_IN="7d"

# 端口（可能由云平台环境变量提供）
PORT=3000

# 生产环境
NODE_ENV="production"

# 可选：限制 CORS
CORS_ORIGIN="https://your-app-domain.com"
```

## 🛠️ 创建 .env 文件的方法

### Windows PowerShell

```powershell
# 进入 server 目录
cd h:\AndroidAPP\Itemremindertool\server

# 创建 .env 文件
New-Item -Path ".env" -ItemType File -Force

# 使用记事本编辑
notepad .env

# 或使用 VSCode 编辑
code .env
```

### Windows 命令提示符 (CMD)

```cmd
cd h:\AndroidAPP\Itemremindertool\server
type nul > .env
notepad .env
```

### 直接在文件资源管理器中创建

1. 打开 `h:\AndroidAPP\Itemremindertool\server` 文件夹
2. 右键 → 新建 → 文本文档
3. 重命名为 `.env`（删除 `.txt` 后缀）
4. 如果提示"必须键入文件名"，请先创建 `env.txt`，然后重命名为 `.env`
5. 用记事本或编辑器打开并粘贴配置

## ✅ 验证配置

创建完 `.env` 文件后，运行以下命令验证：

```bash
# 进入 server 目录
cd server

# 查看 .env 文件是否存在
dir .env

# 测试配置（会显示读取到的环境变量）
npm run dev
```

如果配置正确，应该看到：
```
Server is running on http://localhost:3000
Health check: http://localhost:3000/health
```

## 🔍 常见问题

### 1. 找不到 .env 文件

**原因**: Windows 文件资源管理器默认隐藏点开头的文件

**解决**:
- 打开文件资源管理器
- 点击"查看"选项卡
- 勾选"隐藏的项目"

### 2. 数据库连接失败

**错误信息**: `Error: Access denied for user 'root'@'localhost'`

**解决**:
1. 检查 MySQL 是否启动
2. 检查用户名和密码是否正确
3. 检查数据库是否已创建

创建数据库：
```sql
CREATE DATABASE itemreminder CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3. JWT_SECRET 太短

**错误信息**: `JWT secret must be at least 32 characters`

**解决**: 使用更长的密钥（至少 32 个字符）

### 4. 端口被占用

**错误信息**: `Error: listen EADDRINUSE: address already in use :::3000`

**解决**:
- 修改 `.env` 中的 `PORT` 为其他值（如 3001、8080）
- 或关闭占用 3000 端口的程序

查找占用端口的程序：
```powershell
# Windows
netstat -ano | findstr :3000

# 结束进程（替换 PID）
taskkill /PID <进程ID> /F
```

## 📚 相关文档

- [服务端 README](README.md)
- [Prisma 环境变量文档](https://www.prisma.io/docs/concepts/components/prisma-schema/data-sources)
- [dotenv 文档](https://github.com/motdotla/dotenv)

## ⚠️ 安全提示

1. **永远不要提交 .env 文件到 Git**
   - `.env` 应该在 `.gitignore` 中
   - 只提交 `.env.example` 作为模板

2. **生产环境必须使用强密钥**
   - JWT_SECRET 至少 64 个随机字符
   - 数据库密码使用强密码

3. **定期更换密钥**
   - 生产环境建议每 3-6 个月更换一次

4. **使用环境变量管理服务**
   - 生产环境考虑使用 AWS Secrets Manager、Azure Key Vault 等服务

## 🎯 快速配置模板

### 最小配置（开发环境）

```env
DATABASE_URL="mysql://root:123456@localhost:3306/itemreminder"
JWT_SECRET="dev_secret_at_least_32_characters_long"
JWT_EXPIRES_IN="7d"
PORT=3000
NODE_ENV="development"
```

### 完整配置（生产环境）

```env
DATABASE_URL="mysql://prod_user:strong_password@prod-host:3306/itemreminder_prod"
JWT_SECRET="64_character_random_string_generated_by_crypto_module_here"
JWT_EXPIRES_IN="7d"
PORT=3000
NODE_ENV="production"
CORS_ORIGIN="https://your-domain.com"
LOG_LEVEL="warn"
```

---

**配置完成后，继续执行**:
```bash
npm install
npm run prisma:generate
npm run prisma:push
npm run dev
```
