"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.listActivityEvents = listActivityEvents;
exports.getActivityEvent = getActivityEvent;
exports.upsertActivityEvent = upsertActivityEvent;
const zod_1 = require("zod");
const prisma_1 = require("../prisma");
const response_1 = require("../utils/response");
const pagination_1 = require("../utils/pagination");
const eventSchema = zod_1.z.object({
    uuid: zod_1.z.string().uuid(),
    type: zod_1.z.enum([
        "ITEM_ADDED",
        "ITEM_DELETED",
        "ITEM_UPDATED",
        "ITEM_USED",
        "ITEM_VIEWED",
        "WAREHOUSE_ADDED",
        "WAREHOUSE_DELETED",
        "WAREHOUSE_UPDATED",
        "REMINDER_TRIGGERED",
        "ITEM_EXPIRING",
        "ITEM_LOW_STOCK"
    ]),
    title: zod_1.z.string().min(1),
    description: zod_1.z.string().optional(),
    targetUuid: zod_1.z.string().uuid().nullable().optional(),
    targetName: zod_1.z.string().optional(),
    iconType: zod_1.z.string().optional(),
    createdAt: zod_1.z.string().datetime().optional(),
    metadata: zod_1.z.string().optional()
});
async function listActivityEvents(req, res) {
    const { page, pageSize } = (0, pagination_1.parsePagination)(req.query);
    const userId = req.user?.uuid;
    if (!userId) {
        return res.status(401).json((0, response_1.fail)({
            code: "UNAUTHORIZED",
            message: "Missing user context"
        }));
    }
    const [events, total] = await Promise.all([
        prisma_1.prisma.activityEvent.findMany({
            where: { userId },
            orderBy: { createdAt: "desc" },
            skip: (page - 1) * pageSize,
            take: pageSize
        }),
        prisma_1.prisma.activityEvent.count({ where: { userId } })
    ]);
    return res.json((0, response_1.ok)({
        events,
        page,
        pageSize,
        total
    }));
}
async function getActivityEvent(req, res) {
    const userId = req.user?.uuid;
    if (!userId) {
        return res.status(401).json((0, response_1.fail)({
            code: "UNAUTHORIZED",
            message: "Missing user context"
        }));
    }
    const uuid = String(req.params.uuid);
    const event = await prisma_1.prisma.activityEvent.findFirst({
        where: { uuid, userId }
    });
    if (!event) {
        return res.status(404).json((0, response_1.fail)({
            code: "NOT_FOUND",
            message: "Activity event not found"
        }));
    }
    return res.json((0, response_1.ok)(event));
}
async function upsertActivityEvent(req, res) {
    const userId = req.user?.uuid;
    if (!userId) {
        return res.status(401).json((0, response_1.fail)({
            code: "UNAUTHORIZED",
            message: "Missing user context"
        }));
    }
    const parsed = eventSchema.safeParse(req.body);
    if (!parsed.success) {
        return res.status(400).json((0, response_1.fail)({
            code: "INVALID_INPUT",
            message: "Invalid activity event payload",
            details: parsed.error.flatten()
        }));
    }
    const data = parsed.data;
    const createdAt = data.createdAt ? new Date(data.createdAt) : undefined;
    const event = await prisma_1.prisma.activityEvent.upsert({
        where: {
            uuid_userId: {
                uuid: data.uuid,
                userId
            }
        },
        update: {
            type: data.type,
            title: data.title,
            description: data.description ?? "",
            targetUuid: data.targetUuid ?? null,
            targetName: data.targetName ?? "",
            iconType: data.iconType ?? "",
            createdAt: createdAt ?? undefined,
            metadata: data.metadata ?? ""
        },
        create: {
            uuid: data.uuid,
            userId,
            type: data.type,
            title: data.title,
            description: data.description ?? "",
            targetUuid: data.targetUuid ?? null,
            targetName: data.targetName ?? "",
            iconType: data.iconType ?? "",
            createdAt: createdAt ?? undefined,
            metadata: data.metadata ?? ""
        }
    });
    return res.json((0, response_1.ok)(event));
}
