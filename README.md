# Pushpyns

Pushpyns is a phone-only place notebook with an optional field-service work-order layer. A **pyn** is a saved place: name, address, coordinates, categories, notes, and photos. **Work orders** are a Pro preview: create a job, walk the customer through terms, capture a signature, and share a PDF.

This is a new Android app. It follows the product behavior of the 2016 Pushpyns app (map, pyns, categories, share, directions, work orders, signature) and leaves the old Java/PHP service behind. Nothing in v1 talks to a server.

## Features in v1

- Local profile: your name, business name, and personal / business / both. No accounts.
- Home map centered on the current location, with a preview map when no Google Maps key is set.
- Create, edit, and delete pyns. Photos from the camera or the gallery. Custom categories.
- List with search and sort by category, name, date, or distance.
- Pyn detail with system share (text) and Get Directions (Google Maps).
- Work orders behind a local Pro flag and a placeholder paywall. Play Billing is not connected.
- Work-order flow: customer, address, notes, optional line items, photos, optional link to a pyn (or save the address as a new pyn) → terms → signature → complete → PDF → Android share sheet.
- Light and dark Material 3 themes, with an optional dynamic-color switch. The default palette stays Pushpyns blue.
- Room database behind repository interfaces so a later sync layer can be added without rewriting screens.

## Run it

Requirements: JDK 21, Android SDK platform 37.2, build-tools 37.0.0 (or the 36.0.0 tools AGP also asks for).

```bash
./gradlew assembleDebug
./gradlew test
```

Install `app/build/outputs/apk/debug/app-debug.apk` on a device or emulator (API 26+).

On first launch, enter your name. The map asks for location. **Load sample places** on the home card adds four public demo spots (Yosemite, the Ferry Building, Sue Bierman Park, Lost Dutchman) so the list and map are not empty.

Work orders stay locked until you open **Work orders** or **Settings → Pro** and choose **Enable Pro preview**. That switch is stored only on the phone. It is not a purchase.

## Google Maps key

The app builds and runs with no key. The map is a coordinate preview until a key is present.

1. Create an Android-restricted Maps SDK key in Google Cloud.
2. Add it to `local.properties` (this file is gitignored):

```
MAPS_API_KEY=your_key_here
sdk.dir=/path/to/android-sdk
```

`secrets.defaults.properties` (checked in) sets `MAPS_API_KEY=MISSING`. The secrets Gradle plugin copies the key into `BuildConfig` and the manifest. `MISSING` and a blank value both show the preview map. Do not commit a real key.

## Tests and style

`./gradlew test` runs JVM unit tests (Robolectric where Android types are required):

- distance, sorting, share text, money, validators
- PDF content (business, customer, line totals, signature block) and the complete-work-order use case
- Room repositories for pyns and work orders
- list and paywall ViewModels
- Compose UI tests for the pyn list and the Pro paywall

`./gradlew ktlintCheck` runs ktlint with the Android Studio style. `.github/workflows/android.yml` builds and tests on push.

## Layout

One `:app` module, packages instead of a multi-module split:

- `domain` — models, repositories (interfaces), distance, money, PDF content, completion use case
- `data` — Room, local repositories, location, photos, `PdfDocument` renderer
- `ui` — Compose screens, ViewModels, navigation
- `di` — Hilt

## Roadmap

- Cloud sync and accounts, using the existing repository interfaces
- Play Billing for the real Pro subscription (about $12/month or $100/year in the old plan)
- Web work-order dashboard
- iOS
- Team assign and crew email, which the 2016 app never shipped

## Not in this version

Server login, password reset, live location breadcrumbs, team invites, and a built-in mail composer. Sharing a pyn or a PDF uses the Android share sheet, so the user can pick email, Messages, or another app.
