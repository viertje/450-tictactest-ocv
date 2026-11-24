#!/usr/bin/env bash
# Decides which parts of TicTacTest changed between the previous release tag and the given one.
#
# Usage:  scripts/detect-changes.sh v1.2.0     (needs full history and all tags: fetch-depth: 0)
# Output: key=value lines, ready to be appended to $GITHUB_OUTPUT:
#           previous=<tag|none>  backend=true|false  frontend=true|false
#
# Why compare against the previous RELEASE TAG and not the last commit: several commits can
# lie between two releases, and a release must ship everything that changed since the last
# one that was actually deployed. The last commit alone would miss earlier changes.
#
# Path rules (which files belong to which part):
#   backend   src/, build.gradle, settings.gradle, gradle.properties, gradle/, gradlew
#   frontend  frontend/
#   both      .github/workflows/release.yml and deploy.yml (they change HOW things are
#             deployed; redeploying everything is the only way to prove they still work)
#   neither   docs, CHANGELOG, tests-only, other scripts, README, ...
set -euo pipefail

TAG="${1:?usage: detect-changes.sh <tag>}"

# Start from the parent commit so the tag being released can never match itself.
PREVIOUS=$(git describe --tags --abbrev=0 --match 'v[0-9]*.[0-9]*.[0-9]*' "$TAG^" 2>/dev/null || true)

if [ -z "$PREVIOUS" ]; then
    # First release ever: nothing to compare with, so everything is new.
    echo "previous=none"
    echo "backend=true"
    echo "frontend=true"
    exit 0
fi

backend=false
frontend=false
while IFS= read -r file; do
    case "$file" in
        src/* | build.gradle | settings.gradle | gradle.properties | gradlew | gradle/*)
            backend=true ;;
        frontend/*)
            frontend=true ;;
        .github/workflows/release.yml | .github/workflows/deploy.yml)
            backend=true
            frontend=true ;;
    esac
done < <(git diff --name-only "$PREVIOUS" "$TAG")

echo "previous=$PREVIOUS"
echo "backend=$backend"
echo "frontend=$frontend"
