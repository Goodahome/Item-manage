import { Request, Response } from "express";
import { z } from "zod";
import { prisma } from "../prisma";
import { fail, ok } from "../utils/response";
import { parsePagination } from "../utils/pagination";

const reminderSchema = z.object({
  uuid: z.string().uuid(),
  itemUuid: z.string().uuid(),
  reminderType: z.enum(["ONCE", "DAILY", "MONTHLY", "YEARLY"]),
  reminderTime: z.string().datetime().nullable().optional(),
  dailyTime: z.string().nullable().optional(),
  monthlyDay: z.number().int().nullable().optional(),
  monthlyTime: z.string().nullable().optional(),
  yearlyMonth: z.number().int().nullable().optional(),
  yearlyDay: z.number().int().nullable().optional(),
  yearlyTime: z.string().nullable().optional(),
  reason: z.string().optional(),
  isEnabled: z.boolean().optional(),
  createdAt: z.string().datetime().optional(),
  updatedAt: z.string().datetime().optional()
});

export async function listReminders(req: Request, res: Response) {
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

  const [reminders, total] = await Promise.all([
    prisma.itemReminder.findMany({
      where: { userId },
      orderBy: { updatedAt: "desc" },
      skip: (page - 1) * pageSize,
      take: pageSize
    }),
    prisma.itemReminder.count({ where: { userId } })
  ]);

  return res.json(
    ok({
      reminders,
      page,
      pageSize,
      total
    })
  );
}

export async function getReminder(req: Request, res: Response) {
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
  const reminder = await prisma.itemReminder.findFirst({
    where: { uuid, userId }
  });
  if (!reminder) {
    return res.status(404).json(
      fail({
        code: "NOT_FOUND",
        message: "Reminder not found"
      })
    );
  }

  return res.json(ok(reminder));
}

export async function upsertReminder(req: Request, res: Response) {
  const userId = req.user?.id;
  if (!userId) {
    return res.status(401).json(
      fail({
        code: "UNAUTHORIZED",
        message: "Missing user context"
      })
    );
  }

  const parsed = reminderSchema.safeParse(req.body);
  if (!parsed.success) {
    return res.status(400).json(
      fail({
        code: "INVALID_INPUT",
        message: "Invalid reminder payload",
        details: parsed.error.flatten()
      })
    );
  }

  const data = parsed.data;
  const createdAt = data.createdAt ? new Date(data.createdAt) : undefined;
  const updatedAt = data.updatedAt ? new Date(data.updatedAt) : undefined;

  const reminder = await prisma.itemReminder.upsert({
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

  return res.json(ok(reminder));
}

export async function deleteReminder(req: Request, res: Response) {
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
  const reminder = await prisma.itemReminder.findFirst({
    where: { uuid, userId }
  });
  if (!reminder) {
    return res.status(404).json(
      fail({
        code: "NOT_FOUND",
        message: "Reminder not found"
      })
    );
  }

  await prisma.itemReminder.delete({ where: { id: reminder.id } });
  return res.json(ok({ deleted: true }));
}
