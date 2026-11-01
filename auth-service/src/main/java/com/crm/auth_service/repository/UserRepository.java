package com.crm.auth_service.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.crm.auth_service.entity.User;

public interface UserRepository extends JpaRepository<User, UUID> {
	
		Optional<User> findByEmail(String email);

	    boolean existsByEmail(String email);

	    @EntityGraph(attributePaths = {"roles"})
	    @Query("SELECT u FROM User u WHERE u.id = :id")
	    Optional<User> findByIdWithRoles(@Param("id") UUID id);

	    @EntityGraph(attributePaths = {"roles"})
	    @Query("SELECT u FROM User u WHERE u.email = :email")
	    Optional<User> findByEmailWithRoles(@Param("email") String email);	
	
}