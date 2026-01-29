import { Request, Response } from "express";
import { z } from "zod";
import { prisma } from "../prisma";
import { fail, ok } from "../utils/response";
import { parsePagination } from "../utils/pagination";
import { recordDeletion } from "../utils/deletedRecords";

const warehouseSchema = z.object({
  uuid: z.string().min(1),
  name: z.string().min(1),
  description: z.string().optional(),
  location: z.string().optional(),
  capacity: z.number().int().nullable().optional(),
  parentUuid: z.string().uuid().nullable().optional(),
  level: z.number().int().optional(),
  imageUri: z.string().nullable().optional(),
  createdAt: z.string().datetime().optional(),
  updatedAt: z.string().datetime().optional()
});

export async function listWarehouses(req: Request, res: Response) {
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

  const [warehouses, total] = await Promise.all([
    prisma.warehouse.findMany({
      where: { userId },
      orderBy: { updatedAt: "desc" },
      skip: (page - 1) * pageSize,
      take: pageSize
    }),
    prisma.warehouse.count({ where: { userId } })
  ]);

  return res.json(
    ok({
      warehouses,
      page,
      pageSize,
      total
    })
  );
}

export async function getWarehouse(req: Request, res: Response) {
  const userId = req.user?.uuid;
  if (!userId) {
    return res.status(401).json(
      fail({
        code: "UNAUTHORIZED",
        message: "Missing user context"
      })
    );
  }

  const uuid = String(req.params.uuid);
  const warehouse = await prisma.warehouse.findFirst({
    where: { uuid, userId }
  });
  if (!warehouse) {
    return res.status(404).json(
      fail({
        code: "NOT_FOUND",
        message: "Warehouse not found"
      })
    );
  }

  return res.json(ok(warehouse));
}

export async function upsertWarehouse(req: Request, res: Response) {
  const userId = req.user?.uuid;
  if (!userId) {
    return res.status(401).json(
      fail({
        code: "UNAUTHORIZED",
        message: "Missing user context"
      })
    );
  }

  const parsed = warehouseSchema.safeParse(req.body);
  if (!parsed.success) {
    return res.status(400).json(
      fail({
        code: "INVALID_INPUT",
        message: "Invalid warehouse payload",
        details: parsed.error.flatten()
      })
    );
  }

  const data = parsed.data;
  const createdAt = data.createdAt ? new Date(data.createdAt) : undefined;
  const updatedAt = data.updatedAt ? new Date(data.updatedAt) : undefined;

  const warehouse = await prisma.warehouse.upsert({
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
      parentUuid: data.parentUuid ?? null,
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
      parentUuid: data.parentUuid ?? null,
      level: data.level ?? 1,
      imageUri: data.imageUri ?? null,
      createdAt: createdAt ?? undefined,
      updatedAt: updatedAt ?? undefined
    }
  });

  return res.json(ok(warehouse));
}

export async function deleteWarehouse(req: Request, res: Response) {
  const userId = req.user?.uuid;
  if (!userId) {
    return res.status(401).json(
      fail({
        code: "UNAUTHORIZED",
        message: "Missing user context"
      })
    );
  }

  const uuid = String(req.params.uuid);
  const warehouse = await prisma.warehouse.findFirst({
    where: { uuid, userId }
  });

  if (warehouse) {
    await prisma.warehouse.delete({
      where: { uuid_userId: { uuid: warehouse.uuid, userId: warehouse.userId } }
    });
  }

  await recordDeletion(userId, "warehouse", uuid);

  return res.json(ok({ deleted: true }));
}
