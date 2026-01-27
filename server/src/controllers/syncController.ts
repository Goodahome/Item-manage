import { Request, Response } from "express";
import { z } from "zod";
import { prisma } from "../prisma";
import { fail, ok } from "../utils/response";
import { getUserSettings, setUserSettings } from "../utils/settingsStore";

const snapshotEntrySchema = z.object({
  uuid: z.string().min(1),
  updatedAt: z.string().datetime().optional().nullable()
});

const settingsSnapshotSchema = z.object({
  data: z.record(z.string(), z.string()).optional().default({}),
  updatedAt: z.string().datetime().optional().nullable()
});

const bootstrapSchema = z.object({
  items: z.array(snapshotEntrySchema).optional().default([]),
  categories: z.array(snapshotEntrySchema).optional().default([]),
  warehouses: z.array(snapshotEntrySchema).optional().default([]),
  shoppingItems: z.array(snapshotEntrySchema).optional().default([]),
  activityEvents: z.array(snapshotEntrySchema).optional().default([]),
  settings: settingsSnapshotSchema.optional().nullable()
});

const itemDtoSchema = z.object({
  uuid: z.string().min(1),
  name: z.string().min(1),
  description: z.string().optional().nullable(),
  categoryId: z.number().int().optional().nullable(),
  categoryUuid: z.string().min(1).optional().nullable(),
  warehouseId: z.number().int().optional().nullable(),
  warehouseUuid: z.string().min(1).optional().nullable(),
  tags: z.array(z.string()).optional().nullable(),
  purchaseDate: z.string().datetime().optional().nullable(),
  expiryDate: z.string().datetime().optional().nullable(),
  price: z.number().optional().nullable(),
  quantity: z.number().int().optional().nullable(),
  barcode: z.string().optional().nullable(),
  imageUri: z.string().optional().nullable(),
  imageUris: z.array(z.string()).optional().nullable(),
  primaryImageIndex: z.number().int().optional().nullable(),
  featureCode: z.string().optional().nullable(),
  enableStockAlert: z.boolean().optional().nullable(),
  createdAt: z.string().datetime().optional().nullable(),
  updatedAt: z.string().datetime().optional().nullable()
});

const categoryDtoSchema = z.object({
  uuid: z.string().min(1),
  name: z.string().min(1),
  description: z.string().optional().nullable(),
  color: z.string().optional().nullable(),
  icon: z.string().optional().nullable(),
  createdAt: z.string().datetime().optional().nullable(),
  updatedAt: z.string().datetime().optional().nullable()
});

const warehouseDtoSchema = z.object({
  uuid: z.string().min(1),
  name: z.string().min(1),
  description: z.string().optional().nullable(),
  location: z.string().optional().nullable(),
  capacity: z.number().int().optional().nullable(),
  parentId: z.number().int().optional().nullable(),
  level: z.number().int().optional().nullable(),
  imageUri: z.string().optional().nullable(),
  createdAt: z.string().datetime().optional().nullable(),
  updatedAt: z.string().datetime().optional().nullable()
});

const shoppingItemDtoSchema = z.object({
  uuid: z.string().min(1),
  name: z.string().min(1),
  description: z.string().optional().nullable(),
  quantity: z.number().int().optional().nullable(),
  isCompleted: z.boolean().optional().nullable(),
  priority: z.string().optional().nullable(),
  createdAt: z.string().datetime().optional().nullable(),
  completedAt: z.string().datetime().optional().nullable(),
  imageUri: z.string().optional().nullable(),
  itemUuid: z.string().optional().nullable()
});

const activityEventDtoSchema = z.object({
  uuid: z.string().min(1),
  type: z.string().min(1),
  title: z.string().min(1),
  description: z.string().optional().nullable(),
  targetUuid: z.string().optional().nullable(),
  targetName: z.string().optional().nullable(),
  iconType: z.string().optional().nullable(),
  createdAt: z.string().datetime().optional().nullable(),
  metadata: z.string().optional().nullable()
});

const bootstrapAckSchema = z.object({
  items: z.array(itemDtoSchema).optional().default([]),
  categories: z.array(categoryDtoSchema).optional().default([]),
  warehouses: z.array(warehouseDtoSchema).optional().default([]),
  shoppingItems: z.array(shoppingItemDtoSchema).optional().default([]),
  activityEvents: z.array(activityEventDtoSchema).optional().default([]),
  settings: settingsSnapshotSchema.optional().nullable()
});

