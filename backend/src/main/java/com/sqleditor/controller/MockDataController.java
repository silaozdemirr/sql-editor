package com.sqleditor.controller;

import com.sqleditor.model.MockDataRequest;
import com.sqleditor.service.ConnectionSessionService;
import com.sqleditor.service.QueryService;
import com.sqleditor.service.TaskService;
import com.sqleditor.model.TaskProgress;
import jakarta.validation.Valid;
import net.datafaker.Faker;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/mock")
public class MockDataController {
    @DeleteMapping("/progress/{taskId}")
    public ResponseEntity<?> cancelTask(@PathVariable String taskId) {
        taskService.cancelTask(taskId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/progress/{taskId}")
    public ResponseEntity<TaskProgress> getProgress(@PathVariable String taskId) {
        TaskProgress p = taskService.getProgress(taskId);
        if (p == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(p);
    }

    private final ConnectionSessionService sessions;
    private final QueryService queryService;
    private final ThreadLocal<Faker> fakerThreadLocal = ThreadLocal.withInitial(() -> new Faker(new java.util.Locale("tr")));

    private final TaskService taskService;

    public MockDataController(ConnectionSessionService sessions, QueryService queryService, TaskService taskService) {
        this.sessions = sessions;
        this.queryService = queryService;
        this.taskService = taskService;
    }

    @PostMapping("/generate")
    public ResponseEntity<java.util.Map<String, String>> generateMockData(@Valid @RequestBody MockDataRequest req,
                                                                          @RequestHeader("X-Connection-Token") String token,
                                                                          Authentication auth) throws Exception {
        String role = auth.getAuthorities().iterator().next().getAuthority();
        if (role != null && role.contains("READ_ONLY")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sadece okuma yetkiniz var.");
        }


        String taskId = taskService.submitTask(req.getTableName(), req.getRowCount(), (progress) -> {
            int totalRows = req.getRowCount();
            int batchSize = 5000;
            int numThreads = 8;
            java.util.concurrent.atomic.AtomicInteger globalProcessed = new java.util.concurrent.atomic.AtomicInteger(0);
            long startTime = System.currentTimeMillis();

            StringBuilder sql = new StringBuilder("INSERT INTO ")
                    .append(req.getDatabaseName()).append(".").append(req.getTableName()).append(" (");
            
            StringBuilder placeholders = new StringBuilder();
            for (int i = 0; i < req.getMappings().size(); i++) {
                sql.append("").append(req.getMappings().get(i).getColumnName()).append("");
                placeholders.append("?");
                if (i < req.getMappings().size() - 1) {
                    sql.append(", ");
                    placeholders.append(", ");
                }
            }
            sql.append(") VALUES (").append(placeholders).append(")");
            String finalSql = sql.toString();

            java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(numThreads);
            
            for (int threadIdx = 0; threadIdx < numThreads; threadIdx++) {
                final int tIdx = threadIdx;
                pool.submit(() -> {
                    try (Connection conn = sessions.get(auth.getName(), token)) {
                        conn.setAutoCommit(false);
                        try (PreparedStatement pstmt = conn.prepareStatement(finalSql)) {
                            int rowsPerThread = totalRows / numThreads;
                            int start = tIdx * rowsPerThread;
                            int end = (tIdx == numThreads - 1) ? totalRows : start + rowsPerThread;
                            int count = 0;
                            
                            for (int r = start; r < end; r++) {
                                if ("CANCELLED".equals(progress.getStatus())) break;
                                
                                for (int i = 0; i < req.getMappings().size(); i++) {
                                    String type = req.getMappings().get(i).getFakerType();
                                    Object val = generateValue(type);
                                    pstmt.setObject(i + 1, val);
                                }
                                pstmt.addBatch();
                                count++;
                                
                                if (count % batchSize == 0) {
                                    pstmt.executeBatch();
                                    conn.commit();
                                    int currentProcessed = globalProcessed.addAndGet(batchSize);
                                    progress.setProcessedRows(currentProcessed);
                                    
                                    long elapsed = System.currentTimeMillis() - startTime;
                                    long remainingRows = totalRows - currentProcessed;
                                    long remainingMs = (elapsed * remainingRows) / Math.max(1, currentProcessed);
                                    progress.setEstimatedTimeRemainingMs(remainingMs);
                                }
                            }
                            if (count % batchSize != 0 && !"CANCELLED".equals(progress.getStatus())) {
                                pstmt.executeBatch();
                                conn.commit();
                                globalProcessed.addAndGet(count % batchSize);
                                progress.setProcessedRows(globalProcessed.get());
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
            pool.shutdown();
            try {
                pool.awaitTermination(24, java.util.concurrent.TimeUnit.HOURS);
            } catch (InterruptedException e) { }
        });

        return ResponseEntity.ok(java.util.Map.of("taskId", taskId));
    }


    private Object generateValue(String type) {
        return switch (type) {
            case "Name.fullName" -> fakerThreadLocal.get().name().fullName();
            case "Name.firstName" -> fakerThreadLocal.get().name().firstName();
            case "Name.lastName" -> fakerThreadLocal.get().name().lastName();
            case "Internet.email" -> fakerThreadLocal.get().internet().emailAddress();
            case "Internet.password" -> fakerThreadLocal.get().internet().password();
            case "Internet.domainName" -> fakerThreadLocal.get().internet().domainName();
            case "Internet.macAddress" -> fakerThreadLocal.get().internet().macAddress();
            case "Internet.ipv4Address" -> fakerThreadLocal.get().internet().ipV4Address();
            case "Address.fullAddress" -> fakerThreadLocal.get().address().fullAddress();
            case "Address.city" -> fakerThreadLocal.get().address().city();
            case "Address.country" -> fakerThreadLocal.get().address().country();
            case "Address.zipCode" -> fakerThreadLocal.get().address().zipCode();
            case "PhoneNumber.cellPhone" -> fakerThreadLocal.get().phoneNumber().cellPhone();
            case "Number.randomDigit" -> fakerThreadLocal.get().number().randomDigit();
            case "Number.randomInt" -> fakerThreadLocal.get().number().numberBetween(1, 100000);
            case "Number.age" -> fakerThreadLocal.get().number().numberBetween(0, 101);
            case "Number.randomDouble" -> fakerThreadLocal.get().number().randomDouble(2, 1, 5000);
            case "Date.birthday" -> new java.sql.Date(fakerThreadLocal.get().date().birthday().getTime());
            case "Date.future" -> new java.sql.Timestamp(fakerThreadLocal.get().date().future(365, java.util.concurrent.TimeUnit.DAYS).getTime());
            case "Date.past" -> new java.sql.Timestamp(fakerThreadLocal.get().date().past(365, java.util.concurrent.TimeUnit.DAYS).getTime());
            case "Date.now" -> new java.sql.Timestamp(System.currentTimeMillis());
            case "Company.name" -> fakerThreadLocal.get().company().name();
            case "Company.industry" -> fakerThreadLocal.get().company().industry();
            case "Commerce.productName" -> fakerThreadLocal.get().commerce().productName();
            case "Commerce.price" -> Double.valueOf(fakerThreadLocal.get().commerce().price().replace(",", "."));
            case "Commerce.department" -> fakerThreadLocal.get().commerce().department();
            case "Finance.creditCard" -> fakerThreadLocal.get().finance().creditCard();
            case "Job.title" -> fakerThreadLocal.get().job().title();
            case "Lorem.word" -> fakerThreadLocal.get().lorem().word();
            case "Lorem.sentence" -> fakerThreadLocal.get().lorem().sentence();
            case "Lorem.paragraph" -> fakerThreadLocal.get().lorem().paragraph();
            case "Color.name" -> fakerThreadLocal.get().color().name();
            case "Bool.random" -> fakerThreadLocal.get().bool().bool();
            case "Gender.types" -> fakerThreadLocal.get().gender().types();
            default -> fakerThreadLocal.get().lorem().word();
        };
    }
}
