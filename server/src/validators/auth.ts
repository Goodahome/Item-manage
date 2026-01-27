import { z } from "zod";

export const registerSchema = z.object({
  account: z.string().min(3).max(50),
  displayName: z.string().min(1).max(50),
  password: z.string().min(6).max(100)
});

export const loginSchema = z.object({
  account: z.string().min(3).max(50),
  password: z.string().min(6).max(100)
});
