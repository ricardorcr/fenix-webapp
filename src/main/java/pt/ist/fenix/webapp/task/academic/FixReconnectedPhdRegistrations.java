package pt.ist.fenix.webapp.task.academic;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.phd.PhdIndividualProgramProcess;
import org.fenixedu.academic.domain.phd.PhdIndividualProgramProcessState;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationStateType;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class FixReconnectedPhdRegistrations extends CustomTask {

    @Override
    public void runTask() throws Exception {
        Map<RegistrationStateType, Integer> registrationStateValue = new HashMap<>();
        registrationStateValue.put(RegistrationStateType.CANCELED, 1);
        registrationStateValue.put(RegistrationStateType.INTERRUPTED, 2);
        registrationStateValue.put(RegistrationStateType.EXTERNAL_ABANDON, 3);
        registrationStateValue.put(RegistrationStateType.INTERNAL_ABANDON, 4);
        registrationStateValue.put(RegistrationStateType.REGISTERED, 5);
        registrationStateValue.put(RegistrationStateType.STUDYPLANCONCLUDED, 6);
        registrationStateValue.put(RegistrationStateType.SCHOOLPARTCONCLUDED, 7);
        registrationStateValue.put(RegistrationStateType.CONCLUDED, 8);

        Map<PhdIndividualProgramProcessState, Integer> phdStateValue = new HashMap<>();
        phdStateValue.put(PhdIndividualProgramProcessState.CANCELLED, 1);
        phdStateValue.put(PhdIndividualProgramProcessState.ABANDON, 2);
        phdStateValue.put(PhdIndividualProgramProcessState.TRANSFERRED, 3);
        phdStateValue.put(PhdIndividualProgramProcessState.WORK_DEVELOPMENT, 4);
        phdStateValue.put(PhdIndividualProgramProcessState.THESIS_DISCUSSION, 5);
        phdStateValue.put(PhdIndividualProgramProcessState.CONCLUDED, 6);


        final Spreadsheet reconnectedRegistrations = new Spreadsheet("Matrículas ligadas");
        Bennu.getInstance().getPartysSet().stream()
                .filter(Person.class::isInstance)
                .map(Person.class::cast)
                .flatMap(p -> p.getPhdIndividualProgramProcessesSet().stream())
                .filter(phd -> phd.getRegistration() != null)
                .forEach(phd -> {
                    if (phd.getPerson().getStudent() != null && phd.getPhdProgram() != null) {
                        final Set<Registration> registrations = phd.getPerson().getStudent().getRegistrationsSet().stream()
                                .filter(r -> r.getDegree() == phd.getPhdProgram().getDegree())
                                .collect(Collectors.toSet());
                        if (registrations.size() > 1) {
                            Registration registrationToConnect = null;
                            Registration lowerRegistrationToConnect = null;
                            for (Registration r : registrations) {
                                if (registrationToConnect != null) {
                                    final int rValue = registrationStateValue.get(r.getActiveStateType());
                                    final int toConnectValue = registrationStateValue.get(registrationToConnect.getActiveStateType());
                                    if (rValue > toConnectValue) {
                                        registrationToConnect = r;
                                    } else {
                                        lowerRegistrationToConnect = r;
                                    }
                                } else {
                                    registrationToConnect = r;
                                    lowerRegistrationToConnect = r;
                                }
                            }

                            Set<PhdIndividualProgramProcess> phdSet = new HashSet<>();
                            for (PhdIndividualProgramProcess otherPhd : phd.getPerson().getPhdIndividualProgramProcessesSet()) {
                                if (otherPhd != phd && otherPhd.getPhdProgram().getDegree() == registrationToConnect.getDegree()
                                        && phdStateValue.get(otherPhd.getActiveState()) > 3) {
                                    phdSet.add(otherPhd);
                                }
                            }
                            if (phdSet.isEmpty()) {
                                phd.setRegistration(registrationToConnect);
                                report(reconnectedRegistrations, registrationToConnect, phd);
                            } else {
                                if (phdSet.size() > 1) {
                                    taskLog("Jesus! %s\t%s%n", phd.getPerson().getName(), phdSet.size());
                                    if (phdSet.size() == registrations.size()) {
                                        taskLog("\t-->Menos Mal!");
                                    }
                                } else {
                                    final PhdIndividualProgramProcess otherPhd = phdSet.iterator().next();
                                    final int phdValue = phdStateValue.get(phd.getActiveState());
                                    final int otherPhdValue = phdStateValue.get(otherPhd.getActiveState());
                                    if (phdValue > otherPhdValue) {
                                        phd.setRegistration(registrationToConnect);
                                        otherPhd.setRegistration(lowerRegistrationToConnect);
                                        report(reconnectedRegistrations, registrationToConnect, phd);
                                    } else {
                                        otherPhd.setRegistration(registrationToConnect);
                                        phd.setRegistration(lowerRegistrationToConnect);
                                        report(reconnectedRegistrations, lowerRegistrationToConnect, phd);
                                    }
                                }
                            }
                        }
                    }
                });
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        reconnectedRegistrations.exportToXLSSheet(baos);
        output("matriculas_ligadas.xlsx", baos.toByteArray());
    }

    private void report(final Spreadsheet reconnectedRegistrations, final Registration r, final PhdIndividualProgramProcess phd) {
        final Spreadsheet.Row row = reconnectedRegistrations.addRow();
        row.setCell("OID", r.getExternalId());
        row.setCell("Matrícula", r.getDegreeCurricularPlanName());
        row.setCell("Estado Matrícula", r.getActiveState().getStateType().getName());
        row.setCell("Ligada", r.getPhdIndividualProgramProcess() != null ? "Sim" : "Não");
        String registrationPhd = "";
        if (r.getPhdIndividualProgramProcess() != null) {
            registrationPhd = r.getPhdIndividualProgramProcess().getProcessNumber();
        }
        row.setCell("What", registrationPhd);
        row.setCell("Phd", phd.getProcessNumber());
        row.setCell("Estado Phd", phd.getActiveState().getName());
        row.setCell("Aluno", r.getPerson().getUsername());
        row.setCell("Nome", r.getPerson().getName());
    }
}
