package ch.admin.bit.jeap.jme.messageexchangeservice.perftests;

import ch.admin.bit.jeap.messageexchange.domain.objectstore.BucketType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/perftests")
@RequiredArgsConstructor
@Slf4j
public class PerfTestsController {

    private final JdbcPerfTestsRepository jdbcPerfTestsRepository;
    private final S3PerfTestsRepository s3PerfTestsRepository;

    @Value("${jeap.mes.test-bp-ids}")
    private List<String> testBpIds;

    @PostMapping(path = "/reset")
    public String reset() {
        jdbcPerfTestsRepository.resetDb();
        s3PerfTestsRepository.deleteAllObjects();
        return "Tables truncated & Objects in S3 deleted";
    }

    @GetMapping(path = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public StatusResponse status() {
        return new StatusResponse(jdbcPerfTestsRepository.countObjects("inbound_message"),
                s3PerfTestsRepository.countObjects(BucketType.INTERNAL),
                jdbcPerfTestsRepository.countObjects("b2bhub_db_table"),
                s3PerfTestsRepository.countObjects(BucketType.PARTNER));
    }

    @PostMapping("/fill")
    @SneakyThrows
    public String fill(@RequestParam int count, @RequestParam String table) {
        switch (table) {
            case "inbound_message" -> jdbcPerfTestsRepository.fillInboundMessageTable(count, testBpIds);
            case "b2bhub_db_table" -> jdbcPerfTestsRepository.fillOutboundMessageTable(count, testBpIds);
            default -> throw new IllegalArgumentException("Unknown table: " + table);
        }
        return "Inserted " + count + " rows into " + table;
    }

    @PostConstruct
    void init() {
        log.info("Init perf tests controller with test bpIds: {}", testBpIds);
    }
}
