"use strict";
/**
 * 数据库配置
 * 从分开的环境变量构建 DATABASE_URL
 */
Object.defineProperty(exports, "__esModule", { value: true });
exports.buildDatabaseUrl = buildDatabaseUrl;
exports.initDatabaseConfig = initDatabaseConfig;
/**
 * 构建 MySQL 连接字符串
 * 自动处理密码中的特殊字符（URL 编码）
 */
function buildDatabaseUrl() {
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
    // 对密码进行 URL 编码，处理特殊字符
    const encodedPassword = encodeURIComponent(password);
    // 构建连接字符串
    const url = `mysql://${user}:${encodedPassword}@${host}:${port}/${database}`;
    return url;
}
/**
 * 初始化数据库配置
 * 如果 DATABASE_URL 未设置，则从分开的配置构建
 */
function initDatabaseConfig() {
    if (!process.env.DATABASE_URL) {
        const url = buildDatabaseUrl();
        process.env.DATABASE_URL = url;
        // 开发环境显示调试信息
        if (process.env.NODE_ENV !== "production") {
            console.log("Database URL built from separate config:");
            const maskedUrl = url.replace(/:[^:@]+@/, ":***@");
            console.log(`  ${maskedUrl}`);
        }
    }
    else {
        // 开发环境显示已存在的 DATABASE_URL
        if (process.env.NODE_ENV !== "production") {
            const maskedUrl = process.env.DATABASE_URL.replace(/:[^:@]+@/, ":***@");
            console.log(`Using existing DATABASE_URL: ${maskedUrl}`);
        }
    }
    // 验证 DATABASE_URL 是否已设置
    if (!process.env.DATABASE_URL) {
        console.error("ERROR: DATABASE_URL is not set!");
        console.error("Please check your .env file or set DB_HOST, DB_PORT, DB_USER, DB_PASSWORD, DB_NAME");
        process.exit(1);
    }
}
// 自动初始化（在导入时执行）
initDatabaseConfig();
