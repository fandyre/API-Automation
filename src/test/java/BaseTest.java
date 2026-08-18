import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

public abstract class BaseTest {

    protected static final String DATA = "src/test/resources/data/";
    protected static final String SCHEMA = "src/test/resources/schema/";

    protected static List<String> validStatuses;
    protected static List<String> invalidStatuses;
    protected static List<Object> unknownPetIds;
    protected static List<Object> unknownOrderIds;
    protected static List<String> nonNumericIds;

    protected static Map<String, Object> petTemplate;
    protected static List<String> malformedBodies;

    /** Every new pet created, will be deleted again in @AfterAll**/
    private static final List<Object> createdPets = new ArrayList<>();

    @SuppressWarnings("unchecked")
    protected static List<String> toStringList(List<Object> list) {
        return (List<String>) (List<?>) list;
    }

    protected static Map<String, Object> asMap(Object o) {
        return (Map<String, Object>) o;
    }

    @BeforeAll
    @SuppressWarnings("unchecked")
    static void loadData(){
        validStatuses = toStringList(Json.list(DATA + "common.json", "valid_statuses"));
        invalidStatuses = toStringList(Json.list(DATA + "common.json", "invalid_statuses"));
        unknownPetIds = Json.list(DATA + "common.json", "unknown_pet_ids");
        unknownOrderIds = Json.list(DATA + "common.json", "unknown_order_ids");
        nonNumericIds = toStringList(Json.list(DATA + "common.json", "non_numeric_ids"));

        Map<String, Object> pets = Json.load(DATA + "pets.json");
        petTemplate = asMap(pets.get("template"));
        malformedBodies = toStringList((List<Object>) pets.get("malformed_bodies"));
    }

    @AfterAll
    static void cleanUp(){
        for (Object id: createdPets){
            try {
                Api.deletePet(id);
            } catch (Exception ignored) { }
        }
        createdPets.clear();
    }

    // Need usage in PetTest later
    protected abstract String schema();

    // Assertions
    protected void assertOk(Response res){
        assertNotNull(res);
        assertEquals(200, res.statusCode(), "Unexpected 200 but body was: " + res.asString());
    }

    protected void assertStatus(Response res, int expected){
        assertNotNull(res);
        assertEquals(expected, res.statusCode(), "Unexpected status. Body response was: " + res.asString());
    }

    protected void assertStatusIn(Response res, int... allowed){
        for(int code: allowed){
            if (res.statusCode() == code)return;
        }
        fail("Status " + res.statusCode() + " was not of " + java.util.Arrays.toString(allowed) + ". Body was: " + res.asString());
    }

    protected void assertNotSuccess(Response res){
        assertTrue(res.statusCode() >= 400, "A rejected request must not return 2xx, got "+ res.statusCode());
    }

    protected void assertJsonContentType(Response res){
        assertTrue(res.getContentType() != null && res.getContentType().toLowerCase().contains("json"),
                "Expected a JSON content type, got: " + res.getContentType());
    }

    protected void assertSchema(Response res){
        Json.assertSchema(res, schema());
    }

    protected void assertSchema(Response res, String path){
        Json.assertSchema(res, path);
    }

    protected void assertErrorBody(Response res, String expectedType, String expectedMessage){
        assertSchema(res, SCHEMA + "error-schema.json");
        assertEquals(expectedType, res.jsonPath().getString("type"), "error 'type'");
        assertEquals(expectedMessage, res.jsonPath().getString("message"), "error 'message'");
    }

    //Request Helpers
    //Unique name per run
    protected Map<String, Object> newPet(String name, String status){
        Map<String, Object> pet = new HashMap<>(petTemplate);
        // petstore.swagger.io is a public shared sandbox; leaving "id" for the server to
        // auto-assign risks colliding with another client's pet created at the same time.
        // Picking our own id here avoids reading back someone else's data.
        pet.put("id", System.currentTimeMillis() * 1000 + ThreadLocalRandom.current().nextInt(1000));
        pet.put("name", name + "-" + System.currentTimeMillis());
        pet.put("status", status);
        return pet;
    }

    //Create pet & assert it worked
    protected long createPet(String name, String status){
        Response res = Api.createPet(newPet(name, status));
        assertOk(res);
        long id = res.jsonPath().getLong("id");
        assertTrue(id > 0, "Expected a generated pet id, got " + id);
        createdPets.add(id);
        return id;
    }

    protected static void sleep(long millis){
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e){
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Reads a pet, retrying briefly on 404. The public sandbox is eventually consistent:
     * a read straight after a write is occasionally not yet visible. This is a deliberate
     * mitigation for that environment, not a workaround for a product defect.
     */
    protected Response getPetWithRetry(Object petId){
        Response res = Api.getPet(petId);
        for (int attempt = 0; attempt < 3 && res.statusCode() == 404; attempt++){
            sleep(1000);
            res = Api.getPet(petId);
        }
        return res;
    }

    protected Response getOrderWithRetry(Object orderId){
        Response res = Api.getOrder(orderId);
        for (int attempt = 0; attempt < 3 && res.statusCode() == 404; attempt++){
            sleep(1000);
            res = Api.getOrder(orderId);
        }
        return res;
    }

    protected Map<String, Object> newOrder(long petId, int quantity){
        Map<String, Object> order = new HashMap<>();
        order.put("petId", petId);
        order.put("quantity", quantity);
        order.put("status", "placed");
        order.put("complete", false);
        return order;
    }

}