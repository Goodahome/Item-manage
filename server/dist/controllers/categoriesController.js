"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.listCategories = listCategories;
exports.getCategory = getCategory;
exports.upsertCategory = upsertCategory;
exports.deleteCategory = deleteCategory;
const zod_1 = require("zod");
const prisma_1 = require("../prisma");
const response_1 = require("../utils/response");
const pagination_1 = require("../utils/pagination");
const deletedRecords_1 = require("../utils/deletedRecords");
const categorySchema = zod_1.z.object({
    uuid: zod_1.z.string().min(1),
    name: zod_1.z.string().min(1),
    description: zod_1.z.string().optional(),
    color: zod_1.z.string().optional(),
    icon: zod_1.z.string().optional(),
    createdAt: zod_1.z.string().datetime().optional(),
    updatedAt: zod_1.z.string().datetime().optional()
});
async function listCategories(req, res) {
    const { page, pageSize } = (0, pagination_1.parsePagination)(req.query);
    const userId = req.user?.uuid;
    if (!userId) {
        return res.status(401).json((0, response_1.fail)({
            code: "UNAUTHORIZED",
            message: "Missing user context"
        }));
    }
    const [categories, total] = await Promise.all([
        prisma_1.prisma.category.findMany({
            where: { userId },
            orderBy: { updatedAt: "desc" },
            skip: (page - 1) * pageSize,
            take: pageSize
        }),
        prisma_1.prisma.category.count({ where: { userId } })
    ]);
    return res.json((0, response_1.ok)({
        categories,
        page,
        pageSize,
        total
    }));
}
async function getCategory(req, res) {
    const userId = req.user?.uuid;
    if (!userId) {
        return res.status(401).json((0, response_1.fail)({
            code: "UNAUTHORIZED",
            message: "Missing user context"
        }));
    }
    const uuid = String(req.params.uuid);
    const category = await prisma_1.prisma.category.findFirst({
        where: { uuid, userId }
    });
    if (!category) {
        return res.status(404).json((0, response_1.fail)({
            code: "NOT_FOUND",
            message: "Category not found"
        }));
    }
    return res.json((0, response_1.ok)(category));
}
async function upsertCategory(req, res) {
    const userId = req.user?.uuid;
    if (!userId) {
        return res.status(401).json((0, response_1.fail)({
            code: "UNAUTHORIZED",
            message: "Missing user context"
        }));
    }
    const parsed = categorySchema.safeParse(req.body);
    if (!parsed.success) {
        return res.status(400).json((0, response_1.fail)({
            code: "INVALID_INPUT",
            message: "Invalid category payload",
            details: parsed.error.flatten()
        }));
    }
    const data = parsed.data;
    const createdAt = data.createdAt ? new Date(data.createdAt) : undefined;
    const updatedAt = data.updatedAt ? new Date(data.updatedAt) : undefined;
    const category = await prisma_1.prisma.category.upsert({
        where: {
            uuid_userId: {
                uuid: data.uuid,
                userId
            }
        },
        update: {
            name: data.name,
            description: data.description ?? "",
            color: data.color ?? "#6200EE",
            icon: data.icon ?? "category",
            createdAt: createdAt ?? undefined,
            updatedAt: updatedAt ?? undefined
        },
        create: {
            uuid: data.uuid,
            userId,
            name: data.name,
            description: data.description ?? "",
            color: data.color ?? "#6200EE",
            icon: data.icon ?? "category",
            createdAt: createdAt ?? undefined,
            updatedAt: updatedAt ?? undefined
        }
    });
    return res.json((0, response_1.ok)(category));
}
async function deleteCategory(req, res) {
    const userId = req.user?.uuid;
    if (!userId) {
        return res.status(401).json((0, response_1.fail)({
            code: "UNAUTHORIZED",
            message: "Missing user context"
        }));
    }
    const uuid = String(req.params.uuid);
    const category = await prisma_1.prisma.category.findFirst({
        where: { uuid, userId }
    });
    if (category) {
        await prisma_1.prisma.category.delete({
            where: { uuid_userId: { uuid: category.uuid, userId: category.userId } }
        });
    }
    await (0, deletedRecords_1.recordDeletion)(userId, "category", uuid);
    return res.json((0, response_1.ok)({ deleted: true }));
}
