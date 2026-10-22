// AI-assisted: lists, adds, updates and deletes a customer's document details over the mainframe Connection.
// TEMPORARY: swap this package for Scott's logic package once his skeleton is on main.
package lavs.logic;

import lavs.validation.FieldError;
import lavs.validation.ValidationResult;
import lavs.validation.Validators;
import mainframe.client.Connection;
import mainframe.client.Message;
import mainframe.client.Status;
import mainframe.client.StatusMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// Optional component: a customer's documents, as details only (type, file name, file type and
// size); the file itself is never uploaded. The mainframe has no update message, so an update
// adds the new details and then deletes the old ones (the same approach as co-borrowers).
// TEMPORARY: uses Connection directly until Scott's gateway lands, and writes go straight to
// the mainframe (a save while offline fails) until Aum's SyncService is connected to the app,
// when they should be queued; swap then.
public class CustomerDocumentService {
    public static final String CUSTOMER_NUMBER = "customerNumber";
    static final long PAGE_SIZE = 50;
    private static final String UNREADABLE = "The mainframe sent a reply that could not be read.";

    private final Connection _connection;

    public CustomerDocumentService(Connection connection) {
        _connection = connection;
    }

    // Checks the form without contacting the mainframe: a type and a file name are required, and a
    // size, if given, must be a whole number.
    public ValidationResult validate(DocumentInput input) {
        ValidationResult result = new ValidationResult();
        if (Validators.required(DocumentInput.TYPE, input.type()).isPresent()) {
            result.add(new FieldError(DocumentInput.TYPE, "Choose a document type"));
        }
        if (Validators.required(DocumentInput.FILE_NAME, input.fileName()).isPresent()) {
            result.add(new FieldError(DocumentInput.FILE_NAME, "Enter a file name"));
        }
        if (isPresent(input.size()) && !input.size().trim().matches("\\d{1,18}")) {
            result.add(new FieldError(DocumentInput.SIZE, "Enter a whole number"));
        }
        return result;
    }

    // Every document of the customer. The list message has only the type and file name, so each
    // document is then retrieved for its file type and size; a customer has only a few.
    // Requests pages 1, 2, ... and also stops on an empty page, so a wrong total from the server
    // cannot cause an endless loop.
    public LookupResult<List<DocumentRow>> list(String customerNumber) {
        LookupResult<List<DocumentRow>> invalid = invalid(customerNumber);
        if (invalid != null) {
            return invalid;
        }
        String number = customerNumber.trim();
        try {
            List<DocumentRow> rows = new ArrayList<>();
            long page = 1;
            while (true) {
                // a new message per page, because message objects keep the items from earlier sends
                mainframe.client.messages.customerDocument.List message =
                        new mainframe.client.messages.customerDocument.List(number, page, PAGE_SIZE);
                send(message);
                mainframe.client.messages.customerDocument.List.Item[] items = message.getItems();
                for (mainframe.client.messages.customerDocument.List.Item item : items) {
                    var details = new mainframe.client.messages.customerDocument.Retrieve(item.getId(), number);
                    send(details);
                    rows.add(new DocumentRow(item.getId(), item.getType(), item.getFileName(),
                            details.getFileType(), details.getSize()));
                }
                if (items.length == 0 || rows.size() >= message.getTotal()) {
                    return LookupResult.success(rows);
                }
                page++;
            }
        } catch (Failure failure) {
            return failure.toResult();
        }
    }

    // Adds the document. The result's value is the document as the mainframe saved it.
    public LookupResult<DocumentRow> add(String customerNumber, DocumentInput input) {
        LookupResult<DocumentRow> invalid = invalidInput(customerNumber, input);
        if (invalid != null) {
            return invalid;
        }
        try {
            return LookupResult.success(addDocument(customerNumber.trim(), input));
        } catch (Failure failure) {
            return failure.toResult();
        }
    }

    // Saves changes by adding the new details first and only then deleting the old document, so
    // a refusal of the new details loses nothing. If the old one then cannot be deleted the
    // result says so (the new details are saved and the customer has both).
    public LookupResult<DocumentRow> update(String customerNumber, long existingId, DocumentInput input) {
        LookupResult<DocumentRow> invalid = invalidInput(customerNumber, input);
        if (invalid != null) {
            return invalid;
        }
        String number = customerNumber.trim();
        DocumentRow saved;
        try {
            saved = addDocument(number, input);
        } catch (Failure failure) {
            return failure.toResult();
        }
        try {
            deleteDocument(number, existingId);
        } catch (Failure failure) {
            return failure.withPrefix("The new details were saved, but the old copy could not be deleted: ")
                    .toResult();
        }
        return LookupResult.success(saved);
    }

    // Deletes the document. The result's value is its id.
    public LookupResult<Long> delete(String customerNumber, long id) {
        LookupResult<Long> invalid = invalid(customerNumber);
        if (invalid != null) {
            return invalid;
        }
        try {
            deleteDocument(customerNumber.trim(), id);
            return LookupResult.success(id);
        } catch (Failure failure) {
            return failure.toResult();
        }
    }

    private DocumentRow addDocument(String number, DocumentInput input) throws Failure {
        long size = isPresent(input.size()) ? Long.parseLong(input.size().trim()) : 0;
        var message = new mainframe.client.messages.customer.AddDocument(number, input.fileName().trim(),
                isPresent(input.fileType()) ? input.fileType().trim() : "", size, input.type().trim());
        send(message);
        return new DocumentRow(message.getId(), message.getType(), message.getFileName(), message.getFileType(),
                message.getSize());
    }

    private void deleteDocument(String number, long id) throws Failure {
        send(new mainframe.client.messages.customer.DeleteDocument(number, id));
    }

    private static boolean isPresent(String text) {
        return text != null && !text.isBlank();
    }

    private static <T> LookupResult<T> invalid(String customerNumber) {
        ValidationResult validation = new ValidationResult();
        validation.addIfPresent(Validators.required(CUSTOMER_NUMBER, customerNumber));
        return validation.isValid() ? null : LookupResult.invalid(validation);
    }

    private <T> LookupResult<T> invalidInput(String customerNumber, DocumentInput input) {
        LookupResult<T> invalid = invalid(customerNumber);
        if (invalid != null) {
            return invalid;
        }
        ValidationResult validation = validate(input);
        return validation.isValid() ? null : LookupResult.invalid(validation);
    }

    // Sends the message, turning anything but success into a Failure. RuntimeException: the
    // starter messages throw a NullPointerException when a reply leaves out a number, and they
    // are not ours to change.
    private void send(Message message) throws Failure {
        Status status;
        try {
            status = message.send(_connection);
        } catch (IOException ex) {
            throw new Failure(true, ex.getMessage());
        } catch (RuntimeException ex) {
            throw new Failure(false, UNREADABLE);
        }
        if (!status.getWasSuccessful()) {
            boolean network = StatusMessage.NETWORK_FAILURE_UNAVAILABLE.getCode().equals(status.getErrorCode());
            throw new Failure(network, status.getErrorMessage());
        }
    }

    // Used only inside this class to stop at the first failure; the public methods turn it
    // back into a result, so callers never see it.
    private static final class Failure extends Exception {
        private final boolean _network;

        Failure(boolean network, String message) {
            super(message);
            _network = network;
        }

        Failure withPrefix(String prefix) {
            return new Failure(_network, prefix + getMessage());
        }

        <T> LookupResult<T> toResult() {
            return _network ? LookupResult.networkFailure(getMessage()) : LookupResult.rejected(getMessage());
        }
    }
}
