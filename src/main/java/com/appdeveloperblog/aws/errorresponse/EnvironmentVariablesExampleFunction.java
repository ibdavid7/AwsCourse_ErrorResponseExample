package com.appdeveloperblog.aws.errorresponse;

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClientBuilder;
import com.amazonaws.services.kms.model.DecryptRequest;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.util.Base64;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class EnvironmentVariablesExampleFunction implements RequestHandler<APIGatewayProxyRequestEvent,
        APIGatewayProxyResponseEvent> {

    private final String[] envVariables = {"MY_COGNITO_CLIENT_APP_SECRET", "MY_COGNITO_USER_POOL_ID", "MY_VARIABLE"};

    private final Map<String, String> decryptedEnvVariablesMap = Arrays.stream(envVariables)
            .collect(HashMap::new, (map, ele) -> map.put(ele, decryptKey(ele)), HashMap::putAll);

    private final Map<String, String> encryptedEnvVariablesMap = Arrays.stream(envVariables)
            .collect(Collectors.toMap(Function.identity(), System::getenv));

    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/jason");
        headers.put("Custom-Type", "application/jason");

        // Get Map of env variables
        // Map<String, String> environmentVariables = System.getenv();

        // Log map of env variables
        LambdaLogger logger = context.getLogger();

        //Decrypted
        logger.log("Decrypted: " + decryptedEnvVariablesMap.toString());

        //Encrypted
        logger.log("Encrypted: " + encryptedEnvVariablesMap.toString());

        return new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withHeaders(headers)
                .withBody("{}");
    }

    private String decryptKey(String envVariableName) {
        System.out.println("Decrypting key");
        byte[] encryptedKey = Base64.decode(System.getenv(envVariableName));
        Map<String, String> encryptionContext = new HashMap<>();
        encryptionContext.put("LambdaFunctionName",
                System.getenv("AWS_LAMBDA_FUNCTION_NAME"));

        AWSKMS client = AWSKMSClientBuilder.defaultClient();

        DecryptRequest request = new DecryptRequest()
                .withCiphertextBlob(ByteBuffer.wrap(encryptedKey))
                .withEncryptionContext(encryptionContext);

        ByteBuffer plainTextKey = client.decrypt(request).getPlaintext();
        return new String(plainTextKey.array(), Charset.forName("UTF-8"));
    }

}