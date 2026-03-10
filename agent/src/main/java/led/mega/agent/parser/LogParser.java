package led.mega.agent.parser;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 로그 파일 파싱 모듈
 * Exception 로그를 찾아서 위아래 5줄씩 컨텍스트를 포함하여 반환합니다.
 * 또한 로그 키워드 Tail 모니터링을 지원합니다 (새로 추가된 라인만 처리).
 */
@Slf4j
public class LogParser {
    
    // Exception 패턴 (Java, Python, Node.js 등 다양한 언어 지원)
    private static final Pattern EXCEPTION_PATTERN = Pattern.compile(
        "(?i)(exception|error|fatal|panic|traceback|stack trace)",
        Pattern.CASE_INSENSITIVE
    );
    
    // Java Exception 패턴
    private static final Pattern JAVA_EXCEPTION_PATTERN = Pattern.compile(
        "^([a-zA-Z][a-zA-Z0-9_.]*Exception|Error|Throwable)(:.*)?$"
    );

    /**
     * 파일별 마지막으로 읽은 오프셋(byte position)을 추적합니다.
     * key: 파일 절대 경로, value: RandomAccessFile 기준 마지막 읽기 위치
     */
    private final Map<String, Long> fileOffsets = new HashMap<>();

    // -----------------------------------------------------------------------
    // P2: 키워드 Tail 모니터링
    // -----------------------------------------------------------------------

