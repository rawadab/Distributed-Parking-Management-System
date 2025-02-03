package com.example.queries;



import com.example.shared.models.Citation;
import com.example.shared.models.Transaction;
import com.example.shared.utils.RabbitMQUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Service class for handling municipality officer-related operations.
 * This class provides functionality for generating transaction and citation reports
 * by consuming messages from RabbitMQ queues and parsing the data into model objects.
 * The service interacts with {@link RabbitMQUtil} for message consumption and parses
 * messages into {@link Transaction} and {@link Citation} objects.
 * @version 7
 */
public class MunicipalityOfficerService {

    /** Utility class for interacting with RabbitMQ */
    private final RabbitMQUtil rabbitMQUtil = new RabbitMQUtil();

    /**
     * Generates a report of all transactions by consuming messages from the "transactionsQueue".
     * Parses each message into a {@link Transaction} object and returns a list of transactions.
     *
     * @return a list of transactions parsed from the messages
     */
    public ArrayList<Transaction> generateTransactionReport() {
        List<String> messages = rabbitMQUtil.fetchAndRepublishMessagesCitation("transactionsQueue");
        ArrayList<Transaction> transactions = new ArrayList<>();
        for (String message : messages) {
            Transaction transaction = parseTransactionMessage(message);
            transactions.add(transaction);
        }
        return transactions;
    }

    /**
     * Parses a message string into a {@link Transaction} object.
     *
     * @param message the message string to parse
     * @return a {@link Transaction} object with data from the message
     */
    private Transaction parseTransactionMessage(String message) {
        String[] parts = message.split(", ");
        return new Transaction(
                parts[0].split(": ")[1], // VehicleID
                parts[1].split(": ")[1], // ZoneName
                parts[2].split(": ")[1], // SpaceID
                parts[3].split(": ")[1], // StartTime
                parts[4].split(": ")[1], // EndTime
                parts[5].split(": ")[1]  // TotalCost
        );
    }

    /**
     * Generates a report of all citations by consuming messages from the "citationsQueue".
     * Parses each message into a {@link Citation} object and returns a list of citations.
     *
     * @return a list of citations parsed from the messages
     */
    public ArrayList<Citation> generateCitationReport() {
        List<String> messages = rabbitMQUtil.fetchAndRepublishMessagesCitation("citationsQueue");
        ArrayList<Citation> citations = new ArrayList<>();
        for (String message : messages) {
            Citation citation = parseCitationMessage(message);
            citations.add(citation);
        }
        return citations;
    }

    /**
     * Parses a message string into a {@link Citation} object.
     *
     * @param message the message string to parse
     * @return a {@link Citation} object with data from the message
     */
    private Citation parseCitationMessage(String message) {
        String[] parts = message.split(", ");
        return new Citation(
                parts[0].split(": ")[1], // VehicleID
                parts[1].split(": ")[1], // SpaceID
                parts[2].split(": ")[1], // ParkingZone
                parts[3].split(": ")[1], // inspectionTimes
                Double.parseDouble(parts[4].split(": ")[1]) // totalCost as a Double
        );
    }
}
