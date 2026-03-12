package led.mega.agent.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 에이전트 설정 관리 클래스
 * application.properties 파일에서 설정을 로드합니다.
 */
@Slf4j
@Getter
public class AgentConfig {
    
    private String webserverUrl;
    private String apiKey;
    private String agentName;
    private String hostname;
    private String ipAddress;
    private int heartbeatIntervalSeconds;
    
    // 작업 주기 설정
    private int memoryTaskIntervalSeconds;
    private int diskTaskIntervalSeconds;
    private int cpuTaskIntervalSeconds;
    private int exceptionTaskIntervalSeconds;
    
    // Exception 로그 파일 경로
    private String[] exceptionLogPaths;
    
    private static AgentConfig instance;
    
    private AgentConfig() {
        loadProperties();
    }
    
    public static AgentConfig getInstance() {
        if (instance == null) {
            synchronized (AgentConfig.class) {
                if (instance == null) {
                    instance = new AgentConfig();
                }
            }
        }
        return instance;
    }
    
    private void loadProperties() {
        Properties props = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (is == null) {
                log.error("application.properties 파일을 찾을 수 없습니다.");
                throw new RuntimeException("application.properties 파일을 찾을 수 없습니다.");
            }
            
            props.load(is);
            
            // 웹서버 연결 정보
            webserverUrl = props.getProperty("webserver.url", "http://localhost:8080");
            apiKey = props.getProperty("webserver.api.key", "");
            
            // 에이전트 정보
            agentName = props.getProperty("agent.name", "default-agent");
            hostname = getSystemProperty("HOSTNAME", props.getProperty("agent.hostname", "unknown"));
            ipAddress = getSystemProperty("HOST_IP", props.getProperty("agent.ip", "unknown"));

            // 플레이스홀더 문자열이면 자동 감지 시도
            if ("unknown".equals(hostname) || hostname.contains("${HOSTNAME")) {
                hostname = detectHostname();
            }
            if ("unknown".equals(ipAddress) || ipAddress.contains("${HOST_IP")) {
                ipAddress = detectIpAddress();
            }
            
            // 하트비트 설정
            heartbeatIntervalSeconds = Integer.parseInt(
                props.getProperty("heartbeat.interval.seconds", "30")
            );
            
            // 작업 주기 설정
            memoryTaskIntervalSeconds = Integer.parseInt(props.getProperty("task.memory.interval.seconds", "60"));
            diskTaskIntervalSeconds = Integer.parseInt(props.getProperty("task.disk.interval.seconds", "600"));
            cpuTaskIntervalSeconds = Integer.parseInt(props.getProperty("task.cpu.interval.seconds", "30"));
            exceptionTaskIntervalSeconds = Integer.parseInt(props.getProperty("task.exception.interval.seconds", "600"));
            
            // 로그 파일 경로 설정
            String paths = props.getProperty("task.exception.log.paths", "");
            if (!paths.isEmpty()) {
                exceptionLogPaths = paths.split(",");
                for (int i = 0; i < exceptionLogPaths.length; i++) {
                    exceptionLogPaths[i] = exceptionLogPaths[i].trim();
                }
            } else {
                exceptionLogPaths = new String[0];
            }
            
            log.info("에이전트 설정 로드 완료");
            log.info("웹서버 URL: {}", webserverUrl);
            log.info("에이전트 이름: {}", agentName);
            log.info("호스트명: {}", hostname);
            log.info("IP 주소: {}", ipAddress);
            log.info("하트비트 간격: {}초", heartbeatIntervalSeconds);
            log.info("메모리/디스크/CPU/Exception 주기: {}/{}/{}/{}초", 
                    memoryTaskIntervalSeconds, diskTaskIntervalSeconds, cpuTaskIntervalSeconds, exceptionTaskIntervalSeconds);
            
        } catch (IOException e) {
            log.error("설정 파일 로드 실패", e);
            throw new RuntimeException("설정 파일 로드 실패", e);
        }
    }
    
    private String getSystemProperty(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isEmpty()) {
            value = System.getProperty(key, defaultValue);
        }
        return value;
    }
    
    /**
     * 호스트명 자동 감지
     */
    public String detectHostname() {
        try {
            Process process = Runtime.getRuntime().exec("hostname");
            process.waitFor();
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream())
            );
            String line = reader.readLine();
            if (line != null && !line.trim().isEmpty()) {
                return line.trim();
            }
        } catch (Exception e) {
            log.warn("호스트명 자동 감지 실패", e);
        }
        return "unknown";
    }
    
    /**
     * IP 주소 자동 감지
     */
    public String detectIpAddress() {
        try {
            Process process = Runtime.getRuntime().exec("hostname -I");
            process.waitFor();
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream())
            );
            String line = reader.readLine();
            if (line != null && !line.trim().isEmpty()) {
                return line.trim().split("\\s+")[0]; // 첫 번째 IP 주소
            }
        } catch (Exception e) {
            log.warn("IP 주소 자동 감지 실패", e);
        }
        return "unknown";
    }
}

