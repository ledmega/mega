package led.mega.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MetricDataRequestDto {
    
    private Long taskId;
    
    @NotBlank(message = "메트릭 타입은 필수입니다")
    @Size(max = 50, message = "메트릭 타입은 50자 이하여야 합니다")
    private String metricType;  // CPU, MEMORY, DISK, NETWORK
    
    @Size(max = 100, message = "메트릭 이름은 100자 이하여야 합니다")
    private String metricName;
    
    private BigDecimal metricValue;
    
    @Size(max = 20, message = "단위는 20자 이하여야 합니다")
    private String unit;
    
    @JsonProperty("rawData")
    private String rawData;  // JSON 문자열
    
    @NotNull(message = "수집 시간은 필수입니다")
    private LocalDateTime collectedAt;
}

