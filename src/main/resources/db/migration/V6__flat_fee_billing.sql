-- Third billing type: a fixed fee agreed up front, independent of hours or settlement.
ALTER TABLE legal_case ADD COLUMN flat_fee NUMERIC(12,2);
