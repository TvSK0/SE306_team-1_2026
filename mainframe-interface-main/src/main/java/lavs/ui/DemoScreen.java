// AI-assisted: a developer-only gallery of every shared component in every state, to compare with Figma.
package lavs.ui;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lavs.validation.FieldError;
import lavs.validation.ValidationResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Not part of the product: LavsApp shows this instead of the login stand-in only when the
// app is started with -Dlavs.demo=true or the environment variable LAVS_DEMO=true. Through
// "mvn javafx:run" the plugin does not pass -D options to the app, so use the variable:
//   PowerShell:  $env:LAVS_DEMO = "true"; mvn javafx:run
// It is all thin JavaFX; the decisions it shows live in the tested plain classes.
public final class DemoScreen {
    static final String PROPERTY = "lavs.demo";
    static final String ENVIRONMENT_VARIABLE = "LAVS_DEMO";

    private record Note(String text, String added, boolean uploaded) {
    }

    private record Loan(String number, String principal, String status) {
    }

    private DemoScreen() {
    }

    public static boolean isEnabled() {
        return isEnabled(System.getProperty(PROPERTY), System.getenv(ENVIRONMENT_VARIABLE));
    }

    // either switch set to "true" (any case) turns the demo on
    static boolean isEnabled(String propertyValue, String environmentValue) {
        return "true".equalsIgnoreCase(propertyValue) || "true".equalsIgnoreCase(environmentValue);
    }

    public static Node create() {
        VBox page = new VBox(32);
        page.setPadding(new Insets(32, 40, 32, 40));
        page.getChildren().addAll(
                intro(), buttons(), tags(), formFields(), banners(), notesTable(), loansTable(), emptyTable(),
                loading(), topBars());

        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        // transparent, like the table scroller, so the page background shows
        scroll.getStyleClass().add("lavs-table-scroll");
        return scroll;
    }

    private static Node intro() {
        Label note = new Label("Component demo (developer only). Compare each block with its Figma frame.");
        note.getStyleClass().add("lavs-subtitle");
        return section("Components", note);
    }

    private static Node buttons() {
        Button primary = styled("Primary", "lavs-button-primary");
        Button secondary = styled("Secondary", "lavs-button-secondary");
        Button busy = styled("Searching...", "lavs-button-primary");
        busy.setDisable(true);
        Button link = styled("← Back to search", "lavs-button-link");
        Button danger = styled("Delete", "lavs-button-danger-link");
        Button confirm = styled("Open delete confirmation", "lavs-button-secondary");
        confirm.setOnAction(event -> ConfirmDialog.ask(confirm.getScene().getWindow(),
                ConfirmContent.delete("document"), () -> { }));
        return section("Buttons", row(primary, secondary, busy, link, danger, confirm));
    }

    private static Node tags() {
        return section("Tags", row(
                new Tag(TagVariant.SUCCESS, "Active"), new Tag(TagVariant.NEUTRAL, "Inactive"),
                new Tag(TagVariant.WARNING, "Waiting to upload"), new Tag(TagVariant.DANGER, "Rejected"),
                new Tag(TagVariant.INFO, "New")));
    }

    // starts in the error state (frames 07 and 11); Clear shows the normal state
    private static Node formFields() {
        ComboBox<String> typeBox = new ComboBox<>();
        typeBox.getItems().addAll("Passport", "Driver licence", "Utility bill");
        typeBox.setPromptText("Choose...");
        FormField type = new FormField("Document type *", typeBox);
        FormField fileName = new FormField("File name *", new TextField());
        FormField size = new FormField("Size", new TextField("120"));

        ComboBox<String> rateBox = new ComboBox<>();
        rateBox.getItems().addAll("Fixed", "Floating");
        rateBox.setValue("Fixed");
        FormField rate = new FormField("Rate type (normal dropdown)", rateBox);
        FormField readOnly = new FormField("Customer number (read-only, no input look)", new Label("C-10234"), false);

        Banner validationBanner = new Banner();
        FieldErrorPresenter presenter = new FieldErrorPresenter(Map.of("type", type, "fileName", fileName, "size", size));

        Runnable validate = () -> {
            ValidationResult result = new ValidationResult();
            result.add(new FieldError("type", "Choose a document type"));
            result.add(new FieldError("fileName", "Enter a file name"));
            result.add(new FieldError("mailingAddress", "Add a mailing address"));
            validationBanner.show(BannerContent.validation(presenter.present(result)));
        };
        Button validateButton = styled("Show errors", "lavs-button-primary");
        validateButton.setOnAction(event -> validate.run());
        Button clearButton = styled("Clear errors", "lavs-button-secondary");
        clearButton.setOnAction(event -> {
            presenter.clear();
            validationBanner.hide();
        });
        validate.run();

        VBox card = new VBox(16, validationBanner, type, fileName, size, rate, readOnly, row(clearButton, validateButton));
        card.getStyleClass().add("lavs-card");
        card.setMaxWidth(480);
        return section("Form fields and validation banner (frames 07, 11)", card);
    }

