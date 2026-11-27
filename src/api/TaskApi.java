package api;

import exception.ApiException;
import exception.ConflictException;
import model.Task;
import model.TaskQuery;
import model.TaskRequest;
import repository.IdempotencyStore;
import service.TaskService;

import java.util.List;

public class TaskApi {
    private final TaskService service;
    private final IdempotencyStore idempotencyStore;

    public TaskApi(TaskService service) {
        this(service, IdempotencyStore.getInstance());
    }

    public TaskApi(TaskService service, IdempotencyStore idempotencyStore) {
        this.service = service;
        this.idempotencyStore = idempotencyStore;
    }

    public ApiResponse createTask(TaskRequest request) {
        return createTask(request, null);
    }

    public ApiResponse createTask(TaskRequest request, String idempotencyKey) {
        String fingerprint = fingerprintRequest(request);
        return executeIdempotent("createTask", idempotencyKey, fingerprint, () -> {
            Task created = service.createTask(request);
            return new ApiResponse(201, created.toJson());
        });
    }

    public ApiResponse createTaskFromText(String text, String idempotencyKey) {
        String fingerprint = "createTaskFromText|" + (text == null ? "" : text.trim());
        return executeIdempotent("createTaskFromText", idempotencyKey, fingerprint, () -> {
            Task created = service.createTaskFromText(text);
            return new ApiResponse(201, created.toJson());
        });
    }

    public ApiResponse createSubtask(int parentId, TaskRequest request) {
        try {
            Task created = service.createSubtask(parentId, request);
            return new ApiResponse(201, created.toJson());
        } catch (ApiException e) {
            return ErrorResponseFactory.from(e);
        } catch (Exception e) {
            return ErrorResponseFactory.internalError(e);
        }
    }

    public ApiResponse listSubtasks(int parentId) {
        try {
            List<Task> subtasks = service.listSubtasks(parentId);
            return new ApiResponse(200, toJsonArray(subtasks));
        } catch (ApiException e) {
            return ErrorResponseFactory.from(e);
        } catch (Exception e) {
            return ErrorResponseFactory.internalError(e);
        }
    }

    public ApiResponse breakDownTask(int id, String idempotencyKey) {
        String fingerprint = "breakDownTask|" + id;
        return executeIdempotent("breakDownTask", idempotencyKey, fingerprint, () -> {
            List<Task> subtasks = service.breakDownTask(id);
            return new ApiResponse(201, toJsonArray(subtasks));
        });
    }

    public ApiResponse getTask(int id) {
        try {
            Task task = service.getTaskById(id);
            return new ApiResponse(200, task.toJson());
        } catch (ApiException e) {
            return ErrorResponseFactory.from(e);
        } catch (Exception e) {
            return ErrorResponseFactory.internalError(e);
        }
    }

    public ApiResponse listTasks() {
        try {
            List<Task> tasks = service.getAllTasks();
            return new ApiResponse(200, toJsonArray(tasks));
        } catch (ApiException e) {
            return ErrorResponseFactory.from(e);
        } catch (Exception e) {
            return ErrorResponseFactory.internalError(e);
        }
    }

    public ApiResponse queryTasks(TaskQuery query) {
        try {
            List<Task> tasks = service.queryTasks(query);
            return new ApiResponse(200, toJsonArray(tasks));
        } catch (ApiException e) {
            return ErrorResponseFactory.from(e);
        } catch (Exception e) {
            return ErrorResponseFactory.internalError(e);
        }
    }

    public ApiResponse updateTask(int id, TaskRequest request) {
        try {
            Task updated = service.updateTask(id, request);
            return new ApiResponse(200, updated.toJson());
        } catch (ApiException e) {
            return ErrorResponseFactory.from(e);
        } catch (Exception e) {
            return ErrorResponseFactory.internalError(e);
        }
    }

    public ApiResponse completeTask(int id) {
        try {
            Task completed = service.completeTask(id);
            return new ApiResponse(200, completed.toJson());
        } catch (ApiException e) {
            return ErrorResponseFactory.from(e);
        } catch (Exception e) {
            return ErrorResponseFactory.internalError(e);
        }
    }

    public ApiResponse deleteTask(int id) {
        try {
            service.deleteTask(id);
            return new ApiResponse(200, "{\"deleted\":true,\"id\":" + id + "}");
        } catch (ApiException e) {
            return ErrorResponseFactory.from(e);
        } catch (Exception e) {
            return ErrorResponseFactory.internalError(e);
        }
    }

    private ApiResponse executeIdempotent(String operation, String key, String fingerprint, OperationAction action) {
        try {
            String scopedKey = (key == null || key.isBlank()) ? null : operation + ":" + key.trim();
            if (scopedKey != null && idempotencyStore != null) {
                IdempotencyStore.StoredRecord existing = idempotencyStore.get(scopedKey);
                if (existing != null) {
                    if (!existing.getFingerprint().equals(fingerprint)) {
                        throw new ConflictException("Idempotency key reused with a different request payload");
                    }
                    return existing.getResponse();
                }
            }

            ApiResponse response = action.run();
            if (scopedKey != null && idempotencyStore != null
                    && response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                idempotencyStore.putSuccess(scopedKey, fingerprint, response);
            }
            return response;
        } catch (ApiException e) {
            return ErrorResponseFactory.from(e);
        } catch (Exception e) {
            return ErrorResponseFactory.internalError(e);
        }
    }

    private static String fingerprintRequest(TaskRequest req) {
        if (req == null) return "null";
        return String.join("|",
                nullSafe(req.getTitle()),
                nullSafe(req.getDescription()),
                nullSafe(req.getCompleted()),
                nullSafe(req.getDueDate()),
                nullSafe(req.getPriority()));
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s.trim();
    }

    protected static String toJsonArray(List<Task> tasks) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < tasks.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(tasks.get(i).toJson());
        }
        sb.append("]");
        return sb.toString();
    }

    @FunctionalInterface
    private interface OperationAction {
        ApiResponse run();
    }
}