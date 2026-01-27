@echo off
chcp 65001 >nul
echo ====================================
echo 创建 .env 文件
echo ====================================
echo.

REM 检查 .env 是否已存在
if exist ".env" (
    echo [警告] .env 文件已存在！
    echo.
    set /p overwrite="是否覆盖现有文件？(y/N): "
    if /i not "%overwrite%"=="y" (
        echo 操作已取消。
        pause
        exit /b
    )
)

REM 创建 .env 文件
(
echo # 数据库连接 URL
echo # 格式: mysql://用户名:密码@主机:端口/数据库名
echo DATABASE_URL="mysql://root:123456@localhost:3306/itemreminder"
echo.
echo # JWT 密钥 ^(开发环境示例，生产环境务必修改^)
echo JWT_SECRET="dev_secret_key_change_this_in_production_32chars"
echo.
echo # JWT 过期时间
echo JWT_EXPIRES_IN="7d"
echo.
echo # 服务器端口
echo PORT=3000
echo.
echo # 运行环境
echo NODE_ENV="development"
) > ".env"

if exist ".env" (
    echo [成功] .env 文件已创建！
    echo.
    echo 文件位置: %cd%\.env
    echo.
    echo ====================================
    echo 下一步操作：
    echo ====================================
    echo 1. 编辑 .env 文件，修改数据库配置
    echo 2. 运行: npm install
    echo 3. 运行: npm run prisma:generate
    echo 4. 运行: npm run prisma:push
    echo 5. 运行: npm run dev
    echo.
    echo 按任意键打开 .env 文件编辑...
    pause >nul
    notepad ".env"
) else (
    echo [错误] 创建 .env 文件失败！
)

echo.
pause
