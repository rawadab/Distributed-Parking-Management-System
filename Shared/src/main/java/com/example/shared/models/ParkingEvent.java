package com.example.shared.models;

/**
 * Represents a parking event, including details such as the event ID, parking space ID,
 * start time, end time, and total cost of the event.
 * This class provides multiple constructors for creating instances of parking events
 * with varying levels of detail, as well as getter and setter methods for accessing
 * and modifying the event's properties.
 * @version 1
 */
public class ParkingEvent {

    /** Unique identifier for the parking event */
    private int eventId;

    /** Identifier for the parking space associated with the event */
    private int spaceId;

    /** Start time of the parking event */
    private String startTime;

    /** End time of the parking event */
    private String endTime;

    /** Total cost of the parking event */
    private double totalCost;

    /**
     * Constructor for creating a ParkingEvent with space ID, start time, end time, and total cost.
     *
     * @param spaceId the ID of the parking space
     * @param startTime the start time of the event
     * @param endTime the end time of the event
     * @param totalCost the total cost of the event
     */
    public ParkingEvent(int spaceId, String startTime, String endTime, double totalCost) {
        this.spaceId = spaceId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.totalCost = totalCost;
    }

    /**
     * Constructor for creating a ParkingEvent with event ID, space ID, start time, end time, and total cost.
     *
     * @param eventId the unique ID of the parking event
     * @param spaceId the ID of the parking space
     * @param startTime the start time of the event
     * @param endTime the end time of the event
     * @param totalCost the total cost of the event
     */
    public ParkingEvent(int eventId, int spaceId, String startTime, String endTime, double totalCost) {
        this.eventId = eventId;
        this.spaceId = spaceId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.totalCost = totalCost;
    }

    /**
     * Constructor for creating a ParkingEvent with space ID, start time, and end time.
     *
     * @param spaceId the ID of the parking space
     * @param startTime the start time of the event
     * @param endTime the end time of the event
     */
    public ParkingEvent(int spaceId, String startTime, String endTime) {
        this.spaceId = spaceId;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /**
     * Constructor for creating a ParkingEvent with space ID and start time.
     *
     * @param spaceId the ID of the parking space
     * @param startTime the start time of the event
     */
    public ParkingEvent(int spaceId, String startTime) {
        this.spaceId = spaceId;
        this.startTime = startTime;
    }

    /**
     * Gets the unique ID of the parking event.
     *
     * @return the event ID
     */
    public int getEventId() {
        return eventId;
    }

    /**
     * Sets the unique ID of the parking event.
     *
     * @param eventId the new event ID
     */
    public void setEventId(int eventId) {
        this.eventId = eventId;
    }

    /**
     * Gets the ID of the parking space associated with the event.
     *
     * @return the parking space ID
     */
    public int getSpaceId() {
        return spaceId;
    }

    /**
     * Sets the ID of the parking space associated with the event.
     *
     * @param spaceId the new parking space ID
     */
    public void setSpaceId(int spaceId) {
        this.spaceId = spaceId;
    }

    /**
     * Gets the start time of the parking event.
     *
     * @return the start time
     */
    public String getStartTime() {
        return startTime;
    }

    /**
     * Sets the start time of the parking event.
     *
     * @param startTime the new start time
     */
    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    /**
     * Gets the end time of the parking event.
     *
     * @return the end time
     */
    public String getEndTime() {
        return endTime;
    }

    /**
     * Sets the end time of the parking event.
     *
     * @param endTime the new end time
     */
    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    /**
     * Gets the total cost of the parking event.
     *
     * @return the total cost
     */
    public double getTotalCost() {
        return totalCost;
    }

    /**
     * Sets the total cost of the parking event.
     *
     * @param totalCost the new total cost
     */
    public void setTotalCost(double totalCost) {
        this.totalCost = totalCost;
    }

    /**
     * Returns a string representation of the parking event.
     * @return a string containing event details
     */
    @Override
    public String toString() {
        return "{" +
                "eventId=" + eventId +
                ", spaceId=" + spaceId +
                ", startTime='" + startTime + '\'' +
                ", endTime='" + endTime + '\'' +
                ", totalCost=" + totalCost +
                '}';
    }
}
