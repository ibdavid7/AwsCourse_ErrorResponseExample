package com.appdeveloperblog.aws.errorresponse;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.appdeveloperblog.aws.errorresponse.service.CognitoUserService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import software.amazon.awssdk.awscore.exception.AwsServiceException;

import java.util.HashMap;
import java.util.Map;

public class CreateUserHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final CognitoUserService cognitoUserService;
    private final String appClientId;
    private final String appClientSecret;

    public CreateUserHandler(CognitoUserService cognitoUserService, String appClientId, String appClientSecret) {
        this.cognitoUserService = cognitoUserService;
        this.appClientId = appClientId;
        this.appClientSecret = appClientSecret;
    }

    public CreateUserHandler() {
        this.cognitoUserService = new CognitoUserService(System.getenv("AWS_REGION"));
        this.appClientId = System.getenv("MY_COGNITO_POOL_APP_CLIENT_ID");
        this.appClientSecret = System.getenv("MY_COGNITO_POOL_APP_CLIENT_SECRET");

    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, Context context) {

        // API Response
        APIGatewayProxyResponseEvent responseEvent = new APIGatewayProxyResponseEvent();

        // Logger
        LambdaLogger logger = context.getLogger();

        // Headers
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        // Request body with HTTP Post payload containing user details
        String requestBody = apiGatewayProxyRequestEvent.getBody();
        logger.log("Original JSON request body: " + requestBody);

        // Parse request body into JSON object

        JsonObject userDetails = null;

        try {

            userDetails = JsonParser.parseString(requestBody).getAsJsonObject();

            JsonObject createUserResult = cognitoUserService.createUser(userDetails, appClientId, appClientSecret);
            responseEvent
                    .withStatusCode(200)
                    .withHeaders(headers)
                    .withBody(new Gson().toJson(createUserResult, JsonObject.class));

        } catch (AwsServiceException awsServiceException) {

            logger.log(awsServiceException.awsErrorDetails().errorMessage());

            ErrorResponse errorResponse = new ErrorResponse(awsServiceException.awsErrorDetails().errorMessage());


            responseEvent
                    .withStatusCode(500)
                    .withHeaders(headers)
                    .withBody(new Gson().toJson(errorResponse, ErrorResponse.class));

        } catch (Exception e) {
            logger.log(e.getMessage());

            ErrorResponse errorResponse = new ErrorResponse(e.getMessage());


            return responseEvent
                    .withStatusCode(500)
                    .withBody(
                            new GsonBuilder()
                                    .serializeNulls()
                                    .create()
                                    .toJson(errorResponse, ErrorResponse.class)
                    );
        }


        return responseEvent;

    }
}
