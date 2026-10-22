package lavs.logic;

public enum RecordOwner {
    CUSTOMER("customer", "customer", "Customer"), LOAN("loan", "loanNumber", "Loan");
    final String operation;
    final String numberField;
    private final String categoryPrefix;
    RecordOwner(String operation, String numberField, String categoryPrefix) {
        this.operation = operation;
        this.numberField = numberField;
        this.categoryPrefix = categoryPrefix;
    }
    public String category(String kind) { return categoryPrefix + kind + "Type"; }
}
