# SimCheck — Code & Text Similarity Detector

A full-stack plagiarism and similarity detection tool that compares two pieces of source code or text and returns a similarity score with highlighted matching regions, built on real algorithmic foundations rather than external libraries.

**Built by:** Manoj Kumar M  
**Stack:** Java 17 · Spring Boot 3 · Plain HTML/CSS/JS  
**Role:** TAP-JOB-ID-2537 · Software Development Intern

---

## Live Demo

Open `simcheck-frontend/index.html` directly in any browser.  
Backend runs at `http://localhost:8080` — start with `mvn spring-boot:run`.

---

## How It Works

### 1. Tokenization
Both documents are cleaned and split into tokens. In **Code mode**, comments (`//`, `/* */`, `#`) are stripped first, then every identifier (variable, method, class name) is replaced with a positional placeholder (`id1`, `id2`, ...) assigned in first-seen order, while language keywords (`public`, `int`, `return`, etc.) are left unchanged. This means two structurally identical methods match correctly even if every variable was renamed — the most common way plagiarized code is disguised.

In **Text mode**, raw word tokens are compared directly, since exact wording matters for prose.

### 2. Rabin-Karp Rolling Hash
Each document is broken into overlapping **k-grams** (sliding windows of `k=5` tokens). A polynomial rolling hash fingerprints each k-gram:

```
hash = (t1·B^(k-1) + t2·B^(k-2) + ... + tk) mod M
```

Rolling forward shifts in O(1) per step:

```
newHash = ((oldHash − t_first·B^(k-1)) · B + t_new) mod M
```

This makes indexing a document of `n` tokens **O(n)** instead of O(n·k). Hash collisions are ruled out with a byte-level token equality check before accepting any match.

### 3. Shared K-gram Counting
The two fingerprint indexes are intersected to find shared k-gram patterns. The count is bounded by the smaller document's unique k-gram count, which guarantees the similarity score **cannot exceed 100%** — a mathematical property, not a clamping hack.

### 4. Interval Merging
Matched k-gram positions are collected as token ranges and merged using classic **merge-intervals** (sort by start, O(n log n) + O(n) merge pass) to produce clean, contiguous highlighted blocks instead of scattered per-token fragments.

### 5. Similarity Scoring
A **Dice coefficient** over shared k-gram fingerprints:

```
similarity = (2 × shared_kgrams) / (total_kgrams_doc1 + total_kgrams_doc2) × 100
```

Coverage per document is reported separately (what fraction of that document's tokens fall inside a matched block), giving a richer picture than a single number.

---

## API

**POST** `/api/compare`

```json
Request:
{
  "doc1": "string",
  "doc2": "string",
  "mode": "TEXT" | "CODE",
  "kgramSize": 5
}

Response:
{
  "similarityScore": 92.7,
  "doc1Coverage": 93.3,
  "doc2Coverage": 93.3,
  "matchedKgrams": 38,
  "totalKgramsDoc1": 41,
  "totalKgramsDoc2": 41,
  "matchedSegments": [...],
  "doc1Tokens": [...],
  "doc2Tokens": [...]
}
```

**GET** `/api/health` — returns `"SimCheck backend is running"`

---

## Running Locally

**Backend:**
```bash
cd simcheck-backend
mvn spring-boot:run
```
Starts on `http://localhost:8080`

**Frontend:**  
Open `simcheck-frontend/index.html` directly in your browser. No build step needed.

---

## Project Structure

```
simcheck-backend/
├── controller/
│   └── CompareController.java        # REST endpoint
├── service/
│   ├── SimilarityService.java        # Orchestration pipeline
│   ├── RabinKarpMatcher.java         # Rolling hash + shared k-gram counting
│   └── TokenizerService.java         # Tokenization + identifier normalization
├── util/
│   └── IntervalMerger.java           # Merge-intervals for highlight blocks
└── model/
    ├── CompareRequest.java
    ├── CompareResponse.java
    └── MatchedSegment.java

simcheck-frontend/
├── index.html                        # UI layout
├── style.css                         # Dark/light theme, glassmorphism
└── script.js                         # Fetch, gauge animation, heatmap rendering
```

---

## Key Design Decisions

**Why rolling hash over recomputing each window?**  
Recomputing costs O(k) per shift. Rolling reduces this to O(1) — O(n) total per document regardless of k-gram size.

**Why k-grams over single tokens?**  
Single-token matching is too fragile — one inserted word breaks every subsequent match. K-grams are resilient to small local changes while still catching structural clones. This is the same fundamental technique used by MOSS (Stanford's plagiarism detector).

**Why identifier normalization in code mode?**  
Without it, renaming every variable from `a` to `x` evades detection entirely. Normalization maps each identifier to a positional placeholder in first-seen order, so two renamed-but-structurally-identical methods produce the same normalized token stream.

**Why Dice coefficient over Jaccard?**  
Dice gives more weight to shared elements relative to the union, which is more appropriate for asymmetric document pairs (a short plagiarized excerpt vs a long original document).

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2 |
| Build | Maven |
| Frontend | Plain HTML5 / CSS3 / JavaScript |
| Fonts | Space Grotesk · Inter · JetBrains Mono (Google Fonts) |

---

## Future Scope

- **Batch comparison** — upload multiple files and generate an all-pairs similarity matrix (how real academic plagiarism checkers like MOSS operate)
- **Winnowing** — select only local-minimum hashes within each sliding window to reduce index size while guaranteeing detection of any match above a threshold length
- **Persistent comparison history** — H2/MySQL backend storage for saved comparisons with timestamps and a browsable history panel
- **PDF/DOCX support** — extract plain text from binary document formats before tokenization
- **FAQ chatbot** — an in-app assistant (powered by Claude or GPT) that answers questions about how the algorithm works, explains what a given similarity score means, and guides users through interpreting results
- **Authorship fingerprinting** — style-based similarity metrics (naming conventions, indentation patterns, comment density) as a secondary signal alongside structural matching

---

## Author

**Manoj Kumar M**  
B.E. Information Science & Engineering · APS College of Engineering, Bangalore  
Java Full Stack Intern · Tap Academy  
GitHub: [github.com/Manojkumarm09](https://github.com/Manojkumarm09)  
Portfolio: [manoj-portfolio-nu.vercel.app](https://manoj-portfolio-nu.vercel.app)
