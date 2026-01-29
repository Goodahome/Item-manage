import { randomUUID } from "crypto";
import { prisma } from "../prisma";

export async function recordDeletion(
  userId: string,
  entityType: string,
  entityUuid: string,
  deletedAt: Date = new Date()
) {
  await prisma.deletedRecord.deleteMany({
    where: { userId, entityType, entityUuid }
  });

  await prisma.deletedRecord.create({
    data: {
      uuid: randomUUID(),
      userId,
      entityType,
      entityUuid,
      deletedAt
    }
  });
}
