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

public class CreateRegistrationsForPhdProcessesWithoutDegree extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final Space alamedaCampus = FenixFramework.getDomainObject("2448131360897"); //Alameda
        final Spreadsheet spreadsheet = new Spreadsheet("Validar");
        final Spreadsheet createdRegistrations = spreadsheet.addSpreadsheet("Matrículas Criadas");

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
                    if (phd.getRegistration() == null) {
                        if (date != null) {
                            final ExecutionYear executionYear = ExecutionYear.readByDateTime(date);
                            final PhdProgram phdProgram = phd.getPhdProgram();
                            if (phdProgram != null) {
                                Degree degree = phdProgram.getDegree();
//                                if (degree == null) {
//                                    final Set<Degree> degreeSet = Degree.readNotEmptyDegrees().stream()
//                                            .filter(d -> d.getNameI18N().getContent().contains(phdProgram.getName().getContent()))
//                                            .collect(Collectors.toSet());
//                                    degree = getDegree(phdProgram);
//                                }
                                if (degree != null) {
                                    final Registration registration = createRegistration(phd, degree, executionYear, alamedaCampus);
                                    reportCreatedRegistration(phd, createdRegistrations, registration);
                                } else {
                                    final Set<Degree> degreeSet = Degree.readNotEmptyDegrees().stream()
                                            .filter(Degree::isDEA)
                                            .filter(d -> d.getNameI18N().getContent().contains(phdProgram.getName().getContent()))
                                            .collect(Collectors.toSet());
                                    Degree deaDegree = null;
                                    if (!degreeSet.isEmpty()) {
                                        if (degreeSet.size() > 1) {
                                            deaDegree = degreeSet.stream().filter(d -> d.getSigla().equals("DEFT")).findAny().orElseGet(() -> null);
                                        } else {
                                            deaDegree = degreeSet.iterator().next();
                                        }
                                        final Registration registration = createRegistration(phd, deaDegree, executionYear, alamedaCampus);
                                        phd.getPhdProgram().setDegree(deaDegree);
                                        reportCreatedRegistration(phd, createdRegistrations, registration);
                                    }
                                    final Spreadsheet.Row row = spreadsheet.addRow();
                                    row.setCell("OID", phd.getExternalId());
                                    row.setCell("Phd", phd.getProcessNumber());
                                    row.setCell("Data Estudos", date.toString("dd/MM/yyyy"));
                                    row.setCell("Estado Phd", phd.getActiveState().getName());
                                    row.setCell("Data Estado", phd.getMostRecentState().getStateDate().toString("dd/MM/yyyy"));
                                    row.setCell("Programa", phd.getPhdProgram().getName().getContent());
                                    row.setCell("Aluno", phd.getPerson().getName());
                                    row.setCell("Problema", "Sem Curso");
                                    int count = 0;
                                    for (Degree d : degreeSet) {
                                        row.setCell("Sigla" + count, d.getExternalId() + "-" + d.getSigla());
                                        count++;
                                    }
                                }
                            }
                        }
                    }
                });

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(baos);
        output("matriculas_phd_concluded.xlsx", baos.toByteArray());
    }

    private static void reportCreatedRegistration(PhdIndividualProgramProcess phd, Spreadsheet createdRegistrations, Registration registration) {
        final Spreadsheet.Row row = createdRegistrations.addRow();
        row.setCell("OID", registration.getExternalId());
        row.setCell("Matrícula", registration.getDegreeCurricularPlanName());
        row.setCell("Estado Matrícula", registration.getActiveState().getStateType().getName());
        row.setCell("Phd", phd.getProcessNumber());
        row.setCell("Estado Phd", phd.getActiveState().getName());
        row.setCell("Aluno", registration.getPerson().getUsername());
        row.setCell("Nome", registration.getPerson().getName());
    }

    private Registration createRegistration(PhdIndividualProgramProcess phd, Degree degree, ExecutionYear executionYear, Space alamedaCampus) {
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
                taskLog("Não há execução para %s%n", executionYear.getName());
                curricularPlans.forEach(dcp -> dcp.createExecutionDegree(executionYear, alamedaCampus, false));
            }

        }
        dcps = degree.getDegreeCurricularPlansForYear(executionYear);
        if (dcps.isEmpty()) {
            taskLog("Curso %s%n", degree.getNameI18N().getContent());
        }
        final Registration registration = new Registration(phd.getPerson(), dcps.iterator().next(),
                RegistrationProtocol.getRegular(), CycleType.THIRD_CYCLE, executionYear);
        registration.setHomologationDate(phd.getCandidacyProcess().getWhenRatified());
        registration.setStudiesStartDate(phd.getCandidacyProcess().getWhenStartedStudies());
        registration.setIngressionType(IngressionType.findByPredicate(IngressionType::isInternal3rdCycleAccess).orElse(null));
        registration.setPhdIndividualProgramProcess(phd);
        return registration;
    }
}
