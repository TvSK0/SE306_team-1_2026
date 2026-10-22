// AI-assisted: the constant categories the document form's dropdowns read.
package lavs.ui;

// TEMPORARY: the mainframe's category names for document types and file types are not
// confirmed; these follow the pattern of the others in ConstantCategories (Yu's file, so not
// edited here). Both dropdowns can be typed into, so a wrong name only means the list is empty
// and the MMM types the value. Move these into ConstantCategories once the names are confirmed.
final class DocumentCategories {
    static final String DOCUMENT_TYPE = "CustomerDocumentType";
    static final String FILE_TYPE = "CustomerDocumentFileType";

    private DocumentCategories() {
    }
}
