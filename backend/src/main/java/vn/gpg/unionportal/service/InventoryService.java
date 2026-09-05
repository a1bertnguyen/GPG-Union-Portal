package vn.gpg.unionportal.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gpg.unionportal.dto.ListQuery;
import vn.gpg.unionportal.dto.InventoryModels.GiftIssueRequest;
import vn.gpg.unionportal.dto.InventoryModels.GiftIssueView;
import vn.gpg.unionportal.dto.InventoryModels.InventorySummary;
import vn.gpg.unionportal.dto.InventoryModels.ItemRequest;
import vn.gpg.unionportal.dto.InventoryModels.ItemView;
import vn.gpg.unionportal.dto.InventoryModels.ReceiptRequest;
import vn.gpg.unionportal.dto.InventoryModels.ReceiptView;
import vn.gpg.unionportal.dto.InventoryModels.RecipientSuggestionView;
import vn.gpg.unionportal.exception.ResourceNotFoundException;
import vn.gpg.unionportal.model.InventoryGiftIssue;
import vn.gpg.unionportal.model.InventoryItem;
import vn.gpg.unionportal.model.InventoryReceipt;
import vn.gpg.unionportal.model.Member;
import vn.gpg.unionportal.model.UnionUnit;
import vn.gpg.unionportal.model.DomainEnums.EmploymentStatus;
import vn.gpg.unionportal.model.DomainEnums.MembershipStatus;
import vn.gpg.unionportal.repository.InventoryGiftIssueRepository;
import vn.gpg.unionportal.repository.InventoryItemQuantity;
import vn.gpg.unionportal.repository.InventoryItemRepository;
import vn.gpg.unionportal.repository.InventoryReceiptRepository;
import vn.gpg.unionportal.repository.MemberRepository;
import vn.gpg.unionportal.repository.UnionUnitRepository;
import vn.gpg.unionportal.spec.InventorySpecs;
import vn.gpg.unionportal.spec.MemberSpecs;
import vn.gpg.unionportal.spec.Specs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Inventory ledger for each CĐCS. Balances are derived from receipt and gift-issue slips rather
 * than being stored in a mutable column, making the stock calculation auditable.
 */
@Service
@Transactional(readOnly = true)
public class InventoryService {
    private static final Sort ITEM_SORT = Sort.by(Sort.Direction.ASC, "itemName")
            .and(Sort.by(Sort.Direction.ASC, "itemCode"));
    private static final Sort RECEIPT_SORT = Sort.by(Sort.Direction.DESC, "receiptDate")
            .and(Sort.by(Sort.Direction.DESC, "id"));
    private static final Sort ISSUE_SORT = Sort.by(Sort.Direction.DESC, "issueDate")
            .and(Sort.by(Sort.Direction.DESC, "id"));
    private static final Sort RECIPIENT_SORT = Sort.by(Sort.Direction.ASC, "fullName")
            .and(Sort.by(Sort.Direction.ASC, "employeeCode"));

    private final InventoryItemRepository items;
    private final InventoryReceiptRepository receipts;
    private final InventoryGiftIssueRepository issues;
    private final MemberRepository members;
    private final UnionUnitRepository units;
    private final CurrentUserService currentUser;

    public InventoryService(InventoryItemRepository items,
                            InventoryReceiptRepository receipts,
                            InventoryGiftIssueRepository issues,
                            MemberRepository members,
                            UnionUnitRepository units,
                            CurrentUserService currentUser) {
        this.items = items;
        this.receipts = receipts;
        this.issues = issues;
        this.members = members;
        this.units = units;
        this.currentUser = currentUser;
    }

    public Page<ItemView> pageItems(ListQuery query) {
        Page<InventoryItem> result = items.findAll(Specs.nullSafe(InventorySpecs.items(query, scopedUnitId(query))),
                query.pageable(ITEM_SORT));
        Map<Long, Stock> stock = stocks(result.getContent());
        return result.map(item -> itemView(item, stock.getOrDefault(item.getId(), Stock.ZERO)));
    }

