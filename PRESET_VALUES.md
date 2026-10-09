# Kouzina preset values

Verified 2026-10-09. Chef is Noning Ry and Cashier is Iruma in both the current save and fresh defaults.

## Accounts in your current save

| Role | Full name | Username | Preset password | Status |
|---|---|---|---|---|
| Manager | Jessie James | manager | manager123 | Active |
| Waiter | Wally Waiter | waiter | waiter123 | Active |
| Chef | Noning Ry | chef | chef12345 | Active |
| Cashier | Iruma | cashier | cashier123 | Active |

Fresh installations use Restaurant Manager and Restaurant Waiter as the first two names. All four accounts have separate roles. Presets install once; later password, role, and status changes are preserved.

## Menu defaults

These are the starting menu values defined in code, not a statement of current stock after orders. All items start Active.

| ID | Item | Type | Portion / serving | Price (PHP) | Starting stock |
|---|---|---|---|---:|---:|
| menu-1 | Classic Burger | Food | Single serving | 185.00 | 30 |
| menu-2 | Chicken Alfredo | Food | Single serving | 245.00 | 24 |
| menu-3 | Beef Tapa | Food | Rice meal | 220.00 | 20 |
| menu-4 | Grilled Salmon | Food | Single serving | 365.00 | 14 |
| menu-5 | Margherita Pizza | Food | 10-inch pizza | 280.00 | 18 |
| menu-6 | Caesar Salad | Food | Sharing bowl | 195.00 | 20 |
| menu-7 | Pork Sisig | Food | Sharing plate | 250.00 | 22 |
| menu-8 | Garlic Rice | Food | One cup | 55.00 | 50 |
| menu-9 | Chocolate Cake | Food | One slice | 120.00 | 16 |
| menu-10 | Halo-Halo | Food | Regular bowl | 145.00 | 18 |
| menu-11 | French Fries | Food | Sharing basket | 130.00 | 28 |
| menu-12 | Mushroom Soup | Food | Regular bowl | 110.00 | 20 |
| menu-13 | Iced Tea | Beverage | 450 ml, cold | 75.00 | 40 |
| menu-14 | Lemonade | Beverage | 450 ml, cold | 85.00 | 36 |
| menu-15 | Cola | Beverage | 330 ml, cold | 65.00 | 48 |
| menu-16 | Orange Juice | Beverage | 350 ml, cold | 95.00 | 24 |
| menu-17 | Brewed Coffee | Beverage | 250 ml, hot | 90.00 | 30 |
| menu-18 | Hot Chocolate | Beverage | 300 ml, hot | 105.00 | 24 |
| menu-19 | Bottled Water | Beverage | 500 ml, cold | 45.00 | 60 |
| menu-20 | Mango Shake | Beverage | 450 ml, cold | 125.00 | 20 |

## Table defaults

All tables start Available.

| ID | Table number | Seats |
|---|---:|---:|
| table-1 | 1 | 2 |
| table-2 | 2 | 2 |
| table-3 | 3 | 4 |
| table-4 | 4 | 4 |
| table-5 | 5 | 4 |
| table-6 | 6 | 6 |
| table-7 | 7 | 6 |
| table-8 | 8 | 8 |
| table-9 | 9 | 8 |
| table-10 | 10 | 10 |

## Customer defaults

| ID | First name | Last name | Phone |
|---|---|---|---|
| customer-1 | Sairus | Andurei | 0917-555-0101 |
| customer-2 | Yunos | Mads | 0917-555-0102 |
| customer-3 | Alex | Lewis | 0917-555-0103 |

## Account setup values

- Full name: 1–80 characters; separate from username.
- Username: 3–30 characters, starting with a letter or digit; letters, digits, dots, underscores and hyphens. Stored lowercase and must be unique.
- Personal password: 8–128 characters, case-sensitive; confirmation must match.
- Newly created account: Pending, Unassigned; no operational access.
- Manager approval: Manager selects Waiter, Chef, Cashier, or Manager and approves access.
- Account statuses: Pending, Active, Rejected, Inactive.
- Customers have no login accounts.

## Other initial data

A fresh installation starts without reservations, waitlist entries, dining sessions, orders, bills, payments, or audit events. Activity creates these records; existing saved activity is retained.

## Historical profiles retained in your save

These are older records, not additional active preset logins: Jael Castillo, Lui Vence, Gian Liit, Vera Malinao (Staff); Jerald Anderson (Manager); Jessie James, Kurt Pangan, Jerald Anderson, Lebron James, Reign Magtaca (Waiter). Retired shared Employee and Manager login records also remain for compatibility.

The latest name update has a backup at data/kouzina.dat.before-preset-names.bak. Reopen Kouzina to load the new names.
