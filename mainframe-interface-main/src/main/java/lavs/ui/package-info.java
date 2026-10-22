// AI-assisted: the User Interface layer of the System Model.
//
// The JavaFX screens (FXML files and their controllers), the app shell (MainWindow,
// Navigator, TopBar) and the small classes with no JavaFX that hold each screen's
// wording and rules so they can be unit tested (e.g. LoanSubmission, BorrowersText).
//
// May use: logic, model, validation, and Status/StatusMessage from mainframe.client
// to word a result. Must not use: Connection, the mainframe message classes, JDBC
// or data. Every service call goes through BackgroundTasks.
//
// LavsApp is the one exception: it starts the app and builds the services, so it is
// where data and the Connection are created and handed to logic.
package lavs.ui;
