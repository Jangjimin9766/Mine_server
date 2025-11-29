package com.mine.api.repository;

import com.mine.api.domain.Follow;
import com.mine.api.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    boolean existsByFollowerAndFollowing(User follower, User following);

    void deleteByFollowerAndFollowing(User follower, User following);

    List<Follow> findByFollower(User follower);

    List<Follow> findByFollowing(User following);

    java.util.Optional<Follow> findByFollowerAndFollowing(User follower, User following);

    org.springframework.data.domain.Page<Follow> findByFollower(User follower,
            org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<Follow> findByFollowing(User following,
            org.springframework.data.domain.Pageable pageable);

    long countByFollower(User follower);

    long countByFollowing(User following);
}
