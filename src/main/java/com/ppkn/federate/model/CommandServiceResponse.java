package com.ppkn.federate.model;

import com.ppkn.federate.config.ConfigEnum;
import lombok.Data;

import java.util.Date;

@Data
public class CommandServiceResponse {
    private String status = ConfigEnum.Status.Failure.name();
    private Date expiration;
}
