# Node 后端集成文档

本文档说明如何使用 Node.js 后端与 Android 客户端进行数据同步。

## 概述

系统采用"本地优先"策略：
- 未登录：纯本地存储，所有操作仅在设备上进行
- 已登录：本地+远端双写，数据自动同步到服务器

## 架构设计

### 数据流

```
┌─────────────┐      ┌──────────────┐      ┌─────────────┐
│   Android   │ ───> │ Local Room   │      │  Node API   │
│     App     │      │   Database   │      │  + MySQL    │
└─────────────┘      └──────────────┘      └─────────────┘
       │                    │                      │
       │                    │    (登录后同步)       │
       └────────────────────┴──────────────────────┘
```

### 关键组件

#### 服务端

- **Express**: Web 框架
- **Prisma**: ORM（对象关系映射）
- **MySQL**: 数据库
- **JWT**: 身份认证
- **Zod**: 数据校验

#### 客户端

- **Room**: 本地数据库
- **Retrofit**: HTTP 客户端
- **OkHttp**: 网络库
- **Gson**: JSON 序列化

#### 同步层

- **AuthManager**: 管理 JWT token 和用户信息
- **SyncManager**: 处理本地与远端的数据同步
- **Repository**: 实现双写逻辑（本地+远端）

## 使用指南

### 1. 启动服务端

```bash
cd server
npm install
npm run prisma:generate
npm run prisma:push
npm run dev
```

服务器将在 `http://localhost:3000` 启动。

### 2. 配置客户端

在 Android 应用的设置中配置服务器地址：

```
设置 -> 服务器地址：http://your-server-ip:3000
```

如果使用模拟器测试本地服务器：
- Android 模拟器使用 `http://10.0.2.2:3000`
- 真机使用电脑的局域网 IP，如 `http://192.168.1.100:3000`

### 3. 注册账号

在 Android 应用中：
1. 打开侧边栏
2. 点击"登录"按钮
3. 点击"注册"
4. 输入显示名、账号和密码
5. 点击"注册"

### 4. 登录

注册成功后自动登录，或手动登录：
1. 打开侧边栏
2. 点击"登录"按钮
3. 输入账号和密码
4. 点击"登录"

### 5. 数据同步

登录后，所有数据操作会自动同步到服务器：

- **添加物品**：本地保存后异步同步到服务器
- **更新物品**：本地更新后异步同步到服务器
- **删除物品**：本地删除后异步删除服务器数据
- **分类/容器**：同样的双写逻辑

## 数据模型

### UUID 策略

- **本地主键**：`Long` 类型的自增 ID（Room 使用）
- **跨端唯一键**：`String` 类型的 UUID（同步使用）
- **服务端主键**：`Int` 类型的自增 ID（MySQL 使用）

所有实体都有 `uuid` 字段用于跨端同步：

```kotlin
data class Item(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,              // 本地主键
    val uuid: String = UUID.randomUUID().toString(),  // 跨端唯一键
    val name: String,
    // ... 其他字段
)
```

### 同步策略

#### 写入流程

1. **本地写入**：先写入本地 Room 数据库
2. **生成/保持 UUID**：新数据生成 UUID，已有数据保持不变
3. **异步同步**：后台异步调用 API upsert（不阻塞 UI）

```kotlin
suspend fun insertItem(item: Item): Long {
    // 1. 本地写入
    val itemId = itemDao.insertItem(item)
    val savedItem = itemDao.getItemById(itemId) ?: item
    
    // 2. 异步同步到远端（不阻塞）
    syncManager?.let { manager ->
        CoroutineScope(Dispatchers.IO).launch {
            manager.syncItemToRemote(savedItem)
        }
    }
    
    return itemId
}
```

#### 删除流程

1. **本地删除**：从 Room 数据库删除
2. **记录删除**：写入 DeletedRecord 表
3. **远端删除**：异步调用 API 删除

#### 冲突处理

当前实现采用"最后写入者胜出"策略：
- 不同设备的修改以最后同步到服务器的为准
- 未来可以根据 `updatedAt` 时间戳实现更复杂的冲突解决

## API 认证

### JWT Token 流程

```
┌──────┐                  ┌──────┐
│Client│                  │Server│
└──┬───┘                  └───┬──┘
   │  POST /auth/login       │
   │  {account, password}    │
   ├────────────────────────>│
   │                         │ 验证密码
   │  {token, user}          │
   │<────────────────────────┤
   │                         │
   │  GET /items             │
   │  Authorization: Bearer  │
   ├────────────────────────>│
   │                         │ 验证 token
   │  {items: [...]}         │
   │<────────────────────────┤
```

### Token 管理

