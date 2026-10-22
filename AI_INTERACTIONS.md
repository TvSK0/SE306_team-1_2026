# AI interaction log

Per the team's shared requirement to log AI interactions and mark affected
code with `// AI-assisted: [description]`. Core UI and business logic is
written by hand; entries below are the exception, each noted as such.

Add an entry per session/PR, newest first.

---

## 2026-10-23 - Aum, Claude Code - screens off mainframe.client.Status, and the last methods over 10

**Prompt (summarised):** stop the ui files importing `mainframe.client.Status`, then bring the three
methods still over complexity 10 under the target.

**What it assisted with:**

- New `lavs.logic.MainframeReply`: what a screen needs from a mainframe answer (queued and its
  message, network failure, the mainframe's reason, or not asked). Every result type the screens
  read gains `getReply()`. `BorrowersText`, `LoanSaveMessage`, `LoanSubmission`, `RelationshipsText`,
  `RepaymentsText`, `ResultText`, `EmployersPanel` and the loan, borrower and relationship
  controllers use it instead of `Status`. `LayerRulesTests` now forbids all of `mainframe.client`
  in ui (except `LavsApp`, which builds the Connection), and `ui/package-info.java` says so.
- Refactors, same behaviour, existing tests unchanged: `QueuedWrites.send` 16 -> 3,
  `CustomerEligibilityService.check` 15 -> 7, `PendingRecordReads.apply` 12 -> 4, each split into
  small helpers. These are Gabriel's classes, refactored at the team's request.

**Validation:** `./mvnw clean verify` passes (843 tests, coverage gate met, logic 98.0%).
`./mvnw -Pmetrics validate`: no method in `lavs` is over 10 (highest 7).

---

## 2026-10-23 - Aum, Claude Code - SyncProcessor.send refactor and three more sync integration tests

**Prompt (summarised):** bring `SyncProcessor.send` under the complexity target from the metrics
baseline, and add the integration tests still missing from the dropout/restart/reconnect task.

**What it assisted with:**

- `SyncProcessor.send` (complexity 12) split into `send` (3), `sendByType` (4), `accepted` (4) and
  `rejected` (2). Same behaviour; the existing `SyncProcessorTests` cover every path.
- `SyncIntegrationTests` (real SQLite file, real queue and gateway, `FakeConnection` offline):
  - a loan created offline gets its real number in the co-borrower change queued after it;
  - a possible duplicate confirmed as new: the matches are kept on disk while paused, and the
    customer is then created without searching again;
  - a rejected new customer keeps the change that uses it held across a restart, it is never
    sent, and dismissing the customer clears it too.

**Validation:** `./mvnw clean verify` passes (839 tests, coverage gate met); the integration tests
passed 5 runs in a row. `./mvnw -Pmetrics validate` shows no `SyncProcessor` method over 4.

---

## 2026-10-22 - Gabeliu, Claude Code (Opus 5.5) - Windows wrapper and CI fix

**Prompt (summarised):** complete the remaining work. While checking main's CI
it found that the CI logs showed "Failed to extract Maven distribution" but the
job still passed.

**What it produced** on `fix/mvnw-fresh-download`:

- `mvnw.cmd`: variables are set outside `IF` blocks (cmd expanded `%ZIP_PATH%`
  to empty before the block ran) and failures jump to one `:fail` label that
  exits with 1. Tested with an empty wrapper cache, a missing properties file
  and a failing Maven goal.
- CI: a step that fails unless the test and coverage reports exist.

**Marking:** both files carry an `AI-assisted` note.

---

## 2026-10-22 - Gabeliu, Claude Code (Opus 5.5) - Quality Report draft

**Prompt (summarised):** draft the Quality Report from what we have so far.

**What it produced** on `docs/quality-report`: `docs/quality-report.md`, with the
test results, coverage per package, the acceptance criteria mapped to their
tests, and the metrics baseline. The parts that need the final `main`
(final metrics, clean-clone redo, walkthrough, responsiveness) are marked TODO.

**Marking:** the numbers come from the build and CK output; the team edits
the wording.

---

## 2026-10-22 - Gabeliu, Claude Code (Opus 5.5) - metrics tooling and baseline

**Prompt (summarised):** complete the remaining work, including the metrics
tooling and a "before" baseline for the final report.

**What it produced** on `chore/metrics-baseline`:

- A `metrics` Maven profile (`./mvnw -Pmetrics validate`) that runs CK 0.7.0
  over `src/main/java/lavs` and writes coupling (CBO), complexity (WMC and
  per-method cyclomatic complexity) and cohesion (LCOM*) to `target/metrics/`.
- `docs/metrics/baseline-2026-10-22.md`: the baseline from `main` at `ff950b2`,
  with averages per layer, the methods over 10 and the most coupled classes.
- A readme section on running it.

**Marking:** the profile carries an `AI-assisted` note; the baseline is
generated numbers.

---

## 2026-10-22 - Gabeliu, Claude Code (Opus 5.5) - acceptance tests

**Prompt (summarised):** write the Phase 4 acceptance tests from the
proposal's GIVEN/WHEN/THEN criteria for US6, US9 and US11.

**What it produced** in `src/test/java/lavs/acceptance`: one test per criterion,
named with the criterion's text. US6 and US9 run the real services with a
`FakeConnection`; US11 runs the app's own queue wiring (`SyncSessions`) on a
real SQLite file in a temp folder, going offline, restarting and reconnecting.

**Marking:** each test class carries an `// AI-assisted` comment.

---

## 2026-10-22 - Gabeliu, Claude Code (Opus 5.5) - US9 every co-borrower

**Prompt (summarised):** complete the remaining work; US9's acceptance criteria
say every borrower needs contact details and the MMM is told who to update.

**What it changed:** `LoanStatusService` now checks every co-borrower on the
loan (mailing address and a phone number or email), naming each one who is
missing something, e.g. "co-borrower Mere Brown (C-2) needs a mailing
address". Guarantors aren't checked, as they aren't borrowers. The borrower
list is read once and reused for the guarantor rule. Tests updated and added
in `LoanStatusServiceTests`, `LoanSubmissionTests` and `QueuedWorkflowTests`.

**Marking:** the new methods and tests carry an `// AI-assisted` comment.

---

## 2026-10-22 - Gabeliu, Claude Code (Opus 5.5) - handoff fixes on #47

**Prompt (summarised):** fix the problems listed in the team handoff that are
still there on #47's branch.

**What it changed** on `fix/handoff-issues` (on top of
`feature/complete-customer-workflow`):

- Replaced the out-of-date TEMPORARY notes in AddressService, EmailService,
  PhoneNumberService, CustomerService, EmployerService, FixedStatusFeed and
  SyncStatusFeed: saves now go through the upload queue, and FixedStatusFeed
  is only for the demo screen. No TEMPORARY notes are left.
- RelationshipsController asks with the shared ConfirmDialog instead of an
  Alert.
- Navigator shows an error window when a screen can't be opened, instead of
  only printing the stack trace.
- Moved PendingUploadsState (no JavaFX) from ui to logic with its tests, so
  they count toward the logic coverage gate.

**Marking:** the new Navigator code carries an `// AI-assisted` comment; the
rest are comment, package and wording changes.

---

## 2026-10-22 - Aum, Claude Code - restore the US11 pieces lost when #48 was merged into #47

**Prompt (summarised):** #47 and #48 both routed saves through the upload queue, and the merge
kept #47's version. Bring back the parts of #48 that main lost, on top of the merged code.

**What it assisted with:**

- `PendingChange.duplicateMatches` plus a `duplicateMatches` column in `LocalDatabase` (added
  to older database files when they open) and `SqlitePendingChangeStore` (add, update and the
  batch `commit`). `SyncProcessor` stores the matches on the paused create and clears them once
  the MMM decides. `DuplicateCandidates.forChange` now reads that field instead of parsing the
  "Possible existing customer: ..." message text.
- `SyncService.dismissRejected`: dismissing a rejected new customer or loan also removes the
  changes that were rejected because of it.
- Put the row decisions back in `PendingUploadsState` (now in `logic`, moved there by #49 too) (`canDismiss`, `canResolveDuplicate`, `getDuplicateMatches`,
  `getCustomerLabel`, `getSavedDetails`, and the dismiss / use existing / new customer
  actions, which go through #47's `SyncService.resolveDuplicate`). `PendingUploadsPanel` now
  only builds the controls from those rows.
- Deleted `SyncStatusFeed` and its tests: nothing used it after #47 added `SessionStatusFeed`.
- Tests: duplicate matches in `PendingChangeTests`, `SqlitePendingChangeStoreTests` (round trip,
  batch commit, opening an older file), `SyncProcessorTests` (stored and cleared),
  `SyncServiceTests` (dismiss also clears dependants) and `PendingUploadsStateTests` (row
  decisions, actions, every message-type description).

**Validation:** `./mvnw clean verify` passes: 822 tests, coverage gate met.

**Limits:** a new customer already paused as a possible duplicate in a local database made before
this change has no stored matches, so its row offers no choice; clear that test database. Not
yet tried by hand in the running app.

---

## 2026-10-22 - Gabeliu, Codex - Complete customer workflow

**Prompt (summarised):** implement the remaining delegated tasks with high-quality code and check the supplied project brief. External boards, proposal criteria and reports were explicitly excluded by the user. Keep commits short, omit coauthor trailers, and use a feature branch and PR.

**Changes:** connected durable, per-user uploads to application services; added customer search/selection, customer loans and employer screens; enforced loan eligibility and submission revalidation; added customer/loan notes and document metadata editors; introduced consumed service interfaces; exposed duplicate decisions and saved rejection fields. Number replacement is transactional, and compound borrower/document changes persist their progress across restart. Local saves have distinct UI results.

**Validation:** added service, SQLite restart/rollback, queue isolation, eligibility, optional-component and Windows JavaFX rendering tests. The Maven build targets Java 17 and enforces 95% branch coverage, including a separate business-logic package rule. Exact test totals and reports are produced by CI.

**Limits:** no live-mainframe writes were used for verification. The API has no idempotency key or atomic multi-request update, so a lost response after server acceptance remains ambiguous. The README describes this limitation. This session generated implementation code with the user's explicit authorization; affected components are marked AI-assisted. No team participation or review was fabricated.

---

## 2026-10-22 - Aum, Claude Code (Opus 5.5) - route every save through the upload queue (US11)

**Prompt (summarised):** finish US11 step B with the team's sign-off: route every
remaining service's saves through `SyncService` so the app works offline, and add the
pending-uploads panel actions a queued new customer needs.

**What it assisted with** on `feature/us11-queue-contacts` (stacked on #44):

- `lavs.logic.QueuedWrites` (new) - assisted writing the helper every save goes through: it
  "sends" the usual message (e.g. `customer.AddAddress`) to a connection that only records the
  request, then queues that request's type and fields, so a queued save is exactly what the
  message would have sent. Refuses reads. Also maps a queued result onto a `Status` for the
  older Status-based result types.
- Services now save through the queue (reads stay live): `AddressService`,
  `PhoneNumberService`, `EmailService`, `CustomerRelationshipService`, `LoanBorrowerService`
  (update stays delete-then-add, in order), `LoanStatusService` (checks read live, the status
  change is queued), `LoanService` and `CustomerService`. A customer or loan created offline
  gets a temporary number, which the queue swaps for the real one in later changes.
- Results can now be waiting to upload: `ServiceResult` (`WAITING`, `isSaved()`,
  `isWaitingToUpload()`, `isHeldForReview()`), and `RelationshipResult`, `LoanBorrowerResult`,
  `LoanStatusResult`, `LoanSaveResult` (`isSaved()`, `isWaitingToUpload()`).
- Possible duplicates: `PendingChange.duplicateMatches`, a `duplicateMatches` column in
  `LocalDatabase` (added to older files when they open) and `SqlitePendingChangeStore`, set by
  `SyncProcessor` when it pauses a new customer. `SyncService.dismissRejected` also clears the
  changes rejected because they used a rejected new customer or loan.
- Screens: they move on when a save is saved (sent or queued) and say when it is waiting to
  upload; they don't read lists for a customer or loan that isn't uploaded yet, and don't show
  temporary numbers. The pending-uploads panel shows why a change was paused or rejected and
  has "Use existing", "It's a new customer" and "Dismiss" buttons. `LavsApp` refuses to start
  if the local database can't be opened, since nothing could be saved safely.
- Tests: wrote `QueuedWritesTests` and new tests for the waiting, held and temporary-number
  cases in each service, the result types, the store and database migration, the sync
  dismissal and the panel state; updated the existing service and screen-text tests to run
  over a real `SyncService` (offline saves are now kept rather than reported as failures).

**Known limits:** a record added offline has no mainframe id until it uploads, so lists show
it only after that; a screen keeps a temporary number until it is reopened; an automatic
co-borrower put-back only happens when the replacement is refused straight away. The screen
controllers have no unit tests; the app was started to check it launches.

**Validation:** `mvn -B clean verify` passed - 779 tests, 95% branch coverage gate met
(logic 99.2%, data 96.9%, model 100%).

**Marking:** changed code and new tests carry `// AI-assisted:` comments.

---

## 2026-10-22 - Aum, Claude Code (Opus 4.8) - route EmployerService through SyncService

**Prompt (summarised):** start "step B" of US11 — route one service's writes through the
offline queue instead of calling the mainframe directly. Picked `EmployerService` (US5) because
it isn't wired into any screen yet, so the change is self-contained.

**What it assisted with** on `feature/us5-employers-queue`:

- `lavs.logic.EmployerService` - assisted rewriting add/update/delete to build the message fields
  and call `SyncService.queue(...)` instead of sending `AddEmployer`/`UpdateEmployer`/
  `DeleteEmployer` over the `Connection`. The field keys and string forms match those messages'
  `send` exactly, so `MainframeGateway` replays each queued change unchanged. Writes now work
  offline. Blocks on the queue future (safe: the service runs on a `BackgroundTasks` thread).
- `lavs.logic.EmployerResult` - assisted reshaping it from a mainframe `Status` wrapper to the
  queue outcomes **SAVED / WAITING / REJECTED** plus field errors. This is the template for the
  other services moving onto `SyncService`; it differs from `ServiceResult` because on the queue
  being offline is WAITING, not a failure.
- `lavs.logic.EmployerServiceTests` - fully wrote the tests (21): validation stops a bad call
  before it is queued; a good call is SAVED and replays the exact fields for add/update/delete; an
  offline call is WAITING and stays queued; a refused call is REJECTED with the message; a local
  save failure is reported. Drive a real `SyncService` over the in-memory queue and fake gateway.

**Not done (flagged for the PR):** an employer added offline has no mainframe `id` until it
uploads, so it cannot be updated or deleted before then; and `SyncStep` returns the assigned
number only for customer/loan creates, so a future employer UI must re-list after an add.

**Validation:** `mvn -B clean verify` passed - 734 tests, 95% branch coverage gate met
(`EmployerService` 21/21 branches). The other services still call the gateway directly.

**Marking:** the changed service, result and tests carry `// AI-assisted:` comments.

---

## 2026-10-22 - Aum, Claude Code (Opus 4.8) - wire SyncService into LavsApp

**Prompt (summarised):** now that the mainframe gateway exists, wire the offline
sync service into the app's composition root: start `SyncService`, drive the top
bar from it, open one shared SQLite file and show the pending-uploads panel.

**What it assisted with** on `feature/us11-wire-sync`, all in `lavs.ui.LavsApp`
(the composition root):

- open one `LocalDatabase` shared by the dropdown cache (`CategoryConstantStore`)
  and the upload queue (`SqlitePendingChangeStore`), resolving the TEMPORARY that
  gave the constants their own copy.
- build one `MainframeGateway` over the existing `Connection` and reuse it for both
  the customer services and the sync thread, so queued uploads go out as the
  logged-in MMM.
- construct and `start()` the `SyncService`, and drive the top bar with
  `SyncStatusFeed` instead of `FixedStatusFeed` (falling back to `FixedStatusFeed`
  if the local database cannot be opened).
- wire the pending-uploads panel to the status pill via `setOnStatusClicked`.
- override `Application.stop()` to `close()` the sync thread so a queued change is
  not cut off mid-write.

**Tests:** none added. `LavsApp` is the JavaFX entry point (excluded from the
coverage gate), and the one new behaviour — two stores sharing one SQLite file —
is already covered by `LocalDatabaseTests.createsFileAndBothTables`.

**Not done (next step):** the services still call the gateway directly; routing
each write through `SyncService.queue` is a separate change, done per owner.

**Validation:** `mvnw.cmd -B clean verify` passed — 731 tests, 95% branch coverage
gate met. Launched with `javafx:run`: started with no startup exceptions.

**Marking:** the wiring in `LavsApp` carries `// AI-assisted:` comments.

---

## 2026-10-22 - Aum, Claude Code (Opus 4.8) - fix SyncService.drain notify ordering

**Prompt (summarised):** fix the intermittent `SyncServiceTests.onlineSaveIsSentStraightAway`
failure (~1 in 4) caused by `drain()` completing a change's future before notifying listeners.

**What it assisted with** on `fix/sync-drain-notify-order`:

- `lavs.logic.SyncService.drain()` - assisted swapping the order so `notifyListeners()` runs
  **before** `complete(...)` in the send loop, and before `answerWaitingAfterError(...)` in the
  error path. Now a caller woken by its future's result (and any listener it then reads, e.g. the
  pending count) sees the up-to-date state - the same order `runOnSyncThread` already used.
  Completing first was a race the top bar and the test could lose.

**Tests:** no new test. The existing `onlineSaveIsSentStraightAway` becomes deterministic; ran
`SyncServiceTests` 12 times in a row with no failure to confirm the flake is gone.

**Validation:** `mvn -B clean verify` passed - 731 tests, 95% branch coverage gate met.

**Marking:** the reordered lines in `drain()` carry `// AI-assisted:` comments.

---

## 2026-10-22 - Gabeliu, Codex - PR integration and sync status

**Prompt (summarised):** finish Claude's outstanding work, fix and verify all
eight PRs, then merge them with short commit messages and no coauthor trailers.

**Changes:** moved the offline indicator's connection check onto the sync
executor. UI status reads use the latest cached result. Connection changes
notify listeners even with an empty queue, and a failed probe does not stop
later retries. Added five regression tests for these cases and upload results.

**Validation:** trial-merged all eight branches, preserving both sides of
AI log conflicts. `clean verify` passed all 731 tests and the 95% branch
coverage gate. The starter tests against the live mainframe remain excluded.

**Marking:** changed code and new tests carry AI-assisted comments.

---

## 2026-10-22 - Aum, Claude (Sonnet 4.6) - US11 offline indicator + pending uploads panel

**Prompt (summarised):** assist with the US11 logic and UI layer (offline indicator in the
top bar and the pending-uploads panel that lists queued changes).

**What it assisted with** on `feature/us11-offline-indicator`:

- `lavs.ui.SyncStatusFeed` - assisted implementing the real `StatusFeed` adapter over
  `SyncService` (replaces `FixedStatusFeed`; exposes online/pending state and change
  notifications to the top bar).
- `lavs.logic.SyncService` - assisted adding the one-line `isConnected()` delegation so
  `SyncStatusFeed` only imports from `lavs.logic`, not the data layer.
- `lavs.ui.PendingUploadsState` - assisted implementing the panel's state class (no JavaFX):
  `getRows()`, `isEmpty()`, `hasActionNeeded()`, `descriptionFor()` and `statusTextFor()`.
- `lavs.ui.PendingUploadsPanel` - assisted implementing the JavaFX Popup that appears when
  the user clicks the status pill in the top bar; lists each queued change with description
  and status.
- `lavs.ui.SyncStatusFeedTests` - implemented tests: 5 tests covering online/offline
  delegation, pending count delegation and listener propagation.
- `lavs.ui.PendingUploadsStateTests` - implemented tests: 27 tests covering isEmpty,
  hasActionNeeded, getRows, descriptionFor for every message type, and statusTextFor.

**Marking:** all new and modified files carry `// AI-assisted.` comments.

---

## 2026-10-22 - Taran, Claude Code (Sonnet 5.5)

**Prompt (summarised):** build US1, log in and log out: a service over the
mainframe login, the login screen from Figma 01 with its error states, and
replace the temporary login in `LavsApp`. A requirement ID list (US1-R1 to
R6) was the spec, with one test per ID.

**What it produced** on `feature/us1-login`:

- `AuthService`, `AuthResult` and `LoginForm`: log in and out over the
  `Connection`; outcomes success, network failure, rejected (with the
  mainframe's message) and invalid (a blank field, nothing sent); the
  username is trimmed and the password sent as typed.
- `LoginFlow` and `LoginAttempt`: logs in, then refreshes the dropdown
  values; values saved from last run are enough to carry on with.
- `LoginScreenState` (no JavaFX, unit tested): inline field errors for an
   incomplete form, and a banner for a rejection, a network failure or missing
  failure or missing dropdown values, and when to move on.
- `LoginScreen` (thin JavaFX) and three styles in `lavs.css`: the centred
  card, run through `BackgroundTasks` with a "Logging in..." button.
- In `LavsApp`: the login screen replaces `temporaryLogin` and `signIn`,
  and Log out calls `AuthService.logout()`.
- Unit tests for every service and decision, with the requirement ID above
  each test.

**Marking:** each new file carries an `// AI-assisted` comment.

---

## 2026-10-22 - Gabeliu, Claude Code (Opus 5.5) - CI, PR template, readme

**Prompt (summarised):** review the whole repo against the task list, tidy up
the git side and update the readme and similar files.

**What it produced** on `chore/ci-every-pr`:

- `.github/workflows/ci.yml`: runs on pull requests into any branch, so
  stacked PRs get checked; a newer push cancels a run still going.
- `.github/pull_request_template.md`: the checklist from the task document.
- `mainframe-interface-main/readme.md`: what the coverage gate counts, where
  the app keeps its local file, and a "Working on the code" section (layers,
  branches, PRs, stacked PRs, tests, AI log).
- `README.md` at the repo root, pointing to the app folder and its readme.

**Marking:** config and docs only; the workflow carries an `AI-assisted` note.

---

## 2026-10-22 - Gabeliu, Claude Code (Opus 5.5) - slow FakeConnection

**Prompt (summarised):** same session as the repo review: add what the test
fakes still need for the Phase 4 responsiveness check.

**What it produced** on `test/slow-fake-connection`:

- `FakeConnection.setDelay(Duration)`: every `send()` waits that long before
  it answers (a network failure too). Interrupted while waiting, it answers
  with a network failure and keeps the interrupt. Its lists are now safe to
  use from background threads.
- Four tests for it in `FakeConnectionTests`.

**Marking:** the new method, the waiting code and the tests carry an
`// AI-assisted` comment.

---

## 2026-10-22 - Gabeliu, Claude Code (Opus 5.5) - package skeleton

**Prompt (summarised):** the team agreed I take over Scott's cards; do the
package skeleton.

**What it produced** on `chore/package-skeleton`:

- A `package-info.java` for `ui`, `logic`, `data`, `model` and `validation`
  saying what belongs in each layer and what it may use.
- `LayerRulesTests`: reads the source and fails the build if a layer uses
  something the working rules forbid (imports or fully qualified names;
  comments ignored). `LavsApp` is the one allowed exception. It also tests
  its own checker.
- Removed the "swap this package for Scott's package once his skeleton is
  on main" notes from 34 files, including `LavsApp` and the US1 files. The
  three in files my US8 branch changes are removed there.

**Marking:** the package-info files and the test carry an `// AI-assisted`
comment; the removals are comment-only.

---

## 2026-10-22 - Gabeliu, Claude Code (Opus 5.5) - mainframe gateway

**Prompt (summarised):** the team agreed I take over Scott's cards; build the
mainframe gateway.

**What it produced** on `feature/mainframe-gateway`:

- `lavs.data.GatewayResult`: sorts every `Status` into SUCCESS,
  NETWORK_FAILURE (an unreadable reply counts too, so it's retried) or
  REJECTED.
- `lavs.data.MainframeGateway`: wraps the `Connection`; `send(Message)`,
  `login`, `logout` and `isConnected` never throw. It is also the real
  `UploadGateway` for Aum's `SyncService`: sends a queued change as its
  message, returns the number a `customer.create`/`loan.create` was given,
  and checks for an existing customer (same names and date of birth) before
  a new one is uploaded.
- Tests for both with `FakeConnection` (every branch covered).
- Updated the notes in `SendResult`, `UploadGateway` and
  `DuplicateCheckResult` that were waiting for the gateway.

**Marking:** both classes and their tests carry an `// AI-assisted`
comment. Agreed by the team as a handover of Scott's cards.

---

## 2026-10-22 - Gabeliu, Claude Code (Opus 5.5) - sync integration tests

**Prompt (summarised):** same session; write the Phase 4 integration tests for
dropout, restart and reconnect now that the gateway exists.

**What it produced** on `test/sync-integration` (built on the gateway branch):

- `SyncIntegrationTests`: a real SQLite file in a temp folder, the real
  queue, `SyncService` and `MainframeGateway`, with a `FakeConnection` for
  the network. Covers: a save made offline waits on disk; a dropout half way
  keeps the rest in order; reconnecting sends oldest first; a customer
  created offline gets its real number in later changes; a possible
  duplicate is held until the MMM picks the existing customer; saves left at
  close are sent after a restart; a rejected change stays rejected across a
  restart and is never retried.

**Marking:** the test class carries an `// AI-assisted` comment.

---

## 2026-10-22 - Gabeliu, Claude Code (Opus 5.5) - US3 add and edit customer

**Prompt (summarised):** the team agreed I take over Scott's cards; build US3.

**What it produced** on `feature/us3-customer-details` (built on the gateway
branch):

- `CustomerService` (create, update and load through the gateway) and
  `CustomerInput`. A customer needs a first name, family name and a date of
  birth that is a real date and not in the future; title and nationality are
  optional.
- `ServiceResult<T>`: one result type for services built on the gateway, so
  new services don't each need their own.
- The Details tab (`customer-details.fxml`, `CustomerDetailsController`) and
  a TEMPORARY tabbed customer record (`customer-record.fxml`,
  `CustomerRecordController`, `CustomerScreens`) with Details and Yu's
  Relationships, until Taran's record exists. It replaces the untabbed
  stand-in in `LavsApp`, and the temporary search gets "New customer".
- `ResultText` and `CustomerText` (no JavaFX, unit tested) for the wording.
- Checked the real screens locally under JavaFX with a `FakeConnection` and
  looked at rendered screenshots (not committed: CI has no display).

**Marking:** every new class and the tests carry an `// AI-assisted`
comment. Agreed by the team as a handover of Scott's cards.

---

## 2026-10-22 - Gabeliu, Claude Code (Opus 5.5) - US4 addresses, phones, emails

**Prompt (summarised):** the team agreed I take over Scott's cards; build US4.

**What it produced** on `feature/us4-contact-details` (built on the US3 branch):

- `AddressService`, `PhoneNumberService`, `EmailService` with their form
  inputs: list, add, update and delete through the gateway, returning
  `ServiceResult`. An address needs a type, first line, city and country, and
  a 4-digit postcode in New Zealand; a phone number needs a valid number (the
  country code is optional, like "+64"); an email needs a valid address. An
  "Other" type needs a description. Every branch tested.
- Addresses, Phone numbers and Emails tabs on the temporary customer record,
  built on one `ContactListController` (table, edit, remove with confirm, and
  a form beside it, like Yu's Relationships tab). Each tab reads the
  mainframe the first time it is opened.
- `ContactText` (no JavaFX, unit tested) for the wording.
- Checked the real tabs locally under JavaFX with a `FakeConnection` and
  looked at rendered screenshots (not committed: CI has no display). That
  check caught a crash in Remove, now fixed.

**Marking:** every new class and the tests carry an `// AI-assisted`
comment. Agreed by the team as a handover of Scott's cards.

---

## 2026-10-21 - Gabeliu, Claude Code (Opus 5.5) - US9 submit button

**Prompt (summarised):** do my US9 and US8 parts with high quality: the
service should look everything up from the loan, then the "Submit for
verification" button on the loan screen.

**What it produced** on `feature/us9-submit-button`:

- `LoanStatusService.submitForVerification(loanNumber)`: reads the loan for
  its main borrower and status (only New can be submitted), checks the
  customer's addresses (paging until it finds a mailing one), phone numbers,
  emails and employers, then the guarantor rule. Every problem is returned
  at once as field errors; a failed lookup returns its status. Tested with
  `FakeConnection` (every branch covered).
- `LoanSubmission` (no JavaFX, unit tested): when the button is shown or
  enabled, and the message for each outcome.
- The button and its message in the loan screen header
  (`loan-screen.fxml`, `LoanScreenController`), run through `BackgroundTasks`.
- `LoanScreenServices`: the loan screen's services as one value, so
  `open`/`show` and `LoanScreens` don't grow a parameter per tab.
- Checked the real screen once locally by loading the FXML under JavaFX with
  a `FakeConnection` (not committed: CI has no display).

**Marking:** new classes, the submit handler and the tests carry an
`// AI-assisted` comment. Agreed by me as an exception to the hand-written
rule.

---

## 2026-10-21 - Gabeliu, Claude Code (Opus 5.5) - US9 guarantor rule

**Prompt (summarised):** I decided the guarantor rule (block Pending when
the main borrower has no employer and no guarantor is on the loan); add it
to US9.

**What it produced** on `feature/us9-guarantor-rule`:

- `LoanStatusService.submitForVerification` now takes the main borrower's
  customer number and checks the rule: `customerEmployer.List` first, then
  `loanCoborrower.List` (via `listCoborrowers`) only if there's no employer.
  A failed lookup is returned as the mainframe status; nothing is submitted.
- `LoanStatusServiceTests` rewritten for the new rule and lookups.
- Notes in `LoanStatusService` and `EligibilityChecker` updated: the rule
  lives in US9 only, not the shared eligibility check US6 uses.

**Marking:** the guarantor check and tests carry an `// AI-assisted` comment.

---

## 2026-10-21 - Gabeliu, Claude Code (Opus 5.5)

**Prompt (summarised):** go through the task list, do my tasks that aren't
blocked by anyone else, and write the US8 method that lists a loan's
co-borrowers and guarantors.

**What it produced** on `feature/us8-list-coborrowers`:

- `LoanBorrowerService.listCoborrowers` and `LoanBorrowerListResult`: reads
  every co-borrower and guarantor on a loan with `loanCoborrower.List`, page
  by page, for the Borrowers tab and the US9 guarantor check.
- `LoanCoborrower` gets a `coborrowerName` field (filled in from the list).
- Tests for each in `LoanBorrowerServiceTests` and `LoanCoborrowerTests`.

**Marking:** the new method, result class, model field and tests carry an
`// AI-assisted` comment. Agreed by me as an exception to the hand-written
rule, like the earlier US8/US9 service work.

---

## 2026-10-21 - Gabeliu, Claude Code (Opus 5.5) - US8 Borrowers tab

**Prompt (summarised):** same session as the US9 submit button: build the US8
Borrowers tab on the loan screen.

**What it produced** on `feature/us8-borrowers-tab`:

- The Borrowers tab (`loan-borrowers.fxml`, `LoanBorrowersController`): lists
  everyone else on the loan, changes a selected borrower's type, removes one
  after a confirmation, and adds one by customer number (TEMPORARY until
  customer search has select mode). Every call runs through `BackgroundTasks`
  and the list is read again after each change.
- `BorrowersText` (no JavaFX, unit tested): type names, button text and the
  message for each outcome.
- `LoanBorrowerService.updateCoborrower` now takes the existing record and,
  if the mainframe refuses the replacement, adds the original back so a
  refused change doesn't drop the person from the loan. Tested.
- Checked the real tab locally under JavaFX with a `FakeConnection` and
  looked at rendered screenshots (not committed: CI has no display).

**Marking:** new classes, the changed service method and the tests carry an
`// AI-assisted` comment. Agreed by me as an exception to the hand-written
rule.

---

## 2026-10-19 - Aum, Claude (Sonnet 4.6) - US5 employer service (partial)

**Prompt (summarised):** assist with parts of the US5 logic layer (employers tab —
add, update, delete employer records for a customer) and implement the tests.

**What it assisted with** on `feature/us5-employers`:

- `lavs.logic.EmployerResult` - assisted implementing this result type (same
  shape as `LoanBorrowerResult`; wraps a `ValidationResult` plus a mainframe `Status`).
- `lavs.logic.EmployerService` - assisted implementing parts of the service:
  add, update, delete employer records via `customer.addEmployer` /
  `customer.updateEmployer` / `customer.deleteEmployer`. Validates customerNumber,
  name, type (Primary or Secondary) and optional salary (currency format).
- `lavs.logic.EmployerServiceTests` - implemented the tests: 18 tests, 100%
  branch coverage on `EmployerService` and `EmployerResult`, following the
  `LoanBorrowerServiceTests` pattern (`@ExtendWith(MockitoExtension.class)`,
  `argThat`, `successResponse`, `rejectedResponse`).

**Marking:** all three files carry `// AI-assisted` comments.

---

## 2026-10-19 - Taran, Claude Code (Sonnet 5.5)

**Prompt (summarised):** build the shared UI components every screen will
use: form fields with inline errors, banners, tags, section headings, a
record table and a delete confirmation, plus a demo screen to compare them
with Figma.

**What it produced** on `feature/ui-shared-components`:

- `FormField` and `FieldErrorPresenter`: shows a `ValidationResult` on the
  field named by each `FieldError`, counts fields for the banner title,
  and keeps errors that have no field so they are not lost.
- `BannerContent` (the offline, rejection and validation wording from the
  Figma frames), `Banner`, `Tag`, `SectionHeading`, and the variant enums.
- `RecordTable` (fixed header over scrolling rows, row links, empty state,
  Delete always through `ConfirmDialog`), `ColumnWidths`, `ConfirmContent`.
- `DemoScreen`, shown only when `LAVS_DEMO=true` or `-Dlavs.demo=true`.
- Unit tests for every decision (presenter, banner wording, column widths,
  confirm wording, variant-to-CSS mapping, demo switch).

**Marking:** each new file carries an `// AI-assisted` comment.

---

## 2026-10-19 - Taran, Claude Code (Sonnet 5.5)

**Prompt (summarised):** build the app shell: a list of screens and a
navigator with a back stack, a top bar showing connection state, pending
uploads and the signed-in user, and the main window that joins them.

**What it produced** on `feature/ui-app-shell`:

- `lavs.ui.Screen`, `NavigatorState`, `ScreenRegistry`, `Refreshable`: the
  navigation rules (parameters, a back stack that keeps each built view,
  reset on login/logout, "not built yet" placeholders, a view that fails to
  build leaves the current screen alone), with no JavaFX, and unit tests.
- `Navigator` (thin JavaFX control over those rules).
- `StatusFeed`, `FixedStatusFeed` (TEMPORARY), `TopBarState`, `TopBar`:
  "Online / Offline" with "All changes uploaded" or "N waiting to upload",
  "Signed in: J. Smith", a default title per screen, hidden on Login. The
  text and style decisions are in `TopBarState` and unit tested.
- `MainWindow`; `LavsApp` now builds the shell and starts on a TEMPORARY
  login stand-in; the sample `main-window.fxml` was removed.

**Marking:** each new file carries an `// AI-assisted` comment.

---

## 2026-10-19 - Taran, Claude Code (Sonnet 5.5)

**Prompt (summarised):** build the helper that runs every service call off
the UI thread and shows a loading state, testable without starting JavaFX.

**What it produced** on `feature/ui-background-task`:

- `lavs.ui.BackgroundTasks`: takes a background executor and a UI-thread
  runner; `run(work, onStart, onDone, onError)` returns a handle whose
  `cancel()` means no callback is ever made. 100% branch tested with
  queues standing in for threads (`BackgroundTasksTests`).
- `lavs.ui.LoadingState`: the rules for the busy button text, disabled
  button, Cancel button and skeleton rows. Reads get Cancel and skeletons;
  saves do not, because saves still upload through the sync queue
  (`LoadingStateTests`).
- `LoadingView` and `SkeletonRows`: thin JavaFX controls with no branches
  that copy the state onto the screen.

**Marking:** each new file carries an `// AI-assisted` comment.

---

## 2026-10-19 - Taran, Claude Code (Sonnet 5.5)

**Prompt (summarised):** build the shared LAVS stylesheet from the Figma.

**What it produced** on `feature/ui-stylesheet`:

- `lavs.css`: colour tokens as looked-up colours on `.root`, Segoe UI
  typography (Segoe UI Semibold for Figma's semibold text), and the style
  classes for every shared component.
- `LavsStylesheetTests`: checks that every token used is defined, that no
  one-off hex colours sit outside `.root`, and that every component class
  exists. Runs without starting JavaFX.
- `LavsApp` now loads the stylesheet at 1280x800; `main-window.fxml` has a
  TEMPORARY row of sample buttons and tags for checking the styles by eye.

**Marking:** `lavs.css` and `LavsStylesheetTests` carry an `// AI-assisted`
comment. The `LavsApp` and FXML edits were small.

---

## 2026-10-18 - Aum, Claude (Opus 5.5) - fixes from pre-push testing

**Prompt (summarised):** test the local store and sync work before pushing and
fix anything that could fail.

**What it changed** on `feature/local-store-sync`:

- `SyncService.close()` waits for the sync thread, so saves already queued
  reach the local file and the SQLite file is released (an open file stops
  Windows CI deleting test temp folders).
- `SyncService` no longer leaves a screen waiting forever after an unexpected
  upload error; the change stays queued and the result comes back as WAITING.
- `SyncProcessor` never sends a change that still has the temporary number of
  a paused or rejected record, including indirect cases (changes to a new loan
  whose new customer is paused or rejected), and resumes those changes when
  the MMM resolves the duplicate.
- New tests for each fix in `SyncServiceTests` and `SyncProcessorTests`.

**Marking:** the changed code carries `// AI-assisted` comments.

---

## 2026-10-18 - Aum, Claude (Opus 5.5)

**Prompt (summarised):** assist with Phase 2 local store and sync — result
types, the background service, and tests.

**What it produced** on `feature/local-store-sync`:

- `lavs.data`: `SendResult`, `DuplicateCheckResult`, `UploadGateway` (the
  interface Scott's gateway will implement, marked TEMPORARY).
- `lavs.logic.SyncService` - single background sync thread; queuing,
  draining, future completion and listener notification.
- Unit tests for all of the above, plus tests for the data store and the
  sync processor that Aum wrote: `LocalDatabaseTests`,
  `SqlitePendingChangeStoreTests`, `CategoryConstantStoreTests`,
  `ResultTypesTests`, `InMemoryChangeQueue` (test helper),
  `FakeUploadGateway` (scripted test helper), `SyncProcessorTests`,
  `SyncServiceTests`, `TemporaryNumbersTests`, `PendingChangeTests`.
- `sqlite-jdbc` dependency added to `pom.xml`.

**Marking:** each file that carries a `// AI-assisted` comment. Generating
these tests was agreed by the team as an exception to the hand-written rule.

---

## 2026-10-15 - Aum, Claude (Opus 5.5)

**Prompt (summarised):** generate unit tests for the Phase 1 model classes
in `lavs.model`.

**What it produced** on `feature/model-classes`:

- Unit tests for `Customer`, `Address`, `PhoneNumber`, `Email`, `Employer`,
  `Loan` and `LoanCoborrower` (`src/test/java/lavs/model`).

**Marking:** each test file carries a `// AI-assisted` comment. Generating
these tests was agreed by the team as an exception to the hand-written rule.

---

## 2026-10-11 - Gabeliu, Claude Code (Sonnet 5)

**Prompt (summarised):** work through the sprint backlog (build/test/CI
setup, validation framework, eligibility check, a test fake that can go
offline mid-test, US8 and US9 service logic), resolve the main-borrower/
guarantor question if possible, and don't push or open a PR without
checking first.

**What it produced**, each on its own local branch off `main`, none pushed:

- `chore/build-test-ci-setup` - Maven wrapper (mvnw/mvnw.cmd), Mockito,
  JaCoCo (95% branch-coverage gate on the team's own code), excludes the
  starter tests that call the live mainframe server from the default run,
  `.github/workflows/ci.yml`, and a build/test/run README section.
- `shared/validation-framework` - `lavs.validation`: `FieldError`,
  `ValidationResult`, `Validators` (required/date/phone/email/currency/
  postcode).
- `shared/eligibility-check` - `lavs.logic.EligibilityChecker` (mailing
  address + phone or email).
- `test/fake-connection-offline` - added the "goes offline mid-test ->
  send() returns a rejection" behaviour to `mainframe.client.FakeConnection`
  (whose scripted-response design originates from Yu Tokuda's unmerged
  `feature/us10-constants` branch - needs reconciling when that merges).
- `feature/us8-loan-coborrowers` - `lavs.logic.LoanBorrowerService`: add/
  remove/update a co-borrower or guarantor via `loan.AddCoborrower`/
  `loan.DeleteCoborrower` (update = remove then re-add). Service logic and
  tests only - the screen is blocked on US6 and a UI framework choice.
- `feature/us9-submit-for-verification` - `lavs.logic.LoanStatusService`:
  submit a loan for verification (-> Pending) via `loan.UpdateStatus`,
  gated on `EligibilityChecker`.
- `docs/ai-interaction-log` - this file.

**Left unresolved, on purpose:** whether a main borrower with no employer
must have a guarantor before a loan can become Pending. The notes suggest
blocking submission on it, but that is flagged as an open question for
James on Ed in this session rather than decided - see the doc comments on
`LoanStatusService`/`EligibilityChecker`.

**Marking:** every file these branches add carries a `// AI-assisted`
comment (or, for the brought-in `FakeConnection.java`, a note on exactly
which lines are new) per the team's rule that core UI/business logic is
written by hand - these backlog cards were the stated exception for this
session.
