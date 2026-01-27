import { Request, Response } from "express";
import { z } from "zod";
import { prisma } from "../prisma";
import { fail, ok } from "../utils/response";
import { parsePagination } from "../utils/pagination";

const eventSchema = z.object({
  uuid: z.string().uuid(),
  type: z.enum([
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
  title: z.string().min(1),
  description: z.string().optional(),
  targetUuid: z.string().uuid().nullable().optional(),
  targetName: z.string().optional(),
  iconType: z.string().optional(),
  createdAt: z.string().datetime().optional(),
  metadata: z.string().optional()
});

export async function listActivityEvents(req: Request, res: Response) {
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

  const [events, total] = await Promise.all([
    prisma.activityEvent.findMany({
      where: { userId },
      orderBy: { createdAt: "desc" },
      skip: (page - 1) * pageSize,
      take: pageSize
    }),
    prisma.activityEvent.count({ where: { userId } })
  ]);

  return res.json(
    ok({
      events,
      page,
      pageSize,
      total
    })
  );
}

export async function getActivityEvent(req: Request, res: Response) {
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
  const event = await prisma.activityEvent.findFirst({
    where: { uuid, userId }
  });
  if (!event) {
    return res.status(404).json(
      fail({
        code: "NOT_FOUND",
        message: "Activity event not found"
      })
    );
  }

  return res.json(ok(event));
}

export async function upsertActivityEvent(req: Request, res: Response) {
  const userId = req.user?.id;
  if (!userId) {
    return res.status(401).json(
      fail({
        code: "UNAUTHORIZED",
        message: "Missing user context"
      })
    );
  }

  const parsed = eventSchema.safeParse(req.body);
  if (!parsed.success) {
    return res.status(400).json(
      fail({
        code: "INVALID_INPUT",
        message: "Invalid activity event payload",
        details: parsed.error.flatten()
      })
    );
  }

  const data = parsed.data;
  const createdAt = data.createdAt ? new Date(data.createdAt) : undefined;

  const event = await prisma.activityEvent.upsert({
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

  return res.json(ok(event));
}
