import { readFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const res = path.join(root, "app/src/main/res");
const folders = [
  "values","values-es","values-pt-rBR","values-fr","values-de","values-it",
  "values-pl","values-uk","values-ru","values-tr","values-ar","values-hi",
  "values-bn","values-in","values-vi","values-th","values-ja","values-ko",
  "values-zh-rCN","values-zh-rTW","values-nl","values-sv","values-cs",
  "values-ro","values-el"
];
const tokenPattern = /%(?:\d+\$)?(?:\.\d+)?[a-zA-Z]|%%/g;
const protectedTerms = [
  "SCAN", "TrashPilot", "Trash DNA", "Quick Clean",
  "Privacy Monitor", "Reports", "Settings", "Smart Cleaner"
];
const countOf = (value, term) => value.split(term).length - 1;
async function catalog(folder) {
  const xml = await readFile(path.join(res, folder, "strings.xml"), "utf8");
  const values = new Map();
  for (const match of xml.matchAll(/<string name="([^"]+)"[^>]*>([\s\S]*?)<\/string>/g)) {
    if (values.has(match[1])) throw new Error(`${folder}: duplicate ${match[1]}`);
    values.set(match[1], match[2]);
  }
  if (!xml.includes("<resources>") || !xml.includes("</resources>")) {
    throw new Error(`${folder}: malformed resources root`);
  }
  return values;
}
const base = await catalog("values");
const failures = [];
for (const folder of folders.slice(1)) {
  const localized = await catalog(folder);
  for (const key of base.keys()) if (!localized.has(key)) failures.push(`${folder}: missing ${key}`);
  for (const key of localized.keys()) if (!base.has(key)) failures.push(`${folder}: extra ${key}`);
  for (const [key, value] of base) {
    if (!localized.has(key)) continue;
    const expected = [...value.matchAll(tokenPattern)].map(x => x[0]).sort().join("|");
    const actual = [...localized.get(key).matchAll(tokenPattern)].map(x => x[0]).sort().join("|");
    if (expected !== actual) failures.push(`${folder}:${key}: format mismatch ${expected} != ${actual}`);
    for (const term of protectedTerms) {
      const expectedCount = countOf(value, term);
      if (expectedCount && countOf(localized.get(key), term) !== expectedCount) {
        failures.push(`${folder}:${key}: protected term mismatch for ${term}`);
      }
    }
    const plainBase = value.replace(/<[^>]+>/g, "").replace(tokenPattern, "").trim();
    if (plainBase.length >= 18 && /\s/.test(plainBase) && localized.get(key) === value) {
      failures.push(`${folder}:${key}: probable untranslated English fallback`);
    }
  }
}
if (failures.length) {
  console.error(failures.join("\n"));
  process.exit(1);
}
console.log(`Validated ${folders.length} catalogs with ${base.size} identical keys and matching format tokens.`);
