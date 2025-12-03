package com.autoadda.apis.identity.dao;

import com.autoadda.apis.identity.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    boolean existsByMobileNumber(String mobileNumber);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.mobileNumber = :mobileNumber AND u.isVerified = true")
    boolean existsByMobileNumberAndIsVerified(@Param("mobileNumber") String mobileNumber);

    Optional<User> findByMobileNumber(String mobileNumber);

    @Query("SELECT u FROM User u WHERE u.mobileNumber = :mobileNumber AND u.isVerified = true")
    Optional<User> findByMobileNumberAndIsVerified(@Param("mobileNumber") String mobileNumber);

    @Query("SELECT u FROM User u WHERE u.email = :email AND u.isVerified = true")
    Optional<User> findByEmailAndIsVerified(@Param("email") String email);

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

}
