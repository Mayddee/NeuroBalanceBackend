package org.example.nbcheckinservice.repository;

import org.example.nbcheckinservice.entity.DailyTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyTaskRepository extends JpaRepository<DailyTask, Long> {

    List<DailyTask> findByUserIdAndTaskDate(Long userId, LocalDate taskDate);

    Optional<DailyTask> findByUserIdAndTaskDateAndTaskType(
            Long userId,
            LocalDate taskDate,
            DailyTask.TaskType taskType
    );
    List<DailyTask> findByUserIdAndTaskDateBetween(Long userId, LocalDate startDate, LocalDate endDate);
    long countByUserIdAndTaskDateAndIsCompletedTrue(Long userId, LocalDate taskDate);
}