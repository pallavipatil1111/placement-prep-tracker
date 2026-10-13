package com.placementprep.repository;

import com.placementprep.domain.StageChange;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StageChangeRepository extends JpaRepository<StageChange, Long> {

    List<StageChange> findByApplicationIdOrderByChangedAtAscIdAsc(Long applicationId);

    @Query("select new com.placementprep.repository.StageVisit(sc.application.id, sc.toStage) "
            + "from StageChange sc where sc.application.user.id = :userId")
    List<StageVisit> findVisitsForUser(@Param("userId") Long userId);

    @Modifying
    @Query("delete from StageChange sc where sc.application.id = :applicationId")
    void deleteByApplicationId(@Param("applicationId") Long applicationId);
}
