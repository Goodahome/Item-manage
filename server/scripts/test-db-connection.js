/**
 * 测试数据库连接脚本
 * 用于验证数据库配置是否正确
 */

require("dotenv").config();

const mysql = require("mysql2/promise");

async function testConnection() {
  const host = (process.env.DB_HOST || "localhost").trim();
  const port = parseInt((process.env.DB_PORT || "3306").trim());
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

  console.log("Testing database connection...");
  console.log(`  Host: ${host}`);
  console.log(`  Port: ${port}`);
  console.log(`  User: ${user}`);
  console.log(`  Database: ${database}`);
  console.log(`  Password: ${password ? "*".repeat(password.length) : "(empty)"}`);

  try {
    const connection = await mysql.createConnection({
      host,
      port,
      user,
      password,
      database,
    });

    console.log("\n✅ Connection successful!");
    
    // 测试查询
    const [rows] = await connection.execute("SELECT 1 as test");
    console.log("✅ Query test successful:", rows);

    await connection.end();
    process.exit(0);
  } catch (error) {
    console.error("\n❌ Connection failed!");
    console.error("Error details:", error.message);
    console.error("\nPossible issues:");
    console.error("  1. Database password is incorrect");
    console.error("  2. Database user doesn't have permission");
    console.error("  3. Database doesn't exist");
    console.error("  4. MySQL server is not running");
    console.error("  5. Host or port is incorrect");
    process.exit(1);
  }
}

testConnection();
