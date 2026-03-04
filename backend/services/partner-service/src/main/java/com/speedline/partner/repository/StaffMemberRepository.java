package com.speedline.partner.repository;

import com.speedline.partner.domain.StaffMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StaffMemberRepository extends JpaRepository<StaffMember, Long> {

    List<StaffMember> findByPartnerIdOrderByCreatedAtAsc(Long partnerId);
}
