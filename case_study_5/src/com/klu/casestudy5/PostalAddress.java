package com.klu.casestudy5;

import java.util.Objects;

public record PostalAddress(String street, String city, String postalCode, String country) {
    public PostalAddress {
        Objects.requireNonNull(street, "Street cannot be null");
        Objects.requireNonNull(city, "City cannot be null");
        Objects.requireNonNull(postalCode, "Postal code cannot be null");
        Objects.requireNonNull(country, "Country cannot be null");

        if (street.isBlank()) throw new IllegalArgumentException("Street cannot be blank");
        if (city.isBlank()) throw new IllegalArgumentException("City cannot be blank");
        if (postalCode.isBlank()) throw new IllegalArgumentException("Postal code cannot be blank");
        if (country.isBlank()) throw new IllegalArgumentException("Country cannot be blank");
    }

    @Override
    public String toString() {
        return street + ", " + city + ", " + postalCode + ", " + country;
    }
}
