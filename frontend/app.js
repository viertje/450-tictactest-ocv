(function () {
  "use strict";

  const config = window.TICTACTEST_CONFIG;
  const byId = (id) => document.getElementById(id);

  async function readText(url) {
    const response = await fetch(url, { cache: "no-store" });
    if (!response.ok) {
      throw new Error("HTTP " + response.status + " for " + url);
    }
    return (await response.text()).trim();
  }

  function showVersion(elementId, url) {
    readText(url)
      .then((version) => { byId(elementId).textContent = version; })
      .catch((error) => {
        byId(elementId).textContent = "nicht verfügbar";
        console.error("Version could not be loaded:", error);
      });
  }

  // The frontend's own version is a file inside the release archive, next to this script.
  showVersion("frontend-version", "version.txt");

  if (!config || !config.consoleUrl) {
    byId("config-error").hidden = false;
    byId("backend-version").textContent = "unbekannt";
    return;
  }

  const frame = byId("terminal");
  const startGame = () => { frame.src = config.consoleUrl; };
  startGame();

  byId("restart").addEventListener("click", () => {
    frame.src = "about:blank";
    requestAnimationFrame(startGame);
  });

  if (config.versionUrl) {
    showVersion("backend-version", config.versionUrl);
  } else {
    byId("backend-version").textContent = "unbekannt";
  }
})();
