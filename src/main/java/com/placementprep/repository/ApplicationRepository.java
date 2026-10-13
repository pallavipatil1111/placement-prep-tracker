package com.placementprep.repository;

import com.placementprep.core.Stage;
import com.placementprep.domain.Application;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    List<Application> findByUserIdOrderByUpdatedAtDesc(Long userId);

    List<Application> findByUserIdAndStageOrderByUpdatedAtDesc(Long userId, Stage stage);

    Optional<Application> findByIdAndUserId(Long id, Long userId);

    List<Application> findTop5ByUserIdAndNextStepOnGreaterThanEqualOrderByNextStepOnAsc(Long userId, LocalDate from);

    @Query("select new com.placementprep.repository.StageCount(a.stage, count(a)) "
            + "from Application a where a.user.id = :userId group by a.stage")
    List<StageCount> countByStage(@Param("userId") Long userId);
}
