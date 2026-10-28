package com.staffapp.housekeeping.establishment;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * A campsite. Every room and every staff member belongs to exactly one establishment.
 */
@Entity
public class Establishment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    protected Establishment() {
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
