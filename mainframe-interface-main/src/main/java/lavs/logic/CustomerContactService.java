// AI-assisted: checks whether a customer has a phone number or an email, for select mode.
// TEMPORARY: swap this package for Scott's logic package once his skeleton is on main.
package lavs.logic;

import mainframe.client.Connection;
import mainframe.client.Status;

import java.io.IOException;
import java.util.List;
import java.util.function.BiConsumer;

// US2 select mode: the "Contact details" column says "Complete" when the customer has a phone
// number or an email, and "Missing phone or email" when they have neither. The list messages
// report how many there are in their total, so one item per call is enough.
// TEMPORARY: uses Connection directly until Scott's gateway lands; swap then.
public class CustomerContactService {
    public enum Check { COMPLETE, MISSING, FAILED }

    private final Connection _connection;

    public CustomerContactService(Connection connection) {
        _connection = connection;
    }

    // The phone numbers are asked first and the email only when there is no phone, so a
    // customer with a phone costs one call. Any failure (offline, a refusal, or a reply the
    // starter message classes cannot read) is FAILED rather than a guess either way.
    public Check check(String customerNumber) {
        try {
            mainframe.client.messages.customerPhoneNumber.List phones =
                    new mainframe.client.messages.customerPhoneNumber.List(customerNumber, 1, 1);
            Status phoneStatus = phones.send(_connection);
            if (!phoneStatus.getWasSuccessful()) {
                return Check.FAILED;
            }
            if (phones.getTotal() > 0) {
                return Check.COMPLETE;
            }
            mainframe.client.messages.customerEmail.List emails =
                    new mainframe.client.messages.customerEmail.List(customerNumber, 1, 1);
            Status emailStatus = emails.send(_connection);
            if (!emailStatus.getWasSuccessful()) {
                return Check.FAILED;
            }
            return emails.getTotal() > 0 ? Check.COMPLETE : Check.MISSING;
        } catch (IOException | RuntimeException ex) {
            // RuntimeException: the starter list messages throw a NullPointerException when an
            // item has no id, and they are not ours to change
            return Check.FAILED;
        }
    }

    // Checks the customers one after another, in order, telling onEach as each answer arrives.
    // Meant for a background thread; onEach is called on that thread.
    public void checkAll(List<String> customerNumbers, BiConsumer<String, Check> onEach) {
        for (String number : customerNumbers) {
            onEach.accept(number, check(number));
        }
    }
}
