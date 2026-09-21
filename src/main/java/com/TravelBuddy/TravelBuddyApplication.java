package com.TravelBuddy;

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import com.TravelBuddy.model.Disaster.DisasterDetails;
import com.TravelBuddy.model.Disaster.DisasterEvents;
import com.TravelBuddy.model.Disaster.LocationCoordinates;
import com.TravelBuddy.service.GdacsService;

@SpringBootApplication
public class TravelBuddyApplication {

	public static void main(String[] args) {
		ApplicationContext ctx = SpringApplication.run(TravelBuddyApplication.class, args);
		//SpringApplication.run(TravelBuddyApplication.class, args);

		System.out.println("Testing gdacs api");
		GdacsService gdacs = ctx.getBean(GdacsService.class);
		List<DisasterEvents> allDisaster = gdacs.getDisasterEvents().getAllDisastersEvents();
		for (DisasterEvents d : allDisaster) {
			DisasterDetails disasterDetails = d.getDisasterDetails();
			LocationCoordinates disasterSpecifiedLocation = d.getLocationCoordinates();

			System.out.printf("Disaster Info %d: Specific coordinates: %s, Location: %s, Type: %s, Alert Level: %s, Start: %s, End: %s, Affected countries: %s\n",
			disasterDetails.getDisasterId(),Arrays.toString(disasterSpecifiedLocation.getCoordinates()), disasterDetails.getDisasterCountryLocation(), disasterDetails.getDisasterType().getDisasterName(),
			disasterDetails.getAlertLevel(), disasterDetails.getDisasterStartDate(), disasterDetails.getDisasterEndDate(), disasterDetails.getAffectedCountries());
		}
		
		System.out.println("Done");
	}

}
