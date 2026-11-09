# Contributing to Pushpyns

Pushpyns is a local-first Android app: a notebook of places ("pyns") and a
work-order flow that ends in a signed PDF. Contributions that make that loop
clearer, more reliable, or easier to translate are the ones we want first.

Please read the [Code of Conduct](CODE_OF_CONDUCT.md) before you start.

## Setup

1. Install JDK 21 and Android Studio (current stable).
2. Clone the repo and open the root in Android Studio. Let it use the Gradle
   wrapper (`./gradlew`).
3. The app builds with no Maps key. To see a live map, create
   `local.properties` (gitignored) and add:

   ```
   MAPS_API_KEY=your_key_here
   sdk.dir=/path/to/Android/sdk
   ```

4. Run the debug variant on a device or emulator, API 26 or newer.

Check the project before you open a pull request:

```bash
./gradlew assembleDebug
./gradlew test
./gradlew ktlintCheck
```

`ktlintFormat` can fix most style nits. The Android Studio code style is what
ktlint enforces (see `.editorconfig`).

## Branch and pull request flow

1. Open an issue, or comment on one in
   [docs/GOOD_FIRST_ISSUES.md](docs/GOOD_FIRST_ISSUES.md), so two people do not
   grab the same task.
2. Fork the repo if you do not have push access.
3. Branch from `main`. A short name is enough: `fix-distance-banner`,
   `i18n-snackbars`.
4. Keep the change focused. A pull request that also redesigns navigation is
   two pull requests.
5. Open the PR against `main`. Fill in the template. Link the issue with
   `Fixes #123` when the PR fully addresses it.
6. GitHub Actions runs `assembleDebug`, `test`, and `ktlintCheck`. A red check
   needs a fix or an explanation.

Maintainers squash or rebase as needed so `main` stays readable. Do not force
push a shared branch.

## Tests

New behavior needs a test when one can live in `app/src/test`:

- Pure logic (money, distance, sorting, PDF content, validation) is a plain
  JUnit test.
- Room repositories use Robolectric and an in-memory database. See
  `app/src/test/java/com/pushpyns/data/RepositoryAndPdfTest.kt`.
- ViewModels take fake repositories. See `ViewModelTest.kt` and `Fakes.kt`.
- A screen change that is easy to assert (a label, a button callback) gets a
  Compose test next to `ComposeScreenTest.kt`.

If a change is visual only, say so in the PR and describe what you tapped.

## Code style

- Kotlin, the packages that already exist: `domain`, `data`, `ui`, `di`.
- ViewModels talk to repository interfaces in
  `app/src/main/java/com/pushpyns/domain/repository/Repositories.kt`, not to
  DAOs.
- User-visible copy belongs in `app/src/main/res/values/strings.xml` when you
  touch that screen. Several snackbars are still hardcoded; moving them is a
  welcome change, not a drive-by requirement in an unrelated PR.
- Do not add a new library for something the project already does (images are
  Coil, DI is Hilt, persistence is Room).

## Commit messages

Imperative, specific, and short:

```
Show kilometers when the distance unit is metric

Settings stored the choice but the pyn list still formatted miles.
```

The subject says what changed. The body says why, if the subject is not
enough. Do not commit generated `build/` output, keystores, or
`local.properties`.

## What we will not take yet

A cloud backend, Play Billing charges, or an iOS app as a surprise PR. Those
are on the roadmap and need a design note in an issue first. A small interface
cleanup that makes one of them easier later is welcome.
