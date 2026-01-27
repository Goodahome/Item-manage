"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.loginSchema = exports.registerSchema = void 0;
const zod_1 = require("zod");
exports.registerSchema = zod_1.z.object({
    account: zod_1.z.string().min(3).max(50),
    displayName: zod_1.z.string().min(1).max(50),
    password: zod_1.z.string().min(6).max(100)
});
exports.loginSchema = zod_1.z.object({
    account: zod_1.z.string().min(3).max(50),
    password: zod_1.z.string().min(6).max(100)
});
