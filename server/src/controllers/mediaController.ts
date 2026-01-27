import { Request, Response } from "express";
import { PutObjectCommand, GetObjectCommand } from "@aws-sdk/client-s3";
import { getSignedUrl } from "@aws-sdk/s3-request-presigner";
import { fail, ok } from "../utils/response";
import { getS3Client, getStorageConfig } from "../utils/storage";
import crypto from "crypto";

const MAX_BYTES = Number(process.env.IMAGE_MAX_BYTES || 5 * 1024 * 1024);
const SIGNED_URL_TTL = Number(process.env.SIGNED_URL_TTL_SECONDS || 300);
const STORAGE_SSE = process.env.STORAGE_SSE || "AES256";
const STORAGE_SSE_KMS_KEY = process.env.STORAGE_SSE_KMS_KEY || "";
const ALLOWED_MIME = new Map([
  ["image/jpeg", "jpg"],
  ["image/png", "png"],
  ["image/webp", "webp"]
]);

function buildObjectKey(userId: number, itemUuid: string | null, ext: string) {
  const itemSegment = itemUuid?.trim() ? itemUuid.trim() : "unassigned";
  const fileName = crypto.randomUUID();
  return `users/${userId}/items/${itemSegment}/${fileName}.${ext}`;
}

export async function presignUpload(req: Request, res: Response) {
  const userId = req.user?.id;
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

  const command = new PutObjectCommand({
    Bucket: storage.bucket,
    Key: objectKey,
    ContentType: mimeType,
    ServerSideEncryption: STORAGE_SSE as "AES256" | "aws:kms" | undefined,
    SSEKMSKeyId: STORAGE_SSE === "aws:kms" && STORAGE_SSE_KMS_KEY ? STORAGE_SSE_KMS_KEY : undefined
  });

  const uploadUrl = await getSignedUrl(s3, command, {
    expiresIn: SIGNED_URL_TTL
  });

  const requiredHeaders: Record<string, string> = {};
  if (STORAGE_SSE) {
    requiredHeaders["x-amz-server-side-encryption"] = STORAGE_SSE;
  }
  if (STORAGE_SSE === "aws:kms" && STORAGE_SSE_KMS_KEY) {
    requiredHeaders["x-amz-server-side-encryption-aws-kms-key-id"] = STORAGE_SSE_KMS_KEY;
  }

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
  const userId = req.user?.id;
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

  const signedUrl = await getSignedUrl(s3, command, {
    expiresIn: SIGNED_URL_TTL
  });

  return res.json(
    ok({
      signedUrl,
      expiresIn: SIGNED_URL_TTL
    })
  );
}
