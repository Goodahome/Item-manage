/**
 * 设置 DATABASE_URL 环境变量的脚本
 * 用于 Prisma 命令（migrate、generate 等）
 * 这些命令不会运行应用代码，所以需要单独设置环境变量
 */

require("dotenv").config();

const host = process.env.DB_HOST || "localhost";
const port = process.env.DB_PORT || "3306";
const user = process.env.DB_USER || "root";
const password = process.env.DB_PASSWORD || "";
const database = process.env.DB_NAME || "itemreminder";

// 对密码进行 URL 编码，处理特殊字符
const encodedPassword = encodeURIComponent(password);

// 构建连接字符串
const databaseUrl = `mysql://${user}:${encodedPassword}@${host}:${port}/${database}`;

// 设置环境变量
process.env.DATABASE_URL = databaseUrl;

// 导出供其他脚本使用
module.exports = databaseUrl;
