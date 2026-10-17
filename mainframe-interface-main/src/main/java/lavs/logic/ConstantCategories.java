// TEMPORARY: swap this package for Scott's logic package once his skeleton is on main.
package lavs.logic;

// The category names the mainframe uses for its constants. Screens pass these to
// ConstantsService.getValues instead of typing the strings themselves.
public final class ConstantCategories {
    public static final String CUSTOMER_ADDRESS_TYPE = "CustomerAddressType";
    public static final String CUSTOMER_EMAIL_TYPE = "CustomerEmailType";
    public static final String CUSTOMER_EMPLOYER_TYPE = "CustomerEmployerType";
    public static final String CUSTOMER_PHONE_NUMBER_TYPE = "CustomerPhoneNumberType";
    public static final String CUSTOMER_RELATIONSHIP_TYPE = "CustomerRelationshipType";
    public static final String CUSTOMER_STATUS = "CustomerStatus";
    public static final String LOAN_COBORROWER_TYPE = "LoanCoborrowerType";
    public static final String LOAN_PAYMENT_FREQUENCY = "LoanPaymentFrequency";
    public static final String LOAN_RATE_FREQUENCY = "LoanRateFrequency";
    public static final String LOAN_RATE_TYPE = "LoanRateType";
    public static final String LOAN_STATUS = "LoanStatus";

    private ConstantCategories() {}
}
