package com.example.shared.utils;


import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.Thread.sleep;

/**
 * Utility class for managing RabbitMQ connections and operations.
 * This class provides methods to connect to a RabbitMQ cluster, send and receive messages,
 * and manage message queues efficiently with failure handling.
 */
public class RabbitMQUtil {

    // List of RabbitMQ nodes for failover
    private static final String[] RABBITMQ_NODES = {
            "amqp://100.85.154.51",
            "amqp://100.76.110.20",
            "amqp://100.78.144.87"
    };

    // Queue names
    private static final String TRANSACTIONS_QUEUE = "transactionsQueue";
    private static final String CITATIONS_QUEUE = "citationsQueue";
    private static final String RECOMENDATION_QUEUE = "recommendationResponsesQueue";

    // RabbitMQ API details for managing policies
    private static final String RABBITMQ_API_URL = "http://127.0.0.1:15672/api/policies/%2F";
    private static final String RABBITMQ_USERNAME = "guest";
    private static final String RABBITMQ_PASSWORD = "guest";

    private Connection connection;
    private Channel channel;
    private final Map<String, Integer> failedNodes = new HashMap<>(); // שמירת מספר כשלים לכל צומת

    /**
     * Default constructor that attempts to connect to the RabbitMQ cluster.
     */
    public RabbitMQUtil() {
        connectToCluster();
    }

    /**
     * Constructor to connect to a specific RabbitMQ node.
     *
     * @param rabbitMQIP The RabbitMQ node URI to connect to.
     */
    public RabbitMQUtil(String rabbitMQIP) {
        connectToNode(rabbitMQIP);
    }

    /**
     * Attempts to connect to a specific RabbitMQ node.
     *
     * @param node The RabbitMQ node URI.
     * @return True if the connection is successful, otherwise false.
     */
    private boolean connectToNode(String node) {
        try {
            System.out.println(" Attempting connection to RabbitMQ node: " + node);

            ConnectionFactory factory = new ConnectionFactory();
            factory.setUri(node);
            factory.setUsername(RABBITMQ_USERNAME);
            factory.setPassword(RABBITMQ_PASSWORD);


            factory.setConnectionTimeout(500);
            factory.setHandshakeTimeout(1000);
            factory.setNetworkRecoveryInterval(1000);
            factory.setRequestedHeartbeat(5);
            factory.setAutomaticRecoveryEnabled(false);

            connection = factory.newConnection();
            channel = connection.createChannel();

            channel.basicQos(50); // Prefetch 50 הודעות

            System.out.println(" Connected to RabbitMQ node: " + node);
            failedNodes.put(node, 0); // אפסנו את הכשלונות בצומת הזה
            return true;

        } catch (Exception e) {
            int failures = failedNodes.getOrDefault(node, 0) + 1;
            failedNodes.put(node, failures);
            System.err.println(" Failed to connect to RabbitMQ node: " + node + " (Attempt " + failures + ")");

            // אם יש יותר מ-3 כשלונות, נשאיר את הצומת מחוץ לרשימה למשך 60 שניות
            if (failures >= 3) {
                System.err.println(" Node " + node + " marked as temporarily unavailable.");
                failedNodes.put(node, -1);
            }
            return false;
        }
    }

    /**
     * Attempts to connect to any available RabbitMQ node from the predefined cluster.
     * If all nodes fail, it throws an exception.
     */
    private void connectToCluster() {
        if (connection != null && connection.isOpen()) return;

        for (String node : RABBITMQ_NODES) {
            if (failedNodes.getOrDefault(node, 0) == -1) {
                System.err.println(" Skipping node " + node + " due to repeated failures.");
                continue;
            }
            if (connectToNode(node)) {
                initializeQueues();
                return;
            }
        }
        throw new RuntimeException("Failed to connect to any RabbitMQ node.");
    }

