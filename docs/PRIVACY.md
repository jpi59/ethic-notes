# Privacy Policy — Ethic Notes

Ethic Notes collects **no personal information, no telemetry, and no diagnostic logs**.

## Core Privacy Architecture

1. **Zero Permissions:** The application requests 0 permissions from Android. It has no capability to connect to the internet, read personal contacts, inspect device identifiers, or determine location.
2. **Local Storage Only:** Notes are stored exclusively in the app-private internal database protected by Android's application sandbox (UID isolation).
3. **No Cloud Sync:** There are no remote servers, accounts, or third-party cloud services.
4. **User-Controlled Export / Import:** Document exchange occurs exclusively through the system Storage Access Framework (SAF) when explicitly initiated by the user.
5. **Screen & Recents Protection:** An optional Privacy Shield activates Android's `FLAG_SECURE`, preventing operating system window previews in the task switcher and blocking unauthorized screenshots.
