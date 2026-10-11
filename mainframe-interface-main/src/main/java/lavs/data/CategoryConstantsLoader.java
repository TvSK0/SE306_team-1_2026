// TEMPORARY: swap this package for Scott's data package once his skeleton is on main.
package lavs.data;

import mainframe.client.Connection;

// AI-assisted: skeleton drafted with Claude Code, reviewed by Yu.
// Loads every category constant from the mainframe: constantCategory.list for the
// categories, then constantItem.list for each category's values, page by page.
public class CategoryConstantsLoader {
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
    public ConstantsLoadResult load() {
        // TODO step 6: implement. Track the page number here (start at 1) instead of
        // trusting the response's page value, and create a new message object per page.
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
