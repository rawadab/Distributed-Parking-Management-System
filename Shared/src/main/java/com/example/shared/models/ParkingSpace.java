package com.example.shared.models;

/**
 * Represents a parking space in the parking system.
 * This class contains details such as the parking space ID, occupancy status,
 * and the hourly rate for using the parking space.
 *
 * <p>
 * The class provides constructors to create a parking space instance and
 * getter and setter methods for accessing and modifying its properties.
 * @version 1
 */
public class ParkingSpace {

    /** Unique identifier for the parking space */
    private String spaceId;

    /** Indicates whether the parking space is currently occupied */
    private boolean isOccupied;

    /** Hourly rate for parking in this space */
    private double hourlyRate;

    /**
     * Constructor to create a ParkingSpace object with specified details.
     *
     * @param spaceId the unique ID of the parking space
     * @param isOccupied whether the space is currently occupied
     * @param hourlyRate the hourly rate for using the space
     */
    public ParkingSpace(String spaceId, boolean isOccupied, double hourlyRate) {
        this.spaceId = spaceId;
        this.isOccupied = isOccupied;
        this.hourlyRate = hourlyRate;
    }

    /**
     * Gets the unique ID of the parking space.
     *
     * @return the parking space ID
     */
    public String getSpaceId() {
        return spaceId;
    }

    /**
     * Sets the unique ID of the parking space.
     *
     * @param spaceId the new parking space ID
     */
    public void setSpaceId(String spaceId) {
        this.spaceId = spaceId;
    }

    /**
     * Checks if the parking space is currently occupied.
     *
     * @return {@code true} if the space is occupied, {@code false} otherwise
     */
    public boolean isOccupied() {
        return isOccupied;
    }

    /**
     * Sets the occupancy status of the parking space.
     *
     * @param isOccupied {@code true} to mark the space as occupied, {@code false} otherwise
     */
    public void setOccupied(boolean isOccupied) {
        this.isOccupied = isOccupied;
    }

    /**
     * Gets the hourly rate for parking in this space.
     *
     * @return the hourly rate
     */
    public double getHourlyRate() {
        return hourlyRate;
    }

    /**
     * Sets the hourly rate for parking in this space.
     *
     * @param hourlyRate the new hourly rate
     */
    public void setHourlyRate(double hourlyRate) {
        this.hourlyRate = hourlyRate;
    }
}
