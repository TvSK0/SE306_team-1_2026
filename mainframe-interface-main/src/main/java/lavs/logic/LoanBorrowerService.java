package lavs.logic;

import lavs.model.LoanCoborrower;
import lavs.validation.FieldError;
import lavs.validation.ValidationResult;
import lavs.validation.Validators;
import mainframe.client.Connection;
import lavs.data.MainframeGateway;
import mainframe.client.Status;
import mainframe.client.messages.loan.AddCoborrower;
import mainframe.client.messages.loan.DeleteCoborrower;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// US8: co-borrowers and guarantors on a loan. The Borrowers tab of the loan
// screen (LoanBorrowersController) calls these.
public final class LoanBorrowerService {
    // the two kinds of borrower, so screens never have to import the mainframe message classes
    public static final String COBORROWER = AddCoborrower.TYPE_COBORROWER;
    public static final String GUARANTOR = AddCoborrower.TYPE_GUARANTOR;

    // the field names of the errors addCoborrower and updateCoborrower can return
    public static final String CUSTOMER_NUMBER = "customerNumber";
    public static final String TYPE = "type";

    static final long PAGE_SIZE = 50;

    private final MainframeGateway _gateway;

    // AI-assisted: all application traffic uses the shared gateway.
    public LoanBorrowerService(MainframeGateway gateway) {
        _gateway = gateway;
    }

    public LoanBorrowerService(Connection connection) {
        _gateway = new MainframeGateway(connection);
    }

    // AI-assisted: every co-borrower and guarantor on a loan, using
    // loanCoborrower.List. Requests pages 1, 2, ... until all of them are
    // collected, and also stops on an empty page so a wrong total from the
    // server cannot cause an endless loop (same approach as
    // CategoryConstantsLoader).
    public LoanBorrowerListResult listCoborrowers(String loanNumber) throws IOException {
        ValidationResult validation = new ValidationResult();
        validation.addIfPresent(Validators.required("loanNumber", loanNumber));
        if (!validation.isValid()) {
            return LoanBorrowerListResult.invalid(validation);
        }

        List<LoanCoborrower> coborrowers = new ArrayList<>();
        long page = 1;
        while (true) {
            // a new message per page, because message objects keep the items from earlier sends
            mainframe.client.messages.loanCoborrower.List message =
                    new mainframe.client.messages.loanCoborrower.List(loanNumber, page, PAGE_SIZE);
            Status status = _gateway.send(message).getStatus();
            if (!status.getWasSuccessful()) {
                return LoanBorrowerListResult.fromStatus(status, List.of());
            }
            mainframe.client.messages.loanCoborrower.List.Item[] items = message.getItems();
            for (mainframe.client.messages.loanCoborrower.List.Item item : items) {
                LoanCoborrower coborrower = new LoanCoborrower(loanNumber, item.getCoborrowerNumber(), item.getType());
                coborrower.setId(item.getId());
                coborrower.setCoborrowerName(item.getCoborrowerName());
                coborrowers.add(coborrower);
            }
            if (items.length == 0 || coborrowers.size() >= message.getTotal()) {
                return LoanBorrowerListResult.fromStatus(status, coborrowers);
            }
            page++;
        }
    }

    // Adds a customer as a co-borrower or guarantor on a loan, using
    // loan.AddCoborrower. Returns field errors - without contacting the
    // mainframe - if loanNumber/customerNumber/type are missing, or type
    // isn't AddCoborrower.TYPE_COBORROWER/TYPE_GUARANTOR.
    public LoanBorrowerResult addCoborrower(String loanNumber, String customerNumber, String type) throws IOException {
        ValidationResult validation = validateCoborrower(loanNumber, customerNumber, type);
        if (!validation.isValid()) {
            return LoanBorrowerResult.invalid(validation);
        }
        return LoanBorrowerResult.fromStatus(sendAdd(loanNumber, customerNumber, type));
    }

    // Removes an existing co-borrower/guarantor record from a loan, using
    // loan.DeleteCoborrower.
    public LoanBorrowerResult removeCoborrower(long coborrowerRecordId, String loanNumber) throws IOException {
        ValidationResult validation = new ValidationResult();
        validation.addIfPresent(Validators.required("loanNumber", loanNumber));
        if (!validation.isValid()) {
            return LoanBorrowerResult.invalid(validation);
        }
        return LoanBorrowerResult.fromStatus(sendDelete(coborrowerRecordId, loanNumber));
    }

    // US8: "updates remove then re-add" - the mainframe has no update
    // message for a co-borrower/guarantor, so changing one removes the
    // existing record (as read by listCoborrowers) and adds the replacement.
    // If the removal fails, the add is never attempted, so a failed update
    // can't leave two records for the same person.
    //
    // AI-assisted: if the mainframe refuses the replacement, the existing
    // record is added back as it was, so a refused change doesn't quietly
    // take the person off the loan. That is a best effort - the refusal is
    // what's returned either way, and the screen reloads the list to show
    // what the mainframe now has.
    public LoanBorrowerResult updateCoborrower(LoanCoborrower existing, String customerNumber, String type) throws IOException {
        String loanNumber = existing.getLoanNumber();
        ValidationResult validation = validateCoborrower(loanNumber, customerNumber, type);
        if (!validation.isValid()) {
            return LoanBorrowerResult.invalid(validation);
        }

        if (_gateway.queuesWrites()) {
            return LoanBorrowerResult.fromStatus(_gateway.send(connection -> {
                var request = new mainframe.client.MessageRequest(
                        mainframe.client.MessageRequest.UPDATE, "loan.replaceCoborrower");
                request.setValue("loanNumber", loanNumber);
                request.setValue("id", Long.toString(existing.getId()));
                request.setValue("coborrowerNumber", customerNumber);
                request.setValue("type", type);
                request.setValue("previousCustomer", existing.getCoborrowerNumber());
                request.setValue("previousType", existing.getType());
                return connection.send(request).getStatus();
            }).getStatus());
        }

        Status deleteStatus = sendDelete(existing.getId(), loanNumber);
        if (!deleteStatus.getWasSuccessful()) {
            return LoanBorrowerResult.fromStatus(deleteStatus);
        }
        Status addStatus = sendAdd(loanNumber, customerNumber, type);
        if (!addStatus.getWasSuccessful()) {
            sendAdd(loanNumber, existing.getCoborrowerNumber(), existing.getType());
        }
        return LoanBorrowerResult.fromStatus(addStatus);
    }

    private ValidationResult validateCoborrower(String loanNumber, String customerNumber, String type) {
        ValidationResult validation = new ValidationResult();
        validation.addIfPresent(Validators.required("loanNumber", loanNumber));
        validation.addIfPresent(Validators.required(CUSTOMER_NUMBER, customerNumber));
        validation.addIfPresent(Validators.required(TYPE, type));
        if (type != null && !type.isBlank()
                && !AddCoborrower.TYPE_COBORROWER.equals(type)
                && !AddCoborrower.TYPE_GUARANTOR.equals(type)) {
            validation.add(new FieldError(TYPE,
                    "must be '" + AddCoborrower.TYPE_COBORROWER + "' or '" + AddCoborrower.TYPE_GUARANTOR + "'"));
        }
        return validation;
    }

    private Status sendAdd(String loanNumber, String customerNumber, String type) throws IOException {
        return _gateway.send(new AddCoborrower(customerNumber, loanNumber, type)).getStatus();
    }

    private Status sendDelete(long id, String loanNumber) throws IOException {
        return _gateway.send(new DeleteCoborrower(id, loanNumber)).getStatus();
    }
}
