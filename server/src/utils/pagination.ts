import { z } from "zod";

const paginationSchema = z.object({
  page: z.coerce.number().int().min(1).default(1),
  pageSize: z.coerce.number().int().min(1).max(500).default(20)
});

export function parsePagination(query: unknown) {
  const parsed = paginationSchema.safeParse(query);
  if (!parsed.success) {
    return { page: 1, pageSize: 20 };
  }
  return parsed.data;
}
