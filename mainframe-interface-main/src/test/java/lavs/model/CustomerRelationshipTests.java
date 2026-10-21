package lavs.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CustomerRelationshipTests {
    @Test
    void constructorSetsFieldsAndTheRelationshipIsNew() {
        // Act
        CustomerRelationship relationship = new CustomerRelationship("C1", "C2", "Other", "Business partner");

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertEquals("C1", relationship.getCustomerNumber()),
                () -> Assertions.assertEquals("C2", relationship.getOtherCustomerNumber()),
                () -> Assertions.assertEquals("Other", relationship.getType()),
                () -> Assertions.assertEquals("Business partner", relationship.getOtherType()),
                () -> Assertions.assertNull(relationship.getOtherCustomerName()),
                () -> Assertions.assertTrue(relationship.isNew())
        );
    }

    @Test
    void settersUpdateFieldsAndAnIdMeansItIsSaved() {
        // Arrange
        CustomerRelationship relationship = new CustomerRelationship();

        // Act
        relationship.setId(7L);
        relationship.setCustomerNumber("C3");
        relationship.setOtherCustomerNumber("C4");
        relationship.setOtherCustomerName("Sam Smith");
        relationship.setType("Spouse");
        relationship.setOtherType(null);

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertEquals(7L, relationship.getId()),
                () -> Assertions.assertEquals("C3", relationship.getCustomerNumber()),
                () -> Assertions.assertEquals("C4", relationship.getOtherCustomerNumber()),
                () -> Assertions.assertEquals("Sam Smith", relationship.getOtherCustomerName()),
                () -> Assertions.assertEquals("Spouse", relationship.getType()),
                () -> Assertions.assertNull(relationship.getOtherType()),
                () -> Assertions.assertFalse(relationship.isNew())
        );
    }
}
