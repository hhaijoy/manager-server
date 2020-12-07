package com.example.demo.domain.support;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by wang ming on 2019/2/18.
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeoInfoMeta {

    String city;
    String countryCode;
    String latitude;
    String countryName;
    String region;
    String longitude;
}
