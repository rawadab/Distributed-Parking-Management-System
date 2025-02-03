package com.example.queries;



import com.example.shared.models.Citation;
import com.example.shared.utils.DatabaseUtil;
import com.example.shared.utils.RabbitMQUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service class for Parking Enforcement Officer (PEO) operations.
 * This class provides methods to check parking legality, issue citations,
 * validate parking spaces, and retrieve zone details.
 * It interacts with a database using {@link DatabaseUtil} and utilizes {@link RabbitMQUtil}
 * for logging citation messages.
 * @version 8
 */
public class PEOService {

    /** Utility class for RabbitMQ messaging */
    private final RabbitMQUtil rabbitMQUtil = new RabbitMQUtil();

    /**
     * Checks if a vehicle is legally parked in a parking space.
     *
     * @param vehicleNumber  The vehicle's number.
     * @param parkingSpaceId The ID of the parking space.
     * @return {@code true} if the vehicle is legally parked; {@code false} otherwise.
     */
    public boolean checkIfLegallyParked(String vehicleNumber, String parkingSpaceId) {
        String query = """
                SELECT pe.VehicleID, ps.MaxTime, pe.StartTime
                FROM ParkingEvents pe
                JOIN ParkingSpaces ps ON pe.SpaceID = ps.SpaceID
                WHERE pe.VehicleID = ? AND pe.SpaceID = ? AND pe.EndTime IS NULL
                """;

        try (Connection conn = DatabaseUtil.connect();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, vehicleNumber);
            stmt.setString(2, parkingSpaceId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String maxTime = rs.getString("MaxTime");
                    String startTime = rs.getString("StartTime");

                    // Validate against MaxTime
                    if (isWithinMaxTime(startTime, maxTime)) {
                        recordQuery(conn, vehicleNumber, parkingSpaceId, "Parking Ok");
                        return true;
                    } else {
                        recordQuery(conn, vehicleNumber, parkingSpaceId, "Parking Not Ok");
                        return false;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking parking status: " + e.getMessage());
        }
        return false;
    }

    /**
     * Validates if the current time is within the maximum parking time allowed.
     *
     * @param startTime The start time of the parking event.
     * @param maxTime   The maximum allowed parking duration in minutes.
     * @return {@code true} if the current time is within the allowed time; {@code false} otherwise.
     */
    private boolean isWithinMaxTime(String startTime, String maxTime) {
        LocalDateTime start = LocalDateTime.parse(startTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        LocalDateTime maxEndTime = start.plusMinutes(Integer.parseInt(maxTime));
        return LocalDateTime.now().isBefore(maxEndTime);
    }

    /**
     * Records a query log in the system.
     *
     * @param conn           The database connection.
     * @param vehicleNumber  The vehicle's number.
     * @param parkingSpaceId The parking space ID.
     * @param response       The response status ("Parking Ok" or "Parking Not Ok").
     */
    public void recordQuery(Connection conn, String vehicleNumber, String parkingSpaceId, String response) {
        String logQuery = """
                INSERT INTO SystemLog (QueryTime, VehicleNumber, ParkingSpaceId, Response)
                VALUES (?, ?, ?, ?)
                """;

        try (PreparedStatement stmt = conn.prepareStatement(logQuery)) {
            String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            stmt.setString(1, currentTime);
            stmt.setString(2, vehicleNumber);
            stmt.setString(3, parkingSpaceId);
            stmt.setString(4, response);
            stmt.executeUpdate();
            System.out.println("Query logged successfully: " + response);
        } catch (SQLException e) {
            System.err.println("Error recording query: " + e.getMessage());
        }
    }

    /**
     * Issues a citation for a parking violation.
     *
     * @param citation The citation details.
     * @return {@code true} if the citation was successfully issued; {@code false} otherwise.
     */
    public boolean issueCitation(Citation citation) {
        String insertQuery = """
                INSERT INTO Citations (CitationId, VehicleID, SpaceID, ZoneID, CitationCost, InspectionTime)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = DatabaseUtil.connect();
             PreparedStatement stmt = conn.prepareStatement(insertQuery)) {

            stmt.setInt(1, generateUniqueId("Citations", "CitationID"));
            stmt.setString(2, citation.getVehicleID());
            stmt.setString(3, citation.getSpaceID());
            stmt.setString(4, citation.getParkingZone());
            stmt.setDouble(5, citation.getTotalCost());
            stmt.setString(6, citation.getInspectionTimes());

            stmt.executeUpdate();

            // Log citation message to RabbitMQ
            String citationMessage = String.format(
                    "VehicleID: %s, SpaceID: %s, ParkingZone: %s, inspectionTimes: %s, totalCost: %.2f",
                    citation.getVehicleID(),
                    citation.getSpaceID(),
                    getZoneNameByZoneId(citation.getParkingZone()),
                    citation.getInspectionTimes(),
                    citation.getTotalCost()
            );
            rabbitMQUtil.sendMessage("citationsQueue", citationMessage);
            return true;

        } catch (SQLException e) {
            System.err.println("Error issuing citation: " + e.getMessage());
        }

        return false;
    }

    /**
     * Generates a unique ID for the specified table and column.
     *
     * @param tableName  The name of the table.
     * @param columnName The name of the column.
     * @return A unique ID.
     */
    private int generateUniqueId(String tableName, String columnName) {
        String query = "SELECT MAX(" + columnName + ") AS MaxID FROM " + tableName;
        try (Connection conn = DatabaseUtil.connect();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("MaxID") + 1;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 1;
    }

    /**
     * Retrieves the ZoneID for a given parking space ID.
     *
     * @param parkingSpaceId The parking space ID.
     * @return The ZoneID, or {@code null} if not found.
     */
    public String getZoneIdBySpaceId(String parkingSpaceId) {
        String query = "SELECT ZoneID FROM ParkingSpaces WHERE SpaceID = ?";
        try (Connection conn = DatabaseUtil.connect();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, parkingSpaceId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("ZoneID");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving ZoneID: " + e.getMessage());
        }
        return null;
    }

    public String generateUniqueCitationId() {
        return "CIT" + System.currentTimeMillis();
}
    /**
     * Retrieves the ZoneName for a given ZoneID.
     *
     * @param zoneId The ZoneID.
     * @return The ZoneName, or {@code null} if not found.
     */
    public String getZoneNameByZoneId(String zoneId) {
        String query = "SELECT ZoneName FROM Zones WHERE ZoneID = ?";
        try (Connection conn = DatabaseUtil.connect();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, zoneId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("ZoneName");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving ZoneName: " + e.getMessage());
        }
        return null;
    }

    /**
     * Validates if the given parking space ID exists in the database.
     *
     * @param parkingSpaceId The parking space ID.
     * @return {@code true} if the parking space is valid; {@code false} otherwise.
     */
    public boolean isParkingSpaceValid(String parkingSpaceId) {
        String query = """
                SELECT SpaceID
                FROM ParkingSpaces
                WHERE SpaceID = ?
                """;
        try (Connection conn = DatabaseUtil.connect();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, parkingSpaceId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Error validating ParkingSpaceId: " + e.getMessage());
        }
        return false;
    }

    /**
     * Checks if the vehicle ID exists in the database.
     *
     * @param vehicleNumber The vehicle's ID.
     * @return {@code true} if the vehicle exists in the database; {@code false} otherwise.
     */
    public boolean isVehicleValid(String vehicleNumber) {
        String query = "SELECT VehicleID FROM Vehicles WHERE VehicleID = ?";
        try (Connection conn = DatabaseUtil.connect();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, vehicleNumber);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next(); // If a row is returned, the vehicle exists
            }
        } catch (SQLException e) {
            System.err.println("Error checking vehicle existence: " + e.getMessage());
        }
        return false;
    }
}
