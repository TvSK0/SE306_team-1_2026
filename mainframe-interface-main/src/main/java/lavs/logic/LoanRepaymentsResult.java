// TEMPORARY: swap this package for Scott's logic package once his skeleton is on main.
package lavs.logic;

import mainframe.client.Status;

import java.util.List;

// The outcome of loading a loan's summary and repayment schedule: both of them, or the
// Status that stopped the load. Nothing is returned from a half-finished load.
// TEMPORARY: swap this for Scott's gateway result type once it exists.
public final class LoanRepaymentsResult {
    private final Status _mainframeStatus;
    private final LoanSummary _summary;
    private final List<RepaymentRow> _rows;

    private LoanRepaymentsResult(Status mainframeStatus, LoanSummary summary, List<RepaymentRow> rows) {
        _mainframeStatus = mainframeStatus;
        _summary = summary;
        _rows = rows;
    }

    static LoanRepaymentsResult loaded(Status status, LoanSummary summary, List<RepaymentRow> rows) {
        return new LoanRepaymentsResult(status, summary, List.copyOf(rows));
    }

    static LoanRepaymentsResult failed(Status status) {
        return new LoanRepaymentsResult(status, null, List.of());
    }

    public boolean isSuccessful() {
        return _summary != null;
    }

    // the Status of the last request sent; on failure, the one that failed
    public Status getMainframeStatus() {
        return _mainframeStatus;
    }

    // null unless the load worked
    public LoanSummary getSummary() {
        return _summary;
    }

    // every payment in order; empty unless the load worked
    public List<RepaymentRow> getRows() {
        return _rows;
    }
}