- **存储**：保存在 SharedPreferences 的 `auth_prefs` 中
- **使用**：AuthInterceptor 自动在所有请求头中添加 Bearer token
- **刷新**：调用 `/api/auth/refresh` 获取新 token
- **清除**：登出时调用 `authManager.clearLoginInfo()`

## 错误处理

### 网络错误

同步失败不会阻塞本地操作：
- 本地操作立即完成并返回
- 同步错误仅记录日志，不影响用户体验
- 未来可以实现离线队列重试机制

### 认证错误

- Token 过期：提示用户重新登录
- 无效 Token：自动清除登录状态
- 网络不可达：继续使用本地数据

## 性能优化

### 异步同步

所有远端操作都是异步的，不阻塞 UI：

```kotlin
// 不阻塞
CoroutineScope(Dispatchers.IO).launch {
    syncManager.syncItemToRemote(item)
}
```

### 批量操作

未来可以实现：
- 批量上传多个变更
- 增量同步（只同步变化的数据）
- 压缩传输（gzip）

## 安全考虑

### 密码安全

- **服务端**：使用 bcryptjs hash 存储密码（成本因子 10）
- **传输**：HTTPS 加密（生产环境必须）
- **客户端**：不保存密码，只保存 token

### Token 安全

- **过期时间**：默认 7 天
- **刷新机制**：支持刷新 token 而不需要重新登录
- **安全存储**：使用 SharedPreferences（可升级为 EncryptedSharedPreferences）

### 数据隔离

- 所有数据表都有 `userId` 外键
- API 自动按当前用户过滤数据
- 无法访问其他用户的数据

## 测试

### 手动测试流程

1. **启动服务端**
   ```bash
   cd server
   npm run dev
   ```

2. **配置客户端**
   - 在设置中输入服务器地址
   - 使用模拟器：`http://10.0.2.2:3000`

3. **注册和登录**
   - 注册新账号
   - 验证登录成功

4. **测试同步**
   - 添加物品，查看服务器日志
   - 更新物品，查看服务器日志
   - 删除物品，查看服务器日志

5. **验证数据一致性**
   - 使用 MySQL 客户端查看数据库
   - 确认数据正确同步

### API 测试

使用 curl 或 Postman 测试 API：

```bash
# 注册
curl -X POST http://localhost:3000/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"account":"test","displayName":"测试用户","password":"123456"}'

# 登录
curl -X POST http://localhost:3000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"account":"test","password":"123456"}'

# 获取物品列表（需要 token）
curl -X GET http://localhost:3000/api/items \
  -H "Authorization: Bearer <token>"
```

## 故障排除

### 无法连接到服务器

- 检查服务器是否运行：`curl http://localhost:3000/health`
- 检查防火墙设置
- 确认客户端的服务器地址配置正确

### 登录失败

- 检查账号密码是否正确
- 查看服务器日志是否有错误
- 确认数据库连接正常

### 同步失败

- 检查网络连接
- 查看客户端日志：`adb logcat | grep ItemRepository`
- 查看服务器日志

## 未来改进

### 功能增强

- [ ] 离线队列：网络恢复后自动重试失败的同步
- [ ] 增量同步：只同步变化的数据
- [ ] 冲突解决：基于时间戳的智能合并
- [ ] 批量操作：减少网络请求次数
- [ ] 图片同步：上传物品图片到服务器

### 安全增强

- [ ] HTTPS 强制（生产环境）
- [ ] Token 刷新机制完善
- [ ] 加密存储敏感数据
- [ ] 速率限制（防止 API 滥用）
- [ ] 双因素认证

### 性能优化

- [ ] 数据压缩（gzip）
- [ ] 连接池优化
- [ ] Redis 缓存
- [ ] CDN 加速（图片等静态资源）

## 附录

### 相关文件

#### 服务端
- `server/src/index.ts` - 入口文件
- `server/src/controllers/` - 控制器
- `server/src/routes/` - 路由定义
- `server/prisma/schema.prisma` - 数据模型

#### 客户端
- `app/src/main/java/.../auth/AuthManager.kt` - 认证管理
- `app/src/main/java/.../network/` - 网络层
- `app/src/main/java/.../sync/SyncManager.kt` - 同步管理
- `app/src/main/java/.../data/repository/` - Repository 层

### 参考资料

- [Retrofit 文档](https://square.github.io/retrofit/)
- [Prisma 文档](https://www.prisma.io/docs)
- [JWT 介绍](https://jwt.io/introduction)
- [Room 数据库](https://developer.android.com/training/data-storage/room)

## 支持

如有问题，请查看：
- 服务器日志：`server/logs/`
- 客户端日志：`adb logcat`
- 数据库状态：MySQL Workbench
