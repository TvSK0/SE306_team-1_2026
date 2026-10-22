// AI-assisted: the customer record frame (Figma 03a): header, section sidebar and the selected section.
package lavs.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lavs.logic.CustomerCountsService;
import lavs.logic.CustomerRecordService;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

// Thin control: what the header and the sidebar show is decided in CustomerHeaderState and
// RecordSidebarState (unit tested), and the work is in CustomerRecordService and
// CustomerCountsService. Every call goes through BackgroundTasks so the window never freezes.
//
// The header shows as soon as the customer has loaded; the sidebar counts then arrive one by one
// in a second background task. A section's view is built the first time it is opened and kept, so
// Back from a loan returns to the section as it was. The sections themselves belong to their
// owners: they register a view with the SectionRegistry (see Section for who owns which).
// Implements Refreshable: coming Back to the record counts again and restores the top bar title.
public class CustomerRecordScreen extends BorderPane implements Refreshable {
    private final CustomerRecordService _service;
    private final CustomerCountsService _counts;
    private final SectionRegistry<Node> _sections;
    private final BackgroundTasks _tasks;
    private final StatusFeed _feed;
    private final Consumer<Runnable> _uiThread;
    private final Consumer<String> _setTopBarTitle;

    private final CustomerHeaderState _header;
    private final RecordSidebarState _sidebar;
    private final SectionContext _context;
    private final Label _name = new Label();
    private final Tag _status = new Tag(TagVariant.NEUTRAL, "");
    private final Label _details = new Label();
    private final Banner _offline = new Banner();
    private final Banner _banner = new Banner();
    private final StackPane _section = new StackPane();
    private final Map<Section, HBox> _items = new EnumMap<>(Section.class);
    private final Map<Section, Label> _countLabels = new EnumMap<>(Section.class);
    private final Map<Section, Node> _views = new EnumMap<>(Section.class);

    // customerNumber is null for "+ Add New Customer". onBack is navigator::back, and
    // setTopBarTitle is topBar::setTitle (the Navigator sets the default title, this sets
    // "Customer: <name> (<number>)" once the customer has loaded).
    public CustomerRecordScreen(String customerNumber, CustomerRecordService service, CustomerCountsService counts,
                                SectionRegistry<Node> sections, BackgroundTasks tasks, StatusFeed feed,
                                Consumer<Runnable> uiThread, Runnable onBack, Consumer<String> setTopBarTitle) {
        _service = service;
        _counts = counts;
        _sections = sections;
        _tasks = tasks;
        _feed = feed;
        _uiThread = uiThread;
        _setTopBarTitle = setTopBarTitle;
        _header = new CustomerHeaderState(customerNumber);
        _sidebar = new RecordSidebarState(_header.isSaved());
        _context = new SectionContext(customerNumber, _header::customer, this::goTo, this::refreshCounts);

        Hyperlink back = new Hyperlink("← Back to search");
        back.getStyleClass().add("lavs-button-link");
        back.setOnAction(event -> onBack.run());
        _name.getStyleClass().add("lavs-record-name");
        _details.getStyleClass().add("lavs-subtitle-small");
        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        HBox header = new HBox(16, back, _name, _status, grow, _details);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("lavs-record-header");

        VBox sidebar = new VBox(4);
        sidebar.getStyleClass().add("lavs-sidebar");
        Label sidebarHeading = new Label("CUSTOMER RECORD");
        sidebarHeading.getStyleClass().add("lavs-sidebar-heading");
        sidebar.getChildren().add(sidebarHeading);
        Arrays.stream(Section.values()).forEach(section -> sidebar.getChildren().add(navItem(section)));

        VBox content = new VBox(12, _offline, _banner, _section);
        VBox.setVgrow(_section, Priority.ALWAYS);
        content.setPadding(new Insets(20, 24, 20, 24));

        setTop(header);
        setLeft(sidebar);
        setCenter(content);

        refreshHeader();
        refreshSidebar();
        showSelected();
        refreshOnline();
        feed.onChange(() -> uiThread.accept(this::refreshOnline));
        _header.numberToLoad().ifPresent(this::load);
    }

    // what a section shows until its owner registers a view
    public static Node placeholder(Section section) {
        Label label = new Label(section.placeholderText());
        label.getStyleClass().add("lavs-subtitle");
        label.setWrapText(true);
        return label;
    }

    @Override
    public void onReturn() {
        _setTopBarTitle.accept(_header.topBarTitle());
        refreshCounts();
    }

    private Node navItem(Section section) {
        Label label = new Label(section.label());
        label.getStyleClass().add("lavs-nav-label");
        Label count = new Label();
        count.getStyleClass().add("lavs-nav-count");
        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        HBox item = new HBox(label, grow, count);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setOnMouseClicked(event -> goTo(section));
        _items.put(section, item);
        _countLabels.put(section, count);
        return item;
    }

    private void goTo(Section section) {
        _sidebar.select(section);
        refreshSidebar();
        showSelected();
    }

    private void showSelected() {
        _section.getChildren().setAll(
                _views.computeIfAbsent(_sidebar.selected(), section -> _sections.create(section, _context)));
    }

    private void load(String customerNumber) {
        _tasks.run(() -> _service.load(customerNumber), result -> {
            _header.applyLoad(result).ifPresent(_banner::show);
            refreshHeader();
            refreshCounts();
        }, failure -> _banner.show(CustomerRecordText.unexpected(failure)));
    }

    // counts again, one by one as they arrive; only once the customer has loaded
    private void refreshCounts() {
        _header.numberToCount().ifPresent(this::loadCounts);
    }

    private void loadCounts(String customerNumber) {
        _tasks.run(() -> {
            _counts.fetchCounts(customerNumber, (kind, count) -> _uiThread.accept(() -> {
                _sidebar.setCount(Section.forKind(kind), count);
                refreshSidebar();
            }));
            return null;
        }, done -> { }, failure -> failure.printStackTrace());
    }

    private void refreshHeader() {
        _name.setText(_header.name());
        _details.setText(_header.detailsLine());
        _header.statusTag().ifPresentOrElse(tag -> {
            _status.set(tag.variant(), tag.text());
            _status.setVisible(true);
            _status.setManaged(true);
        }, () -> {
            _status.setVisible(false);
            _status.setManaged(false);
        });
        _setTopBarTitle.accept(_header.topBarTitle());
    }

    private void refreshSidebar() {
        Arrays.stream(Section.values()).forEach(section -> {
            HBox item = _items.get(section);
            item.getStyleClass().setAll(_sidebar.styleClasses(section));
            item.setDisable(!_sidebar.isEnabled(section));
            _countLabels.get(section).setText(_sidebar.countText(section).orElse(""));
        });
    }

    private void refreshOnline() {
        _offline.hide();
        CustomerSearchText.offlineBanner(_feed.isOnline()).ifPresent(_offline::show);
    }
}
