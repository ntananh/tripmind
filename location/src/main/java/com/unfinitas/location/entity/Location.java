package com.unfinitas.location.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "locations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(name = "stop_id")
    private String stopId;

    private Double latitude;
    private Double longitude;

    @Enumerated(EnumType.STRING)
    private LocationType type;

    public enum LocationType {
        STOP, ADDRESS, POI
    }
}
