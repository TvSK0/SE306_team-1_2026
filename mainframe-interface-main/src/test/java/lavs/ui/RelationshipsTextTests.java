// TEMPORARY: swap this package for Scott's ui package once his skeleton is on main.
package lavs.ui;

import lavs.data.LocalDatabase;
import lavs.data.MainframeGateway;
import lavs.data.SqlitePendingChangeStore;
import lavs.logic.CustomerRelationshipService;
import lavs.logic.RelationshipInput;
import lavs.logic.RelationshipResult;
import lavs.logic.SyncService;
import lavs.model.CustomerRelationship;
import mainframe.client.FakeConnection;
import mainframe.client.Status;
import mainframe.client.StatusMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public class RelationshipsTextTests {
    private static final RelationshipInput SPOUSE = new RelationshipInput("C-20456", "Spouse", "");

    // AI-assisted: saves go through the upload queue (US11), kept in a SQLite file in a temp folder
    @TempDir
    Path folder;
    private SyncService _sync;

    private CustomerRelationshipService serviceOver(FakeConnection connection) {
        MainframeGateway gateway = new MainframeGateway(connection);
        _sync = SyncService.create(new SqlitePendingChangeStore(new LocalDatabase(folder.resolve("lavs.db"))), gateway);
        return new CustomerRelationshipService(connection, _sync);
    }

    @AfterEach
    void closeSync() {
        if (_sync != null) {
            _sync.close();
        }
    }

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
        RelationshipResult result = serviceOver(connection).addRelationship("C-10234", SPOUSE);

        // Assert
        Assertions.assertEquals("Relationship added.", RelationshipsText.forResult(result, "Relationship added."));
    }

    @Test
    void asksToFixTheHighlightedFields() throws IOException {
        // Act
        RelationshipResult result = serviceOver(new FakeConnection())
                .addRelationship("C-10234", new RelationshipInput("", "Spouse", ""));

        // Assert
        Assertions.assertEquals("Please fix the highlighted fields.",
                RelationshipsText.forResult(result, "Relationship added."));
    }

    // AI-assisted: since US11 a save that can't reach the mainframe is kept to upload later
    @Test
    void saysASaveIsKeptOnThisLaptopWhenItCannotReachTheMainframe() throws IOException {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.setIsConnected(false);

        // Act
        RelationshipResult result = serviceOver(connection).addRelationship("C-10234", SPOUSE);

        // Assert
        Assertions.assertEquals("Relationship added. " + ResultText.WAITING_TO_UPLOAD,
                RelationshipsText.forResult(result, "Relationship added."));
    }

    @Test
    void passesOnTheMainframesReasonForARejection() throws IOException {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("customer.addRelationship", FakeConnection.rejected("Unknown customer"));

        // Act
        RelationshipResult result = serviceOver(connection).addRelationship("C-10234", SPOUSE);

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
                RelationshipsText.forLoadFailure(status));
    }

    @Test
    void passesOnTheReasonTheListFailed() {
        // Arrange
        Status status = new Status(StatusMessage.SERVER_FAILURE.getCode(), "query error", 1);

        // Act + Assert
        Assertions.assertEquals("The relationships could not be loaded: query error",
                RelationshipsText.forLoadFailure(status));
    }
}
