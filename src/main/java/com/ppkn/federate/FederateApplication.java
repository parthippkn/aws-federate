package com.ppkn.federate;

import com.ppkn.federate.config.ArgsResolverConfig;
import com.ppkn.federate.service.FederationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
@RequiredArgsConstructor
public class FederateApplication implements CommandLineRunner {

    private final ArgsResolverConfig argsResolverConfig;
    private final FederationService federationService;

	public static void main(String[] args) {
		SpringApplication.run(FederateApplication.class, args);
	}

    @Override
    public void run(String... args) throws Exception {
        log.info("FederateApplication started : {}", System.getProperty("user.home"));
        log.info("FederateApplication version : {}", System.getProperty("federate.version"));
        if(args.length ==0) {
            printUsage();
            return;
        }
        argsResolverConfig.setArgs(args);
        federationService.execute();
    }

    private void printUsage() {
	    StringBuilder builder = new StringBuilder();
        builder.append("\n Usage :")
                .append("\n java -jar Federate <key> <uname> <pwd>");
	    log.info("{}", builder.toString());
    }
}
