package com.ppkn.federate.config;

import com.ppkn.federate.util.KeystoreHandler;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;


@Configuration
@EnableConfigurationProperties
@ConfigurationProperties
@RequiredArgsConstructor
@Data
@Slf4j
public class FederateConfig {

    private final ArgsResolverConfig argsResolverConfig;
    private final KeystoreHandler keystoreHandler;
    private Map<String, CognitoPool> roles = new HashMap();
    private String region, env, issuer;
    private Map<String, String> keystore = new HashMap<>();

    @Data
    public static class CognitoPool {
        private String accountId;
        private String token;
        private String appClientId;
        private String userPoolId;
        private String identityPoolId;
        private String roleArn;
    }

    @PostConstruct
    public void postConstruct() {
        log.info("region : {}, Env : {} ", region, env);
        roles.forEach((key, pool) -> {
            try {
                pool.setAccountId(keystoreHandler.getValueFromKeystore(pool.getAccountId()));
                pool.setToken(keystoreHandler.getValueFromKeystore(pool.getToken()));
                pool.setAppClientId(keystoreHandler.getValueFromKeystore(pool.getAppClientId()));
                pool.setUserPoolId(keystoreHandler.getValueFromKeystore(pool.getUserPoolId()));
                pool.setIdentityPoolId(keystoreHandler.getValueFromKeystore(pool.getIdentityPoolId()));
                pool.setRoleArn(keystoreHandler.getValueFromKeystore(pool.getRoleArn()));
            }catch (Exception e) {
                log.error("Missing in keystore key : {}", e.getMessage());
            }
        });
    }

    private CognitoPool getCognitoPool() {
        CognitoPool pool = roles.get(argsResolverConfig.getRoleName());
        Assert.notNull(pool, "Invalid role name supplied. Please check");
        return pool;
    }

    private CognitoPool getCognitoPool(String role) {
        CognitoPool pool = roles.get(role);
        Assert.notNull(pool, "Invalid role name supplied in the method Please check");
        return pool;
    }

    public String getAccountId() {
        return getCognitoPool().getAccountId();
    }

    public String getToken() {
        return getCognitoPool().getToken();
    }

    public String getAppClientId() {
        return getCognitoPool().getAppClientId();
    }

    public String getUserPoolId() {
        return getCognitoPool().getUserPoolId();
    }

    public String getRoleArn() {
        return getCognitoPool().getRoleArn();
    }

    public String getUserPoolId(String role) {
        return getCognitoPool(role).getUserPoolId();
    }

    public String getIdentityPoolId() {
        return getCognitoPool().getIdentityPoolId();
    }

}

