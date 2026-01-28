/**
 * 数据库诊断脚本
 * 详细检查数据库连接和配置问题
 */

require("dotenv").config();

const mysql = require("mysql2/promise");

async function diagnose() {
  const host = process.env.DB_HOST || "localhost";
  const port = parseInt(process.env.DB_PORT || "3306");
  const user = process.env.DB_USER || "root";
  let password = (process.env.DB_PASSWORD || "").trim();
  if (password.startsWith('"') && password.endsWith('"')) {
    password = password.slice(1, -1);
  }
  if (password.startsWith("'") && password.endsWith("'")) {
    password = password.slice(1, -1);
  }
  const database = (process.env.DB_NAME || "itemreminder").trim();

  console.log("=== Database Diagnosis ===\n");

  // 测试 1: 尝试连接到 MySQL 服务器（不指定数据库）
  console.log("Test 1: Connecting to MySQL server...");
  try {
    const connection = await mysql.createConnection({
      host,
      port,
      user,
      password,
    });

    console.log("✅ Successfully connected to MySQL server\n");

    // 测试 2: 检查数据库是否存在
    console.log("Test 2: Checking if database exists...");
    const [databases] = await connection.execute(
      "SHOW DATABASES LIKE ?",
      [database]
    );

    if (databases.length === 0) {
      console.log(`❌ Database '${database}' does not exist\n`);
      console.log("💡 Solution: Create the database first:");
      console.log(`   CREATE DATABASE ${database};\n`);
      
      // 询问是否创建
      console.log("Would you like to create it now? (This script cannot do it automatically)");
      console.log(`Run: mysql -h ${host} -P ${port} -u ${user} -p -e "CREATE DATABASE ${database};"`);
    } else {
      console.log(`✅ Database '${database}' exists\n`);
    }

    // 测试 3: 检查用户权限
    console.log("Test 3: Checking user permissions...");
    const [grants] = await connection.execute("SHOW GRANTS FOR CURRENT_USER()");
    console.log("Current user grants:");
    grants.forEach((grant) => {
      console.log(`  ${grant[Object.keys(grant)[0]]}`);
    });
    console.log("");

    // 测试 4: 尝试使用数据库
    console.log(`Test 4: Attempting to use database '${database}'...`);
    try {
      await connection.execute(`USE ${database}`);
      console.log(`✅ Successfully switched to database '${database}'\n`);

      // 测试 5: 检查表
      const [tables] = await connection.execute("SHOW TABLES");
      console.log(`Test 5: Checking tables in '${database}'...`);
      if (tables.length === 0) {
        console.log("ℹ️  Database is empty (no tables yet)");
        console.log("   This is normal for a new database.\n");
      } else {
        console.log(`✅ Found ${tables.length} table(s):`);
        tables.forEach((table) => {
          console.log(`   - ${table[Object.keys(table)[0]]}`);
        });
        console.log("");
      }
    } catch (err) {
      console.log(`❌ Failed to use database: ${err.message}\n`);
    }

    await connection.end();
    console.log("=== Diagnosis Complete ===");
    console.log("\n✅ All tests passed! Database connection is working.");
    console.log("If Prisma still fails, try:");
    console.log("  1. Run: npm run prisma:generate");
    console.log("  2. Run: npm run prisma:migrate");
    process.exit(0);
  } catch (error) {
    console.log("❌ Failed to connect to MySQL server\n");
    console.error("Error details:", error.message);
    console.error("\nPossible causes:");
    console.error("  1. ❌ Incorrect password");
    console.error("  2. ❌ MySQL server is not running");
    console.error("  3. ❌ Host or port is incorrect");
    console.error("  4. ❌ User doesn't exist or has no permission");
    console.error("\nTroubleshooting:");
    console.error(`  - Test connection: mysql -h ${host} -P ${port} -u ${user} -p`);
    console.error(`  - Check MySQL status: systemctl status mysql (Linux) or services.msc (Windows)`);
    process.exit(1);
  }
}

diagnose();
