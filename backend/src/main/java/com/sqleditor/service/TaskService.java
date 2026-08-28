package com.sqleditor.service;

import com.sqleditor.model.TaskProgress;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@Service
public class TaskService {
    private final Map<String, TaskProgress> tasks = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public String submitTask(String tableName, int totalRows, Consumer<TaskProgress> taskAction) {
        String taskId = UUID.randomUUID().toString();
        TaskProgress progress = new TaskProgress();
        progress.setTaskId(taskId);
        progress.setStatus("RUNNING");
        progress.setTotalRows(totalRows);
        progress.setProcessedRows(0);
        progress.setStartTime(System.currentTimeMillis());
        progress.setMessage("İşlem başlatılıyor...");
        progress.setTableName(tableName);
        tasks.put(taskId, progress);

        executor.submit(() -> {
            try {
                taskAction.accept(progress);
                if (!"CANCELLED".equals(progress.getStatus())) {
                    progress.setStatus("DONE");
                    progress.setMessage("Başarıyla tamamlandı.");
                    progress.setProcessedRows(progress.getTotalRows());
                    progress.setEstimatedTimeRemainingMs(0);
                }
            } catch (Exception e) {
                if (!"CANCELLED".equals(progress.getStatus())) {
                    progress.setStatus("ERROR");
                    progress.setMessage("Hata: " + e.getMessage());
                }
            }
        });
        return taskId;
    }

        public void cancelTask(String taskId) {
        TaskProgress p = tasks.get(taskId);
        if (p != null && "RUNNING".equals(p.getStatus())) {
            p.setStatus("CANCELLED");
            p.setMessage("İptal edildi.");
        }
    }

    public TaskProgress getProgress(String taskId) {
        return tasks.get(taskId);
    }
}
