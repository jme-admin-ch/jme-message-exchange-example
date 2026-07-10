package ch.admin.bit.jeap.jme.messageexchangeservice.perftests;

import ch.admin.bit.jeap.messageexchange.domain.objectstore.BucketType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Slf4j
public class JdbcPerfTestsRepository {

    private static final int BATCH_SIZE = 1000;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    private static final String MESSAGE_ID = "messageId";
    private static final String BP_ID = "bpId";
    private static final String CONTENT_LENGTH = "contentLength";
    private static final String CREATED_AT = "createdAt";

    private static final String MESSAGE_TYPE = "messageType";
    private static final String CONTENT_TYPE = "contentType";
    private static final String DATE_PUBLISHED = "datePublished";
    private static final String TOPIC_NAME = "topicName";

    private final S3PerfTestsRepository s3PerfTestsRepository;

    @Transactional
    public void resetDb() {
        jdbcTemplate.getJdbcTemplate().execute("TRUNCATE TABLE inbound_message");
        log.info("Table inbound_message truncated");

        jdbcTemplate.getJdbcTemplate().execute("TRUNCATE TABLE b2bhub_db_table");
        log.info("Table b2bhub_db_table truncated");
    }

    @Transactional(readOnly = true)
    public long countObjects(String tableName) {
        return jdbcTemplate.getJdbcTemplate().queryForObject(
                "SELECT COUNT(*) FROM " + tableName,
                Long.class);
    }

    public void fillInboundMessageTable(int count, List<String> testBpIds) {
        Random rand = new Random();
        String insertSql = "INSERT INTO inbound_message(\"messageId\",\"bpId\",\"contentLength\",\"createdAt\") VALUES (:messageId,:bpId,:contentLength,:createdAt)";

        List<MapSqlParameterSource> batch = new ArrayList<>(BATCH_SIZE);

        for (int i = 0; i < count; i++) {
            int contentLength = 454;
            String messageId = UUID.randomUUID().toString();
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue(MESSAGE_ID, messageId)
                    .addValue(BP_ID, testBpIds.get(rand.nextInt(testBpIds.size())))
                    .addValue(CONTENT_LENGTH, contentLength)
                    .addValue(CREATED_AT, LocalDateTime.now());

            s3PerfTestsRepository.storeObject(BucketType.PARTNER, messageId);

            batch.add(params);

            if (batch.size() == BATCH_SIZE || i == count - 1) {
                List<MapSqlParameterSource> currentBatch = new ArrayList<>(batch);
                transactionTemplate.executeWithoutResult(status ->
                        jdbcTemplate.batchUpdate(
                                insertSql,
                                currentBatch.toArray(new SqlParameterSource[0]))
                );

                batch.clear();
            }
        }
    }

    public void fillOutboundMessageTable(int count, List<String> testBpIds) {
        Random rand = new Random();
        String insertSql = "INSERT INTO b2bhub_db_table(\"messageId\",\"bpId\",\"topicName\",\"messageType\",\"datePublished\",\"contentType\") VALUES (:messageId,:bpId,:topicName,:messageType,:datePublished,:contentType)";

        List<MapSqlParameterSource> batch = new ArrayList<>(BATCH_SIZE);

        for (int i = 0; i < count; i++) {
            String messageId = UUID.randomUUID().toString();
            String bpId = testBpIds.get(rand.nextInt(testBpIds.size()));
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue(MESSAGE_ID, messageId)
                    .addValue(BP_ID, bpId)
                    .addValue(MESSAGE_TYPE, "b2b-lup-fill")
                    .addValue(DATE_PUBLISHED, LocalDateTime.now())
                    .addValue(TOPIC_NAME, "b2b-lup-fill")
                    .addValue(CONTENT_TYPE, "application/xml");

            s3PerfTestsRepository.storeObject(BucketType.INTERNAL, bpId + "/" + messageId);

            batch.add(params);

            if (batch.size() == BATCH_SIZE || i == count - 1) {
                List<MapSqlParameterSource> currentBatch = new ArrayList<>(batch);
                transactionTemplate.executeWithoutResult(status ->
                        jdbcTemplate.batchUpdate(
                                insertSql,
                                currentBatch.toArray(new SqlParameterSource[0]))
                );

                batch.clear();
            }
        }
    }

}
