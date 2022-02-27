package com.ppkn.federate.config;

public enum ConfigEnum {
    cmd,
    uname,
    pwd,
    newpwd,
    role,
    csvfile,
    time,
    USERNAME,
    PASSWORD;

    public enum Roles {
        dev,
        qa,
        admin;
    }

    public enum Status {
        SUCCESS,
        Failure;
    }

    public enum Cmd {
        setup,
        auth,
        importusers,
        deleteusers,
        changepwd;
    }
}
