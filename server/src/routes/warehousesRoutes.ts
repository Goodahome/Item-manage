import { Router } from "express";
import {
  deleteWarehouse,
  getWarehouse,
  listWarehouses,
  upsertWarehouse
} from "../controllers/warehousesController";
import { requireAuth } from "../middleware/auth";

const router = Router();

router.get("/", requireAuth, listWarehouses);
router.get("/:uuid", requireAuth, getWarehouse);
router.post("/", requireAuth, upsertWarehouse);
router.put("/:uuid", requireAuth, upsertWarehouse);
router.delete("/:uuid", requireAuth, deleteWarehouse);

export default router;
