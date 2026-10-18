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

This compiles the project, runs the test suite with JUnit 5, and fails the build if logic (branch) coverage of the team's own code drops below 95%. Open the human-readable coverage report at:

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
