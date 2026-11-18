package org.ikigaidigital.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

// Assumption: amount stays BigDecimal. No interest math touches it, so it keeps the exact DB value.
public record Withdrawal(int id, BigDecimal amount, LocalDate date) {
}