function parseDate(value?: string | null) {
  return value ? new Date(value) : undefined;
}

function compareUpdated(
  serverDate: Date | null,
  clientDateStr?: string | null
): "server_newer" | "client_newer" | "same" {
  if (!clientDateStr) return "server_newer";
  const clientDate = new Date(clientDateStr);
  if (!serverDate || Number.isNaN(serverDate.getTime()) || Number.isNaN(clientDate.getTime())) {
    return "server_newer";
  }
  if (serverDate.getTime() > clientDate.getTime()) return "server_newer";
  if (serverDate.getTime() < clientDate.getTime()) return "client_newer";
  return "same";
}

export async function bootstrapSync(req: Request, res: Response) {
  const userId = req.user?.id;
  if (!userId) {
    return res.status(401).json(
      fail({
        code: "UNAUTHORIZED",
        message: "Missing user context"
      })
    );
  }

  const parsed = bootstrapSchema.safeParse(req.body);
  if (!parsed.success) {
    return res.status(400).json(
      fail({
        code: "INVALID_INPUT",
        message: "Invalid bootstrap payload",
        details: parsed.error.flatten()
      })
    );
  }

  const client = parsed.data;
  const [items, categories, warehouses, shoppingItems, activityEvents] = await Promise.all([
    prisma.item.findMany({ where: { userId } }),
    prisma.category.findMany({ where: { userId } }),
    prisma.warehouse.findMany({ where: { userId } }),
    prisma.shoppingItem.findMany({ where: { userId } }),
    prisma.activityEvent.findMany({ where: { userId } })
  ]);

  const warehouseMap = new Map(warehouses.map((wh) => [wh.id, wh.uuid]));
  const categoryMap = new Map(categories.map((cat) => [cat.id, cat.uuid]));

  const toApply = {
    items: [] as unknown[],
    categories: [] as unknown[],
    warehouses: [] as unknown[],
    shoppingItems: [] as unknown[],
    activityEvents: [] as unknown[],
    settings: null as null | { data: Record<string, string>; updatedAt: string }
  };
  const toUpload = {
    items: [] as string[],
    categories: [] as string[],
    warehouses: [] as string[],
    shoppingItems: [] as string[],
    activityEvents: [] as string[],
    settings: false
  };

  const clientItems = new Map(client.items.map((entry) => [entry.uuid, entry.updatedAt]));
  const clientCategories = new Map(client.categories.map((entry) => [entry.uuid, entry.updatedAt]));
  const clientWarehouses = new Map(client.warehouses.map((entry) => [entry.uuid, entry.updatedAt]));
  const clientShopping = new Map(client.shoppingItems.map((entry) => [entry.uuid, entry.updatedAt]));
  const clientEvents = new Map(client.activityEvents.map((entry) => [entry.uuid, entry.updatedAt]));

  items.forEach((item) => {
    const clientUpdatedAt = clientItems.get(item.uuid);
    const verdict = compareUpdated(item.updatedAt, clientUpdatedAt);
    if (verdict === "server_newer") {
      toApply.items.push({
        ...item,
        warehouseUuid: item.warehouseId ? warehouseMap.get(item.warehouseId) ?? null : null,
        categoryUuid: item.categoryId ? categoryMap.get(item.categoryId) ?? null : null
      });
    } else if (verdict === "client_newer") {
      toUpload.items.push(item.uuid);
    }
  });

  clientItems.forEach((_updatedAt, uuid) => {
    if (!items.find((item) => item.uuid === uuid)) {
      toUpload.items.push(uuid);
    }
  });

  categories.forEach((category) => {
    const clientUpdatedAt = clientCategories.get(category.uuid);
    const verdict = compareUpdated(category.updatedAt, clientUpdatedAt);
    if (verdict === "server_newer") {
      toApply.categories.push(category);
    } else if (verdict === "client_newer") {
      toUpload.categories.push(category.uuid);
    }
  });

  clientCategories.forEach((_updatedAt, uuid) => {
    if (!categories.find((category) => category.uuid === uuid)) {
      toUpload.categories.push(uuid);
    }
  });

  warehouses.forEach((warehouse) => {
    const clientUpdatedAt = clientWarehouses.get(warehouse.uuid);
    const verdict = compareUpdated(warehouse.updatedAt, clientUpdatedAt);
    if (verdict === "server_newer") {
      toApply.warehouses.push(warehouse);
    } else if (verdict === "client_newer") {
      toUpload.warehouses.push(warehouse.uuid);
    }
  });

  clientWarehouses.forEach((_updatedAt, uuid) => {
    if (!warehouses.find((warehouse) => warehouse.uuid === uuid)) {
      toUpload.warehouses.push(uuid);
    }
  });

  shoppingItems.forEach((shopping) => {
    const serverUpdatedAt = shopping.completedAt ?? shopping.createdAt;
    const clientUpdatedAt = clientShopping.get(shopping.uuid);
    const verdict = compareUpdated(serverUpdatedAt, clientUpdatedAt);
    if (verdict === "server_newer") {
      toApply.shoppingItems.push(shopping);
    } else if (verdict === "client_newer") {
      toUpload.shoppingItems.push(shopping.uuid);
    }
  });

  clientShopping.forEach((_updatedAt, uuid) => {
    if (!shoppingItems.find((item) => item.uuid === uuid)) {
      toUpload.shoppingItems.push(uuid);
    }
  });

  activityEvents.forEach((event) => {
    const clientUpdatedAt = clientEvents.get(event.uuid);
    const verdict = compareUpdated(event.createdAt, clientUpdatedAt);
    if (verdict === "server_newer") {
      toApply.activityEvents.push(event);
    } else if (verdict === "client_newer") {
      toUpload.activityEvents.push(event.uuid);
    }
  });

  clientEvents.forEach((_updatedAt, uuid) => {
    if (!activityEvents.find((event) => event.uuid === uuid)) {
      toUpload.activityEvents.push(uuid);
    }
  });

  const serverSettings = await getUserSettings(userId);
  if (serverSettings) {
    const clientUpdatedAt = client.settings?.updatedAt;
    const verdict = compareUpdated(new Date(serverSettings.updatedAt), clientUpdatedAt);
    if (verdict === "server_newer") {
      toApply.settings = serverSettings;
    } else if (verdict === "client_newer") {
      toUpload.settings = true;
    }
  } else if (client.settings) {
    toUpload.settings = true;
  }

  return res.json(
    ok({
      toApply,
      toUpload
    })
  );
}

