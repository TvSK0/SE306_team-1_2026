// AI-assisted: the customer search screen (Figma 02, 09, 12): search box, results table and Show more.
package lavs.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lavs.logic.CustomerSearchService;
import lavs.model.Customer;

import java.util.Map;
import java.util.function.Consumer;

// Thin control: what to show is decided in CustomerSearchState and CustomerSearchText (unit
// tested) and the work is in CustomerSearchService. Every call goes through BackgroundTasks so
// the window never freezes. While a search runs, Search shows "Searching…" and is disabled,
// skeleton rows show and Cancel drops the answer (a cancelled search never updates the screen).
public class CustomerSearchScreen {
    private final CustomerSearchService _service;
    private final BackgroundTasks _tasks;
    private final StatusFeed _feed;
    private final Consumer<Runnable> _uiThread;
    private final CustomerSearchMode _mode;

    private final CustomerSearchState _model = new CustomerSearchState();
    private final TextField _query = new TextField();
    private final FormField _queryField = new FormField("Search by name or customer number", _query);
    private final FieldErrorPresenter _presenter =
            new FieldErrorPresenter(Map.of(CustomerSearchService.QUERY, _queryField));
    private final VBox _extras = new VBox();
    private final VBox _footer = new VBox();
    private final Banner _offline = new Banner();
    private final Banner _banner = new Banner();
    private final Button _search = new Button("Search");
    private final Button _cancel = new Button("Cancel");
    private final SkeletonRows _skeleton = new SkeletonRows(4);
    private final LoadingState _searching = LoadingState.forRead("Search", "Searching…");
    private final LoadingView _loading = new LoadingView(_searching, _search, _cancel, _skeleton);
    private final Label _count = new Label();
    private final RecordTable<Customer> _table = new RecordTable<>();
    private final Button _more = new Button();
    private final VBox _results = new VBox(12);

    // handles start as ones that are already finished, so cancelling them needs no null check
    private BackgroundTasks.Handle _searchHandle = new BackgroundTasks.Handle();
    private BackgroundTasks.Handle _moreHandle = new BackgroundTasks.Handle();

    // the mode decides the heading, the columns and what a row does (normal search or select mode)
    public CustomerSearchScreen(CustomerSearchService service, BackgroundTasks tasks, StatusFeed feed,
                                Consumer<Runnable> uiThread, CustomerSearchMode mode) {
        _service = service;
        _tasks = tasks;
        _feed = feed;
        _uiThread = uiThread;
        _mode = mode;
    }

    public Node create() {
        SectionHeading heading = SectionHeading.page(_mode.title(), _mode.subtitle());
        _mode.configureHeading(heading);
        _mode.extraControls().ifPresent(_extras.getChildren()::add);
        _mode.footer().ifPresent(_footer.getChildren()::add);

        _search.getStyleClass().add("lavs-button-primary");
        _cancel.getStyleClass().add("lavs-button-secondary");
        _search.setOnAction(event -> search());
        _cancel.setOnAction(event -> cancelSearch());
        _query.setOnAction(event -> _search.fire());
        HBox.setHgrow(_queryField, Priority.ALWAYS);
        HBox searchRow = new HBox(12, _queryField, _search, _cancel);
        searchRow.setAlignment(Pos.BOTTOM_LEFT);

        _count.getStyleClass().add("lavs-subtitle");
        _mode.configureTable(_table);
        _more.getStyleClass().add("lavs-button-secondary");
        _more.setOnAction(event -> showMore());
        VBox.setVgrow(_table, Priority.ALWAYS);
        _results.getChildren().addAll(_count, _table, _more);
        VBox.setVgrow(_results, Priority.ALWAYS);

        VBox page = new VBox(16, heading, _extras, _offline, _banner, searchRow, _skeleton, _results, _footer);
        page.setPadding(new Insets(20, 24, 20, 24));
        showResults();
        refreshOnline();
        _feed.onChange(() -> _uiThread.accept(this::refreshOnline));
        return page;
    }

    private void search() {
        _presenter.clear();
        _banner.hide();
        // a "Show more" still running belongs to the old rows, so drop it
        _moreHandle.cancel();
        showResults();
        String query = _query.getText();
        _searchHandle = _tasks.run(() -> _service.search(query), _loading::start, result -> {
            _loading.finish();
            refreshOnline();
            CustomerSearchState.Display display = _model.applySearch(query, result);
            _presenter.present(display.fieldErrors());
            display.banner().ifPresent(_banner::show);
            showResults();
            display.newRows().ifPresent(rows -> _mode.onRowsReplaced(rows, this::redrawTable));
        }, failure -> {
            _loading.finish();
            refreshOnline();
            _banner.show(CustomerSearchText.unexpected(failure));
        });
    }

    private void cancelSearch() {
        _searchHandle.cancel();
        _loading.finish();
        refreshOnline();
    }

    private void showMore() {
        _banner.hide();
        _moreHandle = _tasks.run(() -> _service.search(_model.query(), _model.nextPage()), () -> {
            _more.setText(CustomerSearchText.LOADING_MORE);
            _more.setDisable(true);
        }, result -> {
            CustomerSearchState.More more = _model.applyMore(result);
            more.banner().ifPresent(_banner::show);
            showResults();
            _mode.onRowsAdded(more.added(), this::redrawTable);
        }, failure -> {
            _banner.show(CustomerSearchText.unexpected(failure));
            showResults();
        });
    }

    // copies the model onto the results area; also puts "Show more" back to its idle look
    private void showResults() {
        _results.setVisible(_model.hasSearched());
        _results.setManaged(_model.hasSearched());
        _count.setText(_model.countLine());
        _table.setEmptyText(_model.emptyText());
        _table.setRows(_model.customers());
        _more.setText(_model.showMoreLabel());
        _more.setDisable(false);
        _more.setVisible(_model.canShowMore());
        _more.setManaged(_model.canShowMore());
    }

    private void redrawTable() {
        _table.setRows(_model.customers());
    }

    // the offline banner and whether Search can be pressed, from the connection state
    private void refreshOnline() {
        boolean online = _feed.isOnline();
        _offline.hide();
        CustomerSearchText.offlineBanner(online).ifPresent(_offline::show);
        _search.setDisable(CustomerSearchText.searchDisabled(online, _searching.isLoading()));
    }
}
