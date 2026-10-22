package lavs.logic;

import lavs.data.GatewayResult;
import lavs.data.MainframeGateway;
import lavs.model.RecordNote;
import lavs.validation.FieldError;
import lavs.validation.ValidationResult;
import lavs.validation.Validators;
import mainframe.client.MessageRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// AI-assisted: notes share validation and pagination across customer and loan records.
public final class RecordNotesService {
    private final MainframeGateway gateway;
    public RecordNotesService(MainframeGateway gateway) { this.gateway = gateway; }

    public ServiceResult<List<RecordNote>> list(RecordOwner owner, String number) {
        ValidationResult errors = validate(number, null);
        if (!errors.isValid()) return ServiceResult.invalid(errors);
        List<RecordNote> notes = new ArrayList<>();
        for (long page = 1; ; page++) {
            var result = request(false, owner.operation + "Note.list", Map.of(
                    "number", number, "page", Long.toString(page), "size", "50"));
            if (!result.isSuccess()) return ServiceResult.from(result, null);
            try {
                int count = Integer.parseInt(result.getValue("items"));
                long total = Long.parseLong(result.getValue("total"));
                for (int i = 1; i <= count; i++) {
                    long id = Long.parseLong(result.getValue(i + ":id"));
                    var detail = request(false, owner.operation + "Note.retrieve", Map.of("number", number, "id", Long.toString(id)));
                    if (!detail.isSuccess()) return ServiceResult.from(detail, null);
                    notes.add(new RecordNote(id, detail.getValue("type"), detail.getValue("lines"),
                            detail.getValue("author"), detail.getValue("dateAdded")));
                }
                if (count == 0 || notes.size() >= total) return ServiceResult.from(result, List.copyOf(notes));
            } catch (NumberFormatException error) {
                return ServiceResult.from(GatewayResult.from(new mainframe.client.Status(
                        mainframe.client.StatusMessage.UNABLE_TO_PARSE.getCode(), "Could not read the notes reply", 0)), null);
            }
        }
    }

    public ServiceResult<Void> save(RecordOwner owner, String number, Long id, String type, String lines) {
        ValidationResult errors = validate(number, id);
        errors.addIfPresent(Validators.required("type", type));
        errors.addIfPresent(Validators.required("lines", lines));
        if (!errors.isValid()) return ServiceResult.invalid(errors);
        Map<String, String> fields = new HashMap<>();
        fields.put(owner.numberField, number);
        fields.put("type", type.trim());
        fields.put("lines", lines);
        if (id != null) fields.put("id", Long.toString(id));
        return ServiceResult.from(request(true, owner.operation + (id == null ? ".addNote" : ".updateNote"), fields), null);
    }

    public ServiceResult<Void> delete(RecordOwner owner, String number, long id) {
        ValidationResult errors = validate(number, id);
        if (!errors.isValid()) return ServiceResult.invalid(errors);
        return ServiceResult.from(request(true, owner.operation + ".deleteNote",
                Map.of(owner.numberField, number, "id", Long.toString(id))), null);
    }

    private ValidationResult validate(String number, Long id) {
        ValidationResult errors = new ValidationResult();
        errors.addIfPresent(Validators.required("number", number));
        if (id != null && id <= 0) errors.add(new FieldError("id", "must identify a saved note"));
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
