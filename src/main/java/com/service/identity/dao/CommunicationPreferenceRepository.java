package com.service.identity.dao;


import com.service.identity.entity.CommunicationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommunicationPreferenceRepository extends JpaRepository<CommunicationPreference, Integer> {
	Optional<CommunicationPreference> findByCustomerId(Integer customerId);

	Optional<CommunicationPreference> findByIdAndCustomerId(Integer id, Integer customerId);

	Optional<CommunicationPreference> findByCustomerIdAndStatus(Integer customerId, String status);

}
