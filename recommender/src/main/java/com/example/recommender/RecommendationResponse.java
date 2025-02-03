package com.example.recommender;

/**
 * Represents a response message from a node (a single recommendation).
 * This response includes the node identifier and the recommended parking space details.
 */
public class RecommendationResponse {
    private String nodeId;         // מזהה ה-Node (למשל URI של RabbitMQ)
    private String recommendation; // לדוגמה: "SpaceID=12,Citations=1,Dist=2"


    /**
     * Constructs a new RecommendationResponse with the specified node ID and recommendation details.
     *
     * @param nodeId         The unique identifier of the responding node (e.g., RabbitMQ URI).
     * @param recommendation The recommended parking space details in a formatted string.
     */
    public RecommendationResponse(String nodeId, String recommendation) {
        this.nodeId = nodeId;
        this.recommendation = recommendation;
    }

    /**
     * Retrieves the identifier of the responding node.
     *
     * @return The node ID (e.g., RabbitMQ URI).
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * Retrieves the recommendation details.
     *
     * @return A formatted string containing parking space recommendation details.
     */
    public String getRecommendation() {
        return recommendation;
    }

    /**
     * Returns a string representation of the response.
     * The format follows: {@code RESPONSE::Node=<nodeId>,Rec=<recommendation>}
     *
     * @return A formatted string representing the recommendation response.
     */
    @Override
    public String toString() {
        // פורמט טקסטואלי לדוגמה
        return "RESPONSE::Node=" + nodeId + ",Rec=" + recommendation;
    }
}
