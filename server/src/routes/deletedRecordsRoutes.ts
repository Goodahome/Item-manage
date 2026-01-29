import { Router } from "express";
import { listDeletedRecords, upsertDeletedRecord } from "../controllers/deletedRecordsController";
import { requireAuth } from "../middleware/auth";

const router = Router();

router.get("/", requireAuth, listDeletedRecords);
router.post("/", requireAuth, upsertDeletedRecord);

export default router;
