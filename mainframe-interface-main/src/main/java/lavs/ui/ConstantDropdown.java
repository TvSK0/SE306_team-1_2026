package lavs.ui;

import javafx.scene.control.ComboBox;
import lavs.logic.ConstantsService;

// A dropdown filled from one category of mainframe constants. Use it in FXML as
//   <ConstantDropdown fx:id="rateType" category="LoanRateType"/>
// then call show(...) from the screen's controller. The rules for what it lists live
// in DropdownOptions, which is unit tested; this class only copies them onto the ComboBox.
public class ConstantDropdown extends ComboBox<String> {
    private String _category;

    // The ConstantCategories name this dropdown lists, e.g. "LoanRateType".
    public String getCategory() {
        return _category;
    }

    public void setCategory(String category) {
        _category = category;
    }

    // Fills the dropdown from the cached constants and selects currentValue, the value
    // saved on the record being edited (null or blank for a new record).
    public void show(ConstantsService service, String currentValue) {
        DropdownOptions options = DropdownOptions.forCategory(service, _category, currentValue);
        getItems().setAll(options.getItems());
        setValue(options.getSelected());
        setPromptText(options.getPromptText());
        setDisable(!options.isEnabled());
    }
}
