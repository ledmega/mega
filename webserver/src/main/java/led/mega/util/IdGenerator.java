package led.mega.util;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 전역 유일 식별자 생성을 위한 유틸리티 클래스.
 * Prefix + 10자리 숫자를 사용하여 테이블별 고유 ID를 생성합니다.
 * 예: MBR0000000001
 */
public class IdGenerator {

    // 실제 운영 환경에서는 DB의 시퀀스나 Redis 등을 사용해야 하지만,
    // 현재는 메모리 기반으로 간단히 구현합니다.
    private static final AtomicLong counter = new AtomicLong(System.currentTimeMillis() % 10000000000L);

    public static String generate(String prefix) {
        long nextValue = counter.incrementAndGet();
        // 10자리 숫자로 패딩 (BTJ0000000001)
        return String.format("%s%010d", prefix.toUpperCase(), nextValue);
    }
    
    // Prefix 정의
    public static final String MEMBER = "MBR";
    public static final String AGENT = "AGT";
    public static final String TASK = "TSK";
    public static final String METRIC = "MET";
    public static final String EXCEPTION = "EXL";
    public static final String HEARTBEAT = "HBT";
    public static final String MENU = "MNU";
    public static final String CONFIG = "CFG";
    public static final String BATCH_JOB = "BTJ";
    public static final String SVC_METRIC = "SMT";
    
    public static final String CS_FAQ = "FAQ";
    public static final String CS_CONV = "CON";
    public static final String CS_MSG = "MSG";
    public static final String CS_INBOUND = "INB";
    public static final String CS_REPORT = "RPT";
}
