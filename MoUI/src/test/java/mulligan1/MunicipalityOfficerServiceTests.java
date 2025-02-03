package mulligan1;

import com.example.queries.MunicipalityOfficerService;
import com.example.queries.PEOService;
import com.example.queries.ParkingService;
import com.example.shared.models.Citation;
import com.example.shared.models.Transaction;
import com.example.shared.utils.DatabaseUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for MunicipalityOfficerService.
 * This class tests the generation of transaction and citation reports.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MunicipalityOfficerServiceTests {

    private MunicipalityOfficerService municipalityOfficerService;
    private ParkingService parkingService;
    private PEOService peoService;

    /**
     * Initializes services before all test cases.
     *
     * @throws Exception if an error occurs during initialization.
     */
    @BeforeAll
    void setUp() throws Exception {
        municipalityOfficerService = new MunicipalityOfficerService();
        parkingService = new ParkingService();
        peoService = new PEOService();
    }

    /**
     * Tests that transactions are generated and included in the report when there are transactions.
     *
     * @throws Exception if a database error occurs.
     */
    @Test
    void getTransactionsReport_thereAreTransactions() throws Exception {
        try (Connection conn = DatabaseUtil.connect()) {
            // Ensure parking space "98" is free for the test
            if (!parkingService.validateParkingSpace(conn, "98")) {
                parkingService.UpdateParkingSpaceToBeFree(conn, "98");
            }

            // Simulate a parking session
            parkingService.startParking(201, "98");
            parkingService.stopParking(201);

            // Generate the transaction report
            List<Transaction> transactions = municipalityOfficerService.generateTransactionReport();

            // Verify the report is not empty
            assertFalse(transactions.isEmpty(), "Expected transactions in the queue.");
        } catch (SQLException e) {
            throw new RuntimeException("Error during database interaction", e);
        }
    }

    /**
     * Tests that no transactions are included in the report when there are no transactions.
     *
     * @throws Exception if a database error occurs.
     */
    @Test
    void getTransactionsReport_noTransactions() throws Exception {
        // Ensure no transactions exist before running the test
        municipalityOfficerService.generateTransactionReport();

        // Generate the transaction report
        List<Transaction> transactions = municipalityOfficerService.generateTransactionReport();

        // Verify the report is empty
        assertTrue(transactions.isEmpty(), "Expected no transactions in the queue.");
    }

    /**
     * Tests that citations are generated and included in the report when there are citations.
     *
     * @throws Exception if a database error occurs.
     */
    @Test
    void getCitationsReport_thereAreCitations() throws Exception {
        boolean legallyParked = peoService.checkIfLegallyParked("202", "100");

        if (!legallyParked) {
            // Issue a citation for illegal parking
            Citation citation = new Citation(
                    peoService.generateUniqueCitationId(),
                    "202",
                    "100",
                    peoService.getZoneIdBySpaceId("100"),
                    500000,
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            );
            peoService.issueCitation(citation);

            // Generate the citation report
            List<Citation> citations = municipalityOfficerService.generateCitationReport();

            // Verify the report is not empty
            assertFalse(citations.isEmpty(), "Expected citations in the queue.");
        }
    }

    /**
     * Tests that no citations are included in the report when there are no citations.
     */
    @Test
    void getCitationsReport_noCitations() {
        // Ensure no citations exist before running the test
        municipalityOfficerService.generateCitationReport();

        // Generate the citation report
        List<Citation> citations = municipalityOfficerService.generateCitationReport();

        // Verify the report is empty
        assertTrue(citations.isEmpty(), "Expected no citations in the citations queue after consuming all.");
    }
}