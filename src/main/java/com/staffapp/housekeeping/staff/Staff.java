package com.staffapp.housekeeping.staff;

import com.staffapp.housekeeping.establishment.Establishment;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/**
 * A staff member of a campsite. The username is the {@code sub} claim of the user's token.
 */
@Entity
public class Staff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    private String firstName;

    @Enumerated(EnumType.STRING)
    private StaffRole role;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Establishment establishment;

    protected Staff() {
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getFirstName() {
        return firstName;
    }

    public StaffRole getRole() {
        return role;
    }

    public Establishment getEstablishment() {
        return establishment;
    }
}
