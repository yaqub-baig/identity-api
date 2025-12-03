package com.autoadda.apis.identity.dao;


import com.autoadda.apis.identity.entity.CommunicationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommunicationPreferenceRepository extends JpaRepository<CommunicationPreference, Integer> {
    Optional<CommunicationPreference> findByUserId(Integer customerId);
}
