package com.ppkn.federate.model;

import com.amazonaws.services.cognitoidentity.AmazonCognitoIdentity;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.model.InitiateAuthResult;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.ppkn.federate.config.ConfigEnum;
import lombok.Data;

@Data
public class CommandServiceRequest {

    private ConfigEnum.Cmd command;
    private AWSCognitoIdentityProvider identityProvider;
    private AmazonCognitoIdentity cognitoIdentity;
    private AWSSecurityTokenService tokenService;
    private InitiateAuthResult authResult;
}
