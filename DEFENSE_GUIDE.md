# Explaining the account redesign

**Why did you remove the profile picker?** A shared password verifies knowledge of one restaurant
credential. It does not verify which person's name someone clicks. Individual passwords link an
authenticated account to each employee's actions.

**Why are username, name and ID different?** The username is a unique login identifier; the name is
what people read. The permanent ID connects historical records even if credentials or roles change.
Two employees may have the same name, so names must never be used to merge records.

**Who assigns the role?** A Manager approves a Pending registration and assigns the job in one
transaction. Registration itself grants no role. Selecting a portal no longer grants permissions.

**What if someone calls a service directly?** Services require a live random session token and check
the current account revision and permission. A known employee ID, hidden button or old UI callback
is insufficient. Logout revokes the token. Account changes invalidate previously issued tokens.

**Where is OOP demonstrated?** Employee encapsulates identity and credential state and defines the
abstract permission contract. Waiter, Chef, Cashier and Manager inherit it and override permissions.
AuthenticationService, StaffService and AuthorizationService separate authentication, administration
and authorization. AppStateRepository abstracts memory/file storage. Enums describe status and
administrative role choices. Existing factory/observer patterns in restaurant modules remain.

**How does a role change preserve records?** Employee.withRole creates the new subtype using the
same employee ID and credential, copies account state, and advances its revision. AppState replaces
the employee under the same key. Previous orders, dining sessions and audit entries still refer to
that ID. New waiter assignments exclude someone whose current job is no longer Waiter.

**How is the first Manager available?** Four preset accounts support immediate classroom use.
The preset Manager approves personal registrations, including additional Managers. All applicants
use the same Account setup form; none can grant themselves a job or access.

**What happens to older data?** Startup backs up and updates it automatically. The old default Manager
becomes the preset Manager under the same ID. Other old profiles remain in history; their owners
register personal accounts through Create account. No migration wizard is shown.

**Why preset accounts?** They make demonstrations and initial access straightforward. The new-account
workflow still requires Manager approval. Restarting never resets a changed password or deactivated account.

**What protects data during conversion?** An exact backup precedes conversion. Changes occur on a
copy, save together, and carry an account version marker. Repeat runs do not reset records. Genuine
pre-change save bytes, earlier package names, collision failures and failed writes were tested.

**Why retain Staff and SharedLogin classes?** Java serialization needs their types to read existing
files. They grant no operational permissions and are not offered as new account types. Their role
is compatibility, not an alternative authentication path.

**Is this production security?** It is a local university prototype. It uses salted password hashes,
individual credentials and service permissions, but assumes the computer and save files are protected.
It has no online verification or automatic password recovery and is intended for one process.

## Short demonstration

1. Sign in with the preset Manager (`manager` / `manager123`), then log out to demonstrate registration.
2. Register a Waiter, Chef and Cashier; show that each is Pending and Unassigned.
3. Sign in as Manager, approve each and assign the correct job.
4. Sign in as Waiter: show that Manager Tools, Kitchen and Billing are unavailable.
5. Seat a guest and confirm an order. Sign in as Chef to prepare it and mark Ready.
6. Sign in as Waiter to mark Served, then Cashier to bill/pay and release the table.
7. Show the audit: each action belongs to the authenticated person's permanent identity.
8. Deactivate an employee and demonstrate that login and an existing session lose access.

Preset credentials and run instructions are in README.md.
