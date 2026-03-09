package led.mega.agent.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import led.mega.agent.config.AgentConfig;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 웹서버와 통신하는 HTTP 클라이언트
 */
@Slf4j
public class ApiClient {
    
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final AgentConfig config;
    private final String baseUrl;
    
    public ApiClient(AgentConfig config) {
        this.config = config;
        this.baseUrl = config.getWebserverUrl();
        
        // HTTP 클라이언트 설정 (타임아웃 포함)
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();
        
        // Gson 설정 (LocalDateTime 직렬화/역직렬화 포함)
        this.gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) -> {
                return new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            })
            .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) -> {
                return LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            })
            .create();
    }
    
    /**
     * 에이전트 등록
     */
    public RegisterResponse registerAgent(RegisterRequest request) throws IOException {
        String url = baseUrl + "/api/agents/register";
        String json = gson.toJson(request);
        
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json; charset=utf-8"));
        Request httpRequest = new Request.Builder()
            .url(url)
            .post(body)
            .build();
        
        try (Response response = httpClient.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                log.error("에이전트 등록 실패: HTTP {} - {}", response.code(), errorBody);
                throw new IOException("에이전트 등록 실패: HTTP " + response.code());
            }
            
            String responseBody = response.body().string();
            RegisterResponse registerResponse = gson.fromJson(responseBody, RegisterResponse.class);
            log.info("에이전트 등록 성공: agentId={}, apiKey={}", 
                registerResponse.getAgentId(), registerResponse.getApiKey());
            return registerResponse;
        }
    }
    
    /**
     * 하트비트 전송
     */
    public boolean sendHeartbeat(String agentId, String apiKey, HeartbeatRequest request) {
        String url = baseUrl + "/api/agents/" + agentId + "/heartbeat";
        String json = gson.toJson(request);
        
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json; charset=utf-8"));
        Request httpRequest = new Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Authorization", "Bearer " + apiKey)
            .build();
        
        try (Response response = httpClient.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                log.error("하트비트 전송 실패: HTTP {} - {}", response.code(), errorBody);
                return false;
            }
            return true;
        } catch (IOException e) {
            log.error("하트비트 전송 중 오류 발생", e);
            return false;
        }
    }
    
    /**
     * 메트릭 데이터 전송
     */
    public boolean sendMetricData(String agentId, String apiKey, MetricRequest request) {
        String url = baseUrl + "/api/agents/" + agentId + "/metrics";
        String json = gson.toJson(request);
        
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json; charset=utf-8"));
        Request httpRequest = new Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Authorization", "Bearer " + apiKey)
            .build();
        
        try (Response response = httpClient.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                log.error("메트릭 데이터 전송 실패: HTTP {} - {}", response.code(), errorBody);
                return false;
            }
            return true;
        } catch (IOException e) {
            log.error("메트릭 데이터 전송 중 오류 발생", e);
            return false;
        }
    }
    
    /**
     * Exception 로그 전송
     */
    public boolean sendExceptionLog(String agentId, String apiKey, ExceptionRequest request) {
        String url = baseUrl + "/api/agents/" + agentId + "/exceptions";
        String json = gson.toJson(request);
        
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json; charset=utf-8"));
        Request httpRequest = new Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Authorization", "Bearer " + apiKey)
            .build();
        
        try (Response response = httpClient.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                log.error("Exception 로그 전송 실패: HTTP {} - {}", response.code(), errorBody);
                return false;
            }
            return true;
        } catch (IOException e) {
            log.error("Exception 로그 전송 중 오류 발생", e);
            return false;
        }
    }
    
    // DTO 클래스들
    public static class RegisterRequest {
        private String agentId;
        private String name;
        private String hostname;
        private String ipAddress;
        private String osType;
        
        public RegisterRequest(String agentId, String name, String hostname, String ipAddress, String osType) {
            this.agentId = agentId;
            this.name = name;
            this.hostname = hostname;
            this.ipAddress = ipAddress;
            this.osType = osType;
        }
        
        // Getters
        public String getAgentId() { return agentId; }
        public String getName() { return name; }
        public String getHostname() { return hostname; }
        public String getIpAddress() { return ipAddress; }
        public String getOsType() { return osType; }
    }
    
    public static class RegisterResponse {
        private Long id;
        private String agentId;
        private String status;
        private String apiKey;
        
        // Getters
        public Long getId() { return id; }
        public String getAgentId() { return agentId; }
        public String getStatus() { return status; }
        public String getApiKey() { return apiKey; }
    }
    
    public static class HeartbeatRequest {
        private String status;
        private LocalDateTime heartbeatAt;
        
        public HeartbeatRequest(String status, LocalDateTime heartbeatAt) {
            this.status = status;
            this.heartbeatAt = heartbeatAt;
        }
    }
    
    public static class MetricRequest {
        private Long taskId;
        private Long monitoringConfigId;
        private String metricType;
        private String metricName;
        private java.math.BigDecimal metricValue;
        private String unit;
        private String rawData;
        private LocalDateTime collectedAt;

        /** 서비스별 메트릭 시 monitoringConfigId 사용, 기존 taskId는 null 가능 */
        public MetricRequest(Long taskId, Long monitoringConfigId, String metricType, String metricName,
                            java.math.BigDecimal metricValue, String unit,
                            String rawData, LocalDateTime collectedAt) {
            this.taskId = taskId;
            this.monitoringConfigId = monitoringConfigId;
            this.metricType = metricType;
            this.metricName = metricName;
            this.metricValue = metricValue;
            this.unit = unit;
            this.rawData = rawData;
            this.collectedAt = collectedAt;
        }

        /** 기존 호환: taskId만 있고 monitoringConfigId 없음 */
        public MetricRequest(Long taskId, String metricType, String metricName,
                            java.math.BigDecimal metricValue, String unit,
                            String rawData, LocalDateTime collectedAt) {
            this(taskId, null, metricType, metricName, metricValue, unit, rawData, collectedAt);
        }
    }
    
    public static class ExceptionRequest {
        private Long taskId;
        private String logFilePath;
        private String exceptionType;
        private String exceptionMessage;
        private String contextBefore;
        private String contextAfter;
        private String fullStackTrace;
        private LocalDateTime occurredAt;
        
        public ExceptionRequest(Long taskId, String logFilePath, String exceptionType,
                              String exceptionMessage, String contextBefore, String contextAfter,
                              String fullStackTrace, LocalDateTime occurredAt) {
            this.taskId = taskId;
            this.logFilePath = logFilePath;
            this.exceptionType = exceptionType;
            this.exceptionMessage = exceptionMessage;
            this.contextBefore = contextBefore;
            this.contextAfter = contextAfter;
            this.fullStackTrace = fullStackTrace;
            this.occurredAt = occurredAt;
        }
    }

    /**
     * 활성화된 서비스 모니터링 설정 목록 조회 (Agent → WebServer Pull)
     */
    public List<MonitoringConfigDto> fetchActiveMonitoringConfigs(Long agentDbId, String apiKey) throws IOException {
        String url = baseUrl + "/api/configs/agent/" + agentDbId + "/active";

        Request httpRequest = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer " + apiKey)
                .build();

        try (Response response = httpClient.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                log.error("모니터링 설정 조회 실패: HTTP {} - {}", response.code(), errorBody);
                throw new IOException("모니터링 설정 조회 실패: HTTP " + response.code());
            }

            String responseBody = response.body().string();
            MonitoringConfigDto[] arr = gson.fromJson(responseBody, MonitoringConfigDto[].class);
            return Arrays.asList(arr != null ? arr : new MonitoringConfigDto[0]);
        }
    }

    /**
     * 에이전트가 사용하는 서비스 모니터링 설정 DTO
     * (webserver MonitoringConfigDto와 필드명을 맞춰서 사용)
     */
    public static class MonitoringConfigDto {
        private Long   id;
        private Long   agentId;
        private String serviceName;
        private String servicePath;
        private String logPath;
        private String collectItems;   // CSV: CPU,MEMORY,DISK,LOG
        private String logKeywords;    // CSV: Error,404,Exception
        private Integer intervalSeconds;
        private Boolean enabled;
        private String description;

        public Long getId() { return id; }
        public Long getAgentId() { return agentId; }
        public String getServiceName() { return serviceName; }
        public String getServicePath() { return servicePath; }
        public String getLogPath() { return logPath; }
        public String getCollectItems() { return collectItems; }
        public String getLogKeywords() { return logKeywords; }
        public Integer getIntervalSeconds() { return intervalSeconds; }
        public Boolean getEnabled() { return enabled; }
        public String getDescription() { return description; }
    }
}

