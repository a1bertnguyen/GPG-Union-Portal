package vn.gpg.unionportal.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gpg.unionportal.dto.ApiModels.ListFacets;
import vn.gpg.unionportal.dto.ListQuery;
import vn.gpg.unionportal.exception.ResourceNotFoundException;
import vn.gpg.unionportal.model.AdminUser;
import vn.gpg.unionportal.repository.AdminUserRepository;
import vn.gpg.unionportal.repository.UnionUnitRepository;
import vn.gpg.unionportal.dto.UserAccountModels.UserAccountRequest;
import vn.gpg.unionportal.dto.UserAccountModels.UserAccountView;
import vn.gpg.unionportal.spec.AdminUserSpecs;
import vn.gpg.unionportal.spec.Specs;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class UserAccountService {
    private static final Sort SORT = Sort.by("role", "username");

    private final AdminUserRepository repository;
    private final UnionUnitRepository unitRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUser;
    private final RealtimeEventPublisher events;

    public UserAccountService(AdminUserRepository repository,
                              UnionUnitRepository unitRepository,
                              PasswordEncoder passwordEncoder,
                              CurrentUserService currentUser,
                              RealtimeEventPublisher events) {
        this.repository = repository;
        this.unitRepository = unitRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentUser = currentUser;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public List<UserAccountView> list() {
        return search(ListQuery.firstPage());
    }

    @Transactional(readOnly = true)
    public Page<AdminUser> page(ListQuery query) {
        return repository.findAll(Specs.nullSafe(AdminUserSpecs.filter(query)), query.pageable(SORT));
    }

    @Transactional(readOnly = true)
    public List<UserAccountView> search(ListQuery query) {
        return repository.findAll(Specs.nullSafe(AdminUserSpecs.filter(query)), SORT).stream()
                .map(this::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public ListFacets facets(ListQuery query) {
        Specification<AdminUser> filtered = Specs.nullSafe(AdminUserSpecs.filter(query));
        Map<String, Number> metrics = new LinkedHashMap<>();
        metrics.put("total", repository.count(filtered));
        metrics.put("activeAdmins", repository.countByRoleAndActiveTrue("ADMIN"));
        metrics.put("activeUsers", repository.countByRoleAndActiveTrue("USER"));
        return new ListFacets(repository.count(), List.of("ACTIVE", "INACTIVE"), metrics);
    }

    /** Exposed so the controller can map a page of entities without duplicating the view mapping. */
    public UserAccountView view(AdminUser account) {
        return toView(account);
    }

    @Transactional
    public UserAccountView create(UserAccountRequest request) {
        String username = normalizeUsername(request.username());
        if (repository.existsByUsernameIgnoreCase(username)) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new IllegalArgumentException("Mật khẩu khởi tạo không được để trống");
        }

        var account = new AdminUser();
        account.setUsername(username);
        account.setPasswordHash(passwordEncoder.encode(request.password()));
        apply(account, request);
        var saved = repository.save(account);
        events.changed("users", "CREATED", saved.getId(), unitId(saved));
        return toView(saved);
    }

    @Transactional
    public UserAccountView update(Long id, UserAccountRequest request) {
        var account = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản với id=" + id));
        String username = normalizeUsername(request.username());
        repository.findByUsernameIgnoreCase(username)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> { throw new IllegalArgumentException("Tên đăng nhập đã tồn tại"); });

        boolean editingSelf = account.getUsername().equalsIgnoreCase(currentUser.username());
        if (editingSelf && (!request.active() || !"ADMIN".equals(request.role()))) {
            throw new IllegalArgumentException("Không thể khóa hoặc hạ quyền tài khoản ADMIN đang đăng nhập");
        }
        if ("ADMIN".equals(account.getRole()) && account.getActive()
                && (!request.active() || !"ADMIN".equals(request.role()))
                && repository.countByRoleAndActiveTrue("ADMIN") <= 1) {
            throw new IllegalArgumentException("Hệ thống phải còn ít nhất một ADMIN đang hoạt động");
        }

        account.setUsername(username);
        if (request.password() != null && !request.password().isBlank()) {
            account.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        apply(account, request);
        var saved = repository.save(account);
        events.changed("users", "UPDATED", saved.getId(), unitId(saved));
        return toView(saved);
    }

    @Transactional
    public void delete(Long id) {
        var account = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản với id=" + id));
        if (account.getUsername().equalsIgnoreCase(currentUser.username())) {
            throw new IllegalArgumentException("Không thể xóa tài khoản đang đăng nhập");
        }
        if ("ADMIN".equals(account.getRole()) && account.getActive()
                && repository.countByRoleAndActiveTrue("ADMIN") <= 1) {
            throw new IllegalArgumentException("Hệ thống phải còn ít nhất một ADMIN đang hoạt động");
        }
        Long accountId = account.getId();
        Long accountUnitId = unitId(account);
        repository.delete(account);
        events.changed("users", "DELETED", accountId, accountUnitId);
    }

    private void apply(AdminUser account, UserAccountRequest request) {
        account.setFullName(request.fullName().trim());
        account.setRole(request.role());
        account.setActive(request.active());
        if ("USER".equals(request.role())) {
            if (request.unionUnitId() == null) throw new IllegalArgumentException("Tài khoản USER phải được gán CĐCS");
            account.setUnionUnit(unitRepository.findById(request.unionUnitId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy CĐCS với id=" + request.unionUnitId())));
        } else {
            account.setUnionUnit(null);
        }
    }

    private UserAccountView toView(AdminUser account) {
        var unit = account.getUnionUnit();
        return new UserAccountView(account.getId(), account.getUsername(), account.getFullName(), account.getRole(),
                account.getActive(), unit == null ? null : unit.getId(), unit == null ? null : unit.getCode(),
                unit == null ? null : unit.getName(), account.getLastLoginAt(), account.getCreatedAt());
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private Long unitId(AdminUser account) {
        return account.getUnionUnit() == null ? null : account.getUnionUnit().getId();
    }
}
