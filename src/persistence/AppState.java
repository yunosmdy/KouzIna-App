package persistence;

import domain.AuditLog;
import domain.Bill;
import domain.Customer;
import domain.DiningSession;
import domain.Employee;
import domain.MenuItem;
import domain.Order;
import domain.Payment;
import domain.Reservation;
import domain.RestaurantTable;
import domain.WaitlistEntry;
import exception.ConflictException;
import exception.ValidationException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Dito yung buong saved state ng restaurant.
 * Di pwede palitan yung records gamit yung collection views,
 * pero pwede i-update ng services yung objects sa loob.
 */
public final class AppState implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public static final int CURRENT_SCHEMA_VERSION = 1;

    private final int schemaVersion;
    private int accountVersion = 2;
    private String securityId = java.util.UUID.randomUUID().toString();
    private boolean initialManagerSetupAllowed = true;
    private String recoveryLoginId;
    private boolean presetAccountsInstalled;
    public boolean hasPresetAccounts() { return presetAccountsInstalled; }
    public void completePresetAccounts() {
        presetAccountsInstalled = true;
        initialManagerSetupAllowed = false;
        recoveryLoginId = null;
    }

    public int getAccountVersion() { return accountVersion; }
    public String getSecurityId() { return securityId; }
    public boolean isInitialManagerSetupAllowed() { return initialManagerSetupAllowed; }
    public void completeInitialManagerSetup() { initialManagerSetupAllowed = false; }
    public String getRecoveryLoginId() { return recoveryLoginId; }
    public void finishAccountMigration(String recoveryId) {
        accountVersion = 2; securityId = java.util.UUID.randomUUID().toString();
        initialManagerSetupAllowed = false; recoveryLoginId = recoveryId;
    }
    public void completeManagerRecovery() { recoveryLoginId = null; }
    public void replaceEmployee(Employee employee) {
        getEmployeeOrThrow(employee.getId());
        boolean duplicate = employees.values().stream().anyMatch(e -> !e.getId().equals(employee.getId())
                && e.getUsername().equalsIgnoreCase(employee.getUsername()));
        if (duplicate) throw new ConflictException("Username already exists.");
        employees.put(employee.getId(), employee);
    }
    /** Permanently removes an account. Callers check that it is safe to remove first. */
    public void removeEmployee(String id) {
        getEmployeeOrThrow(id);
        employees.remove(id);
    }
    private final LinkedHashMap<String, Employee> employees;
    private final LinkedHashMap<String, Customer> customers;
    private final LinkedHashMap<String, RestaurantTable> tables;
    private final LinkedHashMap<String, MenuItem> menuItems;
    private final LinkedHashMap<String, Reservation> reservations;
    private final LinkedHashMap<String, WaitlistEntry> waitlistEntries;
    private final LinkedHashMap<String, DiningSession> sessions;
    private final LinkedHashMap<String, Order> orders;
    private final LinkedHashMap<String, Bill> bills;
    private final LinkedHashMap<String, Payment> payments;
    private final LinkedHashMap<String, AuditLog> auditLogs;

    public AppState() {
        schemaVersion = CURRENT_SCHEMA_VERSION;
        employees = new LinkedHashMap<>();
        customers = new LinkedHashMap<>();
        tables = new LinkedHashMap<>();
        menuItems = new LinkedHashMap<>();
        reservations = new LinkedHashMap<>();
        waitlistEntries = new LinkedHashMap<>();
        sessions = new LinkedHashMap<>();
        orders = new LinkedHashMap<>();
        bills = new LinkedHashMap<>();
        payments = new LinkedHashMap<>();
        auditLogs = new LinkedHashMap<>();
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public Collection<Employee> getEmployees() {
        return unmodifiableValues(employees);
    }

    public Collection<Customer> getCustomers() {
        return unmodifiableValues(customers);
    }

    public Collection<RestaurantTable> getTables() {
        return unmodifiableValues(tables);
    }

    public Collection<MenuItem> getMenuItems() {
        return unmodifiableValues(menuItems);
    }

    public Collection<Reservation> getReservations() {
        return unmodifiableValues(reservations);
    }

    public Collection<WaitlistEntry> getWaitlistEntries() {
        return unmodifiableValues(waitlistEntries);
    }

    public Collection<DiningSession> getSessions() {
        return unmodifiableValues(sessions);
    }

    public Collection<Order> getOrders() {
        return unmodifiableValues(orders);
    }

    public Collection<Bill> getBills() {
        return unmodifiableValues(bills);
    }

    public Collection<Payment> getPayments() {
        return unmodifiableValues(payments);
    }

    public Collection<AuditLog> getAuditLogs() {
        return unmodifiableValues(auditLogs);
    }

    public Employee getEmployeeOrThrow(String id) {
        return getOrThrow(employees, id, "Employee");
    }

    public Customer getCustomerOrThrow(String id) {
        return getOrThrow(customers, id, "Customer");
    }

    public RestaurantTable getTableOrThrow(String id) {
        return getOrThrow(tables, id, "Table");
    }

    public MenuItem getMenuItemOrThrow(String id) {
        return getOrThrow(menuItems, id, "Menu item");
    }

    public Reservation getReservationOrThrow(String id) {
        return getOrThrow(reservations, id, "Reservation");
    }

    public WaitlistEntry getWaitlistEntryOrThrow(String id) {
        return getOrThrow(waitlistEntries, id, "Waitlist entry");
    }

    public DiningSession getSessionOrThrow(String id) {
        return getOrThrow(sessions, id, "Dining session");
    }

    public Order getOrderOrThrow(String id) {
        return getOrThrow(orders, id, "Order");
    }

    public Bill getBillOrThrow(String id) {
        return getOrThrow(bills, id, "Bill");
    }

    public Payment getPaymentOrThrow(String id) {
        return getOrThrow(payments, id, "Payment");
    }

    public AuditLog getAuditLogOrThrow(String id) {
        return getOrThrow(auditLogs, id, "Audit log");
    }

    public Optional<Employee> findEmployeeByUsername(String username) {
        String normalizedUsername = requireText(username, "Username");
        return employees.values().stream()
                .filter(employee -> employee.getUsername().equalsIgnoreCase(normalizedUsername))
                .findFirst();
    }

    public Optional<DiningSession> findOpenSessionByTable(String tableId) {
        String normalizedTableId = requireId(tableId, "Table");
        return sessions.values().stream()
                .filter(DiningSession::isOpen)
                .filter(session -> session.getTableId().equals(normalizedTableId))
                .findFirst();
    }

    public Optional<DiningSession> findSessionByReservation(String reservationId) {
        String normalizedReservationId = requireId(reservationId, "Reservation");
        return sessions.values().stream()
                .filter(session -> session.getReservationId()
                        .map(normalizedReservationId::equals)
                        .orElse(false))
                .findFirst();
    }

    public Optional<Order> findOrderBySession(String sessionId) {
        String normalizedSessionId = requireId(sessionId, "Dining session");
        return orders.values().stream()
                .filter(order -> order.getSessionId().equals(normalizedSessionId))
                .findFirst();
    }

    public Optional<Bill> findBillBySession(String sessionId) {
        String normalizedSessionId = requireId(sessionId, "Dining session");
        return bills.values().stream()
                .filter(bill -> bill.getSessionId().equals(normalizedSessionId))
                .findFirst();
    }

    public Optional<Payment> findPaymentByBill(String billId) {
        String normalizedBillId = requireId(billId, "Bill");
        return payments.values().stream()
                .filter(payment -> payment.getBillId().equals(normalizedBillId))
                .findFirst();
    }

    public void addEmployee(Employee employee) {
        Employee requiredEmployee = requireEntity(employee, "Employee");
        if (findEmployeeByUsername(requiredEmployee.getUsername()).isPresent()) {
            throw new ConflictException(
                    "Employee username " + requiredEmployee.getUsername() + " already exists.");
        }
        add(employees, requiredEmployee.getId(), requiredEmployee, "Employee");
    }

    public void addCustomer(Customer customer) {
        add(customers, requireEntity(customer, "Customer").getId(), customer, "Customer");
    }

    public void addTable(RestaurantTable table) {
        RestaurantTable requiredTable = requireEntity(table, "Table");
        boolean duplicateNumber = tables.values().stream()
                .anyMatch(existing -> existing.getTableNumber() == requiredTable.getTableNumber());
        if (duplicateNumber) {
            throw new ConflictException(
                    "Table number " + requiredTable.getTableNumber() + " already exists.");
        }
        add(tables, requiredTable.getId(), requiredTable, "Table");
    }

    public void addMenuItem(MenuItem menuItem) {
        add(menuItems, requireEntity(menuItem, "Menu item").getId(), menuItem, "Menu item");
    }

    public void addReservation(Reservation reservation) {
        add(reservations, requireEntity(reservation, "Reservation").getId(),
                reservation, "Reservation");
    }

    public void addWaitlistEntry(WaitlistEntry waitlistEntry) {
        add(waitlistEntries, requireEntity(waitlistEntry, "Waitlist entry").getId(),
                waitlistEntry, "Waitlist entry");
    }

    public void addSession(DiningSession session) {
        DiningSession requiredSession = requireEntity(session, "Dining session");
        validateSessionAddition(requiredSession);
        add(sessions, requiredSession.getId(), requiredSession, "Dining session");
    }

    public void addOrder(Order order) {
        Order requiredOrder = requireEntity(order, "Order");
        validateOrderAddition(requiredOrder);
        add(orders, requiredOrder.getId(), requiredOrder, "Order");
    }

    public void addBill(Bill bill) {
        Bill requiredBill = requireEntity(bill, "Bill");
        if (findBillBySession(requiredBill.getSessionId()).isPresent()) {
            throw new ConflictException(
                    "Dining session " + requiredBill.getSessionId() + " already has a bill.");
        }
        add(bills, requiredBill.getId(), requiredBill, "Bill");
    }

    public void addPayment(Payment payment) {
        Payment requiredPayment = requireEntity(payment, "Payment");
        if (findPaymentByBill(requiredPayment.getBillId()).isPresent()) {
            throw new ConflictException(
                    "Bill " + requiredPayment.getBillId() + " already has a payment.");
        }
        add(payments, requiredPayment.getId(), requiredPayment, "Payment");
    }

    public void addAuditLog(AuditLog auditLog) {
        add(auditLogs, requireEntity(auditLog, "Audit log").getId(), auditLog, "Audit log");
    }

    /**
     * Sabay add ng session at order na magkapartner.
     * Check muna lahat bago galawin yung collections, para walang maiwan na kalahati.
     */
    public void addSessionAndOrder(DiningSession session, Order order) {
        DiningSession requiredSession = requireEntity(session, "Dining session");
        Order requiredOrder = requireEntity(order, "Order");
        if (!requiredSession.getOrderId().equals(requiredOrder.getId())
                || !requiredOrder.getSessionId().equals(requiredSession.getId())) {
            throw new ValidationException(
                    "Dining session and order IDs do not reference each other.");
        }
        validateSessionAddition(requiredSession);
        validateOrderAddition(requiredOrder);
        requireAvailableId(sessions, requiredSession.getId(), "Dining session");
        requireAvailableId(orders, requiredOrder.getId(), "Order");

        sessions.put(requiredSession.getId(), requiredSession);
        orders.put(requiredOrder.getId(), requiredOrder);
    }

    /** Separate copy to para di agad magalaw yung original habang may transaction. */
    public AppState deepCopy() {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
                output.writeObject(this);
            }
            try (ObjectInputStream input = new ObjectInputStream(
                    new ByteArrayInputStream(bytes.toByteArray()))) {
                return (AppState) input.readObject();
            }
        } catch (IOException | ClassNotFoundException error) {
            throw new IllegalStateException("Unable to copy application state.", error);
        }
    }

    private static <T> Collection<T> unmodifiableValues(Map<String, T> records) {
        return Collections.unmodifiableCollection(records.values());
    }

    private static <T> T getOrThrow(Map<String, T> records, String id, String entityName) {
        String normalizedId = requireId(id, entityName);
        T entity = records.get(normalizedId);
        if (entity == null) {
            throw new ValidationException(entityName + " " + normalizedId + " was not found.");
        }
        return entity;
    }

    private static <T> void add(Map<String, T> records, String id, T entity, String entityName) {
        requireAvailableId(records, id, entityName);
        records.put(id, entity);
    }

    private void validateSessionAddition(DiningSession session) {
        if (session.isOpen() && findOpenSessionByTable(session.getTableId()).isPresent()) {
            throw new ConflictException(
                    "Table " + session.getTableId() + " already has an open dining session.");
        }
    }

    private void validateOrderAddition(Order order) {
        if (findOrderBySession(order.getSessionId()).isPresent()) {
            throw new ConflictException(
                    "Dining session " + order.getSessionId() + " already has an order.");
        }
    }

    private static <T> void requireAvailableId(
            Map<String, T> records, String id, String entityName) {
        if (records.containsKey(id)) {
            throw new ConflictException(entityName + " " + id + " already exists.");
        }
    }

    private static <T> T requireEntity(T entity, String entityName) {
        if (entity == null) {
            throw new ValidationException(entityName + " is required.");
        }
        return entity;
    }

    private static String requireId(String id, String entityName) {
        return requireText(id, entityName + " ID");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(fieldName + " is required.");
        }
        return value.trim();
    }
}
