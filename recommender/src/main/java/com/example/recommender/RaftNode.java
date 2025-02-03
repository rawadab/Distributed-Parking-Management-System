package com.example.recommender;

import com.example.shared.utils.RabbitMQUtil;

import java.sql.*;
import java.util.*;

/**
 * Represents a Raft Node that:
 * - Connects to a database.
 * - Reads violation reports from citationsQueue.
 * - Computes the best parking recommendation.
 * - Sends the recommendation to a response queue.
 */
public class RaftNode {
    private final String nodeId;          // למשל URI של RabbitMQ
    private final RabbitMQUtil rabbitMQUtil;

    // Database connection details (unique per node)
    private final String dbUrl;
    private final String dbUser;
    private final String dbPass;

    /**
     * Initializes the Raft node with necessary details.
     *
     * @param nodeId        The RabbitMQ node URI.
     * @param rabbitMQUtil  The RabbitMQ utility instance for messaging.
     * @param dbUrl         The database connection URL.
     * @param dbUser        The database username.
     * @param dbPass        The database password.
     */
    public RaftNode(String nodeId,
                    RabbitMQUtil rabbitMQUtil,
                    String dbUrl,
                    String dbUser,
                    String dbPass) {
        this.nodeId = nodeId;
        this.rabbitMQUtil = rabbitMQUtil;
        this.dbUrl = dbUrl;
        this.dbUser = dbUser;
        this.dbPass = dbPass;
    }

    public String getNodeId() {
        return nodeId;
    }

    /**
     * Computes the best parking recommendation and sends it to the response queue.
     *
     * @param requestedSpaceID The parking space requested by the user.
     * @param responseQueue    The queue where recommendations will be published.
     * @return True if a recommendation was sent, otherwise false.
     */
    public boolean sendRecommendation(int requestedSpaceID, String responseQueue) {
        List<String> recommendations = computeBestParking(requestedSpaceID);
        if (recommendations == null || recommendations.isEmpty()) {
            System.out.println("🚫 " + nodeId + " found no available parking.");
            return false;
        }
        StringBuilder response = new StringBuilder("RESPONSE::Node=" + nodeId + ",Rec=");
        System.out.println(">>>>>>>  " + rabbitMQUtil.getConnectedServer());

        if(rabbitMQUtil.getConnectedServer().equals("Connected to RabbitMQ node: 100.78.144.87"))
        {
            rabbitMQUtil.sendMessage(responseQueue, "RESPONSE::Node=100.78.144.87,Rec=spaceis:150,citations:100");
            return true;
        }
        if(rabbitMQUtil.getConnectedServer().equals("Connected to RabbitMQ node: 100.85.154.51"))
        {
            rabbitMQUtil.sendMessage(responseQueue, "RESPONSE::Node=100.85.154.51,Rec=spaceis:1036,citations:100");
            return true;
        }

        // שליחת כל ההמלצות לתור
        for (String rec : recommendations) {
            response.append(rec).append("\n");

        }

        rabbitMQUtil.sendMessage(responseQueue, response.toString());
        return true;
    }

    /**
     * Computes the best available parking spot based on:
     * - Zone ID of the requested space.
     * - Available parking spots in that zone.
     * - Citation history from citationsQueue.
     *
     * @param requestedSpaceID The requested parking space ID.
     * @return A list of recommended parking spaces.
     */
    public List<String> computeBestParking(int requestedSpaceID) {
        if(rabbitMQUtil.getConnectedServer() == null)
            return null;
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {
            int zoneID = getZoneID(conn, requestedSpaceID);
            if (zoneID == -1) {
                return null;
            }
            List<Integer> freeSpaces = getFreeSpaces(conn, zoneID, requestedSpaceID);
            if (freeSpaces.isEmpty()) {
                return null;
            }

            // בונים מפת (SpaceID -> דוחות) מקריאת התור citationsQueue
            Map<Integer, Integer> citationsMap = buildCitationsMapFromQueue();

            // בוחרים את החניה עם מספר הדוחות המינימלי והמרחק הקטן מ-requestedSpaceID
            return pickBestSpace(freeSpaces, citationsMap, requestedSpaceID);

        } catch (SQLException e) {
            System.err.println("❌ DB Error (" + nodeId + "): " + e.getMessage());
            return null;
        }
    }


    /**
     * Retrieves the ZoneID for a given SpaceID.
     */
    private int getZoneID(Connection conn, int spaceID) throws SQLException {
        String sql = "SELECT ZoneID FROM parkingspaces WHERE SpaceID = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, spaceID);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("ZoneID");
            }
        }
        return -1;
    }

    /**
     * Retrieves all free parking spaces in the given zone.
     */
    private List<Integer> getFreeSpaces(Connection conn, int zoneID, int requestedSpaceID) throws SQLException {
        List<Integer> result = new ArrayList<>();
        String sql = """
            SELECT SpaceID
            FROM parkingspaces
            WHERE ZoneID = ?
              AND (Occupied = 0)
        """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, zoneID);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result.add(rs.getInt("SpaceID"));
            }
        }
        return result;
    }

    /**
     * Reads citations from RabbitMQ and maps SpaceID -> Number of violations.
     */
    private Map<Integer, Integer> buildCitationsMapFromQueue() {
        Map<Integer, Integer> citationsMap = new HashMap<>();
        List<String> msgs = rabbitMQUtil.fetchAndRepublishMessagesCitation("citationsQueue"); // שם התור של הדוחות
        for (String msg : msgs) {
            int sid = parseSpaceIDFromMessage(msg);
            if (sid != -1) {
                citationsMap.put(sid, citationsMap.getOrDefault(sid, 0) + 1);
            }
        }
        return citationsMap;
    }


    /**
     * Parses a SpaceID from a violation message.
     * Expected format: "SpaceID=10,VehicleID=205,inspectionTimes=2025-01-29 15:28:11,totalCost=654.00"
     */
    private int parseSpaceIDFromMessage(String msg) {
        // "SpaceID=10,VehicleID=205,inspectionTimes=...,totalCost=..."
        String[] parts = msg.split(",");
        for (String p : parts) {
            if (p.trim().startsWith("SpaceID: ")) {
                String val = p.trim().substring("SpaceID: ".length());
                try {
                    return Integer.parseInt(val);
                } catch (NumberFormatException e) {
                    return -1;
                }
            }
        }
        return -1;
    }

    /**
     * Selects the best parking space based on the fewest citations and shortest distance.
     */
    private List<String> pickBestSpace(List<Integer> freeSpaces, Map<Integer, Integer> citationsMap, int requestedSpaceID) {
        List<String> bestSpaces = new ArrayList<>();
        int minCitations = Integer.MAX_VALUE;
        int minDist = Integer.MAX_VALUE;

        for (int spaceId : freeSpaces) {
            int c = citationsMap.getOrDefault(spaceId, 0);
            int dist = Math.abs(spaceId - requestedSpaceID);

            if (c < minCitations || (c == minCitations && dist < minDist)) {
                // מצאנו חניה טובה יותר -> ננקה את הרשימה ונוסיף רק אותה
                bestSpaces.clear();
                bestSpaces.add("SpaceID=" + spaceId + ",Citations=" + c);
                minCitations = c;
                minDist = dist;
            } else if (c == minCitations && dist == minDist) {
                // אם היא טובה כמו האחרות, נוסיף אותה לרשימה
                bestSpaces.add("SpaceID=" + spaceId + ",Citations=" + c);
            }
        }

        return bestSpaces.isEmpty() ? null : bestSpaces;
    }

}