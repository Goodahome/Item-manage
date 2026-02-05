import { Request, Response } from "express";
import { z } from "zod";
import { prisma } from "../prisma";
import { fail, ok } from "../utils/response";
import { parsePagination } from "../utils/pagination";
import { recordDeletion } from "../utils/deletedRecords";

const iconLibraryItemSchema = z.object({
  uuid: z.string().min(1),
  name: z.string().min(1),
  iconKey: z.string().optional().nullable(),
  fileSize: z.number().int().positive().optional(),
  createdAt: z.string().datetime().optional(),
  updatedAt: z.string().datetime().optional()
});

export async function listIconLibraryItems(req: Request, res: Response) {
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

  const [icons, total] = await Promise.all([
    prisma.iconLibraryItem.findMany({
      where: { userId },
      orderBy: { updatedAt: "desc" },
      skip: (page - 1) * pageSize,
      take: pageSize
    }),
    prisma.iconLibraryItem.count({ where: { userId } })
  ]);

  const responseIcons = icons.map((icon) => ({
    ...icon,
    fileSize: Number(icon.fileSize)
  }));

  return res.json(
    ok({
      icons: responseIcons,
      page,
      pageSize,
      total
    })
  );
}

export async function getIconLibraryItem(req: Request, res: Response) {
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
  const icon = await prisma.iconLibraryItem.findFirst({
    where: { uuid, userId }
  });
  if (!icon) {
    return res.status(404).json(
      fail({
        code: "NOT_FOUND",
        message: "Icon not found"
      })
    );
  }

  const response = {
    ...icon,
    fileSize: Number(icon.fileSize)
  };

  return res.json(ok(response));
}

export async function upsertIconLibraryItem(req: Request, res: Response) {
  const userId = req.user?.uuid;
  if (!userId) {
    return res.status(401).json(
      fail({
        code: "UNAUTHORIZED",
        message: "Missing user context"
      })
    );
  }

  const parsed = iconLibraryItemSchema.safeParse(req.body);
  if (!parsed.success) {
    return res.status(400).json(
      fail({
        code: "INVALID_INPUT",
        message: "Invalid icon library item payload",
        details: parsed.error.flatten()
      })
    );
  }

  const data = parsed.data;
  const createdAt = data.createdAt ? new Date(data.createdAt) : undefined;
  const updatedAt = data.updatedAt ? new Date(data.updatedAt) : undefined;

  const icon = await prisma.iconLibraryItem.upsert({
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

  return res.json(ok(response));
}

export async function deleteIconLibraryItem(req: Request, res: Response) {
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
  const icon = await prisma.iconLibraryItem.findFirst({
    where: { uuid, userId }
  });

  if (icon) {
    await prisma.iconLibraryItem.delete({
      where: { uuid_userId: { uuid: icon.uuid, userId: icon.userId } }
    });
  }

  await recordDeletion(userId, "icon_library_item", uuid);

  return res.json(ok({ deleted: true }));
}
