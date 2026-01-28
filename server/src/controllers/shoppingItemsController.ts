import { Request, Response } from "express";
import { z } from "zod";
import { prisma } from "../prisma";
import { fail, ok } from "../utils/response";
import { parsePagination } from "../utils/pagination";

const shoppingItemSchema = z.object({
  uuid: z.string().min(1),
  name: z.string().min(1),
  description: z.string().optional(),
  quantity: z.number().int().optional(),
  isCompleted: z.boolean().optional(),
  priority: z.string().optional(),
  createdAt: z.string().datetime().optional(),
  completedAt: z.string().datetime().nullable().optional(),
  imageUri: z.string().nullable().optional(),
  itemUuid: z.string().min(1).nullable().optional()
});

export async function listShoppingItems(req: Request, res: Response) {
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

  const [shoppingItems, total] = await Promise.all([
    prisma.shoppingItem.findMany({
      where: { userId },
      orderBy: { createdAt: "desc" },
      skip: (page - 1) * pageSize,
      take: pageSize
    }),
    prisma.shoppingItem.count({ where: { userId } })
  ]);

  return res.json(
    ok({
      shoppingItems,
      page,
      pageSize,
      total
    })
  );
}

export async function getShoppingItem(req: Request, res: Response) {
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
  const shoppingItem = await prisma.shoppingItem.findFirst({
    where: { uuid, userId }
  });
  if (!shoppingItem) {
    return res.status(404).json(
      fail({
        code: "NOT_FOUND",
        message: "Shopping item not found"
      })
    );
  }

  return res.json(ok(shoppingItem));
}

export async function upsertShoppingItem(req: Request, res: Response) {
  const userId = req.user?.uuid;
  if (!userId) {
    return res.status(401).json(
      fail({
        code: "UNAUTHORIZED",
        message: "Missing user context"
      })
    );
  }

  const parsed = shoppingItemSchema.safeParse(req.body);
  if (!parsed.success) {
    return res.status(400).json(
      fail({
        code: "INVALID_INPUT",
        message: "Invalid shopping item payload",
        details: parsed.error.flatten()
      })
    );
  }

  const data = parsed.data;
  const createdAt = data.createdAt ? new Date(data.createdAt) : undefined;
  const completedAt = data.completedAt ? new Date(data.completedAt) : null;

  const shoppingItem = await prisma.shoppingItem.upsert({
    where: {
      uuid_userId: {
        uuid: data.uuid,
        userId
      }
    },
    update: {
      name: data.name,
      description: data.description ?? "",
      quantity: data.quantity ?? 1,
      isCompleted: data.isCompleted ?? false,
      priority: data.priority ?? "MEDIUM",
      completedAt,
      imageUri: data.imageUri ?? null,
      itemUuid: data.itemUuid ?? null,
      createdAt: createdAt ?? undefined
    },
    create: {
      uuid: data.uuid,
      userId,
      name: data.name,
      description: data.description ?? "",
      quantity: data.quantity ?? 1,
      isCompleted: data.isCompleted ?? false,
      priority: data.priority ?? "MEDIUM",
      completedAt,
      imageUri: data.imageUri ?? null,
      itemUuid: data.itemUuid ?? null,
      createdAt: createdAt ?? undefined
    }
  });

  return res.json(ok(shoppingItem));
}

export async function deleteShoppingItem(req: Request, res: Response) {
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
  const shoppingItem = await prisma.shoppingItem.findFirst({
    where: { uuid, userId }
  });
  if (!shoppingItem) {
    return res.status(404).json(
      fail({
        code: "NOT_FOUND",
        message: "Shopping item not found"
      })
    );
  }

  await prisma.shoppingItem.delete({
    where: { uuid_userId: { uuid: shoppingItem.uuid, userId: shoppingItem.userId } }
  });
  return res.json(ok({ deleted: true }));
}