    /**
     * 로그 파일을 tail 방식으로 읽어 새로 추가된 라인에서 키워드를 검색합니다.
     * 이전에 읽은 위치(오프셋)를 기억하여 새 라인만 처리합니다.
     * 파일이 로테이션(재생성)된 경우 오프셋을 0으로 리셋합니다.
     *
     * @param logFilePath 모니터링할 로그 파일 경로
     * @param keywordsCsv 감시할 키워드 CSV (예: "Error,404,Exception,WARN")
     * @return 키워드가 포함된 라인에서 생성된 ExceptionInfo 리스트
     */
    public List<ExceptionInfo> tailAndMatchKeywords(String logFilePath, String keywordsCsv) {
        List<ExceptionInfo> results = new ArrayList<>();

        if (logFilePath == null || logFilePath.isBlank()) return results;
        if (keywordsCsv == null || keywordsCsv.isBlank()) return results;

        // 키워드 목록 파싱 (소문자로 정규화해서 대소문자 무관 매칭)
        List<String> keywords = Arrays.stream(keywordsCsv.split(","))
                .map(String::trim)
                .filter(k -> !k.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toList());

        if (keywords.isEmpty()) return results;

        File logFile = new File(logFilePath);
        if (!logFile.exists() || !logFile.isFile()) {
            log.warn("[Tail] 로그 파일이 존재하지 않습니다: {}", logFilePath);
            // 파일이 없으면 오프셋도 초기화
            fileOffsets.remove(logFilePath);
            return results;
        }

        long currentFileSize = logFile.length();
        long lastOffset = fileOffsets.getOrDefault(logFilePath, 0L);

        // 파일이 로테이션(재생성)되어 크기가 줄었으면 처음부터 읽기
        if (currentFileSize < lastOffset) {
            log.info("[Tail] 로그 파일 로테이션 감지, 오프셋 리셋: {}", logFilePath);
            lastOffset = 0L;
        }

        // 새로 추가된 내용이 없으면 스킵
        if (currentFileSize == lastOffset) {
            log.debug("[Tail] 새 로그 없음: {}", logFilePath);
            return results;
        }

        // 마지막 오프셋부터 파일 읽기
        try (RandomAccessFile raf = new RandomAccessFile(logFile, "r")) {
            raf.seek(lastOffset);

            String line;
            List<String> newLines = new ArrayList<>();
            while ((line = raf.readLine()) != null) {
                // readLine()은 ISO-8859-1로 읽으므로 UTF-8로 변환
                String utf8Line = new String(line.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
                newLines.add(utf8Line);
            }

            // 오프셋 업데이트
            fileOffsets.put(logFilePath, raf.getFilePointer());

            log.debug("[Tail] {}줄 신규 라인 읽음: {}", newLines.size(), logFilePath);

            // 키워드 매칭
            for (int i = 0; i < newLines.size(); i++) {
                String newLine = newLines.get(i);
                String lowerLine = newLine.toLowerCase();

                String matchedKeyword = keywords.stream()
                        .filter(lowerLine::contains)
                        .findFirst()
                        .orElse(null);

                if (matchedKeyword != null) {
                    // 앞뒤 3줄 컨텍스트 수집
                    String contextBefore = newLines.subList(Math.max(0, i - 3), i)
                            .stream().collect(Collectors.joining("\n"));
                    String contextAfter = newLines.subList(i + 1, Math.min(newLines.size(), i + 4))
                            .stream().collect(Collectors.joining("\n"));

                    results.add(new ExceptionInfo(
                            logFilePath,
                            "KeywordAlert[" + matchedKeyword + "]",   // exceptionType
                            newLine.trim(),                             // exceptionMessage = 해당 라인 전체
                            contextBefore,
                            contextAfter,
                            "",                                         // stackTrace 없음
                            newLine
                    ));
                    log.info("[Tail] 키워드 감지 '{}': {}", matchedKeyword, newLine.trim());
                }
            }

        } catch (IOException e) {
            log.error("[Tail] 로그 파일 읽기 실패: {}", logFilePath, e);
        }

        return results;
    }

    /**
     * 특정 파일의 tail 오프셋을 초기화합니다.
     * (config가 삭제되거나 logPath가 변경될 때 호출)
     */
    public void resetOffset(String logFilePath) {
        fileOffsets.remove(logFilePath);
        log.info("[Tail] 오프셋 초기화: {}", logFilePath);
    }


    
    /**
     * 로그 파일에서 Exception을 찾아서 파싱
     * 
     * @param logFilePath 로그 파일 경로
     * @return 파싱된 Exception 정보 리스트
     */
    public List<ExceptionInfo> parseExceptions(String logFilePath) {
        List<ExceptionInfo> exceptions = new ArrayList<>();
        
        File logFile = new File(logFilePath);
        if (!logFile.exists() || !logFile.isFile()) {
            log.warn("로그 파일이 존재하지 않거나 파일이 아닙니다: {}", logFilePath);
            return exceptions;
        }
        
        try {
            List<String> lines = Files.readAllLines(Paths.get(logFilePath), StandardCharsets.UTF_8);
            
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                
                // Exception 패턴 매칭
                if (isExceptionLine(line)) {
                    ExceptionInfo exceptionInfo = extractExceptionInfo(lines, i, logFilePath);
                    if (exceptionInfo != null) {
                        exceptions.add(exceptionInfo);
                        // 다음 Exception을 찾기 위해 스택 트레이스 건너뛰기
                        i = skipStackTrace(lines, i);
                    }
                }
            }
            
            log.info("로그 파일에서 {}개의 Exception을 찾았습니다: {}", exceptions.size(), logFilePath);
            
        } catch (IOException e) {
            log.error("로그 파일 읽기 실패: {}", logFilePath, e);
        }
        
        return exceptions;
    }
    
