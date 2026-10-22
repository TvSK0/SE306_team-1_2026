// AI-assisted: a navigation parameter that also names the top bar title for its screen.
package lavs.ui;

// The Navigator hands the parameter of the screen being shown to the top bar, which asks it
// for a title when it has one (TopBarState.titleFor). Select mode uses it so the caller can
// say "Loan L-5521 · Adding a co-borrower or guarantor".
public interface TitledParameter {
    // null or blank means use the screen's default title
    String topBarTitle();
}
