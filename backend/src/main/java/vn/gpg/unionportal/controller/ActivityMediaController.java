package vn.gpg.unionportal.controller;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.gpg.unionportal.dto.ApiModels.ActivityMediaView;
import vn.gpg.unionportal.dto.ApiModels.ListFacets;
import vn.gpg.unionportal.dto.ApiModels.PageResponse;
import vn.gpg.unionportal.dto.ListQuery;
import vn.gpg.unionportal.model.DomainEnums.ActivityMediaType;
import vn.gpg.unionportal.service.ActivityMediaService;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/activity-media")
public class ActivityMediaController {
    private final ActivityMediaService service;

    public ActivityMediaController(ActivityMediaService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<ActivityMediaView> list(@RequestParam(required = false) Long activityId,
                                                @RequestParam(required = false) String activityStatus,
                                                @ModelAttribute ListQuery query) {
        return query.fetchAll()
                ? PageResponse.ofAll(service.search(query, activityId, activityStatus))
                : PageResponse.of(service.page(query, activityId, activityStatus));
    }

    @GetMapping("/facets")
    public ListFacets facets(@RequestParam(required = false) Long activityId,
                             @RequestParam(required = false) String activityStatus,
                             @ModelAttribute ListQuery query) {
        return service.facets(query, activityId, activityStatus);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ActivityMediaView upload(@RequestParam("activityId") Long activityId,
                                    @RequestParam("mediaType") ActivityMediaType mediaType,
                                    @RequestParam(value = "title", required = false) String title,
                                    @RequestPart("file") MultipartFile file) {
        return service.upload(activityId, mediaType, title, file);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        var file = service.download(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.fileName(), StandardCharsets.UTF_8).build().toString())
                .body(file.data());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
