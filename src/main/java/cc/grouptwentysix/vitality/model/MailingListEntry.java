package cc.grouptwentysix.vitality.model;

public class MailingListEntry {
    private String email;
    private String dateAdded;

    public MailingListEntry() {}

    public MailingListEntry(String email) {
        this.email = email;
        this.dateAdded = new java.util.Date().toString();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(String dateAdded) {
        this.dateAdded = dateAdded;
    }
} 