package com.fnb.menu.repository;

import com.fnb.menu.entity.RestaurantProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RestaurantProfileRepository extends JpaRepository<RestaurantProfile, UUID> {
}
