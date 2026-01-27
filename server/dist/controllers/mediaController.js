"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.presignUpload = presignUpload;
exports.presignRead = presignRead;
const client_s3_1 = require("@aws-sdk/client-s3");
const s3_request_presigner_1 = require("@aws-sdk/s3-request-presigner");
const response_1 = require("../utils/response");
const storage_1 = require("../utils/storage");
const crypto_1 = __importDefault(require("crypto"));
const MAX_BYTES = Number(process.env.IMAGE_MAX_BYTES || 5 * 1024 * 1024);
const SIGNED_URL_TTL = Number(process.env.SIGNED_URL_TTL_SECONDS || 300);
const STORAGE_SSE = process.env.STORAGE_SSE || "AES256";
const STORAGE_SSE_KMS_KEY = process.env.STORAGE_SSE_KMS_KEY || "";
const ALLOWED_MIME = new Map([
    ["image/jpeg", "jpg"],
    ["image/png", "png"],
    ["image/webp", "webp"]
]);
function buildObjectKey(userId, itemUuid, ext) {
    const itemSegment = itemUuid?.trim() ? itemUuid.trim() : "unassigned";
    const fileName = crypto_1.default.randomUUID();
    return `users/${userId}/items/${itemSegment}/${fileName}.${ext}`;
}
async function presignUpload(req, res) {
    const userId = req.user?.id;
    if (!userId) {
        return res.status(401).json((0, response_1.fail)({
            code: "UNAUTHORIZED",
            message: "Missing user context"
        }));
    }
    const { mimeType, fileSize, itemUuid } = req.body || {};
    if (typeof mimeType !== "string" || typeof fileSize !== "number") {
        return res.status(400).json((0, response_1.fail)({
            code: "INVALID_INPUT",
            message: "Missing mimeType or fileSize"
        }));
    }
    const ext = ALLOWED_MIME.get(mimeType);
    if (!ext) {
        return res.status(400).json((0, response_1.fail)({
            code: "INVALID_INPUT",
            message: "Unsupported mime type"
        }));
    }
    if (fileSize <= 0 || fileSize > MAX_BYTES) {
        return res.status(400).json((0, response_1.fail)({
            code: "INVALID_INPUT",
            message: "File size exceeds limit"
        }));
    }
    const storage = (0, storage_1.getStorageConfig)();
    const s3 = (0, storage_1.getS3Client)();
    const objectKey = buildObjectKey(userId, typeof itemUuid === "string" ? itemUuid : null, ext);
    const command = new client_s3_1.PutObjectCommand({
        Bucket: storage.bucket,
        Key: objectKey,
        ContentType: mimeType,
        ServerSideEncryption: STORAGE_SSE,
        SSEKMSKeyId: STORAGE_SSE === "aws:kms" && STORAGE_SSE_KMS_KEY ? STORAGE_SSE_KMS_KEY : undefined
    });
    const uploadUrl = await (0, s3_request_presigner_1.getSignedUrl)(s3, command, {
        expiresIn: SIGNED_URL_TTL
    });
    const requiredHeaders = {};
    if (STORAGE_SSE) {
        requiredHeaders["x-amz-server-side-encryption"] = STORAGE_SSE;
    }
    if (STORAGE_SSE === "aws:kms" && STORAGE_SSE_KMS_KEY) {
        requiredHeaders["x-amz-server-side-encryption-aws-kms-key-id"] = STORAGE_SSE_KMS_KEY;
    }
    return res.json((0, response_1.ok)({
        uploadUrl,
        objectKey,
        expiresIn: SIGNED_URL_TTL,
        requiredHeaders
    }));
}
async function presignRead(req, res) {
    const userId = req.user?.id;
    if (!userId) {
        return res.status(401).json((0, response_1.fail)({
            code: "UNAUTHORIZED",
            message: "Missing user context"
        }));
    }
    const key = typeof req.query.key === "string" ? req.query.key : "";
    if (!key) {
        return res.status(400).json((0, response_1.fail)({
            code: "INVALID_INPUT",
            message: "Missing key"
        }));
    }
    const prefix = `users/${userId}/`;
    if (!key.startsWith(prefix)) {
        return res.status(403).json((0, response_1.fail)({
            code: "FORBIDDEN",
            message: "Access denied"
        }));
    }
    const storage = (0, storage_1.getStorageConfig)();
    const s3 = (0, storage_1.getS3Client)();
    const command = new client_s3_1.GetObjectCommand({
        Bucket: storage.bucket,
        Key: key
    });
    const signedUrl = await (0, s3_request_presigner_1.getSignedUrl)(s3, command, {
        expiresIn: SIGNED_URL_TTL
    });
    return res.json((0, response_1.ok)({
        signedUrl,
        expiresIn: SIGNED_URL_TTL
    }));
}
