# AI interaction log

Per the team's shared requirement to log AI interactions and mark affected
code with `// AI-assisted: [description]`. Core UI and business logic is
written by hand; entries below are the exception, each noted as such.

Add an entry per session/PR, newest first.

---



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
