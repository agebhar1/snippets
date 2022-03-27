package com.github.agebhar1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SqsException;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.UUID;

public class App {

    // https://docs.aws.amazon.com/de_de/AWSSimpleQueueService/latest/SQSDeveloperGuide/welcome.html

    private final static String Queue = "";
    private final static Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) throws URISyntaxException {

        final var sqsClient = SqsClient.builder()
                .credentialsProvider(ProfileCredentialsProvider.create())
                .region(Region.EU_CENTRAL_1)
                .build();

        sendMessage(sqsClient, new URI(Queue), "Hello SQS! " + Instant.now());
        receiveMessages(sqsClient, new URI(Queue));
    }

    private static void sendMessage(final SqsClient sqsClient, final URI queue, final String message) {
        logger.info("Try to send message to SQS: {}.", queue);
        try {
            final var response = sqsClient.sendMessage(builder ->
                    builder
                            .queueUrl(queue.toString())
                            .messageBody(message)
                            .messageDeduplicationId(UUID.randomUUID().toString())
                            .messageGroupId("1")
            );
            logger.info("Response: {}", response);
        } catch (final SqsException e) {
            logger.error("Got Exception: {}", e.toString());
        }
    }

    private static void receiveMessages(final SqsClient sqsClient, final URI queue) {

        logger.info("Try to receive messages from SQS: {}.", queue);
        try {
            final var messages = sqsClient.receiveMessage(builder -> builder.queueUrl(queue.toString()).waitTimeSeconds(10).maxNumberOfMessages(10));
            logger.info("hasMessages {}", messages.hasMessages());
            messages.messages().forEach(message -> {
                logger.info("[{}] {}", message.messageId(), message.body());
                sqsClient.deleteMessage(builder -> builder.queueUrl(queue.toString()).receiptHandle(message.receiptHandle()));
            });
        } catch (final SqsException e) {
            logger.error("Got Exception: {}", e.toString());
        }
    }

}
