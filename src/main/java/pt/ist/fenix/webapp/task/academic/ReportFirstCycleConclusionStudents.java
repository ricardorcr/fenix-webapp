package pt.ist.fenix.webapp.task.academic;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.studentCurriculum.CycleCurriculumGroup;
import org.fenixedu.academic.domain.studentCurriculum.Dismissal;
import org.fenixedu.bennu.core.json.JsonUtils;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ReportFirstCycleConclusionStudents extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        Spreadsheet spreadsheet = new Spreadsheet("Alunos");
        final ExecutionYear executionYear = ExecutionYear.readCurrentExecutionYear();
        Degree.readBolonhaDegrees().stream()
                .flatMap(degree -> degree.getActiveDegreeCurricularPlans().stream())
                .filter(dcp -> dcp.getExecutionDegreeByAcademicInterval(executionYear.getAcademicInterval()) != null)
                .flatMap(dcp -> dcp.getLastStudentCurricularPlan().stream())
                .filter(StudentCurricularPlan::isFirstCycle)
                .filter(scp -> scp.getRegistration().isActive() || scp.getRegistration().isConcluded() || scp.getRegistration().isInMobilityState())
                .filter(scp -> {
                    final CycleCurriculumGroup firstCycle = scp.getFirstCycle();
                    return firstCycle != null && firstCycle.getCreditsConcluded() >= 138 && !firstCycle.isConclusionProcessed();
                })
                .forEach(scp -> report(spreadsheet, scp));
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(baos);
        output("finalistas.xlsx", baos.toByteArray());
    }

    private void report(final Spreadsheet spreadsheet, final StudentCurricularPlan scp) {
        final Registration registration = scp.getRegistration();
        final CycleCurriculumGroup firstCycle = scp.getFirstCycle();
        final Spreadsheet.Row row = spreadsheet.addRow();
        row.setCell("Username", registration.getPerson().getUsername());
        row.setCell("Curso", registration.getDegreeCurricularPlanName());
        final Double curricularCredits = firstCycle.getEnrolments().stream()
                .filter(Enrolment::isApproved)
                .map(Enrolment::getEctsCredits)
                .reduce((double) 0, Double::sum);
        row.setCell("Transitado", registration.getSourceRegistration() != null ? "Sim" : "Não");
        row.setCell("Acesso Directo", hasDirectAccess(registration.getPerson().getUsername()) ? "Sim" : "Não");
        final boolean open2ndCycle = scp.getSecondCycle() != null && !scp.getSecondCycle().getAllCurriculumLines().isEmpty();
        row.setCell("2º ciclo aberto", String.valueOf(open2ndCycle));
        row.setCell("Créditos Curriculares", curricularCredits);
        List<Dismissal> dismissals = new ArrayList<>();
        firstCycle.collectDismissals(dismissals);
        final Double dismissalCredits = dismissals.stream()
                .filter(Dismissal::isCreditsDismissal)
                .map(Dismissal::getEctsCredits)
                .reduce((double) 0, Double::sum);
        row.setCell("Créditos Creditação", dismissalCredits);
        final Double equivalenceCredits = scp.getDismissals().stream()
                .filter(dismissal -> !dismissal.isCreditsDismissal())
                .filter(dismissal -> dismissal.getParentCycleCurriculumGroup() == null || dismissal.getParentCycleCurriculumGroup().isFirstCycle())
                .map(Dismissal::getEctsCredits)
                .reduce((double) 0, Double::sum);
        row.setCell("Créditos Equivalência", equivalenceCredits);
        final Double enrolledCredits = firstCycle.getEnrolments().stream()
                .filter(Enrolment::isEnroled)
                .map(Enrolment::getEctsCredits)
                .reduce((double) 0, Double::sum);
        row.setCell("Créditos Inscritos", enrolledCredits);
        final Double creditsConcluded = firstCycle.getCreditsConcluded();
        row.setCell("Créditos Feitos", creditsConcluded);
        row.setCell("Créditos Por Fazer", Math.max(0, 180 - creditsConcluded));
        row.setCell("Ano Ingresso", registration.getStartDate().getYear());
        final String applications = registration.getPerson().getUser().getIdentity().getAccountSet().stream()
                .flatMap(acc -> acc.getApplicationSet().stream())
                .filter(app -> {
                    if (app.getAdmissionProcessTarget().getOutcomeConfigJson() != null) {
                        final String cycleType = JsonUtils.get(app.getAdmissionProcessTarget().getOutcomeConfigJson(), "cycleType");
                        return cycleType != null && cycleType.equals(CycleType.SECOND_CYCLE.name());
                    } else {
                        return false;
                    }
                })
                .map(app -> app.getAdmissionProcessTarget().getName().getContent())
                .collect(Collectors.joining(","));
        row.setCell("Acordo", registration.getRegistrationProtocol().getDescription().getContent());
        row.setCell("Ingresso", registration.getIngressionType() != null ? registration.getIngressionType().getLocalizedName() : "");
        row.setCell("Candidaturas", applications);
    }

    private boolean hasDirectAccess(String username) {
        final String path = "/afs/ist.utl.pt/ciist/fenix/fenix015/ist/allowed2ndCycles.csv";
        try {
            final List<String> allLines = Files.readAllLines(new File(path).toPath());
            for (String line : allLines) {
                if (line.contains(username)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            throw new Error("Error reading AFS file");
        }
    }
}
