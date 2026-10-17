package lavs.model;

import java.math.BigDecimal;
import java.time.LocalDate;

// A loan for a customer. The loan number is null until the mainframe creates the loan.
// The fields follow the pertaining mainframe messages such as loan.Create, loan.Retrieve etc

public class Loan {

    private String loanNumber;
    private String customerNumber;
    private String status;
    private BigDecimal principal;
    private String rateType;
    private BigDecimal rateAmount;
    private String rateFrequency;
    private BigDecimal paymentAmount;
    private String paymentFrequency;
    private LocalDate startDate;
    private LocalDate renewalDate;

    public Loan() {
    }

    public Loan(String customerNumber, BigDecimal principal, String rateType, BigDecimal rateAmount,
                String rateFrequency, BigDecimal paymentAmount, String paymentFrequency,
                LocalDate startDate, LocalDate renewalDate) {
        this.customerNumber = customerNumber;
        this.principal = principal;
        this.rateType = rateType;
        this.rateAmount = rateAmount;
        this.rateFrequency = rateFrequency;
        this.paymentAmount = paymentAmount;
        this.paymentFrequency = paymentFrequency;
        this.startDate = startDate;
        this.renewalDate = renewalDate;
    }

    // Returns true until the mainframe has created this loan and given it a number.
    public boolean isNew() {
        return loanNumber == null;
    }

    public String getLoanNumber() {
        return loanNumber;
    }

    public void setLoanNumber(String loanNumber) {
        this.loanNumber = loanNumber;
    }

    public String getCustomerNumber() {
        return customerNumber;
    }

    public void setCustomerNumber(String customerNumber) {
        this.customerNumber = customerNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getPrincipal() {
        return principal;
    }

    public void setPrincipal(BigDecimal principal) {
        this.principal = principal;
    }

    public String getRateType() {
        return rateType;
    }

    public void setRateType(String rateType) {
        this.rateType = rateType;
    }

    public BigDecimal getRateAmount() {
        return rateAmount;
    }

    public void setRateAmount(BigDecimal rateAmount) {
        this.rateAmount = rateAmount;
    }

    public String getRateFrequency() {
        return rateFrequency;
    }

    public void setRateFrequency(String rateFrequency) {
        this.rateFrequency = rateFrequency;
    }

    public BigDecimal getPaymentAmount() {
        return paymentAmount;
    }

    public void setPaymentAmount(BigDecimal paymentAmount) {
        this.paymentAmount = paymentAmount;
    }

    public String getPaymentFrequency() {
        return paymentFrequency;
    }

    public void setPaymentFrequency(String paymentFrequency) {
        this.paymentFrequency = paymentFrequency;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getRenewalDate() {
        return renewalDate;
    }

    public void setRenewalDate(LocalDate renewalDate) {
        this.renewalDate = renewalDate;
    }
}
