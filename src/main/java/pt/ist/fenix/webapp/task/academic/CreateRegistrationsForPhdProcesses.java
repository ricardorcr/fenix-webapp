package pt.ist.fenix.webapp.task.academic;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.candidacy.IngressionType;
import org.fenixedu.academic.domain.degreeStructure.CurricularStage;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.academic.domain.phd.PhdIndividualProgramProcess;
import org.fenixedu.academic.domain.phd.PhdProgram;
import org.fenixedu.academic.domain.phd.serviceRequests.documentRequests.PhdRegistryDiplomaRequest;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationProtocol;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.spaces.domain.Space;
import org.joda.time.LocalDate;
import pt.ist.fenixframework.FenixFramework;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CreateRegistrationsForPhdProcesses extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final int[] count = new int[]{0, 0, 0, 0, 0, 0};
        final Space alamedaCampus = FenixFramework.getDomainObject("2448131360897"); //Alameda

        Bennu.getInstance().getPartysSet().stream()
                .filter(Person.class::isInstance)
                .map(Person.class::cast)
                .flatMap(p -> p.getPhdIndividualProgramProcessesSet().stream())
                .filter(phd -> phd.getRegistration() == null)
                .filter(this::hasCertificateOfRegistration)
                .forEach(phd -> {
                    final LocalDate date = phd.getWhenStartedStudies();//StartedStudies();
                    if (phd.getPerson().getStudent() != null) {
                        phd.getPerson().getStudent().getRegistrationsSet().stream()
                                .filter(r -> r.getDegree() == phd.getPhdProgram().getDegree())
                                .forEach(phd::setRegistration);
                    }
                    if (phd.getPerson().getStudent() != null && phd.getPerson().getStudent().getRegistrationsSet().stream()
                            .filter(r -> r.getDegree() == phd.getPhdProgram().getDegree())
//                            .peek(r -> taskLog("%s\t%s%n", r.getNumber(), r.getLastStateType().getName()))
                            .findAny().isPresent()) {
                        count[5] = count[5] + 1;
                    }
                    if (phd.getRegistration() == null) {
                        if (date != null) {
                            final ExecutionYear executionYear = ExecutionYear.readByDateTime(date);
                            final PhdProgram phdProgram = phd.getPhdProgram();
                            if (phdProgram != null) {
                                final Degree degree = phdProgram.getDegree();
                                if (degree != null) {
                                    List<DegreeCurricularPlan> dcps = degree.getDegreeCurricularPlansForYear(executionYear);
                                    if (dcps.isEmpty()) {
                                        count[1] = count[1] + 1;
                                        final Set<DegreeCurricularPlan> curricularPlans = degree.getDegreeCurricularPlansSet().stream()
                                                .filter(dcp -> dcp.getCurricularStage() == CurricularStage.APPROVED)
                                                .collect(Collectors.toSet());
                                        if (curricularPlans.size() > 1) {
                                            taskLog(phd.getProcessNumber());
                                            curricularPlans.forEach(dcp -> taskLog("    %s%n", dcp.getName()));
                                            curricularPlans.stream()
                                                    .filter(dcp -> dcp.getName().endsWith("2006"))
                                                    .forEach(dcp -> dcp.createExecutionDegree(executionYear, alamedaCampus, false));
                                        } else {
                                            curricularPlans.forEach(dcp -> dcp.createExecutionDegree(executionYear, alamedaCampus, false));
                                        }

                                    }
                                    dcps = degree.getDegreeCurricularPlansForYear(executionYear);
                                    final Registration registration = new Registration(phd.getPerson(), dcps.iterator().next(),
                                            RegistrationProtocol.getRegular(), CycleType.THIRD_CYCLE, executionYear);
                                    registration.setHomologationDate(phd.getCandidacyProcess().getWhenRatified());
                                    registration.setStudiesStartDate(phd.getCandidacyProcess().getWhenStartedStudies());
                                    registration.setIngressionType(IngressionType.findByPredicate(IngressionType::isInternal3rdCycleAccess).orElse(null));
                                    registration.setPhdIndividualProgramProcess(phd);
                                    taskLog("Humm: %s%n", phd.getProcessNumber());
                                    count[0] = count[0] + 1;
                                } else {
                                    count[2] = count[2] + 1;
                                }
                            } else {
                                count[3] = count[3] + 1;
                            }
                        } else {
                            count[4] = count[4] + 1;
                        }
                    }
                });
        taskLog("Criadas %s%n", count[0]);
        taskLog("Sem DCP %s%n", count[1]);
        taskLog("Sem curso %s%n", count[2]);
        taskLog("Sem PhdProgram %s%n", count[3]);
        taskLog("Sem ano %s%n", count[4]);
        taskLog("Com registration não ligada: %s%n", count[5]);
    }

    private boolean hasCertificateOfRegistration(final PhdIndividualProgramProcess phdIndividualProgramProcess) {
        return phdIndividualProgramProcess.getPhdAcademicServiceRequestsSet().stream()
                .anyMatch(PhdRegistryDiplomaRequest.class::isInstance);
    }
}
