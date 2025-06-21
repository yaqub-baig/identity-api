package com.platform.auth.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.platform.auth.entity.Offer;

@Repository
public interface OfferRepository extends JpaRepository<Offer, String> {
}
