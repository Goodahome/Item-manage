"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.listIconLibraryItems = listIconLibraryItems;
exports.getIconLibraryItem = getIconLibraryItem;
exports.upsertIconLibraryItem = upsertIconLibraryItem;
exports.deleteIconLibraryItem = deleteIconLibraryItem;
const zod_1 = require("zod");
const prisma_1 = require("../prisma");
const response_1 = require("../utils/response");
const pagination_1 = require("../utils/pagination");
const deletedRecords_1 = require("../utils/deletedRecords");
const iconLibraryItemSchema = zod_1.z.object({
    uuid: zod_1.z.string().min(1),
    name: zod_1.z.string().min(1),
    iconKey: zod_1.z.string().optional().nullable(),
    fileSize: zod_1.z.number().int().positive().optional(),
    createdAt: zod_1.z.string().datetime().optional(),
    updatedAt: zod_1.z.string().datetime().optional()
});
async function listIconLibraryItems(req, res) {
    const { page, pageSize } = (0, pagination_1.parsePagination)(req.query);
    const userId = req.user?.uuid;
    if (!userId) {
        return res.status(401).json((0, response_1.fail)({
            code: "UNAUTHORIZED",
            message: "Missing user context"
        }));
    }
    const [icons, total] = await Promise.all([
        prisma_1.prisma.iconLibraryItem.findMany({
            where: { userId },
            orderBy: { updatedAt: "desc" },
            skip: (page - 1) * pageSize,
            take: pageSize
        }),
        prisma_1.prisma.iconLibraryItem.count({ where: { userId } })
    ]);
    return res.json((0, response_1.ok)({
        icons,
        page,
        pageSize,
        total
    }));
}
async function getIconLibraryItem(req, res) {
    const userId = req.user?.uuid;
    if (!userId) {
        return res.status(401).json((0, response_1.fail)({
            code: "UNAUTHORIZED",
            message: "Missing user context"
        }));
    }
    const uuid = String(req.params.uuid);
    const icon = await prisma_1.prisma.iconLibraryItem.findFirst({
        where: { uuid, userId }
    });
    if (!icon) {
        return res.status(404).json((0, response_1.fail)({
            code: "NOT_FOUND",
            message: "Icon not found"
        }));
    }
    return res.json((0, response_1.ok)(icon));
}
async function upsertIconLibraryItem(req, res) {
    const userId = req.user?.uuid;
    if (!userId) {
        return res.status(401).json((0, response_1.fail)({
            code: "UNAUTHORIZED",
            message: "Missing user context"
        }));
    }
    const parsed = iconLibraryItemSchema.safeParse(req.body);
    if (!parsed.success) {
        return res.status(400).json((0, response_1.fail)({
            code: "INVALID_INPUT",
            message: "Invalid icon library item payload",
            details: parsed.error.flatten()
        }));
    }
    const data = parsed.data;
    const createdAt = data.createdAt ? new Date(data.createdAt) : undefined;
    const updatedAt = data.updatedAt ? new Date(data.updatedAt) : undefined;
    const icon = await prisma_1.prisma.iconLibraryItem.upsert({
        where: {
            uuid_userId: {
                uuid: data.uuid,
                userId
            }
        },
        update: {
            name: data.name,
            iconKey: data.iconKey ?? null,
            fileSize: data.fileSize ? BigInt(data.fileSize) : undefined,
            createdAt: createdAt ?? undefined,
            updatedAt: updatedAt ?? undefined
        },
        create: {
            uuid: data.uuid,
            userId,
            name: data.name,
            iconKey: data.iconKey ?? null,
            fileSize: data.fileSize ? BigInt(data.fileSize) : BigInt(0),
            createdAt: createdAt ?? undefined,
            updatedAt: updatedAt ?? undefined
        }
    });
    // Convert BigInt to number for JSON serialization
    const response = {
        ...icon,
        fileSize: Number(icon.fileSize)
    };
    return res.json((0, response_1.ok)(response));
}
async function deleteIconLibraryItem(req, res) {
    const userId = req.user?.uuid;
    if (!userId) {
        return res.status(401).json((0, response_1.fail)({
            code: "UNAUTHORIZED",
            message: "Missing user context"
        }));
    }
    const uuid = String(req.params.uuid);
    const icon = await prisma_1.prisma.iconLibraryItem.findFirst({
        where: { uuid, userId }
    });
    if (icon) {
        await prisma_1.prisma.iconLibraryItem.delete({
            where: { uuid_userId: { uuid: icon.uuid, userId: icon.userId } }
        });
    }
    await (0, deletedRecords_1.recordDeletion)(userId, "icon_library_item", uuid);
    return res.json((0, response_1.ok)({ deleted: true }));
}
