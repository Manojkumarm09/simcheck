const API_BASE = "http://localhost:8080/api";
const COMPARE_URL = `${API_BASE}/compare`;
const GAUGE_CIRCUMFERENCE = 2 * Math.PI * 60; // r=60
const HEATMAP_BUCKETS = 20;

/* ---------- Theme toggle ---------- */
const root = document.documentElement;
const themeToggle = document.getElementById("themeToggle");
const savedTheme = localStorage.getItem("simcheck-theme") || "dark";
root.setAttribute("data-theme", savedTheme);

themeToggle.addEventListener("click", () => {
  const current = root.getAttribute("data-theme");
  const next = current === "dark" ? "light" : "dark";
  root.setAttribute("data-theme", next);
  localStorage.setItem("simcheck-theme", next);
});

/* ---------- File upload (picker + drag & drop) ---------- */
function wireFileInput(fileInputId, textareaId, dropZoneId) {
  const fileInput = document.getElementById(fileInputId);
  const textarea = document.getElementById(textareaId);
  const dropZone = document.getElementById(dropZoneId);

  fileInput.addEventListener("change", () => {
    if (fileInput.files.length) readFileInto(fileInput.files[0], textarea);
  });

  ["dragenter", "dragover"].forEach(evt =>
    dropZone.addEventListener(evt, e => { e.preventDefault(); dropZone.classList.add("drag-over"); })
  );
  ["dragleave", "drop"].forEach(evt =>
    dropZone.addEventListener(evt, e => { e.preventDefault(); dropZone.classList.remove("drag-over"); })
  );
  dropZone.addEventListener("drop", e => {
    const file = e.dataTransfer.files[0];
    if (file) readFileInto(file, textarea);
  });
}

function readFileInto(file, textarea) {
  const reader = new FileReader();
  reader.onload = () => { textarea.value = reader.result; };
  reader.onerror = () => alert("Couldn't read that file, try pasting the content instead.");
  reader.readAsText(file);
}

wireFileInput("file1", "doc1", "dropZone1");
wireFileInput("file2", "doc2", "dropZone2");

/* ---------- Compare action ---------- */
document.getElementById("compareBtn").addEventListener("click", async () => {
  const doc1 = document.getElementById("doc1").value;
  const doc2 = document.getElementById("doc2").value;
  const mode = document.querySelector('input[name="mode"]:checked').value;

  if (!doc1.trim() || !doc2.trim()) {
    alert("Please paste content into both panels.");
    return;
  }

  const btn = document.getElementById("compareBtn");
  const scanLine = document.getElementById("scanLine");
  btn.textContent = "Comparing...";
  btn.disabled = true;
  scanLine.classList.add("active");

  try {
    const response = await fetch(COMPARE_URL, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ doc1, doc2, mode, kgramSize: 5 })
    });
    const data = await response.json();
    renderResults(data);
  } catch (err) {
    alert("Could not reach the backend. Is it running on localhost:8080?");
    console.error(err);
  } finally {
    btn.textContent = "Compare";
    btn.disabled = false;
    scanLine.classList.remove("active");
  }
});

/* ---------- Tiering — single source of truth used everywhere ---------- */
function scoreTier(score) {
  if (score < 30) return "low";
  if (score < 60) return "mid";
  return "high";
}
function tierColorVar(tier) {
  return tier === "low" ? "--accent-green" : tier === "mid" ? "--accent-amber" : "--accent-red";
}
function tierVerdict(tier) {
  return tier === "low" ? "Likely original" : tier === "mid" ? "Partial overlap" : "High similarity";
}
function tierRiskLabel(tier) {
  return tier === "low" ? "LOW" : tier === "mid" ? "MEDIUM" : "HIGH";
}

/* ---------- Render pipeline ---------- */
function renderResults(data) {
  const resultsEl = document.getElementById("results");
  resultsEl.classList.remove("hidden");

  const tier = scoreTier(data.similarityScore);

  renderGauge(data.similarityScore, tier);
  renderDashboard(data, tier);
  renderSummary(data, tier);
  renderHighlightsList(data);
  renderHeatmap(data);

  document.getElementById("highlight1").innerHTML =
    highlightTokens(data.doc1Tokens, data.matchedSegments, "doc1");
  document.getElementById("highlight2").innerHTML =
    highlightTokens(data.doc2Tokens, data.matchedSegments, "doc2");

  resultsEl.scrollIntoView({ behavior: "smooth", block: "nearest" });
}

function renderGauge(score, tier) {
  const fill = document.getElementById("gaugeFill");
  const offset = GAUGE_CIRCUMFERENCE * (1 - Math.min(score, 100) / 100);

  fill.style.transition = "none";
  fill.getBoundingClientRect(); // force reflow so the animation restarts cleanly
  fill.style.transition = "";
  fill.style.strokeDashoffset = offset;

  const colorVar = getComputedStyle(document.documentElement).getPropertyValue(tierColorVar(tier)).trim();
  fill.style.stroke = colorVar;

  document.getElementById("scoreValue").textContent = score.toFixed(1) + "%";
  document.getElementById("verdictLabel").textContent = tierVerdict(tier);
}