    /**
     * Initializes RabbitMQ queues with quorum queue configuration.
     */
    public void initializeQueues() {
        try {
            ensureChannelOpen();
            Map<String, Object> args = new HashMap<>();
            args.put("x-queue-type", "quorum");

            channel.queueDeclare(TRANSACTIONS_QUEUE, true, false, false, args);
            channel.queueDeclare(CITATIONS_QUEUE, true, false, false, args);
            channel.queueDeclare(RECOMENDATION_QUEUE, true, false, false, args);

            System.out.println(" Quorum queues initialized.");
        } catch (IOException e) {
            System.err.println(" Error initializing RabbitMQ queues: " + e.getMessage());
        }
    }


    /**
     * Sends a message to the specified RabbitMQ queue.
     *
     * @param queueName The name of the queue.
     * @param message   The message content.
     * @return True if the message is sent successfully, otherwise false.
     */
    public boolean sendMessage(String queueName, String message) {
        try {
            ensureChannelOpen();
            channel.basicPublish("", queueName, null, message.getBytes(StandardCharsets.UTF_8));
            System.out.println("Message sent to " + queueName + ": " + message);
            return true;
        } catch (Exception e) {
            System.err.println(" Error sending message to queue " + queueName + ": " + e.getMessage());
            reconnectFast();
            return false;
        }
    }

    /**
     * Fetches and republishes messages from a specified RabbitMQ queue.
     *
     * @param queueName The name of the queue.
     * @return A list of messages retrieved from the queue.
     */
    public List<String> fetchAndRepublishMessagesCitation(String queueName) {
        List<String> messages = new ArrayList<>();
        try {
            ensureChannelOpen();
            List<byte[]> messageBodies = new ArrayList<>();

            while (true) {
                var response = channel.basicGet(queueName, false);
                if (response == null) {
                    break;
                }

                String message = new String(response.getBody(), "UTF-8");
                messages.add(message);
                messageBodies.add(response.getBody());
                channel.basicAck(response.getEnvelope().getDeliveryTag(), false);
            }

            for (byte[] body : messageBodies) {
                channel.basicPublish("", queueName, null, body);
            }

            System.out.println("All messages fetched and republished to " + queueName);
            sleep(200);
        } catch (Exception e) {
            System.err.println("Error fetching and republishing messages from RabbitMQ queue " + queueName + ": " + e.getMessage());
            reconnect();
        }

        return messages;
    }

    /**
     * Ensures the RabbitMQ channel is open before performing operations.
     */
    private void ensureChannelOpen() {
        if (channel == null || !channel.isOpen()) reconnectFast();
    }

    /**
     * Performs a fast reconnection attempt.
     */
    private void reconnectFast() {
        new Thread(() -> {
            try {
                Thread.sleep(500);
                reconnect();
            } catch (InterruptedException e) {
                System.err.println("Quick reconnect error: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Closes the RabbitMQ connection and attempts to reconnect.
     */
    private void reconnect() {
        closeConnection();
        connectToCluster();
    }

    /**
     * Closes the RabbitMQ connection safely.
     */
    public void closeConnection() {
        try {
            if (channel != null && channel.isOpen()) channel.close();
            if (connection != null && connection.isOpen()) connection.close();
        } catch (IOException | TimeoutException e) {
            System.err.println(" Error closing RabbitMQ connection: " + e.getMessage());
        }
    }

    public String getConnectedServer() {
        try {
            if (connection != null && connection.isOpen()) {
                return "Connected to RabbitMQ node: " + connection.getAddress().getHostAddress();
            }
        } catch (Exception e) {
            System.err.println("Error retrieving RabbitMQ connection details: " + e.getMessage());
        }
        System.out.println("No active RabbitMQ connection.");
        return null;
    }

    public void purgeQueue(String queueName) {
        try {
            ensureChannelOpen();
            channel.queuePurge(queueName);
            System.out.println("Queue " + queueName + " has been purged successfully.");
        } catch (IOException e) {
            System.err.println("Error purging queue " + queueName + ": " + e.getMessage());
        }
    }


}