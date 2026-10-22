package lavs.logic;

import mainframe.client.Connection;
import lavs.data.MainframeGateway;
import mainframe.client.Status;
import mainframe.client.StatusMessage;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// US7: fetches a loan's summary and its full repayment schedule from the mainframe.
// The load is all-or-nothing: if any request fails, nothing is returned, so the screen
// never shows part of a schedule as if it were the whole thing.
public class LoanRepaymentsService {
    public static final long DEFAULT_PAGE_SIZE = 100;

    private final MainframeGateway _gateway;
    private final long _pageSize;

    public LoanRepaymentsService(Connection connection) {
        this(connection, DEFAULT_PAGE_SIZE);
    }

    public LoanRepaymentsService(MainframeGateway gateway) {
        _gateway = gateway;
        _pageSize = DEFAULT_PAGE_SIZE;
    }

    public LoanRepaymentsService(Connection connection, long pageSize) {
        _gateway = new MainframeGateway(connection);
        _pageSize = pageSize;
    }

    public LoanRepaymentsResult load(String loanNumber) throws IOException {
        if (loanNumber == null || loanNumber.isBlank()) {
            throw new IllegalArgumentException("a loan number is needed to load its repayments");
        }
        // the starter messages throw if the reply is missing a value they parse,
        // so treat that like any other unreadable reply
        try {
            return loadSummaryAndSchedule(loanNumber);
        } catch (RuntimeException e) {
            return LoanRepaymentsResult.failed(new Status(
                    StatusMessage.UNABLE_TO_PARSE.getCode(), StatusMessage.UNABLE_TO_PARSE.getMessage(), 0));
        }
    }

    private LoanRepaymentsResult loadSummaryAndSchedule(String loanNumber) throws IOException {
        mainframe.client.messages.loanSummary.Retrieve summaryMessage =
                new mainframe.client.messages.loanSummary.Retrieve(loanNumber);
        Status status = _gateway.send(summaryMessage).getStatus();
        if (!status.getWasSuccessful()) {
            return LoanRepaymentsResult.failed(status);
        }
        LoanSummary summary = new LoanSummary(
                toDecimal(summaryMessage.getTotalAmountPaid()),
                toDecimal(summaryMessage.getTotalInterestPaid()),
                summaryMessage.getNumberOfPayments(),
                LocalDate.parse(summaryMessage.getFinalPaymentDate()),
                toDecimal(summaryMessage.getFinalPaymentAmount()));

        List<RepaymentRow> rows = new ArrayList<>();
        long page = 1;
        while (true) {
            // a new message per page: the starter messages keep the items from earlier sends
            mainframe.client.messages.loanRepaymentSchedule.List pageMessage =
                    new mainframe.client.messages.loanRepaymentSchedule.List(loanNumber, page, _pageSize);
            status = _gateway.send(pageMessage).getStatus();
            if (!status.getWasSuccessful()) {
                return LoanRepaymentsResult.failed(status);
            }
            mainframe.client.messages.loanRepaymentSchedule.List.Item[] items = pageMessage.getItems();
            for (mainframe.client.messages.loanRepaymentSchedule.List.Item item : items) {
                rows.add(new RepaymentRow(
                        item.getPaymentNumber(),
                        LocalDate.parse(item.getDate()),
                        toDecimal(item.getAmountPaid()),
                        toDecimal(item.getPrincipalAmount()),
                        toDecimal(item.getInterestAmount()),
                        toDecimal(item.getAmountRemaining())));
            }
            // an empty page also ends the loop, in case the total is wrong
            if (items.length == 0 || rows.size() >= pageMessage.getTotal()) {
                break;
            }
            page++;
        }
        return LoanRepaymentsResult.loaded(status, summary, rows);
    }

    private static BigDecimal toDecimal(Double value) {
        return BigDecimal.valueOf(value);
    }
}