    public List<ItemView> searchItems(ListQuery query) {
        List<InventoryItem> result = items.findAll(Specs.nullSafe(InventorySpecs.items(query, scopedUnitId(query))), ITEM_SORT);
        Map<Long, Stock> stock = stocks(result);
        return result.stream().map(item -> itemView(item, stock.getOrDefault(item.getId(), Stock.ZERO))).toList();
    }

    public ItemView getItem(Long id) {
        InventoryItem item = requireItem(id);
        currentUser.requireUnitAccess(item.getUnionUnit().getId());
        return itemView(item, stockFor(item));
    }

    @Transactional
    public ItemView createItem(ItemRequest request) {
        UnionUnit unit = resolveNewItemUnit(request.unionUnitId());
        String code = requiredText(request.itemCode(), "Mã vật phẩm");
        assertItemCodeAvailable(unit.getId(), code, null);
        InventoryItem item = new InventoryItem();
        applyItem(item, request, unit, code);
        InventoryItem saved = items.save(item);
        return itemView(saved, Stock.ZERO);
    }

    @Transactional
    public ItemView updateItem(Long id, ItemRequest request) {
        InventoryItem item = requireItem(id);
        currentUser.requireUnitAccess(item.getUnionUnit().getId());
        UnionUnit targetUnit = resolveItemUpdateUnit(request.unionUnitId(), item.getUnionUnit());
        boolean unitChanged = !Objects.equals(item.getUnionUnit().getId(), targetUnit.getId());
        if (unitChanged && hasSlipHistory(item.getId())) {
            throw new IllegalArgumentException("Không thể đổi CĐCS của vật phẩm đã có phiếu nhập hoặc cấp quà");
        }
        String code = requiredText(request.itemCode(), "Mã vật phẩm");
        assertItemCodeAvailable(targetUnit.getId(), code, item.getId());
        applyItem(item, request, targetUnit, code);
        InventoryItem saved = items.save(item);
        return itemView(saved, stockFor(saved));
    }

    @Transactional
    public void deleteItem(Long id) {
        InventoryItem item = requireItem(id);
        currentUser.requireUnitAccess(item.getUnionUnit().getId());
        if (hasSlipHistory(item.getId())) {
            throw new IllegalArgumentException("Không thể xóa vật phẩm đã có phiếu nhập hoặc cấp quà");
        }
        items.delete(item);
    }

    public Page<ReceiptView> pageReceipts(ListQuery query, Long itemId) {
        return receipts.findAll(Specs.nullSafe(InventorySpecs.receipts(query, scopedUnitId(query), itemId)),
                        query.pageable(RECEIPT_SORT))
                .map(this::receiptView);
    }

    public List<ReceiptView> searchReceipts(ListQuery query, Long itemId) {
        return receipts.findAll(Specs.nullSafe(InventorySpecs.receipts(query, scopedUnitId(query), itemId)), RECEIPT_SORT)
                .stream().map(this::receiptView).toList();
    }

    public ReceiptView getReceipt(Long id) {
        InventoryReceipt receipt = requireReceipt(id);
        currentUser.requireUnitAccess(receipt.getUnionUnit().getId());
        return receiptView(receipt);
    }

    @Transactional
    public ReceiptView createReceipt(ReceiptRequest request) {
        InventoryItem item = lockItem(requiredId(request.itemId(), "Vật phẩm"));
        verifyTransactionUnit(item, request.unionUnitId());
        InventoryReceipt receipt = new InventoryReceipt();
        applyReceipt(receipt, request, item);
        return receiptView(receipts.save(receipt));
    }

