// TEMPORARY: swap this package for Scott's ui package once his skeleton is on main.
package lavs.ui;

import lavs.logic.CustomerRelationshipService;
import lavs.logic.RelationshipInput;
import lavs.logic.RelationshipResult;
import lavs.model.CustomerRelationship;
import mainframe.client.FakeConnection;
import mainframe.client.Status;
import mainframe.client.StatusMessage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

public class RelationshipsTextTests {
    private static final RelationshipInput SPOUSE = new RelationshipInput("C-20456", "Spouse", "");

    @Test
    void showsAPlainTypeAsItIs() {
        // Act + Assert
        Assertions.assertEquals("Spouse",
                RelationshipsText.type(new CustomerRelationship("C-1", "C-2", "Spouse", null)));
    }

    @Test
    void splitsLegalAuthorityIntoWords() {
        // Act + Assert
        Assertions.assertEquals("Legal authority",
                RelationshipsText.type(new CustomerRelationship("C-1", "C-2", "LegalAuthority", null)));
    }

    @Test
    void addsTheDescriptionToAnOtherRelationship() {
        // Act + Assert
        Assertions.assertEquals("Other: Business partner",
                RelationshipsText.type(new CustomerRelationship("C-1", "C-2", "Other", "Business partner")));
    }

    @Test
    void anOtherRelationshipWithoutADescriptionIsJustOther() {
        // Act + Assert
        Assertions.assertEquals("Other",
                RelationshipsText.type(new CustomerRelationship("C-1", "C-2", "Other", " ")));
    }

    @Test
    void namesTheOtherCustomerWithTheirNumber() {
        // Arrange
        CustomerRelationship relationship = new CustomerRelationship("C-1", "C-20456", "Spouse", null);
        relationship.setOtherCustomerName("Sam Smith");

        // Act + Assert
        Assertions.assertEquals("Sam Smith (C-20456)", RelationshipsText.otherCustomer(relationship));
    }

    @Test
    void showsJustTheNumberWhenThereIsNoName() {
        // Act + Assert
        Assertions.assertEquals("C-20456",
                RelationshipsText.otherCustomer(new CustomerRelationship("C-1", "C-20456", "Spouse", null)));
    }

    @Test
    void saysWhatWasDone() throws IOException {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("customer.addRelationship", FakeConnection.success(Map.of(
                "customer", "C-10234", "id", "12", "secondaryCustomerNumber", "C-20456", "type", "Spouse")));

        // Act
        RelationshipResult result = new CustomerRelationshipService(connection).addRelationship("C-10234", SPOUSE);

        // Assert
        Assertions.assertEquals("Relationship added.", RelationshipsText.forResult(result, "Relationship added."));
    }

    @Test
    void asksToFixTheHighlightedFields() throws IOException {
        // Act
        RelationshipResult result = new CustomerRelationshipService(new FakeConnection())
                .addRelationship("C-10234", new RelationshipInput("", "Spouse", ""));

        // Assert
        Assertions.assertEquals("Please fix the highlighted fields.",
                RelationshipsText.forResult(result, "Relationship added."));
    }

    @Test
    void saysWhenASaveCannotReachTheMainframe() throws IOException {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.setIsConnected(false);

        // Act
        RelationshipResult result = new CustomerRelationshipService(connection).addRelationship("C-10234", SPOUSE);

        // Assert
        Assertions.assertEquals("Couldn't reach the mainframe. Check your connection and try again.",
                RelationshipsText.forResult(result, "Relationship added."));
    }

    @Test
    void passesOnTheMainframesReasonForARejection() throws IOException {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("customer.addRelationship", FakeConnection.rejected("Unknown customer"));

        // Act
        RelationshipResult result = new CustomerRelationshipService(connection).addRelationship("C-10234", SPOUSE);

        // Assert
        Assertions.assertEquals("The mainframe rejected the change: Unknown customer",
                RelationshipsText.forResult(result, "Relationship added."));
    }

    @Test
    void saysWhenTheListCannotReachTheMainframe() {
        // Arrange
        Status status = StatusMessage.NETWORK_FAILURE_UNAVAILABLE.generateMessageResponse(1).getStatus();

        // Act + Assert
        Assertions.assertEquals("Couldn't reach the mainframe. Check your connection and try again.",
                RelationshipsText.forLoadFailure(lavs.logic.MainframeReply.of(status)));
    }

    @Test
    void passesOnTheReasonTheListFailed() {
        // Arrange
        Status status = new Status(StatusMessage.SERVER_FAILURE.getCode(), "query error", 1);

        // Act + Assert
        Assertions.assertEquals("The relationships could not be loaded: query error",
                RelationshipsText.forLoadFailure(lavs.logic.MainframeReply.of(status)));
    }
}
