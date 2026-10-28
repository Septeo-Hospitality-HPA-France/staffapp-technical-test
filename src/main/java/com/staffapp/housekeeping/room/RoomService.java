package com.staffapp.housekeeping.room;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomService {

    private final RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    /**
     * Lists the rooms of an establishment, ordered by number.
     */
    @Transactional(readOnly = true)
    public List<RoomResponse> findByEstablishment(Long establishmentId) {
        return roomRepository.findByEstablishmentIdOrderByNumber(establishmentId).stream()
                .map(RoomResponse::from)
                .toList();
    }

    /**
     * Returns a room by id.
     */
    @Transactional(readOnly = true)
    public RoomResponse findById(Long id) {
        return roomRepository.findById(id)
                .map(RoomResponse::from)
                .orElseThrow(() -> new RuntimeException("Room not found: " + id));
    }
}
