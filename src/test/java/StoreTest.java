import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Store API")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StoreTest extends BaseTest{
    protected String schema() {return SCHEMA + "order-schema.json";}

    static Stream<Object> unknownOrders() {return unknownOrderIds.stream();}

    //POSITIVE CASE

    @Test @Order(1)
    @DisplayName("API #TC 09 -  Positive: When POST /store/order, Then the order is placed and readable")
    void placeAndRetrieveOrderTest(){
        long petId = createPet("AutoPet-Order", "available");

        Map<String, Object> order = newOrder(petId, 3);
        Response placed = Api.placeOrder(order);

        assertOk(placed);
        assertJsonContentType(placed);
        assertEquals(3, placed.jsonPath().getInt("quantity"));
        assertEquals("placed", placed.jsonPath().getString("status"));
        assertSchema(placed);

        long orderId = placed.jsonPath().getLong("id");
        assertTrue(orderId > 0, "Expected a generated order id");

        Response fetched = getOrderWithRetry(orderId);
        assertOk(fetched);
        assertEquals(orderId, fetched.jsonPath().getLong("id"));
        assertEquals(petId, fetched.jsonPath().getLong("petId"), "The order must point back at the pet it was placed for");
        assertSchema(fetched);
    }


    //NEGATIVE CASE
    @ParameterizedTest @Order(2) @MethodSource("unknownOrders")
    @DisplayName("API #TC 10 - Negative: unknown order id returns 404 Order not found")
    void unknownOrderIdTest(Object orderId){
        Response res = Api.getOrder(orderId);

        assertStatus(res, 404);
        assertErrorBody(res, "error", "Order not found");
    }
}
