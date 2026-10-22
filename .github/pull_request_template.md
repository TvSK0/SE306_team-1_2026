## What this does

<!-- A few lines: what changes for the MMM or for the team. Add a screenshot for any screen. -->

**Card:** <!-- e.g. US8, or the foundation task -->

## Checklist

- [ ] Tests for each acceptance criterion and edge case are in this PR
- [ ] `mvnw clean verify` passes (95% branch coverage held)
- [ ] Layer rules kept: `ui` doesn't import `Connection`, the message classes or JDBC; `logic` doesn't import JavaFX or JDBC
- [ ] Screenshot added if a screen changed
- [ ] AI use logged in `AI_INTERACTIONS.md` and marked `// AI-assisted`
- [ ] Reviewer is the owner of any foundation piece this touches

<!-- Stacked on another PR? Say "Merge #N first". Merge that one, then delete its
     branch: GitHub then moves this PR onto main by itself. -->
