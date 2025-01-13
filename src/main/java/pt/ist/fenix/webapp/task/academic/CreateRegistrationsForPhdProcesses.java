package pt.ist.fenix.webapp.task.academic;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.candidacy.IngressionType;
import org.fenixedu.academic.domain.degreeStructure.CurricularStage;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.academic.domain.phd.PhdIndividualProgramProcess;
import org.fenixedu.academic.domain.phd.PhdIndividualProgramProcessState;
import org.fenixedu.academic.domain.phd.PhdProgram;
import org.fenixedu.academic.domain.phd.serviceRequests.documentRequests.PhdRegistryDiplomaRequest;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationProtocol;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.spaces.domain.Space;
import org.joda.time.LocalDate;
import pt.ist.fenixframework.FenixFramework;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CreateRegistrationsForPhdProcesses extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final Space alamedaCampus = FenixFramework.getDomainObject("2448131360897"); //Alameda
        final Spreadsheet spreadsheet = new Spreadsheet("Validar");
        final Spreadsheet reconnectedRegistrations = spreadsheet.addSpreadsheet("Matrículas ligadas");
        final Spreadsheet createdRegistrations = reconnectedRegistrations.addSpreadsheet("Matrículas criadas");

        Bennu.getInstance().getPartysSet().stream()
                .filter(Person.class::isInstance)
                .map(Person.class::cast)
                .flatMap(p -> p.getPhdIndividualProgramProcessesSet().stream())
                .filter(phd -> phd.getRegistration() == null)
                .filter(phd -> phd.getActiveState() != PhdIndividualProgramProcessState.CANDIDACY
                        && phd.getActiveState() != PhdIndividualProgramProcessState.NOT_ADMITTED
                        && phd.getActiveState() != PhdIndividualProgramProcessState.CANCELLED)
                .forEach(phd -> {
                    final LocalDate date = phd.getWhenStartedStudies();
                    if (phd.getPerson().getStudent() != null && phd.getPhdProgram() != null) {
                        phd.getPerson().getStudent().getRegistrationsSet().stream()
                                .filter(r -> r.getDegree() == phd.getPhdProgram().getDegree())
                                .forEach(r -> {
                                    phd.setRegistration(r);
                                    final Spreadsheet.Row row = reconnectedRegistrations.addRow();
                                    row.setCell("OID", r.getExternalId());
                                    row.setCell("Matrícula", r.getDegreeCurricularPlanName());
                                    row.setCell("Estado Matrícula", r.getActiveState().getStateType().getName());
                                    row.setCell("Phd", phd.getProcessNumber());
                                    row.setCell("Estado Phd", phd.getActiveState().getName());
                                    row.setCell("Aluno", r.getPerson().getUsername());
                                    row.setCell("Nome", r.getPerson().getName());
                                });
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
                                    final Spreadsheet.Row row = createdRegistrations.addRow();
                                    row.setCell("OID", registration.getExternalId());
                                    row.setCell("Matrícula", registration.getDegreeCurricularPlanName());
                                    row.setCell("Estado Matrícula", registration.getActiveState().getStateType().getName());
                                    row.setCell("Phd", phd.getProcessNumber());
                                    row.setCell("Estado Phd", phd.getActiveState().getName());
                                    row.setCell("Aluno", registration.getPerson().getUsername());
                                    row.setCell("Nome", registration.getPerson().getName());
                                } else {
                                    final Spreadsheet.Row row = spreadsheet.addRow();
                                    row.setCell("OID", phd.getExternalId());
                                    row.setCell("Phd", phd.getProcessNumber());
                                    row.setCell("Estado Phd", phd.getActiveState().getName());
                                    row.setCell("Programa", phd.getPhdProgram().getName().getContent());
                                    row.setCell("Aluno", phd.getPerson().getName());
                                    row.setCell("Problema", "Sem Curso");
                                }
                            } else {
                                final Spreadsheet.Row row = spreadsheet.addRow();
                                row.setCell("OID", phd.getExternalId());
                                row.setCell("Phd", phd.getProcessNumber());
                                row.setCell("Estado Phd", phd.getActiveState().getName());
                                row.setCell("Programa", "");
                                row.setCell("Aluno", phd.getPerson().getName());
                                row.setCell("Problema", "Sem Programa");
                            }
                        } else {
                            final Spreadsheet.Row row = spreadsheet.addRow();
                            row.setCell("OID", phd.getExternalId());
                            row.setCell("Phd", phd.getProcessNumber());
                            row.setCell("Estado Phd", phd.getActiveState().getName());
                            row.setCell("Programa", phd.getPhdProgram() != null ? phd.getPhdProgram().getName().getContent() : "Sem Programa!");
                            row.setCell("Aluno", phd.getPerson().getName());
                            row.setCell("Problema", "Sem Data Estudos");
                        }
                    }
                });

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(baos);
        output("matriculas_phd.xlsx", baos.toByteArray());
//        throw new Error("Dry run");
    }

    private boolean hasCertificateOfRegistration(final PhdIndividualProgramProcess phdIndividualProgramProcess) {
        return phdIndividualProgramProcess.getPhdAcademicServiceRequestsSet().stream()
                .anyMatch(PhdRegistryDiplomaRequest.class::isInstance);
    }
}
