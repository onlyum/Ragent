import { existsSync } from "node:fs";
import { createRequire } from "node:module";
import { dirname, resolve } from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";

const require = createRequire(import.meta.url);
const scriptDir = dirname(fileURLToPath(import.meta.url));

async function readStdin() {
  const chunks = [];
  for await (const chunk of process.stdin) {
    chunks.push(typeof chunk === "string" ? Buffer.from(chunk) : chunk);
  }
  return Buffer.concat(chunks).toString("utf8");
}

function normalizeChunkStrategy(value) {
  return value === "auto" ? "auto" : "regex";
}

function normalizePackageDir(input) {
  const direct = resolve(input);
  if (existsSync(resolve(direct, "package.json"))) {
    return direct;
  }
  const nested = resolve(direct, "@tobilu", "qmd");
  if (existsSync(resolve(nested, "package.json"))) {
    return nested;
  }
  return direct;
}

function resolvePackageRoot() {
  if (process.env.QMD_PACKAGE_DIR) {
    return normalizePackageDir(process.env.QMD_PACKAGE_DIR);
  }

  const localInstall = resolve(scriptDir, "node_modules", "@tobilu", "qmd");
  if (existsSync(resolve(localInstall, "package.json"))) {
    return localInstall;
  }

  try {
    const entryPath = require.resolve("@tobilu/qmd");
    return resolve(dirname(entryPath), "..");
  } catch {
    try {
      const packageJson = require.resolve("@tobilu/qmd/package.json");
      return dirname(packageJson);
    } catch {
      throw new Error(
        "无法解析 @tobilu/qmd，请先执行 `cd scripts/qmd && npm install`，或在后端配置 QMD_PACKAGE_DIR。"
      );
    }
  }
}

async function loadChunkDocumentAsync() {
  const packageRoot = resolvePackageRoot();
  // QMD 当前公开 exports 未直接暴露 chunk API，这里通过包内 dist/store.js 做薄适配。
  const moduleUrl = pathToFileURL(resolve(packageRoot, "dist", "store.js")).href;
  const storeModule = await import(moduleUrl);
  if (typeof storeModule.chunkDocumentAsync !== "function") {
    throw new Error("未在 @tobilu/qmd/dist/store.js 中找到 chunkDocumentAsync");
  }
  return storeModule.chunkDocumentAsync;
}

function normalizeChunkItem(item, index) {
  const text = typeof item === "string"
    ? item
    : typeof item?.text === "string"
      ? item.text
      : typeof item?.content === "string"
        ? item.content
        : "";

  if (!text || text.trim().length === 0) {
    return null;
  }

  const position = Number.isFinite(item?.position) ? item.position : index;
  return {
    index,
    text,
    position
  };
}

async function main() {
  const payload = await readStdin();
  if (!payload || payload.trim().length === 0) {
    throw new Error("未接收到 QMD 切分请求");
  }

  const request = JSON.parse(payload);
  if (typeof request.text !== "string" || request.text.trim().length === 0) {
    throw new Error("请求缺少有效的 text 字段");
  }

  const chunkDocumentAsync = await loadChunkDocumentAsync();
  const rawChunks = await chunkDocumentAsync(
    request.text,
    Number.isFinite(request.maxChars) ? request.maxChars : undefined,
    Number.isFinite(request.overlapChars) ? request.overlapChars : undefined,
    Number.isFinite(request.windowChars) ? request.windowChars : undefined,
    request.fileName ?? undefined,
    normalizeChunkStrategy(request.chunkStrategy)
  );

  const chunks = Array.isArray(rawChunks)
    ? rawChunks
        .map((item, index) => normalizeChunkItem(item, index))
        .filter(Boolean)
    : [];

  process.stdout.write(JSON.stringify({ engine: "qmd", chunks }));
}

main().catch((error) => {
  process.stderr.write(`${error instanceof Error ? error.stack || error.message : String(error)}\n`);
  process.exitCode = 1;
});
