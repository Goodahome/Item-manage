# Release 版本编译指南

本指南将帮助您配置签名并编译可安装的 Release 版本。

## 步骤 1: 创建签名密钥库（Keystore）

在项目根目录下执行以下命令创建密钥库：

### Windows (PowerShell):
```powershell
keytool -genkey -v -keystore app/release.keystore -alias release -keyalg RSA -keysize 2048 -validity 10000
```

### Linux/Mac:
```bash
keytool -genkey -v -keystore app/release.keystore -alias release -keyalg RSA -keysize 2048 -validity 10000
```

**重要提示：**
- 请记住您输入的密钥库密码和密钥密码，后续需要用到
- 密钥库文件 `app/release.keystore` 已添加到 `.gitignore`，不会提交到版本控制
- 请妥善保管密钥库文件，丢失后将无法更新应用

## 步骤 2: 配置签名信息

1. 复制示例配置文件：
   ```bash
   cp keystore.properties.example keystore.properties
   ```

2. 编辑 `keystore.properties` 文件，填写您的签名信息：
   ```properties
   storeFile=app/release.keystore
   storePassword=您的密钥库密码
   keyAlias=release
   keyPassword=您的密钥密码
   ```

**注意：** `keystore.properties` 文件已添加到 `.gitignore`，不会提交到版本控制。

## 步骤 3: 编译 Release 版本

### 方法 1: 使用 Gradle 命令行

在项目根目录执行：

**Windows:**
```powershell
.\gradlew assembleRelease
```

**Linux/Mac:**
```bash
./gradlew assembleRelease
```

编译完成后，APK 文件位于：
```
app/build/outputs/apk/release/app-release.apk
```

### 方法 2: 使用 Android Studio

1. 在 Android Studio 中，点击菜单 **Build** → **Select Build Variants**
2. 在 **Build Variants** 面板中，将 **app** 模块的构建变体设置为 **release**
3. 点击菜单 **Build** → **Build Bundle(s) / APK(s)** → **Build APK(s)**
4. 编译完成后，点击通知中的 **locate** 链接找到 APK 文件

## 步骤 4: 安装 APK

将生成的 `app-release.apk` 文件传输到手机，然后安装。

**注意：** 如果手机提示"禁止安装未知来源的应用"，请在手机设置中允许安装未知来源的应用。

## 常见问题

### Q: 编译时提示找不到 keystore.properties
A: 请确保已创建 `keystore.properties` 文件并填写了正确的签名信息。

### Q: 编译时提示签名配置错误
A: 请检查 `keystore.properties` 中的密码和别名是否正确，以及密钥库文件路径是否正确。

### Q: 如何更新应用？
A: 使用相同的密钥库文件签名，确保 `versionCode` 递增（在 `app/build.gradle.kts` 中修改）。

### Q: 忘记了密钥库密码怎么办？
A: 如果丢失密钥库密码，将无法更新已发布的应用。请务必妥善保管密钥库文件和密码。

## 安全建议

1. **不要将密钥库文件提交到版本控制**
2. **备份密钥库文件到安全的位置**
3. **使用强密码保护密钥库**
4. **不要在公共场合或网络上分享密钥库文件**