    @Transactional
    public ReceiptView updateReceipt(Long id, ReceiptRequest request) {
        InventoryReceipt receipt = lockReceipt(id);
        currentUser.requireUnitAccess(receipt.getUnionUnit().getId());
        Map<Long, InventoryItem> lockedItems = lockItems(receipt.getItem().getId(), requiredId(request.itemId(), "Vật phẩm"));
        InventoryItem oldItem = lockedItems.get(receipt.getItem().getId());
        InventoryItem newItem = lockedItems.get(request.itemId());
        verifyTransactionUnit(newItem, request.unionUnitId());
        forbidUnitChange(receipt.getUnionUnit(), newItem.getUnionUnit(), "phiếu nhập");
        assertReceiptChangeDoesNotMakeStockNegative(oldItem, receipt.getQuantity(), newItem,
                positiveQuantity(request.quantity()));
        applyReceipt(receipt, request, newItem);
        return receiptView(receipts.save(receipt));
    }

    @Transactional
    public void deleteReceipt(Long id) {
        InventoryReceipt receipt = lockReceipt(id);
        currentUser.requireUnitAccess(receipt.getUnionUnit().getId());
        InventoryItem item = lockItem(receipt.getItem().getId());
        ensureNonNegative(stockFor(item).balance() - receipt.getQuantity(), item,
                "Không thể xóa phiếu nhập vì số lượng xuất sẽ vượt tồn kho");
        receipts.delete(receipt);
    }

    public Page<GiftIssueView> pageIssues(ListQuery query, Long itemId) {
        return issues.findAll(Specs.nullSafe(InventorySpecs.issues(query, scopedUnitId(query), itemId)),
                        query.pageable(ISSUE_SORT))
                .map(this::issueView);
    }

    public List<GiftIssueView> searchIssues(ListQuery query, Long itemId) {
        return issues.findAll(Specs.nullSafe(InventorySpecs.issues(query, scopedUnitId(query), itemId)), ISSUE_SORT)
                .stream().map(this::issueView).toList();
    }

    public GiftIssueView getIssue(Long id) {
        InventoryGiftIssue issue = requireIssue(id);
        currentUser.requireUnitAccess(issue.getUnionUnit().getId());
        return issueView(issue);
    }

    @Transactional
    public GiftIssueView createIssue(GiftIssueRequest request) {
        InventoryItem item = lockItem(requiredId(request.itemId(), "Vật phẩm"));
        verifyTransactionUnit(item, request.unionUnitId());
        int quantity = positiveQuantity(request.quantity());
        assertCanIssue(item, quantity);
        Member member = requireMemberForItem(requiredId(request.memberId(), "Đoàn viên"), item, true);
        InventoryGiftIssue issue = new InventoryGiftIssue();
        applyIssue(issue, request, item, member, quantity, true);
        return issueView(issues.save(issue));
    }

    @Transactional
    public GiftIssueView updateIssue(Long id, GiftIssueRequest request) {
        InventoryGiftIssue issue = lockIssue(id);
        currentUser.requireUnitAccess(issue.getUnionUnit().getId());
        Map<Long, InventoryItem> lockedItems = lockItems(issue.getItem().getId(), requiredId(request.itemId(), "Vật phẩm"));
        InventoryItem oldItem = lockedItems.get(issue.getItem().getId());
        InventoryItem newItem = lockedItems.get(request.itemId());
        verifyTransactionUnit(newItem, request.unionUnitId());
        forbidUnitChange(issue.getUnionUnit(), newItem.getUnionUnit(), "phiếu cấp quà");
        int quantity = positiveQuantity(request.quantity());
        assertIssueChangeDoesNotMakeStockNegative(oldItem, issue.getQuantity(), newItem, quantity);
        boolean recipientChanged = !Objects.equals(issue.getMember().getId(), request.memberId());
        Member member = requireMemberForItem(requiredId(request.memberId(), "Đoàn viên"), newItem, recipientChanged);
        applyIssue(issue, request, newItem, member, quantity, recipientChanged);
        return issueView(issues.save(issue));
    }

