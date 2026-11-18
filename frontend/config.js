// Per-environment settings, loaded before app.js. Nothing environment-specific
// belongs in app.js or index.html.
//
//   local : these defaults (web console and version server on this computer)
//   cloud : the infrastructure (OpenTofu) writes its own config.js into the
//           bucket; the frontend deployment must never overwrite it.
window.TICTACTEST_CONFIG = {
  consoleUrl: "http://localhost:7681",
  versionUrl: "http://localhost:8081/version"
};
