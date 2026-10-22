package lavs.logic;

import lavs.data.GatewayResult;
import lavs.data.MainframeGateway;
import lavs.model.RecordDocument;
import lavs.validation.FieldError;
import lavs.validation.ValidationResult;
import lavs.validation.Validators;
import mainframe.client.MessageRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// AI-assisted: documents share validation and pagination across customer and loan records.
public final class RecordDocumentsService {
    private final MainframeGateway gateway;
    public RecordDocumentsService(MainframeGateway gateway) { this.gateway = gateway; }

    public ServiceResult<List<RecordDocument>> list(RecordOwner owner, String number) {
        ValidationResult errors = validate(number, null);
        if (!errors.isValid()) return ServiceResult.invalid(errors);
        List<RecordDocument> documents = new ArrayList<>();
        for (long page = 1; ; page++) {
            var result = request(false, owner.operation + "Document.list", Map.of(
                    "number", number, "page", Long.toString(page), "size", "50"));
            if (!result.isSuccess()) return ServiceResult.from(result, null);
            try {
                int count = Integer.parseInt(result.getValue("items"));
                long total = Long.parseLong(result.getValue("total"));
                for (int i = 1; i <= count; i++) {
                    long id = Long.parseLong(result.getValue(i + ":id"));
                    var detail = request(false, owner.operation + "Document.retrieve", Map.of("number", number, "id", Long.toString(id)));
                    if (!detail.isSuccess()) return ServiceResult.from(detail, null);
                    documents.add(new RecordDocument(id, detail.getValue("type"), detail.getValue("fileName"),
                            detail.getValue("fileType"), Long.parseLong(detail.getValue("size"))));
                }
                if (count == 0 || documents.size() >= total) return ServiceResult.from(result, List.copyOf(documents));
            } catch (NumberFormatException error) {
                return ServiceResult.from(GatewayResult.from(new mainframe.client.Status(
                        mainframe.client.StatusMessage.UNABLE_TO_PARSE.getCode(), "Could not read the documents reply", 0)), null);
            }
        }
    }

    public ServiceResult<Void> save(RecordOwner owner, String number, Long id, String type,
                                    String fileName, String fileType, String size) {
        ValidationResult errors = validate(number, id);
        errors.addIfPresent(Validators.required("type", type));
        errors.addIfPresent(Validators.required("fileName", fileName));
        errors.addIfPresent(Validators.required("fileType", fileType));
        try {
            if (Long.parseLong(size) < 0) throw new NumberFormatException();
        } catch (NumberFormatException error) {
            errors.add(new FieldError("size", "must be a whole number of bytes, zero or greater"));
        }
        if (!errors.isValid()) return ServiceResult.invalid(errors);
        if (id != null && !gateway.queuesWrites()) {
            throw new IllegalStateException("Document updates require the durable upload queue");
        }
        Map<String, String> fields = new HashMap<>();
        fields.put(owner.numberField, number);
        fields.put("type", type.trim());
        fields.put("fileName", fileName.trim());
        fields.put("fileType", fileType.trim());
        fields.put("size", size);
        if (id != null) fields.put("id", Long.toString(id));
        return ServiceResult.from(request(true, owner.operation + (id == null ? ".addDocument" : ".replaceDocument"), fields), null);
    }

    public ServiceResult<Void> delete(RecordOwner owner, String number, long id) {
        ValidationResult errors = validate(number, id);
        if (!errors.isValid()) return ServiceResult.invalid(errors);
        return ServiceResult.from(request(true, owner.operation + ".deleteDocument",
                Map.of(owner.numberField, number, "id", Long.toString(id))), null);
    }

    private ValidationResult validate(String number, Long id) {
        ValidationResult errors = new ValidationResult();
        errors.addIfPresent(Validators.required("number", number));
        if (id != null && id <= 0) errors.add(new FieldError("id", "must identify a saved document"));
        return errors;
    }

    private GatewayResult request(boolean write, String operation, Map<String, String> fields) {
        return gateway.send(connection -> {
            MessageRequest request = new MessageRequest(write ? MessageRequest.UPDATE : MessageRequest.QUERY, operation);
            fields.forEach(request::setValue);
            return connection.send(request).getStatus();
        });
    }
}
