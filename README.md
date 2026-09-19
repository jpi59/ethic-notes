# Ethic Notes

> **Private, offline, zero-permission notes application for Android.**

Ethic Notes is designed with one unwavering principle: **your thoughts belong to you alone**. It delivers a fast, responsive note-taking experience with **zero permissions**, **zero network calls**, and **zero tracking**.

---

## Key Highlights

- **0 Permissions Declared:** No `<uses-permission>` tags in `AndroidManifest.xml`. The app cannot connect to the internet, read external storage, or access device sensors.
- **Privacy Shield:** Toggle anti-screenshot protection (`FLAG_SECURE`) to block screen captures and hide note content in Android's recent applications switcher.
- **Storage Access Framework (SAF):** Export notes to `.txt` or full JSON backups, and import existing notes via Android's native document picker without granting disk-wide storage access.
- **Hardened SQLite Engine:** Internal private database using 100% parameterized queries to guarantee immunity against SQL injection.
- **Attack Surface Minimization:** Single entry point (`MainActivity`), `allowBackup="false"` to prevent USB/ADB backup extraction, and `usesCleartextTraffic="false"`.
- **Zero Third-Party SDKs:** Built solely with standard Android platform classes. No analytics, no crash reporters, no ads, no trackers.
- **Reproducible Builds:** Fully compatible with F-Droid verification standards.

---

## Technical Specifications

| Parameter | Specification |
|---|---|
| **Package ID** | `org.jpi59.ethicnotes` |
| **Minimum SDK** | Android 7.0 (API 24) |
| **Target SDK** | Android 16 (API 36) |
| **License** | GPL-3.0 |
| **Permissions** | 0 |
| **Network Access** | None (100% Offline) |

---

## Building from Source

```bash
# Build release APK
./gradlew assembleRelease

# Verify privacy rules
./scripts/verify-privacy.sh
```

---

## License

Licensed under the **GNU General Public License v3.0**. See [LICENSE](LICENSE) and [NOTICE.md](NOTICE.md) for details.
