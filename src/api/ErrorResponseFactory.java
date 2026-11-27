package api;

import exception.ApiException;
import exception.FieldErrorDetail;
import java.util.List;

public class ErrorResponseFactory {
    private ErrorResponseFactory() {
    }

    public static ApiResponse from(ApiException e) {
        return build(e.getStatusCode(), e.getErrorCode(), e.getMessage(), e.getDetails());
    }

    public static ApiResponse internalError(Exception e) {
        String msg = (e.getMessage() == null || e.getMessage().isBlank())
                ? "Internal server error"
                : e.getMessage();
        return build(500, "INTERNAL_ERROR", msg, List.of());
    }

    private static ApiResponse build(int status, String code, String message, List<FieldErrorDetail> details) {
        StringBuilder detailsJson = new StringBuilder("[");
        for (int i = 0; i < details.size(); i++) {
            FieldErrorDetail d = details.get(i);
            if (i > 0) detailsJson.append(",");
            detailsJson.append("{\"field\":\"").append(escape(d.getField()))
                       .append("\",\"issue\":\"").append(escape(d.getIssue())).append("\"}");
        }
        detailsJson.append("]");

        String body = "{\"error\":{"
                + "\"code\":\"" + escape(code) + "\","
                + "\"message\":\"" + escape(message) + "\","
                + "\"details\":" + detailsJson
                + "}}";
        return new ApiResponse(status, body);
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}