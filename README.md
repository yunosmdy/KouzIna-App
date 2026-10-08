# Kóuz 'Inà — Restaurant Management System

## Signing in

1. **Welcome to Kóuz 'Inà!** — choose **Employee** or **Manager**.
2. Log in with the shared login for that side:

   | Portal   | Username   | Password      |
   |----------|------------|---------------|
   | Employee | `employee` | `employee123` |
   | Manager  | `manager`  | `manager123`  |

3. **Would you like to log in as** — click your profile (like Netflix).
   - Employee profiles: Jael Castillo, Lui Vence, Gian Liit, Vera Malinao
   - Manager profiles: Jessie James, Jerald Anderson
   - **Add profile** makes a new one. **Manage profiles** changes a photo or removes a profile.

The username is only the shared login. The name shown in the app (sidebar, greeting,
audit log) is the profile you picked. Use **Switch profile** in the sidebar to change person.

The old individual logins still work for testing roles: `waiter`/`waiter123`,
`chef`/`chef123`, `cashier`/`cashier123` (Employee portal).

## Moving around

- The **sidebar** is always visible (shortcuts F1–F6).
- Guests & Tables, Orders, Kitchen, and Billing have a **step bar** at the top:
  `← Back to …`   1 Guests & Tables › 2 Orders › 3 Kitchen › 4 Billing   `Go to … →`
  The selected order comes along, so the next screen opens on the same order.

## Fixing mistakes (retry)

- **Billing → Create bill…** opens a pop-up with the ordered items and a live total.
  A wrong service charge is shown in red and must be fixed before saving.
- **Billing → Change bill…** corrects the discount or service charge of a bill that is not paid yet.
- **Billing → Take payment…** shows the change while you type the cash received.
  Too little cash, letters, or an unusually large amount are caught *before* anything is saved;
  the pop-up stays open so the amount can be fixed and tried again.
- **Orders → Add to order** on a dish that is already in the order asks whether to add more or change
  the amount. Not enough stock? It offers what is left.
- **Orders → Send to kitchen** shows the whole order first ("Go back and edit" / "Send to kitchen").
- **Orders → Take back order** brings a sent order back to "Taking order" (stock is returned)
  as long as the kitchen has not started cooking it.
- Remove item and Mark served ask first.

## Scrolling

On small or zoomed-in screens every screen scrolls (scrollbars + mouse wheel). The mouse wheel
works over tables too: when a table reaches its end, the page keeps scrolling.

## Roles

| Role    | Can do |
|---------|--------|
| Staff (employee profiles) | Guests & Tables, Orders, Kitchen, Billing |
| Manager | Everything, plus Manager Tools (menu & stock, staff & profiles, reports, audit log) and cancelling orders already sent to the kitchen |
| Waiter / Chef / Cashier (old individual logins) | Only their own screens |

Waiters you can assign to tables: Jessie James, Kurt Pangan, Jerald Anderson,
Lebron James, Reign Magtaca, Wally Waiter. Managers can add more in
**Manager Tools → Staff & Profiles**.

## Photos

See `photos/README.txt` — profile photos, menu pictures, and `logo.png`.

## Folder layout (each folder matches its Java package)

    src/        KouzinaApp.java + bootstrap, domain, domain/enums, exception,
                persistence, security, service, ui, util
    test/ui/    WorkflowChecks.java (automated checks)
    data/       kouzina.dat save file (created on first run)
    photos/     profile pictures, menu/ pictures, logo.png
    .vscode/    VS Code settings and run configurations

## Running

- **VS Code:** File → Open Folder → this `Kouzina` folder. Run and Debug → **Run Kouzina** → F5.
  **Run checks (tests)** runs the automated checks.
- **Without VS Code:** double-click `Run.bat` (app) or `Run tests.bat` (checks).
- Needs **JDK 21** (`javac -version` should say 21).

To start over with fresh sample data, close the app and delete `data/kouzina.dat`.
