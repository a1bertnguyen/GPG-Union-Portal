package vn.gpg.unionportal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.gpg.unionportal.model.ActivityMedia;
import vn.gpg.unionportal.model.DomainEnums.ActivityMediaType;

import java.util.Collection;
import java.util.List;

public interface ActivityMediaRepository extends JpaRepository<ActivityMedia, Long>, JpaSpecificationExecutor<ActivityMedia> {
    List<ActivityMedia> findByActivityIdOrderByCreatedAtDesc(Long activityId);

    boolean existsByActivityIdAndMediaType(Long activityId, ActivityMediaType mediaType);

    /** Activities that actually have a stored photo or document, for the KPI evidence check. */
    @Query("select distinct media.activity.id from ActivityMedia media where media.activity.id in :ids")
    List<Long> findActivityIdsWithMedia(@Param("ids") Collection<Long> ids);
}
