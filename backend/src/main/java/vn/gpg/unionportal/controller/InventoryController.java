package vn.gpg.unionportal.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.gpg.unionportal.dto.ApiModels.PageResponse;
import vn.gpg.unionportal.dto.ListQuery;
import vn.gpg.unionportal.dto.InventoryModels.GiftIssueRequest;
import vn.gpg.unionportal.dto.InventoryModels.GiftIssueView;
import vn.gpg.unionportal.dto.InventoryModels.InventorySummary;
import vn.gpg.unionportal.dto.InventoryModels.ItemRequest;
import vn.gpg.unionportal.dto.InventoryModels.ItemView;
import vn.gpg.unionportal.dto.InventoryModels.ReceiptRequest;
import vn.gpg.unionportal.dto.InventoryModels.ReceiptView;
import vn.gpg.unionportal.dto.InventoryModels.RecipientSuggestionView;
import vn.gpg.unionportal.service.InventoryService;

/** REST API for catalogue items, receipt slips and employee gift-issue slips. */
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    private final InventoryService service;

    public InventoryController(InventoryService service) {
        this.service = service;
    }

    @GetMapping("/items")
    public PageResponse<ItemView> listItems(@ModelAttribute ListQuery query) {
        return PageResponse.from(query, service::pageItems, service::searchItems);
    }

    @GetMapping("/items/{id}")
    public ItemView getItem(@PathVariable Long id) {
        return service.getItem(id);
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public ItemView createItem(@Valid @RequestBody ItemRequest request) {
        return service.createItem(request);
    }

    @PutMapping("/items/{id}")
    public ItemView updateItem(@PathVariable Long id, @Valid @RequestBody ItemRequest request) {
        return service.updateItem(id, request);
    }

    @DeleteMapping("/items/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItem(@PathVariable Long id) {
        service.deleteItem(id);
    }

    @GetMapping("/receipts")
    public PageResponse<ReceiptView> listReceipts(@ModelAttribute ListQuery query,
                                                   @RequestParam(required = false) Long itemId) {
        return PageResponse.from(query, value -> service.pageReceipts(value, itemId),
                value -> service.searchReceipts(value, itemId));
    }

    @GetMapping("/receipts/{id}")
    public ReceiptView getReceipt(@PathVariable Long id) {
        return service.getReceipt(id);
    }

    @PostMapping("/receipts")
    @ResponseStatus(HttpStatus.CREATED)
    public ReceiptView createReceipt(@Valid @RequestBody ReceiptRequest request) {
        return service.createReceipt(request);
    }

    @PutMapping("/receipts/{id}")
    public ReceiptView updateReceipt(@PathVariable Long id, @Valid @RequestBody ReceiptRequest request) {
        return service.updateReceipt(id, request);
    }

    @DeleteMapping("/receipts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReceipt(@PathVariable Long id) {
        service.deleteReceipt(id);
    }

    @GetMapping("/issues")
    public PageResponse<GiftIssueView> listIssues(@ModelAttribute ListQuery query,
                                                   @RequestParam(required = false) Long itemId) {
        return PageResponse.from(query, value -> service.pageIssues(value, itemId),
                value -> service.searchIssues(value, itemId));
    }

    @GetMapping("/issues/{id}")
    public GiftIssueView getIssue(@PathVariable Long id) {
        return service.getIssue(id);
    }

    @PostMapping("/issues")
    @ResponseStatus(HttpStatus.CREATED)
    public GiftIssueView createIssue(@Valid @RequestBody GiftIssueRequest request) {
        return service.createIssue(request);
    }

    @PutMapping("/issues/{id}")
    public GiftIssueView updateIssue(@PathVariable Long id, @Valid @RequestBody GiftIssueRequest request) {
        return service.updateIssue(id, request);
    }

    @DeleteMapping("/issues/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteIssue(@PathVariable Long id) {
        service.deleteIssue(id);
    }

    @GetMapping("/recipients")
    public PageResponse<RecipientSuggestionView> recipients(@ModelAttribute ListQuery query) {
        return PageResponse.from(query, service::pageRecipients, service::searchRecipients);
    }

    @GetMapping("/summary")
    public InventorySummary summary(@RequestParam(required = false) Long unitId) {
        return service.summary(unitId);
    }
}
