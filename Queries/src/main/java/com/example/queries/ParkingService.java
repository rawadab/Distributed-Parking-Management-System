package com.example.queries;



import com.example.shared.models.ParkingEvent;
import com.example.shared.utils.DatabaseUtil;
import com.example.shared.utils.RabbitMQUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service class for handling parking-related operations.
 * This class manages parking events, validates parking spaces, calculates costs, and logs transactions to RabbitMQ.
 * It interacts with the database and RabbitMQ for storing and processing events.
 *
 * @author
 * @version 1.0
 * @since 2024
 */
public class ParkingService {

    // RabbitMQ utility for sending messages
    private final RabbitMQUtil rabbitMQUtil = new RabbitMQUtil();

    /**
     * Starts a parking event for the given vehicle and parking space.
     *
     * @param vehicleId      the ID of the vehicle
     * @param parkingSpaceId the ID of the parking space
     * @return {@code true} if the parking event is started successfully; {@code false} otherwise
     */
    public boolean startParking(int vehicleId, String parkingSpaceId) {
        try (Connection conn = DatabaseUtil.connect()) {
            // Check if the parking space ID is provided
            if (parkingSpaceId.isEmpty()) {
                System.out.println("Invalid parking space.");
                return false;
            }

            // Validate if the parking space is available
            if (!validateParkingSpace(conn, parkingSpaceId)) {
                System.out.println("Failed to start parking event, parking space is occupied.");
                return false;
            }

            // Check for an existing active parking event for the vehicle
            if (vehicleHasActiveEvent(conn, vehicleId)) {
                System.out.println("Vehicle already has an active parking event. Stopping the current event...");
                stopParking(vehicleId); // Stop the current active event
            }

            // Start a new parking event
            if (startNewParkingEvent(conn, vehicleId, parkingSpaceId)) {
                // Mark the parking space as occupied
                markSpaceAsOccupied(conn, parkingSpaceId);
                System.out.println("Parking started successfully for Vehicle ID: " + vehicleId);
                return true;
            } else {
                System.out.println("Failed to create a new parking event.");
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Stops an active parking event for the given vehicle and logs the transaction to RabbitMQ.
     *
     * @param vehicleId the ID of the vehicle
     * @return {@code true} if the parking event is stopped successfully; {@code false} otherwise
     */
    public boolean stopParking(int vehicleId) {
        try (Connection conn = DatabaseUtil.connect()) {
            // Retrieve the hourly rate for the parking zone
            String getCostQuery = """
                SELECT z.HourlyRate, pe.StartTime
                FROM zones z
                INNER JOIN ParkingSpaces ps ON ps.ZoneID = z.zoneID
                INNER JOIN ParkingEvents pe ON pe.SpaceID = ps.SpaceID
                WHERE pe.VehicleID = ? AND pe.EndTime IS NULL
                """;

            double costPerHour;
            String startTime;
            try (PreparedStatement costStmt = conn.prepareStatement(getCostQuery)) {
                costStmt.setInt(1, vehicleId); // Set the vehicle ID parameter
                try (ResultSet rs = costStmt.executeQuery()) {
                    if (rs.next()) {
                        costPerHour = rs.getDouble("HourlyRate"); // Retrieve the hourly rate
                        startTime = rs.getString("StartTime"); // Retrieve the start time
                    } else {
                        System.out.println("No active parking event or invalid vehicle ID: " + vehicleId);
                        return false;
                    }
                }
            }

            // Calculate total cost in Java
            LocalDateTime startDateTime = LocalDateTime.parse(startTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            LocalDateTime now = LocalDateTime.now();
            long minutes = java.time.Duration.between(startDateTime, now).toMinutes();
            double totalCost = (minutes / 60.0) * costPerHour;

            // Update the parking event with the end time and total cost
            String updateEventQuery = """
                UPDATE ParkingEvents
                SET EndTime = ?, TotalCost = ?
                WHERE VehicleID = ? AND EndTime IS NULL
                """;

            ParkingEvent temp; // Temporary storage for parking event details
            String zoneName; // Zone name for the event
            try (PreparedStatement stmt = conn.prepareStatement(updateEventQuery)) {
                stmt.setString(1, now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))); // Set the current time
                stmt.setDouble(2, totalCost); // Set the total cost
                stmt.setInt(3, vehicleId); // Set the vehicle ID parameter

                int rowsUpdated = stmt.executeUpdate(); // Execute the update query
                if (rowsUpdated > 0) {
                    System.out.println("Parking event ended for Vehicle ID: " + vehicleId);

                    // Retrieve the details for the completed event
                    String fetchEventQuery = """
                        SELECT pe.*, 
                               z.ZoneName, 
                               (SELECT CustomerID FROM Vehicles WHERE VehicleID = ?) AS CustomerID
                        FROM ParkingEvents pe
                        INNER JOIN ParkingSpaces ps ON pe.SpaceID = ps.SpaceID
                        INNER JOIN Zones z ON ps.ZoneID = z.ZoneID
                        WHERE pe.VehicleID = ? AND pe.EndTime = ?
                        """;
                    try (PreparedStatement fetchStmt = conn.prepareStatement(fetchEventQuery)) {
                        fetchStmt.setInt(1, vehicleId);
                        fetchStmt.setInt(2, vehicleId);
                        fetchStmt.setString(3, now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

                        try (ResultSet rs = fetchStmt.executeQuery()) {
                            if (rs.next()) {
                                temp = new ParkingEvent(
                                        rs.getInt("EventID"),
                                        rs.getInt("SpaceID"),
                                        rs.getString("StartTime"),
                                        rs.getString("EndTime"),
                                        rs.getDouble("TotalCost")
                                );
                                zoneName = rs.getString("ZoneName"); // Retrieve the zone name
                            } else {
                                System.out.println("Failed to fetch transaction details for Vehicle ID: " + vehicleId);
                                return false;
                            }
                        }
                    }

                    // Free the parking space after stopping the event
                    freeParkingSpace(conn, vehicleId);

                    // Log the transaction to RabbitMQ
                    String transactionMessage = String.format(
                            "VehicleID: %s, ZoneName: %s, SpaceID: %d, StartTime: %s, EndTime: %s, TotalCost: %.2f",
                            vehicleId,
                            zoneName,
                            temp.getSpaceId(),
                            temp.getStartTime(),
                            temp.getEndTime(),
                            temp.getTotalCost()
                    );
                    boolean sent = rabbitMQUtil.sendMessage("transactionsQueue", transactionMessage);
                    if (sent) {
                        System.out.println("Transaction logged to RabbitMQ for Vehicle ID: " + vehicleId);
                    } else {
                        System.err.println("Failed to log transaction to RabbitMQ for Vehicle ID: " + vehicleId);
                    }

                    return true;
                } else {
                    System.out.println("No active parking event found for Vehicle ID: " + vehicleId);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }



    /**
     * Frees a parking space after a parking event ends.
     *
     * @param conn      the database connection
     * @param vehicleId the ID of the vehicle
     * @throws SQLException if an SQL error occurs
     */
    public void freeParkingSpace(Connection conn, int vehicleId) throws SQLException {
        String currentTime = java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        );

        String spaceQuery = "SELECT SpaceID FROM ParkingEvents WHERE VehicleID = ? AND EndTime = ?";
        try (PreparedStatement spaceStmt = conn.prepareStatement(spaceQuery)) {
            spaceStmt.setInt(1, vehicleId);
            spaceStmt.setString(2, currentTime);
            try (ResultSet rs = spaceStmt.executeQuery()) {
                if (rs.next()) {
                    int spaceId = rs.getInt("SpaceID");
                    String updateSpaceQuery = "UPDATE ParkingSpaces SET Occupied = 0 WHERE SpaceID = ?";
                    try (PreparedStatement updateSpaceStmt = conn.prepareStatement(updateSpaceQuery)) {
                        updateSpaceStmt.setInt(1, spaceId);
                        updateSpaceStmt.executeUpdate();
                        System.out.println("Parking space " + spaceId + " is now available.");
                    }
                }
            }
        }
    }


    public void UpdateParkingSpaceToBeFree(Connection conn, String parkingSpaceId) throws SQLException {
        String query = "UPDATE ParkingSpaces SET Occupied = 0 WHERE SpaceID = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, parkingSpaceId); // Set the parking space ID parameter
            stmt.executeUpdate(); // Mark the space as occupied
        }
    }




    /**
     * Validates whether a parking space is available for use.
     *
     * @param conn            the database connection
     * @param parkingSpaceId  the ID of the parking space
     * @return {@code true} if the parking space is not occupied; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    public boolean validateParkingSpace(Connection conn, String parkingSpaceId) throws SQLException {
        String query = "SELECT Occupied FROM ParkingSpaces WHERE SpaceID = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, parkingSpaceId); // Set the parking space ID parameter
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return !rs.getBoolean("Occupied"); // Return true if space is not occupied
                }
            }
        }
        return false; // Return false for invalid or non-existent parking spaces
    }

    /**
     * Checks if a vehicle has an active parking event.
     *
     * @param conn      the database connection
     * @param vehicleId the ID of the vehicle
     * @return {@code true} if there is an active parking event; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    public boolean vehicleHasActiveEvent(Connection conn, int vehicleId) throws SQLException {
        String query = "SELECT * FROM ParkingEvents WHERE VehicleID = ? AND EndTime IS NULL";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, vehicleId); // Set the vehicle ID parameter
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next(); // Return true if there is an active event
            }
        }
    }

    /**
     * Starts a new parking event for a vehicle in a given parking space.
     *
     * @param conn            the database connection
     * @param vehicleId       the ID of the vehicle
     * @param parkingSpaceId  the ID of the parking space
     * @return {@code true} if the parking event is started successfully; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    public boolean startNewParkingEvent(Connection conn, int vehicleId, String parkingSpaceId) throws SQLException {
        String maxTimeQuery = "SELECT MaxTime FROM ParkingSpaces WHERE SpaceID = ?";
        int maxTimeMinutes = 0;

        // Retrieve the maximum allowed parking time for the space
        try (PreparedStatement maxTimeStmt = conn.prepareStatement(maxTimeQuery)) {
            maxTimeStmt.setString(1, parkingSpaceId);
            try (ResultSet rs = maxTimeStmt.executeQuery()) {
                if (rs.next()) {
                    maxTimeMinutes = rs.getInt("MaxTime"); // Get the MaxTime in minutes
                }
            }
        }

        // Generate current time and max time
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime maxTime = now.plusMinutes(maxTimeMinutes); // Add max time in minutes
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // Debugging: Print formatted times
        System.out.println("StartTime: " + now.format(formatter));
        System.out.println("MaxTime: " + maxTime.format(formatter));

        // Insert a new parking event into the ParkingEvents table
        String query = """
        INSERT INTO ParkingEvents (EventID, VehicleID, SpaceID, StartTime, MaxTime)
        VALUES (?, ?, ?, ?, ?)
    """;

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, generateUniqueId("ParkingEvents", "EventID")); // Generate unique event ID
            stmt.setInt(2, vehicleId); // Set vehicle ID
            stmt.setString(3, parkingSpaceId); // Set space ID
            stmt.setString(4, now.format(formatter)); // Set current time
            stmt.setString(5, maxTime.format(formatter)); // Set max time

            int rowsInserted = stmt.executeUpdate(); // Execute the insert statement
            return rowsInserted > 0; // Return true if the insert is successful
        } catch (SQLException e) {
            e.printStackTrace();
            throw e; // Re-throw the exception for further handling
        }
    }


    /**
     * Marks a parking space as occupied.
     *
     * @param conn            the database connection
     * @param parkingSpaceId  the ID of the parking space to mark as occupied
     * @throws SQLException if a database access error occurs
     */
    public void markSpaceAsOccupied(Connection conn, String parkingSpaceId) throws SQLException {
        String query = "UPDATE ParkingSpaces SET Occupied = 1 WHERE SpaceID = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, parkingSpaceId); // Set the parking space ID parameter
            stmt.executeUpdate(); // Mark the space as occupied
        }
    }

    /**
     * Generates a unique ID for a specified table and column.
     *
     * @param tableName  the name of the table
     * @param columnName the name of the column
     * @return a unique ID based on the maximum existing ID in the column
     */
    private int generateUniqueId(String tableName, String columnName) {
        try (Connection conn = DatabaseUtil.connect()) {
            String query = "SELECT MAX(" + columnName + ") AS MaxID FROM " + tableName;
            try (PreparedStatement stmt = conn.prepareStatement(query);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("MaxID") + 1; // Return the next available ID
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 1; // Return 1 if no records exist
    }

    /**
     * Retrieves a list of completed parking events for a vehicle.
     *
     * @param vehicleId the ID of the vehicle
     * @return a list of completed {@link ParkingEvent} objects
     */
    public List<ParkingEvent> getParkingcompletedEvents(int vehicleId) {
        List<ParkingEvent> events = new ArrayList<>();
        String completedEventsQuery = """
                SELECT SpaceID, StartTime, EndTime, TotalCost
                FROM ParkingEvents
                WHERE VehicleID = ? AND EndTime IS NOT NULL
                ORDER BY StartTime DESC
                """;

        try (Connection conn = DatabaseUtil.connect();
             PreparedStatement stmt = conn.prepareStatement(completedEventsQuery)) {
            stmt.setInt(1, vehicleId); // Set the vehicle ID parameter

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ParkingEvent event = new ParkingEvent(
                            rs.getInt("SpaceID"), // Retrieve space ID
                            rs.getString("StartTime"), // Retrieve start time
                            rs.getString("EndTime"), // Retrieve end time
                            rs.getDouble("TotalCost") // Retrieve total cost
                    );
                    events.add(event); // Add the event to the list
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return events; // Return the list of completed events
    }

    /**
     * Retrieves a list of active parking events for a vehicle.
     *
     * @param vehicleId the ID of the vehicle
     * @return a list of active {@link ParkingEvent} objects
     */
    public List<ParkingEvent> getParkingactiveEvents(int vehicleId) {
        List<ParkingEvent> events = new ArrayList<>();
        String activeEventsQuery = """
                SELECT EventID, SpaceID, StartTime, TotalCost
                FROM ParkingEvents
                WHERE VehicleID = ? AND EndTime IS NULL
                ORDER BY StartTime DESC
                """;

        try (Connection conn = DatabaseUtil.connect();
             PreparedStatement stmt = conn.prepareStatement(activeEventsQuery)) {
            stmt.setInt(1, vehicleId); // Set the vehicle ID parameter

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ParkingEvent event = new ParkingEvent(
                            rs.getInt("SpaceID"), // Retrieve space ID
                            rs.getString("StartTime") // Retrieve start time
                    );
                    events.add(event); // Add the event to the list
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return events; // Return the list of active events
    }

    /**
     * Retrieves the vehicle number associated with a customer.
     *
     * @param customerID the ID of the customer
     * @return the vehicle ID associated with the customer, or 0 if not found
     */

    public int getVehicleNumber(int customerID) {
        int vehicleID = 0;
        String query = "SELECT VehicleID FROM Vehicles WHERE CustomerID = ?";

        Connection conn = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            conn = DatabaseUtil.connect(); // Establish connection
            preparedStatement = conn.prepareStatement(query); // Prepare the query
            preparedStatement.setInt(1, customerID); // Set the customer ID parameter
            resultSet = preparedStatement.executeQuery(); // Execute the query

            if (resultSet.next()) {
                vehicleID = resultSet.getInt("VehicleID"); // Retrieve the vehicle ID
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            // Ensure all resources are closed
            DatabaseUtil.closeResources(conn, preparedStatement, resultSet);
        }

        return vehicleID; // Return the vehicle ID
    }

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

    public String getZoneNameByZoneID(String Zoneid) {
        String query = "SELECT ZoneName FROM zones WHERE ZoneID = ?";
        try (Connection conn = DatabaseUtil.connect();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, Zoneid);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("ZoneName");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving ZoneID: " + e.getMessage());
        }
        return null;
    }

    /**
     * Deletes all parking events associated with a vehicle.
     *
     * @param vehicleNumber the vehicle ID
     */
    public void deleteAllCustomerParkingEvents(int vehicleNumber) {
        String query = "DELETE FROM ParkingEvents WHERE VehicleID = ?";

        try (Connection conn = DatabaseUtil.connect();
             PreparedStatement preparedStatement = conn.prepareStatement(query)) {
            preparedStatement.setInt(1, vehicleNumber); // Set the vehicle ID parameter
            preparedStatement.executeUpdate(); // Execute the delete statement
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
