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

public class LoginUserHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final CognitoUserService cognitoUserService;
    private final String appClientId;
    private final String appClientSecret;

    public LoginUserHandler() {
        this.cognitoUserService = new CognitoUserService(System.getenv("AWS_REGION"));
        this.appClientId = Utils.decryptKey("MY_COGNITO_POOL_APP_CLIENT_ID");
        this.appClientSecret = Utils.decryptKey("MY_COGNITO_POOL_APP_CLIENT_SECRET");
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, Context context) {

        APIGatewayProxyResponseEvent responseEvent = new APIGatewayProxyResponseEvent();

        LambdaLogger logger = context.getLogger();

        Map<String, String> headers = new HashMap<>();

        headers.put("Content-Type", "application/json");

        try {

            JsonObject requestBodyJson = JsonParser.parseString(apiGatewayProxyRequestEvent.getBody()).getAsJsonObject();

            String username = requestBodyJson.get("username").getAsString();
            String password = requestBodyJson.get("password").getAsString();

            JsonObject loginUserResult = cognitoUserService.loginUser(username, password, appClientId,
                    appClientSecret);

            return responseEvent
                    .withStatusCode(200)
                    .withHeaders(headers)
                    .withBody(new Gson().toJson(loginUserResult, JsonObject.class));

        } catch (AwsServiceException awsServiceException) {
            logger.log(awsServiceException.awsErrorDetails().errorMessage());

            ErrorResponse errorResponse = new ErrorResponse(awsServiceException.awsErrorDetails().errorMessage());

            return responseEvent
                    .withStatusCode(awsServiceException.awsErrorDetails().sdkHttpResponse().statusCode())
                    .withBody(
                            new GsonBuilder()
                                    .serializeNulls()
                                    .create()
                                    .toJson(errorResponse, ErrorResponse.class)
                    );

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

    }
}
