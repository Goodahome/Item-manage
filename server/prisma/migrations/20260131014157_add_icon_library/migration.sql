-- CreateTable
CREATE TABLE `IconLibraryItem` (
    `uuid` VARCHAR(191) NOT NULL,
    `userId` VARCHAR(191) NOT NULL,
    `name` VARCHAR(191) NOT NULL,
    `iconKey` VARCHAR(191) NULL,
    `fileSize` BIGINT NOT NULL,
    `createdAt` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updatedAt` DATETIME(3) NOT NULL,

    INDEX `IconLibraryItem_userId_idx`(`userId`),
    UNIQUE INDEX `IconLibraryItem_uuid_userId_key`(`uuid`, `userId`),
    PRIMARY KEY (`uuid`)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- AddForeignKey
ALTER TABLE `IconLibraryItem` ADD CONSTRAINT `IconLibraryItem_userId_fkey` FOREIGN KEY (`userId`) REFERENCES `User`(`uuid`) ON DELETE CASCADE ON UPDATE CASCADE;
