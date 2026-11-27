package app;

import api.ApiResponse;
import api.TaskApi;
import event.AuditTrailListener;
import event.ConsoleLogListener;
import model.Priority;
import model.TaskQuery;
import model.TaskRequest;
import parser.ParserFactory;
import parser.RuleBasedBreakdownStrategy;
import repository.IdempotencyStore;
import repository.InMemoryTaskRepository;
import service.TaskService;

import java.time.LocalDate;

public class Main {
    public static void main(String[] args) {
        System.out.println("Task Manager started");

        InMemoryTaskRepository repo = InMemoryTaskRepository.getInstance();
        IdempotencyStore idempotency = IdempotencyStore.getInstance();

        TaskService service = new TaskService(
                repo,
                ParserFactory.createParser(ParserFactory.Mode.RULE_BASED, null),
                new RuleBasedBreakdownStrategy()
        );

        ConsoleLogListener consoleListener = new ConsoleLogListener();
        AuditTrailListener auditListener = new AuditTrailListener();
        service.addListener(consoleListener);
        service.addListener(auditListener);

        TaskApi api = new TaskApi(service, idempotency);

        ApiResponse created = api.createTask(
                new TaskRequest("Finish assignment", "Complete Java 17 task manager", "false", "2026-11-27", "HIGH"),
                "main-create-1"
        );
        System.out.println("Created: " + created.toJson());

        ApiResponse fromText = api.createTaskFromText("Pay rent on the 1st of every month", "main-text-1");
        System.out.println("From text: " + fromText.toJson());

        ApiResponse subtasks = api.breakDownTask(1, "main-breakdown-1");
        System.out.println("Subtasks: " + subtasks.toJson());

        ApiResponse filtered = api.queryTasks(new TaskQuery.Builder()
                .completed(false)
                .dueBefore(LocalDate.of(2026, 12, 31))
                .priority(Priority.HIGH)
                .build());
        System.out.println("Filtered: " + filtered.toJson());

        service.removeListener(consoleListener);
        api.completeTask(1);
        System.out.println("Audit summary: " + auditListener.getEventCounts());
    }
}