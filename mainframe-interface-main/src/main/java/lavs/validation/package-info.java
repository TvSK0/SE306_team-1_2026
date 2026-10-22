// AI-assisted: the shared validation framework. Each validator checks one field and
// returns an Optional FieldError instead of throwing; a ValidationResult collects
// them so a screen can show every problem with a form at once, by field name.
//
// Uses nothing outside the JDK: no JavaFX, JDBC, mainframe.client or other lavs
// package.
package lavs.validation;
