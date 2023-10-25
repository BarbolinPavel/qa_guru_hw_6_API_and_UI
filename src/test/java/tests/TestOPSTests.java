package tests;

import com.codeborne.selenide.Configuration;
import com.github.javafaker.Faker;
import io.restassured.RestAssured;
import models.CreateStepsTestCaseBody;
import models.CreateTestCaseBody;
import models.CreateTestCaseResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Cookie;

import java.util.ArrayList;
import java.util.List;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selectors.byName;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import static io.qameta.allure.Allure.step;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestOPSTests {
    static String login = "allure8",
            password = "allure8",
            projectId = "3721",
            caseID = "26632";

    @BeforeAll
    static void setUp() {
        Configuration.baseUrl = "https://allure.autotests.cloud";
        Configuration.holdBrowserOpen = true;

        RestAssured.baseURI = "https://allure.autotests.cloud";
    }

    @Test
    void createWitUIOnlyTest() {
        Faker faker = new Faker();
        String testCaseName = faker.name().fullName();

        step("Authorize", () -> {
            open("/");
            $(byName("username")).setValue(login);
            $(byName("password")).setValue(password);
            $("button[type='submit']").click();
        });
        step("Go to project", () -> {
            open("/project/2220/test-cases");
        });

        step("Create testcase", () -> {
            $("[data-testid=input__create_test_case]").setValue(testCaseName)
                    .pressEnter();
        });

        step("Verify testcase name", () -> {
            $(".LoadableTree__view").shouldHave(text(testCaseName));
        });
    }

    @Test
    void createWitApiOnlyTest() {
        Faker faker = new Faker();
        String testCaseName = faker.name().fullName();

        CreateTestCaseBody testCaseBody = new CreateTestCaseBody();
        testCaseBody.setName(testCaseName);

        CreateTestCaseResponse createTestCaseResponse = step("Create testcase", () ->
                given()
                        .log().all()
                        .header("X-XSRF-TOKEN", "6a9fa16e-0f22-40ae-9397-b4aaf577c401")
                        .cookies("XSRF-TOKEN", "6a9fa16e-0f22-40ae-9397-b4aaf577c401",
                                "ALLURE_TESTOPS_SESSION", "30dd72f2-5dda-4665-b1ee-092921b52cd9")
                        .contentType("application/json;charset=UTF-8")
                        .body(testCaseBody)
                        .queryParam("projectId", projectId)
                        .when()
                        .post("/api/rs/testcasetree/leaf")
                        .then()
                        .log().status()
                        .log().body()
                        .statusCode(200)
                        .body("statusName", is("Draft"))
                        .body("name", is(testCaseName))
                        .extract().as(CreateTestCaseResponse.class));

        step("Verify testcase name", () -> {
            assertEquals(testCaseName, createTestCaseResponse.getName());
        });
    }

    @Test
    public void addStepsTest(){
        String testCaseStepName = "Step1";

        CreateStepsTestCaseBody.ListStepsData step = new CreateStepsTestCaseBody.ListStepsData();
        step.setName(testCaseStepName);
        step.setSpacing("");

        CreateStepsTestCaseBody stepsTestCaseBody = new CreateStepsTestCaseBody();
        List<CreateStepsTestCaseBody.ListStepsData> listSteps = new ArrayList<>();
        listSteps.add(step);
        stepsTestCaseBody.setSteps(listSteps);
        stepsTestCaseBody.setWorkPath(0);

        step("Create steps in testcase", ()->
        given()
                .log().all()
                .header("X-XSRF-TOKEN", "8d90aab7-1eb0-4423-ba9f-a227fc2ad022")
                .cookies("XSRF-TOKEN", "8d90aab7-1eb0-4423-ba9f-a227fc2ad022",
                        "ALLURE_TESTOPS_SESSION", "1316d17e-8e8e-413e-8fb5-5530c2a18e1d")
                .contentType("application/json;charset=UTF-8")
                .body(stepsTestCaseBody)
                .when()
                .post("/api/rs/testcase/" + caseID + "/scenario")
                .then()
                .log().status()
                .log().body()
                .statusCode(200)
        );

        step("Сheck the added step", ()->{
           open("/favicon.ico");
            Cookie authorizationCookie = new Cookie("ALLURE_TESTOPS_SESSION", "1316d17e-8e8e-413e-8fb5-5530c2a18e1d");
            getWebDriver().manage().addCookie(authorizationCookie);

            open("https://allure.autotests.cloud/project/3721/test-cases/" + caseID);
            $(".Multiline").shouldHave(text(testCaseStepName));
        });
    }
}
