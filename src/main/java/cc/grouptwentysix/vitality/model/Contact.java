package cc.grouptwentysix.vitality.model;

public class Contact {
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String orderNumber;
    private String questionType;
    private String message;

    public Contact() {}

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public String getQuestionType() { return questionType; }
    public void setQuestionType(String questionType) { this.questionType = questionType; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}