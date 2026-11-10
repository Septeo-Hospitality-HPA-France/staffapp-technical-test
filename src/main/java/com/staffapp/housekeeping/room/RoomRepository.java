package com.staffapp.housekeeping.room;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findByEstablishmentIdOrderByNumber(Long establishmentId);

    /**
     * Use this rather than {@code findById}: a room is only visible from its own establishment.
     */
    Optional<Room> findByIdAndEstablishmentId(Long id, Long establishmentId);
}
