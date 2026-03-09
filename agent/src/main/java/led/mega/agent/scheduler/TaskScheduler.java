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
    private Long   agentDbId;
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
    public void setAgentCredentials(String agentId, Long agentDbId, String apiKey) {
        this.agentId = agentId;
        this.agentDbId = agentDbId;
        this.apiKey = apiKey;
    }
    
    /**
     * 모든 작업 스케줄 시작
     */
    public void startAllTasks() {
        log.info("작업 스케줄러 시작");
        // 1. 서비스 모니터링 설정 기반 동적 로그 수집
        startServiceLogMonitoringTasks();
        // 2. 서비스별 CPU/MEMORY/DISK 메트릭 수집
        startServiceMetricMonitoringTasks();

        // 3. 기존 고정 시스템 메트릭/로그 수집
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
        }, 30, TimeUnit.SECONDS);
        
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
        }, 1, TimeUnit.MINUTES);
        
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
     * 웹서버에 등록된 MonitoringConfig 중 LOG 수집이 활성화된 항목들을 읽어와
     * 서비스별 로그 모니터링 작업을 동적으로 등록한다.
     *
     * - collectItems 에 LOG 가 포함된 설정만 대상
     * - logPath 가 비어 있으면 무시
     */
    private void startServiceLogMonitoringTasks() {
        if (agentDbId == null) {
            log.warn("agentDbId 가 설정되지 않아 서비스 모니터링 설정을 조회할 수 없습니다.");
            return;
        }

        try {
            java.util.List<ApiClient.MonitoringConfigDto> configs =
                    apiClient.fetchActiveMonitoringConfigs(agentDbId, apiKey);

            for (ApiClient.MonitoringConfigDto cfg : configs) {
                String collectItems = cfg.getCollectItems() != null ? cfg.getCollectItems() : "";
                boolean logEnabled = collectItems.toUpperCase().contains("LOG");
                String logPath = cfg.getLogPath();

                if (!logEnabled || logPath == null || logPath.isBlank()) {
                    continue;
                }

                int interval = cfg.getIntervalSeconds() != null ? cfg.getIntervalSeconds() : 600;
                String taskName = "service-log-" + cfg.getId();

                scheduleTask(taskName, () -> {
                    try {
                        java.util.List<LogParser.ExceptionInfo> exceptions =
                                logParser.parseExceptions(logPath);

                        for (LogParser.ExceptionInfo exceptionInfo : exceptions) {
                            apiClient.sendExceptionLog(agentId, apiKey, new ApiClient.ExceptionRequest(
                                    cfg.getId(),  // taskId 대신 monitoring_config id 연동
                                    exceptionInfo.getLogFilePath(),
                                    exceptionInfo.getExceptionType(),
                                    exceptionInfo.getExceptionMessage(),
                                    exceptionInfo.getContextBefore(),
                                    exceptionInfo.getContextAfter(),
                                    exceptionInfo.getFullStackTrace(),
                                    LocalDateTime.now()
                            ));
                        }

                        log.debug("서비스 로그 메트릭 전송 완료: serviceName={}, logPath={}",
                                cfg.getServiceName(), logPath);
                    } catch (Exception e) {
                        log.error("서비스 로그 메트릭 수집 실패: serviceName={}, logPath={}",
                                cfg.getServiceName(), logPath, e);
                    }
                }, interval, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.error("모니터링 설정을 불러오는 데 실패했습니다. 서비스 로그 모니터링은 일시적으로 비활성화됩니다.", e);
        }
    }

    /**
     * 웹서버에 등록된 MonitoringConfig 중 CPU/MEMORY/DISK 수집이 활성화된 항목마다
     * 서비스별 메트릭 수집 스케줄을 등록한다. 수집한 메트릭은 monitoringConfigId 로 전송한다.
     */
    private void startServiceMetricMonitoringTasks() {
        if (agentDbId == null) {
            log.warn("agentDbId 가 설정되지 않아 서비스 메트릭 수집을 등록할 수 없습니다.");
            return;
        }

        try {
            List<ApiClient.MonitoringConfigDto> configs =
                    apiClient.fetchActiveMonitoringConfigs(agentDbId, apiKey);

            for (ApiClient.MonitoringConfigDto cfg : configs) {
                String collectItems = cfg.getCollectItems() != null ? cfg.getCollectItems().toUpperCase() : "";
                boolean needCpu = collectItems.contains("CPU");
                boolean needMem = collectItems.contains("MEMORY");
                boolean needDisk = collectItems.contains("DISK");
                if (!needCpu && !needMem && !needDisk) {
                    continue;
                }

                int interval = cfg.getIntervalSeconds() != null ? cfg.getIntervalSeconds() : 30;
                Long configId = cfg.getId();
                String taskName = "service-metric-" + configId;

                scheduleTask(taskName, () -> collectAndSendServiceMetrics(configId, needCpu, needMem, needDisk), interval, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.error("모니터링 설정을 불러오는 데 실패했습니다. 서비스 메트릭 수집은 일시적으로 비활성화됩니다.", e);
        }
    }

    private void collectAndSendServiceMetrics(Long monitoringConfigId, boolean needCpu, boolean needMem, boolean needDisk) {
        LocalDateTime now = LocalDateTime.now();
        Gson gson = new Gson();

        if (needCpu) {
            try {
                CommandExecutor.CommandResult result = commandExecutor.execute(new String[]{"top", "-bn1", "-d1"});
                if (result.isSuccess()) {
                    BigDecimal cpuUsage = metricParser.parseCpuUsage(result.getOutputAsString());
                    Map<String, Object> raw = new HashMap<>();
                    raw.put("cpuUsage", cpuUsage);
                    apiClient.sendMetricData(agentId, apiKey, new ApiClient.MetricRequest(
                            null, monitoringConfigId, "CPU", "cpu_usage_percent", cpuUsage, "%", gson.toJson(raw), now));
                }
            } catch (Exception e) {
                log.debug("서비스 CPU 메트릭 수집 실패: configId={}", monitoringConfigId, e);
            }
        }
        if (needMem) {
            try {
                String output = commandExecutor.executeToString("free -m");
                Map<String, Object> metrics = metricParser.parseFreeMemory(output);
                BigDecimal usedPercent = new BigDecimal(metrics.get("usedPercent").toString());
                apiClient.sendMetricData(agentId, apiKey, new ApiClient.MetricRequest(
                        null, monitoringConfigId, "MEMORY", "memory_usage_percent", usedPercent, "%", gson.toJson(metrics), now));
            } catch (Exception e) {
                log.debug("서비스 MEMORY 메트릭 수집 실패: configId={}", monitoringConfigId, e);
            }
        }
        if (needDisk) {
            try {
                String output = commandExecutor.executeToString("df -h");
                Map<String, Map<String, Object>> diskMetrics = metricParser.parseDiskUsage(output);
                for (Map.Entry<String, Map<String, Object>> entry : diskMetrics.entrySet()) {
                    String mountPoint = entry.getKey();
                    Map<String, Object> diskInfo = entry.getValue();
                    BigDecimal usePercent = new BigDecimal(diskInfo.get("usePercent").toString());
                    apiClient.sendMetricData(agentId, apiKey, new ApiClient.MetricRequest(
                            null, monitoringConfigId, "DISK", "disk_usage_" + mountPoint.replace("/", "_"), usePercent, "%", gson.toJson(diskInfo), now));
                }
            } catch (Exception e) {
                log.debug("서비스 DISK 메트릭 수집 실패: configId={}", monitoringConfigId, e);
            }
        }
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

