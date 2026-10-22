# LAVS: Loan Application and Verification System

SOFTENG 306 Project 2, team 1. A JavaFX desktop app for the MMM to manage customers and loans on the bank's mainframe, including while offline.

The app and its tests are in [`mainframe-interface-main/`](mainframe-interface-main/). Its [readme](mainframe-interface-main/readme.md) has the team, the decisions we made, how to build, test and run it, and how we work on the code.

Quick start, with JDK 17 or later:

```
cd mainframe-interface-main
mvnw.cmd clean verify     # Windows: build, run the tests and check coverage
mvnw.cmd javafx:run       # Windows: start the app
./mvnw clean verify       # macOS, Linux or Git Bash
```

AI use is logged in [`AI_INTERACTIONS.md`](AI_INTERACTIONS.md).

Test results, coverage, acceptance tests and code metrics are in the [Quality Report](docs/quality-report.md) (draft).
