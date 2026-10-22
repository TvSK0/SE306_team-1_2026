// AI-assisted: the Loans section of the customer record (Figma 03a, 08).
package lavs.ui;

import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lavs.logic.CustomerLoansService;
import lavs.logic.LoanRow;
import lavs.logic.LoanService;
import lavs.model.Customer;

import java.util.Optional;
import java.util.function.Consumer;

// Thin control: the wording and the decisions are in LoansSectionText, LoanRowFormatter and
// NewLoanCheck (unit tested), and the work is in CustomerLoansService. Every call goes through
// BackgroundTasks so the window never freezes. The list loads when the section is opened and
// again each time the record is returned to (Refreshable), e.g. after a loan was saved.
// "Open loan" and "+ New loan" hand the loan screen the whole customer, so its Back link can show
// the name and return to this record.
public class CustomerLoansSection extends VBox implements Refreshable {
    private final SectionContext _context;
    private final CustomerLoansService _service;
    private final LoanService _loanService;
    private final BackgroundTasks _tasks;
    private final Consumer<LoanScreenRequest> _openLoanScreen;

    private final Banner _banner = new Banner();
    private final SkeletonRows _skeleton = new SkeletonRows(3);
    private final RecordTable<LoanRow> _table = new RecordTable<>();

    // openLoanScreen is navigator.goTo(Screen.LOAN, request)
    public CustomerLoansSection(SectionContext context, CustomerLoansService service, LoanService loanService,
                                BackgroundTasks tasks, Consumer<LoanScreenRequest> openLoanScreen) {
        super(12);
        _context = context;
        _service = service;
        _loanService = loanService;
        _tasks = tasks;
        _openLoanScreen = openLoanScreen;

        SectionHeading heading = SectionHeading.section("Loans", LoansSectionText.subtitle(label()));
        heading.setAction("+ New loan", this::newLoan);
        _table.column("Loan no.", LoanRow::loanNumber, 2)
                .nodeColumn("Role", row -> tag(LoanRowFormatter.roleTag(row.role())), 2)
                .nodeColumn("Status", row -> tag(LoanRowFormatter.statusTag(row.status())), 2)
                .column("Principal", row -> LoanRowFormatter.money(row.principal()), 2)
                .column("Start date", row -> LoanRowFormatter.date(row.startDate()), 2)
                .rowAction("Open loan", this::openLoan);
        _table.setEmptyText(LoansSectionText.EMPTY);
        VBox.setVgrow(_table, Priority.ALWAYS);
        getChildren().addAll(heading, _banner, _skeleton, _table);
        showLoading(false);
        load();
    }

    @Override
    public void onReturn() {
        load();
    }

    private void load() {
        _tasks.run(() -> _service.listLoans(_context.customerNumber()), () -> {
            _banner.hide();
            showLoading(true);
        }, result -> {
            showLoading(false);
            LoansSectionText.loadFailure(result).ifPresent(_banner::show);
            // a failed read leaves the rows that were showing; a successful one replaces them
            Optional.ofNullable(result.getValue()).ifPresent(_table::setRows);
        }, failure -> {
            showLoading(false);
            _banner.show(LoansSectionText.unexpected("Couldn't load the loans", failure));
        });
    }

    private void openLoan(LoanRow row) {
        _banner.hide();
        _tasks.run(() -> _service.openLoan(row.loanNumber()), result -> {
            LoansSectionText.openFailure(result).ifPresent(_banner::show);
            Optional.ofNullable(result.getValue())
                    .ifPresent(loan -> _openLoanScreen.accept(LoanScreenRequest.forLoan(customer(), loan)));
        }, failure -> _banner.show(LoansSectionText.unexpected("Couldn't open the loan", failure)));
    }

    private void newLoan() {
        _banner.hide();
        _tasks.run(() -> _service.startLoanFacts(_context.customerNumber()), facts -> {
            NewLoanCheck.Decision decision = NewLoanCheck.decide(_loanService, label(), facts);
            decision.banner().ifPresent(content -> _banner.showWithLink(content, decision.linkText(),
                    () -> decision.linkSection().ifPresent(_context.goTo()::accept)));
            Optional.of(decision).filter(NewLoanCheck.Decision::canStart)
                    .ifPresent(go -> _openLoanScreen.accept(LoanScreenRequest.forNewLoan(customer())));
        }, failure -> _banner.show(LoansSectionText.unexpected("Couldn't start a new loan", failure)));
    }

    private Customer customer() {
        return LoansSectionText.customerFor(_context.customer().get(), _context.customerNumber());
    }

    private String label() {
        return LoansSectionText.customerLabel(_context.customer().get(), _context.customerNumber());
    }

    private static Label tag(CustomerSearchText.StatusTag tag) {
        return new Tag(tag.variant(), tag.text());
    }

    // reads get skeleton rows (no Cancel: the list loads by itself when the section opens)
    private void showLoading(boolean loading) {
        _skeleton.setVisible(loading);
        _skeleton.setManaged(loading);
    }
}
