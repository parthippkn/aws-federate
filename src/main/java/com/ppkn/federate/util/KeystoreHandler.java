package com.ppkn.federate.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Component
@Slf4j
public class KeystoreHandler {

    private final static String keyStoreLocation = System.getProperty("federate.keystore.path");
    private final static String storePassword = System.getProperty("federate.store.password");
    private final static String keyPassword = System.getProperty("federate.key.password");
    private final static String propFilePath = System.getProperty("federate.aws.prop");

    private KeyStore ks = null;

    @PostConstruct
    public void loadKeystore()  {
        try {
            Assert.hasLength(keyStoreLocation, "keyStoreLocation cannot be null. Please check");
            Assert.notNull(storePassword, "storePassword cannot be empty");
            Assert.notNull(keyPassword, "keyPassword cannot be empty");

            log.info("keyStoreLocation : {}", keyStoreLocation);

            ks = KeyStore.getInstance("PKCS12");
            ks.load(new FileInputStream(keyStoreLocation), storePassword.toCharArray());
            log.info("Successfully loaded keystore..");
        }catch (Exception e) {
            throw new RuntimeException("Unable to load keystore. Please check keystore location set in environment variable ", e);
        }
    }

    private void createEntries() throws Exception {
        final Properties props = new Properties();
        props.load(new FileInputStream(propFilePath));
        Map<String,String> map = new HashMap(props);
        map.forEach((String key, String value )-> {
            try {
                createNewEntry(key, value);
            }catch (Exception e) {
                System.out.println("Error while storing the key");
            }
        });
        System.out.println("Total keys written : " + map.size());
        System.out.println("Successfully written all the values to the jks file");
    }

    private void deleteEntries() throws Exception {
        final Properties props = new Properties();
        props.load(new FileInputStream(propFilePath));
        Map<String,String> map = new HashMap(props);
        map.forEach((String key, String value )-> {
            try {
                deleteEntry(key);
            }catch (Exception e) {
                System.out.println("Error while storing the key");
            }
        });
        System.out.println("Total keys deleted : " + map.size());
        System.out.println("Successfully deleted the values");
    }

    public static void main(String[] args) throws Exception {
        KeystoreHandler keystoreHandler = new KeystoreHandler();
        keystoreHandler.loadKeystore();
        keystoreHandler.deleteEntries();
        keystoreHandler.createEntries();
    }

    private void deleteEntry(String entry)
        throws Exception {
        ks.deleteEntry(entry);
        FileOutputStream fos = new java.io.FileOutputStream(keyStoreLocation);
        ks.store(fos, storePassword.toCharArray());
    }

    private void createNewEntry(String entry, String entryPassword)
        throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBE");
        SecretKey generatedSecret =
            factory.generateSecret(new PBEKeySpec(
                entryPassword.toCharArray()));
        KeyStore.PasswordProtection keyStorePP = new KeyStore.PasswordProtection(keyPassword.toCharArray());
        ks.setEntry(entry, new KeyStore.SecretKeyEntry(
            generatedSecret), keyStorePP);
        FileOutputStream fos = new java.io.FileOutputStream(keyStoreLocation);
        ks.store(fos, storePassword.toCharArray());
    }

    public String getValueFromKeystore(String entry)
        throws Exception{
        KeyStore.PasswordProtection keyStorePP = new KeyStore.PasswordProtection(keyPassword.toCharArray());
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBE");
        KeyStore.SecretKeyEntry ske =
            (KeyStore.SecretKeyEntry)ks.getEntry(entry, keyStorePP);
        PBEKeySpec keySpec = (PBEKeySpec)factory.getKeySpec(
            ske.getSecretKey(),
            PBEKeySpec.class);
        char[] password = keySpec.getPassword();
        return new String(password);
    }
}
