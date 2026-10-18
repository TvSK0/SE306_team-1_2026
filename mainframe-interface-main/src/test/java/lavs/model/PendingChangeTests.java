package lavs.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// AI-assisted: tests generated with Claude (team-approved).
class PendingChangeTests {

    private static Map<String, String> fields(String... pairs) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(pairs[i], pairs[i + 1]);
        }
        return map;
    }

    @Test
    void newChangeIsPendingWithCopiedFields() {
        Map<String, String> source = fields("customer", "C1", "city", "Auckland");
        PendingChange change = new PendingChange("customer.addAddress", "C1", null, source);
        source.put("city", "Changed");

        assertAll(
                () -> assertEquals("customer.addAddress", change.getMessageType()),
                () -> assertEquals("C1", change.getCustomerNumber()),
                () -> assertNull(change.getCreatesNumber()),
                () -> assertEquals("Auckland", change.getFields().get("city")),
                () -> assertEquals(PendingChange.Status.PENDING, change.getStatus()),
                () -> assertNotNull(change.getCreatedAt()),
                () -> assertNull(change.getId()),
                () -> assertNull(change.getStatusMessage()),
                () -> assertFalse(change.isSkipDuplicateCheck()));
    }

    @Test
    void fieldsCannotBeChangedFromOutside() {
        PendingChange change = new PendingChange("customer.addEmail", "C1", null, fields("address", "a@b.nz"));

        assertThrows(UnsupportedOperationException.class, () -> change.getFields().put("x", "y"));
    }

    @Test
    void messageTypeIsRequired() {
        assertThrows(NullPointerException.class, () -> new PendingChange(null, "C1", null, Map.of()));
    }

    @Test
    void settersUpdateFields() {
        PendingChange change = new PendingChange("loan.create", "C1", "TMP-L", Map.of());
        Instant when = Instant.parse("2026-10-18T00:00:00Z");
        change.setId(4L);
        change.setCreatedAt(when);
        change.setStatus(PendingChange.Status.REJECTED);
        change.setStatusMessage("Bad rate");
        change.setSkipDuplicateCheck(true);

        assertAll(
                () -> assertEquals(4L, change.getId()),
                () -> assertEquals(when, change.getCreatedAt()),
                () -> assertEquals("TMP-L", change.getCreatesNumber()),
                () -> assertEquals(PendingChange.Status.REJECTED, change.getStatus()),
                () -> assertEquals("Bad rate", change.getStatusMessage()),
                () -> assertTrue(change.isSkipDuplicateCheck()));
    }

    @Test
    void referencesCustomerNumberCreatedNumberOrField() {
        PendingChange change = new PendingChange("loan.addCoborrower", "C1", "TMP-L",
                fields("coborrowerNumber", "C2"));

        assertAll(
                () -> assertTrue(change.references("C1")),
                () -> assertTrue(change.references("TMP-L")),
                () -> assertTrue(change.references("C2")),
                () -> assertFalse(change.references("C3")),
                () -> assertFalse(change.references(null)));
    }

    @Test
    void replaceNumberSwapsCustomerNumberAndMatchingFields() {
        PendingChange change = new PendingChange("customer.addAddress", "TMP-1", null,
                fields("customer", "TMP-1", "city", "Auckland"));

        change.replaceNumber("TMP-1", "C9");

        assertAll(
                () -> assertEquals("C9", change.getCustomerNumber()),
                () -> assertEquals("C9", change.getFields().get("customer")),
                () -> assertEquals("Auckland", change.getFields().get("city")));
    }

    @Test
    void replaceNumberLeavesOtherCustomerAlone() {
        PendingChange change = new PendingChange("loan.addCoborrower", "C1", null,
                fields("coborrowerNumber", "TMP-2"));

        change.replaceNumber("TMP-2", "C5");

        assertAll(
                () -> assertEquals("C1", change.getCustomerNumber()),
                () -> assertEquals("C5", change.getFields().get("coborrowerNumber")));
    }
}
