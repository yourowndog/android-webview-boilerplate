# AGENT

## Purpose
This repository accepts changes via a GitHub App instead of direct `git push`.

## App ID
1786337

## Token model
Short-lived installation access tokens (~1 hour) are minted from the App ID and private key. Tokens must never be stored in the repository. Mint a new token if the existing one expires.

## Commands available in this workspace
- `put_single "$TOKEN" <repo_path> <local_path> "msg"`
- `multi_commit "$TOKEN" REPO_PATH=LOCAL_PATH [...] "msg"`

## Standard operating procedure (SOP)
1. Mint or refresh `$TOKEN`.
2. Prepare updated files in the workspace.
3. Use `multi_commit` to commit to `master`.
4. Return the commit SHA, commit URL, and list of files changed.
5. If the GitHub API returns 401, 403, 409, or 422, stop and escalate.

## Audit & reconcile policy
Fetch current file contents before overwriting. If there are significant differences, include a short diff snippet in the report and ask before proceeding.

## Merge conflicts
If the parent commit is behind and GitHub refuses to advance the ref, stop and report the exact API error; do not force.
