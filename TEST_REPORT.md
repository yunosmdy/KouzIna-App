# Verification report — 2026-10-09

## Current simplified account flow

| Suite | Assertions | Result |
|---|---:|---|
| AccountChecks | 252 | PASS |
| Restaurant, persistence, validation and component regressions | 84 | PASS |
| UiAccountSmoke | 44 | PASS |
| Total | 380 | PASS |

The current requirements replace the previous initial-Manager, migration-wizard and temporary-password workflows. Tests for those removed flows were replaced with preset-account and automatic compatibility checks. Counts represent assertions, not independent workflows.

## Verified behavior

- Login has exactly two buttons: Sign in and Create account.
- Account setup collects full name, username, password and confirmation.
- New accounts remain Pending; an authenticated Manager assigns the role and approves access.
- Additional Managers follow the same registration and approval process.
- Manager, Waiter, Chef and Cashier presets sign in directly with their assigned permissions.
- Existing old shared-profile saves open without a migration dialog; restaurant records remain intact.
- The known old Chef demo password updates to the documented preset; personal passwords are preserved.
- Restarting does not duplicate accounts, restore a changed password or reactivate a disabled preset.
- Service permission checks, session revocation, role changes, audit identities and self-Manager protection remain covered.
- Reservation, stock, ordering, kitchen, billing and payment regressions pass.
- Corrupt saves, conflicting usernames and write failures preserve the source data and backup.

Tests use memory or isolated temporary saves. Swing tests operate actual components on the event dispatch thread with windows off-screen; the simplified login screenshot was inspected. The source compiles with Java 21.

## Saved project verification

The requested one-time account update was also applied to the project save after backing it up as data/kouzina.dat.before-simple-login.bak. All four documented preset credentials were separately verified against that save. Those verification logins create normal login/logout audit events.

The outdated VS Code application instance was closed before finalizing the update. Run Kouzina again to load the new UI.
