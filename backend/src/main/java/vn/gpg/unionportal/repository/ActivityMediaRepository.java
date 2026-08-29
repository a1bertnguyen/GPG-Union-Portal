package vn.gpg.unionportal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import vn.gpg.unionportal.model.ActivityMedia;
import vn.gpg.unionportal.model.DomainEnums.ActivityMediaType;

import java.util.List;

public interface ActivityMediaRepository extends JpaRepository<ActivityMedia, Long>, JpaSpecificationExecutor<ActivityMedia> {
    List<ActivityMedia> findByActivityIdOrderByCreatedAtDesc(Long activityId);

    boolean existsByActivityIdAndMediaType(Long activityId, ActivityMediaType mediaType);
}
