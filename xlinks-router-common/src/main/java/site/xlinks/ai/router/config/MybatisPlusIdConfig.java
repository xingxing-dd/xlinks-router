package site.xlinks.ai.router.config;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 自定义紧凑型 Long ID 生成器，替代默认 ASSIGN_ID（雪花）以缩短长度。
 *
 * 结构：10位秒级时间戳 + 2位机器码 + 4位序列号（每秒每实例 10000 个）
 */
@Configuration
@Slf4j
public class MybatisPlusIdConfig {

    @Bean
    public IdentifierGenerator identifierGenerator(
            @Value("${xlinks.id.machine-id:#{null}}") Integer configuredMachineId,
            Environment environment
    ) {
        int machineId = resolveMachineId(configuredMachineId, environment);
        return new CompactLongIdentifierGenerator(machineId);
    }

    private int resolveMachineId(Integer configuredMachineId, Environment environment) {
        if (configuredMachineId != null) {
            validateMachineId(configuredMachineId);
            log.info("Using configured xlinks.id.machine-id={}", configuredMachineId);
            return configuredMachineId;
        }
        String appName = environment.getProperty("spring.application.name", "xlinks-router");
        String serverPort = environment.getProperty("server.port", "0");
        String hostName = resolveHostName();
        String process = ManagementFactory.getRuntimeMXBean().getName();
        String fingerprint = appName + "|" + serverPort + "|" + hostName + "|" + process;
        int machineId = Math.floorMod(fingerprint.hashCode(), CompactLongIdentifierGenerator.MAX_MACHINE_ID + 1);
        log.warn("xlinks.id.machine-id is not configured, auto-resolved machineId={} from fingerprint(appName={}, port={}, host={}, process={}). "
                        + "For multi-instance deployment, configure unique xlinks.id.machine-id to avoid collisions.",
                machineId, appName, serverPort, hostName, process);
        return machineId;
    }

    private String resolveHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception ignored) {
            return "unknown-host";
        }
    }

    private void validateMachineId(int machineId) {
        if (machineId < 0 || machineId > CompactLongIdentifierGenerator.MAX_MACHINE_ID) {
            throw new IllegalArgumentException("xlinks.id.machine-id must be between 0 and 99");
        }
    }

    private static final class CompactLongIdentifierGenerator implements IdentifierGenerator {

        private static final int MAX_MACHINE_ID = 99;
        private static final int MAX_SEQUENCE = 9999;

        private final int machineId;
        private final AtomicInteger sequence = new AtomicInteger(0);
        private volatile long lastSecond = -1L;

        private CompactLongIdentifierGenerator(int machineId) {
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
