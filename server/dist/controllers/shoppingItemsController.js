"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.listShoppingItems = listShoppingItems;
exports.getShoppingItem = getShoppingItem;
exports.upsertShoppingItem = upsertShoppingItem;
exports.deleteShoppingItem = deleteShoppingItem;
const zod_1 = require("zod");
const prisma_1 = require("../prisma");
const response_1 = require("../utils/response");
const pagination_1 = require("../utils/pagination");
const deletedRecords_1 = require("../utils/deletedRecords");
const shoppingItemSchema = zod_1.z.object({
    uuid: zod_1.z.string().min(1),
    name: zod_1.z.string().min(1),
    description: zod_1.z.string().optional(),
    quantity: zod_1.z.number().int().optional(),
    isCompleted: zod_1.z.boolean().optional(),
    priority: zod_1.z.string().optional(),
    createdAt: zod_1.z.string().datetime().optional(),
    completedAt: zod_1.z.string().datetime().nullable().optional(),
    imageUri: zod_1.z.string().nullable().optional(),
    itemUuid: zod_1.z.string().min(1).nullable().optional()
});
async function listShoppingItems(req, res) {
    const { page, pageSize } = (0, pagination_1.parsePagination)(req.query);
    const userId = req.user?.uuid;
    if (!userId) {
        return res.status(401).json((0, response_1.fail)({
            code: "UNAUTHORIZED",
            message: "Missing user context"
        }));
    }
    const [shoppingItems, total] = await Promise.all([
        prisma_1.prisma.shoppingItem.findMany({
            where: { userId },
            orderBy: { createdAt: "desc" },
            skip: (page - 1) * pageSize,
            take: pageSize
        }),
        prisma_1.prisma.shoppingItem.count({ where: { userId } })
    ]);
    return res.json((0, response_1.ok)({
        shoppingItems,
        page,
        pageSize,
        total
    }));
}
async function getShoppingItem(req, res) {
    const userId = req.user?.uuid;
    if (!userId) {
        return res.status(401).json((0, response_1.fail)({
            code: "UNAUTHORIZED",
            message: "Missing user context"
        }));
    }
    const uuid = String(req.params.uuid);
    const shoppingItem = await prisma_1.prisma.shoppingItem.findFirst({
        where: { uuid, userId }
    });
    if (!shoppingItem) {
        return res.status(404).json((0, response_1.fail)({
            code: "NOT_FOUND",
            message: "Shopping item not found"
        }));
    }
    return res.json((0, response_1.ok)(shoppingItem));
}
async function upsertShoppingItem(req, res) {
    const userId = req.user?.uuid;
    if (!userId) {
        return res.status(401).json((0, response_1.fail)({
            code: "UNAUTHORIZED",
            message: "Missing user context"
        }));
    }
    const parsed = shoppingItemSchema.safeParse(req.body);
    if (!parsed.success) {
        return res.status(400).json((0, response_1.fail)({
            code: "INVALID_INPUT",
            message: "Invalid shopping item payload",
            details: parsed.error.flatten()
        }));
    }
    const data = parsed.data;
    const createdAt = data.createdAt ? new Date(data.createdAt) : undefined;
    const completedAt = data.completedAt ? new Date(data.completedAt) : null;
    const shoppingItem = await prisma_1.prisma.shoppingItem.upsert({
        where: {
            uuid_userId: {
                uuid: data.uuid,
                userId
            }
        },
        update: {
            name: data.name,
            description: data.description ?? "",
            quantity: data.quantity ?? 1,
            isCompleted: data.isCompleted ?? false,
            priority: data.priority ?? "MEDIUM",
            completedAt,
            imageUri: data.imageUri ?? null,
            itemUuid: data.itemUuid ?? null,
            createdAt: createdAt ?? undefined
        },
        create: {
            uuid: data.uuid,
            userId,
            name: data.name,
            description: data.description ?? "",
            quantity: data.quantity ?? 1,
            isCompleted: data.isCompleted ?? false,
            priority: data.priority ?? "MEDIUM",
            completedAt,
            imageUri: data.imageUri ?? null,
            itemUuid: data.itemUuid ?? null,
            createdAt: createdAt ?? undefined
        }
    });
    return res.json((0, response_1.ok)(shoppingItem));
}
async function deleteShoppingItem(req, res) {
    const userId = req.user?.uuid;
    if (!userId) {
        return res.status(401).json((0, response_1.fail)({
            code: "UNAUTHORIZED",
            message: "Missing user context"
        }));
    }
    const uuid = String(req.params.uuid);
    const shoppingItem = await prisma_1.prisma.shoppingItem.findFirst({
        where: { uuid, userId }
    });
    if (shoppingItem) {
        await prisma_1.prisma.shoppingItem.delete({
            where: { uuid_userId: { uuid: shoppingItem.uuid, userId: shoppingItem.userId } }
        });
    }
    await (0, deletedRecords_1.recordDeletion)(userId, "shopping_item", uuid);
    return res.json((0, response_1.ok)({ deleted: true }));
}
