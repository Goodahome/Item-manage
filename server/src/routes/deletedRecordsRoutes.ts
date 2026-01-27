import { Router } from "express";
import { listDeletedRecords } from "../controllers/deletedRecordsController";
import { requireAuth } from "../middleware/auth";

const router = Router();

router.get("/", requireAuth, listDeletedRecords);

export default router;
