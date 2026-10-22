// AI-assisted: the User Interface layer of the System Model.
//
// The JavaFX screens (FXML files and their controllers), the app shell (MainWindow,
// Navigator, TopBar) and the small classes with no JavaFX that hold each screen's
// wording and rules so they can be unit tested (e.g. LoanSubmission, BorrowersText).
//
// May use: logic, model and validation. A screen words a mainframe answer from the
// result's MainframeReply (logic). Must not use: anything in mainframe.client (Status
// included), JDBC or data. Every service call goes through BackgroundTasks.
//
// LavsApp is the one exception: it starts the app and builds the services, so it is
// where data and the Connection are created and handed to logic.
package lavs.ui;
