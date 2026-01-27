"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.listWarehouses = listWarehouses;
exports.getWarehouse = getWarehouse;
exports.upsertWarehouse = upsertWarehouse;
exports.deleteWarehouse = deleteWarehouse;
const zod_1 = require("zod");
const prisma_1 = require("../prisma");
const response_1 = require("../utils/response");
const pagination_1 = require("../utils/pagination");
const warehouseSchema = zod_1.z.object({
    uuid: zod_1.z.string().min(1),
    name: zod_1.z.string().min(1),
    description: zod_1.z.string().optional(),
    location: zod_1.z.string().optional(),
    capacity: zod_1.z.number().int().nullable().optional(),
    parentId: zod_1.z.number().int().nullable().optional(),
    level: zod_1.z.number().int().optional(),
    imageUri: zod_1.z.string().nullable().optional(),
    createdAt: zod_1.z.string().datetime().optional(),
    updatedAt: zod_1.z.string().datetime().optional()
});
async function listWarehouses(req, res) {
    const { page, pageSize } = (0, pagination_1.parsePagination)(req.query);
    const userId = req.user?.id;
    if (!userId) {
        return res.status(401).json((0, response_1.fail)({
            code: "UNAUTHORIZED",
            message: "Missing user context"
        }));
    }
    const [warehouses, total] = await Promise.all([
        prisma_1.prisma.warehouse.findMany({
            where: { userId },
            orderBy: { updatedAt: "desc" },
            skip: (page - 1) * pageSize,
            take: pageSize
        }),
        prisma_1.prisma.warehouse.count({ where: { userId } })
    ]);
    return res.json((0, response_1.ok)({
        warehouses,
        page,
        pageSize,
        total
    }));
}
async function getWarehouse(req, res) {
    const userId = req.user?.id;
    if (!userId) {
        return res.status(401).json((0, response_1.fail)({
            code: "UNAUTHORIZED",
            message: "Missing user context"
        }));
    }
    const uuid = String(req.params.uuid);
    const warehouse = await prisma_1.prisma.warehouse.findFirst({
        where: { uuid, userId }
    });
    if (!warehouse) {
        return res.status(404).json((0, response_1.fail)({
            code: "NOT_FOUND",
            message: "Warehouse not found"
        }));
    }
    return res.json((0, response_1.ok)(warehouse));
}
async function upsertWarehouse(req, res) {
    const userId = req.user?.id;
    if (!userId) {
        return res.status(401).json((0, response_1.fail)({
            code: "UNAUTHORIZED",
            message: "Missing user context"
        }));
    }
    const parsed = warehouseSchema.safeParse(req.body);
    if (!parsed.success) {
        return res.status(400).json((0, response_1.fail)({
            code: "INVALID_INPUT",
            message: "Invalid warehouse payload",
            details: parsed.error.flatten()
        }));
    }
    const data = parsed.data;
    const createdAt = data.createdAt ? new Date(data.createdAt) : undefined;
    const updatedAt = data.updatedAt ? new Date(data.updatedAt) : undefined;
    const warehouse = await prisma_1.prisma.warehouse.upsert({
        where: {
            uuid_userId: {
                uuid: data.uuid,
                userId
            }
        },
        update: {
            name: data.name,
            description: data.description ?? "",
            location: data.location ?? "",
            capacity: data.capacity ?? null,
            parentId: data.parentId ?? null,
            level: data.level ?? 1,
            imageUri: data.imageUri ?? null,
            createdAt: createdAt ?? undefined,
            updatedAt: updatedAt ?? undefined
        },
        create: {
            uuid: data.uuid,
            userId,
            name: data.name,
            description: data.description ?? "",
            location: data.location ?? "",
            capacity: data.capacity ?? null,
            parentId: data.parentId ?? null,
            level: data.level ?? 1,
            imageUri: data.imageUri ?? null,
            createdAt: createdAt ?? undefined,
            updatedAt: updatedAt ?? undefined
        }
    });
    return res.json((0, response_1.ok)(warehouse));
}
async function deleteWarehouse(req, res) {
    const userId = req.user?.id;
    if (!userId) {
        return res.status(401).json((0, response_1.fail)({
            code: "UNAUTHORIZED",
            message: "Missing user context"
        }));
    }
    const uuid = String(req.params.uuid);
    const warehouse = await prisma_1.prisma.warehouse.findFirst({
        where: { uuid, userId }
    });
    if (!warehouse) {
        return res.status(404).json((0, response_1.fail)({
            code: "NOT_FOUND",
            message: "Warehouse not found"
        }));
    }
    await prisma_1.prisma.warehouse.delete({ where: { id: warehouse.id } });
    return res.json((0, response_1.ok)({ deleted: true }));
}
