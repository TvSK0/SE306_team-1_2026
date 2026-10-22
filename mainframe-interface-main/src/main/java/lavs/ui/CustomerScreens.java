package lavs.ui;

import java.io.IOException;
import java.io.UncheckedIOException;

// AI-assisted: registers the customer record with the Navigator. Open a saved customer with
// navigator.goTo(Screen.CUSTOMER_RECORD, customerNumber), or a new one with null.
// Back goes to whichever screen opened the record.
public class CustomerScreens {
    private final CustomerScreenServices _services;
    // topBar::setTitle, so the record can name the customer in the top bar
    private final java.util.function.Consumer<String> _setTopBarTitle;

    public CustomerScreens(CustomerScreenServices services) {
        this(services, title -> { });
    }

    public CustomerScreens(CustomerScreenServices services, java.util.function.Consumer<String> setTopBarTitle) {
        _services = services;
        _setTopBarTitle = setTopBarTitle;
    }

    public void register(Navigator navigator) {
        navigator.register(Screen.CUSTOMER_RECORD, parameter -> {
            try {
                return CustomerRecordController.open(_services, customerNumber(parameter), navigator::back,
                        _setTopBarTitle);
            } catch (IOException e) {
                // a missing or broken FXML file: the Navigator reports it and stays where it is
                throw new UncheckedIOException(e);
            }
        });
    }

    // The navigation parameter as a customer number; null means a new customer. Anything
    // else is a programming mistake by the screen that opened the record.
    static String customerNumber(Object parameter) {
        if (parameter == null || parameter instanceof String) {
            return (String) parameter;
        }
        throw new IllegalArgumentException("Screen.CUSTOMER_RECORD needs a customer number or null, got: " + parameter);
    }
}