export async function bootstrapSyncAck(req: Request, res: Response) {
  const userId = req.user?.id;
  if (!userId) {
    return res.status(401).json(
      fail({
        code: "UNAUTHORIZED",
        message: "Missing user context"
      })
    );
  }

  const parsed = bootstrapAckSchema.safeParse(req.body);
  if (!parsed.success) {
    return res.status(400).json(
      fail({
        code: "INVALID_INPUT",
        message: "Invalid bootstrap ack payload",
        details: parsed.error.flatten()
      })
    );
  }

  const payload = parsed.data;

  for (const category of payload.categories) {
    await prisma.category.upsert({
      where: { uuid_userId: { uuid: category.uuid, userId } },
      update: {
        name: category.name,
        description: category.description ?? "",
        color: category.color ?? "#6200EE",
        icon: category.icon ?? "category",
        createdAt: parseDate(category.createdAt),
        updatedAt: parseDate(category.updatedAt)
      },
      create: {
        uuid: category.uuid,
        userId,
        name: category.name,
        description: category.description ?? "",
        color: category.color ?? "#6200EE",
        icon: category.icon ?? "category",
        createdAt: parseDate(category.createdAt),
        updatedAt: parseDate(category.updatedAt)
      }
    });
  }

  for (const warehouse of payload.warehouses) {
    await prisma.warehouse.upsert({
      where: { uuid_userId: { uuid: warehouse.uuid, userId } },
      update: {
        name: warehouse.name,
        description: warehouse.description ?? "",
        location: warehouse.location ?? "",
        capacity: warehouse.capacity ?? null,
        parentId: warehouse.parentId ?? null,
        level: warehouse.level ?? 1,
        imageUri: warehouse.imageUri ?? null,
        createdAt: parseDate(warehouse.createdAt),
        updatedAt: parseDate(warehouse.updatedAt)
      },
      create: {
        uuid: warehouse.uuid,
        userId,
        name: warehouse.name,
        description: warehouse.description ?? "",
        location: warehouse.location ?? "",
        capacity: warehouse.capacity ?? null,
        parentId: warehouse.parentId ?? null,
        level: warehouse.level ?? 1,
        imageUri: warehouse.imageUri ?? null,
        createdAt: parseDate(warehouse.createdAt),
        updatedAt: parseDate(warehouse.updatedAt)
      }
    });
  }

  for (const item of payload.items) {
    const resolvedWarehouse = item.warehouseUuid
      ? await prisma.warehouse.findFirst({ where: { uuid: item.warehouseUuid, userId } })
      : null;
    const resolvedCategory = item.categoryUuid
      ? await prisma.category.findFirst({ where: { uuid: item.categoryUuid, userId } })
      : null;

    await prisma.item.upsert({
      where: { uuid_userId: { uuid: item.uuid, userId } },
      update: {
        name: item.name,
        description: item.description ?? "",
        categoryId: resolvedCategory?.id ?? item.categoryId ?? null,
        warehouseId: resolvedWarehouse?.id ?? item.warehouseId ?? null,
        tags: item.tags ?? [],
        purchaseDate: item.purchaseDate ? new Date(item.purchaseDate) : null,
        expiryDate: item.expiryDate ? new Date(item.expiryDate) : null,
        price: item.price ?? null,
        quantity: item.quantity ?? 1,
        barcode: item.barcode ?? null,
        imageUri: item.imageUri ?? null,
        imageUris: item.imageUris ?? [],
        primaryImageIndex: item.primaryImageIndex ?? 0,
        featureCode: item.featureCode ?? null,
        enableStockAlert: item.enableStockAlert ?? true,
        createdAt: parseDate(item.createdAt),
        updatedAt: parseDate(item.updatedAt)
      },
      create: {
        uuid: item.uuid,
        userId,
        name: item.name,
        description: item.description ?? "",
        categoryId: resolvedCategory?.id ?? item.categoryId ?? null,
        warehouseId: resolvedWarehouse?.id ?? item.warehouseId ?? null,
        tags: item.tags ?? [],
        purchaseDate: item.purchaseDate ? new Date(item.purchaseDate) : null,
        expiryDate: item.expiryDate ? new Date(item.expiryDate) : null,
        price: item.price ?? null,
        quantity: item.quantity ?? 1,
        barcode: item.barcode ?? null,
        imageUri: item.imageUri ?? null,
        imageUris: item.imageUris ?? [],
        primaryImageIndex: item.primaryImageIndex ?? 0,
        featureCode: item.featureCode ?? null,
        enableStockAlert: item.enableStockAlert ?? true,
        createdAt: parseDate(item.createdAt),
        updatedAt: parseDate(item.updatedAt)
      }
    });
  }

  for (const shopping of payload.shoppingItems) {
    await prisma.shoppingItem.upsert({
      where: { uuid_userId: { uuid: shopping.uuid, userId } },
      update: {
        name: shopping.name,
        description: shopping.description ?? "",
        quantity: shopping.quantity ?? 1,
        isCompleted: shopping.isCompleted ?? false,
        priority: shopping.priority ?? "MEDIUM",
        completedAt: parseDate(shopping.completedAt) ?? null,
        imageUri: shopping.imageUri ?? null,
        itemUuid: shopping.itemUuid ?? null,
        createdAt: parseDate(shopping.createdAt)
      },
      create: {
        uuid: shopping.uuid,
        userId,
        name: shopping.name,
        description: shopping.description ?? "",
        quantity: shopping.quantity ?? 1,
        isCompleted: shopping.isCompleted ?? false,
        priority: shopping.priority ?? "MEDIUM",
        completedAt: parseDate(shopping.completedAt) ?? null,
        imageUri: shopping.imageUri ?? null,
        itemUuid: shopping.itemUuid ?? null,
        createdAt: parseDate(shopping.createdAt)
      }
    });
  }

  for (const event of payload.activityEvents) {
    await prisma.activityEvent.upsert({
      where: { uuid_userId: { uuid: event.uuid, userId } },
      update: {
        type: event.type as any,
        title: event.title,
        description: event.description ?? "",
        targetUuid: event.targetUuid ?? null,
        targetName: event.targetName ?? "",
        iconType: event.iconType ?? "",
        createdAt: parseDate(event.createdAt),
        metadata: event.metadata ?? ""
      },
      create: {
        uuid: event.uuid,
        userId,
        type: event.type as any,
        title: event.title,
        description: event.description ?? "",
        targetUuid: event.targetUuid ?? null,
        targetName: event.targetName ?? "",
        iconType: event.iconType ?? "",
        createdAt: parseDate(event.createdAt),
        metadata: event.metadata ?? ""
      }
    });
  }

  if (payload.settings?.data) {
    await setUserSettings(userId, {
      data: payload.settings.data,
      updatedAt: payload.settings.updatedAt ?? new Date().toISOString()
    });
  }

  return res.json(ok({ success: true }));
}
