package ch.admin.bit.jeap.jme.messageexchangeservice.perftests;

import ch.admin.bit.jeap.messageexchange.domain.MessageContent;
import ch.admin.bit.jeap.messageexchange.domain.objectstore.BucketType;
import ch.admin.bit.jeap.messageexchange.domain.objectstore.ObjectStore;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

import java.io.ByteArrayInputStream;
import java.util.List;

@Repository
@RequiredArgsConstructor
@Slf4j
public class S3PerfTestsRepository {

    @Value("${jeap.messageexchange.objectstorage.connection.bucket-name-partner}")
    private String bucketNamePartner;

    @Value("${jeap.messageexchange.objectstorage.connection.bucket-name-internal}")
    private String bucketNameInternal;

    private final ObjectStore objectStore;
    private final S3Client s3Client;

    @SneakyThrows
    public void storeObject(BucketType bucketType, String objectKey) {
        objectStore.storeMessage(bucketType,
                objectKey,
                new MessageContent(new ByteArrayInputStream("Test content".getBytes()), "Test content".getBytes().length),
                "application/xml");
    }

    public void deleteAllObjects() {
        deleteAllObjects(bucketNamePartner);
        deleteAllObjects(bucketNameInternal);
    }

    private void deleteAllObjects(String bucketName) {
        ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .build();

        ListObjectsV2Iterable pages = s3Client.listObjectsV2Paginator(listRequest);

        for (ListObjectsV2Response page : pages) {
            if (page.contents().isEmpty()) {
                continue;
            }

            List<ObjectIdentifier> objectsToDelete = page.contents().stream()
                    .map(obj -> ObjectIdentifier.builder().key(obj.key()).build())
                    .toList();

            DeleteObjectsRequest deleteRequest = DeleteObjectsRequest.builder()
                    .bucket(bucketName)
                    .delete(Delete.builder()
                            .objects(objectsToDelete)
                            .build())
                    .build();

            s3Client.deleteObjects(deleteRequest);
        }
    }

    public long countObjects(BucketType bucketType) {
        String bucketName = BucketType.PARTNER.equals(bucketType) ? bucketNameInternal : bucketNamePartner;
        return s3Client.listObjectsV2Paginator(r -> r.bucket(bucketName))
                .stream()
                .mapToLong(r -> r.contents().size())
                .sum();
    }

}
