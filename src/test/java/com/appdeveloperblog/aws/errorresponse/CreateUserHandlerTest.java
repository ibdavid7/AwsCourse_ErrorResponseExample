package com.appdeveloperblog.aws.errorresponse;


import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.appdeveloperblog.aws.errorresponse.service.CognitoUserService;
import com.appdeveloperblog.aws.errorresponse.shared.Constants;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.http.SdkHttpResponse;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CreateUserHandlerTest {

    @Mock
    CognitoUserService cognitoUserService;

    @Mock
    APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent;

    @Mock
    Context context;

    @Mock
    LambdaLogger logger;

    @InjectMocks
    CreateUserHandler createUserHandler;

    @BeforeEach
    public void runBeforeEachTestMethod() {
        System.out.println("Executing @BeforeEach Test Method");
        // Context
        when(context.getLogger()).thenReturn(logger);
    }

    @AfterEach
    public void runAfterEachTestMethod() {
        System.out.println("Executing @AfterEach Test Method");
    }

    @BeforeAll
    public static void runBeforeAllTestMethod() {
        System.out.println("Executing @BeforeAll Test Method");
    }

    @AfterAll
    public static void runAfterAllTestMethod() {
        System.out.println("Executing @AfterAll Test Method");
    }

    @Test
    public void testHandlerFunction_whenValidDetailsProvided_returnsSuccessfulResponse() {
        // AAA
        // Arrange or Given
        // ApiGatewayProxyRequestEvent
        JsonObject userDetails = new JsonObject();

        userDetails.addProperty("firstName", "Peacemaker");
        userDetails.addProperty("lastname", "Smith");
        userDetails.addProperty("email", "peacemaker@peace.com");
        userDetails.addProperty("password", "baldyeagle");

        String userDetailsJsonString = new Gson().toJson(userDetails);

        when(apiGatewayProxyRequestEvent.getBody()).thenReturn(userDetailsJsonString);

        // Context - elevated to @BeforeEach
//        when(context.getLogger()).thenReturn(logger);

        // CognitoUserService
        JsonObject createUserResult = new JsonObject();
        createUserResult.addProperty(Constants.IS_SUCCESSFUL, true);
        createUserResult.addProperty(Constants.STATUS_CODE, "200");
        createUserResult.addProperty(Constants.COGNITO_USER_ID, UUID.randomUUID().toString());
        createUserResult.addProperty(Constants.IS_CONFIRMED, false);

        when(cognitoUserService.createUser(any(JsonObject.class), any(), any())).thenReturn(createUserResult);

        // Act or When
        APIGatewayProxyResponseEvent responseEvent = createUserHandler.handleRequest(apiGatewayProxyRequestEvent, context);
        String responseBody = responseEvent.getBody();
        JsonObject responseBodyJson = JsonParser.parseString(responseBody).getAsJsonObject();


        // Assert or Then
        verify(logger, times(1)).log(anyString());
        assertTrue(responseBodyJson.get(Constants.IS_SUCCESSFUL).getAsBoolean());
        assertEquals(200, responseBodyJson.get(Constants.STATUS_CODE).getAsInt());
        assertNotNull(responseBodyJson.get(Constants.COGNITO_USER_ID).getAsString());
        assertFalse(responseBodyJson.get(Constants.IS_CONFIRMED).getAsBoolean());
        assertEquals(200, responseEvent.getStatusCode(), "Successful HTTP response should return HTTP status 200");
        assertEquals("application/json", responseEvent.getHeaders().get("Content-Type"));
        verify(cognitoUserService, times(1)).createUser(any(JsonObject.class), any(), any());

    }

    @Test
    public void testHandleRequest_whenEmptyRequestBodyProvided_returnsErrorMessage() {
        // Arrange

        when(apiGatewayProxyRequestEvent.getBody()).thenReturn("");
        // Context - elevated to @BeforeEach lifecycle method
        //when(context.getLogger()).thenReturn(logger);

        // Act
        APIGatewayProxyResponseEvent responseEvent = createUserHandler.handleRequest(apiGatewayProxyRequestEvent, context);
        String responseBody = responseEvent.getBody();
        JsonObject responseBodyJson = JsonParser.parseString(responseBody).getAsJsonObject();

        // Assert
        assertEquals(500, responseEvent.getStatusCode());
        assertNotNull(responseBodyJson.get("message"), "Missing the 'message' property in JSON response");
        assertFalse(responseBodyJson.get("message").getAsString().isEmpty(), "Error message property should not be " +
                "empty");
    }

    @Test
    public void testCreateUserHander_whenAwsServiceExceptionTakesPlace_returnsErrorMessage() {
        // Arrange

        JsonObject userDetails = new JsonObject();

        userDetails.addProperty("firstName", "Peacemaker");
        userDetails.addProperty("lastname", "Smith");
        userDetails.addProperty("email", "peacemaker@peace.com");
        userDetails.addProperty("password", "baldyeagle");

        String userDetailsJsonString = new Gson().toJson(userDetails);

        when(apiGatewayProxyRequestEvent.getBody()).thenReturn(userDetailsJsonString);

        AwsErrorDetails awsErrorDetails = AwsErrorDetails.builder()
                .errorCode("")
                .sdkHttpResponse(SdkHttpResponse.builder().statusCode(500).build())
                .errorMessage("AWS Service Exception took place")
                .build();

        when(cognitoUserService.createUser(any(), any(), any()))
                .thenThrow(AwsServiceException
                        .builder()
                        .statusCode(500)
                        .awsErrorDetails(awsErrorDetails)
                        .build());

        // Act
        APIGatewayProxyResponseEvent responseEvent = createUserHandler.handleRequest(apiGatewayProxyRequestEvent, context);
        String responseBody = responseEvent.getBody();
        JsonObject responseBodyJson = JsonParser.parseString(responseBody).getAsJsonObject();


        // Assert
        assertEquals(awsErrorDetails.sdkHttpResponse().statusCode(), responseEvent.getStatusCode());
        assertNotNull(responseBodyJson.get("message"), "Message property cannot be empty");
        assertEquals(awsErrorDetails.errorMessage(), responseBodyJson.get("message").getAsString());

    }

}
