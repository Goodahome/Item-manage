/**
 * 显示构建的 DATABASE_URL（隐藏密码）
 * 用于调试数据库连接问题
 */

require("dotenv").config();

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

console.log("=== Database Configuration ===");
console.log(`DB_HOST: ${host}`);
console.log(`DB_PORT: ${port}`);
console.log(`DB_USER: ${user}`);
console.log(`DB_PASSWORD: ${password ? "*".repeat(password.length) : "(empty)"}`);
console.log(`DB_NAME: ${database}`);
console.log("");

// 检查密码是否包含引号
if (password.startsWith('"') && password.endsWith('"')) {
  console.log("⚠️  WARNING: Password appears to have quotes around it!");
  console.log("   This might cause authentication issues.");
  console.log("   Remove quotes from .env file if present.");
  console.log("");
}

// 对密码进行 URL 编码
const encodedPassword = encodeURIComponent(password);
const databaseUrl = `mysql://${user}:${encodedPassword}@${host}:${port}/${database}`;

console.log("=== Generated DATABASE_URL ===");
console.log(`mysql://${user}:***@${host}:${port}/${database}`);
console.log("");
console.log("Full URL (for debugging):");
console.log(databaseUrl);
console.log("");
console.log("=== Test Connection ===");
console.log("Run: npm run test:db");
console.log("Or test manually with MySQL client:");
console.log(`  mysql -h ${host} -P ${port} -u ${user} -p ${database}`);
