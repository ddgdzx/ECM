# ECM development rules

- Any user-facing feature, behavior, data-model, or UI change must be implemented for both `FOR Android` and `FOR IOS` in the same task unless the user explicitly limits the change to one platform.
- Keep Android and iOS wording, workflows, persistence behavior, and interaction semantics aligned.
- Before handing off a change, verify both platform builds. Preserve existing user data when changing either database schema.
