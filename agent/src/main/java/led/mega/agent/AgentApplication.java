package led.mega.agent;

import led.mega.agent.client.ApiClient;
import led.mega.agent.config.AgentConfig;
import led.mega.agent.executor.CommandExecutor;
import led.mega.agent.parser.LogParser;
import led.mega.agent.parser.MetricParser;
import led.mega.agent.scheduler.TaskScheduler;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 에이전트 메인 애플리케이션
 * 리모트 리눅스 서버에서 실행되어 시스템 모니터링 및 데이터 수집을 수행합니다.
 */
@Slf4j
public class AgentApplication {
    
    private AgentConfig config;
    private ApiClient apiClient;
    private CommandExecutor commandExecutor;
    private MetricParser metricParser;
    private LogParser logParser;
    private TaskScheduler taskScheduler;
    private ScheduledExecutorService heartbeatScheduler;
    
    private String agentId;
    private String apiKey;
    private boolean running = false;
    
    public static void main(String[] args) {
        AgentApplication app = new AgentApplication();
        app.start();
        
        // 종료 시그널 처리
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("종료 시그널 수신, 애플리케이션 종료 중...");
            app.stop();
        }));
        
        // 메인 스레드 대기
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            log.error("메인 스레드 중단", e);
        }
    }
    
    /**
     * 애플리케이션 시작
     */
    public void start() {
        log.info("에이전트 애플리케이션 시작");
        
        try {
            // 1. 설정 로드
            config = AgentConfig.getInstance();
            
            // 호스트명과 IP 자동 감지
            if ("unknown".equals(config.getHostname())) {
                String detectedHostname = config.detectHostname();
                log.info("호스트명 자동 감지: {}", detectedHostname);
            }
            if ("unknown".equals(config.getIpAddress())) {
                String detectedIp = config.detectIpAddress();
                log.info("IP 주소 자동 감지: {}", detectedIp);
            }
            
            // 2. 컴포넌트 초기화
            apiClient = new ApiClient(config);
            commandExecutor = new CommandExecutor();
            metricParser = new MetricParser();
            logParser = new LogParser();
            taskScheduler = new TaskScheduler(config, apiClient, commandExecutor, metricParser, logParser);
            
            // 3. 에이전트 등록
            if (!registerAgent()) {
                log.error("에이전트 등록 실패, 애플리케이션 종료");
                System.exit(1);
            }
            
            // 4. 작업 스케줄러 시작
            taskScheduler.setAgentCredentials(agentId, apiKey);
            taskScheduler.startAllTasks();
            
            // 5. 하트비트 전송 시작
            startHeartbeat();
            
            running = true;
            log.info("에이전트 애플리케이션 시작 완료");
            
        } catch (Exception e) {
            log.error("에이전트 시작 실패", e);
            System.exit(1);
        }
    }
    
    /**
     * 에이전트 등록
     */
    private boolean registerAgent() {
        try {
            String osType = System.getProperty("os.name", "Linux");
            String hostname = config.getHostname();
            String ipAddress = config.getIpAddress();
            
            ApiClient.RegisterRequest request = new ApiClient.RegisterRequest(
                config.getAgentName(),
                config.getAgentName(),
                hostname,
                ipAddress,
                osType
            );
            
            ApiClient.RegisterResponse response = apiClient.registerAgent(request);
            
            this.agentId = response.getAgentId();
            this.apiKey = response.getApiKey();
            
            log.info("에이전트 등록 성공: agentId={}", agentId);
            return true;
            
        } catch (Exception e) {
            log.error("에이전트 등록 실패", e);
            return false;
        }
    }
    
    /**
     * 하트비트 전송 시작
     */
    private void startHeartbeat() {
        heartbeatScheduler = Executors.newScheduledThreadPool(1);
        
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            try {
                if (agentId != null && apiKey != null) {
                    ApiClient.HeartbeatRequest request = new ApiClient.HeartbeatRequest(
                        "ONLINE",
                        LocalDateTime.now()
                    );
                    
                    boolean success = apiClient.sendHeartbeat(agentId, apiKey, request);
                    if (success) {
                        log.debug("하트비트 전송 성공");
                    } else {
                        log.warn("하트비트 전송 실패");
                    }
                }
            } catch (Exception e) {
                log.error("하트비트 전송 중 오류 발생", e);
            }
        }, 0, config.getHeartbeatIntervalSeconds(), TimeUnit.SECONDS);
        
        log.info("하트비트 전송 시작 (주기: {}초)", config.getHeartbeatIntervalSeconds());
    }
    
    /**
     * 애플리케이션 종료
     */
    public void stop() {
        if (!running) {
            return;
        }
        
        log.info("에이전트 애플리케이션 종료 중...");
        running = false;
        
        // 하트비트 중지
        if (heartbeatScheduler != null) {
            heartbeatScheduler.shutdown();
            try {
                if (!heartbeatScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    heartbeatScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                heartbeatScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        // 작업 스케줄러 중지
        if (taskScheduler != null) {
            taskScheduler.stopAllTasks();
        }
        
        // 마지막 하트비트 (OFFLINE 상태)
        if (agentId != null && apiKey != null) {
            try {
                ApiClient.HeartbeatRequest request = new ApiClient.HeartbeatRequest(
                    "OFFLINE",
                    LocalDateTime.now()
                );
                apiClient.sendHeartbeat(agentId, apiKey, request);
            } catch (Exception e) {
                log.error("마지막 하트비트 전송 실패", e);
            }
        }
        
        log.info("에이전트 애플리케이션 종료 완료");
    }
}
