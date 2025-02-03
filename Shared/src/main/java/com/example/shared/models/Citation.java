package com.example.shared.models;

/**
 * Represents a parking citation issued for parking violations.
 * This class encapsulates details such as vehicle ID, parking space ID,
 * parking zone, inspection times, and citation cost.
 * It provides constructors for creating citation instances and getters/setters
 * for accessing and modifying citation details.
 * @version 1.0
 */
public class Citation {

    /** Unique identifier for the citation */
    private String citationId;

    /** Vehicle ID associated with the citation */
    private String VehicleID;

    /** Parking space ID where the violation occurred */
    private String SpaceID;

    /** Total cost of the citation */
    private double totalCost;

    /** Timestamp or times of inspection for the citation */
    private String inspectionTimes;

    /** Parking zone where the violation occurred */
    private String ParkingZone;

    /**
     * Constructor for creating a Citation with basic details.
     * @param VehicleID the ID of the vehicle
     * @param SpaceID the ID of the parking space
     * @param ParkingZone the parking zone
     * @param inspectionTimes the times of inspection
     * @param totalCost the total cost of the citation
     */
    public Citation(String VehicleID, String SpaceID, String ParkingZone, String inspectionTimes, Double totalCost) {
        this.VehicleID = VehicleID;
        this.SpaceID = SpaceID;
        this.ParkingZone = ParkingZone;
        this.inspectionTimes = inspectionTimes;
        this.totalCost = totalCost;
    }
    /**
     * Constructor for creating a Citation with a citation ID and detailed information.
     * @param citationId the unique ID for the citation
     * @param VehicleID the ID of the vehicle
     * @param SpaceID the ID of the parking space
     * @param ParkingZone the parking zone
     * @param totalCost the total cost of the citation
     * @param inspectionTimes the times of inspection
     */
    public Citation(String citationId, String VehicleID, String SpaceID, String ParkingZone, double totalCost, String inspectionTimes) {
        this.citationId = citationId;
        this.VehicleID = VehicleID;
        this.SpaceID = SpaceID;
        this.ParkingZone = ParkingZone;
        this.totalCost = totalCost;
        this.inspectionTimes = inspectionTimes;
    }
    /**
     * Constructor for creating a Citation with a citation ID and basic details.
     *
     * @param citationId the unique ID for the citation
     * @param VehicleID the ID of the vehicle
     * @param SpaceID the ID of the parking space
     * @param totalCost the total cost of the citation
     * @param inspectionTimes the times of inspection
     */
    public Citation(String citationId, String VehicleID, String SpaceID, double totalCost, String inspectionTimes) {
        this.citationId = citationId;
        this.VehicleID = VehicleID;
        this.SpaceID = SpaceID;
        this.totalCost = totalCost;
        this.inspectionTimes = inspectionTimes;
    }
    /**
     * Gets the citation ID.
     * @return the citation ID
     */
    public String getCitationId() {
        return citationId;
    }
    /**
     * Sets the citation ID
     * @param citationId the new citation ID
     */
    public void setCitationId(String citationId) {
        this.citationId = citationId;
    }
    /**
     * Gets the vehicle ID.
     * @return the vehicle ID
     */
    public String getVehicleID() {
        return VehicleID;
    }
    /**
     * Sets the vehicle ID.
     * @param vehicleID the new vehicle ID
     */
    public void setVehicleID(String vehicleID) {
        this.VehicleID = vehicleID;
    }
    /**
     * Gets the parking space ID.
     * @return the parking space ID
     */
    public String getSpaceID() {
        return SpaceID;
    }
    /**
     * Sets the parking space ID.
     * @param spaceID the new parking space ID
     */
    public void setSpaceID(String spaceID) {
        this.SpaceID = spaceID;
    }
    /**
     * Gets the total cost of the citation.
     * @return the total cost
     */
    public double getTotalCost() {
        return totalCost;
    }
    /**
     * Sets the total cost of the citation.
     * @param totalCost the new total cost
     */
    public void setTotalCost(double totalCost) {
        this.totalCost = totalCost;
    }
    /**
     * Gets the inspection times.
     * @return the inspection times
     */
    public String getInspectionTimes() {
        return inspectionTimes;
    }
    /**
     * Sets the inspection times.
     * @param inspectionTimes the new inspection times
     */
    public void setInspectionTimes(String inspectionTimes) {
        this.inspectionTimes = inspectionTimes;
    }
    /**
     * Gets the parking zone.
     * @return the parking zone
     */
    public String getParkingZone() {
        return ParkingZone;
    }
    /**
     * Sets the parking zone.
     * @param parkingZone the new parking zone
     */
    public void setParkingZone(String parkingZone) {
        this.ParkingZone = parkingZone;
    }
}