// Every value here is read directly from the API response — nothing invented.
function renderDashboard(data, tier) {
  const riskEl = document.getElementById("riskLevel");
  riskEl.textContent = tierRiskLabel(tier);
  riskEl.className = "risk-" + tier;

  document.getElementById("doc1MatchCard").textContent = data.doc1Coverage.toFixed(1) + "%";
  document.getElementById("doc2MatchCard").textContent = data.doc2Coverage.toFixed(1) + "%";

  const modificationScore = Math.max(0, 100 - data.similarityScore);
  document.getElementById("modificationScore").textContent = modificationScore.toFixed(1) + "%";
}

// Plain-language summary built from real numbers, not a templated forensic claim.
function renderSummary(data, tier) {
  const lead = tier === "high"
    ? `These documents are highly similar (${data.similarityScore.toFixed(1)}%). `
    : tier === "mid"
    ? `These documents show partial overlap (${data.similarityScore.toFixed(1)}%). `
    : `These documents are largely original relative to each other (${data.similarityScore.toFixed(1)}%). `;

  const detail = `Doc 1 overlaps ${data.doc1Coverage.toFixed(1)}% with Doc 2, and Doc 2 overlaps `
    + `${data.doc2Coverage.toFixed(1)}% with Doc 1, based on ${data.matchedKgrams} matched k-gram fingerprints.`;

  document.getElementById("analysisText").textContent = lead + detail;
}

// Real facts derived from the actual matched segments, not a canned list.
function renderHighlightsList(data) {
  const validSegments = (data.matchedSegments || []).filter(s => s.doc1Start >= 0 && s.doc2Start >= 0);
  const listEl = document.getElementById("changesList");

  if (validSegments.length === 0) {
    listEl.innerHTML = "<li>No matching blocks found between these documents.</li>";
    return;
  }

  const longest = Math.max(...validSegments.map(s => s.doc1End - s.doc1Start));
  const items = [
    `${validSegments.length} distinct matching block${validSegments.length === 1 ? "" : "s"} found`,
    `Largest matching block spans ${longest} tokens`,
    `${data.matchedKgrams} k-gram fingerprints matched (k-gram size: 5 tokens)`
  ];
  listEl.innerHTML = items.map(i => `<li>${escapeHtml(i)}</li>`).join("");
}

// Real per-segment density, computed from the actual matched ranges in Doc 1 —
// not the same global score repeated across every block.
function renderHeatmap(data) {
  const heatmapEl = document.getElementById("heatmap");
  heatmapEl.innerHTML = "";

  const totalTokens = (data.doc1Tokens || []).length;
  if (totalTokens === 0) return;

  const ranges = (data.matchedSegments || [])
    .filter(s => s.doc1Start >= 0)
    .map(s => [s.doc1Start, s.doc1End]);

  const bucketSize = totalTokens / HEATMAP_BUCKETS;

  for (let b = 0; b < HEATMAP_BUCKETS; b++) {
    const bucketStart = b * bucketSize;
    const bucketEnd = (b + 1) * bucketSize;

    let covered = 0;
    for (const [rs, re] of ranges) {
      const overlapStart = Math.max(bucketStart, rs);
      const overlapEnd = Math.min(bucketEnd, re);
      if (overlapEnd > overlapStart) covered += (overlapEnd - overlapStart);
    }
    const density = bucketSize > 0 ? (covered / bucketSize) * 100 : 0;

    const block = document.createElement("div");
    block.classList.add("heat-block");
    if (density > 0) block.classList.add(scoreTier(density));
    block.title = density.toFixed(0) + "% matched";
    heatmapEl.appendChild(block);
  }
}

function highlightTokens(tokens, segments, docKey) {
  const ranges = segments
    .filter(s => (docKey === "doc1" ? s.doc1Start : s.doc2Start) >= 0)
    .map(s => docKey === "doc1" ? [s.doc1Start, s.doc1End] : [s.doc2Start, s.doc2End]);

  let html = "";
  let i = 0;
  while (i < tokens.length) {
    const range = ranges.find(r => i >= r[0] && i < r[1]);
    if (range) {
      const chunk = tokens.slice(range[0], range[1]).join(" ");
      html += `<mark>${escapeHtml(chunk)}</mark> `;
      i = range[1];
    } else {
      html += escapeHtml(tokens[i]) + " ";
      i++;
    }
  }
  return html;
}

function escapeHtml(str) {
  return str.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
}

/* ---------- Cosmetic only: cursor-tracking spotlight on .spotlight cards ----------
   Purely visual, does not read or affect any comparison data or logic. */
document.querySelectorAll(".spotlight").forEach(card => {
  card.addEventListener("mousemove", e => {
    const rect = card.getBoundingClientRect();
    card.style.setProperty("--spot-x", `${e.clientX - rect.left}px`);
    card.style.setProperty("--spot-y", `${e.clientY - rect.top}px`);
  });
});
