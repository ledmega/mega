package led.mega.batch.repository;

import led.mega.batch.entity.BatchJob;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public interface BatchJobRepository extends ReactiveCrudRepository<BatchJob, Long> {

    Flux<BatchJob> findByEnabledTrueOrderByJobNameAsc();

    Flux<BatchJob> findAllByOrderByCreatedAtAsc();

    Mono<BatchJob> findByJobName(String jobName);

    Mono<Boolean> existsByJobName(String jobName);

    @Modifying
    @Query("UPDATE batch_job SET last_run_at = :lastRunAt, last_run_status = :status, last_run_message = :message WHERE id = :id")
    Mono<Integer> updateRunResult(Long id, LocalDateTime lastRunAt, String status, String message);

    @Modifying
    @Query("UPDATE batch_job SET enabled = :enabled WHERE id = :id")
    Mono<Integer> updateEnabled(Long id, Boolean enabled);
}
