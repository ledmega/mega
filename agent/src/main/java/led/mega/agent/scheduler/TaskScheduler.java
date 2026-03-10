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
import java.util.stream.Collectors;

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
    // 현재 활성화된 서비스 모니터링 설정 (동적 스케줄링 관리를 위함)
    private final Map<Long, ApiClient.MonitoringConfigDto> activeServiceMonitoringConfigs = new HashMap<>();
    
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
        // 2. 서비스별 CPU/MEMORY/DISK 메트릭 수집
        // 이 두 가지는 refreshServiceMonitoringTasks() 에서 관리
        refreshServiceMonitoringTasks(); // 초기 로드
        scheduleTask("refresh-monitoring-configs", this::refreshServiceMonitoringTasks, 60, TimeUnit.SECONDS); // 60초마다 재로드

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
        
        // 30초마다 CPU 사용률 수집 (/proc/stat 델타 방식 - top보다 정확)
        scheduleTask("cpu-usage", () -> {
            try {
                // [FIX] top -bn1은 첫 실행 시 비교 기준값 없어 0 반환
                //       → cat /proc/stat 토대 이전 스냅샷과 델타로 정확한 CPU% 계산
                String procStat = commandExecutor.executeToString("cat /proc/stat");
                BigDecimal cpuUsage = metricParser.parseCpuUsageFromProcStat(procStat, "host-cpu");

                Map<String, Object> rawData = new HashMap<>();
                rawData.put("cpuUsage", cpuUsage);
                String rawDataJson = new Gson().toJson(rawData);

                apiClient.sendMetricData(agentId, apiKey, new ApiClient.MetricRequest(
                    null, "CPU", "cpu_usage_percent",
                    cpuUsage, "%", rawDataJson, LocalDateTime.now()
                ));

                log.debug("CPU 메트릭 전송 완료: {}%", cpuUsage);
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
     * 서버에서 모니터링 설정을 주기적으로 가져와 동적으로 스케줄링을 관리합니다.
     * 추가/변경/삭제된 설정을 반영하여 스케줄된 작업을 업데이트합니다.
     */
    private void refreshServiceMonitoringTasks() {
        if (agentDbId == null) {
            log.warn("agentDbId 가 설정되지 않아 서비스 모니터링 설정을 조회할 수 없습니다.");
            return;
        }

        try {
            List<ApiClient.MonitoringConfigDto> latestConfigs =
                    apiClient.fetchActiveMonitoringConfigs(agentDbId, apiKey);

            Map<Long, ApiClient.MonitoringConfigDto> newActiveConfigs = latestConfigs.stream()
                    .collect(Collectors.toMap(ApiClient.MonitoringConfigDto::getId, cfg -> cfg));

            // 1. 삭제된 작업 중지
            for (Map.Entry<Long, ApiClient.MonitoringConfigDto> entry : activeServiceMonitoringConfigs.entrySet()) {
                Long configId = entry.getKey();
                if (!newActiveConfigs.containsKey(configId)) {
                    stopServiceMonitoringTasks(configId);
                }
            }

            // 2. 새로 추가되거나 변경된 작업 시작/업데이트
            for (ApiClient.MonitoringConfigDto newCfg : latestConfigs) {
                Long configId = newCfg.getId();
                ApiClient.MonitoringConfigDto oldCfg = activeServiceMonitoringConfigs.get(configId);

                boolean isNew     = (oldCfg == null);
                boolean isChanged = !isNew && isConfigChanged(oldCfg, newCfg);

                if (isNew || isChanged) {
                    if (isChanged) {
                        stopServiceMonitoringTasks(configId);
                        log.info("[Config Refresh] 설정 변경 감지 → 작업 재등록: configId={}, serviceName={}",
                                configId, newCfg.getServiceName());
                    } else {
                        log.info("[Config Refresh] 신규 설정 감지 → 작업 등록: configId={}, serviceName={}",
                                configId, newCfg.getServiceName());
                    }
                    startServiceMonitoringTasks(newCfg);
                }
            }

            // 현재 활성화된 설정 업데이트
            activeServiceMonitoringConfigs.clear();
            activeServiceMonitoringConfigs.putAll(newActiveConfigs);

            log.info("서비스 모니터링 설정 새로고침 완료. 현재 활성 설정 수: {}", activeServiceMonitoringConfigs.size());

        } catch (Exception e) {
            log.error("모니터링 설정을 불러오는 데 실패했습니다. 서비스 모니터링 스케줄러 새로고침 실패.", e);
        }
    }

    /**
     * 두 config의 주요 필드(intervalSeconds, collectItems, logPath)가 달라졌는지 판별한다.
     * 이 중 하나라도 변경되면 기존 작업을 취소하고 재등록해야 한다.
     */
    private boolean isConfigChanged(ApiClient.MonitoringConfigDto oldCfg, ApiClient.MonitoringConfigDto newCfg) {
        int oldInterval = oldCfg.getIntervalSeconds() != null ? oldCfg.getIntervalSeconds() : 30;
        int newInterval = newCfg.getIntervalSeconds() != null ? newCfg.getIntervalSeconds() : 30;
        if (oldInterval != newInterval) return true;

        String oldItems = oldCfg.getCollectItems() != null ? oldCfg.getCollectItems() : "";
        String newItems = newCfg.getCollectItems() != null ? newCfg.getCollectItems() : "";
        if (!oldItems.equalsIgnoreCase(newItems)) return true;

        String oldLog = oldCfg.getLogPath() != null ? oldCfg.getLogPath() : "";
        String newLog = newCfg.getLogPath() != null ? newCfg.getLogPath() : "";
        return !oldLog.equals(newLog);
    }

    /**
     * 특정 MonitoringConfig 에 해당하는 모든 스케줄된 작업을 중지합니다.
     */
    private void stopServiceMonitoringTasks(Long configId) {
        String logTaskName = "service-log-" + configId;
        String metricTaskName = "service-metric-" + configId;

        ScheduledFuture<?> logFuture = scheduledTasks.remove(logTaskName);
        if (logFuture != null) {
            logFuture.cancel(false);
            log.info("서비스 로그 모니터링 작업 중지: {}", logTaskName);
        }

        ScheduledFuture<?> metricFuture = scheduledTasks.remove(metricTaskName);
        if (metricFuture != null) {
            metricFuture.cancel(false);
            log.info("서비스 메트릭 모니터링 작업 중지: {}", metricTaskName);
        }

        // 해당 config의 logPath 오프셋 초기화
        ApiClient.MonitoringConfigDto cfg = activeServiceMonitoringConfigs.get(configId);
        if (cfg != null && cfg.getLogPath() != null) {
            logParser.resetOffset(cfg.getLogPath());
        }
    }

    /**
     * MonitoringConfigDto 에 따라 서비스 모니터링 작업을 스케줄링합니다.
     */
    private void startServiceMonitoringTasks(ApiClient.MonitoringConfigDto cfg) {
        String collectItems = cfg.getCollectItems() != null ? cfg.getCollectItems().toUpperCase() : "";
        int interval = cfg.getIntervalSeconds() != null ? cfg.getIntervalSeconds() : 60; // 기본 60초

        // 로그 키워드 모니터링 (LOG 수집 + logPath 있을 때)
        boolean logEnabled = collectItems.contains("LOG");
        String logPath = cfg.getLogPath();
        String logKeywords = cfg.getLogKeywords(); // CSV: "Error,404,Exception,WARN"
        if (logEnabled && logPath != null && !logPath.isBlank()) {
            String taskName = "service-log-" + cfg.getId();
            scheduleTask(taskName, () -> {
                try {
                    java.util.List<LogParser.ExceptionInfo> hits;

                    if (logKeywords != null && !logKeywords.isBlank()) {
                        // [P2] Tail + 키워드 매칭: 새로 추가된 라인에서만 키워드 검색
                        hits = logParser.tailAndMatchKeywords(logPath, logKeywords);
                    } else {
                        // 키워드 미설정 시 기존 Java Exception 스택트레이스 파싱
                        hits = logParser.parseExceptions(logPath);
                    }

                    for (LogParser.ExceptionInfo ex : hits) {
                        apiClient.sendExceptionLog(agentId, apiKey, new ApiClient.ExceptionRequest(
                                cfg.getId(),
                                ex.getLogFilePath(),
                                ex.getExceptionType(),
                                ex.getExceptionMessage(),
                                ex.getContextBefore(),
                                ex.getContextAfter(),
                                ex.getFullStackTrace(),
                                LocalDateTime.now()
                        ));
                    }
                    if (!hits.isEmpty()) {
                        log.info("[Log Monitor] {}건 전송: serviceName={}, keywords={}",
                                hits.size(), cfg.getServiceName(), logKeywords);
                    } else {
                        log.debug("[Log Monitor] 새 감지 없음: serviceName={}", cfg.getServiceName());
                    }
                } catch (Exception e) {
                    log.error("[Log Monitor] 서비스 로그 수집 실패: serviceName={}, logPath={}", cfg.getServiceName(), logPath, e);
                }
            }, interval, TimeUnit.SECONDS);
        }

        // 메트릭 모니터링
        boolean needCpu = collectItems.contains("CPU");
        boolean needMem = collectItems.contains("MEMORY");
        boolean needDisk = collectItems.contains("DISK");
        if (needCpu || needMem || needDisk) {
            String taskName = "service-metric-" + cfg.getId();
            scheduleTask(taskName, () -> collectAndSendServiceMetrics(cfg, needCpu, needMem, needDisk), interval, TimeUnit.SECONDS);
        }
    }

    /**
     * 웹서버에 등록된 MonitoringConfig 중 LOG 수집이 활성화된 항목들을 읽어와
     * 서비스별 로그 모니터링 작업을 동적으로 등록한다.
     *
     * - collectItems 에 LOG 가 포함된 설정만 대상
     * - logPath 가 비어 있으면 무시
     */
    private void startServiceLogMonitoringTasks() {
        // 이 메서드는 refreshServiceMonitoringTasks() 에 의해 대체됩니다.
        // 기존 코드는 유지하되, 호출되지 않도록 합니다.
    }

    /**
     * 웹서버에 등록된 MonitoringConfig 중 CPU/MEMORY/DISK 수집이 활성화된 항목마다
     * 서비스별 메트릭 수집 스케줄을 등록한다. 수집한 메트릭은 monitoringConfigId 로 전송한다.
     */
    private void startServiceMetricMonitoringTasks() {
        // 이 메서드는 refreshServiceMonitoringTasks() 에 의해 대체됩니다.
        // 기존 코드는 유지하되, 호출되지 않도록 합니다.
    }

    private void collectAndSendServiceMetrics(ApiClient.MonitoringConfigDto cfg, boolean needCpu, boolean needMem, boolean needDisk) {
        Long configId = cfg.getId();
        String targetType = cfg.getTargetType() != null ? cfg.getTargetType() : "HOST";
        String targetName = cfg.getTargetName();
        LocalDateTime now = LocalDateTime.now();

        BigDecimal cpu = null;
        BigDecimal memMb = null;
        BigDecimal memPct = null;
        BigDecimal diskPct = null;

        if ("DOCKER".equalsIgnoreCase(targetType) && targetName != null && !targetName.trim().isEmpty()) {
            try {
                // docker stats --no-stream --format "{{.CPUPerc}}|{{.MemUsage}}|{{.MemPerc}}" [컨테이너명]
                String cmd = "docker stats --no-stream --format \"{{.CPUPerc}}|{{.MemUsage}}|{{.MemPerc}}\" " + targetName;
                led.mega.agent.executor.CommandExecutor.CommandResult res = commandExecutor.execute(new String[]{"sh", "-c", cmd});
                String output = res.getOutputAsString();
                
                if (output != null && !output.trim().isEmpty()) {
                    String[] parts = output.trim().split("\\|");
                    if (parts.length >= 3) {
                        cpu = new BigDecimal(parts[0].replaceAll("[^0-9.]", ""));
                        memPct = new BigDecimal(parts[2].replaceAll("[^0-9.]", ""));
                        // MemUsage = 150MiB / 2GiB -> MB로 대략 변환
                        String memStr = parts[1].split("/")[0].replaceAll("[^0-9.]", "");
                        if (!memStr.isEmpty()) memMb = new BigDecimal(memStr);
                    }
                }
            } catch (Exception e) {
                log.debug("도커 메트릭 수집 실패: {}", targetName, e);
            }
        } else if ("PROCESS".equalsIgnoreCase(targetType) && targetName != null && !targetName.trim().isEmpty()) {
            try {
                // ps aux | grep -v grep | grep "이름" | awk '{cpu+=$3; mem+=$4} END {print cpu"|"mem}'
                String cmd = "ps aux | grep -v grep | grep \"" + targetName + "\" | awk '{cpu+=$3; mem+=$4} END {print cpu\"|\"mem}'";
                led.mega.agent.executor.CommandExecutor.CommandResult res = commandExecutor.execute(new String[]{"sh", "-c", cmd});
                String output = res.getOutputAsString();
                
                if (output != null && !output.trim().isEmpty() && output.contains("|")) {
                    String[] parts = output.trim().split("\\|");
                    if (parts.length == 2 && !parts[0].isEmpty()) {
                        cpu = new BigDecimal(parts[0]);
                        memPct = new BigDecimal(parts[1]);
                        memMb = memPct; // 프로세스 전체 MB계산은 어려워 % 값 동일 복사 (지표용 트릭)
                    }
                }
            } catch (Exception e) {
                log.debug("프로세스 메트릭 수집 실패: {}", targetName, e);
            }
        } 
        
        // HOST 모드일 때 (또는 위에서 아무것도 못구했을 때 보강용 - 기본 시스템 수집로직)
        if (cpu == null && ("HOST".equalsIgnoreCase(targetType) || needCpu)) {
            try {
                String procStat = commandExecutor.executeToString("cat /proc/stat");
                cpu = metricParser.parseCpuUsageFromProcStat(procStat, "service-cpu-" + configId);
            } catch (Exception e) {}
        }
        if (memPct == null && ("HOST".equalsIgnoreCase(targetType) || needMem)) {
            try {
                String output = commandExecutor.executeToString("free -m");
                Map<String, Object> metrics = metricParser.parseFreeMemory(output);
                if (metrics.containsKey("usedPercent")) memPct = new BigDecimal(metrics.get("usedPercent").toString());
                if (metrics.containsKey("used")) memMb = new BigDecimal(metrics.get("used").toString());
            } catch (Exception e) {}
        }
        if (diskPct == null && ("HOST".equalsIgnoreCase(targetType) || needDisk)) {
            try {
                String output = commandExecutor.executeToString("df -h");
                Map<String, Map<String, Object>> diskMetrics = metricParser.parseDiskUsage(output);
                if (!diskMetrics.isEmpty()) {
                    Map<String, Object> rootDisk = diskMetrics.get("/");
                    if (rootDisk == null) rootDisk = diskMetrics.values().iterator().next();
                    if (rootDisk.containsKey("usePercent")) diskPct = new BigDecimal(rootDisk.get("usePercent").toString());
                }
            } catch (Exception e) {}
        }

        try {
            apiClient.sendServiceMetricData(agentId, apiKey, 
                new ApiClient.ServiceMetricDataRequestDto(configId, cpu, memMb, memPct, diskPct, null, null, now)
            );
        } catch (Exception e) {
            log.error("Service Metric 전송 실패", e);
        }
    }

    /**
     * 작업 스케줄
     */
    private void scheduleTask(String taskName, Runnable task, long period, TimeUnit unit) {
        // 기존에 같은 이름의 작업이 있다면 중지하고 새로 스케줄링
        ScheduledFuture<?> existingTask = scheduledTasks.get(taskName);
        if (existingTask != null) {
            existingTask.cancel(false);
            log.info("기존 작업 중지 및 재스케줄링: {}", taskName);
        }

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
        activeServiceMonitoringConfigs.clear(); // 동적 스케줄러 상태도 초기화
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
