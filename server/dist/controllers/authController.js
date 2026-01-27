"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.register = register;
exports.login = login;
exports.refresh = refresh;
exports.logout = logout;
const bcryptjs_1 = __importDefault(require("bcryptjs"));
const jsonwebtoken_1 = __importDefault(require("jsonwebtoken"));
const prisma_1 = require("../prisma");
const response_1 = require("../utils/response");
const auth_1 = require("../validators/auth");
const JWT_SECRET = process.env.JWT_SECRET || "dev_secret_change_me";
const JWT_EXPIRES_IN = process.env.JWT_EXPIRES_IN || "7d";
async function register(req, res) {
    const parsed = auth_1.registerSchema.safeParse(req.body);
    if (!parsed.success) {
        return res.status(400).json((0, response_1.fail)({
            code: "INVALID_INPUT",
            message: "Invalid register payload",
            details: parsed.error.flatten()
        }));
    }
    const { account, displayName, password } = parsed.data;
    const existing = await prisma_1.prisma.user.findUnique({ where: { account } });
    if (existing) {
        return res.status(409).json((0, response_1.fail)({
            code: "ACCOUNT_EXISTS",
            message: "Account already exists"
        }));
    }
    const passwordHash = await bcryptjs_1.default.hash(password, 10);
    const user = await prisma_1.prisma.user.create({
        data: {
            account,
            displayName,
            passwordHash
        }
    });
    const token = jsonwebtoken_1.default.sign({ id: user.id, account: user.account }, JWT_SECRET, { expiresIn: JWT_EXPIRES_IN });
    return res.json((0, response_1.ok)({
        token,
        user: {
            id: user.id,
            account: user.account,
            displayName: user.displayName
        }
    }));
}
async function login(req, res) {
    const parsed = auth_1.loginSchema.safeParse(req.body);
    if (!parsed.success) {
        return res.status(400).json((0, response_1.fail)({
            code: "INVALID_INPUT",
            message: "Invalid login payload",
            details: parsed.error.flatten()
        }));
    }
    const { account, password } = parsed.data;
    const user = await prisma_1.prisma.user.findUnique({ where: { account } });
    if (!user) {
        return res.status(401).json((0, response_1.fail)({
            code: "INVALID_CREDENTIALS",
            message: "Account or password incorrect"
        }));
    }
    const isValid = await bcryptjs_1.default.compare(password, user.passwordHash);
    if (!isValid) {
        return res.status(401).json((0, response_1.fail)({
            code: "INVALID_CREDENTIALS",
            message: "Account or password incorrect"
        }));
    }
    const token = jsonwebtoken_1.default.sign({ id: user.id, account: user.account }, JWT_SECRET, { expiresIn: JWT_EXPIRES_IN });
    return res.json((0, response_1.ok)({
        token,
        user: {
            id: user.id,
            account: user.account,
            displayName: user.displayName
        }
    }));
}
async function refresh(req, res) {
    if (!req.user) {
        return res.status(401).json((0, response_1.fail)({
            code: "UNAUTHORIZED",
            message: "Missing user context"
        }));
    }
    const token = jsonwebtoken_1.default.sign({ id: req.user.id, account: req.user.account }, JWT_SECRET, { expiresIn: JWT_EXPIRES_IN });
    return res.json((0, response_1.ok)({
        token
    }));
}
async function logout(_req, res) {
    return res.json((0, response_1.ok)({
        message: "Logged out"
    }));
}
