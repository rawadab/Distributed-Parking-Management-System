package com.example.queries;



import com.example.shared.utils.DatabaseUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class Server {

    /**
     * Authenticates a customer by checking their Customer ID and Password in the database.
     *
     * @param customerId The Customer ID entered by the user.
     * @param password   The Password entered by the user.
     * @return The customer's name if authentication is successful, null otherwise.
     */
    public String authenticateCustomer(String customerId, String password) {
        String query = "SELECT Name, Password FROM customers WHERE CustomerID = ?";

        try (Connection connection = DatabaseUtil.connect();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            // Set Customer ID in the query
            preparedStatement.setString(1, customerId);
            ResultSet resultSet = preparedStatement.executeQuery();

            // Check if customer exists
            if (resultSet.next()) {
                // Get the stored password and customer name
                String storedPassword = resultSet.getString("Password");
                String customerName = resultSet.getString("Name");

                // Compare the entered password with the stored password
                if (password.equals(storedPassword)) {
                    return customerName; // Return customer name if authentication is successful
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null; // Return null if authentication fails
    }
}