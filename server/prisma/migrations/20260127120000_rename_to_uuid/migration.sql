-- Rename columns from Id to Uuid
ALTER TABLE `Item` CHANGE `categoryId` `categoryUuid` VARCHAR(191) NULL;
ALTER TABLE `Item` CHANGE `warehouseId` `warehouseUuid` VARCHAR(191) NULL;
ALTER TABLE `Warehouse` CHANGE `parentId` `parentUuid` VARCHAR(191) NULL;
