// AI-assisted: what the customer search screen is showing: the rows, the total and the page.
package lavs.ui;

import lavs.logic.CustomerSearchResult;
import lavs.model.Customer;
import lavs.validation.ValidationResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Plain (no JavaFX), so it is unit tested; CustomerSearchScreen copies it onto the controls.
// A new search replaces the rows and goes back to page 1; "Show more" appends the next page
// (US2-R8). A failed search or page keeps whatever was already showing.
public final class CustomerSearchState {
    private String _query = "";
    private List<Customer> _customers = List.of();
    private long _total;
    private long _page;
    private boolean _searched;

    // fieldErrors: INVALID only, shown inline. banner: a network failure or a refusal.
    // newRows: present only when the search worked, so the rows were replaced.
    public record Display(ValidationResult fieldErrors, Optional<BannerContent> banner,
                          Optional<List<Customer>> newRows) {
    }

    // banner: a network failure or a refusal. added: the rows appended (empty on a failure).
    public record More(Optional<BannerContent> banner, List<Customer> added) {
    }

    // The result of a new search for query (as typed). On success the rows are replaced.
    public Display applySearch(String query, CustomerSearchResult result) {
        if (result.isSuccessful()) {
            _query = query.trim();
            _customers = result.getCustomers();
            _total = result.getTotal();
            _page = 1;
            _searched = true;
        }
        return new Display(result.getValidationErrors(), failure(result),
                Optional.of(result.getCustomers()).filter(rows -> result.isSuccessful()));
    }

    // The result of "Show more": on success its rows are added under the ones already shown.
    // The page counter is our own, because a mainframe that leaves out "page" would otherwise
    // make us ask for the same page again. A page with no rows ends the list, so a wrong total
    // cannot leave a button that never does anything.
    public More applyMore(CustomerSearchResult result) {
        if (result.isSuccessful()) {
            List<Customer> all = new ArrayList<>(_customers);
            all.addAll(result.getCustomers());
            _customers = List.copyOf(all);
            _page++;
            _total = result.getCustomers().isEmpty() ? _customers.size() : result.getTotal();
        }
        return new More(failure(result), result.getCustomers());
    }

    private static Optional<BannerContent> failure(CustomerSearchResult result) {
        return switch (result.getOutcome()) {
            case NETWORK_FAILURE -> Optional.of(BannerContent.offline());
            case REJECTED -> Optional.of(CustomerSearchText.rejected(result.getMessage()));
            case SUCCESS, INVALID -> Optional.empty();
        };
    }

    // true once a search has worked: before that the results area is not shown at all
    public boolean hasSearched() {
        return _searched;
    }

    public List<Customer> customers() {
        return _customers;
    }

    // the trimmed query of the search the rows came from
    public String query() {
        return _query;
    }

    // the page "Show more" should ask for
    public long nextPage() {
        return _page + 1;
    }

    public String countLine() {
        return CustomerSearchText.countLine(_total);
    }

    public String emptyText() {
        return CustomerSearchText.emptyText(_query);
    }

    public boolean canShowMore() {
        return _customers.size() < _total;
    }

    public String showMoreLabel() {
        return CustomerSearchText.showMoreLabel(_customers.size(), _total);
    }
}
