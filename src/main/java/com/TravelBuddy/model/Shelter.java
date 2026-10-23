package com.TravelBuddy.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Shelter {
    private Integer shelter_id;
    private String shelter_name;
    private Double latitude;
    private Double longitude;
    private String facility_type;
}
