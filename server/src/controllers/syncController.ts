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
  categoryUuid: z.string().uuid().optional().nullable(),
  warehouseUuid: z.string().uuid().optional().nullable(),
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
  parentUuid: z.string().uuid().optional().nullable(),
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
  itemUuid: z.string().uuid().optional().nullable()
});

const activityEventDtoSchema = z.object({
  uuid: z.string().min(1),
  type: z.string().min(1),
  title: z.string().min(1),
  description: z.string().optional().nullable(),
  targetUuid: z.string().uuid().optional().nullable(),
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

const MAX_SYNC_ACTIVITY_EVENTS = 5;

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
  const userId = req.user?.uuid;
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
    prisma.activityEvent.findMany({
      where: { userId },
      orderBy: { createdAt: "desc" },
      take: MAX_SYNC_ACTIVITY_EVENTS
    })
  ]);

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
  const clientEvents = new Map(
    client.activityEvents
      .slice()
      .sort((a, b) => {
        const aTime = a.updatedAt ? new Date(a.updatedAt).getTime() : 0;
        const bTime = b.updatedAt ? new Date(b.updatedAt).getTime() : 0;
        return bTime - aTime;
      })
      .slice(0, MAX_SYNC_ACTIVITY_EVENTS)
      .map((entry) => [entry.uuid, entry.updatedAt])
  );

  items.forEach((item) => {
    const clientUpdatedAt = clientItems.get(item.uuid);
    const verdict = compareUpdated(item.updatedAt, clientUpdatedAt);
    if (verdict === "server_newer") {
      toApply.items.push({
        ...item,
        warehouseUuid: item.warehouseUuid ?? null,
        categoryUuid: item.categoryUuid ?? null
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
  const userId = req.user?.uuid;
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
  const limitedActivityEvents = payload.activityEvents.slice(0, MAX_SYNC_ACTIVITY_EVENTS);

  // 辅助函数：检查是否为示例数据（通过名称模式）
  const isSampleData = (name: string): boolean => {
    const sampleKeywords = ["示例", "Sample", "sample", "EXAMPLE", "Example", "演示", "Demo", "demo"];
    return sampleKeywords.some(keyword => name.includes(keyword));
  };

  // 冲突记录列表，用于返回给客户端
  const conflicts = {
    items: [] as string[],
    categories: [] as string[],
    warehouses: [] as string[],
    shoppingItems: [] as string[],
    activityEvents: [] as string[]
  };

  // 使用事务确保多设备并发更新的一致性
  // 使用 Serializable 隔离级别确保最高一致性
  try {
    await prisma.$transaction(async (tx) => {
      // 处理分类 - 使用乐观锁检查
    for (const category of payload.categories) {
      // 拒绝示例数据
      if (isSampleData(category.name)) {
        console.log(`拒绝示例分类: ${category.name} (UUID: ${category.uuid})`);
        continue;
      }
      const existing = await tx.category.findUnique({
        where: { uuid_userId: { uuid: category.uuid, userId } }
      });

      if (existing) {
        // 乐观锁：如果客户端发送的 updatedAt 早于服务器当前的 updatedAt，拒绝更新
        const clientUpdatedAt = parseDate(category.updatedAt);
        const serverUpdatedAt = existing.updatedAt;
        
        if (clientUpdatedAt && serverUpdatedAt && clientUpdatedAt < serverUpdatedAt) {
          conflicts.categories.push(category.uuid);
          continue; // 跳过这个更新，保留服务器版本
        }
      }

      await tx.category.upsert({
        where: { uuid_userId: { uuid: category.uuid, userId } },
        update: {
          name: category.name,
          description: category.description ?? "",
          color: category.color ?? "#6200EE",
          icon: category.icon ?? "category",
          createdAt: parseDate(category.createdAt),
          updatedAt: parseDate(category.updatedAt) ?? new Date()
        },
        create: {
          uuid: category.uuid,
          userId,
          name: category.name,
          description: category.description ?? "",
          color: category.color ?? "#6200EE",
          icon: category.icon ?? "category",
          createdAt: parseDate(category.createdAt),
          updatedAt: parseDate(category.updatedAt) ?? new Date()
        }
      });
    }

    // 处理容器 - 使用乐观锁检查
    for (const warehouse of payload.warehouses) {
      // 拒绝示例数据
      if (isSampleData(warehouse.name)) {
        console.log(`拒绝示例容器: ${warehouse.name} (UUID: ${warehouse.uuid})`);
        continue;
      }

      const existing = await tx.warehouse.findUnique({
        where: { uuid_userId: { uuid: warehouse.uuid, userId } }
      });

      if (existing) {
        // 乐观锁：如果客户端发送的 updatedAt 早于服务器当前的 updatedAt，拒绝更新
        const clientUpdatedAt = parseDate(warehouse.updatedAt);
        const serverUpdatedAt = existing.updatedAt;
        
        if (clientUpdatedAt && serverUpdatedAt && clientUpdatedAt < serverUpdatedAt) {
          conflicts.warehouses.push(warehouse.uuid);
          continue; // 跳过这个更新，保留服务器版本
        }
      }

      await tx.warehouse.upsert({
        where: { uuid_userId: { uuid: warehouse.uuid, userId } },
        update: {
          name: warehouse.name,
          description: warehouse.description ?? "",
          location: warehouse.location ?? "",
          capacity: warehouse.capacity ?? null,
          parentUuid: warehouse.parentUuid ?? null,
          level: warehouse.level ?? 1,
          imageUri: warehouse.imageUri ?? null,
          createdAt: parseDate(warehouse.createdAt),
          updatedAt: parseDate(warehouse.updatedAt) ?? new Date()
        },
        create: {
          uuid: warehouse.uuid,
          userId,
          name: warehouse.name,
          description: warehouse.description ?? "",
          location: warehouse.location ?? "",
          capacity: warehouse.capacity ?? null,
          parentUuid: warehouse.parentUuid ?? null,
          level: warehouse.level ?? 1,
          imageUri: warehouse.imageUri ?? null,
          createdAt: parseDate(warehouse.createdAt),
          updatedAt: parseDate(warehouse.updatedAt) ?? new Date()
        }
      });
    }

    // 处理物品 - 使用乐观锁检查
    for (const item of payload.items) {
      // 拒绝示例数据
      if (isSampleData(item.name)) {
        console.log(`拒绝示例物品: ${item.name} (UUID: ${item.uuid})`);
        continue;
      }

      const categoryUuid = item.categoryUuid ?? null;
      const warehouseUuid = item.warehouseUuid ?? null;

      const existing = await tx.item.findUnique({
        where: { uuid_userId: { uuid: item.uuid, userId } }
      });

      if (existing) {
        // 乐观锁：如果客户端发送的 updatedAt 早于服务器当前的 updatedAt，拒绝更新
        const clientUpdatedAt = parseDate(item.updatedAt);
        const serverUpdatedAt = existing.updatedAt;
        
        if (clientUpdatedAt && serverUpdatedAt && clientUpdatedAt < serverUpdatedAt) {
          conflicts.items.push(item.uuid);
          continue; // 跳过这个更新，保留服务器版本
        }
      }

      await tx.item.upsert({
        where: { uuid_userId: { uuid: item.uuid, userId } },
        update: {
          name: item.name,
          description: item.description ?? "",
          categoryUuid,
          warehouseUuid,
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
          updatedAt: parseDate(item.updatedAt) ?? new Date()
        },
        create: {
          uuid: item.uuid,
          userId,
          name: item.name,
          description: item.description ?? "",
          categoryUuid,
          warehouseUuid,
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
          updatedAt: parseDate(item.updatedAt) ?? new Date()
        }
      });
    }

    // 处理购物项 - 使用乐观锁检查（使用 completedAt 或 createdAt）
    for (const shopping of payload.shoppingItems) {
      // 拒绝示例数据
      if (isSampleData(shopping.name)) {
        console.log(`拒绝示例购物项: ${shopping.name} (UUID: ${shopping.uuid})`);
        continue;
      }

      const existing = await tx.shoppingItem.findUnique({
        where: { uuid_userId: { uuid: shopping.uuid, userId } }
      });

      if (existing) {
        // 乐观锁：比较 completedAt 或 createdAt
        const clientTime = parseDate(shopping.completedAt) ?? parseDate(shopping.createdAt);
        const serverTime = existing.completedAt ?? existing.createdAt;
        
        if (clientTime && serverTime && clientTime < serverTime) {
          conflicts.shoppingItems.push(shopping.uuid);
          continue; // 跳过这个更新，保留服务器版本
        }
      }

      await tx.shoppingItem.upsert({
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

    // 处理活动事件 - 使用乐观锁检查（使用 createdAt）
    for (const event of limitedActivityEvents) {
      const existing = await tx.activityEvent.findUnique({
        where: { uuid_userId: { uuid: event.uuid, userId } }
      });

      if (existing) {
        // 乐观锁：如果客户端发送的 createdAt 早于服务器当前的 createdAt，拒绝更新
        const clientCreatedAt = parseDate(event.createdAt);
        const serverCreatedAt = existing.createdAt;
        
        if (clientCreatedAt && serverCreatedAt && clientCreatedAt < serverCreatedAt) {
          conflicts.activityEvents.push(event.uuid);
          continue; // 跳过这个更新，保留服务器版本
        }
      }

      await tx.activityEvent.upsert({
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

    const latestEvents = await tx.activityEvent.findMany({
      where: { userId },
      orderBy: { createdAt: "desc" },
      take: MAX_SYNC_ACTIVITY_EVENTS,
      select: { uuid: true }
    });
    if (latestEvents.length > 0) {
      await tx.activityEvent.deleteMany({
        where: {
          userId,
          uuid: { notIn: latestEvents.map((entry) => entry.uuid) }
        }
      });
    }

    if (payload.settings?.data) {
      await setUserSettings(userId, {
        data: payload.settings.data,
        updatedAt: payload.settings.updatedAt ?? new Date().toISOString()
      });
    }
    }, {
      timeout: 30000, // 30秒超时，确保有足够时间处理大量数据
      isolationLevel: 'Serializable' as const // 使用可串行化隔离级别，确保最高一致性
    });

    // 返回成功结果和冲突信息
    return res.json(ok({ 
      success: true,
      conflicts: conflicts // 返回冲突的 UUID 列表，客户端需要重新拉取这些记录
    }));
  } catch (error: any) {
    // 处理事务错误（可能是超时或死锁）
    console.error("Sync transaction error:", error);
    
    // 如果是超时错误，返回特殊错误码
    if (error.code === 'P2034' || error.message?.includes('timeout')) {
      return res.status(408).json(
        fail({
          code: "SYNC_TIMEOUT",
          message: "Sync operation timed out. Please try again with fewer items.",
          details: { conflicts } // 返回已检测到的冲突
        })
      );
    }
    
    // 如果是死锁错误，返回特殊错误码
    if (error.code === 'P2034' || error.message?.includes('deadlock')) {
      return res.status(409).json(
        fail({
          code: "SYNC_CONFLICT",
          message: "Database conflict detected. Please sync again.",
          details: { conflicts } // 返回已检测到的冲突
        })
      );
    }
    
    // 其他错误
    return res.status(500).json(
      fail({
        code: "SYNC_ERROR",
        message: "Failed to sync data",
        details: error.message
      })
    );
  }
}
