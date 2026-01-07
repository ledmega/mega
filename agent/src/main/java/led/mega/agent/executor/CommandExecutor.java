package led.mega.agent.executor;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 리눅스 명령어 실행 모듈
 */
@Slf4j
public class CommandExecutor {
    
    /**
     * 명령어 실행 및 결과 반환
     * 
     * @param command 실행할 명령어 (예: "free -m")
     * @return 명령어 실행 결과 (줄별 리스트)
     */
    public CommandResult execute(String command) {
        return execute(command.split("\\s+"));
    }
    
    /**
     * 명령어 실행 및 결과 반환
     * 
     * @param commandParts 명령어를 공백으로 분리한 배열 (예: ["free", "-m"])
     * @return 명령어 실행 결과
     */
    public CommandResult execute(String[] commandParts) {
        List<String> output = new ArrayList<>();
        List<String> error = new ArrayList<>();
        int exitCode = -1;
        
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(commandParts);
            processBuilder.redirectErrorStream(false);
            
            Process process = processBuilder.start();
            
            // 표준 출력 읽기
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.add(line);
                }
            }
            
            // 에러 출력 읽기
            try (BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = errorReader.readLine()) != null) {
                    error.add(line);
                }
            }
            
            exitCode = process.waitFor();
            
            if (exitCode != 0) {
                log.warn("명령어 실행 실패: {} (exit code: {})", String.join(" ", commandParts), exitCode);
                if (!error.isEmpty()) {
                    log.warn("에러 출력: {}", String.join("\n", error));
                }
            }
            
        } catch (Exception e) {
            log.error("명령어 실행 중 오류 발생: {}", String.join(" ", commandParts), e);
            error.add("명령어 실행 중 오류: " + e.getMessage());
        }
        
        return new CommandResult(exitCode, output, error);
    }
    
    /**
     * 명령어 실행 결과를 단일 문자열로 반환
     */
    public String executeToString(String command) {
        CommandResult result = execute(command);
        return String.join("\n", result.getOutput());
    }
    
    /**
     * 명령어 실행 결과 클래스
     */
    public static class CommandResult {
        private final int exitCode;
        private final List<String> output;
        private final List<String> error;
        
        public CommandResult(int exitCode, List<String> output, List<String> error) {
            this.exitCode = exitCode;
            this.output = output;
            this.error = error;
        }
        
        public int getExitCode() {
            return exitCode;
        }
        
        public List<String> getOutput() {
            return output;
        }
        
        public List<String> getError() {
            return error;
        }
        
        public boolean isSuccess() {
            return exitCode == 0;
        }
        
        public String getOutputAsString() {
            return String.join("\n", output);
        }
        
        public String getErrorAsString() {
            return String.join("\n", error);
        }
    }
}

