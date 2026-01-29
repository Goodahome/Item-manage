"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.parsePagination = parsePagination;
const zod_1 = require("zod");
const paginationSchema = zod_1.z.object({
    page: zod_1.z.coerce.number().int().min(1).default(1),
    pageSize: zod_1.z.coerce.number().int().min(1).max(500).default(20)
});
function parsePagination(query) {
    const parsed = paginationSchema.safeParse(query);
    if (!parsed.success) {
        return { page: 1, pageSize: 20 };
    }
    return parsed.data;
}
