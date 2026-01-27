import { NextFunction, Request, Response } from "express";
import { fail } from "../utils/response";

export function errorHandler(
  err: unknown,
  _req: Request,
  res: Response,
  _next: NextFunction
) {
  const message = err instanceof Error ? err.message : "Unknown error";
  return res.status(500).json(
    fail({
      code: "INTERNAL_ERROR",
      message
    })
  );
}
