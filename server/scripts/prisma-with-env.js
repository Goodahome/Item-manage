/**
 * Prisma 命令包装脚本
 * 在运行 Prisma 命令前设置 DATABASE_URL 环境变量
 */

const { spawn } = require("child_process");
const path = require("path");

// 加载环境变量
require("dotenv").config({ path: path.join(__dirname, "..", ".env") });

// 构建 DATABASE_URL
const host = (process.env.DB_HOST || "localhost").trim();
const port = (process.env.DB_PORT || "3306").trim();
const user = (process.env.DB_USER || "root").trim();
// 去除可能的引号
let password = (process.env.DB_PASSWORD || "").trim();
if (password.startsWith('"') && password.endsWith('"')) {
  password = password.slice(1, -1);
}
if (password.startsWith("'") && password.endsWith("'")) {
  password = password.slice(1, -1);
}
const database = (process.env.DB_NAME || "itemreminder").trim();

// 对密码进行 URL 编码
const encodedPassword = encodeURIComponent(password);
const databaseUrl = `mysql://${user}:${encodedPassword}@${host}:${port}/${database}`;

// 调试信息（不显示完整密码）
console.log("Database configuration:");
console.log(`  Host: ${host}`);
console.log(`  Port: ${port}`);
console.log(`  User: ${user}`);
console.log(`  Database: ${database}`);
console.log(`  Password length: ${password.length} characters`);
console.log(`  Encoded password: ${encodedPassword}`);
console.log(`  Connection URL: mysql://${user}:***@${host}:${port}/${database}`);

// 设置环境变量
process.env.DATABASE_URL = databaseUrl;

// 获取要执行的 Prisma 命令和参数
const prismaCommand = process.argv[2]; // 例如: migrate, generate, db push
const prismaArgs = process.argv.slice(3); // 其他参数

// 构建完整的命令
const args = [prismaCommand, ...prismaArgs];

// 运行 Prisma 命令
const prismaProcess = spawn("npx", ["prisma", ...args], {
  stdio: "inherit",
  shell: true,
  env: process.env,
});

prismaProcess.on("close", (code) => {
  process.exit(code || 0);
});

prismaProcess.on("error", (error) => {
  console.error("Error running Prisma command:", error);
  process.exit(1);
});
