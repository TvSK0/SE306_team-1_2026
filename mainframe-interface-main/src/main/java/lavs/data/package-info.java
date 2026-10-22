// AI-assisted: the Database layer of the System Model, and the way out to the
// mainframe.
//
// The local SQLite file (LocalDatabase), the upload queue and category constants
// kept in it (SqlitePendingChangeStore, CategoryConstantStore), and the classes
// that talk to the mainframe (CategoryConstantsLoader, the upload gateway).
//
// May use: model, JDBC and mainframe.client. Must not use: JavaFX, ui or logic.
// Only logic calls this package (and LavsApp, which builds it).
package lavs.data;
