"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.prisma = void 0;
const client_1 = require("@prisma/client");
const dotenv_1 = __importDefault(require("dotenv"));
// 确保环境变量已加载（如果还没有加载）
if (!process.env.DATABASE_URL && !process.env.DB_HOST) {
    dotenv_1.default.config();
}
// 初始化数据库配置（必须在 PrismaClient 之前）
require("./config/database");
exports.prisma = new client_1.PrismaClient();
