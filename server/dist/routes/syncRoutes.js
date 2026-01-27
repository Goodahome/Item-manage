"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const express_1 = require("express");
const auth_1 = require("../middleware/auth");
const syncController_1 = require("../controllers/syncController");
const router = (0, express_1.Router)();
router.post("/bootstrap", auth_1.requireAuth, syncController_1.bootstrapSync);
router.post("/bootstrap-ack", auth_1.requireAuth, syncController_1.bootstrapSyncAck);
exports.default = router;
