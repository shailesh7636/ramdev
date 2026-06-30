package com.ramdev.repository;

import com.ramdev.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByMobile(String mobile);
    
    @Query("SELECT u FROM User u JOIN FETCH u.roles WHERE u.mobile = :mobile")
    Optional<User> findByMobileWithRoles(@Param("mobile") String mobile);
    
    boolean existsByMobile(String mobile);
    
    @Query("SELECT u FROM User u JOIN FETCH u.roles r WHERE r.name = :roleName")
    List<User> findAllByRoles_Name(@Param("roleName") String roleName);
}
