import { Request, Response } from "express";
import { z } from "zod";
import { prisma } from "../prisma";
import { fail, ok } from "../utils/response";
import { parsePagination } from "../utils/pagination";

const categorySchema = z.object({
  uuid: z.string().min(1),
  name: z.string().min(1),
  description: z.string().optional(),
  color: z.string().optional(),
  icon: z.string().optional(),
  createdAt: z.string().datetime().optional(),
  updatedAt: z.string().datetime().optional()
});

export async function listCategories(req: Request, res: Response) {
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

  const [categories, total] = await Promise.all([
    prisma.category.findMany({
      where: { userId },
      orderBy: { updatedAt: "desc" },
      skip: (page - 1) * pageSize,
      take: pageSize
    }),
    prisma.category.count({ where: { userId } })
  ]);

  return res.json(
    ok({
      categories,
      page,
      pageSize,
      total
    })
  );
}

export async function getCategory(req: Request, res: Response) {
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
  const category = await prisma.category.findFirst({
    where: { uuid, userId }
  });
  if (!category) {
    return res.status(404).json(
      fail({
        code: "NOT_FOUND",
        message: "Category not found"
      })
    );
  }

  return res.json(ok(category));
}

export async function upsertCategory(req: Request, res: Response) {
  const userId = req.user?.uuid;
  if (!userId) {
    return res.status(401).json(
      fail({
        code: "UNAUTHORIZED",
        message: "Missing user context"
      })
    );
  }

  const parsed = categorySchema.safeParse(req.body);
  if (!parsed.success) {
    return res.status(400).json(
      fail({
        code: "INVALID_INPUT",
        message: "Invalid category payload",
        details: parsed.error.flatten()
      })
    );
  }

  const data = parsed.data;
  const createdAt = data.createdAt ? new Date(data.createdAt) : undefined;
  const updatedAt = data.updatedAt ? new Date(data.updatedAt) : undefined;

  const category = await prisma.category.upsert({
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

  return res.json(ok(category));
}

export async function deleteCategory(req: Request, res: Response) {
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
  const category = await prisma.category.findFirst({
    where: { uuid, userId }
  });
  if (!category) {
    return res.status(404).json(
      fail({
        code: "NOT_FOUND",
        message: "Category not found"
      })
    );
  }

  await prisma.category.delete({
    where: { uuid_userId: { uuid: category.uuid, userId: category.userId } }
  });
  return res.json(ok({ deleted: true }));
}
