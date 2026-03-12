package led.mega.batch.task;

import led.mega.batch.entity.BatchJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 웹에서 직접 입력한 SQL들을 순차적으로 실행하는 범용 배치 테스크
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicSqlTask implements BatchTask {

    private final DatabaseClient databaseClient;

    @Override
    public String getJobType() {
        return "DYNAMIC_SQL_BATCH";
    }

    @Override
    public String getDisplayName() {
        return "사용자 정의 SQL 배치 (복합 쿼리 지원)";
    }

    @Override
    public Mono<String> execute(BatchJob job, LocalDateTime threshold) {
        String sqlContent = job.getJobConfig();
        if (sqlContent == null || sqlContent.trim().isEmpty()) {
            return Mono.just("실행할 SQL 내용이 없습니다.");
        }

        // 세미콜론(;) 기준으로 쿼리 분리
        String[] queries = sqlContent.split(";");
        
        return Flux.fromArray(queries)
                .map(String::trim)
                .filter(q -> !q.isEmpty())
                .flatMap(q -> {
                    log.info("[DynamicSql] 실행 쿼리: {}", q);
                    return databaseClient.sql(q).fetch().rowsUpdated()
                            .map(count -> "[" + q.substring(0, Math.min(q.length(), 20)) + "...] -> " + count + " rows");
                })
                .collectList()
                .map(results -> "SQL 배치 실행 완료: " + String.join(", ", results));
    }
}
