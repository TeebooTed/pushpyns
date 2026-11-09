# Good first issues

These are scoped so a new contributor can finish one without redesigning the
app. Each item is written so it can be pasted into a GitHub issue. Comment on
the issue before you start, and follow [CONTRIBUTING.md](../CONTRIBUTING.md).

## 1. Move snackbar copy into strings.xml

**Body:** English sentences are hardcoded in the pyn and work-order ViewModels, so they cannot be translated.

**Where:** `app/src/main/java/com/pushpyns/ui/pyns/PynViewModels.kt` (`EditorEvent.Message`), `app/src/main/java/com/pushpyns/ui/work/WorkOrderViewModels.kt`, `app/src/main/java/com/pushpyns/ui/home/HomeViewModel.kt`, `app/src/main/res/values/strings.xml`.

**Done when:** Those messages use `R.string` (or a string resource id the UI resolves), and a unit test still compiles.

## 2. Add a time picker next to the work-order date

**Body:** Scheduling a job always stores 9:00 local time. The form should let the technician pick the hour and minute.

**Where:** `localNineAm` and the date button in `app/src/main/java/com/pushpyns/ui/work/WorkOrderScreens.kt`, plus `scheduledAt` on `WorkEditorUiState` in `WorkOrderViewModels.kt`.

**Done when:** Choosing a date and a time round-trips into `WorkOrder.scheduledAt`, and clearing the schedule still works.

## 3. Let distance display miles or kilometers

**Body:** `formatDistance` always prints feet and miles. A setting should switch the pyn list and home chips to kilometers.

**Where:** `formatDistance` in `app/src/main/java/com/pushpyns/domain/geo/Geo.kt`, `SettingsRepository` in `domain/repository/Repositories.kt`, `app/src/main/java/com/pushpyns/ui/settings/SettingsScreen.kt`.

**Done when:** A unit test covers both formats, and the list uses the saved choice.

## 4. Separate empty copy for open and completed work orders

**Body:** The completed tab still explains how to create a job. It should say there are no signed jobs yet.

**Where:** `WorkOrderListScreen` in `app/src/main/java/com/pushpyns/ui/work/WorkOrderScreens.kt` and `strings.xml` (`work_empty_done` is only a title today).

**Done when:** Open and complete empty states use different body text, covered by a Compose test or an obvious string assertion.

## 5. Confirm before leaving a dirty pyn or work-order form

**Body:** Back from the editor throws away unsaved name, notes, and photos with no prompt.

**Where:** `PynEditorRoute` / `WorkOrderEditorRoute` in `PynScreens.kt` and `WorkOrderScreens.kt`. Compare current fields with the loaded snapshot in the matching ViewModels.

**Done when:** A clean form pops immediately. A dirty form asks, and Discard actually leaves.

## 6. TalkBack labels for the map, photo strip, and signature pad

**Body:** Several controls rely on visible text only. The map preview, photo thumbnails, and signature surface need content descriptions and larger touch targets where they are under 48dp.

**Where:** `PlaceholderMap` and `PynMap` in `ui/components`, `PhotoStrip` in `PynScreens.kt`, `SignaturePad` in `SignaturePad.kt`, the home overlay in `HomeScreen.kt`.

**Done when:** Each of those nodes has a `contentDescription` or semantics block, and you note what you checked with TalkBack or Compose semantics.

## 7. IME Next and Done on the pyn and work-order forms

**Body:** Every field is a dead-end for the keyboard. Next should move through the form and the last field should offer Done.

**Where:** `Field` in `PynScreens.kt` and `OrderField` in `WorkOrderScreens.kt`.

**Done when:** Keyboard actions advance focus in order, and the screen still scrolls the focused field into view.

## 8. Highlight the search match on a pyn row

**Body:** Search filters the list but does not show why a row matched (name, address, or category).

**Where:** `Pyn.matchesQuery` in `domain/geo/Geo.kt` and `PynRow` in `PynScreens.kt`.

**Done when:** The matching substring is visually marked, with a unit test for the match span and a Compose test that the row still shows the name.

## 9. Compose previews for the home, list, detail, and paywall screens

**Body:** `@Preview` is missing, so Android Studio cannot show the main screens without launching the app.

**Where:** `HomeScreen`, `PynListScreen`, `PynDetailScreen`, `PaywallScreen`. Sample data already lives in `ui/preview/PreviewData.kt`.

**Done when:** Each of those four composables has a light preview that builds.

## 10. Capture real README screenshots

**Body:** `README.md` has a placeholder table instead of pictures. An emulator or a device run should produce PNGs of home, pyn list, pyn detail, new pyn, work order, signature, and the signed PDF state.

**Where:** Replace the placeholder table in `README.md`. Put images in `docs/images/` (not a fake render). A Robolectric capture was tried and came out black, so do this on an emulator.

**Done when:** The README shows those screens and the images are real captures.

## 11. Test PDF pagination when a job has many lines and photos

**Body:** The composer is tested for one short order. A long line-item list plus missing photo files should still produce a valid multi-page document, or a clear failure.

**Where:** `WorkOrderPdfComposer.kt`, `AndroidWorkOrderPdfWriter.kt`, and `WorkOrderPdfAndCompleteTest.kt`. `PdfDocument` needs a device or an instrumentation test; Robolectric's `PdfDocument` has no native document in CI today.

**Done when:** A test feeds enough line items to cross a page and asserts the writer output is a `%PDF` file larger than a one-page order, or documents why that assertion has to be an instrumented test.

## 12. Remember the last pyn sort

**Body:** My Pyns always reopens on Date. The last sort (and whether distance was unavailable) should stick.

**Where:** `PynListViewModel` and `DataStoreSettingsRepository` (`data/repository/LocalAccountRepositories.kt`).

**Done when:** Killing and reopening the app restores the sort, with a ViewModel or repository test.

## 13. Tablet list and detail side by side

**Body:** Width over about 840dp still uses the phone stack. The pyn list and the open pyn should sit next to each other.

**Where:** `PushpynsNavHost.kt` and `PynListScreen` / `PynDetailScreen`. The drawer already switches gestures; this is the content pane.

**Done when:** A wide window shows both panes, and a phone-width window does not.

## 14. Document how a sync implementation would wrap the repositories

**Body:** The interfaces are ready for a later cloud sync, but a new contributor cannot see the intended boundary.

**Where:** Add `docs/SYNC.md` describing `PynRepository`, `WorkOrderRepository`, `ProfileRepository`, and `ProAccess` in `domain/repository/Repositories.kt`. Do not add a backend.

**Done when:** The note says what stays on the phone, what a sync worker would push, and which ids are local Room ids today.

## 15. Expand the Compose test past the happy list

**Body:** `ComposeScreenTest` covers a populated list and the paywall button. The empty search state and the Pro-already-on paywall are untested.

**Where:** `app/src/test/java/com/pushpyns/ui/ComposeScreenTest.kt`, `PynListScreen`, `PaywallScreen`.

**Done when:** New tests assert the empty-search string and that "Turn off Pro preview" calls the disable callback.
