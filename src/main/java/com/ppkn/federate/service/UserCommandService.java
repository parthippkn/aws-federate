package com.ppkn.federate.service;

import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.model.*;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.ppkn.federate.config.ConfigEnum;
import com.ppkn.federate.config.FederateConfig;
import com.ppkn.federate.model.CommandServiceRequest;
import com.ppkn.federate.model.CommandServiceResponse;
import com.ppkn.federate.model.UserDomain;
import com.ppkn.federate.util.RandonPasswordGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.lang.reflect.Method;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserCommandService implements CommandService {
    private final FederateConfig federateConfig;
    private final Map<ConfigEnum.Cmd, Method> commandMap = new HashMap<>();

    @Override
    public CommandServiceResponse execute(CommandServiceRequest request) {
        CommandServiceResponse response = null;
        if( ConfigEnum.Cmd.importusers.equals(request.getCommand())) {
            response = importUsers(request);
        }
        else if( ConfigEnum.Cmd.deleteusers.equals(request.getCommand())) {
            response = deleteUsers(request);
        }
        else if( ConfigEnum.Cmd.changepwd.equals(request.getCommand())) {
            response = changePassword(request);
        }
        return response;
    }

    private CommandServiceResponse importUsers(CommandServiceRequest request) {
        if(!StringUtils.hasLength(federateConfig.getArgsResolverConfig().getCsvFile())) {
            throw new RuntimeException("For importusers, csvfile should be supplied");
        }
        CommandServiceResponse response = new CommandServiceResponse();
        List<UserDomain> userDomainList = getUsers();
        if(!userDomainList.isEmpty()) {
            userDomainList.forEach(user->{
                response.setStatus(  createUser(user, request)  ? ConfigEnum.Status.SUCCESS.name()
                                            : ConfigEnum.Status.Failure.name());
            });
        }
        return response;
    }

    private CommandServiceResponse deleteUsers(CommandServiceRequest request) {
        if(!StringUtils.hasLength(federateConfig.getArgsResolverConfig().getCsvFile())) {
            throw new RuntimeException("For deleteUsers, csvfile should be supplied");
        }
        CommandServiceResponse response = new CommandServiceResponse();
        List<UserDomain> userDomainList = getUsers();
        if(!userDomainList.isEmpty()) {
            userDomainList.forEach(user->{
                response.setStatus(  deleteUser(user, request)  ? ConfigEnum.Status.SUCCESS.name()
                                    : ConfigEnum.Status.Failure.name());
            });
        }
        return response;
    }

    private CommandServiceResponse changePassword(CommandServiceRequest request) {
        CommandServiceResponse response = new CommandServiceResponse();
        AWSCognitoIdentityProvider identityProvider = request.getIdentityProvider();

        AdminSetUserPasswordRequest passwordRequest = new AdminSetUserPasswordRequest();
        passwordRequest.withUserPoolId(federateConfig.getUserPoolId())
            .withUsername(federateConfig.getArgsResolverConfig().getUserName())
            .withPassword(federateConfig.getArgsResolverConfig().getNewPassword())
            .withPermanent(true);
        AdminSetUserPasswordResult passwordResult = identityProvider.adminSetUserPassword(passwordRequest);
        log.info("Password updated successfully. ");
        return response;
    }

    private List<UserDomain> getUsers() {
        List<UserDomain> userDomainList = Collections.emptyList();
        try {
            File csvFile = new File(federateConfig.getArgsResolverConfig().getCsvFile());
            CsvMapper csvMapper = new CsvMapper();
            CsvSchema csvSchema = csvMapper
                .typedSchemaFor(UserDomain.class)
                .withHeader()
                .withColumnSeparator(',')
                .withComments();
            MappingIterator<UserDomain> userIterator = csvMapper
                .readerWithTypedSchemaFor(UserDomain.class)
                .with(csvSchema)
                .readValues(csvFile);
            userDomainList = userIterator.readAll();
            if(!userDomainList.isEmpty()) {
                log.info("Number of users read from csv file [{}] is : {}", csvFile.getName(), userDomainList.size());
            }
        }catch (Exception e) {
            throw new RuntimeException("Exception in importUsers", e);
        }
        return userDomainList;
    }

    private boolean createUser(UserDomain userDomain, CommandServiceRequest request) {
        log.info("Creating user : {} ", userDomain.toString());
        AWSCognitoIdentityProvider identityProvider = request.getIdentityProvider();
        List<AttributeType> attributeTypeList = new ArrayList<>();

        AttributeType userAttribType = new AttributeType();
        userAttribType.setName("email");
        userAttribType.setValue(userDomain.getEmail());
        attributeTypeList.add(userAttribType);
        userAttribType = new AttributeType();
        userAttribType.setName("name");
        userAttribType.setValue(userDomain.getName());
        attributeTypeList.add(userAttribType);

        String tempPassword = getTempPassword();
        AdminCreateUserResult createUserResult = createUser(userDomain, tempPassword, attributeTypeList, identityProvider);
        AdminSetUserPasswordRequest passwordRequest = new AdminSetUserPasswordRequest();
        passwordRequest.withUserPoolId(federateConfig.getUserPoolId(userDomain.getRole()))
            .withUsername(userDomain.getUserName())
            .withPassword(tempPassword)
            .withPermanent(true);
        AdminSetUserPasswordResult passwordResult = identityProvider.adminSetUserPassword(passwordRequest);
        log.info("User created : {}, status : {}, passwordResult : {}",  userDomain.getUserName(), createUserResult.getUser().getUserStatus(), passwordResult.toString());
        return true;
    }

    private AdminCreateUserResult createUser(UserDomain userDomain, String tempPassword,
                               List<AttributeType> attributeTypeList,
                               AWSCognitoIdentityProvider identityProvider) {
        AdminCreateUserRequest createUserRequest = new AdminCreateUserRequest();
        createUserRequest.withUserPoolId(federateConfig.getUserPoolId(userDomain.getRole()))
            .withUsername(userDomain.getUserName())
            .withTemporaryPassword(tempPassword)
            .withUserAttributes(attributeTypeList)
            .withDesiredDeliveryMediums(DeliveryMediumType.EMAIL);
        return identityProvider.adminCreateUser(createUserRequest);
    }

    private String getTempPassword() {
        return RandonPasswordGenerator.generatePassayPassword();
    }

    private boolean deleteUser(UserDomain userDomain, CommandServiceRequest request) {
        log.info("Deleting user : {} ", userDomain.toString());
        AWSCognitoIdentityProvider identityProvider = request.getIdentityProvider();
        AdminDeleteUserRequest deleteUserRequest = new AdminDeleteUserRequest();
        deleteUserRequest.withUserPoolId(federateConfig.getUserPoolId(userDomain.getRole()))
            .withUsername(userDomain.getUserName());
        AdminDeleteUserResult deleteUserResult = identityProvider.adminDeleteUser(deleteUserRequest);
        log.info("User deleted : {}, status : {}, passwordResult : {}",  userDomain.getUserName(), deleteUserResult);
        return true;
    }
}
