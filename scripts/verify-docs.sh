#!/usr/bin/env bash

set -euo pipefail

readonly EN_DOCS_DIR="docs/i18n/en"
readonly ZH_DOCS_DIR="docs/i18n/zh"

if [[ ! -d "$EN_DOCS_DIR" || ! -d "$ZH_DOCS_DIR" ]]; then
    echo "Expected both $EN_DOCS_DIR and $ZH_DOCS_DIR to exist." >&2
    exit 1
fi

temp_dir="$(mktemp -d)"
trap 'rm -rf "$temp_dir"' EXIT

find "$EN_DOCS_DIR" -type f -name '*.md' | sed "s#^$EN_DOCS_DIR/##" | sort > "$temp_dir/en-docs"
find "$ZH_DOCS_DIR" -type f -name '*.md' | sed "s#^$ZH_DOCS_DIR/##" | sort > "$temp_dir/zh-docs"

if ! diff -u "$temp_dir/en-docs" "$temp_dir/zh-docs"; then
    echo "English and Chinese documentation paths must stay aligned." >&2
    exit 1
fi

failures=0
while IFS= read -r document; do
    while IFS= read -r link; do
        [[ -z "$link" || "$link" == \#* || "$link" =~ ^[a-zA-Z][a-zA-Z0-9+.-]*: || "$link" == //* ]] && continue

        target="${link%%#*}"
        [[ -z "$target" ]] && continue

        path="$(dirname "$document")/$target"
        if [[ ! -e "$path" ]]; then
            echo "Missing local Markdown target: $document -> $link" >&2
            failures=1
        fi
    done < <(perl -ne 'while (/\]\(([^ )]+)(?:\s+"[^"]*")?\)/g) { print "$1\n" }' "$document")
done < <(find docs -type f -name '*.md' -print; find . -maxdepth 1 -type f -name 'README*.md' -print)

exit "$failures"
