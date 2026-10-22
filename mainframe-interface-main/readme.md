# Mainframe Interface: Project 2 Starter

Welcome to the second project for SOFTENG 306. 

The project specification is available [on Canvas](https://canvas.auckland.ac.nz/courses/142477/pages/project-2-overview?module_item_id=3007942). The four deliverables for this project are:
* [Proposal](https://canvas.auckland.ac.nz/courses/142477/assignments/514280)
* [Presentation](https://canvas.auckland.ac.nz/courses/142477/assignments/514279)
* [Code](https://canvas.auckland.ac.nz/courses/142477/assignments/514277)
* [Documentation](https://canvas.auckland.ac.nz/courses/142477/assignments/514278)

## Important

Update this readme file to contain the names of each team member and your GitHub login.

* *Replace this text: Team Member Name 1 - GitHub Login*
* *Replace this text: Team Member Name 2 - GitHub Login*
* *Replace this text: Team Member Name 3 - GitHub Login*
* *Replace this text: Team Member Name 4 - GitHub Login*

You must also update this file to contain either the instructions for running your code or a link to the instructions.

## Initial Repository Code

This repository initially contains the code for the mainframe interface and the unit tests. The following code is supplied:
* [mainframe](src/main/java/mainframe/client/): the interfaces for sending requests to and receiving responses from the mainframe. You will need to use the [Connection](src/main/java/mainframe/client/Connection.java) interface in your code. We have provided an implementation that connects to a web server in [HttpConnection](src/main/java/mainframe/client/HttpConnection.java).
* [tests](src/test/java/mainframe/client/): the unit tests. Most of these connect to the web server, but there is an [example unit test](src/test/java/mainframe/client/SimulateListCustomers.java) for connecting to a stub.

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

### Running the application

LAVS is a JavaFX desktop app. To build it and open the main window:

```
# macOS / Linux / Git Bash
./mvnw javafx:run

# Windows (cmd or PowerShell)
mvnw.cmd javafx:run
```

Maven downloads JavaFX for your operating system automatically; you only need JDK 17 or later.

The app talks to the shared class mainframe server set in `LavsApp` (the same one as `TestConstants`). It keeps the dropdown values in a small SQLite file at `~/.lavs/lavs.db` in your user folder, so they still load offline; deleting the file is safe.

## Working on the code

The plan is in our task delegation document; these are the parts that matter day to day.

- **Packages follow the System Model layers** in `src/main/java/lavs`: `ui` (screens), `logic` (business rules and services), `data` (the local SQLite store and the connection to the mainframe), plus `model` and `validation`. `ui` never imports `Connection`, the mainframe message classes or JDBC, `logic` never imports JavaFX or JDBC, and only `logic` calls `data`.
- **One branch per card**, named like `feature/us4-addresses`, `test/...`, `chore/...` or `docs/...`. No direct commits to `main`.
- **Pull requests** are small, need one approval and a green CI run, and get a screenshot when a screen changes. The PR template has the checklist. The reviewer is the owner of any foundation piece the change touches.
- **Stacked PRs** (a PR whose base is another PR's branch): merge the bottom one first and then delete its branch. GitHub then moves the next PR onto `main` by itself. Merging the top one first lands it in the other branch instead of `main`.
- **Tests go in the same PR as the logic.** A slice isn't done under 95% branch coverage.
- **AI use** is logged in [`AI_INTERACTIONS.md`](../AI_INTERACTIONS.md), newest first, and the code it touched is marked `// AI-assisted: [description]`.
