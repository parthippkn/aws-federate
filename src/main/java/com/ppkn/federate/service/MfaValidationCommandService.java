package com.ppkn.federate.service;

import com.amazonaws.services.cognitoidentity.AmazonCognitoIdentity;
import com.amazonaws.services.cognitoidentity.model.*;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.model.*;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.model.AssumeRoleWithWebIdentityRequest;
import com.amazonaws.services.securitytoken.model.AssumeRoleWithWebIdentityResult;
import com.amazonaws.services.securitytoken.model.Credentials;
import com.ppkn.federate.config.ConfigEnum;
import com.ppkn.federate.config.FederateConfig;
import com.ppkn.federate.model.CommandServiceRequest;
import com.ppkn.federate.model.CommandServiceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

@Service
@Slf4j
@RequiredArgsConstructor
public class MfaValidationCommandService implements CommandService {
    private final FederateConfig federateConfig;

    @Override
    public CommandServiceResponse execute(CommandServiceRequest request) {
        CommandServiceResponse response = executeValidation(request.getIdentityProvider(),
            request.getCognitoIdentity(),
            request.getTokenService(),
            request.getAuthResult());
        return response;
    }

    private CommandServiceResponse executeValidation(AWSCognitoIdentityProvider cognitoClient,
                                                     AmazonCognitoIdentity cognitoIdentity,
                                                     AWSSecurityTokenService tokenService,
                                                     InitiateAuthResult result) {
        if (!result.getChallengeName().equalsIgnoreCase(ChallengeNameType.SOFTWARE_TOKEN_MFA.name())) {
            throw new RuntimeException("Auth challenge is NOT SOFTWARE_TOKEN_MFA. Please check user needs to change password");
        }
        log.info("Enter access code from your Mobile : ");
        CommandServiceResponse response = new CommandServiceResponse();
        String userCode = new Scanner(System.in).nextLine();

        RespondToAuthChallengeRequest challengeRequest = new RespondToAuthChallengeRequest();
        Map<String, String> challengeMap = new HashMap<>();
        challengeMap.putIfAbsent("USERNAME", federateConfig.getArgsResolverConfig().getUserName());
        challengeMap.putIfAbsent("SOFTWARE_TOKEN_MFA_CODE", userCode);

        challengeRequest.withClientId(federateConfig.getAppClientId())
            .withChallengeName(ChallengeNameType.SOFTWARE_TOKEN_MFA)
            .withChallengeResponses(challengeMap)
            .withSession(result.getSession());

        try {
            RespondToAuthChallengeResult challengeResult = cognitoClient.respondToAuthChallenge(challengeRequest);
            log.debug("Successfully completed challengeResult ");
            GetIdResult idResult = getId(cognitoIdentity, challengeResult);
            GetOpenIdTokenResult openIdTokenResult = getOpenIdToken(cognitoIdentity, challengeResult, idResult);
            AssumeRoleWithWebIdentityResult webIdentityResult = getTokenCredentials(tokenService, openIdTokenResult);

            createCredentials(webIdentityResult.getCredentials());
            log.info("Successfully created credentials and it will expire at : {}", webIdentityResult.getCredentials().getExpiration());
            response.setStatus(ConfigEnum.Status.SUCCESS.name());
            response.setExpiration(webIdentityResult.getCredentials().getExpiration());
        } catch (Exception e) {
            throw new RuntimeException("MFA Validation Service Failed : ", e);
        }
        return response;
    }

    private String getLoginUrl() {
        StringBuilder builder = new StringBuilder("cognito-idp");
        builder.append(".")
            .append(federateConfig.getRegion())
            .append(".")
            .append("amazonaws.com/")
            .append(federateConfig.getUserPoolId());
        return builder.toString();
    }

    private GetIdResult getId(AmazonCognitoIdentity cognitoIdentity, RespondToAuthChallengeResult result) {
        Map<String, String> logins = new HashMap<>();
        logins.putIfAbsent(getLoginUrl(), result.getAuthenticationResult().getIdToken());

        GetIdRequest request = new GetIdRequest();
        request.withAccountId(federateConfig.getAccountId())
            .withIdentityPoolId(federateConfig.getIdentityPoolId())
            .withLogins(logins);
        GetIdResult idResult = cognitoIdentity.getId(request);
        log.debug("Successfully completed getId : {}", idResult.getIdentityId());
        return idResult;
    }

