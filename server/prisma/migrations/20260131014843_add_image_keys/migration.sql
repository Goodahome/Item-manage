-- AlterTable
ALTER TABLE `Item` ADD COLUMN `imageKeys` JSON NULL;

-- AlterTable
ALTER TABLE `Warehouse` ADD COLUMN `imageKey` VARCHAR(191) NULL;
