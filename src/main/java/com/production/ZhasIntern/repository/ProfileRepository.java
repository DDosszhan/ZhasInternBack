package com.production.ZhasIntern.repository;

import com.production.ZhasIntern.entity.UserProfile;
import com.production.ZhasIntern.entity.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProfileRepository extends JpaRepository<UserProfile, UUID> {

    Page<UserProfile> findByRoleAndSchoolEntity_IdOrderByFullNameAsc(UserRole role, UUID schoolId, Pageable pageable);

    @Query("""
            select p
            from UserProfile p
            where p.role = :role
              and lower(coalesce(p.school, '')) = lower(:schoolName)
            order by p.fullName asc
            """)
    Page<UserProfile> findByRoleAndSchoolNameIgnoreCase(
            @Param("role") UserRole role,
            @Param("schoolName") String schoolName,
            Pageable pageable
    );
}