    /**
     * Exception 라인인지 확인
     */
    private boolean isExceptionLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            return false;
        }
        
        // Java Exception 패턴 확인
        Matcher javaMatcher = JAVA_EXCEPTION_PATTERN.matcher(line.trim());
        if (javaMatcher.find()) {
            return true;
        }
        
        // 일반 Exception 패턴 확인
        Matcher matcher = EXCEPTION_PATTERN.matcher(line);
        return matcher.find();
    }
    
    /**
     * Exception 정보 추출 (위아래 5줄 컨텍스트 포함)
     */
    private ExceptionInfo extractExceptionInfo(List<String> lines, int exceptionLineIndex, String logFilePath) {
        if (exceptionLineIndex < 0 || exceptionLineIndex >= lines.size()) {
            return null;
        }
        
        String exceptionLine = lines.get(exceptionLineIndex);
        
        // Exception 타입 추출
        String exceptionType = extractExceptionType(exceptionLine);
        
        // Exception 메시지 추출
        String exceptionMessage = extractExceptionMessage(exceptionLine);
        
        // 위 5줄 컨텍스트
        List<String> contextBefore = new ArrayList<>();
        int startIndex = Math.max(0, exceptionLineIndex - 5);
        for (int i = startIndex; i < exceptionLineIndex; i++) {
            contextBefore.add(lines.get(i));
        }
        
        // 아래 5줄 컨텍스트 및 스택 트레이스
        List<String> contextAfter = new ArrayList<>();
        List<String> stackTrace = new ArrayList<>();
        int endIndex = Math.min(lines.size(), exceptionLineIndex + 50); // 최대 50줄까지
        boolean inStackTrace = false;
        
        for (int i = exceptionLineIndex + 1; i < endIndex; i++) {
            String line = lines.get(i);
            
            // 스택 트레이스 시작 확인 (at, Caused by 등)
            if (line.trim().startsWith("at ") || 
                line.trim().startsWith("Caused by:") ||
                line.trim().matches("^\\s+at .*")) {
                inStackTrace = true;
                stackTrace.add(line);
            } else if (inStackTrace && (line.trim().isEmpty() || 
                       line.trim().startsWith("...") ||
                       isExceptionLine(line))) {
                // 스택 트레이스 종료 또는 새로운 Exception 시작
                break;
            } else if (inStackTrace) {
                stackTrace.add(line);
            } else {
                // 컨텍스트 (아래 5줄)
                if (contextAfter.size() < 5) {
                    contextAfter.add(line);
                } else {
                    break;
                }
            }
        }
        
        return new ExceptionInfo(
            logFilePath,
            exceptionType,
            exceptionMessage,
            String.join("\n", contextBefore),
            String.join("\n", contextAfter),
            String.join("\n", stackTrace),
            exceptionLine
        );
    }
    
    /**
     * Exception 타입 추출
     */
    private String extractExceptionType(String line) {
        Matcher matcher = JAVA_EXCEPTION_PATTERN.matcher(line.trim());
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        // 일반적인 패턴에서 추출
        int colonIndex = line.indexOf(':');
        if (colonIndex > 0) {
            String beforeColon = line.substring(0, colonIndex).trim();
            if (beforeColon.contains("Exception") || beforeColon.contains("Error")) {
                return beforeColon;
            }
        }
        
        return "UnknownException";
    }
    
    /**
     * Exception 메시지 추출
     */
    private String extractExceptionMessage(String line) {
        int colonIndex = line.indexOf(':');
        if (colonIndex > 0 && colonIndex < line.length() - 1) {
            return line.substring(colonIndex + 1).trim();
        }
        return line.trim();
    }
    
    /**
     * 스택 트레이스 건너뛰기
     */
    private int skipStackTrace(List<String> lines, int currentIndex) {
        int i = currentIndex + 1;
        while (i < lines.size()) {
            String line = lines.get(i).trim();
            
            // 스택 트레이스가 아닌 라인을 만나면 종료
            if (!line.startsWith("at ") && 
                !line.startsWith("Caused by:") &&
                !line.matches("^\\s+at .*") &&
                !line.isEmpty() &&
                !isExceptionLine(lines.get(i))) {
                break;
            }
            i++;
        }
        return i - 1;
    }
    
    /**
     * Exception 정보 클래스
     */
    public static class ExceptionInfo {
        private final String logFilePath;
        private final String exceptionType;
        private final String exceptionMessage;
        private final String contextBefore;
        private final String contextAfter;
        private final String fullStackTrace;
        private final String exceptionLine;
        
        public ExceptionInfo(String logFilePath, String exceptionType, String exceptionMessage,
                           String contextBefore, String contextAfter, String fullStackTrace,
                           String exceptionLine) {
            this.logFilePath = logFilePath;
            this.exceptionType = exceptionType;
            this.exceptionMessage = exceptionMessage;
            this.contextBefore = contextBefore;
            this.contextAfter = contextAfter;
            this.fullStackTrace = fullStackTrace;
            this.exceptionLine = exceptionLine;
        }
        
        public String getLogFilePath() { return logFilePath; }
        public String getExceptionType() { return exceptionType; }
        public String getExceptionMessage() { return exceptionMessage; }
        public String getContextBefore() { return contextBefore; }
        public String getContextAfter() { return contextAfter; }
        public String getFullStackTrace() { return fullStackTrace; }
        public String getExceptionLine() { return exceptionLine; }
    }
}

