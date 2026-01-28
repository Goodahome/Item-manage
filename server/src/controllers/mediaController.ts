import { Request, Response } from "express";
import { PutObjectCommand, GetObjectCommand } from "@aws-sdk/client-s3";
import { getSignedUrl } from "@aws-sdk/s3-request-presigner";
import { fail, ok } from "../utils/response";
import { getS3Client, getStorageConfig } from "../utils/storage";
import crypto from "crypto";

const MAX_BYTES = Number(process.env.IMAGE_MAX_BYTES || 5 * 1024 * 1024);
const SIGNED_URL_TTL = Number(process.env.SIGNED_URL_TTL_SECONDS || 300);
// 服务器端加密配置（如果 MinIO 未配置 KMS，设置为空字符串以禁用加密）
const STORAGE_SSE = (process.env.STORAGE_SSE || "").trim();
const STORAGE_SSE_KMS_KEY = (process.env.STORAGE_SSE_KMS_KEY || "").trim();
const ALLOWED_MIME = new Map([
  ["image/jpeg", "jpg"],
  ["image/png", "png"],
  ["image/webp", "webp"]
]);

console.log(
  `[mediaController] STORAGE_SSE=${STORAGE_SSE ? `"${STORAGE_SSE}"` : "(未设置)"}, 服务器端加密: ${STORAGE_SSE !== "" ? "启用" : "禁用"}`
);

function buildObjectKey(userId: string, itemUuid: string | null, ext: string) {
  const itemSegment = itemUuid?.trim() ? itemUuid.trim() : "unassigned";
  const fileName = crypto.randomUUID();
  return `users/${userId}/items/${itemSegment}/${fileName}.${ext}`;
}

export async function presignUpload(req: Request, res: Response) {
  const userId = req.user?.uuid;
  if (!userId) {
    return res.status(401).json(
      fail({
        code: "UNAUTHORIZED",
        message: "Missing user context"
      })
    );
  }

  const { mimeType, fileSize, itemUuid } = req.body || {};
  if (typeof mimeType !== "string" || typeof fileSize !== "number") {
    return res.status(400).json(
      fail({
        code: "INVALID_INPUT",
        message: "Missing mimeType or fileSize"
      })
    );
  }

  const ext = ALLOWED_MIME.get(mimeType);
  if (!ext) {
    return res.status(400).json(
      fail({
        code: "INVALID_INPUT",
        message: "Unsupported mime type"
      })
    );
  }

  if (fileSize <= 0 || fileSize > MAX_BYTES) {
    return res.status(400).json(
      fail({
        code: "INVALID_INPUT",
        message: "File size exceeds limit"
      })
    );
  }

  const storage = getStorageConfig();
  const s3 = getS3Client();
  const objectKey = buildObjectKey(userId, typeof itemUuid === "string" ? itemUuid : null, ext);

  console.log(`[presignUpload] 用户: ${userId}, objectKey: ${objectKey}, 文件大小: ${fileSize}, MIME: ${mimeType}`);
  console.log(`[presignUpload] MinIO 配置: endpoint=${storage.endpoint}, bucket=${storage.bucket}, region=${storage.region}`);

  // 构建 PutObjectCommand，只在配置了 SSE 时添加加密参数
  const commandOptions: any = {
    Bucket: storage.bucket,
    Key: objectKey,
    ContentType: mimeType
  };
  
  // 只在配置了服务器端加密时添加加密参数（未配置或为空则禁用）
  if (STORAGE_SSE !== "") {
    commandOptions.ServerSideEncryption = STORAGE_SSE as "AES256" | "aws:kms";
    if (STORAGE_SSE === "aws:kms" && STORAGE_SSE_KMS_KEY) {
      commandOptions.SSEKMSKeyId = STORAGE_SSE_KMS_KEY;
    }
    console.log(`[presignUpload] 启用服务器端加密: ${STORAGE_SSE}`);
  } else {
    console.log(`[presignUpload] 未启用服务器端加密`);
  }
  
  const command = new PutObjectCommand(commandOptions);

  let uploadUrl = await getSignedUrl(s3, command, {
    expiresIn: SIGNED_URL_TTL
  });

  console.log(`[presignUpload] 生成的原始签名URL: ${uploadUrl}`);

  // 如果生成的签名 URL 使用了错误的端点，替换为正确的端点
  // 确保使用 MinIO API 端点（配置的 endpoint），而不是 Console 端点或其他错误地址
  if (storage.endpoint) {
    const correctEndpoint = storage.endpoint.replace(/\/$/, ''); // 移除末尾的斜杠
    const correctBaseUrl = `${correctEndpoint}/${storage.bucket}`;
    
    // 检查签名 URL 是否使用了错误的端点
    // 如果 URL 包含 Console 地址（21152端口）或其他错误地址，替换为正确的 API 端点（9090端口）
    const urlObj = new URL(uploadUrl);
    const currentHost = urlObj.host; // 例如: 182.140.144.150:21152
    const correctHost = new URL(correctEndpoint).host; // 例如: 182.140.144.150:9090
    
    if (currentHost !== correctHost) {
      // 提取路径和查询参数
      let objectPath = urlObj.pathname;
      // 移除 bucket 名称（如果存在）
      objectPath = objectPath.replace(/^\/itemremindertool/, '');
      // 构建正确的 URL：正确的端点 + bucket + 对象路径 + 查询参数
      uploadUrl = `${correctBaseUrl}${objectPath}${urlObj.search}`;
      console.log(`[presignUpload] 端点修复: ${currentHost} -> ${correctHost}`);
      console.log(`[presignUpload] 修复后的签名URL: ${uploadUrl}`);
    }
  }

  const requiredHeaders: Record<string, string> = {};
  if (STORAGE_SSE !== "") {
    requiredHeaders["x-amz-server-side-encryption"] = STORAGE_SSE;
    if (STORAGE_SSE === "aws:kms" && STORAGE_SSE_KMS_KEY) {
      requiredHeaders["x-amz-server-side-encryption-aws-kms-key-id"] = STORAGE_SSE_KMS_KEY;
    }
  }

  console.log(`[presignUpload] 必需的请求头:`, requiredHeaders);

  return res.json(
    ok({
      uploadUrl,
      objectKey,
      expiresIn: SIGNED_URL_TTL,
      requiredHeaders
    })
  );
}

