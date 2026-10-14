package com.krishna.briefcase.outcome;

import com.krishna.briefcase.cases.LegalCase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/** Recorded once per case: the legal result plus the computed financial breakdown. */
@Entity
@Table(name = "case_outcome")
@Getter
@Setter
public class CaseOutcome {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_id", nullable = false, unique = true)
    private LegalCase legalCase;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CaseResult result;

    @Column(name = "settlement_amount")
    private BigDecimal settlementAmount;

    @Column(name = "gross_revenue", nullable = false)
    private BigDecimal grossRevenue;

    @Column(name = "firm_cut", nullable = false)
    private BigDecimal firmCut;

    @Column(name = "net_distributable", nullable = false)
    private BigDecimal netDistributable;

    @Column(name = "recorded_by", nullable = false)
    private String recordedBy;

    @CreationTimestamp
    @Column(name = "recorded_at", updatable = false)
    private Instant recordedAt;
}