    @Transactional
    public void deleteIssue(Long id) {
        InventoryGiftIssue issue = lockIssue(id);
        currentUser.requireUnitAccess(issue.getUnionUnit().getId());
        // Removing a gift issue only increases stock. Lock the item so it cannot race an issue creation.
        lockItem(issue.getItem().getId());
        issues.delete(issue);
    }

    public Page<RecipientSuggestionView> pageRecipients(ListQuery query) {
        Specification<Member> filter = Specs.nullSafe(eligibleRecipients(query));
        return members.findAll(filter, query.pageable(RECIPIENT_SORT)).map(this::recipientView);
    }

    public List<RecipientSuggestionView> searchRecipients(ListQuery query) {
        Specification<Member> filter = Specs.nullSafe(eligibleRecipients(query));
        return members.findAll(filter, RECIPIENT_SORT).stream().map(this::recipientView).toList();
    }

    private Specification<Member> eligibleRecipients(ListQuery query) {
        Specification<Member> eligible = (root, criteria, cb) -> cb.and(
                cb.equal(root.get("membershipStatus"), MembershipStatus.MEMBER),
                cb.equal(root.get("employmentStatus"), EmploymentStatus.ACTIVE));
        return Specs.allOf(MemberSpecs.filter(query, scopedUnitId(query)), eligible);
    }

    public InventorySummary summary(Long requestedUnitId) {
        Long unitId = currentUser.scopedUnitId(requestedUnitId);
        List<InventoryItem> catalogue = items.findAll(Specs.nullSafe(Specs.unitScope(unitId)), ITEM_SORT);
        Map<Long, Stock> stock = stocks(catalogue);
        long received = 0;
        long issued = 0;
        long balance = 0;
        long low = 0;
        long out = 0;
        for (InventoryItem item : catalogue) {
            Stock value = stock.getOrDefault(item.getId(), Stock.ZERO);
            received += value.received();
            issued += value.issued();
            balance += value.balance();
            if (value.balance() == 0) out++;
            else if (item.getMinimumStock() > 0 && value.balance() <= item.getMinimumStock()) low++;
        }
        UnionUnit unit = unitId == null ? null : requireUnit(unitId);
        return new InventorySummary(unitId, unit == null ? null : unit.getCode(),
                unit == null ? null : unit.getCompanyName(), catalogue.size(), received, issued, balance, low, out);
    }

    private Long scopedUnitId(ListQuery query) {
        return currentUser.scopedUnitId(query.unitId());
    }

    private UnionUnit resolveNewItemUnit(Long requestedUnitId) {
        Long unitId = currentUser.scopedUnitId(requestedUnitId);
        if (unitId == null) throw new IllegalArgumentException("Vui lòng chọn CĐCS quản lý vật phẩm");
        currentUser.requireUnitAccess(unitId);
        return requireUnit(unitId);
    }

    private UnionUnit resolveItemUpdateUnit(Long requestedUnitId, UnionUnit currentUnit) {
        if (requestedUnitId == null) return currentUnit;
        Long unitId = currentUser.scopedUnitId(requestedUnitId);
        if (unitId == null) throw new IllegalArgumentException("Vui lòng chọn CĐCS quản lý vật phẩm");
        currentUser.requireUnitAccess(unitId);
        return requireUnit(unitId);
    }

    /** Validates an optional client unit against the item's authoritative CĐCS. */
    private void verifyTransactionUnit(InventoryItem item, Long requestedUnitId) {
        Long actualUnitId = item.getUnionUnit().getId();
        currentUser.requireUnitAccess(actualUnitId);
        if (requestedUnitId != null && !Objects.equals(currentUser.scopedUnitId(requestedUnitId), actualUnitId)) {
            throw new IllegalArgumentException("Vật phẩm không thuộc CĐCS đã chọn");
        }
    }

    private void forbidUnitChange(UnionUnit existingUnit, UnionUnit targetUnit, String label) {
        if (!Objects.equals(existingUnit.getId(), targetUnit.getId())) {
            throw new IllegalArgumentException("Không thể đổi CĐCS của " + label);
        }
    }

