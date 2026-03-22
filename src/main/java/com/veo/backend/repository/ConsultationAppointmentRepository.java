package com.veo.backend.repository;

import com.veo.backend.entity.ConsultationAppointment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ConsultationAppointmentRepository extends JpaRepository<ConsultationAppointment, Long> {
    List<ConsultationAppointment> findAllByOrderByAppointmentTimeAscIdDesc();
}
