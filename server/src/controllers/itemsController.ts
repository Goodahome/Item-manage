import { Request, Response } from "express";
import { z } from "zod";
import { prisma } from "../prisma";
import { fail, ok } from "../utils/response";
import { parsePagination } from "../utils/pagination";
import { recordDeletion } from "../utils/deletedRecords";

const itemSchema = z.object({
  uuid: z.string().min(1),
  name: z.string().min(1),
  description: z.string().optional(),
  categoryUuid: z.string().uuid().nullable().optional(),
  warehouseUuid: z.string().uuid().nullable().optional(),
  tags: z.array(z.string()).optional(),
  purchaseDate: z.string().datetime().nullable().optional(),
  expiryDate: z.string().datetime().nullable().optional(),
  price: z.number().nullable().optional(),
  quantity: z.number().int().optional(),
  barcode: z.string().nullable().optional(),
  imageUri: z.string().nullable().optional(),
  imageUris: z.array(z.string()).optional(),
  primaryImageIndex: z.number().int().optional(),
  featureCode: z.string().nullable().optional(),
  enableStockAlert: z.boolean().optional(),
  createdAt: z.string().datetime().optional(),
  updatedAt: z.string().datetime().optional()
});

export async function listItems(req: Request, res: Response) {
  const { page, pageSize } = parsePagination(req.query);
  const userId = req.user?.uuid;
  if (!userId) {
    return res.status(401).json(
      fail({
        code: "UNAUTHORIZED",
        message: "Missing user context"
      })
    );
  }

  const search = typeof req.query.search === "string" ? req.query.search : "";
  const categoryUuid =
    typeof req.query.categoryUuid === "string" && req.query.categoryUuid.length > 0
      ? req.query.categoryUuid
      : undefined;
  const warehouseUuid =
    typeof req.query.warehouseUuid === "string" && req.query.warehouseUuid.length > 0
      ? req.query.warehouseUuid
      : undefined;

  const where: Record<string, unknown> = { userId };
  if (search) {
    where.name = { contains: search };
  }
  if (categoryUuid) {
    where.categoryUuid = categoryUuid;
  }
  if (warehouseUuid) {
    where.warehouseUuid = warehouseUuid;
  }

  const [items, total] = await Promise.all([
    prisma.item.findMany({
      where,
      orderBy: { updatedAt: "desc" },
      skip: (page - 1) * pageSize,
      take: pageSize
    }),
    prisma.item.count({ where })
  ]);

  const withUuids = items.map((item) => ({
    ...item,
    warehouseUuid: item.warehouseUuid ?? null,
    categoryUuid: item.categoryUuid ?? null
  }));

  return res.json(
    ok({
      items: withUuids,
      page,
      pageSize,
      total
    })
  );
}

export async function getItem(req: Request, res: Response) {
  const userId = req.user?.uuid;
  if (!userId) {
    return res.status(401).json(
      fail({
        code: "UNAUTHORIZED",
        message: "Missing user context"
      })
    );
  }

  const uuid = String(req.params.uuid);
  const item = await prisma.item.findFirst({
    where: {
      uuid,
      userId
    }
  });
  if (!item) {
    return res.status(404).json(
      fail({
        code: "NOT_FOUND",
        message: "Item not found"
      })
    );
  }

  return res.json(
    ok({
      ...item,
      warehouseUuid: item.warehouseUuid ?? null,
      categoryUuid: item.categoryUuid ?? null
    })
  );
}

export async function upsertItem(req: Request, res: Response) {
  const userId = req.user?.uuid;
  if (!userId) {
    return res.status(401).json(
      fail({
        code: "UNAUTHORIZED",
        message: "Missing user context"
      })
    );
  }

  const parsed = itemSchema.safeParse(req.body);
  if (!parsed.success) {
    return res.status(400).json(
      fail({
        code: "INVALID_INPUT",
        message: "Invalid item payload",
        details: parsed.error.flatten()
      })
    );
  }

  const data = parsed.data;
  const createdAt = data.createdAt ? new Date(data.createdAt) : undefined;
  const updatedAt = data.updatedAt ? new Date(data.updatedAt) : undefined;
  const categoryUuid = data.categoryUuid ?? null;
  const warehouseUuid = data.warehouseUuid ?? null;

  const item = await prisma.item.upsert({
    where: {
      uuid_userId: {
        uuid: data.uuid,
        userId
      }
    },
    update: {
      name: data.name,
      description: data.description ?? "",
      categoryUuid,
      warehouseUuid,
      tags: data.tags ?? [],
      purchaseDate: data.purchaseDate ? new Date(data.purchaseDate) : null,
      expiryDate: data.expiryDate ? new Date(data.expiryDate) : null,
      price: data.price ?? null,
      quantity: data.quantity ?? 1,
      barcode: data.barcode ?? null,
      imageUri: data.imageUri ?? null,
      imageUris: data.imageUris ?? [],
      primaryImageIndex: data.primaryImageIndex ?? 0,
      featureCode: data.featureCode ?? null,
      enableStockAlert: data.enableStockAlert ?? true,
      createdAt: createdAt ?? undefined,
      updatedAt: updatedAt ?? undefined
    },
    create: {
      uuid: data.uuid,
      userId,
      name: data.name,
      description: data.description ?? "",
      categoryUuid,
      warehouseUuid,
      tags: data.tags ?? [],
      purchaseDate: data.purchaseDate ? new Date(data.purchaseDate) : null,
      expiryDate: data.expiryDate ? new Date(data.expiryDate) : null,
      price: data.price ?? null,
      quantity: data.quantity ?? 1,
      barcode: data.barcode ?? null,
      imageUri: data.imageUri ?? null,
      imageUris: data.imageUris ?? [],
      primaryImageIndex: data.primaryImageIndex ?? 0,
      featureCode: data.featureCode ?? null,
      enableStockAlert: data.enableStockAlert ?? true,
      createdAt: createdAt ?? undefined,
      updatedAt: updatedAt ?? undefined
    }
  });

  return res.json(ok(item));
}

export async function deleteItem(req: Request, res: Response) {
  const userId = req.user?.uuid;
  if (!userId) {
    return res.status(401).json(
      fail({
        code: "UNAUTHORIZED",
        message: "Missing user context"
      })
    );
  }

  const uuid = String(req.params.uuid);
  const item = await prisma.item.findFirst({
    where: {
      uuid,
      userId
    }
  });

  if (item) {
    await prisma.item.delete({
      where: { uuid_userId: { uuid: item.uuid, userId: item.userId } }
    });
  }

  await recordDeletion(userId, "item", uuid);

  return res.json(ok({ deleted: true }));
}
