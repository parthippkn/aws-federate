package com.ppkn.federate.service;

import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.model.*;
import com.ppkn.federate.config.ConfigEnum;
import com.ppkn.federate.config.FederateConfig;
import com.ppkn.federate.model.CommandServiceRequest;
import com.ppkn.federate.model.CommandServiceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Scanner;

@Service
@Slf4j
@RequiredArgsConstructor
public class MfaSetupCommandService implements CommandService {

    private final FederateConfig federateConfig;
    private static final String QRCODE_TEMPLATE_URL = "https://www.google.com/chart?chs=200x200&chld=M|0&cht=qr&chl=" +
        "otpauth://totp/orgIssuer:role:uName?secret=secretKey&issuer=orgIssuer";

    @Override
    public CommandServiceResponse execute(CommandServiceRequest request) {
        CommandServiceResponse response = executeSetup(request.getIdentityProvider(), request.getAuthResult());
        return response;
    }

    private CommandServiceResponse executeSetup(AWSCognitoIdentityProvider cognitoClient, InitiateAuthResult result) {
        //associate token will generate QRCode
        AssociateSoftwareTokenResult aResult = createQRCode(cognitoClient, result);
        //verify software token
        VerifySoftwareTokenResult tokenResult = verifyQRCode(cognitoClient, aResult);
        CommandServiceResponse response = new CommandServiceResponse();
        response.setStatus(tokenResult.getStatus());
        return response;
    }

    private AssociateSoftwareTokenResult createQRCode(AWSCognitoIdentityProvider cognitoClient, InitiateAuthResult result) {
        AssociateSoftwareTokenRequest tokenRequest = new AssociateSoftwareTokenRequest();
        tokenRequest.withAccessToken(federateConfig.getToken())
            .withSession(result.getSession());

        AssociateSoftwareTokenResult tokenResult =  cognitoClient.associateSoftwareToken(tokenRequest);
        String qrCodeUrl = QRCODE_TEMPLATE_URL.replaceAll("secretKey", tokenResult.getSecretCode());
        qrCodeUrl = QRCODE_TEMPLATE_URL.replaceAll("orgIssuer", federateConfig.getIssuer());
        qrCodeUrl = qrCodeUrl.replaceAll("uName", federateConfig.getArgsResolverConfig().getUserName());
        qrCodeUrl = qrCodeUrl.replaceAll("role", federateConfig.getArgsResolverConfig().getRoleName());
        log.info("Click this QRCODE in the browser   : \n {}\n", qrCodeUrl);
        return tokenResult;
    }

    private VerifySoftwareTokenResult verifyQRCode(AWSCognitoIdentityProvider cognitoClient, AssociateSoftwareTokenResult tokenResult) {
        log.info("Enter access code from your Mobile : ");
        String userCode = new Scanner(System.in).nextLine();
        VerifySoftwareTokenRequest tokenRequest = new VerifySoftwareTokenRequest();
        tokenRequest.withAccessToken(federateConfig.getToken())
            .withSession(tokenResult.getSession())
            .withUserCode(userCode);

        VerifySoftwareTokenResult vTtokenResult =   cognitoClient.verifySoftwareToken(tokenRequest);
        log.debug("vTtokenResult : {}", vTtokenResult);
        if ( vTtokenResult.getStatus().equalsIgnoreCase(ConfigEnum.Status.SUCCESS.name())) {
            log.info("Successfully registered your mobile device for MFA");
        }
        return vTtokenResult;
    }
}
