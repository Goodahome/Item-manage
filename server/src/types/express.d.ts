import "express";

declare module "express-serve-static-core" {
  interface Request {
    user?: {
      uuid: string;
      account: string;
    };
  }
}
