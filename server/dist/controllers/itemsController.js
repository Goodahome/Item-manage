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
const deletedRecords_1 = require("../utils/deletedRecords");
const itemSchema = zod_1.z.object({
    uuid: zod_1.z.string().min(1),
    name: zod_1.z.string().min(1),
    description: zod_1.z.string().optional(),
    categoryUuid: zod_1.z.string().uuid().nullable().optional(),
    warehouseUuid: zod_1.z.string().uuid().nullable().optional(),
    tags: zod_1.z.array(zod_1.z.string()).optional(),
    purchaseDate: zod_1.z.string().datetime().nullable().optional(),
    expiryDate: zod_1.z.string().datetime().nullable().optional(),
    price: zod_1.z.number().nullable().optional(),
    quantity: zod_1.z.number().int().optional(),
    quantityUnit: zod_1.z.string().nullable().optional(),
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
    const userId = req.user?.uuid;
    if (!userId) {
        return res.status(401).json((0, response_1.fail)({
            code: "UNAUTHORIZED",
            message: "Missing user context"
        }));
    }
    const search = typeof req.query.search === "string" ? req.query.search : "";
    const categoryUuid = typeof req.query.categoryUuid === "string" && req.query.categoryUuid.length > 0
        ? req.query.categoryUuid
        : undefined;
    const warehouseUuid = typeof req.query.warehouseUuid === "string" && req.query.warehouseUuid.length > 0
        ? req.query.warehouseUuid
        : undefined;
    const where = { userId };
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
        prisma_1.prisma.item.findMany({
            where,
            orderBy: { updatedAt: "desc" },
            skip: (page - 1) * pageSize,
            take: pageSize
        }),
        prisma_1.prisma.item.count({ where })
    ]);
    const withUuids = items.map((item) => ({
        ...item,
        warehouseUuid: item.warehouseUuid ?? null,
        categoryUuid: item.categoryUuid ?? null
    }));
    return res.json((0, response_1.ok)({
        items: withUuids,
        page,
        pageSize,
        total
    }));
}
async function getItem(req, res) {
    const userId = req.user?.uuid;
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
    return res.json((0, response_1.ok)({
        ...item,
        warehouseUuid: item.warehouseUuid ?? null,
        categoryUuid: item.categoryUuid ?? null
    }));
}
async function upsertItem(req, res) {
    const userId = req.user?.uuid;
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
    const categoryUuid = data.categoryUuid ?? null;
    const warehouseUuid = data.warehouseUuid ?? null;
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
            categoryUuid,
            warehouseUuid,
            tags: data.tags ?? [],
            purchaseDate: data.purchaseDate ? new Date(data.purchaseDate) : null,
            expiryDate: data.expiryDate ? new Date(data.expiryDate) : null,
            price: data.price ?? null,
            quantity: data.quantity ?? 1,
            quantityUnit: data.quantityUnit ?? null,
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
            quantityUnit: data.quantityUnit ?? null,
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
    const userId = req.user?.uuid;
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
    if (item) {
        await prisma_1.prisma.item.delete({
            where: { uuid_userId: { uuid: item.uuid, userId: item.userId } }
        });
    }
    await (0, deletedRecords_1.recordDeletion)(userId, "item", uuid);
    return res.json((0, response_1.ok)({ deleted: true }));
}
