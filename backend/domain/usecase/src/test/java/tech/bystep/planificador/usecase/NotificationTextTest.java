package tech.bystep.planificador.usecase;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class NotificationTextTest {

    @Test
    void formatsMoneyInColombianPesos() {
        assertEquals("$ 150.000", NotificationText.money(new BigDecimal("150000")));
        assertEquals("$ 1.234.568", NotificationText.money(new BigDecimal("1234567.6")));
        assertEquals("$ 999", NotificationText.money(new BigDecimal("999")));
        assertEquals("$ 0", NotificationText.money(null));
        assertEquals("$ 0", NotificationText.money(new BigDecimal("-5")));
    }

    @Test
    void formatsDates() {
        assertEquals("5/09/2026", NotificationText.date(LocalDate.of(2026, 9, 5)));
        assertEquals("por confirmar", NotificationText.date(null));
    }

    @Test
    void normalizesColombianMobilePhones() {
        assertEquals("573001234567", NotificationText.normalizePhone("300 123 4567"));
        assertEquals("573001234567", NotificationText.normalizePhone("+57 300 123 4567"));
        assertEquals("573001234567", NotificationText.normalizePhone("573001234567"));
        assertEquals("573001234567", NotificationText.normalizePhone("0057 3001234567"));
        assertEquals("13055551234", NotificationText.normalizePhone("+1 305 555 1234"));
        assertNull(NotificationText.normalizePhone("6041234567"));
        assertNull(NotificationText.normalizePhone("12345"));
        assertNull(NotificationText.normalizePhone(null));
    }

    @Test
    void cleansTemplateParameters() {
        assertEquals("Anillo de oro 18k", NotificationText.param("  Anillo\n de   oro\t18k "));
        assertEquals("-", NotificationText.param("   "));
        assertEquals("-", NotificationText.param(null));
    }

    @Test
    void masksPhones() {
        assertEquals("57300****567", NotificationText.maskPhone("573001234567"));
    }
}
