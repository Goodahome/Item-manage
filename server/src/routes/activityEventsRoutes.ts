import { Router } from "express";
import { listActivityEvents, getActivityEvent, upsertActivityEvent } from "../controllers/activityEventsController";
import { requireAuth } from "../middleware/auth";

const router = Router();

router.get("/", requireAuth, listActivityEvents);
router.get("/:uuid", requireAuth, getActivityEvent);
router.post("/", requireAuth, upsertActivityEvent);

export default router;
