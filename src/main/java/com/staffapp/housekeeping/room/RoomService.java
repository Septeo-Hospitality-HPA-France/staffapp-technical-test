package com.staffapp.housekeeping.room;

import com.staffapp.housekeeping.error.NotFoundException;
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
     * Returns a room of an establishment by id.
     *
     * @throws NotFoundException if the room does not exist or belongs to another establishment: both cases give the
     *                           same answer, so that an id never reveals whether it exists elsewhere
     */
    @Transactional(readOnly = true)
    public RoomResponse findById(Long id, Long establishmentId) {
        return roomRepository.findByIdAndEstablishmentId(id, establishmentId)
                .map(RoomResponse::from)
                .orElseThrow(() -> new NotFoundException("Room", id));
    }
}
