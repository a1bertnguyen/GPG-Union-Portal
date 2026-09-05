package vn.gpg.unionportal.service.kpi;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gpg.unionportal.service.CurrentUserService;
import vn.gpg.unionportal.dto.KpiModels.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Predicate;

@Service
@Transactional(readOnly=true)
public class KpiActivityService {
    private final JdbcTemplate jdbc;
    private final CurrentUserService user;
    private final KpiPopulationService populations;
    public KpiActivityService(JdbcTemplate jdbc, CurrentUserService user, KpiPopulationService populations) {
        this.jdbc=jdbc;this.user=user;this.populations=populations;
    }
    public record Source(String module, String id, Map<String,String> fields) {}
    public record Statistic(String groupCode,String code,String label,String dimensionType,String dimensionKey,
                            BigDecimal numerator,BigDecimal denominator,String measure,List<String> numeratorIds,List<String> denominatorIds,
                            List<String> excludedIds,String status) {}
    public record ActivityView(Long populationSnapshotId,Long activeEmployeeCount,Long activeUnionMemberCount,
                               List<Statistic> statistics,List<Source> sources,List<String> blockers) {}
    private record Definition(String table,String module,String key,String date,String columns) {}
    private static final List<Definition> SOURCES=List.of(
        new Definition("union_units","DM_CONG_DOAN","code",null,"id,code,name,decision_number,term_start,term_end,legal_status"),
        new Definition("member_changes","BIEN_DONG_DOAN_VIEN","id","effective_date","id,member_id,effective_date,change_type,created_at"),
        new Definition("monthly_reports","BAO_CAO_DINH_KY","id","report_month","id,report_month,status,submitted_at"),
        new Definition("welfare_records","CHAM_SOC_NLD","record_code","event_date","id,record_code,welfare_type,member_id,event_date,deadline,status,amount,standard_amount,document_status,receipt_status,completed_at,cancellation_reason"),
        new Definition("labor_cases","SO_KIEN_NGHI","case_code","received_date","id,case_code,received_date,deadline,status,issue_group,severity,approved_at,response_date,affected_people"),
        new Definition("union_activities","HOAT_DONG","activity_code","event_date","id,activity_code,event_date,status,planned_budget,actual_cost,invited_count,participant_count,check_in_count,workers_reached,usefulness_score,report_completed,document_status,cancellation_reason"),
        new Definition("finance_entries","TAI_CHINH_CD","entry_code","transaction_date","id,entry_code,transaction_date,entry_type,category,amount,document_status")
    );
    public ActivityView read(long unitId,Period period,List<Detail> details){
        user.requireUnitAccess(unitId);
        var population=populations.approved(unitId,period.year());
        LocalDate asOf=LocalDate.now(GpgKpiEngine.BUSINESS_ZONE);
        if(asOf.isAfter(period.periodEnd())) asOf=period.periodEnd();
        List<Source> sources=new ArrayList<>();
        Set<String> demo=new HashSet<>(jdbc.query("SELECT source_module,source_record_key FROM kpi_source_exclusions WHERE active=TRUE",
            (rs,n)->rs.getString(1)+":"+rs.getString(2)));
        for(Definition d:SOURCES){
            String sql="SELECT "+d.columns()+" FROM "+d.table()+" WHERE "
                +(d.table().equals("union_units")?"id=?":d.table().equals("member_changes")?"member_id IN (SELECT id FROM members WHERE union_unit_id=?)":"union_unit_id=?");
            List<Object> args=new ArrayList<>();args.add(unitId);
            if(d.date()!=null){
                sql+=" AND "+d.date()+"<=?";args.add(java.sql.Date.valueOf(asOf));
                if(!d.table().equals("labor_cases")){sql+=" AND "+d.date()+">=?";args.add(java.sql.Date.valueOf(period.periodStart()));}
            }
            sql+=" ORDER BY id";
            for(var row:jdbc.queryForList(sql,args.toArray())){
                Map<String,String> fields=new TreeMap<>();
                row.forEach((k,v)->fields.put(k.toLowerCase(Locale.ROOT),v==null?null:v.toString()));
                if(!demo.contains(d.module()+":"+fields.get(d.key()))) sources.add(new Source(d.module(),fields.get("id"),fields));
            }
        }
        List<String> blockers=new ArrayList<>();
        if(population==null) blockers.add("Chưa có danh sách nhân sự cuối năm được phê duyệt");
        List<Statistic> stats=new ArrayList<>();
        for(Detail d:details) stats.add(new Statistic(d.groupCode(),d.kpiCode(),d.name(),"KPI",d.kpiCode(),d.numerator(),d.denominator(),"ratio",
            d.evidence().stream().filter(e->e.role()==EvidenceRole.NUMERATOR).map(e->e.sourceModule()+":"+e.sourceRecordId()).toList(),
            d.evidence().stream().filter(e->e.role()==EvidenceRole.DENOMINATOR).map(e->e.sourceModule()+":"+e.sourceRecordId()).toList(),List.of(),d.resultStatus().name()));
        List<Source> care=module(sources,"CHAM_SOC_NLD"), acts=module(sources,"HOAT_DONG");
        for(Source s:sources) if(cancelled(s)&&blank(s.fields().get("cancellation_reason")))
            blockers.add(s.module()+":"+s.id()+" hủy nhưng thiếu lý do");
        Set<String> roster=population==null?Set.of():population.members().stream().map(p->String.valueOf(p.memberId())).collect(java.util.stream.Collectors.toSet());
        for(Source s:care) if("BIRTHDAY".equals(s.fields().get("welfare_type"))&&!cancelled(s)
            && (blank(s.fields().get("member_id")) || population!=null&&!roster.contains(s.fields().get("member_id"))))
                blockers.add("Sinh nhật "+s.id()+" chưa liên kết đúng nhân sự trong danh sách cuối năm");
        List<Source> cases=module(sources,"SO_KIEN_NGHI");
        List<Source> finances=module(sources,"TAI_CHINH_CD");
        for(int month=0;month<=12;month++){
            String dimension=month==0?"YEAR":"MONTH", key=month==0?String.valueOf(period.year()):String.format("%d-%02d",period.year(),month);
            final int m=month;
            List<Source> c=care.stream().filter(s->inMonth(s,"event_date",m)).toList();
            for(String type:List.of("BIRTHDAY","FUNERAL","WEDDING","VISIT","CHILDBIRTH","HARDSHIP")){
                var rows=c.stream().filter(s->type.equals(s.fields().get("welfare_type"))).toList();
                if(type.equals("BIRTHDAY")){
                    List<String> done=rows.stream().filter(s->completed(s)&&roster.contains(s.fields().get("member_id")))
                        .map(s->s.fields().get("member_id")).distinct().sorted().toList();
                    stats.add(new Statistic("CARE","CARE_"+type,"Sinh nhật: số người đã chăm lo / nhân sự cuối năm",dimension,key,
                        BigDecimal.valueOf(done.size()),population==null?null:BigDecimal.valueOf(roster.size()),"people",
                        done.stream().map(id->"POPULATION:"+id).toList(),roster.stream().sorted().map(id->"POPULATION:"+id).toList(),
                        rows.stream().filter(KpiActivityService::cancelled).map(KpiActivityService::ref).toList(),population==null?"MISSING_DATA":"CALCULATED"));
                }else stats.add(ratio("CARE","CARE_"+type,type,dimension,key,rows,KpiActivityService::completed));
            }
            var cohort=cases.stream().filter(s->inPeriod(s,"received_date",period)&&inMonth(s,"received_date",m)).toList();
            stats.add(ratio("GRV","RESOLVED","Đã giải quyết / phát sinh cùng kỳ",dimension,key,cohort,s->closedBy(s,period.periodEnd())));
            var closed=cases.stream().filter(s->closedBy(s,period.periodEnd())&&inPeriod(s,"approved_at",period)&&inMonth(s,"approved_at",m)).toList();
            stats.add(count("GRV","CLOSED","Tổng đóng trong kỳ (gồm tồn đầu kỳ)",dimension,key,closed));
            var a=acts.stream().filter(s->inMonth(s,"event_date",m)).toList();
            stats.add(ratio("ACT","COMPLETED","Hoạt động hoàn thành / kế hoạch",dimension,key,a,KpiActivityService::completed));
            stats.add(sumRatio("ACT","PARTICIPATION","Người tham gia / số mời",dimension,key,a,"participant_count","invited_count"));
            var f=finances.stream().filter(s->inMonth(s,"transaction_date",m)).toList();
            for(String type:List.of("INCOME","EXPENSE","ADVANCE")){
                var rows=f.stream().filter(s->type.equals(s.fields().get("entry_type"))).toList();
                stats.add(new Statistic("FIN",type,type,dimension,key,sum(rows,"amount"),null,"VND",rows.stream().map(KpiActivityService::ref).toList(),List.of(),List.of(),"CALCULATED"));
                stats.add(count("FIN",type+"_COUNT","Số giao dịch "+type,dimension,key,rows));
            }
            stats.add(ratio("FIN","DOCUMENTS","Giao dịch đủ chứng từ khai báo",dimension,key,f,s->"COMPLETE".equals(s.fields().get("document_status"))));
            stats.add(count("DATA","CHANGES","Biến động nhân sự",dimension,key,module(sources,"BIEN_DONG_DOAN_VIEN").stream().filter(s->inMonth(s,"effective_date",m)).toList()));
            stats.add(count("REP","REPORTS","Báo cáo đã nộp/duyệt",dimension,key,module(sources,"BAO_CAO_DINH_KY").stream()
                .filter(s->inMonth(s,"report_month",m)&&Set.of("SUBMITTED","APPROVED").contains(s.fields().get("status"))).toList()));
        }
        for(String group:cases.stream().map(s->s.fields().get("issue_group")).filter(Objects::nonNull).distinct().sorted().toList())
            stats.add(ratio("GRV","ISSUE_GROUP",group,"ISSUE_GROUP",group,cases.stream().filter(s->group.equals(s.fields().get("issue_group"))&&inPeriod(s,"received_date",period)).toList(),s->closedBy(s,period.periodEnd())));
        return new ActivityView(population==null?null:population.id(),population==null?null:(long)population.members().size(),
            population==null?null:population.members().stream().filter(KpiPopulationService.Person::unionMember).count(),List.copyOf(stats),List.copyOf(sources),List.copyOf(blockers));
    }
    private static List<Source> module(List<Source> rows,String module){return rows.stream().filter(s->s.module().equals(module)).toList();}
    private static boolean blank(String s){return s==null||s.isBlank();}
    private static boolean cancelled(Source s){return "CANCELLED".equals(s.fields().get("status"));}
    private static boolean completed(Source s){return "COMPLETED".equals(s.fields().get("status"));}
    private static String ref(Source s){return s.module()+":"+s.id();}
    private static boolean inMonth(Source s,String field,int month){String d=s.fields().get(field);return month==0||d!=null&&d.length()>=7&&Integer.parseInt(d.substring(5,7))==month;}
    private static boolean inPeriod(Source s,String field,Period p){String d=s.fields().get(field);if(d==null||d.length()<10)return false;LocalDate date=LocalDate.parse(d.substring(0,10));return !date.isBefore(p.periodStart())&&!date.isAfter(p.periodEnd());}
    private static boolean closedBy(Source s,LocalDate end){String d=s.fields().get("approved_at");return "CLOSED".equals(s.fields().get("status"))&&d!=null&&!LocalDate.parse(d.substring(0,10)).isAfter(end);}
    private static Statistic count(String group,String code,String label,String dimension,String key,List<Source> rows){
        return new Statistic(group,code,label,dimension,key,BigDecimal.valueOf(rows.size()),null,"count",rows.stream().map(KpiActivityService::ref).toList(),List.of(),List.of(),"CALCULATED");
    }
    private static Statistic ratio(String group,String code,String label,String dimension,String key,List<Source> rows,Predicate<Source> done){
        var eligible=rows.stream().filter(s->!cancelled(s)||blank(s.fields().get("cancellation_reason"))).toList();
        var numerator=eligible.stream().filter(done).toList();
        return new Statistic(group,code,label,dimension,key,BigDecimal.valueOf(numerator.size()),BigDecimal.valueOf(eligible.size()),"ratio",
            numerator.stream().map(KpiActivityService::ref).toList(),eligible.stream().map(KpiActivityService::ref).toList(),
            rows.stream().filter(s->cancelled(s)&&!blank(s.fields().get("cancellation_reason"))).map(KpiActivityService::ref).toList(),
            rows.stream().anyMatch(s->cancelled(s)&&blank(s.fields().get("cancellation_reason")))?"FAILED_VALIDATION":eligible.isEmpty()?"NO_OCCURRENCE":"CALCULATED");
    }
    private static BigDecimal sum(List<Source> rows,String field){return rows.stream().map(s->s.fields().get(field)).filter(Objects::nonNull).map(BigDecimal::new).reduce(BigDecimal.ZERO,BigDecimal::add);}
    private static Statistic sumRatio(String group,String code,String label,String dimension,String key,List<Source> rows,String n,String d){
        if(rows.stream().anyMatch(s->cancelled(s)&&blank(s.fields().get("cancellation_reason"))))
            return new Statistic(group,code,label,dimension,key,null,null,"ratio",List.of(),List.of(),
                rows.stream().filter(KpiActivityService::cancelled).map(KpiActivityService::ref).toList(),"FAILED_VALIDATION");
        var eligible=rows.stream().filter(s->!cancelled(s)).toList();
        return new Statistic(group,code,label,dimension,key,sum(eligible,n),sum(eligible,d),"ratio",eligible.stream().map(KpiActivityService::ref).toList(),eligible.stream().map(KpiActivityService::ref).toList(),List.of(),"CALCULATED");
    }
}
