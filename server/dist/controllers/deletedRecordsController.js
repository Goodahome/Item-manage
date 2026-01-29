"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.listDeletedRecords = listDeletedRecords;
exports.upsertDeletedRecord = upsertDeletedRecord;
const zod_1 = require("zod");
const prisma_1 = require("../prisma");
const response_1 = require("../utils/response");
const pagination_1 = require("../utils/pagination");
const deletedRecordSchema = zod_1.z.object({
    uuid: zod_1.z.string().uuid(),
    entityType: zod_1.z.string().min(1),
    entityUuid: zod_1.z.string().uuid(),
    deletedAt: zod_1.z.string().datetime().optional()
});
async function listDeletedRecords(req, res) {
    const { page, pageSize } = (0, pagination_1.parsePagination)(req.query);
    const userId = req.user?.uuid;
    if (!userId) {
        return res.status(401).json((0, response_1.fail)({
            code: "UNAUTHORIZED",
            message: "Missing user context"
        }));
    }
    const [deletedRecords, total] = await Promise.all([
        prisma_1.prisma.deletedRecord.findMany({
            where: { userId },
            orderBy: { deletedAt: "desc" },
            skip: (page - 1) * pageSize,
            take: pageSize
        }),
        prisma_1.prisma.deletedRecord.count({ where: { userId } })
    ]);
    return res.json((0, response_1.ok)({
        deletedRecords,
        page,
        pageSize,
        total
    }));
}
async function upsertDeletedRecord(req, res) {
    const userId = req.user?.uuid;
    if (!userId) {
        return res.status(401).json((0, response_1.fail)({
            code: "UNAUTHORIZED",
            message: "Missing user context"
        }));
    }
    const parsed = deletedRecordSchema.safeParse(req.body);
    if (!parsed.success) {
        return res.status(400).json((0, response_1.fail)({
            code: "INVALID_INPUT",
            message: "Invalid deleted record payload",
            details: parsed.error.flatten()
        }));
    }
    const data = parsed.data;
    const deletedAt = data.deletedAt ? new Date(data.deletedAt) : undefined;
    const record = await prisma_1.prisma.deletedRecord.upsert({
        where: {
            uuid_userId: {
                uuid: data.uuid,
                userId
            }
        },
        update: {
            entityType: data.entityType,
            entityUuid: data.entityUuid,
            deletedAt: deletedAt ?? undefined
        },
        create: {
            uuid: data.uuid,
            userId,
            entityType: data.entityType,
            entityUuid: data.entityUuid,
            deletedAt: deletedAt ?? undefined
        }
    });
    switch (record.entityType) {
        case "item":
            await prisma_1.prisma.item.deleteMany({ where: { uuid: record.entityUuid, userId } });
            break;
        case "category":
            await prisma_1.prisma.category.deleteMany({ where: { uuid: record.entityUuid, userId } });
            break;
        case "warehouse":
            await prisma_1.prisma.warehouse.deleteMany({ where: { uuid: record.entityUuid, userId } });
            break;
        case "shopping_item":
            await prisma_1.prisma.shoppingItem.deleteMany({ where: { uuid: record.entityUuid, userId } });
            break;
        case "reminder":
            await prisma_1.prisma.itemReminder.deleteMany({ where: { uuid: record.entityUuid, userId } });
            break;
        case "activity_event":
            await prisma_1.prisma.activityEvent.deleteMany({ where: { uuid: record.entityUuid, userId } });
            break;
        default:
            break;
    }
    return res.json((0, response_1.ok)(record));
}
