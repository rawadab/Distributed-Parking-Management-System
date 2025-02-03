package mulligan1;

import com.example.queries.ParkingService;
import com.example.recommender.ConsensusProtocol;
import com.example.recommender.RaftNode;
import com.example.recommender.RecommendationResponse;
import com.example.shared.models.ParkingEvent;
import com.example.shared.utils.DatabaseUtil;
import com.example.shared.utils.RabbitMQUtil;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the parking and recommendation system.
 * These tests cover various scenarios such as starting and stopping parking,
 * handling active parking events, retrieving completed parking events, and
 * testing the recommender system under failure conditions.
 *
 * <p>
 * Tests assume the use of a pre-configured database accessible via MySQL.
 * A valid customer ID and vehicle ID are used to simulate operations.
 * </p>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class customerTests {

    private ParkingService parkingService;
    private final int CUSTOMER_ID = 315281964; // Test customer ID
    private int VEHICLE_ID; // Vehicle ID associated with the test customer

    private ConsensusProtocol protocol;
    private List<RaftNode> raftNodes;
    private List<RabbitMQUtil> rabbitMQNodes;

    /**
     * Initializes the ParkingService and the recommender system.
     * This setup runs once before all test methods.
     *
     * @throws Exception if there is an error initializing the service or retrieving the vehicle ID.
     */
    @BeforeAll
    void setUp() throws Exception {
        parkingService = new ParkingService();
        VEHICLE_ID = parkingService.getVehicleNumber(CUSTOMER_ID);

        // Set up mock nodes for recommender system tests
        RabbitMQUtil mockRabbit1 = mock(RabbitMQUtil.class);
        RabbitMQUtil mockRabbit2 = mock(RabbitMQUtil.class);
        RabbitMQUtil mockRabbit3 = mock(RabbitMQUtil.class);

        RaftNode node1 = mock(RaftNode.class);
        RaftNode node2 = mock(RaftNode.class);
        RaftNode node3 = mock(RaftNode.class);

        raftNodes = Arrays.asList(node1, node2, node3);
        rabbitMQNodes = Arrays.asList(mockRabbit1, mockRabbit2, mockRabbit3);

        protocol = new ConsensusProtocol(raftNodes, rabbitMQNodes);
    }

    // --- Existing Parking Tests ---

    @Test
    void startParking_validInput() {
        try (Connection conn = DatabaseUtil.connect()) {
            if (!parkingService.validateParkingSpace(conn, "98")) {
                parkingService.UpdateParkingSpaceToBeFree(conn, "98");
            }
            boolean actual = parkingService.startParking(VEHICLE_ID, "98");
            assertTrue(actual, "Expected parking to start successfully.");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void startParking_invalidSpace() {
        boolean result = parkingService.startParking(VEHICLE_ID, "");
        assertFalse(result, "Parking should not start for an empty space ID.");
    }

    @Test
    void stopParking_noActiveEvent() {
        try (Connection conn = DatabaseUtil.connect()) {
            if (parkingService.vehicleHasActiveEvent(conn, VEHICLE_ID)) {
                parkingService.stopParking(VEHICLE_ID);
            }

            boolean result = parkingService.stopParking(VEHICLE_ID);
            assertFalse(result, "Expected stopParking to fail when no active event exists.");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // --- New Recommender System Tests ---

    /**
     * Tests the case where one recommender server returns an incorrect answer.
     * The majority should still determine the correct recommendation.
     */
    @Test
    void testOneRecommenderServerReturnsIncorrectAnswer() {
        when(rabbitMQNodes.get(0).fetchAndRepublishMessagesCitation(anyString()))
                .thenReturn(Collections.singletonList("RESPONSE::Node=A,Rec=SpaceID=5,Citations=1"));
        when(rabbitMQNodes.get(1).fetchAndRepublishMessagesCitation(anyString()))
                .thenReturn(Collections.singletonList("RESPONSE::Node=B,Rec=SpaceID=5,Citations=1"));
        when(rabbitMQNodes.get(2).fetchAndRepublishMessagesCitation(anyString()))
                .thenReturn(Collections.singletonList("RESPONSE::Node=C,Rec=SpaceID=99,Citations=99"));

        List<RecommendationResponse> responses = protocol.collectResponses();
        System.out.println("responses " + responses);
        String consensus = protocol.getConsensusDecision(responses);
        System.out.println("consensus " + consensus);
        assertEquals("SpaceID=5,Citations=1", consensus, "Consensus should reject the incorrect response.");
    }

    /**
     * Tests the case where two recommender servers return incorrect answers.
     * The remaining correct node should still determine the consensus.
     */
    @Test
    void testTwoRecommenderServersReturnIncorrectAnswers() {
        when(rabbitMQNodes.get(0).fetchAndRepublishMessagesCitation(anyString()))
                .thenReturn(Collections.singletonList("RESPONSE::Node=A,Rec=SpaceID=3,Citations=0"));
        when(rabbitMQNodes.get(1).fetchAndRepublishMessagesCitation(anyString()))
                .thenReturn(Collections.singletonList("RESPONSE::Node=B,Rec=SpaceID=99,Citations=99"));
        when(rabbitMQNodes.get(2).fetchAndRepublishMessagesCitation(anyString()))
                .thenReturn(Collections.singletonList("RESPONSE::Node=C,Rec=SpaceID=100,Citations=50"));

        List<RecommendationResponse> responses = protocol.collectResponses();
        System.out.println("responses " + responses);
        String consensus = protocol.getConsensusDecision(responses);
        System.out.println("consensus " + consensus);
        assertEquals(null, consensus, "Consensus should return null");
    }

    @Test
    void testThreeRecommenderServersReturnCorrectAnswers() {
        when(rabbitMQNodes.get(0).fetchAndRepublishMessagesCitation(anyString()))
                .thenReturn(Collections.singletonList("RESPONSE::Node=A,Rec=SpaceID=5,Citations=1"));
        when(rabbitMQNodes.get(1).fetchAndRepublishMessagesCitation(anyString()))
                .thenReturn(Collections.singletonList("RESPONSE::Node=B,Rec=SpaceID=5,Citations=1"));
        when(rabbitMQNodes.get(2).fetchAndRepublishMessagesCitation(anyString()))
                .thenReturn(Collections.singletonList("RESPONSE::Node=C,Rec=SpaceID=5,Citations=1"));

        List<RecommendationResponse> responses = protocol.collectResponses();
        System.out.println("responses " + responses);
        String consensus = protocol.getConsensusDecision(responses);
        System.out.println("consensus " + consensus);
        assertEquals("SpaceID=5,Citations=1", consensus, "Consensus should choose the correct majority response.");
    }

    @Test
    void testOneRecommenderServersNoResponse() {
        when(rabbitMQNodes.get(0).fetchAndRepublishMessagesCitation(anyString()))
                .thenReturn(Collections.singletonList("RESPONSE::Node=A,Rec=SpaceID=5,Citations=1"));
        when(rabbitMQNodes.get(1).fetchAndRepublishMessagesCitation(anyString()))
                .thenReturn(Collections.singletonList("RESPONSE::Node=B,Rec=SpaceID=5,Citations=1"));


        List<RecommendationResponse> responses = protocol.collectResponses();
        System.out.println("responses " + responses);
        String consensus = protocol.getConsensusDecision(responses);
        System.out.println("consensus " + consensus);
        assertEquals("SpaceID=5,Citations=1", consensus, "Consensus should choose the correct majority response.");
    }

    @Test
    void testTwoRecommenderServersNoResponse() {
        when(rabbitMQNodes.get(0).fetchAndRepublishMessagesCitation(anyString()))
                .thenReturn(Collections.singletonList("RESPONSE::Node=A,Rec=SpaceID=5,Citations=1"));


        List<RecommendationResponse> responses = protocol.collectResponses();
        System.out.println("responses " + responses);
        String consensus = protocol.getConsensusDecision(responses);
        System.out.println("consensus " + consensus);
        assertEquals(null, consensus, "Consensus should choose the correct majority response.");
    }

    @Test
    void testOneRecommenderServersNoResponseAndTwoRecommenderServersReturnIncorrectAnswers() {
        when(rabbitMQNodes.get(0).fetchAndRepublishMessagesCitation(anyString()))
                .thenReturn(Collections.singletonList("RESPONSE::Node=A,Rec=SpaceID=5,Citations=1"));
        when(rabbitMQNodes.get(1).fetchAndRepublishMessagesCitation(anyString()))
                .thenReturn(Collections.singletonList("RESPONSE::Node=B,Rec=SpaceID=8,Citations=3"));


        List<RecommendationResponse> responses = protocol.collectResponses();
        System.out.println("responses " + responses);
        String consensus = protocol.getConsensusDecision(responses);
        System.out.println("consensus " + consensus);
        assertEquals(null, consensus, "Consensus should choose the correct majority response.");
    }


}