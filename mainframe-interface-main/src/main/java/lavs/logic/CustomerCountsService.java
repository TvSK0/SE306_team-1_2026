// AI-assisted: how many addresses, phones, emails and so on a customer has, for the record sidebar.
// TEMPORARY: swap this package for Scott's logic package once his skeleton is on main.
package lavs.logic;

import mainframe.client.Connection;
import mainframe.client.Message;
import mainframe.client.Status;

import java.io.IOException;
import java.util.List;
import java.util.OptionalLong;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

// The customer record's sidebar shows a count beside each section. Every list message reports
// how many there are in its total, so one item per call is enough: eight calls in all. A count
// that could not be read is empty, never a guess.
// TEMPORARY: uses Connection directly until Scott's gateway lands; swap then.
public class CustomerCountsService {
    public enum Kind { ADDRESSES, PHONE_NUMBERS, EMAILS, EMPLOYERS, RELATIONSHIPS, NOTES, DOCUMENTS, LOANS }

    private record Counter(Kind kind, Message message, Supplier<Long> total) {
    }

    private final Connection _connection;

    public CustomerCountsService(Connection connection) {
        _connection = connection;
    }

    // Asks for each count one after another, telling onEach as each answer arrives (the
    // sidebar shows a count the moment it is known). Meant for a background thread; onEach is
    // called on that thread.
    public void fetchCounts(String customerNumber, BiConsumer<Kind, OptionalLong> onEach) {
        counters(customerNumber).forEach(counter -> onEach.accept(counter.kind(), count(counter)));
    }

    private List<Counter> counters(String number) {
        var addresses = new mainframe.client.messages.customerAddress.List(number, 1, 1);
        var phones = new mainframe.client.messages.customerPhoneNumber.List(number, 1, 1);
        var emails = new mainframe.client.messages.customerEmail.List(number, 1, 1);
        var employers = new mainframe.client.messages.customerEmployer.List(number, 1, 1);
        var relationships = new mainframe.client.messages.customerRelationship.List(number, 1, 1);
        var notes = new mainframe.client.messages.customerNote.List(number, 1, 1);
        var documents = new mainframe.client.messages.customerDocument.List(number, 1, 1);
        var loans = new mainframe.client.messages.loan.List(number, 1, 1);
        return List.of(
                new Counter(Kind.ADDRESSES, addresses, addresses::getTotal),
                new Counter(Kind.PHONE_NUMBERS, phones, phones::getTotal),
                new Counter(Kind.EMAILS, emails, emails::getTotal),
                new Counter(Kind.EMPLOYERS, employers, employers::getTotal),
                new Counter(Kind.RELATIONSHIPS, relationships, relationships::getTotal),
                new Counter(Kind.NOTES, notes, notes::getTotal),
                new Counter(Kind.DOCUMENTS, documents, documents::getTotal),
                new Counter(Kind.LOANS, loans, loans::getTotal));
    }

    private OptionalLong count(Counter counter) {
        try {
            Status status = counter.message().send(_connection);
            return status.getWasSuccessful() ? OptionalLong.of(counter.total().get()) : OptionalLong.empty();
        } catch (IOException | RuntimeException ex) {
            // RuntimeException: the starter list messages throw a NullPointerException when an
            // item has no id, and they are not ours to change
            return OptionalLong.empty();
        }
    }
}
