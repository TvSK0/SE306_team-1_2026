# AI interaction log

Per the team's shared requirement to log AI interactions and mark affected
code with `// AI-assisted: [description]`. Core UI and business logic is
written by hand; entries below are the exception, each noted as such.

Add an entry per session/PR, newest first.

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
