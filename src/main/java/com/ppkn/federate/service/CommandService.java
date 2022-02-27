package com.ppkn.federate.service;

import com.ppkn.federate.model.CommandServiceRequest;
import com.ppkn.federate.model.CommandServiceResponse;

public interface CommandService {
    CommandServiceResponse execute(CommandServiceRequest request);
}
