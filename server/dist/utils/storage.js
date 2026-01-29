"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.getStorageConfig = getStorageConfig;
exports.getS3Client = getS3Client;
const client_s3_1 = require("@aws-sdk/client-s3");
function getEnvBoolean(key, fallback) {
    const raw = process.env[key];
    if (!raw)
        return fallback;
    return raw.toLowerCase() === "true";
}
function getStorageConfig() {
    const bucket = process.env.STORAGE_BUCKET;
    const accessKeyId = process.env.STORAGE_ACCESS_KEY;
    const secretAccessKey = process.env.STORAGE_SECRET_KEY;
    if (!bucket || !accessKeyId || !secretAccessKey) {
        throw new Error("Missing storage configuration");
    }
    return {
        bucket,
        region: process.env.STORAGE_REGION || "auto",
        endpoint: process.env.STORAGE_ENDPOINT || undefined,
        forcePathStyle: getEnvBoolean("STORAGE_FORCE_PATH_STYLE", Boolean(process.env.STORAGE_ENDPOINT)),
        credentials: {
            accessKeyId,
            secretAccessKey
        }
    };
}
function getS3Client() {
    const config = getStorageConfig();
    // 确保端点格式正确（移除末尾斜杠）
    let endpoint = config.endpoint;
    if (endpoint) {
        endpoint = endpoint.replace(/\/$/, ''); // 移除末尾斜杠
    }
    console.log(`[getS3Client] 配置 S3 客户端: endpoint=${endpoint}, bucket=${config.bucket}, region=${config.region}, forcePathStyle=${config.forcePathStyle}`);
    return new client_s3_1.S3Client({
        region: config.region,
        endpoint: endpoint,
        forcePathStyle: config.forcePathStyle,
        credentials: config.credentials
    });
}
