# Raising the PR (C2 evidence)

The repository is currently local-only (no git remote configured), so no branch has been pushed.
When you are ready to raise the PR, run these from the repo root:

```bash
cd /home/ronyroy/work/Learning/SSE/prompt-template-registry

# 1. create a feature branch for the work (commits are currently on the default branch)
git switch -c feature/prompt-template-registry

# 2. add your remote (GitHub example) and push
git remote add origin git@github.com:<you>/prompt-template-registry.git
git push -u origin feature/prompt-template-registry

# 3. open the PR (GitHub CLI)
gh pr create \
  --title "Prompt Template Registry + Renderer (C2 SSE evidence)" \
  --body-file specs/001-prompt-template-registry/PR-BODY.md
```

## Suggested PR body

See [`PR-BODY.md`](./PR-BODY.md) — a ready-to-use description linking the spec artifacts and the
acceptance-criteria checklist.

## Notes before pushing

- **Commit author identity:** all commits were authored as
  `Rony Roy <rony.roy@tothenew.com>` via per-commit `-c` flags (global git config was left
  untouched). If you want a different identity on the history, rewrite before pushing, e.g.
  `git rebase -i --exec 'git commit --amend --no-edit --reset-author' <root>` after setting your
  `user.name`/`user.email`, or simply set them and re-commit.
- **No secrets in the repo.** This standalone app has no credentials, `.env`, or `.mcp.json`;
  nothing sensitive to scrub. (The C2 `.eml`/`.xlsx` docs live in the parent directory and are not
  part of this repo.)
- **Nothing to push yet without your action** — this file is instructions only; I did not add a
  remote or push, per the "push only on explicit ask" rule.
