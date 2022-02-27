package com.ppkn.federate.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class ArgsResolverConfig {

    private Map<String, String> argMap = new HashMap<>();

    public void setArgs(String... args) {
        for(String arg : args) {
            if(!arg.contains("pwd")) {
                log.debug("run: {}", arg);
            }
            String[] array = arg.split("=");
            if(array.length == 2) {
                argMap.put(array[0], array[1]);
            }
        }
    }

    public String getRoleName() {
        return  argMap.get(ConfigEnum.role.name())  != null ?  argMap.get(ConfigEnum.role.name())
            : ConfigEnum.Roles.dev.name();
    }

    public String getUserName() {
        return  argMap.get(ConfigEnum.uname.name());
    }
    public String getNewPassword() {
        return  argMap.get(ConfigEnum.newpwd.name());
    }
    public String getPassword() {
        return  argMap.get(ConfigEnum.pwd.name());
    }
    public String getCsvFile() {
        return  argMap.get(ConfigEnum.csvfile.name());
    }
    public String getCommand() {
        return  argMap.get(ConfigEnum.cmd.name())  != null ?  argMap.get(ConfigEnum.cmd.name())
            : ConfigEnum.Cmd.auth.name();
    }
    public int getTimeInHours() {
        int hrs = 4;
        try{
            if(StringUtils.hasLength(argMap.get(ConfigEnum.time.name()))) {
                hrs = Integer.parseInt(argMap.get(ConfigEnum.time.name()));
                hrs = hrs > 12 ? 12 : hrs;
                log.info("Supplied Token expiration in hours : {}", hrs);
            }else {
                log.info("No time supplied using default token expiration : 4hrs");
            }
        }catch (Exception e) {
            log.error("Error converting time, using default time 4hrs");
        }
        return  hrs;
    }
}
