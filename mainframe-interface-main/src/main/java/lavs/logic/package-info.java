// AI-assisted: the Business Logic layer of the System Model.
//
// The services each user story calls (e.g. LoanService, LoanStatusService), their
// result types, the shared rules (EligibilityChecker) and the upload queue's rules
// (SyncService, SyncProcessor). A service validates first and returns field errors
// or a result object instead of throwing, so the screen handles every outcome.
//
// May use: data, model, validation and mainframe.client. Must not use: JavaFX, JDBC
// or ui. Branch coverage of this package is held at 95% or more.
package lavs.logic;
