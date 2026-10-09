# Legacy serialization fixtures

These synthetic binary saves were generated with the original pre-account-redesign source.

- individual.dat: old individual Manager username `manager`, Waiter and saved customer.
- shared.dat: shared logins, Manager/Staff profiles, assignment-only waiters, an inactive Staff
  profile and a completed paid restaurant visit with deducted stock.
- renamed-shared.dat: equivalent old data written using the earlier `com.kouzina.*` packages.
- collision.dat: deliberately inconsistent old username collision to verify atomic rejection.
- inactive-shared.dat: previously disabled shared Manager credential; must not open recovery.

The pre-change fixture generator and original source backup were retained in this chat's work
folder during implementation. Fixtures are immutable test inputs; tests copy them before migration.
Passwords such as manager123 are test-fixture credentials only, never enabled by fresh production
sample data. They exist to exercise compatibility with the old application.
