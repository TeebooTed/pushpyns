# Security

Pushpyns v1 keeps places, photos, signatures, and PDFs on the device. There is
no account server. The sensitive things in this repository are build secrets
and, once someone runs the app, the data on their phone.

## Reporting a vulnerability

Please report security issues privately through
[GitHub security advisories](https://github.com/TeebooTed/pushpyns/security/advisories/new).

Do not open a public GitHub issue for:

- a leaked API key, keystore, or credential
- a way to read another person's pyns, signatures, or PDFs
- a crash that exposes local files to another app

Include the version or commit, the Android version, and steps to reproduce.
You should hear back within a week. If you do not, open a private advisory
again and mention that the first report went unanswered.

## What not to commit

Never commit:

- `local.properties` (it holds `sdk.dir` and, if you add one, `MAPS_API_KEY`)
- `*.keystore`, `*.jks`, or upload certificates
- Google Cloud or Maps keys, Play Console credentials, or OAuth client secrets
- real customer names, signatures, photos, or completed work-order PDFs

`secrets.defaults.properties` is supposed to contain only
`MAPS_API_KEY=MISSING`. That sentinel means "no key" and is safe to commit.
A real key belongs in `local.properties`, which is gitignored.

If a secret lands on `main`, revoke it in the provider console first, then
open a security advisory. Rewriting git history does not un-leak a key that
was already pushed.
