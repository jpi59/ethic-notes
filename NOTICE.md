# Notice and Provenance — Ethic Notes

## Project Identity
- **Application Name:** Ethic Notes
- **Application ID:** `org.jpi59.ethicnotes`
- **Maintainer:** jpi59
- **License:** GNU General Public License v3.0 (GPL-3.0)

## Upstream Provenance & Context
Ethic Notes is an independent, hardened, zero-permission adaptation inspired by the ethos of the Fossify Notes and Simple Mobile Tools projects:
- Upstream Inspiration: [Fossify Notes](https://github.com/FossifyOrg/Notes) (GPL-3.0)
- Architectural Differentiation:
  1. **Zero Declared Permissions:** Completely removes legacy storage permissions, boot receivers, wake locks, and alarm permissions.
  2. **Storage Access Framework (SAF):** All imports and exports use system-mediated SAF intents (`ACTION_CREATE_DOCUMENT` / `ACTION_OPEN_DOCUMENT`) without granting disk access to the application.
  3. **Attack Surface Reduction:** Single entry point, `allowBackup="false"`, `usesCleartextTraffic="false"`, and parameterized SQL statements preventing SQL injection.
  4. **Privacy Shield:** Integrated `FLAG_SECURE` preventing screen captures and recent tasks visual leaks.
  5. **Original Visual Assets:** All vector icons and launcher drawables are original designs created specifically for Ethic Notes.
