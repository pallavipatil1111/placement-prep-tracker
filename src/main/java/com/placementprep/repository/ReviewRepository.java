package com.placementprep.repository;

import com.placementprep.domain.Review;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByProblemIdOrderByReviewedOnAscIdAsc(Long problemId);

    @Query("select new com.placementprep.repository.DayCount(r.reviewedOn, count(r)) "
            + "from Review r where r.problem.user.id = :userId group by r.reviewedOn")
    List<DayCount> reviewsPerDay(@Param("userId") Long userId);

    @Modifying
    @Query("delete from Review r where r.problem.id = :problemId")
    void deleteByProblemId(@Param("problemId") Long problemId);
}
