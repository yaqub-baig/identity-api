package com.platform.auth.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.platform.auth.entity.AuthenticationDetails;

@Repository
public interface AuthenticationDetailsRepository extends JpaRepository<AuthenticationDetails, Integer>{

	AuthenticationDetails findByUserName(String username);

	AuthenticationDetails findByMemberIdAndMemberType(Integer memberId, String memberType);

}
