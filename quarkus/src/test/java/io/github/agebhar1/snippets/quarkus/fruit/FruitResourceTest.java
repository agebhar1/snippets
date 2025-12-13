package io.github.agebhar1.snippets.quarkus.fruit;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static net.javacrumbs.jsonunit.JsonMatchers.jsonStringEquals;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.equalTo;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FruitResourceTest {

    @Test
    @Order(1)
    @DisplayName("GET /fruits")
    void testFruitsGET() {

        var expected = """
                [
                    {
                        "id": 1,
                        "name": "Apple",
                        "description": "Winter fruit"
                    },
                    {
                        "id": 2,
                        "name": "Pineapple",
                        "description": "Tropical fruit"
                    }
                ]
                """;

        given()
                .when()
                .get("/fruits")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body(jsonStringEquals(expected));
    }

    @Test
    @Order(2)
    @DisplayName("GET /fruits/{id}")
    void testFruitGETbyId() {

        var expected = """
                {
                    "id": 1,
                    "name": "Apple",
                    "description": "Winter fruit"
                }
                """;

        given()
                .when()
                .get("/fruits/1")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body(jsonStringEquals(expected));
    }

    @Test
    @Order(3)
    @DisplayName("GET /fruits/{id} - Not Found with RFC 9457 response")
    void testFruitGETbyIdNotFound() {

        // https://datatracker.ietf.org/doc/html/rfc9457
        var expected = """
                {
                    "status": 404,
                    "title": "Not Found",
                    "detail": "HTTP 404 Not Found",
                    "instance": "/fruits/0"
                }
                """;

        given()
                .when()
                .get("/fruits/0")
                .then()
                .statusCode(404)
                .contentType("application/problem+json")
                .body(jsonStringEquals(expected));
    }

    @Test
    @Order(4)
    @DisplayName("POST /fruits/")
    void testFruitPOST() {

        var response = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "Orange",
                            "description": "Winter fruit"
                        }
                        """)
                .redirects().follow(false)
                .post("/fruits");

        response.then()
                .statusCode(303)
                .header("Location", endsWith("/fruits/3"));
    }

    @Test
    @Order(5)
    @DisplayName("POST /fruits/ - Bad Request")
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
                            "message": "darf nicht leer sein"
                        }
                    ]
                }
                """;

        var response = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                             "name": ""
                        }
                        """)
                .redirects().follow(false)
                .post("/fruits");

        response.then()
                .statusCode(400)
                .contentType("application/problem+json")
                .body(jsonStringEquals(expected));
    }

    @Test
    @Order(6)
    @DisplayName("DELETE /fruits/{id}")
    void testFruitDELETE() {
        given()
                .when()
                .delete("/fruits/1")
                .then()
                .statusCode(204)
                .body(equalTo(""));
    }

    @Test
    @Order(7)
    @DisplayName("DELETE /fruits/{id} - Not Found with RFC 9457 response")
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

        given()
                .when()
                .delete("/fruits/0")
                .then()
                .statusCode(404)
                .body(jsonStringEquals(expected));
    }

}