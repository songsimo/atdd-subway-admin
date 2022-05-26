package nextstep.subway.station;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("지하철역 관련 기능")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class StationAcceptanceTest {
    @LocalServerPort
    int port;

    @BeforeEach
    public void setUp() {
        if (RestAssured.port == RestAssured.UNDEFINED_PORT) {
            RestAssured.port = port;
        }
    }

    /**
     * When 지하철역을 생성하면
     * Then 지하철역이 생성된다
     * Then 지하철역 목록 조회 시 생성한 역을 찾을 수 있다
     */
    @DisplayName("지하철역을 생성한다.")
    @Test
    void createStation() {
        // when
        Map<String, String> params = new HashMap<>();
        params.put("name", "강남역");

        ExtractableResponse<Response> response =
                RestAssured.given().log().all()
                           .body(params)
                           .contentType(MediaType.APPLICATION_JSON_VALUE)
                           .when().post("/stations")
                           .then().log().all()
                           .extract();

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());

        // then
        List<String> stationNames =
                RestAssured.given().log().all()
                           .when().get("/stations")
                           .then().log().all()
                           .extract().jsonPath().getList("name", String.class);
        assertThat(stationNames).containsAnyOf("강남역");
    }

    /**
     * Given 지하철역을 생성하고
     * When 기존에 존재하는 지하철역 이름으로 지하철역을 생성하면
     * Then 지하철역 생성이 안된다
     */
    @DisplayName("기존에 존재하는 지하철역 이름으로 지하철역을 생성한다.")
    @Test
    void createStationWithDuplicateName() {
        // given
        Map<String, String> params = new HashMap<>();
        params.put("name", "강남역");

        RestAssured.given().log().all()
                   .body(params)
                   .contentType(MediaType.APPLICATION_JSON_VALUE)
                   .when().post("/stations")
                   .then().log().all();

        // when
        ExtractableResponse<Response> response =
                RestAssured.given().log().all()
                           .body(params)
                           .contentType(MediaType.APPLICATION_JSON_VALUE)
                           .when().post("/stations")
                           .then().log().all()
                           .extract();

        // then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    /**
     * Given 2개의 지하철역을 생성하고
     * When 지하철역 목록을 조회하면
     * Then 2개의 지하철역을 응답 받는다
     */
    @DisplayName("지하철역을 조회한다.")
    @Test
    void getStations() {
        // given
        List<String> names = Arrays.asList("강남역", "서울역");

        for (String name : names) {
            // when
            Map<String, String> params = new HashMap<>();
            params.put("name", name);

            ExtractableResponse<Response> response =
                    RestAssured.given().log().all()
                               .body(params)
                               .contentType(MediaType.APPLICATION_JSON_VALUE)
                               .when().post("/stations")
                               .then().log().all()
                               .extract();

            assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        }

        // then
        List<String> stationNames =
                RestAssured.given().log().all()
                           .when().get("/stations")
                           .then().log().all()
                           .extract().jsonPath().getList("name", String.class);

        assertThat(stationNames).containsExactly("강남역", "서울역");
        assertThat(stationNames).hasSize(names.size());
    }

    /**
     * Given 지하철역을 생성하고
     * When 그 지하철역을 삭제하면
     * Then 그 지하철역 목록 조회 시 생성한 역을 찾을 수 없다
     */
    @DisplayName("지하철역을 제거한다.")
    @Test
    void deleteStation() {
        // given
        Map<String, String> params = new HashMap<>();
        params.put("name", "강남역");

        ExtractableResponse<Response> created =
                RestAssured.given().log().all()
                           .body(params)
                           .contentType(MediaType.APPLICATION_JSON_VALUE)
                           .when().post("/stations")
                           .then().log().all()
                           .extract();

        assertThat(created.statusCode()).isEqualTo(HttpStatus.CREATED.value());

        // when
        ExtractableResponse<Response> deleted =
                RestAssured.given().log().all()
                           .when().delete("/stations/{id}", created.jsonPath().getLong("id"))
                           .then().log().all()
                           .extract();

        assertThat(deleted.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());

        // then
        List<String> stationNames =
                RestAssured.given().log().all()
                           .when().get("/stations")
                           .then().log().all()
                           .extract().jsonPath().getList("name", String.class);

        assertThat(stationNames.contains("강남역")).isFalse();
    }
}
