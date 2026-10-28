package com.staffapp.housekeeping.room;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findByEstablishmentIdOrderByNumber(Long establishmentId);
}
