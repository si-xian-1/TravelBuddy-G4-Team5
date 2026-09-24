package com.TravelBuddy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import com.TravelBuddy.service.GdacsService;

@SpringBootApplication
public class TravelBuddyApplication {

	public static void main(String[] args) {
		ApplicationContext ctx = SpringApplication.run(TravelBuddyApplication.class, args);

		// Retrieve disasters information on start up
		System.out.println("Retrieving disasters information from GDACS api...");
        
		GdacsService gdacs = ctx.getBean(GdacsService.class);
        gdacs.loadDisastersOnStartup();
	}

}
