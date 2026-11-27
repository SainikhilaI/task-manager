package parser;

import java.time.LocalDate;

public class RuleBasedTaskParser implements TaskParser {
    @Override
    public ParsedTask parse(String text, String today) {
        String safeText = text == null ? "" : text.trim();
        LocalDate baseDate;
        try {
            baseDate = LocalDate.parse(today);
        } catch (Exception e) {
            baseDate = LocalDate.now();
        }

        String lower = safeText.toLowerCase();
        String dueDate = null;
        String recurrence = null;

        if (lower.contains("tomorrow")) {
            dueDate = baseDate.plusDays(1).toString();
        }
        if (lower.contains("every month") || lower.contains("monthly")) {
            recurrence = "MONTHLY";
        }
        if (lower.contains("1st")) {
            int year = baseDate.getYear();
            int month = baseDate.getMonthValue();
            if (baseDate.getDayOfMonth() > 1) {
                LocalDate nextMonth = baseDate.plusMonths(1).withDayOfMonth(1);
                dueDate = nextMonth.toString();
            } else {
                dueDate = LocalDate.of(year, month, 1).toString();
            }
            if (recurrence == null && (lower.contains("every") || lower.contains("month"))) {
                recurrence = "MONTHLY";
            }
        }

        String title = safeText.isEmpty() ? "" : safeText.substring(0, Math.min(safeText.length(), 60));
        return new ParsedTask(title, safeText, dueDate, recurrence);
    }
}