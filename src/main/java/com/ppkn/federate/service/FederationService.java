package com.ppkn.federate.service;

import com.amazonaws.services.cognitoidentity.AmazonCognitoIdentity;
import com.amazonaws.services.cognitoidentity.AmazonCognitoIdentityClientBuilder;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClientBuilder;
import com.amazonaws.services.cognitoidp.model.AuthFlowType;
import com.amazonaws.services.cognitoidp.model.InitiateAuthRequest;
import com.amazonaws.services.cognitoidp.model.InitiateAuthResult;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.ppkn.federate.config.ConfigEnum;
import com.ppkn.federate.config.FederateConfig;
import com.ppkn.federate.model.CommandServiceRequest;
import com.ppkn.federate.model.CommandServiceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class FederationService {
    private final FederateConfig federateConfig;
    private final MfaSetupCommandService mfaSetupService;
    private final MfaValidationCommandService mfaValidationService;
    private final Map<ConfigEnum.Cmd, CommandService> commandMap = new HashMap<>();

    @PostConstruct
    public void postConstruct() {
        log.debug("FederationService::execute");
        commandMap.putIfAbsent(ConfigEnum.Cmd.setup, new MfaSetupCommandService(federateConfig));
        commandMap.putIfAbsent(ConfigEnum.Cmd.auth, new MfaValidationCommandService(federateConfig));
        commandMap.putIfAbsent(ConfigEnum.Cmd.importusers, new UserCommandService(federateConfig));
        commandMap.putIfAbsent(ConfigEnum.Cmd.deleteusers, new UserCommandService(federateConfig));
        commandMap.putIfAbsent(ConfigEnum.Cmd.changepwd, new UserCommandService(federateConfig));
    }

    public void execute() {
        processFederation();
    }

    private void processFederation() {
        Assert.hasLength(federateConfig.getAppClientId(), "ClientId cannot be null. Please check");
        Assert.notNull(federateConfig.getArgsResolverConfig().getUserName(), "User name cannot be empty");
        Assert.notNull(federateConfig.getArgsResolverConfig().getPassword(), "Password cannot be empty");

        ConfigEnum.Cmd command = getSupportedCommand();
        CommandServiceRequest serviceRequest = buildRequest();
        CommandServiceResponse serviceResponse =  commandMap.get(command).execute(serviceRequest);
        log.info("FederationService process completed, status : {}", serviceResponse.getStatus());
    }

    private ConfigEnum.Cmd getSupportedCommand() {
        String commandStr = federateConfig.getArgsResolverConfig().getCommand();
        try {
           return ConfigEnum.Cmd.valueOf(commandStr);
        }catch (Exception ex) {
            throw new RuntimeException("Invalid supplied command [ " + commandStr + " ]. Please check");
        }
    }

    private CommandServiceRequest buildRequest() {
        CommandServiceRequest request = new CommandServiceRequest();
        request.setCommand(ConfigEnum.Cmd.valueOf(federateConfig.getArgsResolverConfig().getCommand()));
        request.setIdentityProvider(getCognitoIdp());
        request.setCognitoIdentity(getCognitoIdentity());
        request.setTokenService(getTokenService());
        InitiateAuthResult result = initiateAuth(request.getIdentityProvider());
        if( result == null) {
            throw new RuntimeException("Please check your credentials and try again.");
        }
        request.setAuthResult(result);
        return request;
    }
    private AWSCognitoIdentityProvider getCognitoIdp() {
        return AWSCognitoIdentityProviderClientBuilder.standard()
                                                        .withRegion(federateConfig.getRegion())
                                                        .build();
    }

    private AmazonCognitoIdentity getCognitoIdentity() {
        return AmazonCognitoIdentityClientBuilder.standard()
            .withRegion(federateConfig.getRegion())
            .build();
    }

    private AWSSecurityTokenService getTokenService() {
        return AWSSecurityTokenServiceClientBuilder.standard()
            .withRegion(federateConfig.getRegion())
            .build();
    }

    private InitiateAuthResult initiateAuth(AWSCognitoIdentityProvider cognitoClient) {
        InitiateAuthResult authResult = null;
        Map<String, String> authParameters = new HashMap<>();
        authParameters.put(ConfigEnum.USERNAME.name(), federateConfig.getArgsResolverConfig().getUserName());
        authParameters.put(ConfigEnum.PASSWORD.name(), federateConfig.getArgsResolverConfig().getPassword());

        final InitiateAuthRequest initiateAuthRequest = new InitiateAuthRequest();
        initiateAuthRequest.withAuthFlow(AuthFlowType.USER_PASSWORD_AUTH)
            .withClientId(federateConfig.getAppClientId())
            .withAuthParameters(authParameters);

        try {
            log.info("initiateAuthRequest clientId : {}, User Id: {} ", initiateAuthRequest.getClientId(), federateConfig.getArgsResolverConfig().getUserName());
            authResult =  cognitoClient.initiateAuth(initiateAuthRequest);
            log.info("Successfully authenticated, userId : {} ", federateConfig.getArgsResolverConfig().getUserName());
            log.debug("authResult : {}", authResult.getChallengeName());
        }catch (Exception ex) {
            log.error("Not authorized : {}", ex.getMessage());
        }
        return authResult;
    }
}
