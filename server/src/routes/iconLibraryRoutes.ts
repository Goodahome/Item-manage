import { Router } from "express";
import {
  deleteIconLibraryItem,
  getIconLibraryItem,
  listIconLibraryItems,
  upsertIconLibraryItem
} from "../controllers/iconLibraryController";
import { requireAuth } from "../middleware/auth";

const router = Router();

router.get("/", requireAuth, listIconLibraryItems);
router.get("/:uuid", requireAuth, getIconLibraryItem);
router.post("/", requireAuth, upsertIconLibraryItem);
router.delete("/:uuid", requireAuth, deleteIconLibraryItem);

export default router;
