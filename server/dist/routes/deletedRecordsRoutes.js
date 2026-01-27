"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const express_1 = require("express");
const deletedRecordsController_1 = require("../controllers/deletedRecordsController");
const auth_1 = require("../middleware/auth");
const router = (0, express_1.Router)();
router.get("/", auth_1.requireAuth, deletedRecordsController_1.listDeletedRecords);
exports.default = router;
