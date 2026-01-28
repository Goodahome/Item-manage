"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.bootstrapSync = bootstrapSync;
exports.bootstrapSyncAck = bootstrapSyncAck;
const zod_1 = require("zod");
const prisma_1 = require("../prisma");
const response_1 = require("../utils/response");
const settingsStore_1 = require("../utils/settingsStore");
const snapshotEntrySchema = zod_1.z.object({
    uuid: zod_1.z.string().min(1),
    updatedAt: zod_1.z.string().datetime().optional().nullable()
});
const settingsSnapshotSchema = zod_1.z.object({
    data: zod_1.z.record(zod_1.z.string(), zod_1.z.string()).optional().default({}),
    updatedAt: zod_1.z.string().datetime().optional().nullable()
});
const bootstrapSchema = zod_1.z.object({
    items: zod_1.z.array(snapshotEntrySchema).optional().default([]),
    categories: zod_1.z.array(snapshotEntrySchema).optional().default([]),
    warehouses: zod_1.z.array(snapshotEntrySchema).optional().default([]),
    shoppingItems: zod_1.z.array(snapshotEntrySchema).optional().default([]),
    activityEvents: zod_1.z.array(snapshotEntrySchema).optional().default([]),
    settings: settingsSnapshotSchema.optional().nullable()
});
const itemDtoSchema = zod_1.z.object({
    uuid: zod_1.z.string().min(1),
    name: zod_1.z.string().min(1),
    description: zod_1.z.string().optional().nullable(),
    categoryUuid: zod_1.z.string().uuid().optional().nullable(),
    warehouseUuid: zod_1.z.string().uuid().optional().nullable(),
    tags: zod_1.z.array(zod_1.z.string()).optional().nullable(),
    purchaseDate: zod_1.z.string().datetime().optional().nullable(),
    expiryDate: zod_1.z.string().datetime().optional().nullable(),
    price: zod_1.z.number().optional().nullable(),
    quantity: zod_1.z.number().int().optional().nullable(),
    barcode: zod_1.z.string().optional().nullable(),
    imageUri: zod_1.z.string().optional().nullable(),
    imageUris: zod_1.z.array(zod_1.z.string()).optional().nullable(),
    primaryImageIndex: zod_1.z.number().int().optional().nullable(),
    featureCode: zod_1.z.string().optional().nullable(),
    enableStockAlert: zod_1.z.boolean().optional().nullable(),
    createdAt: zod_1.z.string().datetime().optional().nullable(),
    updatedAt: zod_1.z.string().datetime().optional().nullable()
});
const categoryDtoSchema = zod_1.z.object({
    uuid: zod_1.z.string().min(1),
    name: zod_1.z.string().min(1),
    description: zod_1.z.string().optional().nullable(),
    color: zod_1.z.string().optional().nullable(),
    icon: zod_1.z.string().optional().nullable(),
    createdAt: zod_1.z.string().datetime().optional().nullable(),
    updatedAt: zod_1.z.string().datetime().optional().nullable()
});
const warehouseDtoSchema = zod_1.z.object({
    uuid: zod_1.z.string().min(1),
    name: zod_1.z.string().min(1),
    description: zod_1.z.string().optional().nullable(),
    location: zod_1.z.string().optional().nullable(),
    capacity: zod_1.z.number().int().optional().nullable(),
    parentUuid: zod_1.z.string().uuid().optional().nullable(),
    level: zod_1.z.number().int().optional().nullable(),
    imageUri: zod_1.z.string().optional().nullable(),
    createdAt: zod_1.z.string().datetime().optional().nullable(),
    updatedAt: zod_1.z.string().datetime().optional().nullable()
});
const shoppingItemDtoSchema = zod_1.z.object({
    uuid: zod_1.z.string().min(1),
    name: zod_1.z.string().min(1),
    description: zod_1.z.string().optional().nullable(),
    quantity: zod_1.z.number().int().optional().nullable(),
    isCompleted: zod_1.z.boolean().optional().nullable(),
    priority: zod_1.z.string().optional().nullable(),
    createdAt: zod_1.z.string().datetime().optional().nullable(),
    completedAt: zod_1.z.string().datetime().optional().nullable(),
    imageUri: zod_1.z.string().optional().nullable(),
    itemUuid: zod_1.z.string().uuid().optional().nullable()
});
const activityEventDtoSchema = zod_1.z.object({
    uuid: zod_1.z.string().min(1),
    type: zod_1.z.string().min(1),
    title: zod_1.z.string().min(1),
    description: zod_1.z.string().optional().nullable(),
    targetUuid: zod_1.z.string().uuid().optional().nullable(),
    targetName: zod_1.z.string().optional().nullable(),
    iconType: zod_1.z.string().optional().nullable(),
    createdAt: zod_1.z.string().datetime().optional().nullable(),
    metadata: zod_1.z.string().optional().nullable()
});
const bootstrapAckSchema = zod_1.z.object({
    items: zod_1.z.array(itemDtoSchema).optional().default([]),
    categories: zod_1.z.array(categoryDtoSchema).optional().default([]),
    warehouses: zod_1.z.array(warehouseDtoSchema).optional().default([]),
    shoppingItems: zod_1.z.array(shoppingItemDtoSchema).optional().default([]),
    activityEvents: zod_1.z.array(activityEventDtoSchema).optional().default([]),
    settings: settingsSnapshotSchema.optional().nullable()
});
function parseDate(value) {
    return value ? new Date(value) : undefined;
}
function compareUpdated(serverDate, clientDateStr) {
    if (!clientDateStr)
        return "server_newer";
    const clientDate = new Date(clientDateStr);
    if (!serverDate || Number.isNaN(serverDate.getTime()) || Number.isNaN(clientDate.getTime())) {
        return "server_newer";
    }
    if (serverDate.getTime() > clientDate.getTime())
        return "server_newer";
    if (serverDate.getTime() < clientDate.getTime())
        return "client_newer";
    return "same";
}
async function bootstrapSync(req, res) {
    const userId = req.user?.uuid;
    if (!userId) {
        return res.status(401).json((0, response_1.fail)({
            code: "UNAUTHORIZED",
            message: "Missing user context"
        }));
    }
    const parsed = bootstrapSchema.safeParse(req.body);
    if (!parsed.success) {
        return res.status(400).json((0, response_1.fail)({
            code: "INVALID_INPUT",
            message: "Invalid bootstrap payload",
            details: parsed.error.flatten()
        }));
    }
    const client = parsed.data;
    const [items, categories, warehouses, shoppingItems, activityEvents] = await Promise.all([
        prisma_1.prisma.item.findMany({ where: { userId } }),
        prisma_1.prisma.category.findMany({ where: { userId } }),
        prisma_1.prisma.warehouse.findMany({ where: { userId } }),
        prisma_1.prisma.shoppingItem.findMany({ where: { userId } }),
        prisma_1.prisma.activityEvent.findMany({ where: { userId } })
    ]);
    const toApply = {
        items: [],
        categories: [],
        warehouses: [],
        shoppingItems: [],
        activityEvents: [],
        settings: null
    };
    const toUpload = {
        items: [],
        categories: [],
        warehouses: [],
        shoppingItems: [],
        activityEvents: [],
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
                warehouseUuid: item.warehouseUuid ?? null,
                categoryUuid: item.categoryUuid ?? null
            });
        }
        else if (verdict === "client_newer") {
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
        }
        else if (verdict === "client_newer") {
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
        }
        else if (verdict === "client_newer") {
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
        }
        else if (verdict === "client_newer") {
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
        }
        else if (verdict === "client_newer") {
            toUpload.activityEvents.push(event.uuid);
        }
    });
    clientEvents.forEach((_updatedAt, uuid) => {
        if (!activityEvents.find((event) => event.uuid === uuid)) {
            toUpload.activityEvents.push(uuid);
        }
    });
    const serverSettings = await (0, settingsStore_1.getUserSettings)(userId);
    if (serverSettings) {
        const clientUpdatedAt = client.settings?.updatedAt;
        const verdict = compareUpdated(new Date(serverSettings.updatedAt), clientUpdatedAt);
        if (verdict === "server_newer") {
            toApply.settings = serverSettings;
        }
        else if (verdict === "client_newer") {
            toUpload.settings = true;
        }
    }
    else if (client.settings) {
        toUpload.settings = true;
    }
    return res.json((0, response_1.ok)({
        toApply,
        toUpload
    }));
}
async function bootstrapSyncAck(req, res) {
    const userId = req.user?.uuid;
    if (!userId) {
        return res.status(401).json((0, response_1.fail)({
            code: "UNAUTHORIZED",
            message: "Missing user context"
        }));
    }
    const parsed = bootstrapAckSchema.safeParse(req.body);
    if (!parsed.success) {
        return res.status(400).json((0, response_1.fail)({
            code: "INVALID_INPUT",
            message: "Invalid bootstrap ack payload",
            details: parsed.error.flatten()
        }));
    }
    const payload = parsed.data;
    for (const category of payload.categories) {
        await prisma_1.prisma.category.upsert({
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
        await prisma_1.prisma.warehouse.upsert({
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
                updatedAt: parseDate(warehouse.updatedAt)
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
                updatedAt: parseDate(warehouse.updatedAt)
            }
        });
    }
    for (const item of payload.items) {
        const categoryUuid = item.categoryUuid ?? null;
        const warehouseUuid = item.warehouseUuid ?? null;
        await prisma_1.prisma.item.upsert({
            where: { uuid_userId: { uuid: item.uuid, userId } },
            update: {
                name: item.name,
                description: item.description ?? "",
                categoryUuid: categoryUuid,
                warehouseUuid: warehouseUuid,
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
                categoryUuid: categoryUuid,
                warehouseUuid: warehouseUuid,
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
        await prisma_1.prisma.shoppingItem.upsert({
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
        await prisma_1.prisma.activityEvent.upsert({
            where: { uuid_userId: { uuid: event.uuid, userId } },
            update: {
                type: event.type,
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
                type: event.type,
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
        await (0, settingsStore_1.setUserSettings)(userId, {
            data: payload.settings.data,
            updatedAt: payload.settings.updatedAt ?? new Date().toISOString()
        });
    }
    return res.json((0, response_1.ok)({ success: true }));
}
