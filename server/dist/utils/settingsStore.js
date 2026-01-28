"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.getUserSettings = getUserSettings;
exports.setUserSettings = setUserSettings;
const fs_1 = require("fs");
const path_1 = __importDefault(require("path"));
const SETTINGS_PATH = path_1.default.resolve(__dirname, "../../data/user_settings.json");
async function readSettingsFile() {
    try {
        const content = await fs_1.promises.readFile(SETTINGS_PATH, "utf-8");
        return JSON.parse(content);
    }
    catch (error) {
        return {};
    }
}
async function writeSettingsFile(data) {
    await fs_1.promises.mkdir(path_1.default.dirname(SETTINGS_PATH), { recursive: true });
    await fs_1.promises.writeFile(SETTINGS_PATH, JSON.stringify(data, null, 2), "utf-8");
}
async function getUserSettings(userId) {
    const all = await readSettingsFile();
    return all[userId] ?? null;
}
async function setUserSettings(userId, settings) {
    const all = await readSettingsFile();
    all[userId] = settings;
    await writeSettingsFile(all);
}
