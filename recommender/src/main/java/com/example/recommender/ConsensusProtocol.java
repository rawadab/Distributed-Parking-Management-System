package com.example.recommender;

import com.example.recommender.RaftNode;
import com.example.recommender.RecommendationResponse;
import com.example.shared.utils.RabbitMQUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implements the Consensus Protocol:
 *  1) Sends a request to all nodes.
 *  2) Collects responses from the nodes.
 *  3) Performs Majority Voting to determine the consensus decision.
 */
public class ConsensusProtocol {
    // Name of the queue for receiving responses
    private static final String RESPONSE_QUEUE = "recommendationResponsesQueue";

    private final List<RaftNode> raftNodes;
    private final List<RabbitMQUtil> rabbitMQNodes;


    /**
     * Constructor for ConsensusProtocol
     *
     * @param raftNodes      List of Raft nodes participating in consensus
     * @param rabbitMQNodes  List of RabbitMQ utilities for message communication
     */
    public ConsensusProtocol(List<RaftNode> raftNodes,
                             List<RabbitMQUtil> rabbitMQNodes) {
        this.raftNodes = raftNodes;
        this.rabbitMQNodes = rabbitMQNodes;
    }

    /**
     * Sends a recommendation request to all nodes.
     * Each node processes the request and sends a response to RESPONSE_QUEUE.
     *
     * @param spaceID The parking space ID for which recommendation is requested.
     */
    public void sendRecommendationRequest(int spaceID) {
        for (RaftNode node : raftNodes) {
            // Node שולח את תגובתו לתור RESPONSE_QUEUE
            node.sendRecommendation(spaceID, RESPONSE_QUEUE);
        }
    }

    /**
     * Collects all responses from the RESPONSE_QUEUE across all nodes.
     *
     * @return List of RecommendationResponse objects containing responses from nodes.
     */
    public List<RecommendationResponse> collectResponses() {
        List<String> rawMsgs = new ArrayList<>();
        for (RabbitMQUtil rmq : rabbitMQNodes) {
            rawMsgs.addAll(rmq.fetchAndRepublishMessagesCitation(RESPONSE_QUEUE));
        }
        return rawMsgs.stream()
                .map(this::parseResponse)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Determines the consensus recommendation using Majority Voting.
     * The following rules apply:
     * - If all responses are different, return null.
     * - If only one response exists, return null.
     * - If two responses exist and are different, return null.
     * - If the most frequent recommendation has less than half of total responses, return null.
     *
     * @param responses List of responses received from nodes.
     * @return The recommendation with the highest agreement, or null if no consensus is reached.
     */
    public String getConsensusDecision(List<RecommendationResponse> responses) {
        if (responses.isEmpty()) {
            return null;
        }

        // Check if all responses are the same
        boolean allEqual = true;
        String firstRecommendation = responses.get(0).getRecommendation();
        for (RecommendationResponse rr : responses) {
            if (!firstRecommendation.equals(rr.getRecommendation())) {
                allEqual = false;
                break;
            }
        }

        // If all responses are not the same, return null (no consensus)
        if (!allEqual) {
            return null;
        }

        // Count occurrences of each recommendation
        Map<String, Integer> voteCount = new HashMap<>();
        for (RecommendationResponse rr : responses) {
            String rec = rr.getRecommendation();
            voteCount.put(rec, voteCount.getOrDefault(rec, 0) + 1);
        }

        System.out.println("Responses: " + responses);
        System.out.println("Responses size: " + responses.size());

        // If there's only one response, return null (no consensus)
        if (responses.size() == 1) {
            return null;
        }

        // If exactly two responses exist and they are different, return null
        if (responses.size() == 2 && voteCount.size() == 2) {
            return null;
        }

        // If three responses exist and they are all different, return null
        if (responses.size() == 3 && voteCount.size() == 3) {
            return null;
        }

        // Find the recommendation with the highest vote count
        int total = responses.size();
        Optional<Map.Entry<String, Integer>> consensusEntry = voteCount.entrySet().stream()
                .max(Map.Entry.comparingByValue());

        if (consensusEntry.isPresent()) {
            Map.Entry<String, Integer> bestCandidate = consensusEntry.get();

            // If the highest-voted recommendation does not exceed half of total responses, return null
            if (bestCandidate.getValue() < 2) {
                return null;
            }

            return bestCandidate.getKey();
        }

        return null;
    }



    /**
     * Parses a raw response string into a RecommendationResponse object.
     *
     * Example format: "RESPONSE::Node=amqp://100.85.154.51,Rec=SpaceID=10,Citations=0,Dist=2"
     *
     * @param msg Raw response string from RabbitMQ.
     * @return RecommendationResponse object or null if parsing fails.
     */
    private RecommendationResponse parseResponse(String msg) {
        // דוגמה: "RESPONSE::Node=amqp://100.85.154.51,Rec=SpaceID=10,Citations=0,Dist=2"
        if (!msg.startsWith("RESPONSE::")) {
            return null;
        }
        String raw = msg.substring("RESPONSE::".length()); // Node=...,Rec=...
        String[] parts = raw.split(",Rec=");
        if (parts.length != 2) {
            return null;
        }
        String nodePart = parts[0].trim();  // "Node=amqp://100.85.154.51"
        String recPart  = parts[1].trim();  // "SpaceID=10,Citations=0,Dist=2"

        if (!nodePart.startsWith("Node=")) {
            return null;
        }
        String nodeId = nodePart.substring("Node=".length());
        return new RecommendationResponse(nodeId, recPart);
    }
}