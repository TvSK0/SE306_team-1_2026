// TEMPORARY: swap this package for Scott's logic package once his skeleton is on main.
package lavs.logic;

import lavs.data.MainframeGateway;
import lavs.model.CustomerRelationship;
import lavs.validation.FieldError;
import lavs.validation.ValidationResult;
import mainframe.client.FakeConnection;
import mainframe.client.MessageRequest;
import mainframe.client.StatusMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class CustomerRelationshipServiceTests {
    private static final String CUSTOMER_NUMBER = "C-10234";
    private static final String OTHER_NUMBER = "C-20456";

    private static final RelationshipInput SPOUSE = new RelationshipInput(OTHER_NUMBER, "Spouse", "");

    // AI-assisted: saves go through the upload queue (US11). Each service gets a real SyncService
    // over an in-memory queue that sends to the same FakeConnection, as the same requests.
    private final List<SyncService> _syncs = new ArrayList<>();
    private InMemoryChangeQueue _queue;

    private CustomerRelationshipService serviceOver(FakeConnection connection) {
        _queue = new InMemoryChangeQueue();
        SyncService sync = new SyncService(_queue, new MainframeGateway(connection),
                Executors.newSingleThreadScheduledExecutor());
        _syncs.add(sync);
        return new CustomerRelationshipService(connection, sync);
    }

    @AfterEach
    void closeSyncs() {
        _syncs.forEach(SyncService::close);
    }

    // --- validate ---

    @Test
    void acceptsAValidRelationship() {
        // Arrange
        CustomerRelationshipService service = serviceOver(new FakeConnection());

        // Act
        ValidationResult result = service.validate(CUSTOMER_NUMBER, SPOUSE);

        // Assert
        Assertions.assertTrue(result.isValid());
    }

    @Test
    void reportsEveryEmptyRequiredField() {
        // Arrange
        CustomerRelationshipService service = serviceOver(new FakeConnection());

        // Act
        ValidationResult result = service.validate("", new RelationshipInput(" ", null, ""));

        // Assert
        Assertions.assertEquals(
                List.of(CustomerRelationshipService.CUSTOMER_NUMBER, RelationshipInput.OTHER_CUSTOMER_NUMBER,
                        RelationshipInput.TYPE),
                fieldNames(result));
    }

    @Test
    void anOtherRelationshipNeedsADescription() {
        // Arrange
        CustomerRelationshipService service = serviceOver(new FakeConnection());

        // Act
        ValidationResult result = service.validate(CUSTOMER_NUMBER, new RelationshipInput(OTHER_NUMBER, "Other", " "));

        // Assert
        Assertions.assertEquals(List.of(RelationshipInput.OTHER_TYPE), fieldNames(result));
    }

    @Test
    void anOtherRelationshipWithADescriptionIsValid() {
        // Arrange
        CustomerRelationshipService service = serviceOver(new FakeConnection());

        // Act
        ValidationResult result = service.validate(CUSTOMER_NUMBER,
                new RelationshipInput(OTHER_NUMBER, "Other", "Business partner"));

        // Assert
        Assertions.assertTrue(result.isValid());
    }

    @Test
    void aCustomerCannotBeRelatedToThemselves() {
        // Arrange
        CustomerRelationshipService service = serviceOver(new FakeConnection());

        // Act
        ValidationResult result = service.validate(CUSTOMER_NUMBER,
                new RelationshipInput(" " + CUSTOMER_NUMBER + " ", "Spouse", ""));

        // Assert
        Assertions.assertEquals(List.of(RelationshipInput.OTHER_CUSTOMER_NUMBER), fieldNames(result));
    }

    // --- listRelationships ---

    @Test
    void listsEachRelationshipWithTheOtherCustomersNameAndDescription() throws IOException {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("customerRelationship.list", Map.of("page", "1"),
                listReply(2, item(7, OTHER_NUMBER, "Spouse"), item(9, "C-30789", "Other")));
        connection.whenRequested("customerRelationship.retrieve", Map.of("id", "7"),
                retrieveReply(7, OTHER_NUMBER, "Sam Smith", "Spouse", null));
        connection.whenRequested("customerRelationship.retrieve", Map.of("id", "9"),
                retrieveReply(9, "C-30789", "Pat Lee", "Other", "Business partner"));
        CustomerRelationshipService service = serviceOver(connection);

        // Act
        RelationshipListResult result = service.listRelationships(CUSTOMER_NUMBER);

        // Assert
        List<CustomerRelationship> relationships = result.getRelationships();
        Assertions.assertAll(
                () -> Assertions.assertTrue(result.isSuccessful()),
                () -> Assertions.assertEquals(2, relationships.size()),
                () -> Assertions.assertEquals(7L, relationships.get(0).getId()),
                () -> Assertions.assertEquals(CUSTOMER_NUMBER, relationships.get(0).getCustomerNumber()),
                () -> Assertions.assertEquals(OTHER_NUMBER, relationships.get(0).getOtherCustomerNumber()),
                () -> Assertions.assertEquals("Sam Smith", relationships.get(0).getOtherCustomerName()),
                () -> Assertions.assertEquals("Spouse", relationships.get(0).getType()),
                () -> Assertions.assertEquals("Other", relationships.get(1).getType()),
                () -> Assertions.assertEquals("Business partner", relationships.get(1).getOtherType()),
                () -> Assertions.assertEquals(CUSTOMER_NUMBER,
                        connection.getSentRequests().get(0).getValue("number"))
        );
    }

    @Test
    void listsEveryPage() throws IOException {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("customerRelationship.list", Map.of("page", "1"),
                listReply(2, item(7, OTHER_NUMBER, "Spouse")));
        connection.whenRequested("customerRelationship.list", Map.of("page", "2"),
                listReply(2, item(9, "C-30789", "Parent")));
        connection.whenRequested("customerRelationship.retrieve",
                retrieveReply(0, OTHER_NUMBER, "Sam Smith", "Spouse", null));
        CustomerRelationshipService service = serviceOver(connection);

        // Act
        RelationshipListResult result = service.listRelationships(CUSTOMER_NUMBER);

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertTrue(result.isSuccessful()),
                () -> Assertions.assertEquals(2, result.getRelationships().size()),
                () -> Assertions.assertEquals(9L, result.getRelationships().get(1).getId())
        );
    }

    @Test
    void stopsOnAnEmptyPageEvenIfTheTotalSaysMore() throws IOException {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("customerRelationship.list", Map.of("page", "1"), listReply(5));
        CustomerRelationshipService service = serviceOver(connection);

        // Act
        RelationshipListResult result = service.listRelationships(CUSTOMER_NUMBER);

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertTrue(result.isSuccessful()),
                () -> Assertions.assertEquals(List.of(), result.getRelationships()),
                () -> Assertions.assertEquals(1, connection.getSentRequests().size())
        );
    }

    @Test
    void listingRequiresACustomerNumber() throws IOException {
        // Arrange
        FakeConnection connection = new FakeConnection();
        CustomerRelationshipService service = serviceOver(connection);

        // Act
        RelationshipListResult result = service.listRelationships(" ");

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertFalse(result.isSuccessful()),
                () -> Assertions.assertEquals(List.of(CustomerRelationshipService.CUSTOMER_NUMBER),
                        fieldNames(result.getValidationErrors())),
                () -> Assertions.assertEquals(0, connection.getSentRequests().size())
        );
    }

    @Test
    void reportsAFailedListWithNoRelationships() throws IOException {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.setIsConnected(false);
        CustomerRelationshipService service = serviceOver(connection);

        // Act
        RelationshipListResult result = service.listRelationships(CUSTOMER_NUMBER);

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertFalse(result.isSuccessful()),
                () -> Assertions.assertEquals(List.of(), result.getRelationships()),
                () -> Assertions.assertEquals(StatusMessage.NETWORK_FAILURE_UNAVAILABLE.getCode(),
                        result.getMainframeStatus().getErrorCode())
        );
    }

    @Test
    void reportsAFailedLookupOfOneRelationship() throws IOException {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("customerRelationship.list", Map.of("page", "1"),
                listReply(1, item(7, OTHER_NUMBER, "Spouse")));
        connection.whenRequested("customerRelationship.retrieve", FakeConnection.rejected("query error"));
        CustomerRelationshipService service = serviceOver(connection);

        // Act
        RelationshipListResult result = service.listRelationships(CUSTOMER_NUMBER);

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertFalse(result.isSuccessful()),
                () -> Assertions.assertEquals(List.of(), result.getRelationships()),
                () -> Assertions.assertEquals("query error", result.getMainframeStatus().getErrorMessage())
        );
    }

    // --- addRelationship ---

    @Test
    void addsTheRelationshipAndReturnsWhatWasSaved() throws IOException {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("customer.addRelationship", savedReply(12, OTHER_NUMBER, "Spouse", null));
        CustomerRelationshipService service = serviceOver(connection);

        // Act
        RelationshipResult result = service.addRelationship(CUSTOMER_NUMBER, new RelationshipInput(
                " " + OTHER_NUMBER + " ", "Spouse", "ignored unless Other"));

        // Assert
        MessageRequest sent = connection.getSentRequests().get(0);
        Assertions.assertAll(
                () -> Assertions.assertTrue(result.isSuccessful()),
                () -> Assertions.assertTrue(result.isSaved()),
                // what was saved, as sent; the new id comes from listing again
                () -> Assertions.assertNull(result.getRelationship().getId()),
                () -> Assertions.assertEquals(OTHER_NUMBER, result.getRelationship().getOtherCustomerNumber()),
                () -> Assertions.assertEquals("Spouse", result.getRelationship().getType()),
                () -> Assertions.assertEquals(CUSTOMER_NUMBER, sent.getValue("customer")),
                () -> Assertions.assertEquals(OTHER_NUMBER, sent.getValue("secondaryCustomerNumber")),
                () -> Assertions.assertEquals("Spouse", sent.getValue("type")),
                () -> Assertions.assertNull(sent.getValue("otherType"))
        );
    }

    @Test
    void addsAnOtherRelationshipWithItsDescription() throws IOException {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("customer.addRelationship", savedReply(12, OTHER_NUMBER, "Other", "Business partner"));
        CustomerRelationshipService service = serviceOver(connection);

        // Act
        RelationshipResult result = service.addRelationship(CUSTOMER_NUMBER,
                new RelationshipInput(OTHER_NUMBER, "Other", " Business partner "));

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertTrue(result.isSuccessful()),
                () -> Assertions.assertEquals("Business partner", result.getRelationship().getOtherType()),
                () -> Assertions.assertEquals("Business partner",
                        connection.getSentRequests().get(0).getValue("otherType"))
        );
    }

    @Test
    void doesNotContactTheMainframeWhenTheRelationshipIsInvalid() throws IOException {
        // Arrange
        FakeConnection connection = new FakeConnection();
        CustomerRelationshipService service = serviceOver(connection);

        // Act
        RelationshipResult result = service.addRelationship(CUSTOMER_NUMBER, new RelationshipInput("", "Spouse", ""));

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertFalse(result.isSuccessful()),
                () -> Assertions.assertEquals(List.of(RelationshipInput.OTHER_CUSTOMER_NUMBER),
                        fieldNames(result.getValidationErrors())),
                () -> Assertions.assertNull(result.getMainframeStatus()),
                () -> Assertions.assertEquals(0, connection.getSentRequests().size())
        );
    }

    @Test
    void reportsARejectedAdd() throws IOException {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("customer.addRelationship", FakeConnection.rejected("Unknown customer"));
        CustomerRelationshipService service = serviceOver(connection);

        // Act
        RelationshipResult result = service.addRelationship(CUSTOMER_NUMBER, SPOUSE);

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertFalse(result.isSuccessful()),
                () -> Assertions.assertNull(result.getRelationship()),
                () -> Assertions.assertEquals(StatusMessage.SERVER_FAILURE.getCode(),
                        result.getMainframeStatus().getErrorCode()),
                () -> Assertions.assertEquals("Unknown customer", result.getMainframeStatus().getErrorMessage())
        );
    }

    @Test
    void keepsARelationshipAddedOfflineToUploadLater() throws IOException {
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("customer.addRelationship", FakeConnection.networkDown());
        CustomerRelationshipService service = serviceOver(connection);

        RelationshipResult result = service.addRelationship(CUSTOMER_NUMBER, SPOUSE);

        Assertions.assertAll(
                () -> Assertions.assertFalse(result.isSuccessful()),
                () -> Assertions.assertTrue(result.isWaitingToUpload()),
                () -> Assertions.assertTrue(result.isSaved()),
                () -> Assertions.assertEquals(OTHER_NUMBER, result.getRelationship().getOtherCustomerNumber()),
                () -> Assertions.assertEquals(1, _queue.all().size())
        );
    }

    @Test
    void keepsAnUpdateAndADeleteMadeOfflineToUploadLater() throws IOException {
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("customer.updateRelationship", FakeConnection.networkDown());
        connection.whenRequested("customer.deleteRelationship", FakeConnection.networkDown());
        CustomerRelationshipService service = serviceOver(connection);

        RelationshipResult updated = service.updateRelationship(CUSTOMER_NUMBER, 12, SPOUSE);
        RelationshipResult deleted = service.deleteRelationship(CUSTOMER_NUMBER, 12);

        Assertions.assertAll(
                () -> Assertions.assertTrue(updated.isWaitingToUpload()),
                () -> Assertions.assertEquals(12L, updated.getRelationship().getId()),
                () -> Assertions.assertTrue(deleted.isWaitingToUpload()),
                () -> Assertions.assertNull(deleted.getRelationship())
        );
    }

    @Test
    void aRejectedQueuedSaveIsNotSaved() throws IOException {
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("customer.addRelationship", FakeConnection.rejected("Unknown customer"));

        RelationshipResult result = serviceOver(connection).addRelationship(CUSTOMER_NUMBER, SPOUSE);

        Assertions.assertFalse(result.isSaved());
        Assertions.assertFalse(result.isWaitingToUpload());
    }

    // --- updateRelationship ---

    @Test
    void updatesTheRelationshipByItsId() throws IOException {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("customer.updateRelationship", savedReply(12, OTHER_NUMBER, "Parent", null));
        CustomerRelationshipService service = serviceOver(connection);

        // Act
        RelationshipResult result = service.updateRelationship(CUSTOMER_NUMBER, 12,
                new RelationshipInput(OTHER_NUMBER, "Parent", ""));

        // Assert
        MessageRequest sent = connection.getSentRequests().get(0);
        Assertions.assertAll(
                () -> Assertions.assertTrue(result.isSuccessful()),
                () -> Assertions.assertEquals("Parent", result.getRelationship().getType()),
                () -> Assertions.assertEquals("12", sent.getValue("id")),
                () -> Assertions.assertEquals(CUSTOMER_NUMBER, sent.getValue("customer")),
                () -> Assertions.assertEquals("Parent", sent.getValue("type"))
        );
    }

    @Test
    void doesNotUpdateAnInvalidRelationship() throws IOException {
        // Arrange
        FakeConnection connection = new FakeConnection();
        CustomerRelationshipService service = serviceOver(connection);

        // Act
        RelationshipResult result = service.updateRelationship(CUSTOMER_NUMBER, 12,
                new RelationshipInput(OTHER_NUMBER, "Other", ""));

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertEquals(List.of(RelationshipInput.OTHER_TYPE),
                        fieldNames(result.getValidationErrors())),
                () -> Assertions.assertEquals(0, connection.getSentRequests().size())
        );
    }

    @Test
    // AI-assisted: since US11 an update that can't reach the mainframe is kept to upload later
    void keepsAnUpdateThatCannotReachTheMainframe() throws IOException {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.setIsConnected(false);
        CustomerRelationshipService service = serviceOver(connection);

        // Act
        RelationshipResult result = service.updateRelationship(CUSTOMER_NUMBER, 12, SPOUSE);

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertFalse(result.isSuccessful()),
                () -> Assertions.assertTrue(result.isWaitingToUpload()),
                () -> Assertions.assertEquals(12L, result.getRelationship().getId()),
                () -> Assertions.assertEquals(StatusMessage.NETWORK_FAILURE_UNAVAILABLE.getCode(),
                        result.getMainframeStatus().getErrorCode())
        );
    }

    // --- deleteRelationship ---

    @Test
    void deletesTheRelationshipByItsId() throws IOException {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("customer.deleteRelationship",
                FakeConnection.success(Map.of("customer", CUSTOMER_NUMBER, "id", "12")));
        CustomerRelationshipService service = serviceOver(connection);

        // Act
        RelationshipResult result = service.deleteRelationship(CUSTOMER_NUMBER, 12);

        // Assert
        MessageRequest sent = connection.getSentRequests().get(0);
        Assertions.assertAll(
                () -> Assertions.assertTrue(result.isSuccessful()),
                () -> Assertions.assertNull(result.getRelationship()),
                () -> Assertions.assertEquals(CUSTOMER_NUMBER, sent.getValue("customer")),
                () -> Assertions.assertEquals("12", sent.getValue("id"))
        );
    }

    @Test
    void deletingRequiresACustomerNumber() throws IOException {
        // Arrange
        FakeConnection connection = new FakeConnection();
        CustomerRelationshipService service = serviceOver(connection);

        // Act
        RelationshipResult result = service.deleteRelationship("", 12);

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertEquals(List.of(CustomerRelationshipService.CUSTOMER_NUMBER),
                        fieldNames(result.getValidationErrors())),
                () -> Assertions.assertEquals(0, connection.getSentRequests().size())
        );
    }

    @Test
    void reportsARejectedDelete() throws IOException {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("customer.deleteRelationship", FakeConnection.rejected("Not found"));
        CustomerRelationshipService service = serviceOver(connection);

        // Act
        RelationshipResult result = service.deleteRelationship(CUSTOMER_NUMBER, 12);

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertFalse(result.isSuccessful()),
                () -> Assertions.assertEquals("Not found", result.getMainframeStatus().getErrorMessage())
        );
    }

    // one row of a customerRelationship.list reply: id, other customer number, type
    private static String[] item(long id, String otherCustomerNumber, String type) {
        return new String[] {Long.toString(id), otherCustomerNumber, type};
    }

    private static mainframe.client.MessageResponse listReply(long total, String[]... items) {
        Map<String, String> values = new HashMap<>();
        values.put("customerNumber", CUSTOMER_NUMBER);
        values.put("total", Long.toString(total));
        values.put("items", Integer.toString(items.length));
        for (int i = 1; i <= items.length; i++) {
            values.put(i + ":id", items[i - 1][0]);
            values.put(i + ":secondaryCustomerNumber", items[i - 1][1]);
            values.put(i + ":type", items[i - 1][2]);
        }
        return FakeConnection.success(values);
    }

    private static mainframe.client.MessageResponse retrieveReply(long id, String otherCustomerNumber,
                                                                  String otherCustomerName, String type,
                                                                  String otherType) {
        Map<String, String> values = new HashMap<>();
        values.put("customerNumber", CUSTOMER_NUMBER);
        values.put("id", Long.toString(id));
        values.put("secondaryCustomerNumber", otherCustomerNumber);
        values.put("secondaryCustomerName", otherCustomerName);
        values.put("type", type);
        if (otherType != null) values.put("otherType", otherType);
        return FakeConnection.success(values);
    }

    // the reply to customer.addRelationship and customer.updateRelationship
    private static mainframe.client.MessageResponse savedReply(long id, String otherCustomerNumber, String type,
                                                               String otherType) {
        Map<String, String> values = new HashMap<>();
        values.put("customer", CUSTOMER_NUMBER);
        values.put("id", Long.toString(id));
        values.put("secondaryCustomerNumber", otherCustomerNumber);
        values.put("type", type);
        if (otherType != null) values.put("otherType", otherType);
        return FakeConnection.success(values);
    }

    private static List<String> fieldNames(ValidationResult result) {
        List<String> names = new ArrayList<>();
        for (FieldError error : result.getErrors()) {
            names.add(error.getFieldName());
        }
        return names;
    }
}
