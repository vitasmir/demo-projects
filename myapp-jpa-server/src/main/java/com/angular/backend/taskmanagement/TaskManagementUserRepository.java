package com.angular.backend.taskmanagement;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskManagementUserRepository extends JpaRepository<TaskManagementUser, String> {

    Optional<TaskManagementUser> findByUsername(String username);

    boolean existsByUsername(String username);

    List<TaskManagementUser> findAllByOrderByDisplayNameAscUsernameAsc();

    @Query("""
            select distinct u
            from TaskManagementUser u
            left join u.accessibleProjects p
            where p.id = :projectId or u.admin = true
            order by u.displayName asc, u.username asc
            """)
    List<TaskManagementUser> findUsersVisibleForProject(@Param("projectId") Long projectId);
}
