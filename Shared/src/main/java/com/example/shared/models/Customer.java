package com.example.shared.models;

/**
 * Represents a customer in the parking system.
 * This class contains customer details such as their ID, name, and associated vehicle number.
 * The class provides getter and setter methods to access and modify the customer details.
 * @version 1
 */
public class Customer {
    /** Unique identifier for the customer */
    private String customerId;
    /** Name of the customer */
    private String name;
    /** Vehicle number associated with the customer */
    private String vehicleNumber;
    /**
     * Gets the customer ID.
     * @return the customer ID
     */
    public String getCustomerId() {
        return customerId;
    }
    /**
     * Sets the customer ID.
     * @param customerId the new customer ID
     */
    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }
    /**
     * Gets the customer's name.
     * @return the customer's name
     */
    public String getName() {
        return name;
    }
    /**
     * Sets the customer's name.
     * @param name the new customer's name
     */
    public void setName(String name) {
        this.name = name;
    }
    /**
     * Gets the vehicle number associated with the customer.
     * @return the vehicle number
     */
    public String getVehicleNumber() {
        return vehicleNumber;
    }
    /**
     * Sets the vehicle number associated with the customer.
     * @param vehicleNumber the new vehicle number
     */
    public void setVehicleNumber(String vehicleNumber) {
        this.vehicleNumber = vehicleNumber;
    }
}
