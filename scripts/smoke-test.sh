#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
plugin_dir="$(cd "$script_dir/.." && pwd)"
build_dir="$plugin_dir/build/smoke"

mkdir -p "$build_dir"

if command -v javac >/dev/null 2>&1; then
  mapfile -t core_sources < <(find "$plugin_dir/src/main/java/vn/logcraft/core" -name '*.java' -type f | sort)
  javac -d "$build_dir" "${core_sources[@]}" "$plugin_dir/src/smoke/java/vn/logcraft/core/SmokeTest.java"
else
  java "$script_dir/SmokeCompiler.java" "$plugin_dir" "$build_dir"
fi

java -cp "$build_dir" vn.logcraft.core.SmokeTest
