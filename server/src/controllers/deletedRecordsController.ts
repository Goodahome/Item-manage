import { Request, Response } from "express";
import { z } from "zod";
import { prisma } from "../prisma";
import { fail, ok } from "../utils/response";
import { parsePagination } from "../utils/pagination";

const deletedRecordSchema = z.object({
  uuid: z.string().uuid(),
  entityType: z.string().min(1),
  entityUuid: z.string().uuid(),
  deletedAt: z.string().datetime().optional()
});

export async function listDeletedRecords(req: Request, res: Response) {
  const { page, pageSize } = parsePagination(req.query);
  const userId = req.user?.uuid;
  if (!userId) {
    return res.status(401).json(
      fail({
        code: "UNAUTHORIZED",
        message: "Missing user context"
      })
    );
  }

  const [deletedRecords, total] = await Promise.all([
    prisma.deletedRecord.findMany({
      where: { userId },
      orderBy: { deletedAt: "desc" },
      skip: (page - 1) * pageSize,
      take: pageSize
    }),
    prisma.deletedRecord.count({ where: { userId } })
  ]);

  return res.json(
    ok({
      deletedRecords,
      page,
      pageSize,
      total
    })
  );
}

export async function upsertDeletedRecord(req: Request, res: Response) {
  const userId = req.user?.uuid;
  if (!userId) {
    return res.status(401).json(
      fail({
        code: "UNAUTHORIZED",
        message: "Missing user context"
      })
    );
  }

  const parsed = deletedRecordSchema.safeParse(req.body);
  if (!parsed.success) {
    return res.status(400).json(
      fail({
        code: "INVALID_INPUT",
        message: "Invalid deleted record payload",
        details: parsed.error.flatten()
      })
    );
  }

  const data = parsed.data;
  const deletedAt = data.deletedAt ? new Date(data.deletedAt) : undefined;

  const record = await prisma.deletedRecord.upsert({
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
      await prisma.item.deleteMany({ where: { uuid: record.entityUuid, userId } });
      break;
    case "category":
      await prisma.category.deleteMany({ where: { uuid: record.entityUuid, userId } });
      break;
    case "warehouse":
      await prisma.warehouse.deleteMany({ where: { uuid: record.entityUuid, userId } });
      break;
    case "shopping_item":
      await prisma.shoppingItem.deleteMany({ where: { uuid: record.entityUuid, userId } });
      break;
    case "reminder":
      await prisma.itemReminder.deleteMany({ where: { uuid: record.entityUuid, userId } });
      break;
    case "activity_event":
      await prisma.activityEvent.deleteMany({ where: { uuid: record.entityUuid, userId } });
      break;
    default:
      break;
  }

  return res.json(ok(record));
}
