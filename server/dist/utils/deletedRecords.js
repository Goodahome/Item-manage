"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.recordDeletion = recordDeletion;
const crypto_1 = require("crypto");
const prisma_1 = require("../prisma");
async function recordDeletion(userId, entityType, entityUuid, deletedAt = new Date()) {
    await prisma_1.prisma.deletedRecord.deleteMany({
        where: { userId, entityType, entityUuid }
    });
    await prisma_1.prisma.deletedRecord.create({
        data: {
            uuid: (0, crypto_1.randomUUID)(),
            userId,
            entityType,
            entityUuid,
            deletedAt
        }
    });
}
