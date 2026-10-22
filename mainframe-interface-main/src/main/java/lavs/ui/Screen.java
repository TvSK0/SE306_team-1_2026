// AI-assisted: the screens the Navigator can show.
package lavs.ui;

// Teammates register the view for their screen with Navigator.register(Screen, factory).
// The parameter passed to the factory depends on the screen:
//   CUSTOMER_RECORD  - the customer number, or null for a new customer
//   LOAN             - whatever Yu's loan screen needs to open a loan
//   CUSTOMER_SELECT  - a CustomerSelectRequest (title, subtitle, extra controls, callbacks)
public enum Screen {
    LOGIN,
    CUSTOMER_SEARCH,
    CUSTOMER_RECORD,
    LOAN,
    CUSTOMER_SELECT
}
