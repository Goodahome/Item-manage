"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.listItems = listItems;
exports.getItem = getItem;
exports.upsertItem = upsertItem;
exports.deleteItem = deleteItem;
const zod_1 = require("zod");
const prisma_1 = require("../prisma");
const response_1 = require("../utils/response");
const pagination_1 = require("../utils/pagination");
const itemSchema = zod_1.z.object({
    uuid: zod_1.z.string().min(1),
    name: zod_1.z.string().min(1),
    description: zod_1.z.string().optional(),
    categoryId: zod_1.z.number().int().nullable().optional(),
    categoryUuid: zod_1.z.string().min(1).nullable().optional(),
    warehouseId: zod_1.z.number().int().nullable().optional(),
    warehouseUuid: zod_1.z.string().min(1).nullable().optional(),
    tags: zod_1.z.array(zod_1.z.string()).optional(),
    purchaseDate: zod_1.z.string().datetime().nullable().optional(),
    expiryDate: zod_1.z.string().datetime().nullable().optional(),
    price: zod_1.z.number().nullable().optional(),
    quantity: zod_1.z.number().int().optional(),
    barcode: zod_1.z.string().nullable().optional(),
    imageUri: zod_1.z.string().nullable().optional(),
    imageUris: zod_1.z.array(zod_1.z.string()).optional(),
    primaryImageIndex: zod_1.z.number().int().optional(),
    featureCode: zod_1.z.string().nullable().optional(),
    enableStockAlert: zod_1.z.boolean().optional(),
    createdAt: zod_1.z.string().datetime().optional(),
    updatedAt: zod_1.z.string().datetime().optional()
});
async function listItems(req, res) {
    const { page, pageSize } = (0, pagination_1.parsePagination)(req.query);
    const userId = req.user?.id;
    if (!userId) {
        return res.status(401).json((0, response_1.fail)({
            code: "UNAUTHORIZED",
            message: "Missing user context"
        }));
    }
    const search = typeof req.query.search === "string" ? req.query.search : "";
    const categoryId = typeof req.query.categoryId === "string"
        ? Number(req.query.categoryId)
        : undefined;
    const warehouseId = typeof req.query.warehouseId === "string"
        ? Number(req.query.warehouseId)
        : undefined;
    const where = { userId };
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
        prisma_1.prisma.item.findMany({
            where,
            orderBy: { updatedAt: "desc" },
            skip: (page - 1) * pageSize,
            take: pageSize
        }),
        prisma_1.prisma.item.count({ where })
    ]);
    const warehouseIds = Array.from(new Set(items.map((item) => item.warehouseId).filter((id) => typeof id === "number")));
    const categoryIds = Array.from(new Set(items.map((item) => item.categoryId).filter((id) => typeof id === "number")));
    const [warehouses, categories] = await Promise.all([
        warehouseIds.length
            ? prisma_1.prisma.warehouse.findMany({ where: { userId, id: { in: warehouseIds } } })
            : Promise.resolve([]),
        categoryIds.length
            ? prisma_1.prisma.category.findMany({ where: { userId, id: { in: categoryIds } } })
            : Promise.resolve([])
    ]);
    const warehouseMap = new Map(warehouses.map((wh) => [wh.id, wh.uuid]));
    const categoryMap = new Map(categories.map((cat) => [cat.id, cat.uuid]));
    const withUuids = items.map((item) => ({
        ...item,
        warehouseUuid: item.warehouseId ? warehouseMap.get(item.warehouseId) ?? null : null,
        categoryUuid: item.categoryId ? categoryMap.get(item.categoryId) ?? null : null
    }));
    return res.json((0, response_1.ok)({
        items: withUuids,
        page,
        pageSize,
        total
    }));
}
async function getItem(req, res) {
    const userId = req.user?.id;
    if (!userId) {
        return res.status(401).json((0, response_1.fail)({
            code: "UNAUTHORIZED",
            message: "Missing user context"
        }));
    }
    const uuid = String(req.params.uuid);
    const item = await prisma_1.prisma.item.findFirst({
        where: {
            uuid,
            userId
        }
    });
    if (!item) {
        return res.status(404).json((0, response_1.fail)({
            code: "NOT_FOUND",
            message: "Item not found"
        }));
    }
    const [warehouse, category] = await Promise.all([
        item.warehouseId
            ? prisma_1.prisma.warehouse.findFirst({ where: { id: item.warehouseId, userId } })
            : Promise.resolve(null),
        item.categoryId
            ? prisma_1.prisma.category.findFirst({ where: { id: item.categoryId, userId } })
            : Promise.resolve(null)
    ]);
    return res.json((0, response_1.ok)({
        ...item,
        warehouseUuid: warehouse?.uuid ?? null,
        categoryUuid: category?.uuid ?? null
    }));
}
async function upsertItem(req, res) {
    const userId = req.user?.id;
    if (!userId) {
        return res.status(401).json((0, response_1.fail)({
            code: "UNAUTHORIZED",
            message: "Missing user context"
        }));
    }
    const parsed = itemSchema.safeParse(req.body);
    if (!parsed.success) {
        return res.status(400).json((0, response_1.fail)({
            code: "INVALID_INPUT",
            message: "Invalid item payload",
            details: parsed.error.flatten()
        }));
    }
    const data = parsed.data;
    const createdAt = data.createdAt ? new Date(data.createdAt) : undefined;
    const updatedAt = data.updatedAt ? new Date(data.updatedAt) : undefined;
    const resolvedWarehouse = data.warehouseUuid
        ? await prisma_1.prisma.warehouse.findFirst({ where: { uuid: data.warehouseUuid, userId } })
        : null;
    const resolvedCategory = data.categoryUuid
        ? await prisma_1.prisma.category.findFirst({ where: { uuid: data.categoryUuid, userId } })
        : null;
    // 记录警告：如果客户端发送了warehouseUuid但找不到对应容器
    if (data.warehouseUuid && !resolvedWarehouse) {
        console.warn(`[upsertItem] 警告：物品 ${data.name} (${data.uuid}) 的容器 UUID ${data.warehouseUuid} 未找到，warehouseId 将设置为 null`);
    }
    if (data.categoryUuid && !resolvedCategory) {
        console.warn(`[upsertItem] 警告：物品 ${data.name} (${data.uuid}) 的分类 UUID ${data.categoryUuid} 未找到，categoryId 将设置为 null`);
    }
    const categoryId = resolvedCategory?.id ?? data.categoryId ?? null;
    const warehouseId = resolvedWarehouse?.id ?? data.warehouseId ?? null;
    const item = await prisma_1.prisma.item.upsert({
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
    return res.json((0, response_1.ok)(item));
}
async function deleteItem(req, res) {
    const userId = req.user?.id;
    if (!userId) {
        return res.status(401).json((0, response_1.fail)({
            code: "UNAUTHORIZED",
            message: "Missing user context"
        }));
    }
    const uuid = String(req.params.uuid);
    const item = await prisma_1.prisma.item.findFirst({
        where: {
            uuid,
            userId
        }
    });
    if (!item) {
        return res.status(404).json((0, response_1.fail)({
            code: "NOT_FOUND",
            message: "Item not found"
        }));
    }
    await prisma_1.prisma.item.delete({
        where: {
            id: item.id
        }
    });
    return res.json((0, response_1.ok)({ deleted: true }));
}