    private GetOpenIdTokenResult getOpenIdToken(AmazonCognitoIdentity cognitoIdentity,
                                                RespondToAuthChallengeResult result, GetIdResult idResult) {
        Map<String, String> logins = new HashMap<>();
        logins.putIfAbsent(getLoginUrl(), result.getAuthenticationResult().getIdToken());

        GetOpenIdTokenRequest request = new GetOpenIdTokenRequest();
        request.withIdentityId(idResult.getIdentityId())
            .withLogins(logins);
        GetOpenIdTokenResult openIdTokenResult = cognitoIdentity.getOpenIdToken(request);
        log.debug("Successfully completed getOpenIdToken : {}", openIdTokenResult.getIdentityId());
        return openIdTokenResult;
    }

    private AssumeRoleWithWebIdentityResult getTokenCredentials(AWSSecurityTokenService tokenService,
                                                                GetOpenIdTokenResult openIdTokenResult) {
        //String roleArn = "arn:aws:iam::555994199423:role/Cognito_adminpoolAuth_Role";
        AssumeRoleWithWebIdentityRequest identityRequest = new AssumeRoleWithWebIdentityRequest();
        identityRequest.withRoleArn(federateConfig.getRoleArn())
            .withRoleSessionName(federateConfig.getArgsResolverConfig().getUserName()+"-Session")
            .withWebIdentityToken(openIdTokenResult.getToken())
            .withDurationSeconds(federateConfig.getArgsResolverConfig().getTimeInHours() * 60 * 60); // 4hrs

        AssumeRoleWithWebIdentityResult webIdentityResult = tokenService.assumeRoleWithWebIdentity(identityRequest);
        log.debug("Successfully completed getTokenCredentials : {}", webIdentityResult.getCredentials());
        return webIdentityResult;
    }

    private void createCredentials(Credentials credentials) {
        try {
            String homeDir = System.getProperty("user.home");
            String awsDir = homeDir + System.getProperty("file.separator") + ".aws";
            String credFileName = awsDir + System.getProperty("file.separator") + "credentials";
            String configFileName = awsDir + System.getProperty("file.separator") + "config";
            Path path = Paths.get(awsDir);
            if(!Files.exists(path)) {
                Files.createDirectories(path);
            }

            createCredentialFile(credFileName, credentials);
            createConfigFile(configFileName);
        } catch (Exception e) {
            throw new RuntimeException("Unable to create credentials : ", e);
        }
    }

    private void createCredentialFile(String credFileName, Credentials credentials) {
        try {
            File credFile = new File(credFileName);
            if(!credFile.exists()) {
                credFile.createNewFile();
            }

            FileWriter fileWriter = new FileWriter(credFileName);
            PrintWriter printWriter = new PrintWriter(fileWriter);
            printWriter.println("[default]");
            printWriter.printf("aws_access_key_id = %s\n", credentials.getAccessKeyId());
            printWriter.printf("aws_secret_access_key = %s\n", credentials.getSecretAccessKey());
            printWriter.printf("aws_session_token = %s\n", credentials.getSessionToken());
            printWriter.close();
            fileWriter.close();
        } catch (Exception e) {
            throw new RuntimeException("Unable to create createCredentialFile : ", e);
        }
    }

    private void createConfigFile(String configFileName) {
        try {
            File credFile = new File(configFileName);
            if(!credFile.exists()) {
                credFile.createNewFile();
            }
            FileWriter fileWriter = new FileWriter(configFileName);
            PrintWriter printWriter = new PrintWriter(fileWriter);
            printWriter.println("[default]");
            printWriter.printf("region = %s\n", federateConfig.getRegion());
            printWriter.printf("output = json\n");
            printWriter.close();
        } catch (Exception e) {
            throw new RuntimeException("Unable to create createConfigFile : ", e);
        }
    }
}
