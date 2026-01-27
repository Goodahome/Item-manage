import { Router } from "express";
import {
  deleteReminder,
  getReminder,
  listReminders,
  upsertReminder
} from "../controllers/remindersController";
import { requireAuth } from "../middleware/auth";

const router = Router();

router.get("/", requireAuth, listReminders);
router.get("/:uuid", requireAuth, getReminder);
router.post("/", requireAuth, upsertReminder);
router.put("/:uuid", requireAuth, upsertReminder);
router.delete("/:uuid", requireAuth, deleteReminder);

export default router;
