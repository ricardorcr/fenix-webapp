package pt.ist.fenix.webapp.task.academic;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.candidacy.degree.ShiftDistributionEntry;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.admissions.domain.AdmissionProcessTarget;
import org.fenixedu.admissions.domain.AdmissionsSystem;
import org.fenixedu.admissions.domain.Application;
import org.fenixedu.admissions.ist.domain.Utils;
import org.fenixedu.bennu.core.json.JsonUtils;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import pt.ist.fenixframework.DomainObject;
import pt.ist.fenixframework.FenixFramework;

import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CheckShiftDistribution extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        final ExecutionYear executionYear = ExecutionYear.readCurrentExecutionYear();
        final Map<ExecutionDegree, Set<Integer>> map = executionYear.getShiftDistribution().getShiftDistributionEntriesSet().stream()
                .collect(Collectors.toMap(e -> e.getExecutionDegree(), e -> toSet(e), (s1, s2) -> merge(s1, s2)));
        final Map<ExecutionDegree, Set<Integer>> mapD = executionYear.getShiftDistribution().getShiftDistributionEntriesSet().stream()
                .filter(shiftDistributionEntry -> !shiftDistributionEntry.alreadyDistributed())
                .collect(Collectors.toMap(e -> e.getExecutionDegree(), e -> toSet(e), (s1, s2) -> merge(s1, s2)));

        final Spreadsheet spreadsheet = new Spreadsheet("DistributionCheck");
        map.forEach((ed, s) -> {
            final long countCNAES = cnaesTargetStream(executionYear, ed.getDegree())
                    .mapToLong(target -> target.getSlots())
                    .sum();
            final long countOthers = applicationStream(executionYear, ed.getDegree())
                    .filter(application -> application.getAdmitted())
                    .count();
            final long registered = applicationStream(executionYear, ed.getDegree())
                    .filter(application -> application.getAdmitted())
                    .filter(application -> Utils.registrationFor(application) != null)
                    .count();

            final int consumed = mapD.containsKey(ed) ? mapD.get(ed).size() : 0;

            final Spreadsheet.Row row = spreadsheet.addRow();
            row.setCell("Degree", ed.getDegree().getSigla());
            row.setCell("ShiftDistributionCount", s.size());
            row.setCell("ExpectedShiftDistributionCount", Long.toString(countCNAES + countOthers));
            row.setCell("Vagas CNAES", Long.toString(countCNAES));
            row.setCell("Colocados Outros Concursos", Long.toString(countOthers));
            row.setCell("Já Matriculados", Long.toString(registered));
            row.setCell("ShiftDistributionCount Já consumidos", consumed);
        });

        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(stream);
        output("distribution_check.xlsx", stream.toByteArray());
    }

    private Set<Integer> merge(final Set<Integer> s1, final Set<Integer> s2) {
        s1.addAll(s2);
        return s1;
    }

    private Set<Integer> toSet(final ShiftDistributionEntry e) {
        final Set<Integer> set = new HashSet<>();
        set.add(e.getAbstractStudentNumber());
        return set;
    }

    private Stream<Application> applicationStream(final ExecutionYear executionYear, final Degree degree) {
        return AdmissionsSystem.getInstance().getAdmissionProcessSet().stream()
                .filter(admissionProcess -> Utils.isDegreeType(admissionProcess))
                .filter(admissionProcess -> hasAutoEnrolment(admissionProcess))
                .flatMap(admissionProcess -> admissionProcess.getAdmissionProcessTargetSet().stream())
                .filter(target -> executionYear == get(target, "year"))
                .filter(target -> degree == get(target, "degree"))
                .filter(target -> CycleType.FIRST_CYCLE == get(target))
                .flatMap(target -> target.getApplicationSet().stream());
    }

    private boolean hasAutoEnrolment(final AdmissionProcess admissionProcess) {
        final JsonObject config = admissionProcess.getOutcomeConfigJson();
        final JsonElement automaticEnrollment = config.get("automaticEnrollment");
        return automaticEnrollment != null && automaticEnrollment.getAsBoolean();
    }

    private Stream<AdmissionProcessTarget> targetStream(final ExecutionYear executionYear, final Degree degree) {
        return AdmissionsSystem.getInstance().getAdmissionProcessSet().stream()
                .filter(admissionProcess -> Utils.isDegreeType(admissionProcess))
                .flatMap(admissionProcess -> admissionProcess.getAdmissionProcessTargetSet().stream())
                .filter(target -> executionYear == get(target, "year"))
                .filter(target -> degree == get(target, "degree"));
    }

    private Stream<AdmissionProcessTarget> cnaesTargetStream(final ExecutionYear executionYear, final Degree degree) {
        return AdmissionsSystem.getInstance().getAdmissionProcessSet().stream()
                .filter(admissionProcess -> Utils.isDges(admissionProcess))
                .flatMap(admissionProcess -> admissionProcess.getAdmissionProcessTargetSet().stream())
                .filter(target -> executionYear == get(target, "year"))
                .filter(target -> degree == get(target, "degree"));
    }

    private <T extends DomainObject> T get(final AdmissionProcessTarget target, final String key) {
        final JsonObject outcome = target.getOutcomeConfigJson();
        final String id = JsonUtils.get(outcome, key);
        return id == null ? null : FenixFramework.getDomainObject(id);
    }

    private CycleType get(final AdmissionProcessTarget target) {
        final JsonObject outcome = target.getOutcomeConfigJson();
        final String id = JsonUtils.get(outcome, "cycleType");
        return id == null ? null : CycleType.valueOf(id);
    }

}