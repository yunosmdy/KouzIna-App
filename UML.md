# Account design

```mermaid
classDiagram
    class Employee {
        <<abstract>>
        -String id
        -String username
        -String name
        -Credential credential
        -AccountStatus accountStatus
        -long accountRevision
        +isActive() boolean
        +allows(Permission) boolean
        +withRole(Role) Employee
    }
    Employee <|-- Waiter
    Employee <|-- Chef
    Employee <|-- Cashier
    Employee <|-- Manager
    Employee <|-- PendingEmployee
    Employee <|-- Staff : retired serialized identity
    Employee <|-- SharedLogin : retired serialized credential
    AppState o-- Employee
    Employee *-- Credential
    AuthenticationService --> AppStateRepository
    AuthenticationService --> PasswordHasher
    AuthenticationService --> AuthorizationService
    StaffService --> AppStateRepository
    StaffService --> AuthorizationService
    StaffService --> Employee : approve / replace role
    AuthorizationService --> Employee : live token and revision check
    LoginPanel --> AuthenticationService
    AccountsPanel --> StaffService
    AccountSetup --> AppState : one-time normalization
```

```mermaid
flowchart TD
    A[Create account] --> B[Pending and unassigned]
    B --> C{Manager reviews}
    C -->|Reject| D[Rejected]
    C -->|Approve and assign role| E[Active individual account]
    E --> F[Username and password login]
    F --> G[Validate account and issue session token]
    G --> H[Role-specific dashboard]
    H --> I{Operation}
    I --> J[Service validates token, current revision and permission]
    J --> K[Commit operation and audit permanent employee ID]
    E -->|Manager deactivates| L[Inactive]
    L -->|Manager reactivates| E
    H -->|Logout or account change| M[Session ended]
```

The Role enum is an administration input; the Employee subtype remains the source of role
permissions. Role changes create a replacement subtype with the same employee ID, credentials,
name and lifecycle. Relationships from sessions/audits continue using that stable ID.

AccountStatus has Pending, Active, Rejected and Inactive values. New accounts remain Pending until
a Manager assigns a role and approves access. PresetAccounts installs four starter accounts once;
ApplicationServices applies old-file compatibility changes without extra login screens.
