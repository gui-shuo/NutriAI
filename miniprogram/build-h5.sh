#!/bin/bash
# Build H5 + inject PWA support
# Usage: ./build-h5.sh

set -e
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

echo "=== Building H5 version ==="
npm run build:h5

H5_DIR="dist/build/h5"

echo "=== Injecting PWA assets ==="
cp h5-pwa/manifest.webmanifest "$H5_DIR/"
cp h5-pwa/sw.js "$H5_DIR/"

# Inject PWA meta tags into index.html
sed -i 's|</head>|<link rel="manifest" href="/h5/manifest.webmanifest"><meta name="theme-color" content="#4CAF50"><meta name="apple-mobile-web-app-capable" content="yes"><meta name="apple-mobile-web-app-status-bar-style" content="black-translucent"><meta name="apple-mobile-web-app-title" content="NutriAI"><link rel="apple-touch-icon" href="/h5/static/icons/icon-192.png"><meta name="description" content="AI驱动的智能健康饮食平台"></head>|' "$H5_DIR/index.html"

# Inject service worker registration
sed -i 's|</body>|<script>if("serviceWorker"in navigator){window.addEventListener("load",function(){navigator.serviceWorker.register("/h5/sw.js").catch(function(){})})}</script></body>|' "$H5_DIR/index.html"

echo "=== H5 + PWA build complete ==="
echo "Output: $H5_DIR"
echo ""
echo "Deploy to nginx:"
echo "  docker cp $H5_DIR/. nutriai-frontend:/usr/share/nginx/h5/"
echo "  docker exec nutriai-frontend nginx -s reload"
