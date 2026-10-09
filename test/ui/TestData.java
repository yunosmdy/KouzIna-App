package ui;
import bootstrap.SampleDataFactory;
import domain.*;
import persistence.AppState;
import security.PasswordHasher;
/** Test credentials only. Production sample data has no employee accounts. */
final class TestData {
    static AppState create() {
        AppState state=SampleDataFactory.create();PasswordHasher h=new PasswordHasher(1000);
        state.addEmployee(new Manager("employee-manager","manager","Morgan Manager",h.hash("manager123".toCharArray())));
        state.addEmployee(new Waiter("employee-waiter","waiter","Wally Waiter",h.hash("waiter123".toCharArray())));
        state.addEmployee(new Chef("employee-chef","chef","Casey Chef",h.hash("chef12345".toCharArray())));
        state.addEmployee(new Cashier("employee-cashier","cashier","Carmen Cashier",h.hash("cashier123".toCharArray())));
        state.completeInitialManagerSetup();return state;
    }
}
