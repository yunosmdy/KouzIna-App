package service;

import domain.Customer;
import domain.enums.Permission;
import persistence.AppStateRepository;

import java.util.Objects;

/** Check muna permission bago i-update yung contact details ng customer. */
public final class CustomerService {
    private final AppStateRepository repository;
    private final IdGenerator idGenerator;

    public CustomerService(AppStateRepository repository, IdGenerator idGenerator) {
        this.repository = Objects.requireNonNull(repository, "Repository is required.");
        this.idGenerator = Objects.requireNonNull(idGenerator, "ID generator is required.");
    }

    public String createCustomer(
            String sessionToken, String firstName, String lastName, String phoneNumber) {
        return repository.transact(state -> {
            AuthorizationService.require(state, sessionToken, Permission.MANAGE_CUSTOMERS);
            Customer customer = new Customer(idGenerator.nextId(), firstName, lastName, phoneNumber);
            state.addCustomer(customer);
            return customer.getId();
        });
    }

    public void updateCustomer(
            String sessionToken, String customerId, String firstName, String lastName,
            String phoneNumber) {
        repository.transact(state -> {
            AuthorizationService.require(state, sessionToken, Permission.MANAGE_CUSTOMERS);
            state.getCustomerOrThrow(customerId).updateContact(firstName, lastName, phoneNumber);
            return null;
        });
    }
}
