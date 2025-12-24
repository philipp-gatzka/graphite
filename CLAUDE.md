# Development Workflow Rules – Graphite Project

These rules are **non-negotiable** and apply to all work in this repository.

---

## 1. Issue Management

### 1.1 Issue Requirements

Every issue **MUST** contain:

- **User Story**: `As a [role], I want [feature], so that [benefit]`
- **Description**: Brief technical explanation of the task
- **Acceptance Criteria**: Numbered checklist that **MUST** include:
  - `[ ] Implemented code is covered with automated tests`
  - `[ ] Documentation has been updated`
  - *(Additional task-specific criteria)*
- **Labels** (required):
  - Size: `size/xs`, `size/s`, `size/m`, `size/l`, `size/xl`
  - Complexity: `complexity/trivial`, `complexity/simple`, `complexity/moderate`, `complexity/complex`
- **Labels** (encouraged):
  - Area: `area/ui`, `area/backend`, `area/api`, `area/database`, `area/infra`, `area/docs`, `area/testing`
  - Type: `type/feature`, `type/bugfix`, `type/refactor`, `type/chore`

### 1.2 Issue Granularity

- Issues must be **atomic**: one logical change per issue
- Forbidden: umbrella issues like "implement user registration"
- Preferred: `Create user registration form`, `Add email validation endpoint`, `Implement password hashing service`
- If an issue grows beyond `size/l`, split it into sub-issues

### 1.3 Issue Lifecycle

```
Open → In Progress → In Review → Done
```

- Assign yourself when starting work
- Link the PR to the issue
- Close issues only via merged PR (use `Closes #<id>` in PR description)

---

## 2. Branching Strategy

### 2.1 Branch Rules

- **One branch per issue** – no exceptions
- Branch deleted immediately after merge
- No direct commits to `main` – enforced via branch protection
- All changes must go through Pull Request

### 2.2 Branch Naming Convention

```
<issue-id>-<issue-title-normalized>
```

- Normalize: lowercase, spaces become hyphens, remove special characters
- Max 50 characters (truncate if needed)
- Examples:
  - Issue #42 "Add login button" → `42-add-login-button`
  - Issue #7 "Fix: API returns 500 on empty payload" → `7-fix-api-returns-500-on-empty-payload`

---

## 3. Commit Standards

### 3.1 Commit Message Format

```
#<issue-id> <message-in-lowercase>
```

- Message must be lowercase
- Max 72 characters for first line
- Reference the issue number with `#` prefix

### 3.2 Atomic Commits (Non-Negotiable)

- **One logical change per commit**
- **One file per commit** when possible
- 30 file changes = 30 commits, not 1 commit
- Each commit must be:
  - Self-contained
  - Independently understandable
  - Buildable (no broken intermediate states)
- Examples of proper atomicity:
  - `#42 add user model`
  - `#42 add user repository interface`
  - `#42 add user repository implementation`
  - `#42 add user service`
  - `#42 add user controller`
  - `#42 add user model tests`
  - `#42 add user repository tests`
  - `#42 add user service tests`
  - `#42 update api documentation`

### 3.3 Commit Message Enforcement

A `commit-msg` git hook validates:

- Message starts with `#<number> `
- Message body is lowercase (after the issue reference)
- Non-compliant commits are rejected

---

## 4. Pull Request Workflow

### 4.1 PR Requirements

- Title: `#<issue-id> <description>`
- Description must include:
  - Link to issue (`Closes #<id>`)
  - Summary of changes
  - Testing notes (if applicable)
  - Screenshots (for UI changes)

### 4.2 PR Lifecycle

```
1. Create issue
2. Create branch from main
3. Implement changes (atomic commits)
4. Push branch
5. Open PR
6. Request review
7. Address feedback
8. Receive approval
9. Merge (squash or rebase)
10. Delete branch
11. Verify CI on main
12. If CI fails → create follow-up issue immediately
```

### 4.3 PR Merge Requirements (Non-Negotiable)

A Pull Request **MUST NOT** be merged unless:

- **ALL** acceptance criteria are checked off, including:
  - `[x] Implemented code is covered with automated tests`
  - `[x] Documentation has been updated`
- Minimum 1 approval received
- All CI checks pass
- No unresolved conversations

**Merging with unchecked acceptance criteria is strictly forbidden.**

---

## 5. CI/CD Requirements

### 5.1 CI Checks

- Lint
- Type check
- Unit tests
- Build verification

### 5.2 Post-Merge Verification

- After every merge, verify CI passes on `main`
- If CI fails: **immediately** create a follow-up issue with:
  - Label: `priority/critical`, `type/bugfix`
  - Reference to the PR that broke CI
  - Error logs/details

---

## 6. Branch Protection Rules

Configured on `main`:

- Require pull request before merging
- Require at least 1 approval
- Require status checks to pass
- Require branches to be up to date
- Do not allow bypassing settings
- Automatically delete head branches

---

## 7. Forbidden Actions

- Direct commits to `main`
- Force pushing to `main`
- Merging without approval
- Merging with unchecked acceptance criteria
- Merging without test coverage
- Merging without documentation updates
- Creating issues without required fields
- Creating issues without mandatory acceptance criteria
- Multiple branches for one issue
- Commits without issue reference
- Bulk commits (multiple unrelated changes in one commit)
- Multi-file commits when separate commits are possible
- Leaving failed CI unaddressed

---

## 8. Claude Workflow

When implementing changes, Claude will:

1. Create a properly formatted GitHub issue
2. Create a branch following the naming convention
3. Implement changes with atomic commits
4. Push and create a PR
5. Wait for user approval
6. Merge upon approval
7. Verify CI status
8. Create follow-up issue if CI fails
