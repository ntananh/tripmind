package com.unfinitas.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationDTO {
    private @Nullable String id;
    private @Nullable String name;
    private @Nullable String stopId;
    private @Nullable Double latitude;
    private @Nullable Double longitude;
    private @Nullable String type;
}
