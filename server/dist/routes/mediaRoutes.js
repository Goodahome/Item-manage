"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const express_1 = require("express");
const auth_1 = require("../middleware/auth");
const mediaController_1 = require("../controllers/mediaController");
const router = (0, express_1.Router)();
router.post("/presign-upload", auth_1.requireAuth, mediaController_1.presignUpload);
router.get("/presign-read", auth_1.requireAuth, mediaController_1.presignRead);
exports.default = router;
