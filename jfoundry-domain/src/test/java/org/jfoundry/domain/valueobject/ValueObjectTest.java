package org.jfoundry.domain.valueobject;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// P3-1: jfoundry ValueObject must extend jmolecules ValueObject so business code
/// imports only the wrapper and gets jmolecules ecosystem compatibility for free.
class ValueObjectTest {

    @Test
    void jfoundryValueObjectExtendsJmoleculesValueObject() {
        assertTrue(org.jmolecules.ddd.types.ValueObject.class.isAssignableFrom(ValueObject.class));
    }

    @Test
    void sampleRecordMayImplementValueObject() {
        Money money = new Money(BigDecimal.TEN, "CNY");
        assertNotNull(money);
        assertTrue(money instanceof ValueObject);
        assertTrue(money instanceof org.jmolecules.ddd.types.ValueObject);
    }

    record Money(BigDecimal amount, String currency) implements ValueObject {
        public Money {
            if (amount == null || amount.signum() < 0) {
                throw new IllegalArgumentException("amount must be non-negative");
            }
            if (currency == null || currency.isBlank()) {
                throw new IllegalArgumentException("currency must not be blank");
            }
        }
    }
}
