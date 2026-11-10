package com.staffapp.housekeeping.pms;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface PmsNotificationRepository extends JpaRepository<PmsNotification, Long> {

    /**
     * The oldest notifications still to send, at most 100 per run of the relay.
     */
    List<PmsNotification> findTop100BySentAtIsNullOrderByIdAsc();
}
