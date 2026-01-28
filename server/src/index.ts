import express from "express";
import cors from "cors";
import morgan from "morgan";
import compression from "compression";
import dotenv from "dotenv";

// 必须先加载环境变量，然后才能初始化数据库配置
dotenv.config();

// 初始化数据库配置（必须在 dotenv.config() 之后）
import "./config/database";

import { errorHandler } from "./middleware/errorHandler";
import { authRateLimiter, apiRateLimiter } from "./middleware/rateLimiter";
import authRoutes from "./routes/authRoutes";
import itemsRoutes from "./routes/itemsRoutes";
import categoriesRoutes from "./routes/categoriesRoutes";
import warehousesRoutes from "./routes/warehousesRoutes";
import shoppingItemsRoutes from "./routes/shoppingItemsRoutes";
import remindersRoutes from "./routes/remindersRoutes";
import activityEventsRoutes from "./routes/activityEventsRoutes";
import deletedRecordsRoutes from "./routes/deletedRecordsRoutes";
import mediaRoutes from "./routes/mediaRoutes";
import syncRoutes from "./routes/syncRoutes";

const app = express();
const PORT = process.env.PORT || 3000;
const IS_PRODUCTION = process.env.NODE_ENV === "production";
const FORCE_HTTPS = process.env.FORCE_HTTPS === "false";

// 中间件
app.use(cors());
app.use(compression()); // Gzip 压缩
app.use(express.json({ limit: "10mb" }));
app.use(express.urlencoded({ extended: true, limit: "10mb" }));
app.use(morgan("combined"));

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
app.use("/api/auth", authRateLimiter, authRoutes); // 认证接口：更严格的限制
app.use("/api/items", apiRateLimiter, itemsRoutes);
app.use("/api/categories", apiRateLimiter, categoriesRoutes);
app.use("/api/warehouses", apiRateLimiter, warehousesRoutes);
app.use("/api/shopping-items", apiRateLimiter, shoppingItemsRoutes);
app.use("/api/reminders", apiRateLimiter, remindersRoutes);
app.use("/api/activity-events", apiRateLimiter, activityEventsRoutes);
app.use("/api/deleted-records", apiRateLimiter, deletedRecordsRoutes);
app.use("/api/media", apiRateLimiter, mediaRoutes);
app.use("/api/sync", apiRateLimiter, syncRoutes);

// 错误处理
app.use(errorHandler);

// 启动服务器
app.listen(PORT, () => {
  console.log(`Server is running on http://localhost:${PORT}`);
  console.log(`Health check: http://localhost:${PORT}/health`);
});