    private void applyItem(InventoryItem item, ItemRequest request, UnionUnit unit, String itemCode) {
        item.setUnionUnit(unit);
        item.setItemCode(itemCode);
        item.setItemName(requiredText(request.itemName(), "Tên vật phẩm"));
        item.setCategory(optionalText(request.category()));
        item.setSupplier(optionalText(request.supplier()));
        item.setUnitOfMeasure(defaultText(request.unitOfMeasure(), "Cái"));
        item.setMinimumStock(request.minimumStock() == null ? 0 : request.minimumStock());
        item.setNote(optionalText(request.note()));
    }

    private void applyReceipt(InventoryReceipt receipt, ReceiptRequest request, InventoryItem item) {
        receipt.setUnionUnit(item.getUnionUnit());
        receipt.setItem(item);
        receipt.setReceiptDate(Objects.requireNonNull(request.receiptDate(), "Ngày nhập không được để trống"));
        receipt.setQuantity(positiveQuantity(request.quantity()));
        receipt.setSupplier(optionalText(request.supplier()));
        receipt.setReferenceNo(optionalText(request.referenceNo()));
        receipt.setNote(optionalText(request.note()));
    }

    private void applyIssue(InventoryGiftIssue issue, GiftIssueRequest request, InventoryItem item,
                            Member member, int quantity, boolean refreshRecipientSnapshot) {
        UnionUnit unit = item.getUnionUnit();
        issue.setUnionUnit(unit);
        issue.setItem(item);
        issue.setMember(member);
        issue.setIssueDate(Objects.requireNonNull(request.issueDate(), "Ngày cấp quà không được để trống"));
        issue.setQuantity(quantity);
        issue.setProgramName(optionalText(request.programName()));
        issue.setReferenceNo(optionalText(request.referenceNo()));
        issue.setNote(optionalText(request.note()));
        if (refreshRecipientSnapshot) {
            issue.setEmployeeCodeSnapshot(member.getEmployeeCode());
            issue.setRecipientNameSnapshot(member.getFullName());
            issue.setCompanyNameSnapshot(unit.getCompanyName());
            issue.setJobTitleSnapshot(member.getJobTitle());
            issue.setProfessionalTitleSnapshot(member.getProfessionalTitle());
            issue.setWorkplaceSnapshot(member.getWorkplace());
            issue.setEmailSnapshot(member.getEmail());
            issue.setPhoneSnapshot(member.getPhone());
            issue.setGenderSnapshot(member.getGender() == null ? null : member.getGender().name());
            issue.setPlaceOfBirthSnapshot(member.getPlaceOfBirth());
            issue.setCurrentResidenceSnapshot(member.getCurrentResidence());
            issue.setStartWorkDateSnapshot(member.getStartWorkDate());
        }
    }

    private void assertItemCodeAvailable(Long unitId, String itemCode, Long currentId) {
        items.findByUnionUnit_IdAndItemCodeIgnoreCase(unitId, itemCode)
                .filter(found -> !Objects.equals(found.getId(), currentId))
                .ifPresent(found -> {
                    throw new IllegalArgumentException("Mã vật phẩm đã tồn tại trong CĐCS này");
                });
    }

    private boolean hasSlipHistory(Long itemId) {
        return receipts.countByItem_Id(itemId) > 0 || issues.countByItem_Id(itemId) > 0;
    }

    private void assertCanIssue(InventoryItem item, int quantity) {
        ensureNonNegative(stockFor(item).balance() - quantity, item,
                "Số lượng cấp quà vượt quá tồn kho của vật phẩm");
    }

