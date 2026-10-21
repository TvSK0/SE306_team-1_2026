// TEMPORARY: swap this package for Scott's ui package once his skeleton is on main.
package lavs.ui;

import lavs.logic.LoanBorrowerResult;
import lavs.logic.LoanBorrowerService;
import lavs.model.LoanCoborrower;
import mainframe.client.Status;
import mainframe.client.StatusMessage;

import java.io.UncheckedIOException;

// AI-assisted: US8, how the Borrowers tab names people and types and what it says after each
// change. Kept free of JavaFX so it can be unit tested; LoanBorrowersController shows it.
public final class BorrowersText {
    public static final String NOBODY_YET = "Nobody else is on this loan yet.";
    public static final String LOADING = "Loading...";
    public static final String NOT_LOADED = "Co-borrowers and guarantors not loaded.";

    private BorrowersText() {}

    // "Co-borrower" or "Guarantor", as the MMM reads them; anything else is shown as it came
    public static String typeLabel(String type) {
        if (LoanBorrowerService.COBORROWER.equals(type)) {
            return "Co-borrower";
        }
        if (LoanBorrowerService.GUARANTOR.equals(type)) {
            return "Guarantor";
        }
        return type == null ? "" : type;
    }

    // what "change type" switches a borrower to
    public static String otherType(String type) {
        return LoanBorrowerService.GUARANTOR.equals(type) ? LoanBorrowerService.COBORROWER : LoanBorrowerService.GUARANTOR;
    }

    // "Make guarantor" for a co-borrower and the other way round; "Change type" with nobody selected
    public static String changeTypeButton(LoanCoborrower selected) {
        if (selected == null) {
            return "Change type";
        }
        return "Make " + typeLabel(otherType(selected.getType())).toLowerCase();
    }

    // the name the mainframe gave, or the customer number when it gave none
    public static String name(LoanCoborrower borrower) {
        String name = borrower.getCoborrowerName();
        return name == null || name.isBlank() ? borrower.getCoborrowerNumber() : name;
    }

    public static String confirmRemove(LoanCoborrower borrower) {
        return "Remove " + name(borrower) + " from this loan?";
    }

    public static String added(String customerNumber, String type) {
        return "Added " + customerNumber + " as " + withArticle(type) + ".";
    }

    public static String changed(LoanCoborrower borrower, String newType) {
        return name(borrower) + " is now " + withArticle(newType) + ".";
    }

    public static String removed(LoanCoborrower borrower) {
        return "Removed " + name(borrower) + " from the loan.";
    }

    // successText is what to say when the change worked
    public static String forChange(LoanBorrowerResult result, String successText) {
        if (result.isSuccessful()) {
            return successText;
        }
        Status status = result.getMainframeStatus();
        // no status means the field checks stopped it before the mainframe was contacted
        if (status == null) {
            return "Please fix the highlighted fields.";
        }
        return forFailure(status, "The mainframe didn't accept the change: ");
    }

    // when the list itself couldn't be read
    public static String forListFailure(Status status) {
        return forFailure(status, "The mainframe couldn't list the co-borrowers and guarantors: ");
    }

    // for something a service threw rather than returned
    public static String forError(Throwable failure) {
        if (failure instanceof UncheckedIOException) {
            return couldNotReachTheMainframe();
        }
        return "Something went wrong: " + failure.getMessage();
    }

    private static String forFailure(Status status, String refusedPrefix) {
        if (StatusMessage.NETWORK_FAILURE_UNAVAILABLE.getCode().equals(status.getErrorCode())) {
            return couldNotReachTheMainframe();
        }
        return refusedPrefix + status.getErrorMessage();
    }

    private static String couldNotReachTheMainframe() {
        return "Couldn't reach the mainframe. Check your connection and try again.";
    }

    // "a co-borrower" or "a guarantor"
    private static String withArticle(String type) {
        return "a " + typeLabel(type).toLowerCase();
    }
}
