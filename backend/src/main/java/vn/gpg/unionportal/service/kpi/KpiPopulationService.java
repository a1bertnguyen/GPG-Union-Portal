package vn.gpg.unionportal.service.kpi;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gpg.unionportal.model.Member;
import vn.gpg.unionportal.model.DomainEnums.*;
import vn.gpg.unionportal.repository.MemberRepository;
import vn.gpg.unionportal.service.CurrentUserService;
import vn.gpg.unionportal.exception.ResourceNotFoundException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class KpiPopulationService {
    private final JdbcTemplate jdbc;
    private final MemberRepository members;
    private final CurrentUserService user;
    public KpiPopulationService(JdbcTemplate jdbc, MemberRepository members, CurrentUserService user) {
        this.jdbc = jdbc; this.members = members; this.user = user;
    }
    public record Person(long memberId, String employeeCode, String fullName, boolean unionMember,
                         boolean profileComplete, boolean identityDeclared, boolean identityUnique) {}
    public record Population(long id, long unitId, int year, int revision, String status,
                             String reconciliationNote, String preparedBy, String approvedBy, List<Person> members) {}
    public record PrepareRequest(Long unitId, int year, List<Long> memberIds, String reconciliationNote) {}

    public List<Population> list(int year, Long requestedUnit) {
        GpgKpiEngine.resolvePeriod(vn.gpg.unionportal.dto.KpiModels.PeriodType.YEAR, year, 1);
        Long unit = user.scopedUnitId(requestedUnit);
        return jdbc.query("SELECT id FROM kpi_population_snapshots WHERE population_year=?"
                + (unit == null ? "" : " AND union_unit_id=?") + " ORDER BY union_unit_id, revision DESC",
                (rs,n) -> rs.getLong(1), unit == null ? new Object[]{year} : new Object[]{year,unit})
                .stream().map(this::get).toList();
    }
    public Population get(long id) {
        var rows = jdbc.query("SELECT * FROM kpi_population_snapshots WHERE id=?", (rs,n) ->
            new Population(id,rs.getLong("union_unit_id"),rs.getInt("population_year"),rs.getInt("revision"),
                rs.getString("status"),rs.getString("reconciliation_note"),rs.getString("prepared_by"),
                rs.getString("approved_by"),List.of()), id);
        if (rows.isEmpty()) throw new ResourceNotFoundException("Không tìm thấy danh sách nhân sự");
        Population p = rows.getFirst(); user.requireUnitAccess(p.unitId());
        return new Population(p.id(),p.unitId(),p.year(),p.revision(),p.status(),p.reconciliationNote(),
            p.preparedBy(),p.approvedBy(),jdbc.query("SELECT * FROM kpi_population_members WHERE snapshot_id=? ORDER BY employee_code",
                (rs,n)->new Person(rs.getLong("member_id"),rs.getString("employee_code"),rs.getString("full_name"),
                    rs.getBoolean("union_member"),rs.getBoolean("profile_complete"),rs.getBoolean("identity_declared"),
                    rs.getBoolean("identity_unique")),id));
    }
    public Population approved(long unitId, int year) {
        var ids = jdbc.query("SELECT id FROM kpi_population_snapshots WHERE union_unit_id=? AND population_year=? AND status='APPROVED' ORDER BY revision DESC",
            (rs,n)->rs.getLong(1),unitId,year);
        return ids.isEmpty() ? null : get(ids.getFirst());
    }
    @Transactional
    public Population prepare(PrepareRequest request) {
        Long unit = user.scopedUnitId(request.unitId());
        if (unit == null) throw new IllegalArgumentException("Chọn CĐCS để lập danh sách");
        user.requireUnitAccess(unit);
        GpgKpiEngine.resolvePeriod(vn.gpg.unionportal.dto.KpiModels.PeriodType.YEAR,request.year(),1);
        if (request.year() > LocalDate.now(GpgKpiEngine.BUSINESS_ZONE).getYear())
            throw new IllegalArgumentException("Không lập danh sách cho năm chưa bắt đầu");
        if (!present(request.reconciliationNote()) || request.reconciliationNote().length()>2000)
            throw new IllegalArgumentException("Cần ghi nguồn và nội dung đối soát (tối đa 2000 ký tự)");
        // Serialize revision allocation, including simultaneous first revisions.
        jdbc.queryForObject("SELECT id FROM union_units WHERE id=? FOR UPDATE", Long.class, unit);
        List<Member> selected = request.memberIds()==null
            ? members.findAll().stream().filter(m->m.getUnionUnit().getId().equals(unit)
                && m.getEmploymentStatus()==EmploymentStatus.ACTIVE
                && (m.getStartWorkDate()==null || !m.getStartWorkDate().isAfter(LocalDate.of(request.year(),12,31)))).toList()
            : members.findAllById(new HashSet<>(request.memberIds()));
        if (request.memberIds()!=null && selected.size()!=new HashSet<>(request.memberIds()).size())
            throw new IllegalArgumentException("Danh sách chứa nhân sự không tồn tại");
        for (Member m : selected) {
            if (!m.getUnionUnit().getId().equals(unit))
                throw new AccessDeniedException("Nhân sự không thuộc CĐCS");
            if (m.getEmploymentStatus() != EmploymentStatus.ACTIVE
                    || m.getStartWorkDate() != null && m.getStartWorkDate().isAfter(LocalDate.of(request.year(),12,31)))
                throw new IllegalArgumentException("Danh sách cuối năm chỉ nhận nhân sự đang làm việc đến ngày 31/12");
        }
        Set<String> excluded = new HashSet<>(jdbc.query("SELECT source_record_key FROM kpi_source_exclusions WHERE active=TRUE AND source_module='DOAN_VIEN'",
            (rs,n)->rs.getString(1)));
        selected = selected.stream().filter(m->!excluded.contains(m.getEmployeeCode())).toList();
        int revision = jdbc.queryForObject("SELECT COALESCE(MAX(revision),0)+1 FROM kpi_population_snapshots WHERE union_unit_id=? AND population_year=?",Integer.class,unit,request.year());
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbc.update(c->{var ps=c.prepareStatement("INSERT INTO kpi_population_snapshots(union_unit_id,population_year,revision,status,reconciliation_note,prepared_by) VALUES (?,?,?,'DRAFT',?,?)",new String[]{"id"});
            ps.setLong(1,unit);ps.setInt(2,request.year());ps.setInt(3,revision);ps.setString(4,request.reconciliationNote());ps.setString(5,user.username());return ps;},key);
        long id=Objects.requireNonNull(key.getKey()).longValue();
        Map<String,Long> identities=counts(selected,true), phones=counts(selected,false);
        for(Member m:selected) jdbc.update("INSERT INTO kpi_population_members(snapshot_id,member_id,employee_code,full_name,union_member,profile_complete,identity_declared,identity_unique) VALUES (?,?,?,?,?,?,?,?)",
            id,m.getId(),m.getEmployeeCode(),m.getFullName(),m.getMembershipStatus()==MembershipStatus.MEMBER,
            vn.gpg.unionportal.spec.MemberSpecs.hasRequiredProfileFields(m),
            present(m.getNationalId())||present(m.getPhone()),unique(identities,m.getNationalId())&&unique(phones,m.getPhone()));
        return get(id);
    }
    @Transactional
    public Population submit(long id) {
        Population p=get(id);
        int changed=jdbc.update("UPDATE kpi_population_snapshots SET status='SUBMITTED',submitted_at=CURRENT_TIMESTAMP WHERE id=? AND status='DRAFT'",id);
        if(changed!=1) throw new IllegalArgumentException("Chỉ gửi duyệt bản nháp");
        return get(p.id());
    }
    @Transactional
    public Population approve(long id) {
        if(!user.isAdmin()) throw new AccessDeniedException("Chỉ ADMIN được duyệt danh sách");
        Population p=get(id);
        if(LocalDate.of(p.year(),12,31).isAfter(LocalDate.now(GpgKpiEngine.BUSINESS_ZONE)))
            throw new IllegalArgumentException("Chỉ duyệt danh sách cuối năm sau khi năm kết thúc");
        int changed=jdbc.update("UPDATE kpi_population_snapshots SET status='APPROVED',approved_by=?,approved_at=CURRENT_TIMESTAMP WHERE id=? AND status='SUBMITTED'",user.username(),id);
        if(changed!=1) throw new IllegalArgumentException("Danh sách chưa được gửi duyệt");
        return get(id);
    }
    private static boolean present(String s){return s!=null&&!s.isBlank();}
    private static String norm(String s){return s==null?"":s.trim().toLowerCase(Locale.ROOT);}
    private static boolean unique(Map<String,Long> c,String v){return !present(v)||c.getOrDefault(norm(v),0L)<=1;}
    private static Map<String,Long> counts(List<Member> rows,boolean national){
        Map<String,Long> result=new HashMap<>();
        for(Member m:rows){String v=national?m.getNationalId():m.getPhone();if(present(v))result.merge(norm(v),1L,Long::sum);}return result;
    }
}
