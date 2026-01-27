import { Router } from "express";
import { requireAuth } from "../middleware/auth";
import { presignRead, presignUpload } from "../controllers/mediaController";

const router = Router();

router.post("/presign-upload", requireAuth, presignUpload);
router.get("/presign-read", requireAuth, presignRead);

export default router;
