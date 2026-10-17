package lavs.model;

import java.math.BigDecimal;

// One of a customer's employers. Follows the mainframe messages like customer.AddEmployer, customerEmployer.Retrieve etc.
public class Employer {

    private Long id;
    private String customerNumber;
    private String type;
    private String name;
    private String position;
    private BigDecimal salary;

    public Employer() {
    }

    public Employer(String customerNumber, String type, String name, String position, BigDecimal salary) {
        this.customerNumber = customerNumber;
        this.type = type;
        this.name = name;
        this.position = position;
        this.salary = salary;
    }

    // Returns true until the mainframe's assigned this employer an id.
    public boolean isNew() {
        return id == null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCustomerNumber() {
        return customerNumber;
    }

    public void setCustomerNumber(String customerNumber) {
        this.customerNumber = customerNumber;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public BigDecimal getSalary() {
        return salary;
    }

    public void setSalary(BigDecimal salary) {
        this.salary = salary;
    }
}
