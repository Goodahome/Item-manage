import { Router } from "express";
import {
  deleteCategory,
  getCategory,
  listCategories,
  upsertCategory
} from "../controllers/categoriesController";
import { requireAuth } from "../middleware/auth";

const router = Router();

router.get("/", requireAuth, listCategories);
router.get("/:uuid", requireAuth, getCategory);
router.post("/", requireAuth, upsertCategory);
router.put("/:uuid", requireAuth, upsertCategory);
router.delete("/:uuid", requireAuth, deleteCategory);

export default router;
