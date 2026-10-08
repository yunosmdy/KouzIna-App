package service;

import domain.AuditLog;
import domain.enums.Permission;
import persistence.AppState;
import persistence.AppStateRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Check muna permission bago ipakita yung audit history, read-only lang to. */
public final class AuditService {
    private final AppStateRepository repository;

    public AuditService(AppStateRepository repository) {
        this.repository = Objects.requireNonNull(repository, "Repository is required.");
    }

    /** Copy ng audit logs na sorted by time, di pwede i-edit yung list. */
    public List<AuditLog> getAuditLog(String actorId) {
        AppState state = repository.snapshot();
        AuthorizationService.require(state, actorId, Permission.VIEW_AUDIT_LOG);
        return state.getAuditLogs().stream()
                .sorted(Comparator.comparing(AuditLog::getTimestamp)
                        .thenComparing(AuditLog::getId))
                .toList();
    }
}
