# PowerShell 脚本：创建 .env 文件

Write-Host "====================================" -ForegroundColor Cyan
Write-Host "创建 .env 文件" -ForegroundColor Cyan
Write-Host "====================================" -ForegroundColor Cyan
Write-Host ""

# 检查 .env 是否已存在
if (Test-Path ".env") {
    Write-Host "[警告] .env 文件已存在！" -ForegroundColor Yellow
    Write-Host ""
    $overwrite = Read-Host "是否覆盖现有文件？(y/N)"
    if ($overwrite -ne "y" -and $overwrite -ne "Y") {
        Write-Host "操作已取消。" -ForegroundColor Yellow
        exit
    }
}

# .env 文件内容
$envContent = @"
# 数据库连接 URL
# 格式: mysql://用户名:密码@主机:端口/数据库名
DATABASE_URL="mysql://root:123456@localhost:3306/itemreminder"

# JWT 密钥（开发环境示例，生产环境务必修改）
JWT_SECRET="dev_secret_key_change_this_in_production_32chars"

# JWT 过期时间
JWT_EXPIRES_IN="7d"

# 服务器端口
PORT=3000

# 运行环境
NODE_ENV="development"
"@

# 创建 .env 文件
try {
    $envContent | Out-File -FilePath ".env" -Encoding UTF8 -NoNewline
    Write-Host "[成功] .env 文件已创建！" -ForegroundColor Green
    Write-Host ""
    Write-Host "文件位置: $pwd\.env" -ForegroundColor Gray
    Write-Host ""
    
    Write-Host "====================================" -ForegroundColor Cyan
    Write-Host "下一步操作：" -ForegroundColor Cyan
    Write-Host "====================================" -ForegroundColor Cyan
    Write-Host "1. 编辑 .env 文件，修改数据库配置" -ForegroundColor White
    Write-Host "2. 运行: npm install" -ForegroundColor White
    Write-Host "3. 运行: npm run prisma:generate" -ForegroundColor White
    Write-Host "4. 运行: npm run prisma:push" -ForegroundColor White
    Write-Host "5. 运行: npm run dev" -ForegroundColor White
    Write-Host ""
    
    # 询问是否打开编辑器
    $edit = Read-Host "是否现在打开 .env 文件编辑？(Y/n)"
    if ($edit -ne "n" -and $edit -ne "N") {
        if (Get-Command code -ErrorAction SilentlyContinue) {
            code ".env"
            Write-Host "已使用 VSCode 打开文件" -ForegroundColor Green
        } else {
            notepad ".env"
            Write-Host "已使用记事本打开文件" -ForegroundColor Green
        }
    }
} catch {
    Write-Host "[错误] 创建 .env 文件失败！" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
}

Write-Host ""
Write-Host "按任意键退出..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
