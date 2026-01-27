"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.apiRateLimiter = exports.authRateLimiter = void 0;
exports.createRateLimiter = createRateLimiter;
const response_1 = require("../utils/response");
function getEnvNumber(key, fallback) {
    const raw = process.env[key];
    if (!raw)
        return fallback;
    const parsed = Number(raw);
    return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}
/**
 * 创建速率限制中间件
 * @param config 配置
 */
function createRateLimiter(config = {}) {
    const windowMs = config.windowMs || 15 * 60 * 1000; // 默认 15 分钟
    const maxRequests = config.maxRequests || 100; // 默认 100 次请求
    const requestCounts = new Map();
    // 清理过期的记录（每小时执行一次）
    setInterval(() => {
        const now = Date.now();
        for (const [ip, data] of requestCounts.entries()) {
            if (now > data.resetTime) {
                requestCounts.delete(ip);
            }
        }
    }, 60 * 60 * 1000);
    return (req, res, next) => {
        // 获取客户端 IP
        const ip = req.ip || req.socket.remoteAddress || "unknown";
        const now = Date.now();
        // 获取或创建该 IP 的记录
        let record = requestCounts.get(ip);
        if (!record || now > record.resetTime) {
            // 创建新记录或重置过期记录
            record = {
                count: 1,
                resetTime: now + windowMs
            };
            requestCounts.set(ip, record);
            return next();
        }
        // 增加请求计数
        record.count++;
        // 检查是否超过限制
        if (record.count > maxRequests) {
            const retryAfter = Math.ceil((record.resetTime - now) / 1000);
            res.setHeader("Retry-After", retryAfter.toString());
            res.setHeader("X-RateLimit-Limit", maxRequests.toString());
            res.setHeader("X-RateLimit-Remaining", "0");
            res.setHeader("X-RateLimit-Reset", new Date(record.resetTime).toISOString());
            return res.status(429).json((0, response_1.fail)({
                code: "RATE_LIMIT_EXCEEDED",
                message: `请求过于频繁，请在 ${retryAfter} 秒后重试`
            }));
        }
        // 设置速率限制头
        res.setHeader("X-RateLimit-Limit", maxRequests.toString());
        res.setHeader("X-RateLimit-Remaining", (maxRequests - record.count).toString());
        res.setHeader("X-RateLimit-Reset", new Date(record.resetTime).toISOString());
        next();
    };
}
/**
 * 认证接口的速率限制（更严格）
 */
exports.authRateLimiter = createRateLimiter({
    windowMs: getEnvNumber("RATE_LIMIT_WINDOW_MS", 15 * 60 * 1000),
    maxRequests: getEnvNumber("RATE_LIMIT_AUTH_MAX", 20)
});
/**
 * 一般 API 的速率限制
 */
exports.apiRateLimiter = createRateLimiter({
    windowMs: getEnvNumber("RATE_LIMIT_WINDOW_MS", 15 * 60 * 1000),
    maxRequests: getEnvNumber("RATE_LIMIT_API_MAX", 1000)
});
