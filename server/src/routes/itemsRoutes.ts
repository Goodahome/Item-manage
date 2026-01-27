import { Router } from "express";
import { deleteItem, getItem, listItems, upsertItem } from "../controllers/itemsController";
import { requireAuth } from "../middleware/auth";

const router = Router();

router.get("/", requireAuth, listItems);
router.get("/:uuid", requireAuth, getItem);
router.post("/", requireAuth, upsertItem);
router.put("/:uuid", requireAuth, upsertItem);
router.delete("/:uuid", requireAuth, deleteItem);

export default router;
