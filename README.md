Kouz Ina Restaurant Management System

Java 21 desktop restaurant application with individual employee accounts and manager-approved roles.

## Run

Open this exact project folder in VS Code (the folder containing `src`, `test`, and `.vscode`).
Use **Run and Debug â†’ Run Kouzina**, or double-click **Run.bat**.
Without VS Code: `powershell -ExecutionPolicy Bypass -File .\Run.ps1`.
JDK 21 must be on PATH (`javac -version`). No external dependencies are required.

## Login and account creation

The login screen has two buttons: **Sign in** and **Create account**.

Preset accounts are ready for the classroom demo:

| Role | Default full name | Username | Password |
|---|---|---|---|
| Manager | Restaurant Manager | manager | manager123 |
| Waiter | Restaurant Waiter | waiter | waiter123 |
| Chef | Noning Ry | chef | chef12345 |
| Cashier | Iruma | cashier | cashier123 |

Existing saves retain their Manager/Waiter names. See PRESET_VALUES.md for the complete preset inventory.

For a personal account: **Create account → Account setup → Manager gives access**.
Enter full name, username, password, and confirmation. The account stays Pending until a Manager
opens **Manager Tools → Accounts**, selects it, assigns the job, and clicks **Approve**.
New Managers use exactly the same form and approval process. The preset Manager provides initial access.
There is no migration wizard, first-Manager setup, profile picker, or temporary-password screen.

## Individual accounts

The permanent employee ID links records. The unique username is used for login. The full name is
used for greetings, staff lists, and audit history. Two people can share a full name.

Usernames have 3â€“30 ASCII letters, digits, dots, underscores or hyphens, and begin with a letter or
digit. Usernames are trimmed and case-insensitive; passwords retain their exact characters.
Passwords have 8â€“128 characters and must match their confirmation. Saved credentials use salted
PBKDF2 hashes. Password work runs in background workers so Swing remains responsive.

Account states are Pending, Active, Rejected and Inactive. Only an approved Active account with
personal credentials can work. Rejected, inactive and retired usernames remain reserved.

Managers can approve/reject requests, change roles, and deactivate/reactivate approved accounts. A Manager cannot deactivate or demote their own account, and the final
active Manager is protected. Role/status changes invalidate existing sessions. Logout revokes
the session, closes dialogs and discards its screens. Each login builds screens for that role.

## Roles

| Role | Access |
|---|---|
| Waiter | Customers, reservations, walk-ins, seating, draft/confirm orders, mark Ready orders Served |
| Chef | Kitchen queue; Confirmed â†’ Preparing â†’ Ready |
| Cashier | Bills and payments for eligible served orders |
| Manager | All operations, menu/stock, reports, audit, accounts, confirmed-order cancellation |

Sidebar items, dashboard cards, workflow links and F1â€“F6 shortcuts respect the role. Services
independently require a valid session and permission. A predictable employee ID alone cannot
authorize an operation. New waiter assignments use active individual Waiter accounts.

## Saved data

The app uses `data/kouzina.dat` in the project folder, including when run from `src` or `out`.
Existing restaurant records are retained. Automatic format updates make an exact backup first;
the simple-account update uses `kouzina.dat.before-simple-login.bak`.

Preset accounts are installed once. Restarting does not duplicate them or reset changed passwords,
roles, or deactivations. Existing personal accounts keep their credentials; the known old Chef demo
password is upgraded from `chef123` to `chef12345`. Old shared logins are retired. The old default
Manager profile keeps its ID when converted into the preset Manager. Other historical profiles remain
stored for old records but do not appear as account requests. Their owners use Create account.
Any already-assigned temporary password is accepted as that account's password without an extra screen.
Names are never used to merge identities. Unreadable saves and username collisions produce errors
instead of resetting restaurant data.

## Restaurant workflow

Waiter: register/seat guests â†’ draft and confirm order.
Chef: start preparation â†’ mark Ready.
Waiter: mark Served.
Cashier: issue bill â†’ accept cash or simulated electronic payment.
Successful payment closes the visit, completes its reservation when applicable, and releases the
table. Existing stock, billing, retry and transaction rules are preserved.

Use **Change bill** to correct an unpaid bill, or **Take back order** before preparation to fix
an order. Manager cancellation rules continue to apply. Menu/stock, reports and audit remain in
Manager Tools. Pictures remain in `photos/`; use a picture named after the person's full name.

## Verification

Double-click **Run tests.bat**, use **Run checks (tests)** in VS Code, or run
`powershell -ExecutionPolicy Bypass -File .\Run.ps1 -Test`.
The launcher locates the legacy fixtures even when invoked from another working directory.

The normal test run covers accounts, permissions, persistence, and restaurant workflows.
For real Swing component checks, run `java -Djava.awt.headless=false -cp out ui.UiAccountSmoke`
from the project folder after `Run.ps1 -Test`. Tests use isolated data.

## Limits

This is a local, single-process university prototype. Protect the computer and save-file access;
it is not designed to resist a person editing Java code or replacing local files. Accounts have
no email verification, network service, automatic password recovery, or online payment processing.
Closing the application ends its in-memory sessions; an abrupt termination may omit a final logout
audit event. Use the same data file in only one application process at a time.

