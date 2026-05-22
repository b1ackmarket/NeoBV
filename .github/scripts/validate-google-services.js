const fs = require("fs");
const path = require("path");

const googleServicesPath = path.join(process.cwd(), "app", "google-services.json");
const expectedProjectId = "neo-bv";
const expectedPackageNames = [
  "dev.b1ackmarket.bv",
  "dev.b1ackmarket.bv.debug",
  "dev.b1ackmarket.bv.r8test",
];

function fail(message) {
  console.error(`google-services.json validation failed: ${message}`);
  process.exit(1);
}

if (!fs.existsSync(googleServicesPath)) {
  console.log("No google-services.json found; Firebase integrations will be disabled for this build.");
  process.exit(0);
}

let config;
try {
  config = JSON.parse(fs.readFileSync(googleServicesPath, "utf8"));
} catch (error) {
  fail(`invalid JSON (${error.message})`);
}

const projectId = config?.project_info?.project_id;
if (projectId !== expectedProjectId) {
  fail(`expected project_id "${expectedProjectId}", got "${projectId || "<missing>"}"`);
}

const packageNames = new Set(
  (config?.client || [])
    .map((client) => client?.client_info?.android_client_info?.package_name)
    .filter(Boolean),
);

const missingPackageNames = expectedPackageNames.filter((packageName) => !packageNames.has(packageName));
if (missingPackageNames.length > 0) {
  fail(`missing Android clients: ${missingPackageNames.join(", ")}`);
}

console.log(
  `google-services.json validated for project "${expectedProjectId}" and packages: ${expectedPackageNames.join(", ")}`,
);
