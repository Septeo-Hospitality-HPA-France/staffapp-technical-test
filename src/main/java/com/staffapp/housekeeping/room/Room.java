package com.staffapp.housekeeping.room;

import com.staffapp.housekeeping.establishment.Establishment;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.time.Instant;

/**
 * An accommodation of a campsite: mobile home, chalet or tent. Its number is unique within its establishment.
 */
@Entity
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String number;

    @Enumerated(EnumType.STRING)
    private RoomType type;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Establishment establishment;

    private Instant lastCleanedAt;

    protected Room() {
    }

    public Long getId() {
        return id;
    }

    public String getNumber() {
        return number;
    }

    public RoomType getType() {
        return type;
    }

    public Establishment getEstablishment() {
        return establishment;
    }

    public Instant getLastCleanedAt() {
        return lastCleanedAt;
    }
}
