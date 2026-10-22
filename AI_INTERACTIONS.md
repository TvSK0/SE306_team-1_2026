# AI interaction log

Per the team's shared requirement to log AI interactions and mark affected
code with `// AI-assisted: [description]`. Core UI and business logic is
written by hand; entries below are the exception, each noted as such.

Add an entry per session/PR, newest first.

---

## 2026-10-22 - Taran, Claude Code (Sonnet 5.5)

**Prompt (summarised):** build US2, customer search: a service over the
mainframe search, the search screen from Figma 02 with loading, Cancel,
paging and the offline state, select mode from Figma 10 for the Borrowers
tab to reuse, and replace the temporary search in `LavsApp`. A requirement ID
list (US2-R1 to R8, US2-S1 to S4) was the spec, with one test per ID.

**What it produced** on `feature/us2-customer-search`:

- `CustomerSearchService` and `CustomerSearchResult`: search by name or
  number, ten rows a page; outcomes success, network failure, rejected and
  invalid (a blank query, nothing sent). Returns the existing `Customer`.
- `CustomerSearchText` and `CustomerSearchState` (no JavaFX, unit tested):
  the count line, empty-state text, status tags, dd/MM/yyyy dates, the
  "Show more" button, and the rules for replacing or appending rows.
- `CustomerSearchScreen` (thin JavaFX) with two modes behind
  `CustomerSearchMode`: `NormalSearchMode` and `SelectSearchMode`.
- Select mode: `CustomerSelectRequest` (what a caller passes), a Contact
  details column filled by a second background pass (`CustomerContactService`,
  `ContactDetailsColumn`), and the caller's own top bar title through
  `TitledParameter`.
- In `LavsApp`: the real search and select screens replace the temporary
  search, and its loan stand-ins moved into the temporary customer record.
- Unit tests for every service and decision, with the requirement ID above
  each test. Also checked once by building the screens under the JavaFX
  toolkit with a scripted connection (not committed).

**Marking:** each new file carries an `// AI-assisted` comment.

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
