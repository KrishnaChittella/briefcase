package com.krishna.briefcase.audit;

import com.krishna.briefcase.cases.event.CaseAssignmentChangedEvent;
import com.krishna.briefcase.cases.event.CaseStatusChangedEvent;
import com.krishna.briefcase.outcome.CaseOutcomeRecordedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Observer: reacts to case domain events by writing an audit trail row. Runs after the
 * publishing transaction commits ({@code AFTER_COMMIT}) so the log only reflects changes that
 * actually took effect; {@code fallbackExecution = true} keeps it working if an event is ever
 * published outside a transaction (e.g. from a test).
 *
 * <p>Each handler is explicitly {@code REQUIRES_NEW}: immediately after a commit, Spring hasn't
 * finished clearing the just-completed transaction's thread-bound resources, so a plain
 * {@code @Transactional} repository call here would silently try to participate in that dead
 * transaction instead of opening a fresh one — the write would appear to succeed but never
 * reach the database. REQUIRES_NEW forces a genuinely new transaction.
 */
@Component
public class AuditLogListener {

    private final AuditLogRepository repository;

    public AuditLogListener(AuditLogRepository repository) {
        this.repository = repository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onStatusChanged(CaseStatusChangedEvent event) {
        save(event.caseId(), "STATUS_CHANGED", event.changedBy(),
                "status: " + event.oldStatus() + " -> " + event.newStatus());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAssignmentChanged(CaseAssignmentChangedEvent event) {
        save(event.caseId(), "ASSIGNMENT_" + event.changeType(), event.changedBy(),
                "attorneyId=" + event.attorneyId() + ", role=" + event.role());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onOutcomeRecorded(CaseOutcomeRecordedEvent event) {
        save(event.caseId(), "FEE_CALCULATED", event.recordedBy(),
                "result=" + event.result() + ", grossRevenue=" + event.grossRevenue()
                        + ", firmCut=" + event.firmCut() + ", netDistributable=" + event.netDistributable());
    }

    private void save(Long caseId, String action, String performedBy, String details) {
        AuditLog entry = new AuditLog();
        entry.setEntityType("CASE");
        entry.setEntityId(caseId);
        entry.setAction(action);
        entry.setPerformedBy(performedBy);
        entry.setDetails(details);
        repository.save(entry);
    }
}
