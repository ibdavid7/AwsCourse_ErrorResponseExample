package com.appdeveloperblog.aws.errorresponse;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.appdeveloperblog.aws.errorresponse.service.CognitoUserService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.Map;

public class GetUserByUsernameHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final CognitoUserService cognitoUserService;

    public GetUserByUsernameHandler() {
        this.cognitoUserService = new CognitoUserService(System.getenv("AWS_REGION"));
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent,
                                                      final Context context) {

        APIGatewayProxyResponseEvent responseEvent = new APIGatewayProxyResponseEvent();
        responseEvent.withHeaders(Map.of("Content-Type", "application/json"));

        String username = apiGatewayProxyRequestEvent.getPathParameters().get("username");
        String poolId = System.getenv("PHOTO_APP_USERS_POOL_ID");

        try {
            JsonObject userDetails = cognitoUserService.getUserByUsername(username, poolId);
            responseEvent
                    .withStatusCode(200)
                    .withBody(new Gson().toJson(userDetails, JsonObject.class));

        } catch (Exception e) {
            responseEvent.withBody("{\"message\":\"" + e.getMessage() + "\"}");
            responseEvent.withStatusCode(500);
        }

        return responseEvent;

    }
}
