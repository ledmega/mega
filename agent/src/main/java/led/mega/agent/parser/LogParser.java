package led.mega.agent.parser;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 로그 파일 파싱 모듈
 * Exception 로그를 찾아서 위아래 5줄씩 컨텍스트를 포함하여 반환합니다.
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

