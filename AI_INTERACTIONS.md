# AI interaction log

Per the team's shared requirement to log AI interactions and mark affected
code with `// AI-assisted: [description]`. Core UI and business logic is
written by hand; entries below are the exception, each noted as such.

Add an entry per session/PR, newest first.

---

## 2026-10-22 - Gabeliu, Codex - Complete customer workflow

**Prompt (summarised):** implement the remaining delegated tasks with high-quality code and check the supplied project brief. External boards, proposal criteria and reports were explicitly excluded by the user. Keep commits short, omit coauthor trailers, and use a feature branch and PR.

**Changes:** connected durable, per-user uploads to application services; added customer search/selection, customer loans and employer screens; enforced loan eligibility and submission revalidation; added customer/loan notes and document metadata editors; introduced consumed service interfaces; exposed duplicate decisions and saved rejection fields. Number replacement is transactional, and compound borrower/document changes persist their progress across restart. Local saves have distinct UI results.

**Validation:** added service, SQLite restart/rollback, queue isolation, eligibility, optional-component and Windows JavaFX rendering tests. The Maven build targets Java 17 and enforces 95% branch coverage, including a separate business-logic package rule. Exact test totals and reports are produced by CI.

**Limits:** no live-mainframe writes were used for verification. The API has no idempotency key or atomic multi-request update, so a lost response after server acceptance remains ambiguous. The README describes this limitation. This session generated implementation code with the user's explicit authorization; affected components are marked AI-assisted. No team participation or review was fabricated.

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
