package com.noserbulgaria.micromarket.customer;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {

  @EntityGraph(attributePaths = {"customer", "user"})
  Optional<Profile> findByUserId(UUID userId);

  @EntityGraph(attributePaths = {"customer", "user"})
  Optional<Profile> findByCustomer_Email(String email);

  @Modifying
  @Query(
      """
          update Profile p
          set p.points = p.points - :points,
              p.lastChangeReason = :reason
          where p.user.id = :userId
            and p.points >= :points
          """
  )
  int trySpendPoints(
      @Param("userId") UUID userId,
      @Param("points") long points,
      @Param("reason") PointChangeReason reason
  );

  @Modifying
  @Query(
      """
          update Profile p
          set p.points = p.points + :points,
              p.lastChangeReason = :reason
          where p.id = :profileId
          """
  )
  int addPoints(
      @Param("profileId") UUID profileId,
      @Param("points") long points,
      @Param("reason") PointChangeReason reason
  );
}
