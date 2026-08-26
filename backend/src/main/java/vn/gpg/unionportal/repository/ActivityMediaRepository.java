package vn.gpg.unionportal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import vn.gpg.unionportal.model.ActivityMedia;

import java.util.List;

public interface ActivityMediaRepository extends JpaRepository<ActivityMedia, Long>, JpaSpecificationExecutor<ActivityMedia> {
    List<ActivityMedia> findByActivityIdOrderByCreatedAtDesc(Long activityId);
}
