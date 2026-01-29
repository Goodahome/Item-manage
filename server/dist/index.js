"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const express_1 = __importDefault(require("express"));
const cors_1 = __importDefault(require("cors"));
const morgan_1 = __importDefault(require("morgan"));
const compression_1 = __importDefault(require("compression"));
const dotenv_1 = __importDefault(require("dotenv"));
// 必须先加载环境变量，然后才能初始化数据库配置
dotenv_1.default.config();
// 初始化数据库配置（必须在 dotenv.config() 之后）
require("./config/database");
const errorHandler_1 = require("./middleware/errorHandler");
const rateLimiter_1 = require("./middleware/rateLimiter");
const authRoutes_1 = __importDefault(require("./routes/authRoutes"));
const itemsRoutes_1 = __importDefault(require("./routes/itemsRoutes"));
const categoriesRoutes_1 = __importDefault(require("./routes/categoriesRoutes"));
const warehousesRoutes_1 = __importDefault(require("./routes/warehousesRoutes"));
const shoppingItemsRoutes_1 = __importDefault(require("./routes/shoppingItemsRoutes"));
const remindersRoutes_1 = __importDefault(require("./routes/remindersRoutes"));
const activityEventsRoutes_1 = __importDefault(require("./routes/activityEventsRoutes"));
const deletedRecordsRoutes_1 = __importDefault(require("./routes/deletedRecordsRoutes"));
const mediaRoutes_1 = __importDefault(require("./routes/mediaRoutes"));
const syncRoutes_1 = __importDefault(require("./routes/syncRoutes"));
const app = (0, express_1.default)();
const PORT = process.env.PORT || 3000;
const IS_PRODUCTION = process.env.NODE_ENV === "production";
const FORCE_HTTPS = process.env.FORCE_HTTPS === "false";
// 中间件
app.use((0, cors_1.default)());
app.use((0, compression_1.default)()); // Gzip 压缩
app.use(express_1.default.json({ limit: "10mb" }));
app.use(express_1.default.urlencoded({ extended: true, limit: "10mb" }));
app.use((0, morgan_1.default)("combined"));
// HTTPS 强制（生产环境 + 显式开启）
if (IS_PRODUCTION && FORCE_HTTPS) {
    app.use((req, res, next) => {
        if (req.headers["x-forwarded-proto"] !== "https") {
            return res.redirect("https://" + req.headers.host + req.url);
        }
        next();
    });
}
// 健康检查端点
app.get("/health", (_req, res) => {
    res.json({ status: "ok", timestamp: new Date().toISOString() });
});
// API 路由（带速率限制）
app.use("/api/auth", rateLimiter_1.authRateLimiter, authRoutes_1.default); // 认证接口：更严格的限制
app.use("/api/items", rateLimiter_1.apiRateLimiter, itemsRoutes_1.default);
app.use("/api/categories", rateLimiter_1.apiRateLimiter, categoriesRoutes_1.default);
app.use("/api/warehouses", rateLimiter_1.apiRateLimiter, warehousesRoutes_1.default);
app.use("/api/shopping-items", rateLimiter_1.apiRateLimiter, shoppingItemsRoutes_1.default);
app.use("/api/reminders", rateLimiter_1.apiRateLimiter, remindersRoutes_1.default);
app.use("/api/activity-events", rateLimiter_1.apiRateLimiter, activityEventsRoutes_1.default);
app.use("/api/deleted-records", rateLimiter_1.apiRateLimiter, deletedRecordsRoutes_1.default);
app.use("/api/media", rateLimiter_1.apiRateLimiter, mediaRoutes_1.default);
app.use("/api/sync", rateLimiter_1.apiRateLimiter, syncRoutes_1.default);
// 错误处理
app.use(errorHandler_1.errorHandler);
// 启动服务器
app.listen(PORT, () => {
    console.log(`Server is running on http://localhost:${PORT}`);
    console.log(`Health check: http://localhost:${PORT}/health`);
});
