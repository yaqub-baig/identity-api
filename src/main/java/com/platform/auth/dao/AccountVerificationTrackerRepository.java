package com.platform.auth.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.platform.auth.entity.AccountVerificationTracker;

@Repository
public interface AccountVerificationTrackerRepository extends JpaRepository<AccountVerificationTracker, Integer>{

	AccountVerificationTracker findByToken(String token);

	AccountVerificationTracker findByTokenAndStatus(String token, String status);

	AccountVerificationTracker findByMemberIdAndMemberTypeAndStatus(int memberId, String memberType, String string);

}
