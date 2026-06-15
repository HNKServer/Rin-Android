#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

echo "[ew] Building PC/desktop ew server..."
if ! command -v cargo >/dev/null 2>&1; then
  echo "ERROR: cargo was not found. Install Rust first: https://rustup.rs/" >&2
  exit 1
fi

cargo --version

if [ ! -f "webui/index.html" ]; then
  echo "[ew] webui submodule is missing; creating a minimal placeholder webui/index.html for source-zip builds."
  mkdir -p webui
  cat > webui/index.html <<'EOF'
<!doctype html>
<html><head><meta charset="utf-8"><title>ew WebUI placeholder</title></head><body>ew WebUI placeholder</body></html>
EOF
fi

cargo build --release

echo
echo "Build finished."
echo "Output:"
echo "  $PWD/target/release/ew"
echo
echo "Example run:"
echo "  ./target/release/ew --port 8080 --path ./ew_data --asset-path /path/to/sif2_assets"
