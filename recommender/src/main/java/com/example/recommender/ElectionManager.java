package com.example.recommender;

import com.example.shared.utils.RabbitMQUtil;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Handles Leader Election based on active connected nodes.
 */
public class ElectionManager {
    private final List<String> nodes;
    private String leader;



    /**
     * Constructor for the ElectionManager.
     * It initializes the list of nodes and elects a leader.
     *
     * @param nodes The list of RabbitMQ nodes (URIs).
     */
    public ElectionManager(List<String> nodes) {
        this.nodes = nodes;
        electLeader();
    }


    /**
     * Elects a leader from the list of active nodes.
     */
    public void electLeader() {
        List<String> activeNodes = nodes.stream()
                .filter(this::isNodeConnected) // מסנן צמתים מחוברים בלבד
                .collect(Collectors.toList());

        if (activeNodes.isEmpty()) {
            System.err.println("❌ No active RabbitMQ nodes available for leader election!");
            leader = null; // אין מנהיג אם אין חיבור
            return;
        }

        Random rand = new Random();
        this.leader = activeNodes.get(rand.nextInt(activeNodes.size()));
        System.out.println("🏆 New Leader elected: " + leader);
    }

    /**
     * Initializes RabbitMQ connections for all nodes.
     */
    private boolean isNodeConnected(String node) {
        RabbitMQUtil rabbitMQUtil = new RabbitMQUtil(node);
        String connectedNode = rabbitMQUtil.getConnectedServer();
        rabbitMQUtil.closeConnection();
        return connectedNode != null;
    }

    /**
     * Gets the current leader node.
     *
     * @return The leader node's URI, or null if no leader is available.
     */
    public String getLeader() {
        return leader;
    }

    /**
     * Checks if a given node is the elected leader.
     *
     * @param nodeId The node ID (URI).
     * @return True if the node is the leader, otherwise false.
     */
    public boolean isLeader(String nodeId) {
        return nodeId.equals(leader);
    }
}
