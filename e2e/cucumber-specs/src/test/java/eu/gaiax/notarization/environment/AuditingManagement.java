package eu.gaiax.notarization.environment;

import eu.gaiax.notarization.domain.HttpNotarizationRequestAudit;
import java.util.ArrayList;
import java.util.List;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;


/**
 *
 * @author Mike Prechtl
 */
@ApplicationScoped
public class AuditingManagement {

    @Inject
    EntityManager entityManager;

    private List<HttpNotarizationRequestAudit> latestAuditEntries = new ArrayList<>();

    public List<HttpNotarizationRequestAudit> fetchAuditEntries() {
        latestAuditEntries = HttpNotarizationRequestAudit.listAll();
        return latestAuditEntries;
    }

    public boolean doesRequestorEntryExist(String sessionId, String action) {
        // we update the collected auditentries
        latestAuditEntries = fetchAuditEntries();

        return latestAuditEntries.stream()
                .filter(auditEntry -> auditEntry.sessionId != null)
                .filter(auditEntry -> auditEntry.sessionId.equals(sessionId))
                .anyMatch(auditEntry -> auditEntry.action.equals(action));
    }

    public boolean doesNotaryAuditEntryExist(String notarizationRequestid, String action) throws InterruptedException {
        // we update the collected auditentries
        latestAuditEntries = fetchAuditEntries();

        return latestAuditEntries.stream()
                .filter(auditEntry -> auditEntry.notarizationId != null)
                .filter(auditEntry -> auditEntry.notarizationId.equals(notarizationRequestid))
                .anyMatch(auditEntry -> auditEntry.action.equals(action));
    }

}
