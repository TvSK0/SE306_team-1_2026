package lavs.data;

import mainframe.client.Connection;
import mainframe.client.Message;
import mainframe.client.Status;
import mainframe.client.StatusMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Loads every category constant from the mainframe: constantCategory.list for the
// categories, then constantItem.list for each category's values, page by page.
public class CategoryConstantsLoader implements ConstantsSource {
    public static final long DEFAULT_PAGE_SIZE = 100;

    private final MainframeGateway _gateway;
    private final long _pageSize;

    public CategoryConstantsLoader(MainframeGateway gateway) {
        _gateway = gateway;
        _pageSize = DEFAULT_PAGE_SIZE;
    }

    public CategoryConstantsLoader(Connection connection) {
        this(connection, DEFAULT_PAGE_SIZE);
    }

    public CategoryConstantsLoader(Connection connection, long pageSize) {
        _gateway = new MainframeGateway(connection);
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

    // Requests pages 1, 2, ... until every category is collected. Also stops on an
    // empty page, so a wrong total from the server cannot cause an endless loop.
    private List<mainframe.client.messages.constantCategory.List.Item> loadCategories() throws LoadFailure {
        List<mainframe.client.messages.constantCategory.List.Item> all = new ArrayList<>();
        long page = 1;
        while (true) {
            // a new message per page, because message objects keep the items from earlier sends
            mainframe.client.messages.constantCategory.List message =
                    new mainframe.client.messages.constantCategory.List(page, _pageSize);
            send(message);
            mainframe.client.messages.constantCategory.List.Item[] items = message.getItems();
            for (mainframe.client.messages.constantCategory.List.Item item : items) {
                all.add(item);
            }
            if (items.length == 0 || all.size() >= message.getTotal()) return all;
            page++;
        }
    }

    // Same paging as loadCategories, for one category's values. Returns the value
    // names sorted by their order number.
    private List<String> loadValues(long categoryId) throws LoadFailure {
        List<mainframe.client.messages.constantItem.List.Item> all = new ArrayList<>();
        long page = 1;
        while (true) {
            mainframe.client.messages.constantItem.List message =
                    new mainframe.client.messages.constantItem.List(categoryId, page, _pageSize);
            send(message);
            mainframe.client.messages.constantItem.List.Item[] items = message.getItems();
            for (mainframe.client.messages.constantItem.List.Item item : items) {
                all.add(item);
            }
            if (items.length == 0 || all.size() >= message.getTotal()) break;
            page++;
        }

        // smallest order number first
        all.sort((first, second) -> Long.compare(first.getOrder(), second.getOrder()));
        List<String> names = new ArrayList<>();
        for (mainframe.client.messages.constantItem.List.Item item : all) {
            names.add(item.getName());
        }
        return names;
    }

    private void send(Message message) throws LoadFailure {
        Status status = _gateway.send(message).getStatus();
        if (status.getWasSuccessful()) return;
        if (StatusMessage.NETWORK_FAILURE_UNAVAILABLE.getCode().equals(status.getErrorCode())) {
            throw new LoadFailure(ConstantsLoadResult.networkFailure(status.getErrorMessage()));
        }
        throw new LoadFailure(ConstantsLoadResult.rejected(status.getErrorMessage()));
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
