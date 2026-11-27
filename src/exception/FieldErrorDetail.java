package exception;

public class FieldErrorDetail {
    private final String field;
    private final String issue;

    public FieldErrorDetail(String field, String issue) {
        this.field = field;
        this.issue = issue;
    }

    public String getField() { return field; }
    public String getIssue() { return issue; }
}
