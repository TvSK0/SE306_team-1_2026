package lavs.ui;

import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TabPane;
import lavs.data.MainframeGateway;
import lavs.data.ConstantsLoadResult;
import lavs.logic.*;
import lavs.model.Customer;
import lavs.model.Loan;
import mainframe.client.FakeConnection;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

// AI-assisted: exercise real FXML and layout on the Windows target platform.
@EnabledOnOs(OS.WINDOWS)
class FxScreenSmokeTests {
    @BeforeAll static void startToolkit() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(started::countDown);
        assertTrue(started.await(10, TimeUnit.SECONDS));
        Platform.setImplicitExit(false);
    }
    @Test void customerLoanAndOptionalTabsLoadAndLayOut() throws Exception {
        CompletableFuture<Void> done = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                FakeConnection connection = new FakeConnection(); connection.setIsConnected(false);
                MainframeGateway gateway = new MainframeGateway(connection);
                BackgroundTasks tasks = new BackgroundTasks(Runnable::run, Runnable::run);
                ConstantsService constants = new ConstantsService(() -> ConstantsLoadResult.success(Map.of(
                        "CustomerNoteType", List.of("General"), "LoanNoteType", List.of("General"),
                        "CustomerDocumentType", List.of("Identity"), "LoanDocumentType", List.of("Income"))));
                constants.refresh();
                Navigator navigator = new Navigator(Screen.CUSTOMER_SEARCH);
                var notes = new RecordNotesService(gateway); var documents = new RecordDocumentsService(gateway);
                var loans = new LoanService(gateway);
                var customerServices = new CustomerScreenServices(new CustomerService(gateway, 1), new AddressService(gateway),
                        new PhoneNumberService(gateway), new EmailService(gateway), new CustomerRelationshipService(gateway),
                        constants, tasks, new EmployerService(gateway), new CustomerLoansService(gateway),
                        new CustomerEligibilityService(gateway), loans, navigator, notes, documents);
                snapshot(CustomerRecordController.open(customerServices, null, () -> {}), "customer");
                Customer customer = new Customer(); customer.setNumber("C1"); customer.setFirstName("Ana");
                Loan loan = new Loan(); loan.setLoanNumber("L1"); loan.setCustomerNumber("C1"); loan.setStatus("New");
                var loanServices = new LoanScreenServices(loans, new LoanRepaymentsService(gateway), constants,
                        new LoanStatusService(gateway), new LoanBorrowerService(gateway), tasks, navigator, notes, documents);
                Parent screen = LoanScreenController.open(loanServices, customer, loan, () -> {});
                snapshot(screen, "loan");
                TabPane tabs = (TabPane) screen.lookup("#_tabs"); assertNotNull(tabs);
                for (var tab : tabs.getTabs()) {
                    tabs.getSelectionModel().select(tab);
                    snapshot(screen, "loan-" + tab.getText().toLowerCase().replace(' ', '-'));
                }
                snapshot(new CustomerSearchScreen(new CustomerSearchService(gateway), tasks, navigator, null), "search");
                snapshot(new NotesPanel(notes, RecordOwner.CUSTOMER, "C1", constants, tasks), "customer-notes");
                snapshot(new DocumentsPanel(documents, RecordOwner.CUSTOMER, "C1", constants, tasks), "customer-documents");
                snapshot(new EmployersPanel(new EmployerService(gateway), tasks, "C1", constants), "employers");
                assertFalse(tasks.isBusy());
                done.complete(null);
            } catch (Throwable error) { done.completeExceptionally(error); }
        });
        done.get(30, TimeUnit.SECONDS);
    }
    private static void snapshot(Parent root, String name) throws Exception {
        Scene scene = root.getScene();
        if (scene == null) {
            scene = new Scene(root, 1100, 760);
            scene.getStylesheets().add(LavsApp.class.getResource("lavs.css").toExternalForm());
        }
        root.applyCss(); root.layout();
        var image = root.snapshot(null, null);
        assertTrue(image.getWidth() > 500);
        var output = new java.awt.image.BufferedImage((int) image.getWidth(), (int) image.getHeight(), java.awt.image.BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < output.getHeight(); y++) for (int x = 0; x < output.getWidth(); x++)
            output.setRGB(x, y, image.getPixelReader().getArgb(x, y));
        Path folder = Path.of("target", "ui-smoke"); java.nio.file.Files.createDirectories(folder);
        javax.imageio.ImageIO.write(output, "png", folder.resolve(name + ".png").toFile());
    }
}
