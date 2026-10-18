import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class Api {

    static {
        RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());
    }

    public static final String BASE = "https://petstore.swagger.io/v2";

    public static final String PET = "/pet";
    public static final String PET_BY_ID = "/pet/{petId}";
    public static final String FIND_BY_STATUS = "/pet/findByStatus";
    public static final String UPLOAD_IMAGE = "/pet/{petId}/uploadImage";
    public static final String ORDER = "/store/order";
    public static final String ORDER_BY_ID = "/store/order/{orderId}";
    public static final String INVENTORY = "/store/inventory";

    public static final int TIMEOUT_MS = 15000;

    public static final String[] PET_STATUSES = {"available", "pending", "sold"};

    private static RequestSpecification base(){
        return RestAssured.given()
                .baseUri(BASE)
                .contentType("application/json")
                .accept("application/json");
    }

    public static class Request {
        private final String endpoint;
        private final Map<String, Object> params = new HashMap<>();
        private final Map<String, Object> paths = new HashMap<>();
        private Object body;

        public Request(String endpoint) { this.endpoint = endpoint; }

        public Request param(String k, Object v) {
            params.put(k, v);
            return this;
        }
        public Request path(String k, Object v) {
            paths.put(k, v); return this;
        }
        public Request body(Object v) {
            this.body= v; return this;
        }
        public Request status(String v) {
            params.put("status", v); return this;
        }

        private RequestSpecification build(){
            var req = base();
            if(!params.isEmpty()) req.queryParams(params);
            if(!paths.isEmpty()) req.pathParams(paths);
            if(body != null) req.body(body);
            return req;
        }

        public Response get() {return build().get(endpoint);}
        public Response post() {return build().post(endpoint);}
        public Response put() {return build().put(endpoint);}
        public Response delete() {return build().delete(endpoint);}
    }

    // ===== SHORTCUTS =====
    public static Response createPet(Object body){
        return new Request(PET).body(body).post();
    }

    public static Response getPet(Object petId){
        return new Request(PET_BY_ID).path("petId", petId).get();
    }

    public static Response updatePet(Object body) {
        return new Request(PET).body(body).put();
    }

    public static Response deletePet(Object petId) {
        return new Request(PET_BY_ID).path("petId", petId).delete();
    }

    public static Response findByStatus(String status) {
        return new Request(FIND_BY_STATUS).status(status).get();
    }

    public static Response uploadImage(Object petId, Map<String, Object> body){
        String additionalMetadata = (String) body.get("additionalMetadata");
        File file = new File((String) body.get("file"));

        return RestAssured.given()
                .baseUri(BASE)
                .accept("application/json")
                .pathParam("petId", petId)
                .multiPart("additionalMetadata", additionalMetadata)
                .multiPart("file", file, "image/jpeg")
                .post(UPLOAD_IMAGE);
    }

    public static Response placeOrder(Object body) {
        return new Request(ORDER).body(body).post();
    }

    public static Response getOrder(Object orderId) {
        return new Request(ORDER_BY_ID).path("orderId", orderId).get();
    }
}