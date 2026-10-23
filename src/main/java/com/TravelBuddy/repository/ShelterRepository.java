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
                SELECT shelter_id, shelter_name, latitude, longitude, facility_type
                FROM safe_shelter
                """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> {
                    Shelter shelter = new Shelter();
                    shelter.setShelter_id(rs.getInt("shelter_id"));
                    shelter.setShelter_name(rs.getString("shelter_name"));
                    shelter.setLatitude(rs.getDouble("latitude"));
                    shelter.setLongitude(rs.getDouble("longitude"));
                    shelter.setFacility_type(rs.getString("facility_type"));
                    return shelter;
                }
        );
    }
}