    private void assertReceiptChangeDoesNotMakeStockNegative(InventoryItem oldItem, int oldQuantity,
                                                               InventoryItem newItem, int newQuantity) {
        if (Objects.equals(oldItem.getId(), newItem.getId())) {
            ensureNonNegative(stockFor(oldItem).balance() - oldQuantity + newQuantity, newItem,
                    "Thay đổi phiếu nhập làm tồn kho âm");
            return;
        }
        ensureNonNegative(stockFor(oldItem).balance() - oldQuantity, oldItem,
                "Không thể chuyển phiếu nhập vì vật phẩm cũ sẽ có tồn kho âm");
    }

    private void assertIssueChangeDoesNotMakeStockNegative(InventoryItem oldItem, int oldQuantity,
                                                             InventoryItem newItem, int newQuantity) {
        if (Objects.equals(oldItem.getId(), newItem.getId())) {
            ensureNonNegative(stockFor(oldItem).balance() + oldQuantity - newQuantity, newItem,
                    "Thay đổi phiếu cấp quà làm tồn kho âm");
            return;
        }
        ensureNonNegative(stockFor(newItem).balance() - newQuantity, newItem,
                "Không thể chuyển phiếu cấp quà vì vật phẩm mới không đủ tồn kho");
    }

    private void ensureNonNegative(long balance, InventoryItem item, String message) {
        if (balance < 0) {
            throw new IllegalArgumentException(message + " (" + item.getItemCode() + ")");
        }
    }

    private Member requireMemberForItem(Long memberId, InventoryItem item, boolean mustBeEligible) {
        Member member = members.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đoàn viên với id=" + memberId));
        if (!Objects.equals(member.getUnionUnit().getId(), item.getUnionUnit().getId())) {
            throw new IllegalArgumentException("Đoàn viên phải thuộc cùng CĐCS với vật phẩm cấp");
        }
        if (mustBeEligible && (member.getMembershipStatus() != MembershipStatus.MEMBER
                || member.getEmploymentStatus() != EmploymentStatus.ACTIVE)) {
            throw new IllegalArgumentException("Chỉ được cấp quà cho đoàn viên đang làm việc");
        }
        currentUser.requireUnitAccess(member.getUnionUnit().getId());
        return member;
    }

    private Map<Long, InventoryItem> lockItems(Long... itemIds) {
        List<Long> ordered = Arrays.stream(itemIds)
                .filter(Objects::nonNull)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();
        Map<Long, InventoryItem> locked = new LinkedHashMap<>();
        for (Long itemId : ordered) locked.put(itemId, lockItem(itemId));
        return locked;
    }

    private InventoryItem lockItem(Long id) {
        return items.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vật phẩm với id=" + id));
    }

    private InventoryReceipt lockReceipt(Long id) {
        return receipts.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu nhập với id=" + id));
    }

    private InventoryGiftIssue lockIssue(Long id) {
        return issues.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu cấp quà với id=" + id));
    }

    private InventoryItem requireItem(Long id) {
        return items.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vật phẩm với id=" + id));
    }

    private InventoryReceipt requireReceipt(Long id) {
        return receipts.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu nhập với id=" + id));
    }

    private InventoryGiftIssue requireIssue(Long id) {
        return issues.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu cấp quà với id=" + id));
    }

    private UnionUnit requireUnit(Long id) {
        return units.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy CĐCS với id=" + id));
    }

    private Stock stockFor(InventoryItem item) {
        return stocks(List.of(item)).getOrDefault(item.getId(), Stock.ZERO);
    }

    private Map<Long, Stock> stocks(Collection<InventoryItem> catalogue) {
        if (catalogue.isEmpty()) return Map.of();
        List<Long> ids = catalogue.stream().map(InventoryItem::getId).toList();
        Map<Long, Long> received = quantities(receipts.totalQuantityByItemIds(ids));
        Map<Long, Long> issued = quantities(issues.totalQuantityByItemIds(ids));
        Map<Long, Stock> result = new HashMap<>();
        for (Long id : ids) {
            result.put(id, new Stock(received.getOrDefault(id, 0L), issued.getOrDefault(id, 0L)));
        }
        return result;
    }

