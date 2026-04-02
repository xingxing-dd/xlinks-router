package site.xlinks.ai.router.config;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 自定义紧凑型 Long ID 生成器，替代默认 ASSIGN_ID（雪花）以缩短长度。
 *
 * 结构：10位秒级时间戳 + 2位机器码 + 4位序列号（每秒每实例 10000 个）
 */
@Configuration
public class MybatisPlusIdConfig {

    @Bean
    public IdentifierGenerator identifierGenerator(
            @Value("${xlinks.id.machine-id:1}") int machineId
    ) {
        return new CompactLongIdentifierGenerator(machineId);
    }

    private static final class CompactLongIdentifierGenerator implements IdentifierGenerator {

        private static final int MAX_MACHINE_ID = 99;
        private static final int MAX_SEQUENCE = 9999;

        private final int machineId;
        private final AtomicInteger sequence = new AtomicInteger(0);
        private volatile long lastSecond = -1L;

        private CompactLongIdentifierGenerator(int machineId) {
            if (machineId < 0 || machineId > MAX_MACHINE_ID) {
                throw new IllegalArgumentException("xlinks.id.machine-id must be between 0 and 99");
            }
            this.machineId = machineId;
        }

        @Override
        public Number nextId(Object entity) {
            long second = Instant.now().getEpochSecond();
            int seq;

            synchronized (this) {
                if (second != lastSecond) {
                    lastSecond = second;
                    sequence.set(0);
                }

                seq = sequence.getAndIncrement();
                if (seq > MAX_SEQUENCE) {
                    do {
                        second = Instant.now().getEpochSecond();
                    } while (second <= lastSecond);

                    lastSecond = second;
                    sequence.set(0);
                    seq = sequence.getAndIncrement();
                }
            }

            return second * 1_000_000L + (long) machineId * 10_000 + seq;
        }
    }
}
