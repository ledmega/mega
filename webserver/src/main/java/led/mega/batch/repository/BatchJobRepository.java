package led.mega.batch.repository;

import led.mega.batch.entity.BatchJob;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public interface BatchJobRepository extends ReactiveCrudRepository<BatchJob, String> {

    Flux<BatchJob> findByEnabledTrueOrderByJobNameAsc();

    Flux<BatchJob> findAllByOrderByCreatedAtAsc();

    Mono<BatchJob> findByJobName(String jobName);

    Mono<Boolean> existsByJobName(String jobName);

    @Modifying
    @Query("UPDATE batch_job SET last_run_at = :lastRunAt, last_run_status = :status, last_run_message = :message WHERE batch_job_id = :id")
    Mono<Integer> updateRunResult(String id, LocalDateTime lastRunAt, String status, String message);

    @Modifying
    @Query("UPDATE batch_job SET enabled = :enabled WHERE batch_job_id = :id")
    Mono<Integer> updateEnabled(String id, Boolean enabled);
}
