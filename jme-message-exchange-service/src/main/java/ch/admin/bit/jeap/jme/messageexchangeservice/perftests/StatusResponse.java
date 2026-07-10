package ch.admin.bit.jeap.jme.messageexchangeservice.perftests;

public record StatusResponse(
        long inboundDbTable,
        long inboundS3Bucket,
        long outboundDbTable,
        long outboundS3Bucket
) {
}
