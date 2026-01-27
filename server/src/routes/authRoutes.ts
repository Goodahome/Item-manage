import { Router } from "express";
import { login, logout, refresh, register } from "../controllers/authController";
import { requireAuth } from "../middleware/auth";

const router = Router();

router.post("/register", register);
router.post("/login", login);
router.post("/refresh", requireAuth, refresh);
router.post("/logout", requireAuth, logout);

export default router;
