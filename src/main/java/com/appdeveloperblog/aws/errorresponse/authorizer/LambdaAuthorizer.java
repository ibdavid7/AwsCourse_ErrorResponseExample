package com.appdeveloperblog.aws.errorresponse.authorizer;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.appdeveloperblog.aws.errorresponse.utils.JwtUtils;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Arrays;
import java.util.List;

public class LambdaAuthorizer implements RequestHandler<APIGatewayProxyRequestEvent, AuthorizerOutput> {

    @Override
    public AuthorizerOutput handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
        LambdaLogger logger = context.getLogger();

        String userName = input.getPathParameters().get("username");
        logger.log("username: " + userName);

        String effect = "Allow";

        String jwt = input.getHeaders().get("Authorization");
        String region = System.getenv("AWS_REGION");
        String userPoolId = System.getenv("PHOTO_APP_USERS_POOL_ID");
        String audience = System.getenv("PHOTO_APP_USERS_APP_CLIENT_ID");

        JwtUtils jwtUtils = new JwtUtils();
        DecodedJWT decodedJWT = null;

        logger.log("jwt: " + jwt + ", " + "region: " + region + ", " + "userPoolId: " + userPoolId + ", " + "userName: " + userName + ", " + "audience: " + audience);

        try {
            decodedJWT = jwtUtils.validateJwtForUser(jwt, region, userPoolId, userName, audience);
            userName = decodedJWT.getSubject();
        } catch (RuntimeException e) {
            effect = "Deny";
            e.printStackTrace();

        }

        APIGatewayProxyRequestEvent.ProxyRequestContext proxyRequestContext =
                input.getRequestContext();

        String arn = String.format("arn:aws:execute-api:%s:%s:%s/%s/%s/%s",
                System.getenv("AWS_REGION"),
                proxyRequestContext.getAccountId(),
                proxyRequestContext.getApiId(),
                proxyRequestContext.getStage(),
                proxyRequestContext.getHttpMethod(),
                "*");

        Statement statement = Statement.builder()
                .action("execute-api:Invoke")
                .effect(effect)
                .resource(arn)
                .build();

        PolicyDocument policyDocument = PolicyDocument.builder()
                .version("2012-10-17")
                .statements(Arrays.asList(statement))
                .build();

        AuthorizerOutput authorizerOutput = AuthorizerOutput.builder()
                .principalId(userName)
                .policyDocument(policyDocument)
                .build();

        logger.log(new Gson().toJson(authorizerOutput, AuthorizerOutput.class));

        return authorizerOutput;
    }
}
