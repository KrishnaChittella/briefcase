package com.krishna.briefcase.notification;

import com.krishna.briefcase.cases.event.CaseAssignmentChangedEvent;
import com.krishna.briefcase.cases.event.CaseStatusChangedEvent;
import com.krishna.briefcase.outcome.CaseOutcomeRecordedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Observer: a second, independent reaction to the same case events consumed by
 * {@code AuditLogListener} — proof the two listeners don't know about each other. Today this
 * just logs; Phase 11 swaps the body for a real message-queue publish without touching the
 * event-producing services.
 */
@Component
public class NotificationListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onStatusChanged(CaseStatusChangedEvent event) {
        log.info("[notify] case {} status {} -> {} (by {})",
                event.caseId(), event.oldStatus(), event.newStatus(), event.changedBy());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onAssignmentChanged(CaseAssignmentChangedEvent event) {
        log.info("[notify] case {} attorney {} {} as {} (by {})",
                event.caseId(), event.attorneyId(), event.changeType(), event.role(), event.changedBy());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOutcomeRecorded(CaseOutcomeRecordedEvent event) {
        log.info("[notify] case {} resolved as {} (by {}) — gross={}, firmCut={}, net={}",
                event.caseId(), event.result(), event.recordedBy(),
                event.grossRevenue(), event.firmCut(), event.netDistributable());
    }
}
