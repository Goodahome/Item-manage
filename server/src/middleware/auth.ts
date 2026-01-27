import { NextFunction, Request, Response } from "express";
import jwt from "jsonwebtoken";
import { fail } from "../utils/response";

const JWT_SECRET = process.env.JWT_SECRET || "dev_secret_change_me";

type TokenPayload = {
  id: number;
  account: string;
};

export function requireAuth(req: Request, res: Response, next: NextFunction) {
  const authHeader = req.headers.authorization || "";
  const token = authHeader.startsWith("Bearer ") ? authHeader.slice(7) : "";
  if (!token) {
    return res.status(401).json(
      fail({
        code: "UNAUTHORIZED",
        message: "Missing token"
      })
    );
  }

  try {
    const payload = jwt.verify(token, JWT_SECRET) as TokenPayload;
    req.user = { id: payload.id, account: payload.account };
    return next();
  } catch (error) {
    return res.status(401).json(
      fail({
        code: "INVALID_TOKEN",
        message: "Invalid or expired token"
      })
    );
  }
}
