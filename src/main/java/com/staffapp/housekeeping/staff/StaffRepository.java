package com.staffapp.housekeeping.staff;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffRepository extends JpaRepository<Staff, Long> {

    Optional<Staff> findByUsername(String username);

    /**
     * Use this rather than {@code findById}: an employee is only visible from their own establishment.
     */
    Optional<Staff> findByIdAndEstablishmentId(Long id, Long establishmentId);
}