    private Map<Long, Long> quantities(List<InventoryItemQuantity> projections) {
        Map<Long, Long> result = new HashMap<>();
        for (InventoryItemQuantity projection : projections) {
            if (projection.getItemId() != null) {
                result.put(projection.getItemId(), projection.getQuantity() == null ? 0L : projection.getQuantity());
            }
        }
        return result;
    }

    private ItemView itemView(InventoryItem item, Stock stock) {
        UnionUnit unit = item.getUnionUnit();
        return new ItemView(item.getId(), unit.getId(), unit.getCode(), unit.getCompanyName(), item.getItemCode(),
                item.getItemName(), item.getCategory(), item.getSupplier(), item.getUnitOfMeasure(),
                item.getMinimumStock(), stock.received(), stock.issued(), stock.balance(), item.getNote(),
                item.getCreatedAt(), item.getUpdatedAt());
    }

    private ReceiptView receiptView(InventoryReceipt receipt) {
        UnionUnit unit = receipt.getUnionUnit();
        InventoryItem item = receipt.getItem();
        return new ReceiptView(receipt.getId(), unit.getId(), unit.getCode(), unit.getCompanyName(), item.getId(),
                item.getItemCode(), item.getItemName(), receipt.getReceiptDate(), receipt.getQuantity(),
                receipt.getSupplier(), receipt.getReferenceNo(), receipt.getNote(), receipt.getCreatedAt(),
                receipt.getUpdatedAt());
    }

    private GiftIssueView issueView(InventoryGiftIssue issue) {
        UnionUnit unit = issue.getUnionUnit();
        InventoryItem item = issue.getItem();
        return new GiftIssueView(issue.getId(), unit.getId(), unit.getCode(), issue.getCompanyNameSnapshot(),
                item.getId(), item.getItemCode(), item.getItemName(), issue.getMember().getId(),
                issue.getEmployeeCodeSnapshot(), issue.getRecipientNameSnapshot(), issue.getJobTitleSnapshot(),
                issue.getProfessionalTitleSnapshot(), issue.getWorkplaceSnapshot(), issue.getEmailSnapshot(),
                issue.getPhoneSnapshot(), issue.getGenderSnapshot(), issue.getPlaceOfBirthSnapshot(),
                issue.getCurrentResidenceSnapshot(), issue.getStartWorkDateSnapshot(), issue.getIssueDate(),
                issue.getQuantity(), issue.getProgramName(), issue.getReferenceNo(), issue.getNote(),
                issue.getCreatedAt(), issue.getUpdatedAt());
    }

    private RecipientSuggestionView recipientView(Member member) {
        UnionUnit unit = member.getUnionUnit();
        return new RecipientSuggestionView(member.getId(), member.getEmployeeCode(), member.getFullName(), unit.getId(),
                unit.getCode(), unit.getCompanyName(), member.getJobTitle(), member.getProfessionalTitle(),
                member.getWorkplace(), member.getEmail(), member.getPhone(),
                member.getGender() == null ? null : member.getGender().name(), member.getPlaceOfBirth(),
                member.getCurrentResidence(), member.getStartWorkDate());
    }

    private static Long requiredId(Long value, String field) {
        if (value == null) throw new IllegalArgumentException(field + " không được để trống");
        return value;
    }

    private static int positiveQuantity(Integer value) {
        if (value == null || value < 1) throw new IllegalArgumentException("Số lượng phải lớn hơn 0");
        return value;
    }

    private static String requiredText(String value, String field) {
        String normalized = optionalText(value);
        if (normalized == null) throw new IllegalArgumentException(field + " không được để trống");
        return normalized;
    }

    private static String defaultText(String value, String fallback) {
        String normalized = optionalText(value);
        return normalized == null ? fallback : normalized;
    }

    private static String optionalText(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private record Stock(long received, long issued) {
        private static final Stock ZERO = new Stock(0, 0);

        private long balance() {
            return received - issued;
        }
    }
}
