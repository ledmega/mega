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
    private static final String SCHEMA_SQL_FILE = "classpath:sql/schema.sql";

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("=== 데이터베이스 초기화 데모 시작 ===");
        
        try {
            // 1. 데이터베이스 존재 여부 확인 및 생성
            initializeDatabase();
            
            // 2. 테이블 존재 여부 확인 및 생성
            initializeTables();
            
            // 3. 기본 메뉴 데이터 초기화 (없으면 INSERT)
            initializeDefaultMenus();
            
            log.info("=== 데이터베이스 초기화 데모 완료 ===");
        } catch (Exception e) {
            log.error("데이터베이스 초기화 실패: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 데이터베이스가 없으면 생성
     * BASE_URL: datasourceUrl에서 DB명을 제거하고 포트만 남긴 URL 사용 (하드코딩 방지)
     */
    private void initializeDatabase() {
        log.info("데이터베이스 '{}' 존재 여부 확인 중...", DB_NAME);

        // jdbc:mariadb://host:port/dbname?params → jdbc:mariadb://host:port/?params
        String baseUrl = datasourceUrl
                .replaceFirst("/(\\w+)(\\?|$)", "/$2")  // DB명 제거
                .replaceFirst("//([^/]+)$", "//$1/");    // trailing slash 보장
        // 파라미터가 없는 경우 인코딩 옵션 추가
        if (!baseUrl.contains("?")) {
            baseUrl += "?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Seoul";
        }

        try (Connection conn = DriverManager.getConnection(baseUrl, username, password);
             Statement stmt = conn.createStatement()) {

            String checkDbSql = "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = '" + DB_NAME + "'";
            ResultSet rs = stmt.executeQuery(checkDbSql);

            if (!rs.next()) {
                log.info("데이터베이스 '{}'가 없습니다. 생성합니다...", DB_NAME);
                String createDbSql = "CREATE DATABASE IF NOT EXISTS " + DB_NAME + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci";
                stmt.executeUpdate(createDbSql);
                log.info("✓ 데이터베이스 '{}' 생성 완료!", DB_NAME);
            } else {
                log.info("✓ 데이터베이스 '{}'가 이미 존재합니다.", DB_NAME);
            }

        } catch (Exception e) {
            log.error("✗ 데이터베이스 초기화 중 오류 발생: {}", e.getMessage(), e);
            log.warn("데이터베이스 생성에 실패했습니다. 이미 존재하거나 권한이 필요할 수 있습니다. 계속 진행합니다...");
        }
    }

    /**
     * 기본 네비게이션 메뉴 데이터 시딩
     * menu 테이블이 비어 있으면 기본 메뉴를 자동 INSERT
     */
    private void initializeDefaultMenus() {
        log.info("기본 메뉴 데이터 확인 중...");
        String dbUrl = datasourceUrl.split("\\?")[0];

        try (Connection conn = DriverManager.getConnection(dbUrl, username, password);
             Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS cnt FROM menu");
            rs.next();
            int menuCount = rs.getInt("cnt");

            if (menuCount == 0) {
                log.info("메뉴가 없습니다. 기본 메뉴를 생성합니다...");
                String[] items = {
                    "('MNU0000000001', '대시보드', '/dashboard', 'chart-bar', 1, NULL, TRUE)",
                    "('MNU0000000002', '에이전트', '/agents', 'server', 2, NULL, TRUE)",
                    "('MNU0000000003', '서비스 관리', '/services', 'cog', 3, NULL, TRUE)",
                    "('MNU0000000004', '배치 스케줄러', '/scheduler', 'clock', 4, NULL, TRUE)",
                    "('MNU0000000005', '회원 관리', '/members', 'users', 5, 'ROLE_ADMIN', TRUE)",
                    "('MNU0000000006', '메뉴 관리', '/menu', 'list', 6, 'ROLE_ADMIN', TRUE)",
                    "('MNU0000000007', 'OS 설정 관리', '/os-configs', 'microchip', 7, NULL, TRUE)"
                };
                for (String item : items) {
                    String sql = "INSERT INTO menu (menu_id, name, url, icon, sort_order, required_role, is_enabled) VALUES " + item;
                    stmt.executeUpdate(sql);
                }
                log.info("✓ 기본 메뉴 {}개 생성 완료!", items.length);
            } else {
                // 서비스 관리 메뉴가 없으면 추가
                ResultSet rs2 = stmt.executeQuery("SELECT COUNT(*) AS cnt FROM menu WHERE url = '/services'");
                rs2.next();
                if (rs2.getInt("cnt") == 0) {
                    stmt.executeUpdate("INSERT INTO menu (menu_id, name, url, icon, sort_order, required_role, is_enabled) " +
                            "VALUES ('MNU0000000003', '서비스 관리', '/services', 'cog', 3, NULL, TRUE)");
                    log.info("✓ '서비스 관리' 메뉴 생성 완료!");
                }
                // 배치 스케줄러 메뉴가 없으면 추가
                ResultSet rs3 = stmt.executeQuery("SELECT COUNT(*) AS cnt FROM menu WHERE url = '/scheduler'");
                rs3.next();
                if (rs3.getInt("cnt") == 0) {
                    stmt.executeUpdate("INSERT INTO menu (menu_id, name, url, icon, sort_order, required_role, is_enabled) " +
                            "VALUES ('MNU0000000004', '배치 스케줄러', '/scheduler', 'clock', 4, NULL, TRUE)");
                    log.info("✓ '배치 스케줄러' 메뉴 생성 완료!");
                }
                // OS 설정 관리 메뉴가 없으면 추가
                ResultSet rs4 = stmt.executeQuery("SELECT COUNT(*) AS cnt FROM menu WHERE url = '/os-configs'");
                rs4.next();
                if (rs4.getInt("cnt") == 0) {
                    stmt.executeUpdate("INSERT INTO menu (menu_id, name, url, icon, sort_order, required_role, is_enabled) " +
                            "VALUES ('MNU0000000007', 'OS 설정 관리', '/os-configs', 'microchip', 7, NULL, TRUE)");
                    log.info("✓ 'OS 설정 관리' 메뉴 생성 완료!");
                } else {
                    log.info("✓ 메뉴 데이터가 이미 존재합니다.");
                }
            }

        } catch (Exception e) {
            log.error("✗ 기본 메뉴 초기화 중 오류: {}", e.getMessage(), e);
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