export async function presignRead(req: Request, res: Response) {
  const userId = req.user?.uuid;
  if (!userId) {
    return res.status(401).json(
      fail({
        code: "UNAUTHORIZED",
        message: "Missing user context"
      })
    );
  }

  const key = typeof req.query.key === "string" ? req.query.key : "";
  if (!key) {
    return res.status(400).json(
      fail({
        code: "INVALID_INPUT",
        message: "Missing key"
      })
    );
  }

  const prefix = `users/${userId}/`;
  if (!key.startsWith(prefix)) {
    return res.status(403).json(
      fail({
        code: "FORBIDDEN",
        message: "Access denied"
      })
    );
  }

  const storage = getStorageConfig();
  const s3 = getS3Client();
  const command = new GetObjectCommand({
    Bucket: storage.bucket,
    Key: key
  });

  let signedUrl = await getSignedUrl(s3, command, {
    expiresIn: SIGNED_URL_TTL
  });

  console.log(`[presignRead] 生成的原始签名URL: ${signedUrl}`);

  // 如果生成的签名 URL 使用了错误的端点，替换为正确的端点
  if (storage.endpoint) {
    const correctEndpoint = storage.endpoint.replace(/\/$/, ''); // 移除末尾的斜杠
    const correctBaseUrl = `${correctEndpoint}/${storage.bucket}`;
    
    // 检查签名 URL 是否使用了错误的端点
    const urlObj = new URL(signedUrl);
    const currentHost = urlObj.host; // 例如: 182.140.144.150:21152
    const correctHost = new URL(correctEndpoint).host; // 例如: 182.140.144.150:9090
    
    if (currentHost !== correctHost) {
      // 提取路径和查询参数
      let objectPath = urlObj.pathname;
      // 移除 bucket 名称（如果存在）
      objectPath = objectPath.replace(/^\/itemremindertool/, '');
      // 构建正确的 URL：正确的端点 + bucket + 对象路径 + 查询参数
      signedUrl = `${correctBaseUrl}${objectPath}${urlObj.search}`;
      console.log(`[presignRead] 端点修复: ${currentHost} -> ${correctHost}`);
      console.log(`[presignRead] 修复后的签名URL: ${signedUrl}`);
    }
  }

  return res.json(
    ok({
      signedUrl,
      expiresIn: SIGNED_URL_TTL
    })
  );
}
