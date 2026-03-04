package led.mega.config;

// [NEW] R2dbcConfig - JPA의 Hibernate 설정을 대체하는 R2DBC 설정
//
// 기존 JPA:
//   @EnableJpaAuditing → @CreationTimestamp, @UpdateTimestamp (Hibernate)
//   spring.jpa.hibernate.ddl-auto=update
//
// [REACTIVE] R2DBC:
//   @EnableR2dbcAuditing → @CreatedDate, @LastModifiedDate (Spring Data)
//   DDL은 DatabaseInitializer에서 JDBC로 직접 처리

import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;

@Configuration
@EnableR2dbcAuditing  // [NEW] @CreatedDate, @LastModifiedDate 자동 채움 활성화
public class R2dbcConfig {
    // R2DBC ConnectionFactory, ConnectionPool 등은 application.properties 기반으로 자동 설정됨
    // 커스텀 타입 변환(Converter)이 필요할 경우 여기에 추가
}
