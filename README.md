# Task Manager (Java 17)

An in-memory REST-like Task Manager built with Java 17 and zero external dependencies.

## Features

- **CRUD & Validation:** Create, retrieve, update, complete, and delete tasks with strict calendar date validation (including leap-year rules) and field-level error details.
- **AI-Native Task Parsing & Breakdown:** Parse plain-English task descriptions and split tasks into subtasks using swappable Strategy and Factory implementations (`LlmTaskParser` / `RuleBasedTaskParser`, `LlmBreakdownStrategy` / `RuleBasedBreakdownStrategy`).
- **Idempotency Support:** Scoped idempotency keys prevent duplicate task creation on retries (`201`) and return `422` when reused with a different payload.
- **Filtering, Search, Sorting & Pagination:** Query tasks using `TaskQuery` (Builder pattern) with status, priority, due-date range, substring search, pluggable sort strategies, and pagination.
- **Event Notifications:** Observer pattern (`TaskEventListener`) supporting runtime listener registration and removal (`ConsoleLogListener`, `AuditTrailListener`).

## Project Structure

```text
taskmanager/
├── .gitignore
├── README.md
├── API_CONTRACT.md
└── src/
    ├── api/         # TaskApi, ApiResponse, ErrorResponseFactory
    ├── app/         # Main, TestRunner
    ├── event/       # TaskEventListener, ConsoleLogListener, AuditTrailListener
    ├── exception/   # ApiException hierarchy and FieldErrorDetail
    ├── model/       # Task (Builder), Priority, TaskRequest, TaskQuery (Builder)
    ├── parser/      # TaskParser, BreakdownStrategy, ParserFactory, LlmClient, FakeLlmClient, FaultyLlmClient, SimpleJson
    ├── repository/  # TaskRepository, InMemoryTaskRepository (Singleton), IdempotencyStore (Singleton)
    └── service/     # TaskService, TaskFilter implementations, SortStrategy implementations