package com.example.shared.models;

/**
 * Represents a parking transaction in the system.
 * This class contains details such as the vehicle ID, zone name, parking space ID,
 * start time, end time, and total cost of the transaction.
 * The class provides getter and setter methods to access and modify transaction details.
 * @version 1
 */
public class Transaction {

    /** Vehicle ID associated with the transaction */
    private String vehicleId;

    /** Name of the parking zone for the transaction */
    private String zoneName;

    /** Parking space ID used in the transaction */
    private String spaceId;

    /** Start time of the transaction */
    private String startTime;

    /** End time of the transaction */
    private String endTime;

    /** Total cost of the transaction */
    private String totalCost;

    /**
     * Constructs a new Transaction with specified details.
     *
     * @param vehicleId the ID of the vehicle involved in the transaction
     * @param zoneName the name of the parking zone
     * @param spaceId the ID of the parking space
     * @param startTime the start time of the transaction
     * @param endTime the end time of the transaction
     * @param totalCost the total cost of the transaction
     */
    public Transaction(String vehicleId, String zoneName, String spaceId, String startTime, String endTime, String totalCost) {
        this.vehicleId = vehicleId;
        this.zoneName = zoneName;
        this.spaceId = spaceId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.totalCost = totalCost;
    }

    /**
     * Gets the vehicle ID associated with the transaction.
     *
     * @return the vehicle ID
     */
    public String getVehicleId() {
        return vehicleId;
    }

    /**
     * Sets the vehicle ID for the transaction.
     *
     * @param vehicleId the new vehicle ID
     */
    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    /**
     * Gets the name of the parking zone for the transaction.
     *
     * @return the zone name
     */
    public String getZoneName() {
        return zoneName;
    }

    /**
     * Sets the name of the parking zone for the transaction.
     *
     * @param zoneName the new zone name
     */
    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    /**
     * Gets the ID of the parking space used in the transaction.
     *
     * @return the parking space ID
     */
    public String getSpaceId() {
        return spaceId;
    }

    /**
     * Sets the ID of the parking space used in the transaction.
     *
     * @param spaceId the new parking space ID
     */
    public void setSpaceId(String spaceId) {
        this.spaceId = spaceId;
    }

    /**
     * Gets the start time of the transaction.
     *
     * @return the start time
     */
    public String getStartTime() {
        return startTime;
    }

    /**
     * Sets the start time of the transaction.
     *
     * @param startTime the new start time
     */
    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    /**
     * Gets the end time of the transaction.
     *
     * @return the end time
     */
    public String getEndTime() {
        return endTime;
    }

    /**
     * Sets the end time of the transaction.
     *
     * @param endTime the new end time
     */
    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    /**
     * Gets the total cost of the transaction.
     *
     * @return the total cost
     */
    public String getTotalCost() {
        return totalCost;
    }

    /**
     * Sets the total cost of the transaction.
     *
     * @param totalCost the new total cost
     */
    public void setTotalCost(String totalCost) {
        this.totalCost = totalCost;
    }
}
