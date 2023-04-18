package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.candidacy.IngressionType;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.admissions.ist.domain.Utils;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.WriteCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.connect.domain.Identity;

import java.io.ByteArrayOutputStream;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class FixMissingSourceRegistrationsFromTransitions extends WriteCustomTask {

    @Override
    public void runTask() throws Exception {
        final Spreadsheet spreadsheet = new Spreadsheet("SourceRegistrationLog");
        final Spreadsheet ingressionSheet = spreadsheet.addSpreadsheet("IngressionTypeLog");
        Bennu.getInstance().getRegistrationsSet().stream()
                .filter(registration -> registration.getSourceRegistration() == null)
                .filter(registration -> registration.getIngressionType() == null)
                .forEach(registration -> {
                    final Registration source = findSourceRegistration(registration);
                    if (source != null) {
                        taskLog("Fix source registration for: %s ; destination: %s ; source: %s%n",
                                registration.getPerson().getUsername(),
                                registration.getDegree().getSigla(),
                                source.getDegree().getSigla());
                        spreadsheet.addRow()
                                .setCell("Person", registration.getPerson().getUsername())
                                .setCell("DestinationRegistration", registration.getExternalId())
                                .setCell("DestinationDegree", registration.getDegree().getSigla())
                                .setCell("SourceRegistration", source.getExternalId())
                                .setCell("SourceDegree", source.getDegree().getSigla());
                        registration.setSourceRegistration(source);
                    }
                });

        final IngressionType ingressionType = IngressionType.findIngressionTypeByCode("DA1C").get();
        Bennu.getInstance().getRegistrationsSet().stream()
                .filter(registration -> registration.getIngressionType() == null)
                .filter(registration -> registration.getDegree().isSecondCycle())
                .filter(registration -> !hasAdmissionsProcess(registration))
                .filter(registration -> registration.getRegistrationDataByExecutionYearSet().stream().anyMatch(data -> data.getExecutionYear().isCurrent()))
                .forEach(registration -> {
                    final Registration originRegistration = findOriginWithAffinity(registration, registration.getDegree());
                    final Spreadsheet.Row row = ingressionSheet.addRow()
                            .setCell("Person", registration.getPerson().getUsername())
                            .setCell("Registration", registration.getExternalId())
                            .setCell("Registration", registration.getDegree().getSigla());
                    if (originRegistration != null) {
                        row.setCell("OriginRegistration", originRegistration.getExternalId())
                                .setCell("OriginRegistration", originRegistration.getDegree().getSigla());
//                    registration.setIngressionType(ingressionType);
                    } else {
                    }
                });


        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(stream);
        output("SourceRegistrationLog.xlsx", stream.toByteArray());
    }

    private Registration findOriginWithAffinity(final Registration registration, final Degree degree) {
        return registration.getStudent().getRegistrationsSet().stream()
                .filter(other -> other != registration)
                .filter(other -> other.getStudentCurricularPlanStream()
                        .map(scp -> scp.getCycle(CycleType.FIRST_CYCLE))
                        .filter(Objects::nonNull)
                        .anyMatch(group -> group.isConcluded()))
                .filter(other -> other.getLastStudentCurricularPlan().getDegreeCurricularPlan()
                        .getDestinationAffinities(CycleType.FIRST_CYCLE).stream()
                        .anyMatch(cycleCourseGroup -> cycleCourseGroup.getDegree() == degree))
                .max((r1, r2) -> r1.getStartExecutionYear().compareTo(r2.getStartExecutionYear()))
                .orElse(null);
    }

    private boolean hasAdmissionsProcess(final Registration registration) {
        final Identity identity = registration.getStudent().getPerson().getUser().getIdentity();
        return identity != null && identity.getAccountSet().stream()
                .flatMap(account -> account.getApplicationSet().stream())
                .filter(application -> Utils.registrationFor(application) == registration)
                .anyMatch(application -> Utils.degree(application.getAdmissionProcessTarget()) == registration.getDegree());
    }

    private Registration findSourceRegistration(final Registration registration) {
        final Student student = registration.getStudent();
        final Registration result = student.getStudentDegreeCurricularTransitionPlanSet().stream()
                .filter(plan -> plan.getDegreeCurricularTransitionPlan().getDestinationDegreeCurricularPlan().getDegree() == registration.getDegree())
                .map(plan -> student.getRegistrationsSet().stream().filter(other -> other.getDegree() == plan.getDegreeCurricularTransitionPlan().getOriginDegreeCurricularPlan().getDegree()).findAny().orElse(null))
                .filter(Objects::nonNull)
                .filter(other -> other != registration)
                .findAny().orElse(null);
        if (result != null) {
            return result;
        }
        final Set<DegreeCurricularPlan> originPlans = registration.getLastStudentCurricularPlan().getDegreeCurricularPlan().getOriginTransitionPlanSet().stream()
                .map(plan -> plan.getOriginDegreeCurricularPlan())
                .collect(Collectors.toSet());
        return student.getRegistrationsSet().stream()
                .filter(other -> other != registration)
                .filter(other -> originPlans.contains(other.getLastDegreeCurricularPlan()))
                .max((r1, r2) -> r1.getStartExecutionYear().compareTo(r2.getStartExecutionYear()))
                .orElse(null);
    }

}