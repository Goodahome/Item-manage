-- CreateTable
CREATE TABLE `User` (
    `uuid` VARCHAR(191) NOT NULL,
    `account` VARCHAR(191) NOT NULL,
    `displayName` VARCHAR(191) NOT NULL,
    `passwordHash` VARCHAR(191) NOT NULL,
    `createdAt` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updatedAt` DATETIME(3) NOT NULL,

    UNIQUE INDEX `User_account_key`(`account`),
    PRIMARY KEY (`uuid`)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- CreateTable
CREATE TABLE `Item` (
    `uuid` VARCHAR(191) NOT NULL,
    `userId` VARCHAR(191) NOT NULL,
    `name` VARCHAR(191) NOT NULL,
    `description` VARCHAR(191) NOT NULL DEFAULT '',
    `categoryId` VARCHAR(191) NULL,
    `warehouseId` VARCHAR(191) NULL,
    `tags` JSON NULL,
    `purchaseDate` DATETIME(3) NULL,
    `expiryDate` DATETIME(3) NULL,
    `price` DOUBLE NULL,
    `quantity` INTEGER NOT NULL DEFAULT 1,
    `barcode` VARCHAR(191) NULL,
    `imageUri` VARCHAR(191) NULL,
    `imageUris` JSON NULL,
    `primaryImageIndex` INTEGER NOT NULL DEFAULT 0,
    `featureCode` VARCHAR(191) NULL,
    `enableStockAlert` BOOLEAN NOT NULL DEFAULT true,
    `createdAt` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updatedAt` DATETIME(3) NOT NULL,

    INDEX `Item_userId_idx`(`userId`),
    UNIQUE INDEX `Item_uuid_userId_key`(`uuid`, `userId`),
    PRIMARY KEY (`uuid`)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- CreateTable
CREATE TABLE `Category` (
    `uuid` VARCHAR(191) NOT NULL,
    `userId` VARCHAR(191) NOT NULL,
    `name` VARCHAR(191) NOT NULL,
    `description` VARCHAR(191) NOT NULL DEFAULT '',
    `color` VARCHAR(191) NOT NULL DEFAULT '#6200EE',
    `icon` VARCHAR(191) NOT NULL DEFAULT 'category',
    `createdAt` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updatedAt` DATETIME(3) NOT NULL,

    INDEX `Category_userId_idx`(`userId`),
    UNIQUE INDEX `Category_uuid_userId_key`(`uuid`, `userId`),
    PRIMARY KEY (`uuid`)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- CreateTable
CREATE TABLE `Warehouse` (
    `uuid` VARCHAR(191) NOT NULL,
    `userId` VARCHAR(191) NOT NULL,
    `name` VARCHAR(191) NOT NULL,
    `description` VARCHAR(191) NOT NULL DEFAULT '',
    `location` VARCHAR(191) NOT NULL DEFAULT '',
    `capacity` INTEGER NULL,
    `parentId` VARCHAR(191) NULL,
    `level` INTEGER NOT NULL DEFAULT 1,
    `imageUri` VARCHAR(191) NULL,
    `createdAt` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updatedAt` DATETIME(3) NOT NULL,

    INDEX `Warehouse_userId_idx`(`userId`),
    UNIQUE INDEX `Warehouse_uuid_userId_key`(`uuid`, `userId`),
    PRIMARY KEY (`uuid`)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- CreateTable
CREATE TABLE `ShoppingItem` (
    `uuid` VARCHAR(191) NOT NULL,
    `userId` VARCHAR(191) NOT NULL,
    `name` VARCHAR(191) NOT NULL,
    `description` VARCHAR(191) NOT NULL DEFAULT '',
    `quantity` INTEGER NOT NULL DEFAULT 1,
    `isCompleted` BOOLEAN NOT NULL DEFAULT false,
    `priority` VARCHAR(191) NOT NULL DEFAULT 'MEDIUM',
    `createdAt` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `completedAt` DATETIME(3) NULL,
    `imageUri` VARCHAR(191) NULL,
    `itemUuid` VARCHAR(191) NULL,

    INDEX `ShoppingItem_userId_idx`(`userId`),
    UNIQUE INDEX `ShoppingItem_uuid_userId_key`(`uuid`, `userId`),
    PRIMARY KEY (`uuid`)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- CreateTable
CREATE TABLE `ItemReminder` (
    `uuid` VARCHAR(191) NOT NULL,
    `userId` VARCHAR(191) NOT NULL,
    `itemUuid` VARCHAR(191) NOT NULL,
    `reminderType` ENUM('ONCE', 'DAILY', 'MONTHLY', 'YEARLY') NOT NULL,
    `reminderTime` DATETIME(3) NULL,
    `dailyTime` VARCHAR(191) NULL,
    `monthlyDay` INTEGER NULL,
    `monthlyTime` VARCHAR(191) NULL,
    `yearlyMonth` INTEGER NULL,
    `yearlyDay` INTEGER NULL,
    `yearlyTime` VARCHAR(191) NULL,
    `reason` VARCHAR(191) NOT NULL DEFAULT '',
    `isEnabled` BOOLEAN NOT NULL DEFAULT true,
    `createdAt` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updatedAt` DATETIME(3) NOT NULL,

    INDEX `ItemReminder_userId_idx`(`userId`),
    UNIQUE INDEX `ItemReminder_uuid_userId_key`(`uuid`, `userId`),
    PRIMARY KEY (`uuid`)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- CreateTable
CREATE TABLE `ActivityEvent` (
    `uuid` VARCHAR(191) NOT NULL,
    `userId` VARCHAR(191) NOT NULL,
    `type` ENUM('ITEM_ADDED', 'ITEM_DELETED', 'ITEM_UPDATED', 'ITEM_USED', 'ITEM_VIEWED', 'WAREHOUSE_ADDED', 'WAREHOUSE_DELETED', 'WAREHOUSE_UPDATED', 'REMINDER_TRIGGERED', 'ITEM_EXPIRING', 'ITEM_LOW_STOCK') NOT NULL,
    `title` VARCHAR(191) NOT NULL,
    `description` VARCHAR(191) NOT NULL DEFAULT '',
    `targetUuid` VARCHAR(191) NULL,
    `targetName` VARCHAR(191) NOT NULL DEFAULT '',
    `iconType` VARCHAR(191) NOT NULL DEFAULT '',
    `createdAt` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `metadata` VARCHAR(191) NOT NULL DEFAULT '',

    INDEX `ActivityEvent_userId_idx`(`userId`),
    UNIQUE INDEX `ActivityEvent_uuid_userId_key`(`uuid`, `userId`),
    PRIMARY KEY (`uuid`)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- CreateTable
CREATE TABLE `DeletedRecord` (
    `uuid` VARCHAR(191) NOT NULL,
    `userId` VARCHAR(191) NOT NULL,
    `entityType` VARCHAR(191) NOT NULL,
    `entityUuid` VARCHAR(191) NOT NULL,
    `deletedAt` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    INDEX `DeletedRecord_userId_idx`(`userId`),
    UNIQUE INDEX `DeletedRecord_uuid_userId_key`(`uuid`, `userId`),
    PRIMARY KEY (`uuid`)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- AddForeignKey
ALTER TABLE `Item` ADD CONSTRAINT `Item_userId_fkey` FOREIGN KEY (`userId`) REFERENCES `User`(`uuid`) ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE `Category` ADD CONSTRAINT `Category_userId_fkey` FOREIGN KEY (`userId`) REFERENCES `User`(`uuid`) ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE `Warehouse` ADD CONSTRAINT `Warehouse_userId_fkey` FOREIGN KEY (`userId`) REFERENCES `User`(`uuid`) ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE `ShoppingItem` ADD CONSTRAINT `ShoppingItem_userId_fkey` FOREIGN KEY (`userId`) REFERENCES `User`(`uuid`) ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE `ItemReminder` ADD CONSTRAINT `ItemReminder_userId_fkey` FOREIGN KEY (`userId`) REFERENCES `User`(`uuid`) ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE `ActivityEvent` ADD CONSTRAINT `ActivityEvent_userId_fkey` FOREIGN KEY (`userId`) REFERENCES `User`(`uuid`) ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE `DeletedRecord` ADD CONSTRAINT `DeletedRecord_userId_fkey` FOREIGN KEY (`userId`) REFERENCES `User`(`uuid`) ON DELETE CASCADE ON UPDATE CASCADE;
