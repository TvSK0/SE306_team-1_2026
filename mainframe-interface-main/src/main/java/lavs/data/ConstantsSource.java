// TEMPORARY: swap this package for Scott's data package once his skeleton is on main.
package lavs.data;

// Where the category constants come from. CategoryConstantsLoader reads them from the
// mainframe; tests can pass a lambda instead.
// TEMPORARY: once Aum's local store exists, a second implementation can read the
// CategoryConstant table so dropdowns still work offline.
public interface ConstantsSource {
    ConstantsLoadResult load();
}
