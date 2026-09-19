#!/bin/sh
set -eu

manifest='app/src/main/AndroidManifest.xml'

# 1. Zero declared permissions
if grep -q '<uses-permission' "$manifest"; then
    echo 'ERROR: Manifest contains unexpected <uses-permission> tag!' >&2
    exit 1
fi

# 2. Strict backup policy: no ADB / cloud backup extraction
if ! grep -q 'android:allowBackup="false"' "$manifest"; then
    echo 'ERROR: allowBackup must be set to false!' >&2
    exit 1
fi

# 3. No cleartext traffic permitted
if ! grep -q 'android:usesCleartextTraffic="false"' "$manifest"; then
    echo 'ERROR: usesCleartextTraffic must be false!' >&2
    exit 1
fi

# 4. Only one exported activity (MainActivity launcher)
exported_count=$(grep -c 'android:exported="true"' "$manifest" || true)
if [ "$exported_count" -ne 1 ]; then
    echo "ERROR: Expected exactly 1 exported component (launcher), found: $exported_count" >&2
    exit 1
fi

echo "All Ethic Notes privacy and attack surface verification checks PASSED!"
