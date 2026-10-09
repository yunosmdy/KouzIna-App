# Individual accounts and authorization requirements

Implemented 2026-10-09 for the existing KÃ³uz 'InÃ  Java 21 application.

## Identity

- A permanent employee ID is the reference used by transactions and audit records.
- Full name and login username are separate fields. Names may repeat; usernames may not.
- Usernames are case-insensitive and normalized using Locale.ROOT; passwords are exact.
- PBKDF2 credentials contain salt, derived hash and iteration count, never a stored raw password.

## Lifecycle and roles

- Public registration produces a PendingEmployee with Pending status and no operational role.
- Only an authenticated Manager can approve, reject, assign/change roles, deactivate/reactivate
  approved accounts.
- Approval and role assignment commit together. Rejected accounts cannot bypass approval through
  reactivation. Usernames remain reserved across statuses.
- Waiter, Chef, Cashier and Manager subclasses define their permissions. Role is not a user-selected
  login option. The Role enum selects a subtype during a Manager-authorized change.
- Pending, Rejected and Inactive accounts have no normal access. Retired Staff and SharedLogin have no permissions.
- Managers cannot remove their own Manager access or the final active Manager.

## Authentication boundary

- Login returns display identity and a random process-local session token.
- Services accept the token, resolve the person in current state and verify account revision,
  active state and required permission. An employee ID alone is not authentication.
- Role, status and credential changes increment the account revision and invalidate old tokens.
- Logout revokes the token even if saving its audit event fails.
- UI screen callbacks belong to one login. Logout closes dialogs and destroys screen state.
- Menus, dashboard cards, workflow navigation and shortcuts follow the authenticated role.

## Simple setup and preset accounts

- Login shows Sign in and Create account only.
- Create account opens Account setup: full name, username, password and confirmation.
- Every applicant, including future Managers, waits for an existing Manager to assign a role and approve.
- Manager, Waiter, Chef and Cashier presets work immediately; credentials are documented in README.
- Install presets once. Do not reset personal passwords, roles or deactivations on restart.
- Back up saved data before compatibility changes; retain restaurant records and historical identities.
- Convert the old default Manager automatically; no user-facing migration or temporary-password workflow.
- Preserve other old profiles as historical records; their owners register through the normal form.
- Conflicting usernames, corruption and disk failures must not reset restaurant data.

## Audit and operations

- Account approvals and changes identify the acting Manager and target employee ID.
- Registration is recorded as an applicant request, never as self-approval.
- Successful normal login/logout is audited when observable. Password material is excluded.
- New waiter assignments require active individual Waiter accounts. Role changes do not destroy
  historical waiter assignments or prevent completion of an existing restaurant visit.
- Existing restaurant ordering, stock, reservation and billing rules remain covered by regressions.
