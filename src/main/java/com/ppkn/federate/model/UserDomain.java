package com.ppkn.federate.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
@JsonPropertyOrder({ "role", "userName", "name", "email" })
public class UserDomain {
    private String role;
    private String userName;
    private String name;
    private String email;
}
