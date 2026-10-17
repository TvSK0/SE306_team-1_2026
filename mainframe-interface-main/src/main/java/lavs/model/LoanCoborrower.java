package lavs.model;

// Links another customer to a loan as a co-borrower or guarantor (differntiated by .type)
// Fields follow th emainframe loan.AddCoborrower, loanCoborrower.Retrieve messages
// The id is null until the mainframe has stored this link.
public class LoanCoborrower {

    private Long id;
    private String loanNumber;
    private String coborrowerNumber;
    private String type;

    public LoanCoborrower() {
    }

    public LoanCoborrower(String loanNumber, String coborrowerNumber, String type) {
        this.loanNumber = loanNumber;
        this.coborrowerNumber = coborrowerNumber;
        this.type = type;
    }

    // Returns true until the mainframe has assigned this link an id.
    public boolean isNew() {
        return id == null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLoanNumber() {
        return loanNumber;
    }

    public void setLoanNumber(String loanNumber) {
        this.loanNumber = loanNumber;
    }

    public String getCoborrowerNumber() {
        return coborrowerNumber;
    }

    public void setCoborrowerNumber(String coborrowerNumber) {
        this.coborrowerNumber = coborrowerNumber;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
