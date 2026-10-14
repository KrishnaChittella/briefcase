package com.krishna.briefcase.cases;

import com.krishna.briefcase.client.Client;
import com.krishna.briefcase.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "legal_case")
@Getter
@Setter
public class LegalCase extends BaseEntity {

    @Column(name = "case_number", nullable = false, unique = true)
    private String caseNumber;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_type", nullable = false)
    private BillingType billingType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CaseStatus status = CaseStatus.OPEN;

    @Column(name = "contingency_rate")
    private BigDecimal contingencyRate;

    @Column(name = "hourly_rate")
    private BigDecimal hourlyRate;

    @Column(name = "logged_hours")
    private BigDecimal loggedHours;

    @Column(name = "flat_fee")
    private BigDecimal flatFee;

    @Column(name = "financially_resolved", nullable = false)
    private boolean financiallyResolved = false;

    @CreationTimestamp
    @Column(name = "opened_at", updatable = false)
    private Instant openedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "closed_at")
    private Instant closedAt;
}
