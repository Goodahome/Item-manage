import { promises as fs } from "fs";
import path from "path";

type StoredSettings = {
  data: Record<string, string>;
  updatedAt: string;
};

type SettingsFile = Record<string, StoredSettings>;

const SETTINGS_PATH = path.resolve(__dirname, "../../data/user_settings.json");

async function readSettingsFile(): Promise<SettingsFile> {
  try {
    const content = await fs.readFile(SETTINGS_PATH, "utf-8");
    return JSON.parse(content) as SettingsFile;
  } catch (error) {
    return {};
  }
}

async function writeSettingsFile(data: SettingsFile) {
  await fs.mkdir(path.dirname(SETTINGS_PATH), { recursive: true });
  await fs.writeFile(SETTINGS_PATH, JSON.stringify(data, null, 2), "utf-8");
}

export async function getUserSettings(userId: number): Promise<StoredSettings | null> {
  const all = await readSettingsFile();
  return all[String(userId)] ?? null;
}

export async function setUserSettings(userId: number, settings: StoredSettings) {
  const all = await readSettingsFile();
  all[String(userId)] = settings;
  await writeSettingsFile(all);
}
