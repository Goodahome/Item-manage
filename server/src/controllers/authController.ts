import { Request, Response } from "express";
import bcrypt from "bcryptjs";
import jwt, { SignOptions } from "jsonwebtoken";
import { prisma } from "../prisma";
import { fail, ok } from "../utils/response";
import { loginSchema, registerSchema } from "../validators/auth";

const JWT_SECRET: string = process.env.JWT_SECRET || "dev_secret_change_me";
const JWT_EXPIRES_IN: string = process.env.JWT_EXPIRES_IN || "7d";

export async function register(req: Request, res: Response) {
  const parsed = registerSchema.safeParse(req.body);
  if (!parsed.success) {
    return res.status(400).json(
      fail({
        code: "INVALID_INPUT",
        message: "Invalid register payload",
        details: parsed.error.flatten()
      })
    );
  }

  const { account, displayName, password } = parsed.data;
  const existing = await prisma.user.findUnique({ where: { account } });
  if (existing) {
    return res.status(409).json(
      fail({
        code: "ACCOUNT_EXISTS",
        message: "Account already exists"
      })
    );
  }

  const passwordHash = await bcrypt.hash(password, 10);
  const user = await prisma.user.create({
    data: {
      account,
      displayName,
      passwordHash
    }
  });

  const token = jwt.sign(
    { id: user.id, account: user.account },
    JWT_SECRET,
    { expiresIn: JWT_EXPIRES_IN } as SignOptions
  );

  return res.json(
    ok({
      token,
      user: {
        id: user.id,
        account: user.account,
        displayName: user.displayName
      }
    })
  );
}

export async function login(req: Request, res: Response) {
  const parsed = loginSchema.safeParse(req.body);
  if (!parsed.success) {
    return res.status(400).json(
      fail({
        code: "INVALID_INPUT",
        message: "Invalid login payload",
        details: parsed.error.flatten()
      })
    );
  }

  const { account, password } = parsed.data;
  const user = await prisma.user.findUnique({ where: { account } });
  if (!user) {
    return res.status(401).json(
      fail({
        code: "INVALID_CREDENTIALS",
        message: "Account or password incorrect"
      })
    );
  }

  const isValid = await bcrypt.compare(password, user.passwordHash);
  if (!isValid) {
    return res.status(401).json(
      fail({
        code: "INVALID_CREDENTIALS",
        message: "Account or password incorrect"
      })
    );
  }

  const token = jwt.sign(
    { id: user.id, account: user.account },
    JWT_SECRET,
    { expiresIn: JWT_EXPIRES_IN } as SignOptions
  );

  return res.json(
    ok({
      token,
      user: {
        id: user.id,
        account: user.account,
        displayName: user.displayName
      }
    })
  );
}

export async function refresh(req: Request, res: Response) {
  if (!req.user) {
    return res.status(401).json(
      fail({
        code: "UNAUTHORIZED",
        message: "Missing user context"
      })
    );
  }

  const token = jwt.sign(
    { id: req.user.id, account: req.user.account },
    JWT_SECRET,
    { expiresIn: JWT_EXPIRES_IN } as SignOptions
  );

  return res.json(
    ok({
      token
    })
  );
}

export async function logout(_req: Request, res: Response) {
  return res.json(
    ok({
      message: "Logged out"
    })
  );
}
