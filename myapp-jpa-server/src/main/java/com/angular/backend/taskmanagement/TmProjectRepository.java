package com.angular.backend.taskmanagement;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TmProjectRepository extends JpaRepository<TmProject, Long> {

	List<TmProject> findAllByOrderByNameAscIdAsc();

	List<TmProject> findDistinctByMembersIdOrderByNameAscIdAsc(String userId);

	boolean existsByIdAndMembersId(Long projectId, String userId);

	@EntityGraph(attributePaths = "members")
	@Query("select p from TmProject p")
	List<TmProject> findAllWithMembers();

	@EntityGraph(attributePaths = "members")
	@Query("select p from TmProject p where p.id = :projectId")
	Optional<TmProject> findWithMembersById(@Param("projectId") Long projectId);
}
