import { PrismaClient } from "@prisma/client";
import dotenv from "dotenv";

// 确保环境变量已加载（如果还没有加载）
if (!process.env.DATABASE_URL && !process.env.DB_HOST) {
  dotenv.config();
}

// 初始化数据库配置（必须在 PrismaClient 之前）
import "./config/database";

export const prisma = new PrismaClient();
