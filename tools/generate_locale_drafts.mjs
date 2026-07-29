// Automated localization drafts. Output requires native review before final release.
import { readFile, mkdir, writeFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const basePath = path.join(root, "app/src/main/res/values/strings.xml");
const locales = {
  "values-es":"es","values-pt-rBR":"pt","values-fr":"fr","values-de":"de",
  "values-it":"it","values-pl":"pl","values-uk":"uk","values-ru":"ru",
  "values-tr":"tr","values-ar":"ar","values-hi":"hi","values-bn":"bn",
  "values-in":"id","values-vi":"vi","values-th":"th","values-ja":"ja",
  "values-ko":"ko","values-zh-rCN":"zh-CN","values-zh-rTW":"zh-TW",
  "values-nl":"nl","values-sv":"sv","values-cs":"cs","values-ro":"ro",
  "values-el":"el"
};
const protectedTerms = [
  "Privacy Monitor", "Smart Cleaner", "TrashPilot", "Trash DNA",
  "Quick Clean", "Settings", "Reports", "SCAN"
];
const force = process.argv.includes("--force");
const requestedFolders = new Set(process.argv.slice(2).filter(arg => !arg.startsWith("--")));
const overrides = {
  "values-it": { settings_feedback_subject: "Feedback su TrashPilot" },
  "values-nl": { settings_feedback_subject: "Feedback over TrashPilot" },
  "values-sv": { settings_feedback_subject: "Feedback om TrashPilot" },
  "values-ro": { settings_feedback_subject: "Feedback despre TrashPilot" }
};
const decode = value => value
  .replaceAll("&amp;", "&").replaceAll("&lt;", "<").replaceAll("&gt;", ">")
  .replaceAll("&quot;", '"').replaceAll("&apos;", "'");
const escapeXml = value => value
  .replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;")
  .replaceAll("'", "\\'");
const source = await readFile(basePath, "utf8");
const entries = [...source.matchAll(/<string name="([^"]+)"([^>]*)>([\s\S]*?)<\/string>/g)]
  .map(match => ({ name: match[1], attrs: match[2], value: decode(match[3]) }));
const tokenPattern = /%(?:\d+\$)?(?:\.\d+)?[a-zA-Z]|%%/g;

async function translate(value, language) {
  const tokens = [];
  const protect = token => {
    tokens.push(token);
    return `https://127.0.0.1/${tokens.length - 1}`;
  };
  let protectedValue = value;
  for (const term of protectedTerms) {
    protectedValue = protectedValue.replaceAll(term, () => protect(term));
  }
  protectedValue = protectedValue.replace(tokenPattern, protect);
  const url = new URL("https://translate.googleapis.com/translate_a/single");
  for (const [key, val] of Object.entries({
    client: "gtx", sl: "en", tl: language, dt: "t", q: protectedValue
  })) url.searchParams.set(key, val);
  let response;
  for (let attempt = 0; attempt < 4; attempt++) {
    response = await fetch(url, { headers: { "User-Agent": "TrashPilot-localization-draft/1.0" } });
    if (response.ok) break;
    await new Promise(resolve => setTimeout(resolve, 300 * (attempt + 1)));
  }
  if (!response?.ok) throw new Error(`${language}: HTTP ${response?.status}`);
  const payload = await response.json();
  let result = payload[0].map(part => part[0] ?? "").join("");
  tokens.forEach((token, index) => {
    result = result.replaceAll(`https://127.0.0.1/${index}`, token);
  });
  return result;
}

async function mapLimited(values, limit, mapper) {
  const output = new Array(values.length);
  let cursor = 0;
  await Promise.all(Array.from({ length: limit }, async () => {
    while (cursor < values.length) {
      const index = cursor++;
      output[index] = await mapper(values[index]);
    }
  }));
  return output;
}

for (const [folder, language] of Object.entries(locales)) {
  if (requestedFolders.size && !requestedFolders.has(folder)) continue;
  const existingPath = path.join(root, "app/src/main/res", folder, "strings.xml");
  try {
    const existing = await readFile(existingPath, "utf8");
    if (!force && [...existing.matchAll(/<string name="/g)].length === entries.length) {
      process.stdout.write(`${folder}: already complete\n`);
      continue;
    }
  } catch {}
  const localized = await mapLimited(entries, 12, async entry => ({
    ...entry,
    value: overrides[folder]?.[entry.name] ?? (
      entry.attrs.includes('translatable="false"')
        ? entry.value : await translate(entry.value, language)
    )
  }));
  const lines = ['<?xml version="1.0" encoding="utf-8"?>', "<resources>"];
  for (const entry of localized) {
    lines.push(`    <string name="${entry.name}"${entry.attrs}>${escapeXml(entry.value)}</string>`);
  }
  lines.push("</resources>", "");
  const targetDir = path.join(root, "app/src/main/res", folder);
  await mkdir(targetDir, { recursive: true });
  await writeFile(path.join(targetDir, "strings.xml"), lines.join("\n"), "utf8");
  process.stdout.write(`${folder}: ${localized.length} keys\n`);
}
