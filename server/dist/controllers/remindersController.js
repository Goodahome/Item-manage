"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.listReminders = listReminders;
exports.getReminder = getReminder;
exports.upsertReminder = upsertReminder;
exports.deleteReminder = deleteReminder;
const zod_1 = require("zod");
const prisma_1 = require("../prisma");
const response_1 = require("../utils/response");
const pagination_1 = require("../utils/pagination");
const reminderSchema = zod_1.z.object({
    uuid: zod_1.z.string().uuid(),
    itemUuid: zod_1.z.string().uuid(),
    reminderType: zod_1.z.enum(["ONCE", "DAILY", "MONTHLY", "YEARLY"]),
    reminderTime: zod_1.z.string().datetime().nullable().optional(),
    dailyTime: zod_1.z.string().nullable().optional(),
    monthlyDay: zod_1.z.number().int().nullable().optional(),
    monthlyTime: zod_1.z.string().nullable().optional(),
    yearlyMonth: zod_1.z.number().int().nullable().optional(),
    yearlyDay: zod_1.z.number().int().nullable().optional(),
    yearlyTime: zod_1.z.string().nullable().optional(),
    reason: zod_1.z.string().optional(),
    isEnabled: zod_1.z.boolean().optional(),
    createdAt: zod_1.z.string().datetime().optional(),
    updatedAt: zod_1.z.string().datetime().optional()
});
async function listReminders(req, res) {
    const { page, pageSize } = (0, pagination_1.parsePagination)(req.query);
    const userId = req.user?.uuid;
    if (!userId) {
        return res.status(401).json((0, response_1.fail)({
            code: "UNAUTHORIZED",
            message: "Missing user context"
        }));
    }
    const [reminders, total] = await Promise.all([
        prisma_1.prisma.itemReminder.findMany({
            where: { userId },
            orderBy: { updatedAt: "desc" },
            skip: (page - 1) * pageSize,
            take: pageSize
        }),
        prisma_1.prisma.itemReminder.count({ where: { userId } })
    ]);
    return res.json((0, response_1.ok)({
        reminders,
        page,
        pageSize,
        total
    }));
}
async function getReminder(req, res) {
    const userId = req.user?.uuid;
    if (!userId) {
        return res.status(401).json((0, response_1.fail)({
            code: "UNAUTHORIZED",
            message: "Missing user context"
        }));
    }
    const uuid = String(req.params.uuid);
    const reminder = await prisma_1.prisma.itemReminder.findFirst({
        where: { uuid, userId }
    });
    if (!reminder) {
        return res.status(404).json((0, response_1.fail)({
            code: "NOT_FOUND",
            message: "Reminder not found"
        }));
    }
    return res.json((0, response_1.ok)(reminder));
}
async function upsertReminder(req, res) {
    const userId = req.user?.uuid;
    if (!userId) {
        return res.status(401).json((0, response_1.fail)({
            code: "UNAUTHORIZED",
            message: "Missing user context"
        }));
    }
    const parsed = reminderSchema.safeParse(req.body);
    if (!parsed.success) {
        return res.status(400).json((0, response_1.fail)({
            code: "INVALID_INPUT",
            message: "Invalid reminder payload",
            details: parsed.error.flatten()
        }));
    }
    const data = parsed.data;
    const createdAt = data.createdAt ? new Date(data.createdAt) : undefined;
    const updatedAt = data.updatedAt ? new Date(data.updatedAt) : undefined;
    const reminder = await prisma_1.prisma.itemReminder.upsert({
        where: {
            uuid_userId: {
                uuid: data.uuid,
                userId
            }
        },
        update: {
            itemUuid: data.itemUuid,
            reminderType: data.reminderType,
            reminderTime: data.reminderTime ? new Date(data.reminderTime) : null,
            dailyTime: data.dailyTime ?? null,
            monthlyDay: data.monthlyDay ?? null,
            monthlyTime: data.monthlyTime ?? null,
            yearlyMonth: data.yearlyMonth ?? null,
            yearlyDay: data.yearlyDay ?? null,
            yearlyTime: data.yearlyTime ?? null,
            reason: data.reason ?? "",
            isEnabled: data.isEnabled ?? true,
            createdAt: createdAt ?? undefined,
            updatedAt: updatedAt ?? undefined
        },
        create: {
            uuid: data.uuid,
            userId,
            itemUuid: data.itemUuid,
            reminderType: data.reminderType,
            reminderTime: data.reminderTime ? new Date(data.reminderTime) : null,
            dailyTime: data.dailyTime ?? null,
            monthlyDay: data.monthlyDay ?? null,
            monthlyTime: data.monthlyTime ?? null,
            yearlyMonth: data.yearlyMonth ?? null,
            yearlyDay: data.yearlyDay ?? null,
            yearlyTime: data.yearlyTime ?? null,
            reason: data.reason ?? "",
            isEnabled: data.isEnabled ?? true,
            createdAt: createdAt ?? undefined,
            updatedAt: updatedAt ?? undefined
        }
    });
    return res.json((0, response_1.ok)(reminder));
}
async function deleteReminder(req, res) {
    const userId = req.user?.uuid;
    if (!userId) {
        return res.status(401).json((0, response_1.fail)({
            code: "UNAUTHORIZED",
            message: "Missing user context"
        }));
    }
    const uuid = String(req.params.uuid);
    const reminder = await prisma_1.prisma.itemReminder.findFirst({
        where: { uuid, userId }
    });
    if (!reminder) {
        return res.status(404).json((0, response_1.fail)({
            code: "NOT_FOUND",
            message: "Reminder not found"
        }));
    }
    await prisma_1.prisma.itemReminder.delete({
        where: { uuid_userId: { uuid: reminder.uuid, userId: reminder.userId } }
    });
    return res.json((0, response_1.ok)({ deleted: true }));
}
