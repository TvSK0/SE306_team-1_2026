// TEMPORARY: swap this package for Scott's data package once his skeleton is on main.
package lavs.data;

import mainframe.client.Connection;
import mainframe.client.Message;
import mainframe.client.Status;
import mainframe.client.StatusMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Loads every category constant from the mainframe: constantCategory.list for the
// categories, then constantItem.list for each category's values, page by page.
public class CategoryConstantsLoader implements ConstantsSource {
    public static final long DEFAULT_PAGE_SIZE = 100;

    // TEMPORARY: swap this Connection for Scott's gateway once it exists, so only
    // the gateway sends mainframe messages. The paging and sorting logic stays the same.
    private final Connection _connection;
    private final long _pageSize;

    public CategoryConstantsLoader(Connection connection) {
        this(connection, DEFAULT_PAGE_SIZE);
    }

    public CategoryConstantsLoader(Connection connection, long pageSize) {
        _connection = connection;
        _pageSize = pageSize;
    }

    // Returns every category name mapped to its values, sorted by order.
    // All or nothing: if any request fails, no values are returned.
    @Override
    public ConstantsLoadResult load() {
        try {
            Map<String, List<String>> values = new HashMap<>();
            for (mainframe.client.messages.constantCategory.List.Item category : loadCategories()) {
                values.put(category.getName(), loadValues(category.getId()));
            }
            return ConstantsLoadResult.success(values);
        } catch (LoadFailure failure) {
            return failure.result;
        }
    }

    private List<mainframe.client.messages.constantCategory.List.Item> loadCategories() throws LoadFailure {
        return loadAllPages(page -> {
            // a new message per page, because message objects keep the items from earlier sends
            var message = new mainframe.client.messages.constantCategory.List(page, _pageSize);
            send(message);
            return new Page<>(List.of(message.getItems()), message.getTotal());
        });
    }

    private List<String> loadValues(long categoryId) throws LoadFailure {
        List<mainframe.client.messages.constantItem.List.Item> items = loadAllPages(page -> {
            var message = new mainframe.client.messages.constantItem.List(categoryId, page, _pageSize);
            send(message);
            return new Page<>(List.of(message.getItems()), message.getTotal());
        });
        return items.stream()
                .sorted(Comparator.comparing(mainframe.client.messages.constantItem.List.Item::getOrder))
                .map(mainframe.client.messages.constantItem.List.Item::getName)
                .toList();
    }

    // Requests pages 1, 2, ... until every item is collected. Also stops on an empty
    // page, so a wrong total from the server cannot cause an endless loop.
    private static <T> List<T> loadAllPages(PageRequest<T> request) throws LoadFailure {
        List<T> all = new ArrayList<>();
        long page = 1;
        while (true) {
            Page<T> result = request.send(page);
            all.addAll(result.items());
            if (result.items().isEmpty() || all.size() >= result.total()) return all;
            page++;
        }
    }

    // TEMPORARY: swap this for Scott's gateway, which will turn a Status into
    // success, network failure or rejected for every message.
    private void send(Message message) throws LoadFailure {
        Status status;
        try {
            status = message.send(_connection);
        } catch (IOException ex) {
            throw new LoadFailure(ConstantsLoadResult.networkFailure(ex.getMessage()));
        }
        if (status.getWasSuccessful()) return;
        if (StatusMessage.NETWORK_FAILURE_UNAVAILABLE.getCode().equals(status.getErrorCode())) {
            throw new LoadFailure(ConstantsLoadResult.networkFailure(status.getErrorMessage()));
        }
        throw new LoadFailure(ConstantsLoadResult.rejected(status.getErrorMessage()));
    }

    private record Page<T>(List<T> items, long total) {}

    private interface PageRequest<T> {
        Page<T> send(long page) throws LoadFailure;
    }

    // Used only inside this class to stop loading at the first failure;
    // load() turns it back into a result, so callers never see it.
    private static final class LoadFailure extends Exception {
        private final ConstantsLoadResult result;

        LoadFailure(ConstantsLoadResult result) {
            this.result = result;
        }
    }
}
