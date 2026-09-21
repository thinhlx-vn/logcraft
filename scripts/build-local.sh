#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
plugin_dir="$(cd "$script_dir/.." && pwd)"

"$script_dir/smoke-test.sh"
distribution_path="$(java "$script_dir/BuildPlugin.java" "$plugin_dir" | tail -n 1)"

jar_name="logcraft-$(sed -n 's/^pluginVersion=//p' "$plugin_dir/gradle.properties").jar"
jar_path="$plugin_dir/build/local/$jar_name"
bytecode="$(java --add-modules jdk.jdeps com.sun.tools.javap.Main \
  -classpath "$jar_path" -c -p vn.logcraft.ui.LogCraftToolWindowFactory)"

if ! rg -q 'invokeinterface.*ContentFactory.createContent' <<<"$bytecode"; then
  echo "Compatibility check failed: ContentFactory.createContent is not invoked as an interface" >&2
  exit 1
fi

echo "IntelliJ ContentFactory bytecode compatibility: PASS"
echo "$distribution_path"
