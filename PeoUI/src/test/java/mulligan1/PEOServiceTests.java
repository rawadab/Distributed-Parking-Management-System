package mulligan1;

import com.example.queries.PEOService;
import com.example.queries.ParkingService;
import com.example.shared.models.Citation;
import com.example.shared.utils.DatabaseUtil;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PEOService.
 * This class tests functionality related to checking legal parking and issuing citations.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PEOServiceTests {

    private PEOService peoService;
    private ParkingService parkingService;
    private final int CUSTOMER_ID = 315281964; // Test customer ID
    private int VEHICLE_ID; // Vehicle ID associated with the test customer

    /**
     * Initializes services and retrieves the vehicle ID for the test customer.
     *
     * @throws Exception if an error occurs during initialization.
     */
    @BeforeAll
    void setUp() throws Exception {
        peoService = new PEOService();
        parkingService = new ParkingService();
        VEHICLE_ID = parkingService.getVehicleNumber(CUSTOMER_ID);
    }

    /**
     * Tests that a vehicle is legally parked in a valid parking space.
     *
     * @throws Exception if a database error occurs.
     */
    @Test
    void checkIfLegallyParked_Success() throws Exception {
        try (Connection conn = DatabaseUtil.connect()) {
            // Ensure the parking space "80" is available and start parking
            if (parkingService.validateParkingSpace(conn, "91")) {
                parkingService.startParking(VEHICLE_ID, "91");
            }
        }

        // Check if the vehicle is legally parked
        boolean result = peoService.checkIfLegallyParked("" + VEHICLE_ID, "80");
        assertTrue(result, "Vehicle should be legally parked.");
    }

    /**
     * Tests issuing a citation for a vehicle that is not legally parked.
     *
     * @throws Exception if a database error occurs.
     */
    @Test
    void issueCitation_notLegallyParked() throws Exception {
        String parkingSpaceId = "95"; // Space where the vehicle is not legally parked
        double citationCost = 500; // Citation cost for illegal parking

        // Check if the vehicle is legally parked
        boolean result = peoService.checkIfLegallyParked("" + VEHICLE_ID, parkingSpaceId);

        // If not legally parked, issue a citation
        if (!result) {
            Citation citation = new Citation(
                    peoService.generateUniqueCitationId(),
                    "" + VEHICLE_ID,
                    parkingSpaceId,
                    peoService.getZoneIdBySpaceId(parkingSpaceId),
                    citationCost,
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            );
            peoService.issueCitation(citation);
        }

        assertFalse(result, "Vehicle should not be legally parked.");
    }
}