import { Router } from "express";
import {
  deleteShoppingItem,
  getShoppingItem,
  listShoppingItems,
  upsertShoppingItem
} from "../controllers/shoppingItemsController";
import { requireAuth } from "../middleware/auth";

const router = Router();

router.get("/", requireAuth, listShoppingItems);
router.get("/:uuid", requireAuth, getShoppingItem);
router.post("/", requireAuth, upsertShoppingItem);
router.put("/:uuid", requireAuth, upsertShoppingItem);
router.delete("/:uuid", requireAuth, deleteShoppingItem);

export default router;
