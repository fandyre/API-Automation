import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.response.Response;
import io.restassured.module.jsv.JsonSchemaValidator;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class Json {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static void assertSchema(Response res, String path){
        File schema = new File(path);
        if(!schema.exists()) throw new AssertionError("Schema not found: "+path);
        res.then().assertThat().body(JsonSchemaValidator.matchesJsonSchema(schema));
    }

    public static boolean hasFields(Response res, String... fields){
        try {
            JsonNode root = mapper.readTree(res.asString());
            for (String f: fields) if (!root.has(f)) return false;
            return true;
        } catch (IOException e){
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> load(String path){
        try {
            return mapper.readValue(new File(path), Map.class);
        } catch (IOException e){
            throw new RuntimeException("Cannot load: " + path, e);
        }
    }

    @SuppressWarnings("unchecked")
    public static List<Object> list(String path, String key){
        return (List<Object>) load(path).get(key);
    }


    public static boolean isJson(Response res){
        try {
            mapper.readTree(res.asString()); return true;
        } catch (IOException e) {
            return false;
        }
    }
}