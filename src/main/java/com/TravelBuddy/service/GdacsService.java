package com.TravelBuddy.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.TravelBuddy.model.Disaster;

@Service
public class GdacsService {
    // External api connecting to GDACS api
    private final RestClient restClient;
    private final String base_url = "https://www.gdacs.org/gdacsapi/api";

    public GdacsService(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    public Disaster getDisasterEvents() {
        return restClient.get()
                .uri(base_url + "/Events/geteventlist/events4app")
                .retrieve()
                .body(Disaster.class);
    }
}
