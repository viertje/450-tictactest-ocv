# gh-pages

This branch is generated. Do not edit the CSV files by hand — the
"Build & Test" workflow (`.github/workflows/build.yml`) appends one row to
`branch-coverage-history.csv` after every push to `main`, then pushes the update here itself.

`index.html` renders it as a chart via GitHub Pages.

`coverage-history.csv` is the older line-coverage history (until 2026-10-22). It is kept
for reference and no longer updated or read.
