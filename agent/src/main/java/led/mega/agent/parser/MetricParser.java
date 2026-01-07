package led.mega.agent.parser;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 메트릭 데이터 파서
 * 명령어 실행 결과를 파싱하여 메트릭 데이터로 변환합니다.
 */
@Slf4j
public class MetricParser {
    
    /**
     * free -m 명령어 결과 파싱
     * 
     * @param output free -m 명령어 출력
     * @return 파싱된 메모리 메트릭 데이터
     */
    public Map<String, Object> parseFreeMemory(String output) {
        Map<String, Object> metrics = new HashMap<>();
        
        try {
            String[] lines = output.split("\n");
            for (String line : lines) {
                if (line.startsWith("Mem:")) {
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length >= 4) {
                        long total = Long.parseLong(parts[1]);
                        long used = Long.parseLong(parts[2]);
                        long free = Long.parseLong(parts[3]);
                        long available = parts.length >= 7 ? Long.parseLong(parts[6]) : free;
                        
                        metrics.put("total", total);
                        metrics.put("used", used);
                        metrics.put("free", free);
                        metrics.put("available", available);
                        metrics.put("usedPercent", (double) used / total * 100);
                        metrics.put("freePercent", (double) free / total * 100);
                    }
                } else if (line.startsWith("Swap:")) {
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length >= 4) {
                        long swapTotal = Long.parseLong(parts[1]);
                        long swapUsed = Long.parseLong(parts[2]);
                        long swapFree = Long.parseLong(parts[3]);
                        
                        metrics.put("swapTotal", swapTotal);
                        metrics.put("swapUsed", swapUsed);
                        metrics.put("swapFree", swapFree);
                        if (swapTotal > 0) {
                            metrics.put("swapUsedPercent", (double) swapUsed / swapTotal * 100);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("메모리 메트릭 파싱 실패", e);
        }
        
        return metrics;
    }
    
    /**
     * df -h 명령어 결과 파싱
     * 
     * @param output df -h 명령어 출력
     * @return 파싱된 디스크 메트릭 데이터 리스트
     */
    public Map<String, Map<String, Object>> parseDiskUsage(String output) {
        Map<String, Map<String, Object>> diskMetrics = new HashMap<>();
        
        try {
            String[] lines = output.split("\n");
            // 첫 번째 줄은 헤더이므로 건너뛰기
            for (int i = 1; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.isEmpty()) continue;
                
                String[] parts = line.split("\\s+");
                if (parts.length >= 6) {
                    String filesystem = parts[0];
                    String size = parts[1];
                    String used = parts[2];
                    String avail = parts[3];
                    String usePercent = parts[4].replace("%", "");
                    String mountedOn = parts[5];
                    
                    Map<String, Object> diskInfo = new HashMap<>();
                    diskInfo.put("filesystem", filesystem);
                    diskInfo.put("size", size);
                    diskInfo.put("used", used);
                    diskInfo.put("avail", avail);
                    diskInfo.put("usePercent", Double.parseDouble(usePercent));
                    diskInfo.put("mountedOn", mountedOn);
                    
                    diskMetrics.put(mountedOn, diskInfo);
                }
            }
        } catch (Exception e) {
            log.error("디스크 메트릭 파싱 실패", e);
        }
        
        return diskMetrics;
    }
    
    /**
     * CPU 사용률 파싱 (top 또는 /proc/stat 사용)
     * 
     * @param output top 명령어 출력 또는 /proc/stat 내용
     * @return CPU 사용률 (퍼센트)
     */
    public BigDecimal parseCpuUsage(String output) {
        try {
            // top 명령어 출력에서 CPU 사용률 추출
            Pattern pattern = Pattern.compile("%Cpu\\(s\\):\\s+(\\d+\\.\\d+)%us");
            Matcher matcher = pattern.matcher(output);
            if (matcher.find()) {
                return new BigDecimal(matcher.group(1));
            }
            
            // /proc/stat 형식 파싱
            if (output.contains("cpu ")) {
                String[] lines = output.split("\n");
                for (String line : lines) {
                    if (line.startsWith("cpu ")) {
                        String[] parts = line.trim().split("\\s+");
                        if (parts.length >= 8) {
                            long user = Long.parseLong(parts[1]);
                            long nice = Long.parseLong(parts[2]);
                            long system = Long.parseLong(parts[3]);
                            long idle = Long.parseLong(parts[4]);
                            long iowait = Long.parseLong(parts[5]);
                            long irq = Long.parseLong(parts[6]);
                            long softirq = Long.parseLong(parts[7]);
                            
                            long total = user + nice + system + idle + iowait + irq + softirq;
                            long used = user + nice + system;
                            
                            if (total > 0) {
                                double usage = (double) used / total * 100;
                                return new BigDecimal(usage).setScale(2, RoundingMode.HALF_UP);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("CPU 사용률 파싱 실패", e);
        }
        
        return BigDecimal.ZERO;
    }
    
    /**
     * 숫자 문자열을 BigDecimal로 변환 (K, M, G, T 단위 지원)
     */
    public BigDecimal parseSize(String sizeStr) {
        if (sizeStr == null || sizeStr.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        sizeStr = sizeStr.trim().toUpperCase();
        try {
            if (sizeStr.endsWith("K")) {
                return new BigDecimal(sizeStr.substring(0, sizeStr.length() - 1))
                    .multiply(new BigDecimal("1024"));
            } else if (sizeStr.endsWith("M")) {
                return new BigDecimal(sizeStr.substring(0, sizeStr.length() - 1))
                    .multiply(new BigDecimal("1048576"));
            } else if (sizeStr.endsWith("G")) {
                return new BigDecimal(sizeStr.substring(0, sizeStr.length() - 1))
                    .multiply(new BigDecimal("1073741824"));
            } else if (sizeStr.endsWith("T")) {
                return new BigDecimal(sizeStr.substring(0, sizeStr.length() - 1))
                    .multiply(new BigDecimal("1099511627776"));
            } else {
                return new BigDecimal(sizeStr);
            }
        } catch (NumberFormatException e) {
            log.warn("크기 파싱 실패: {}", sizeStr);
            return BigDecimal.ZERO;
        }
    }
}

