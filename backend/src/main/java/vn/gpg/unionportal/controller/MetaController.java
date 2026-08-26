package vn.gpg.unionportal.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.gpg.unionportal.i18n.EnumLabels;

import java.util.Map;

/**
 * Reference data the frontend needs but should not duplicate.
 *
 * <p>The Vietnamese enum labels drive both display and server-side search. Serving them from here
 * keeps the two in step: the frontend hydrates its label map from this endpoint instead of shipping
 * its own copy that could drift from what the search specifications match on.
 */
@RestController
@RequestMapping("/api/meta")
public class MetaController {
    @GetMapping("/enum-labels")
    public Map<String, String> enumLabels() {
        return EnumLabels.all();
    }
}
