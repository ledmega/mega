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
     * Ubuntu top 출력 형식: "%Cpu(s):  2.3 us,  1.0 sy, ..."
     *
     * @param output top 명령어 출력 또는 /proc/stat 내용
     * @return CPU 사용률 (퍼센트)
     */
    public BigDecimal parseCpuUsage(String output) {
        try {
            // Ubuntu top 현재 형식: "%Cpu(s):  2.3 us,  1.0 sy, ..."
            // 기존 '%us' 패턴은 틀린 형식으로 항상 0 반환 → 올바른 '숫자 us' 패턴으로 수정
            Pattern topPattern = Pattern.compile("%Cpu\\(s\\):\\s+([\\d.]+)\\s+us");
            Matcher topMatcher = topPattern.matcher(output);
            if (topMatcher.find()) {
                BigDecimal val = new BigDecimal(topMatcher.group(1)).setScale(2, RoundingMode.HALF_UP);
                log.debug("CPU 파싱 성공 (top us%): {}%", val);
                return val;
            }

            // top 구버전 형식: "Cpu(s):  2.3%us" 폴백
            Pattern topOldPattern = Pattern.compile("Cpu\\(s\\):\\s+([\\d.]+)%us");
            Matcher topOldMatcher = topOldPattern.matcher(output);
            if (topOldMatcher.find()) {
                BigDecimal val = new BigDecimal(topOldMatcher.group(1)).setScale(2, RoundingMode.HALF_UP);
                log.debug("CPU 파싱 성공 (top old %us): {}%", val);
                return val;
            }

            // /proc/stat 형식 파싱 (idle + iowait = 유휴 시간)
            if (output.contains("cpu ")) {
                String[] lines = output.split("\n");
                for (String line : lines) {
                    if (line.startsWith("cpu ")) {
                        String[] parts = line.trim().split("\\s+");
                        if (parts.length >= 8) {
                            long user    = Long.parseLong(parts[1]);
                            long nice    = Long.parseLong(parts[2]);
                            long system  = Long.parseLong(parts[3]);
                            long idle    = Long.parseLong(parts[4]);
                            long iowait  = Long.parseLong(parts[5]);
                            long irq     = Long.parseLong(parts[6]);
                            long softirq = Long.parseLong(parts[7]);

                            long totalIdle = idle + iowait;
                            long totalBusy = user + nice + system + irq + softirq;
                            long total     = totalIdle + totalBusy;

                            if (total > 0) {
                                double usage = (double) totalBusy / total * 100;
                                BigDecimal val = new BigDecimal(usage).setScale(2, RoundingMode.HALF_UP);
                                log.debug("CPU 파싱 성공 (/proc/stat): {}%", val);
                                return val;
                            }
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("CPU 사용률 파싱 실패", e);
        }

        // 파싱 실패 시 output 앞부분 로그 출력 (디버그용)
        log.warn("CPU 파싱 실패 - 0 반환. output 샘플: [{}]",
                output != null && output.length() > 150 ? output.substring(0, 150) : output);
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

