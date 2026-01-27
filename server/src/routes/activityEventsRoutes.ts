import { Router } from "express";
import { listActivityEvents, getActivityEvent } from "../controllers/activityEventsController";
import { requireAuth } from "../middleware/auth";

const router = Router();

router.get("/", requireAuth, listActivityEvents);
router.get("/:uuid", requireAuth, getActivityEvent);

export default router;
