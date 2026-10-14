package com.krishna.briefcase.billing;

/** Why a given FeeDistribution row has the amount it does. */
public enum FeeShareType {
    /** The LEAD attorney's fixed-rate cut, taken before the equal split. */
    LEAD_CUT,
    /** An ASSOCIATE's equal share of what remained after the lead cut. */
    EQUAL_SPLIT,
    /** LEAD was the only non-paralegal assignee, so they received the entire net amount. */
    SOLE_LEAD
}
