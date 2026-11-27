package app;

import api.ApiResponse;
import api.TaskApi;
import event.AuditTrailListener;
import model.Priority;
import model.TaskQuery;
import model.TaskRequest;
import parser.FaultyLlmClient;
import parser.LlmBreakdownStrategy;
import parser.LlmTaskParser;
import parser.ParserFactory;
import parser.RuleBasedBreakdownStrategy;
import repository.IdempotencyStore;
import repository.InMemoryTaskRepository;
import service.TaskService;

import java.time.LocalDate;

public class TestRunner {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        testCrudAndValidation();
        testAiParsingAndBreakdown();
        testIdempotency();
        testQueryAndObserver();

        System.out.println("Tests passed: " + passed + ", failed: " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void resetState() {
        InMemoryTaskRepository.getInstance().clear();
        IdempotencyStore.getInstance().clear();
    }

    private static void testCrudAndValidation() {
        resetState();
        TaskApi api = new TaskApi(new TaskService(InMemoryTaskRepository.getInstance()));

        ApiResponse created = api.createTask(
                new TaskRequest("Prepare deck", "Slides for review", "false", "2026-12-20", "HIGH"));
        assertTrue("createTask returns 201", created.getStatusCode() == 201);

        ApiResponse badDate = api.createTask(
                new TaskRequest("Leap check", "Invalid Feb 29", "false", "2026-02-29", "LOW"));
        assertTrue("invalid date 2026-02-29 returns 400", badDate.getStatusCode() == 400);

        ApiResponse missing = api.getTask(4040);
        assertTrue("unknown id returns 404", missing.getStatusCode() == 404);
    }

    private static void testAiParsingAndBreakdown() {
        resetState();
        TaskService service = new TaskService(
                InMemoryTaskRepository.getInstance(),
                ParserFactory.createParser(ParserFactory.Mode.RULE_BASED, null),
                new RuleBasedBreakdownStrategy()
        );
        TaskApi api = new TaskApi(service);

        ApiResponse parsed = api.createTaskFromText("Pay internet bill tomorrow", null);
        assertTrue("rule-based text task returns 201", parsed.getStatusCode() == 201);
        assertTrue("dueDate set to tomorrow", parsed.getBody().contains(LocalDate.now().plusDays(1).toString()));

        ApiResponse parent = api.createTask(
                new TaskRequest("Ship release", "Write tests, package build and publish", "false", null, "HIGH"));
        ApiResponse brokenDown = api.breakDownTask(1, null);
        assertTrue("breakdown creates subtasks with 201", brokenDown.getStatusCode() == 201);
        assertTrue("subtasks reference parentId 1", brokenDown.getBody().contains("\"parentId\":1"));

        service.setTaskParser(new LlmTaskParser(new FaultyLlmClient(FaultyLlmClient.FailureMode.BAD_GATEWAY)));
        ApiResponse badGateway = api.createTaskFromText("Anything", null);
        assertTrue("faulty llm returns 502", badGateway.getStatusCode() == 502);

        service.setBreakdownStrategy(new LlmBreakdownStrategy(new FaultyLlmClient(FaultyLlmClient.FailureMode.MALFORMED_JSON)));
        ApiResponse badBreakdown = api.breakDownTask(parent.getStatusCode() == 201 ? 2 : 1, null);
        assertTrue("malformed breakdown returns 502", badBreakdown.getStatusCode() == 502);
    }

    private static void testIdempotency() {
        resetState();
        TaskApi api = new TaskApi(new TaskService(InMemoryTaskRepository.getInstance()));
        TaskRequest req = new TaskRequest("Sync repo", "Push latest commits", "false", null, "MEDIUM");

        ApiResponse first = api.createTask(req, "idem-100");
        ApiResponse second = api.createTask(req, "idem-100");
        assertTrue("same key & payload returns same body", first.getBody().equals(second.getBody()));
        assertTrue("only 1 task stored after retry", api.listTasks().getBody().indexOf("\"id\":2") == -1);

        TaskRequest different = new TaskRequest("Different task", "Changed payload", "false", null, "LOW");
        ApiResponse conflict = api.createTask(different, "idem-100");
        assertTrue("same key & different payload returns 422", conflict.getStatusCode() == 422);
    }

    private static void testQueryAndObserver() {
        resetState();
        TaskService service = new TaskService(InMemoryTaskRepository.getInstance());
        AuditTrailListener audit = new AuditTrailListener();
        service.addListener(audit);
        TaskApi api = new TaskApi(service);

        api.createTask(new TaskRequest("Alpha API", "Design endpoints", "false", "2026-12-02", "HIGH"));
        api.createTask(new TaskRequest("Beta DB", "Tune indexes", "false", "2026-12-08", "MEDIUM"));
        api.createTask(new TaskRequest("Gamma Docs", "Update guide", "true", "2026-12-01", "LOW"));

        TaskQuery q = new TaskQuery.Builder()
                .completed(false)
                .priority(Priority.HIGH)
                .sortBy("dueDate")
                .ascending(true)
                .page(1)
                .pageSize(1)
                .build();

        ApiResponse res = api.queryTasks(q);
        assertTrue("query returns 200", res.getStatusCode() == 200);
        assertTrue("query matches Alpha API", res.getBody().contains("Alpha API"));
        assertTrue("audit recorded 3 TASK_CREATED events", audit.getEventCounts().getOrDefault("TASK_CREATED", 0) == 3);
    }

    private static void assertTrue(String name, boolean condition) {
        if (condition) {
            passed++;
        } else {
            failed++;
            System.err.println("FAIL: " + name);
        }
    }
}