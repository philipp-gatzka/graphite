# GitHub Repository Setup

After the first push to `main`, run this command to enable branch protection:

```bash
gh api repos/philipp-gatzka/graphite/branches/main/protection \
  -X PUT \
  -H "Accept: application/vnd.github+json" \
  --input .github/branch-protection.json
```

## Required Labels

Create these labels in the repository:

### Size Labels
- `size/xs` - Extra small (< 1 hour)
- `size/s` - Small (1-4 hours)
- `size/m` - Medium (1-2 days)
- `size/l` - Large (3-5 days)
- `size/xl` - Extra large (> 1 week, consider splitting)

### Complexity Labels
- `complexity/trivial` - No thought required
- `complexity/simple` - Straightforward implementation
- `complexity/moderate` - Requires some design decisions
- `complexity/complex` - Significant technical challenges

### Type Labels
- `type/feature` - New functionality
- `type/bugfix` - Bug fix
- `type/refactor` - Code improvement
- `type/chore` - Maintenance task

### Area Labels (Optional)
- `area/ui` - User interface
- `area/backend` - Backend logic
- `area/api` - API endpoints
- `area/database` - Database changes
- `area/infra` - Infrastructure
- `area/docs` - Documentation
- `area/testing` - Test infrastructure

### Priority Labels
- `priority/critical` - Must be fixed immediately
- `priority/high` - High priority
- `priority/medium` - Normal priority
- `priority/low` - Low priority

## Repository Settings

Enable these settings in GitHub:
1. Settings → General → Pull Requests:
   - [x] Allow squash merging
   - [x] Allow rebase merging
   - [ ] Allow merge commits
   - [x] Automatically delete head branches

2. Settings → Branches → Add rule for `main`:
   - [x] Require a pull request before merging
   - [x] Require approvals (1)
   - [x] Dismiss stale pull request approvals
   - [x] Require status checks to pass before merging
   - [x] Require branches to be up to date before merging
   - [x] Require linear history
   - [x] Do not allow bypassing the above settings