    private static Node banners() {
        return section("Banners", new VBox(12,
                new Banner(BannerContent.offline()),
                new Banner(BannerContent.rejection("document", "File name already exists", true)),
                new Banner(BannerContent.rejection("document", "File name already exists", false)),
                new Banner(new BannerContent(BannerVariant.INFO, "Info banner", "Something worth knowing."))));
    }

    // frame 09 Notes: heading with an action above the table
    private static Node notesTable() {
        SectionHeading heading = SectionHeading.section("Notes", "Notes on this customer.");
        heading.setAction("+ Add note", () -> { });

        RecordTable<Note> table = new RecordTable<Note>()
                .column("Note", Note::text, 5)
                .column("Added", Note::added, 2)
                .nodeColumn("Status", note -> note.uploaded()
                        ? new Tag(TagVariant.SUCCESS, "Uploaded")
                        : new Tag(TagVariant.WARNING, "Waiting to upload"), 3)
                .rowAction("Edit", note -> { })
                .deleteAction("note", note -> { });
        table.setRows(List.of(
                new Note("Asked about a top-up next year", "14/03/2026", true),
                new Note("Passport copy seen in branch", "02/04/2026", false),
                new Note("Prefers contact by email", "19/04/2026", true)));
        return section("Heading with action above a table (frame 09)", heading, table);
    }

    // a 3-link row (Open, Edit, Delete) and enough rows to scroll under the fixed header
    private static Node loansTable() {
        SectionHeading heading = SectionHeading.page("Find a customer",
                "Search before adding a new customer to avoid creating a duplicate.");
        heading.setAction("+ Add New Customer", () -> { });

        RecordTable<Loan> table = new RecordTable<Loan>()
                .column("Loan number", Loan::number, 3)
                .column("Principal", Loan::principal, 3)
                .nodeColumn("Status", loan -> new Tag(tagFor(loan.status()), loan.status()), 2)
                .rowAction("Open loan", loan -> { })
                .rowAction("Edit", loan -> { })
                .deleteAction("loan", loan -> { });
        List<Loan> loans = new ArrayList<>();
        String[] statuses = {"Active", "Pending", "New", "Declined", "Closed"};
        for (int i = 0; i < 14; i++) {
            loans.add(new Loan("L-" + (1000 + i), "$" + (450 + i * 10) + ",000.00", statuses[i % statuses.length]));
        }
        table.setRows(loans);
        // a limited height, so the rows scroll while the header stays put
        table.setPrefHeight(300);
        table.setMaxHeight(300);
        return section("Three links per row, scrolling rows under a fixed header", heading, table);
    }

    private static Node emptyTable() {
        RecordTable<Note> table = new RecordTable<Note>().column("Note", Note::text, 1);
        table.setEmptyText("No documents yet.");
        table.setRows(List.of());
        return section("Empty state", table);
    }

    private static Node loading() {
        Button search = styled("Search", "lavs-button-primary");
        Button cancel = styled("Cancel", "lavs-button-secondary");
        SkeletonRows skeleton = new SkeletonRows(4);
        LoadingView read = new LoadingView(LoadingState.forRead("Search", "Searching..."), search, cancel, skeleton);
        search.setOnAction(event -> read.start());
        cancel.setOnAction(event -> read.finish());

        Button save = styled("Save document", "lavs-button-primary");
        LoadingView saving = new LoadingView(LoadingState.forSave("Save document", "Saving..."), save);
        save.setOnAction(event -> {
            saving.start();
            PauseTransition pause = new PauseTransition(Duration.seconds(1.5));
            pause.setOnFinished(done -> saving.finish());
            pause.play();
        });

        return section("Loading states (frame 12): click Search, then Cancel; Save has no Cancel",
                row(search, cancel), skeleton, row(save));
    }

    private static Node topBars() {
        return section("Top bar: online, waiting, offline",
                topBar(new FixedStatusFeed(true, 0)),
                topBar(new FixedStatusFeed(true, 3)),
                topBar(new FixedStatusFeed(false, 3)));
    }

    private static TopBar topBar(StatusFeed feed) {
        TopBar bar = new TopBar(feed, Platform::runLater);
        bar.setTitle("Customer search");
        bar.setUserName("Jane Smith");
        return bar;
    }

    private static TagVariant tagFor(String status) {
        return switch (status) {
            case "Active" -> TagVariant.SUCCESS;
            case "Pending" -> TagVariant.WARNING;
            case "New" -> TagVariant.INFO;
            case "Declined" -> TagVariant.DANGER;
            default -> TagVariant.NEUTRAL;
        };
    }

    private static VBox section(String title, Node... content) {
        Label heading = new Label(title);
        heading.getStyleClass().add("lavs-section-title");
        VBox box = new VBox(12, heading);
        box.getChildren().addAll(content);
        return box;
    }

    private static HBox row(Node... nodes) {
        HBox box = new HBox(12, nodes);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private static Button styled(String text, String styleClass) {
        Button button = new Button(text);
        button.getStyleClass().add(styleClass);
        return button;
    }
}
