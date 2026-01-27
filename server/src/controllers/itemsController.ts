import { Request, Response } from "express";
import { z } from "zod";
import { prisma } from "../prisma";
import { fail, ok } from "../utils/response";
import { parsePagination } from "../utils/pagination";

const itemSchema = z.object({
  uuid: z.string().min(1),
  name: z.string().min(1),
  description: z.string().optional(),
  categoryId: z.number().int().nullable().optional(),
  categoryUuid: z.string().min(1).nullable().optional(),
  warehouseId: z.number().int().nullable().optional(),
  warehouseUuid: z.string().min(1).nullable().optional(),
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
  const userId = req.user?.id;
  if (!userId) {
    return res.status(401).json(
      fail({
        code: "UNAUTHORIZED",
        message: "Missing user context"
      })
    );
  }

  const search = typeof req.query.search === "string" ? req.query.search : "";
  const categoryId =
    typeof req.query.categoryId === "string"
      ? Number(req.query.categoryId)
      : undefined;
  const warehouseId =
    typeof req.query.warehouseId === "string"
      ? Number(req.query.warehouseId)
      : undefined;

  const where: Record<string, unknown> = { userId };
  if (search) {
    where.name = { contains: search };
  }
  if (!Number.isNaN(categoryId)) {
    where.categoryId = categoryId;
  }
  if (!Number.isNaN(warehouseId)) {
    where.warehouseId = warehouseId;
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

  const warehouseIds = Array.from(
    new Set(items.map((item) => item.warehouseId).filter((id): id is number => typeof id === "number"))
  );
  const categoryIds = Array.from(
    new Set(items.map((item) => item.categoryId).filter((id): id is number => typeof id === "number"))
  );
  const [warehouses, categories] = await Promise.all([
    warehouseIds.length
      ? prisma.warehouse.findMany({ where: { userId, id: { in: warehouseIds } } })
      : Promise.resolve([]),
    categoryIds.length
      ? prisma.category.findMany({ where: { userId, id: { in: categoryIds } } })
      : Promise.resolve([])
  ]);
  const warehouseMap = new Map(warehouses.map((wh) => [wh.id, wh.uuid]));
  const categoryMap = new Map(categories.map((cat) => [cat.id, cat.uuid]));

  const withUuids = items.map((item) => ({
    ...item,
    warehouseUuid: item.warehouseId ? warehouseMap.get(item.warehouseId) ?? null : null,
    categoryUuid: item.categoryId ? categoryMap.get(item.categoryId) ?? null : null
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
  const userId = req.user?.id;
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

  const [warehouse, category] = await Promise.all([
    item.warehouseId
      ? prisma.warehouse.findFirst({ where: { id: item.warehouseId, userId } })
      : Promise.resolve(null),
    item.categoryId
      ? prisma.category.findFirst({ where: { id: item.categoryId, userId } })
      : Promise.resolve(null)
  ]);

  return res.json(
    ok({
      ...item,
      warehouseUuid: warehouse?.uuid ?? null,
      categoryUuid: category?.uuid ?? null
    })
  );
}

export async function upsertItem(req: Request, res: Response) {
  const userId = req.user?.id;
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
  const resolvedWarehouse = data.warehouseUuid
    ? await prisma.warehouse.findFirst({ where: { uuid: data.warehouseUuid, userId } })
    : null;
  const resolvedCategory = data.categoryUuid
    ? await prisma.category.findFirst({ where: { uuid: data.categoryUuid, userId } })
    : null;
  const categoryId = resolvedCategory?.id ?? data.categoryId ?? null;
  const warehouseId = resolvedWarehouse?.id ?? data.warehouseId ?? null;

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
      categoryId,
      warehouseId,
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
      categoryId,
      warehouseId,
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
  const userId = req.user?.id;
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

  await prisma.item.delete({
    where: {
      id: item.id
    }
  });

  return res.json(ok({ deleted: true }));
}
