package com.placementprep.repository;

import com.placementprep.domain.Difficulty;
import com.placementprep.domain.Problem;
import com.placementprep.domain.Topic;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProblemRepository extends JpaRepository<Problem, Long> {

    Optional<Problem> findByIdAndUserId(Long id, Long userId);

    @Query("select p from Problem p where p.user.id = :userId "
            + "and (:topic is null or p.topic = :topic) "
            + "and (:difficulty is null or p.difficulty = :difficulty) "
            + "and (:q is null or lower(p.title) like lower(concat('%', :q, '%'))) "
            + "order by p.solvedOn desc, p.id desc")
    List<Problem> search(@Param("userId") Long userId, @Param("topic") Topic topic,
                         @Param("difficulty") Difficulty difficulty, @Param("q") String q);

    /** Revision queue: everything due today or overdue, most overdue first. */
    List<Problem> findByUserIdAndNextReviewOnLessThanEqualOrderByNextReviewOnAscIdAsc(Long userId, LocalDate today);

    long countByUserId(Long userId);

    long countByUserIdAndNextReviewOnLessThanEqual(Long userId, LocalDate today);

    @Query("select new com.placementprep.repository.DayCount(p.solvedOn, count(p)) "
            + "from Problem p where p.user.id = :userId group by p.solvedOn")
    List<DayCount> solvedPerDay(@Param("userId") Long userId);

    @Query("select new com.placementprep.repository.TopicCount(p.topic, count(p)) "
            + "from Problem p where p.user.id = :userId group by p.topic")
    List<TopicCount> countByTopic(@Param("userId") Long userId);

    @Query("select new com.placementprep.repository.DifficultyCount(p.difficulty, count(p)) "
            + "from Problem p where p.user.id = :userId group by p.difficulty")
    List<DifficultyCount> countByDifficulty(@Param("userId") Long userId);
}
