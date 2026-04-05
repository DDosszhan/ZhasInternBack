package com.production.ZhasIntern.repository;

import com.production.ZhasIntern.entity.School;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Collection;
import java.util.UUID;

public interface SchoolRepository extends JpaRepository<School, UUID> {

    Optional<School> findBySourceAndSourceVersionAndExternalId(String source, String sourceVersion, String externalId);

    List<School> findAllBySourceAndSourceVersionAndExternalIdIn(String source, String sourceVersion, Collection<String> externalIds);

    @Query("""
            select s from School s
            where (:region is null or lower(s.regionRu) = :region)
              and (:district is null or lower(s.districtRu) = :district)
              and (:locality is null or lower(s.localityRu) = :locality)
            order by s.schoolNameRu asc
            """)
    List<School> findByFilters(
            @Param("region") String region,
            @Param("district") String district,
            @Param("locality") String locality
    );

    @Query("""
            select s from School s
            where (:region is null or lower(s.regionRu) = :region)
              and (:district is null or lower(s.districtRu) = :district)
              and (:locality is null or lower(s.localityRu) = :locality)
              and (
                   s.normalizedName like concat('%', :search, '%')
                   or lower(coalesce(s.regionRu, '')) like concat('%', :search, '%')
                   or lower(coalesce(s.districtRu, '')) like concat('%', :search, '%')
                   or lower(coalesce(s.localityRu, '')) like concat('%', :search, '%')
              )
            order by s.schoolNameRu asc
            """)
    List<School> searchByFilters(
            @Param("search") String search,
            @Param("region") String region,
            @Param("district") String district,
            @Param("locality") String locality
    );

    @Query("select distinct s.regionRu from School s where s.regionRu is not null and s.regionRu <> '' order by s.regionRu asc")
    List<String> findDistinctRegionsRu();

    @Query("""
            select distinct s.districtRu from School s
            where (:region is null or lower(s.regionRu) = :region)
              and s.districtRu is not null and s.districtRu <> ''
            order by s.districtRu asc
            """)
    List<String> findDistinctDistrictsRu(@Param("region") String region);

    @Query("""
            select distinct s.localityRu from School s
            where (:region is null or lower(s.regionRu) = :region)
              and (:district is null or lower(s.districtRu) = :district)
              and s.localityRu is not null and s.localityRu <> ''
            order by s.localityRu asc
            """)
    List<String> findDistinctLocalitiesRu(@Param("region") String region, @Param("district") String district);
}
