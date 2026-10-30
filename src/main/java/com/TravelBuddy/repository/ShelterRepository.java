package com.TravelBuddy.repository;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.TravelBuddy.model.Shelter;

@Repository
public class ShelterRepository {
    private final JdbcTemplate jdbcTemplate;

    public ShelterRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Shelter> getAllShelters() {
        String sql = """
                SELECT shelter_id, shelter_name, latitude, longitude, facility_type, country
                FROM safe_shelter
                """;

        return jdbcTemplate.query(sql, (resultSet, rowNum) -> {
            Shelter shelter = new Shelter();
            shelter.setShelter_id(resultSet.getInt("shelter_id"));
            shelter.setShelter_name(resultSet.getString("shelter_name"));
            shelter.setLatitude(resultSet.getDouble("latitude"));
            shelter.setLongitude(resultSet.getDouble("longitude"));
            shelter.setFacility_type(resultSet.getString("facility_type"));
            shelter.setCountry(resultSet.getString("country"));
            return shelter;
        });
    }
}