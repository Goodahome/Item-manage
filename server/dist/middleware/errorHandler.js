"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.errorHandler = errorHandler;
const response_1 = require("../utils/response");
function errorHandler(err, _req, res, _next) {
    const message = err instanceof Error ? err.message : "Unknown error";
    return res.status(500).json((0, response_1.fail)({
        code: "INTERNAL_ERROR",
        message
    }));
}
