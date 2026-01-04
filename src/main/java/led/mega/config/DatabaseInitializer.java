package led.mega.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
@Order(1) // 가장 먼저 실행되도록 설정
public class DatabaseInitializer implements ApplicationRunner {

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    private static final String DB_NAME = "ledmega";
    // 데이터베이스명 없이 연결 (포트만 지정) - 모든 사용자가 접근 가능
    private static final String BASE_URL = "jdbc:mariadb://localhost:3306/?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Seoul";
    private static final String SCHEMA_SQL_FILE = "classpath:sql/schema.sql";

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("=== 데이터베이스 초기화 데몬 시작 ===");
        
        try {
            // 1. 데이터베이스 존재 여부 확인 및 생성
            initializeDatabase();
            
            // 2. 테이블 존재 여부 확인 및 생성
            initializeTables();
            
            log.info("=== 데이터베이스 초기화 데몬 완료 ===");
        } catch (Exception e) {
            log.error("데이터베이스 초기화 실패: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 데이터베이스가 없으면 생성
     */
    private void initializeDatabase() {
        log.info("데이터베이스 '{}' 존재 여부 확인 중...", DB_NAME);
        
        try (Connection conn = DriverManager.getConnection(BASE_URL, username, password);
             Statement stmt = conn.createStatement()) {

            // INFORMATION_SCHEMA를 사용하여 데이터베이스 존재 여부 확인 (어떤 DB에 연결해도 접근 가능)
            String checkDbSql = "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = '" + DB_NAME + "'";
            ResultSet rs = stmt.executeQuery(checkDbSql);

            if (!rs.next()) {
                // 데이터베이스가 없으면 생성
                log.info("데이터베이스 '{}'가 없습니다. 생성합니다...", DB_NAME);
                String createDbSql = "CREATE DATABASE IF NOT EXISTS " + DB_NAME + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci";
                stmt.executeUpdate(createDbSql);
                log.info("✓ 데이터베이스 '{}' 생성 완료!", DB_NAME);
            } else {
                log.info("✓ 데이터베이스 '{}'가 이미 존재합니다.", DB_NAME);
            }

        } catch (Exception e) {
            log.error("✗ 데이터베이스 초기화 중 오류 발생: {}", e.getMessage(), e);
            // 데이터베이스 생성 권한이 없을 수 있으므로, 경고만 출력하고 계속 진행
            log.warn("데이터베이스 생성에 실패했습니다. 이미 존재하거나 권한이 필요할 수 있습니다. 계속 진행합니다...");
        }
    }

    /**
     * 테이블이 없으면 생성 (하나의 SQL 파일에서 모든 CREATE 문 읽기)
     */
    private void initializeTables() {
        log.info("SQL 스키마 파일을 읽는 중...");
        
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource schemaResource;
        
        try {
            schemaResource = resolver.getResource(SCHEMA_SQL_FILE);
            if (!schemaResource.exists()) {
                log.warn("스키마 파일을 찾을 수 없습니다: {}. 테이블 초기화를 건너뜁니다.", SCHEMA_SQL_FILE);
                return;
            }
        } catch (Exception e) {
            log.error("스키마 파일 로드 실패: {}", e.getMessage(), e);
            return;
        }
        
        // SQL 파일에서 모든 CREATE TABLE 문 파싱
        String schemaSql = loadSchemaSql(schemaResource);
        List<TableCreateStatement> createStatements = parseCreateTableStatements(schemaSql);
        
        if (createStatements.isEmpty()) {
            log.warn("CREATE TABLE 문을 찾을 수 없습니다. schema.sql 파일을 확인하세요.");
            return;
        }
        
        log.info("총 {}개의 테이블을 초기화합니다.", createStatements.size());
        
        String dbUrl = datasourceUrl.split("\\?")[0]; // 쿼리 파라미터 제거
        
        try (Connection conn = DriverManager.getConnection(dbUrl, username, password);
             Statement stmt = conn.createStatement()) {

            for (TableCreateStatement createStmt : createStatements) {
                initializeTable(stmt, createStmt);
            }

        } catch (Exception e) {
            log.error("✗ 테이블 초기화 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("테이블 초기화 실패", e);
        }
    }

    /**
     * SQL 파일 전체 읽기
     */
    private String loadSchemaSql(Resource resource) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            
            return reader.lines()
                    .collect(Collectors.joining("\n"));
                    
        } catch (Exception e) {
            log.error("SQL 파일 읽기 실패: {}", resource.getFilename(), e);
            throw new RuntimeException("SQL 파일 읽기 실패: " + resource.getFilename(), e);
        }
    }

    /**
     * SQL 파일에서 CREATE TABLE 문들을 파싱하여 추출
     */
    private List<TableCreateStatement> parseCreateTableStatements(String sql) {
        List<TableCreateStatement> statements = new ArrayList<>();
        
        // 먼저 주석 제거
        String cleanedSql = sql.lines()
                .filter(line -> !line.trim().startsWith("--"))
                .filter(line -> !line.trim().isEmpty())
                .collect(Collectors.joining("\n"));
        
        log.debug("주석 제거 후 SQL 길이: {} 문자", cleanedSql.length());
        
        // CREATE TABLE 문을 찾기 위한 정규식 (세미콜론까지 포함)
        // CREATE TABLE ... ; 패턴을 찾음
        Pattern createTablePattern = Pattern.compile(
            "(?i)(CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?[`']?([\\w]+)[`']?[^;]*;)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        Matcher matcher = createTablePattern.matcher(cleanedSql);
        
        while (matcher.find()) {
            String fullStatement = matcher.group(1).trim();
            String tableName = matcher.group(2);
            
            statements.add(new TableCreateStatement(tableName, fullStatement));
            log.info("CREATE TABLE 문 발견: 테이블명 = {}", tableName);
        }
        
        if (statements.isEmpty()) {
            log.warn("CREATE TABLE 문을 찾을 수 없습니다.");
            log.warn("파싱된 SQL 내용 (처음 500자): {}", 
                    cleanedSql.length() > 0 ? cleanedSql.substring(0, Math.min(500, cleanedSql.length())) : "(비어있음)");
        }
        
        return statements;
    }

    /**
     * 개별 테이블 초기화
     */
    private void initializeTable(Statement stmt, TableCreateStatement createStmt) {
        String tableName = createStmt.getTableName();
        log.info("테이블 '{}' 존재 여부 확인 중...", tableName);
        
        try {
            // 테이블 존재 여부 확인
            String checkTableSql = "SELECT COUNT(*) as cnt FROM INFORMATION_SCHEMA.TABLES " +
                    "WHERE TABLE_SCHEMA = '" + DB_NAME + "' AND TABLE_NAME = '" + tableName + "'";
            ResultSet rs = stmt.executeQuery(checkTableSql);
            
            rs.next();
            int tableCount = rs.getInt("cnt");

            if (tableCount == 0) {
                // 테이블이 없으면 CREATE TABLE 문 실행
                log.info("테이블 '{}'가 없습니다. 생성합니다...", tableName);
                stmt.executeUpdate(createStmt.getCreateStatement());
                log.info("✓ 테이블 '{}' 생성 완료!", tableName);
            } else {
                log.info("✓ 테이블 '{}'가 이미 존재합니다.", tableName);
            }

        } catch (Exception e) {
            log.error("✗ 테이블 '{}' 초기화 중 오류 발생: {}", tableName, e.getMessage(), e);
            throw new RuntimeException("테이블 '" + tableName + "' 초기화 실패", e);
        }
    }

    /**
     * CREATE TABLE 문 정보를 담는 내부 클래스
     */
    private static class TableCreateStatement {
        private final String tableName;
        private final String createStatement;

        public TableCreateStatement(String tableName, String createStatement) {
            this.tableName = tableName;
            this.createStatement = createStatement;
        }

        public String getTableName() {
            return tableName;
        }

        public String getCreateStatement() {
            return createStatement;
        }
    }
}

