package com.github.agebhar1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SqsException;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class App {

    // https://docs.aws.amazon.com/de_de/AWSSimpleQueueService/latest/SQSDeveloperGuide/welcome.html

    private final static String Queue = "";
    private final static Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) throws URISyntaxException {
        async(args);
    }

    public static void async(String[] args) throws URISyntaxException {

        final var asyncSqsClient = SqsAsyncClient.builder()
                .credentialsProvider(ProfileCredentialsProvider.create(""))
                .region(Region.EU_CENTRAL_1)
                .build();

        var message = "Hello SQS! " + Instant.now();

        sendMessage(asyncSqsClient, new URI(Queue), message);
        receiveMessages(asyncSqsClient, new URI(Queue));
    }

    public static void sync(String[] args) throws URISyntaxException {

        final var sqsClient = SqsClient.builder()
                .credentialsProvider(ProfileCredentialsProvider.create(""))
                .region(Region.EU_CENTRAL_1)
                .build();

        var message = "Hello SQS! " + Instant.now();

        sendMessage(sqsClient, new URI(Queue), message);
        receiveMessages(sqsClient, new URI(Queue));
    }

    private static void sendMessage(final SqsClient sqsClient, final URI queue, final String message) {
        logger.info("Try to send message async to SQS: {}.", queue);
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

    private static void sendMessage(final SqsAsyncClient sqsClient, final URI queue, final String message) {
        logger.info("Try to send message to SQS: {}.", queue);
        try {
            sqsClient
                    .sendMessage(builder ->
                            builder
                                    .queueUrl(queue.toString())
                                    .messageBody(message)
                                    .messageDeduplicationId(UUID.randomUUID().toString())
                                    .messageGroupId("1")
                    )
                    .whenComplete((response, error) -> logger.info("Response: {}, Error: {}", response, error))
                    .join();
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

    private static void receiveMessages(final SqsAsyncClient sqsClient, final URI queue) {
        logger.info("Try to receive messages async from SQS: {}.", queue);
        try {
            sqsClient
                    .receiveMessage(builder -> builder.queueUrl(queue.toString()).maxNumberOfMessages(10))
                    .whenComplete((response, error) ->
                            logger.info("completed receiveMessage(...), response: {}, error: {}", response, error)
                    )
                    .handle((messages, error) -> {
                        logger.info("hasMessages {}", messages.hasMessages());
                        return CompletableFuture.allOf(messages.messages().stream().map(message -> {
                                    logger.info("[{}] {}", message.messageId(), message.body());
                                    return sqsClient
                                            .deleteMessage(builder -> builder.queueUrl(queue.toString()).receiptHandle(message.receiptHandle()))
                                            .whenComplete((response, error2) ->
                                                    logger.info("[{}] deleted, response: {}, error: {}", message.messageId(), response, error2)
                                            );
                                }).toList().toArray(new CompletableFuture[0]))
                                .whenComplete((response, error3) ->
                                        logger.info("completed allOf(...) response: {}, error: {}", response, error3)
                                )
                                .join();
                    })
                    .join();

        } catch (final SqsException e) {
            logger.error("Got Exception: {}", e.toString());
        }
    }

}
