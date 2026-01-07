package led.mega.agent.scheduler;

import com.google.gson.Gson;
import led.mega.agent.client.ApiClient;
import led.mega.agent.config.AgentConfig;
import led.mega.agent.executor.CommandExecutor;
import led.mega.agent.parser.LogParser;
import led.mega.agent.parser.MetricParser;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 작업 스케줄러
 * 주기적으로 명령어를 실행하고 결과를 서버에 전송합니다.
 */
@Slf4j
public class TaskScheduler {
    
    private final ScheduledExecutorService scheduler;
    private final CommandExecutor commandExecutor;
    private final MetricParser metricParser;
    private final LogParser logParser;
    private final ApiClient apiClient;
    private final AgentConfig config;
    
    private String agentId;
    private String apiKey;
    
    // 스케줄된 작업들
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new HashMap<>();
    
    public TaskScheduler(AgentConfig config, ApiClient apiClient, 
                        CommandExecutor commandExecutor, MetricParser metricParser, LogParser logParser) {
        this.config = config;
        this.apiClient = apiClient;
        this.commandExecutor = commandExecutor;
        this.metricParser = metricParser;
        this.logParser = logParser;
        this.scheduler = Executors.newScheduledThreadPool(10);
    }
    
    /**
     * 에이전트 ID와 API 키 설정
     */
    public void setAgentCredentials(String agentId, String apiKey) {
        this.agentId = agentId;
        this.apiKey = apiKey;
    }
    
    /**
     * 모든 작업 스케줄 시작
     */
    public void startAllTasks() {
        log.info("작업 스케줄러 시작");
        
        // 1분마다 free -m 실행
        scheduleTask("free-memory", () -> {
            try {
                String output = commandExecutor.executeToString("free -m");
                Map<String, Object> metrics = metricParser.parseFreeMemory(output);
                
                // 메모리 사용량 전송
                BigDecimal usedPercent = new BigDecimal(metrics.get("usedPercent").toString());
                String rawData = new Gson().toJson(metrics);
                
                apiClient.sendMetricData(agentId, apiKey, new ApiClient.MetricRequest(
                    null, "MEMORY", "memory_usage_percent", 
                    usedPercent, "%", rawData, LocalDateTime.now()
                ));
                
                // 사용 가능한 메모리 전송
                BigDecimal available = new BigDecimal(metrics.get("available").toString());
                apiClient.sendMetricData(agentId, apiKey, new ApiClient.MetricRequest(
                    null, "MEMORY", "available_memory", 
                    available, "MB", rawData, LocalDateTime.now()
                ));
                
                log.debug("메모리 메트릭 전송 완료");
            } catch (Exception e) {
                log.error("메모리 메트릭 수집 실패", e);
            }
        }, 1, TimeUnit.MINUTES);
        
        // 10분마다 df -h 실행
        scheduleTask("disk-usage", () -> {
            try {
                String output = commandExecutor.executeToString("df -h");
                Map<String, Map<String, Object>> diskMetrics = metricParser.parseDiskUsage(output);
                
                Gson gson = new Gson();
                for (Map.Entry<String, Map<String, Object>> entry : diskMetrics.entrySet()) {
                    String mountPoint = entry.getKey();
                    Map<String, Object> diskInfo = entry.getValue();
                    
                    BigDecimal usePercent = new BigDecimal(diskInfo.get("usePercent").toString());
                    String rawData = gson.toJson(diskInfo);
                    
                    apiClient.sendMetricData(agentId, apiKey, new ApiClient.MetricRequest(
                        null, "DISK", "disk_usage_" + mountPoint.replace("/", "_"),
                        usePercent, "%", rawData, LocalDateTime.now()
                    ));
                }
                
                log.debug("디스크 메트릭 전송 완료");
            } catch (Exception e) {
                log.error("디스크 메트릭 수집 실패", e);
            }
        }, 10, TimeUnit.MINUTES);
        
        // 30초마다 CPU 사용률 수집
        scheduleTask("cpu-usage", () -> {
            try {
                // top 명령어로 CPU 사용률 수집 (1초 동안)
                CommandExecutor.CommandResult result = commandExecutor.execute(
                    new String[]{"top", "-bn1", "-d1"}
                );
                
                if (result.isSuccess()) {
                    String output = result.getOutputAsString();
                    BigDecimal cpuUsage = metricParser.parseCpuUsage(output);
                    
                    Map<String, Object> rawData = new HashMap<>();
                    rawData.put("cpuUsage", cpuUsage);
                    String rawDataJson = new Gson().toJson(rawData);
                    
                    apiClient.sendMetricData(agentId, apiKey, new ApiClient.MetricRequest(
                        null, "CPU", "cpu_usage_percent",
                        cpuUsage, "%", rawDataJson, LocalDateTime.now()
                    ));
                    
                    log.debug("CPU 메트릭 전송 완료: {}%", cpuUsage);
                }
            } catch (Exception e) {
                log.error("CPU 메트릭 수집 실패", e);
            }
        }, 30, TimeUnit.SECONDS);
        
        // 10분마다 로그 파일에서 Exception 파싱
        scheduleTask("exception-log", () -> {
            try {
                // 일반적인 로그 파일 경로들
                String[] logPaths = {
                    "/var/log/app/application.log",
                    "/var/log/app/error.log",
                    "/opt/app/logs/application.log",
                    "./logs/application.log"
                };
                
                for (String logPath : logPaths) {
                    List<LogParser.ExceptionInfo> exceptions = logParser.parseExceptions(logPath);
                    
                    for (LogParser.ExceptionInfo exceptionInfo : exceptions) {
                        apiClient.sendExceptionLog(agentId, apiKey, new ApiClient.ExceptionRequest(
                            null, exceptionInfo.getLogFilePath(),
                            exceptionInfo.getExceptionType(),
                            exceptionInfo.getExceptionMessage(),
                            exceptionInfo.getContextBefore(),
                            exceptionInfo.getContextAfter(),
                            exceptionInfo.getFullStackTrace(),
                            LocalDateTime.now()
                        ));
                    }
                }
                
                log.debug("Exception 로그 전송 완료");
            } catch (Exception e) {
                log.error("Exception 로그 수집 실패", e);
            }
        }, 10, TimeUnit.MINUTES);
        
        log.info("모든 작업 스케줄 완료");
    }
    
    /**
     * 작업 스케줄
     */
    private void scheduleTask(String taskName, Runnable task, long period, TimeUnit unit) {
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
            () -> {
                try {
                    task.run();
                } catch (Exception e) {
                    log.error("작업 실행 중 오류 발생: {}", taskName, e);
                }
            },
            0, // 초기 지연 시간
            period, // 실행 주기
            unit
        );
        
        scheduledTasks.put(taskName, future);
        log.info("작업 스케줄 등록: {} (주기: {} {})", taskName, period, unit);
    }
    
    /**
     * 모든 작업 중지
     */
    public void stopAllTasks() {
        log.info("작업 스케줄러 중지");
        
        for (Map.Entry<String, ScheduledFuture<?>> entry : scheduledTasks.entrySet()) {
            entry.getValue().cancel(false);
            log.info("작업 중지: {}", entry.getKey());
        }
        
        scheduledTasks.clear();
        scheduler.shutdown();
        
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        log.info("작업 스케줄러 종료 완료");
    }
}

