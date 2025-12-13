package io.github.agebhar1.snippets.quarkus.fruit;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.enableLoggingOfRequestAndResponseIfValidationFails;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static io.restassured.http.ContentType.JSON;
import static net.javacrumbs.jsonunit.JsonMatchers.jsonStringEquals;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.text.MatchesPattern.matchesPattern;

@QuarkusTest
@TestProfile(FruitResourceTest.Profile.class)
//@TestTransaction -- rollback changes from @BeforeEach
class FruitResourceTest {

    public static class Profile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.http.proxy.proxy-address-forwarding", "true",
                    "quarkus.http.proxy.enable-forwarded-host", "true",
                    "quarkus.log.json.pretty-print", "false",
                    "quarkus.log.json.log-format", "ecs",
                    "quarkus.log.json.console.enabled", "true",
                    "quarkus.otel.sdk.disabled", "true"
            );
        }
    }

    @BeforeAll
    static void beforeAll() {
        enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @PersistenceContext
    EntityManager em;

    @BeforeEach
    @Transactional
    void beforeEach() {
        if (em != null) {
            var query = """
                    TRUNCATE TABLE fruit;
                    
                    INSERT INTO fruit(id, version, name, description, created_at, updated_at)
                    VALUES ('019b30dc-2ce8-74e8-bd9e-0db463acce58', 1, 'Apple', 'Winter fruit', '2025-12-25T12:47:41+01:00', '2025-12-25T12:47:41+01:00');
                    
                    INSERT INTO fruit(id, version, name, description, created_at, updated_at)
                    VALUES ('019b30dc-bd8a-7de1-858e-382aad5678c1', 1, 'Pineapple', 'Tropical fruit', '2025-12-25T12:47:41+01:00', '2025-12-25T12:47:41+01:00');
                    """;
            em.createNativeQuery(query).executeUpdate();
        }
    }

    @Test
    @DisplayName("GET /fruits - Ok")
    void testFruitsGET() {

        var expected = """
                [
                    {
                        "id": "019b30dc-2ce8-74e8-bd9e-0db463acce58",
                        "name": "Apple",
                        "version": 1,
                        "description": "Winter fruit",
                        "metadata": {
                            "createdAt": "2025-12-25T11:47:41Z",
                            "updatedAt": "2025-12-25T11:47:41Z"
                        }
                    },
                    {
                        "id": "019b30dc-bd8a-7de1-858e-382aad5678c1",
                        "name": "Pineapple",
                        "version": 1,
                        "description": "Tropical fruit",
                        "metadata": {
                            "createdAt": "2025-12-25T11:47:41Z",
                            "updatedAt": "2025-12-25T11:47:41Z"
                        }
                    }
                ]
                """;

        when().get("/fruits")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body(jsonStringEquals(expected));
    }

    @Test
    @DisplayName("GET /fruits/{id} - Ok")
    void testFruitGETbyId() {

        var expected = """
                {
                    "id": "019b30dc-2ce8-74e8-bd9e-0db463acce58",
                    "name": "Apple",
                    "version": 1,
                    "description": "Winter fruit",
                    "metadata": {
                        "createdAt": "2025-12-25T11:47:41Z",
                        "updatedAt": "2025-12-25T11:47:41Z"
                    }
                }
                """;

        when().get("/fruits/019b30dc-2ce8-74e8-bd9e-0db463acce58")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body(jsonStringEquals(expected));
    }

    @Test
    @DisplayName("GET /fruits/{id} - Not Found (w/ RFC 9457 response)")
    void testFruitGETbyIdNotFound() {

        // https://datatracker.ietf.org/doc/html/rfc9457
        var expected = """
                {
                    "status": 404,
                    "title": "Not Found",
                    "detail": "HTTP 404 Not Found",
                    "instance": "/fruits/019b30e0-2919-7b47-a734-f91694fd1afc"
                }
                """;

        when().get("/fruits/019b30e0-2919-7b47-a734-f91694fd1afc")
                .then()
                .statusCode(404)
                .contentType("application/problem+json")
                .body(jsonStringEquals(expected));
    }

    @Test
    @DisplayName("POST /fruits/ - See Other")
    void testFruitPOST() {

        var response = given()
                .contentType(JSON)
                .header("X-Forwarded-Host", "example.com")
                .header("X-Forwarded-Proto", "https")
                .header("X-Forwarded-Port", "443")
                .body("""
                        {
                            "name": "Orange",
                            "description": "Winter fruit"
                        }
                        """)
                .redirects().follow(false)
                .when()
                .post("/fruits")
                .then();

        response.statusCode(303)
                .header("Location", matchesPattern("https://example[.]com/fruits/[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$"));
    }

    @Test
    @DisplayName("POST /fruits/ - $.metadata ignored from request")
    void testFruitPOSTMetadata() {

        var response = given()
                .contentType(JSON)
                .body("""
                        {
                            "name": "Raspberry",
                            "description": "Summer fruit",
                            "metadata": {
                                "createdAt": "1970-01-01T00:00Z",
                                "updatedAt": "1970-01-01T00:00Z"
                            }
                        }
                        """)
                .when()
                .post("/fruits")
                .then();

        var expected = """
                {
                    "id": "${json-unit.any-string}",
                    "name": "Raspberry",
                    "version": 1,
                    "description": "Summer fruit",
                    "metadata": {
                        "createdAt": "${json-unit.any-string}",
                        "updatedAt": "${json-unit.any-string}"
                    }
                }
                """;

        response.statusCode(200)
                .contentType(JSON)
                .body(jsonStringEquals(expected))
                .body("metadata.createdAt", not(equalTo("1970-01-01T00:00Z")))
                .body("metadata.updatedAt", not(equalTo("1970-01-01T00:00Z")));
    }

    @Test
    @DisplayName("POST /fruits/ - Bad Request (w/ RFC 9457 response)")
    void testFruitPOSTBadRequest() {

        var expected = """
                {
                    "status": 400,
                    "title": "Bad Request",
                    "instance": "/fruits",
                    "violations": [
                        {
                            "field": "name",
                            "in": "body",
                            "message": "must not be blank"
                        }
                    ]
                }
                """;

        var response = given()
                .contentType(JSON)
                .body("""
                        {
                             "name": ""
                        }
                        """)
                .redirects().follow(false)
                .when()
                .post("/fruits")
                .then();

        response.statusCode(400)
                .contentType("application/problem+json")
                .body(jsonStringEquals(expected));
    }

    @Test
    @DisplayName("PUT /fruits/{id} - Ok ($.metadata ignored from request)")
    void testFruitPUT() {

        var response = given()
                .contentType(JSON)
                .body("""
                        {
                            "id": "019b30dc-2ce8-74e8-bd9e-0db463acce58",
                            "name": "Apple",
                            "version": 1,
                            "description": "Winter fruit (update)",
                            "metadata": {
                                "createdAt": "1970-01-01T00:00Z",
                                "updatedAt": "1970-01-01T00:00Z"
                            }
                        }
                        """)
                .when()
                .put("/fruits/019b30dc-2ce8-74e8-bd9e-0db463acce58")
                .then();

        var expected = """
                {
                    "id": "019b30dc-2ce8-74e8-bd9e-0db463acce58",
                    "version": 2,
                    "name": "Apple",
                    "description": "Winter fruit (update)",
                    "metadata": {
                        "createdAt": "2025-12-25T11:47:41Z",
                        "updatedAt": "${json-unit.any-string}"
                    }
                }
                """;

        response.statusCode(200)
                .contentType(JSON)
                .body(jsonStringEquals(expected))
                .body("metadata.updatedAt", not(equalTo("2025-12-25T11:47:41Z")));
    }

    @Test
    @DisplayName("PUT /fruits/{id} - Conflict (w/ RFC 9457 response) - optimistic locking")
    void testFruitPUTConflict() {

        var response = given()
                .contentType(JSON)
                .body("""
                        {
                            "id": "019b30dc-bd8a-7de1-858e-382aad5678c1",
                            "name": "Pineapple",
                            "version": 2,
                            "description": "Tropical fruit (update)"
                        }
                        """)
                .when()
                .put("/fruits/019b30dc-bd8a-7de1-858e-382aad5678c1")
                .then();

        var expected = """
                {
                    "status": 409,
                    "title": "Conflict",
                    "detail": "HTTP 409 Conflict",
                    "instance": "/fruits/019b30dc-bd8a-7de1-858e-382aad5678c1"
                }
                """;

        response.statusCode(409)
                .contentType("application/problem+json")
                .body(jsonStringEquals(expected));
    }

    @Test
    @DisplayName("DELETE /fruits/{id} - No Content")
    void testFruitDELETE() {
        when().delete("/fruits/019b30dc-2ce8-74e8-bd9e-0db463acce58")
                .then()
                .statusCode(204)
                .body(equalTo(""));
    }

    @Test
    @DisplayName("DELETE /fruits/{id} - Not Found (w/ RFC 9457 response)")
    void testFruitDELETENotFound() {

        // https://datatracker.ietf.org/doc/html/rfc9457
        var expected = """
                {
                    "status": 404,
                    "title": "Not Found",
                    "detail": "HTTP 404 Not Found",
                    "instance": "/fruits/0"
                }
                """;

        when().delete("/fruits/0")
                .then()
                .statusCode(404)
                .body(jsonStringEquals(expected));
    }
}
