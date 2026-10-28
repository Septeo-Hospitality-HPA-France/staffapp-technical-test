package com.staffapp.housekeeping.staff;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffRepository extends JpaRepository<Staff, Long> {

    Optional<Staff> findByUsername(String username);
}
