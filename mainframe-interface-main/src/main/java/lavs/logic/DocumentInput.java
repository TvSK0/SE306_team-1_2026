// AI-assisted: what the MMM typed into the add or edit document form.
package lavs.logic;

// type and fileName are required; fileType and size may be blank (a blank size is sent as 0).
// The field names are how a FieldError tells the form which field to highlight.
public record DocumentInput(String type, String fileName, String fileType, String size) {
    public static final String TYPE = "type";
    public static final String FILE_NAME = "fileName";
    public static final String FILE_TYPE = "fileType";
    public static final String SIZE = "size";
}
