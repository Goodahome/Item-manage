import { Router } from "express";
import { requireAuth } from "../middleware/auth";
import { bootstrapSync, bootstrapSyncAck } from "../controllers/syncController";

const router = Router();

router.post("/bootstrap", requireAuth, bootstrapSync);
router.post("/bootstrap-ack", requireAuth, bootstrapSyncAck);

export default router;
