# Mainframe Interface: Project 2 Starter

Welcome to the second project for SOFTENG 306. 

The project specification is available [on Canvas](https://canvas.auckland.ac.nz/courses/142477/pages/project-2-overview?module_item_id=3007942). The four deliverables for this project are:
* [Proposal](https://canvas.auckland.ac.nz/courses/142477/assignments/514280)
* [Presentation](https://canvas.auckland.ac.nz/courses/142477/assignments/514279)
* [Code](https://canvas.auckland.ac.nz/courses/142477/assignments/514277)
* [Documentation](https://canvas.auckland.ac.nz/courses/142477/assignments/514278)

## Important

Update this readme file to contain the names of each team member and your GitHub login.

* Taranveer Singh - TvSK0
* Gabriel Liu - Gabeliu
* Aum Patel - aumiieee
* Yu Tokuda - silucksi
* [Scott full name] - [Scott's login]

You must also update this file to contain either the instructions for running your code or a link to the instructions.

## Initial Repository Code

This repository initially contains the code for the mainframe interface and the unit tests. The following code is supplied:
* [mainframe](src/main/java/mainframe/client/): the interfaces for sending requests to and receiving responses from the mainframe. You will need to use the [Connection](src/main/java/mainframe/client/Connection.java) interface in your code. We have provided an implementation that connects to a web server in [HttpConnection](src/main/java/mainframe/client/HttpConnection.java).
* [tests](src/test/java/mainframe/client/): the unit tests. Most of these connect to the web server, but there is an [example unit test](src/test/java/mainframe/client/SimulateListCustomers.java) for connecting to a stub.

## Team decisions

<!-- AI-assisted: drafted from the code and the task delegation document; checked by Taran. -->
The five decisions we settled before coding, from our task delegation document.

1. **How a save reaches the mainframe.** Every save is queued first in the local SQLite store, then sent straight away if the mainframe is reachable (`SyncService.queue`). If it is not reachable the change stays queued and is retried on a timer. One background thread sends changes in order, so the screen never waits on the network. A change the mainframe rejects is marked rejected, shown with the mainframe's message and never retried. Application services use one gateway with durable writes; queries go directly to the mainframe, with local reads for unsent customers and loans.
2. **Layer names.** We use the System Model's three layer names: `ui` is the User Interface, `logic` the Business Logic and `data` the Database. Two supporting packages sit beside them: `model` (plain records) and `validation` (`ValidationResult` / `FieldError`). `ui` never imports `Connection`, the mainframe message classes or JDBC, `logic` never imports JavaFX or JDBC, and only `logic` calls `data` (`LavsApp` is the exception, as it builds the services).
3. **UI framework and build tool.** JavaFX for the interface and Maven (with the Maven Wrapper) for the build, with JUnit 5 for tests and a JaCoCo gate of 95% branch coverage.
4. **The guarantor rule.** A main borrower with no employer needs a guarantor on the loan before it can move from New to Pending. It is checked when a loan is submitted for verification (`LoanStatusService`), not when a loan is started, because a new loan has no guarantors yet. Starting a loan only needs a mailing address and a phone number or an email (`EligibilityChecker`).
5. **A duplicate found at upload.** Before a new customer is sent, the upload looks for an existing customer. If one is found the upload pauses (`PAUSED`) and the MMM chooses either the existing customer or "this is a new customer", after which the upload carries on (`SyncService`, `SyncProcessor`). The MMM's buttons for this are in the pending-uploads panel (US11).

<!-- AI-assisted: build/test/run instructions, verified on a fresh clone (Windows 10+, no Maven pre-installed). -->
## Build, test and run

**Requirements:** JDK 17 or later on your `PATH`. Nothing else — this project uses the Maven Wrapper, so you do not need Maven installed; the first command downloads it automatically.

### One command: build, run all tests, produce a coverage report

```
# macOS / Linux / Git Bash
./mvnw clean verify

# Windows (cmd or PowerShell)
mvnw.cmd clean verify
```

This compiles the project, runs the test suite with JUnit 5, and fails the build if branch coverage drops below 95%. The gate counts our own code apart from the JavaFX screens in `lavs.ui` (keep screen rules in small classes with no JavaFX, which are tested) and the starter `mainframe.client` code. Open the human-readable coverage report at:

```
target/site/jacoco/index.html
```

### Just run the tests (no coverage gate)

```
./mvnw test        # or: mvnw.cmd test
```

### Running the full suite, including the live-server tests

Most of the starter tests under `mainframe.client.messages` and `HttpConnectionTests` talk to the shared mainframe test server referenced in [`TestConstants`](src/test/java/mainframe/client/TestConstants.java), rather than a local fake. They are **excluded by default** (see `pom.xml`) so `./mvnw clean verify` is deterministic and doesn't need network access — this is what CI runs. If you have access to that server and want to run those too:

```
./mvnw clean verify -Pfull
```

### Code metrics (coupling, complexity, cohesion)

```
./mvnw -Pmetrics validate        # or: mvnw.cmd -Pmetrics validate
```

This runs CK over our code and writes the numbers for every class and method to `target/metrics/class.csv` and `target/metrics/method.csv`. The "before" baseline for the final report is in [`docs/metrics/baseline-2026-10-23.md`](../docs/metrics/baseline-2026-10-23.md).

### Running the application

LAVS is a JavaFX desktop app. To build it and open the main window:

```
# macOS / Linux / Git Bash
./mvnw javafx:run

# Windows (cmd or PowerShell)
mvnw.cmd javafx:run
```

Maven downloads JavaFX for your operating system automatically; you only need JDK 17 or later.

The default server is the shared class mainframe. Set `LAVS_SERVER_URL` to override it and `LAVS_ORGANISATION` to the organisation number used for new customers (default `1`). For PowerShell:

```powershell
$env:LAVS_SERVER_URL = "https://your-mainframe-server/"
$env:LAVS_ORGANISATION = "1"
.\mvnw.cmd javafx:run
```

Dropdown values are cached in `~/.lavs/lavs.db`. Pending saves are stored separately in `~/.lavs/pending-<user-key>.db`. Keep these pending files: they contain unsent work. Each account resumes only its own queue after login. Login still requires the mainframe; the cache does not grant offline authentication.

## Implemented workflow

Search and page through customers, open or create a customer, then manage details, addresses, phones, emails, employers and relationships. The Loans tab lists that customer's loans. New loans require a mailing address and either a phone or email; offline drafts are checked against current records before upload. The loan screen provides details, repayment information, customer selection for borrowers, and submission for verification. Submission rechecks eligibility, current status and the employer/guarantor rule on upload.

Both customer and loan screens include notes and document metadata (type, file name, file type and byte size). Documents contain metadata only. The API has no document-update operation, so edits save the replacement before removing the original. A refused removal leaves both records and a visible rejection for review. Borrower changes persist their remove/add/restore stage so reconnection can resume the operation.

The top bar shows the last background connection result and pending count. Open Pending uploads to review saved fields, resolve duplicate customers, and inspect or dismiss rejections. A local save is labelled "Waiting to upload" rather than reported as a mainframe success. Accepted customer/loan numbers and dependent queued changes are committed in one SQLite transaction.

A connection loss after the server commits but before its response arrives remains ambiguous: the supplied API exposes no idempotency key or cross-request transaction. The local queue retains work, but automatic retries cannot guarantee exactly-once creation. Check the mainframe for duplicates in that situation. If an accepted customer or loan returns no identifier, the queue pauses for review instead of discarding it.

## UI verification and assets

On Windows, `clean verify` also loads the real JavaFX customer/loan screens and renders their tabs. Screenshots are written to `target/ui-smoke/`. Tests use scripted connections and temporary SQLite databases; the default suite does not write to the shared server. Windows CI uses JDK 17, and compilation uses Java's `--release 17` API check.

No downloaded image or icon assets are used by these screens. Their controls, text, CSS and rendered screenshots are repository-owned. Runtime dependencies remain under their own licences: OpenJFX (GPLv2 with the Classpath Exception), OkHttp and SQLite JDBC (Apache 2.0). Retain dependency notices when distributing a packaged application; the Maven dependency files contain the upstream metadata.

## Working on the code

The plan is in our task delegation document; these are the parts that matter day to day.

- **Packages follow the System Model layers** in `src/main/java/lavs`: `ui` (screens), `logic` (business rules and services), `data` (the local SQLite store and the connection to the mainframe), plus `model` and `validation`. `ui` never imports `Connection`, the mainframe message classes or JDBC, `logic` never imports JavaFX or JDBC, and only `logic` calls `data`.
- **One branch per card**, named like `feature/us4-addresses`, `test/...`, `chore/...` or `docs/...`. No direct commits to `main`.
- **Pull requests** are small, need one approval and a green CI run, and get a screenshot when a screen changes. The PR template has the checklist. The reviewer is the owner of any foundation piece the change touches.
- **Stacked PRs** (a PR whose base is another PR's branch): merge the bottom one first and then delete its branch. GitHub then moves the next PR onto `main` by itself. Merging the top one first lands it in the other branch instead of `main`.
- **Tests go in the same PR as the logic.** A slice isn't done under 95% branch coverage.
- **AI use** is logged in [`AI_INTERACTIONS.md`](../AI_INTERACTIONS.md), newest first, and the code it touched is marked `// AI-assisted: [description]`.
