// AI-assisted: one line of a customer's Documents table: the document's details.
package lavs.logic;

// Only the details are held: the file itself stays with the customer and is not uploaded.
// fileType may be null if the mainframe sent none.
public record DocumentRow(long id, String type, String fileName, String fileType, long size) {
}
