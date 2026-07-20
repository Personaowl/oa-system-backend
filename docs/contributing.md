# Contributing

## Branches

- `main`: protected demo-ready branch
- `develop`: daily integration branch
- `feature/<issue>-<name>`: feature work
- `fix/<issue>-<name>`: bug fixes
- `docs/<issue>-<name>`: documentation-only work

## Pull requests

- Link an issue and list acceptance criteria.
- Include test commands and any API, SQL or Nacos configuration changes.
- Require at least one review.
- Prefer squash merge with a Conventional Commit title.

Examples:

```text
feat(attendance): support employee check-in
fix(gateway): reject expired access tokens
docs(user): document RBAC permission codes
```

## Definition of done

A change is complete only when it builds, tests pass, API documentation is updated, secrets are absent, and the feature is callable through Gateway.

