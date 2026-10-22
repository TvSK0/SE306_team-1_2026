// AI-assisted: tests for CustomerDocumentService, one per documents requirement.
package lavs.logic;

import lavs.validation.FieldError;
import lavs.validation.ValidationResult;
import mainframe.client.FakeConnection;
import mainframe.client.MessageRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomerDocumentServiceTests {
    private static final String LIST = "customerDocument.list";
    private static final String RETRIEVE = "customerDocument.retrieve";
    private static final String ADD = "customer.addDocument";
    private static final String DELETE = "customer.deleteDocument";
    private static final DocumentInput PASSPORT = new DocumentInput("Proof of Identity", "passport.pdf",
            "application/pdf", "120");

    // DOC-R2
    @Test
    void acceptsATypeAndAFileName() {
        Assertions.assertTrue(service(new FakeConnection())
                .validate(new DocumentInput("Proof of Identity", "passport.pdf", "", "")).isValid());
    }

    // DOC-R2
    @Test
    void aBlankTypeAndFileNameAreEachFlaggedWithTheFigmaWording() {
        // Act
        ValidationResult result = service(new FakeConnection()).validate(new DocumentInput(" ", null, "", ""));

        // Assert
        Assertions.assertEquals(List.of(new FieldError(DocumentInput.TYPE, "Choose a document type"),
                new FieldError(DocumentInput.FILE_NAME, "Enter a file name")), result.getErrors());
    }

    // DOC-R2
    @Test
    void aSizeThatIsNotAWholeNumberIsFlagged() {
        for (String size : List.of("12a", "-1", "1.5", "12345678901234567890")) {
            ValidationResult result = service(new FakeConnection())
                    .validate(new DocumentInput("Proof of Identity", "a.pdf", "", size));
            Assertions.assertEquals(List.of(DocumentInput.SIZE),
                    result.getErrors().stream().map(FieldError::getFieldName).toList(), size);
        }
    }

    // DOC-R2
    @Test
    void aBlankSizeIsFine() {
        Assertions.assertTrue(service(new FakeConnection())
                .validate(new DocumentInput("Proof of Identity", "a.pdf", "", "  ")).isValid());
    }

    // DOC-R1
    @Test
    void listsTheDocumentsWithTheirFileTypeAndSize() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested(LIST, Map.of("number", "C-1"), FakeConnection.success(Map.of(
                "total", "2", "items", "2",
                "1:id", "7", "1:type", "Proof of Identity", "1:fileName", "passport.pdf",
                "2:id", "8", "2:type", "Proof of Address", "2:fileName", "bill.pdf")));
        connection.whenRequested(RETRIEVE, Map.of("id", "7", "number", "C-1"), FakeConnection.success(Map.of(
                "id", "7", "type", "Proof of Identity", "fileName", "passport.pdf", "fileType", "application/pdf",
                "size", "120")));
        connection.whenRequested(RETRIEVE, Map.of("id", "8", "number", "C-1"), FakeConnection.success(Map.of(
                "id", "8", "type", "Proof of Address", "fileName", "bill.pdf", "size", "5")));

        // Act
        LookupResult<List<DocumentRow>> result = service(connection).list(" C-1 ");

        // Assert
        Assertions.assertTrue(result.isSuccessful());
        Assertions.assertEquals(new DocumentRow(7, "Proof of Identity", "passport.pdf", "application/pdf", 120),
                result.getValue().get(0));
        Assertions.assertEquals(new DocumentRow(8, "Proof of Address", "bill.pdf", null, 5),
                result.getValue().get(1));
    }

    // DOC-R1
    @Test
    void aCustomerWithNoDocumentsHasAnEmptyList() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested(LIST, FakeConnection.success(Map.of("total", "0", "items", "0")));

        // Act
        LookupResult<List<DocumentRow>> result = service(connection).list("C-1");

        // Assert
        Assertions.assertTrue(result.isSuccessful());
        Assertions.assertTrue(result.getValue().isEmpty());
    }

    // DOC-R1 (an empty page ends the list, so a wrong total cannot cause an endless loop)
    @Test
    void anEmptyPageEndsTheListEvenIfTheTotalSaysThereAreMore() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested(LIST, Map.of("page", "1"), FakeConnection.success(Map.of(
                "total", "5", "items", "1", "1:id", "7", "1:type", "Proof of Identity", "1:fileName", "a.pdf")));
        connection.whenRequested(LIST, Map.of("page", "2"), FakeConnection.success(Map.of("total", "5", "items", "0")));
        connection.whenRequested(RETRIEVE, FakeConnection.success(Map.of("size", "1")));

        // Act
        LookupResult<List<DocumentRow>> result = service(connection).list("C-1");

        // Assert
        Assertions.assertEquals(1, result.getValue().size());
    }

    // DOC-R1
    @Test
    void theListNeedsANumberAndAConnectionAndCanBeRefused() {
        // Arrange
        FakeConnection offline = new FakeConnection();
        offline.setIsConnected(false);
        FakeConnection refused = new FakeConnection();
        refused.whenRequested(LIST, FakeConnection.rejected("No such customer"));
        FakeConnection refusedRetrieve = new FakeConnection();
        refusedRetrieve.whenRequested(LIST, FakeConnection.success(Map.of(
                "total", "1", "items", "1", "1:id", "7", "1:type", "t", "1:fileName", "f")));
        refusedRetrieve.whenRequested(RETRIEVE, FakeConnection.rejected("No such document"));

        // Act and Assert
        Assertions.assertEquals(LookupResult.Outcome.INVALID, service(new FakeConnection()).list(" ").getOutcome());
        Assertions.assertEquals(LookupResult.Outcome.NETWORK_FAILURE, service(offline).list("C-1").getOutcome());
        LookupResult<List<DocumentRow>> rejected = service(refused).list("C-1");
        Assertions.assertEquals(LookupResult.Outcome.REJECTED, rejected.getOutcome());
        Assertions.assertEquals("No such customer", rejected.getMessage());
        Assertions.assertEquals("No such document", service(refusedRetrieve).list("C-1").getMessage());
    }

    // DOC-R1 (the starter list message throws when an item has no id; that must not escape)
    @Test
    void aListReplyTheStarterMessageCannotReadIsRejected() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested(LIST, FakeConnection.success(Map.of("total", "1", "items", "1")));

        // Act
        LookupResult<List<DocumentRow>> result = service(connection).list("C-1");

        // Assert
        Assertions.assertEquals(LookupResult.Outcome.REJECTED, result.getOutcome());
        Assertions.assertEquals("The mainframe sent a reply that could not be read.", result.getMessage());
    }

    // DOC-R2, DOC-R5 (only the details are sent)
    @Test
    void addsTheDocumentDetailsAndNothingElse() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested(ADD, FakeConnection.success(added(9, "passport.pdf")));

        // Act
        LookupResult<DocumentRow> result = service(connection).add(" C-1 ",
                new DocumentInput(" Proof of Identity ", " passport.pdf ", " application/pdf ", " 120 "));

        // Assert
        Assertions.assertTrue(result.isSuccessful());
        Assertions.assertEquals(new DocumentRow(9, "Proof of Identity", "passport.pdf", "application/pdf", 120),
                result.getValue());
        MessageRequest sent = connection.getSentRequests().get(0);
        Assertions.assertEquals("C-1", sent.getValue("customer"));
        Assertions.assertEquals("Proof of Identity", sent.getValue("type"));
        Assertions.assertEquals("passport.pdf", sent.getValue("fileName"));
        Assertions.assertEquals("application/pdf", sent.getValue("fileType"));
        Assertions.assertEquals("120", sent.getValue("size"));
        Assertions.assertEquals(5, sent.getValueKeys().length);
    }

    // DOC-R2
    @Test
    void aBlankFileTypeAndSizeAreSentAsEmptyAndZero() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested(ADD, FakeConnection.success(added(9, "a.pdf")));

        // Act
        service(connection).add("C-1", new DocumentInput("Proof of Identity", "a.pdf", null, null));

        // Assert
        MessageRequest sent = connection.getSentRequests().get(0);
        Assertions.assertEquals("", sent.getValue("fileType"));
        Assertions.assertEquals("0", sent.getValue("size"));
    }

    // DOC-R2
    @Test
    void aBlankTypeOrFileNameSendsNothing() {
        // Arrange
        FakeConnection connection = new FakeConnection();

        // Act
        LookupResult<DocumentRow> result = service(connection).add("C-1", new DocumentInput("", " ", "", ""));

        // Assert
        Assertions.assertEquals(LookupResult.Outcome.INVALID, result.getOutcome());
        Assertions.assertEquals(2, result.getValidationErrors().getErrors().size());
        Assertions.assertTrue(connection.getSentRequests().isEmpty());
    }

    // DOC-R2
    @Test
    void aBlankCustomerNumberSendsNothing() {
        // Arrange
        FakeConnection connection = new FakeConnection();

        // Act
        LookupResult<DocumentRow> result = service(connection).add(null, PASSPORT);

        // Assert
        Assertions.assertEquals(LookupResult.Outcome.INVALID, result.getOutcome());
        Assertions.assertEquals(List.of(CustomerDocumentService.CUSTOMER_NUMBER),
                result.getValidationErrors().getErrors().stream().map(FieldError::getFieldName).toList());
        Assertions.assertTrue(connection.getSentRequests().isEmpty());
    }

    // DOC-R6 (a rejection is not retried and keeps the mainframe's reason)
    @Test
    void aRefusedAddReportsTheMainframesReasonAndSendsNothingElse() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested(ADD, FakeConnection.rejected("File name already exists"));

        // Act
        LookupResult<DocumentRow> result = service(connection).add("C-1", PASSPORT);

        // Assert
        Assertions.assertEquals(LookupResult.Outcome.REJECTED, result.getOutcome());
        Assertions.assertEquals("File name already exists", result.getMessage());
        Assertions.assertEquals(1, connection.getSentRequests().size());
    }

    // DOC-R6 (saving needs a connection: without one nothing is saved)
    @Test
    void addingWithoutAConnectionIsANetworkFailure() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.setIsConnected(false);

        // Act and Assert
        Assertions.assertEquals(LookupResult.Outcome.NETWORK_FAILURE,
                service(connection).add("C-1", PASSPORT).getOutcome());
    }

    // DOC-R2 (the starter message throws when the reply leaves out a number)
    @Test
    void anAddReplyTheStarterMessageCannotReadIsRejected() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested(ADD, FakeConnection.success(Map.of("id", "9")));

        // Act
        LookupResult<DocumentRow> result = service(connection).add("C-1", PASSPORT);

        // Assert
        Assertions.assertEquals(LookupResult.Outcome.REJECTED, result.getOutcome());
    }

    // DOC-R3 (the mainframe has no update message: add the new details, then delete the old)
    @Test
    void anUpdateAddsTheNewDetailsAndThenDeletesTheOldDocument() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested(ADD, FakeConnection.success(added(9, "passport-2.pdf")));
        connection.whenRequested(DELETE, FakeConnection.success(Map.of("customer", "C-1", "id", "7")));

        // Act
        LookupResult<DocumentRow> result = service(connection).update("C-1", 7, PASSPORT);

        // Assert
        Assertions.assertTrue(result.isSuccessful());
        Assertions.assertEquals(9, result.getValue().id());
        List<MessageRequest> sent = connection.getSentRequests();
        Assertions.assertEquals(List.of(ADD, DELETE), sent.stream().map(MessageRequest::getRequestType).toList());
        Assertions.assertEquals("7", sent.get(1).getValue("id"));
    }

    // DOC-R3, DOC-R6 (a refused update must lose nothing)
    @Test
    void aRefusedUpdateKeepsTheOldDocument() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested(ADD, FakeConnection.rejected("File name already exists"));

        // Act
        LookupResult<DocumentRow> result = service(connection).update("C-1", 7, PASSPORT);

        // Assert
        Assertions.assertEquals(LookupResult.Outcome.REJECTED, result.getOutcome());
        Assertions.assertEquals("File name already exists", result.getMessage());
        Assertions.assertEquals(1, connection.getSentRequests().size());
    }

    // DOC-R3
    @Test
    void ifTheOldDocumentCannotBeDeletedTheResultSaysTheNewOneWasSaved() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested(ADD, FakeConnection.success(added(9, "passport.pdf")));
        connection.whenRequested(DELETE, FakeConnection.rejected("No such document"));

        // Act
        LookupResult<DocumentRow> result = service(connection).update("C-1", 7, PASSPORT);

        // Assert
        Assertions.assertEquals(LookupResult.Outcome.REJECTED, result.getOutcome());
        Assertions.assertEquals("The new details were saved, but the old copy could not be deleted: No such document",
                result.getMessage());
    }

    // DOC-R3
    @Test
    void anUpdateWithBadInputSendsNothing() {
        // Arrange
        FakeConnection connection = new FakeConnection();

        // Act
        LookupResult<DocumentRow> result = service(connection).update("C-1", 7, new DocumentInput("", "", "", ""));

        // Assert
        Assertions.assertEquals(LookupResult.Outcome.INVALID, result.getOutcome());
        Assertions.assertTrue(connection.getSentRequests().isEmpty());
    }

    // DOC-R4
    @Test
    void deletesTheDocument() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested(DELETE, FakeConnection.success(Map.of("customer", "C-1", "id", "7")));

        // Act
        LookupResult<Long> result = service(connection).delete(" C-1 ", 7);

        // Assert
        Assertions.assertEquals(7L, result.getValue());
        Assertions.assertEquals("C-1", connection.getSentRequests().get(0).getValue("customer"));
    }

    // DOC-R4
    @Test
    void aDeleteNeedsANumberAndCanBeRefusedOrOffline() {
        // Arrange
        FakeConnection refused = new FakeConnection();
        refused.whenRequested(DELETE, FakeConnection.rejected("No such document"));
        FakeConnection offline = new FakeConnection();
        offline.setIsConnected(false);

        // Act and Assert
        Assertions.assertEquals(LookupResult.Outcome.INVALID, service(new FakeConnection()).delete("", 7).getOutcome());
        Assertions.assertEquals("No such document", service(refused).delete("C-1", 7).getMessage());
        Assertions.assertEquals(LookupResult.Outcome.NETWORK_FAILURE, service(offline).delete("C-1", 7).getOutcome());
    }

    private static CustomerDocumentService service(FakeConnection connection) {
        return new CustomerDocumentService(connection);
    }

    private static Map<String, String> added(long id, String fileName) {
        Map<String, String> values = new HashMap<>();
        values.put("customer", "C-1");
        values.put("id", Long.toString(id));
        values.put("type", "Proof of Identity");
        values.put("fileName", fileName);
        values.put("fileType", "application/pdf");
        values.put("size", "120");
        return values;
    }
}
