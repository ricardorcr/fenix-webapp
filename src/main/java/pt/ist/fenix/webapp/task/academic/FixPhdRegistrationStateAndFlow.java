package pt.ist.fenix.webapp.task.academic;

import com.google.gson.JsonObject;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.phd.PhdIndividualProgramProcess;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationState;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationStateType;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.smartFlow.domain.Flow;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import java.util.Objects;

public class FixPhdRegistrationStateAndFlow extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final LocalDate date = new LocalDate(2024, 03, 11);
        final User user = User.findByUsername("ist24616");
        Bennu.getInstance().getPartysSet().stream()
                .filter(Person.class::isInstance)
                .map(Person.class::cast)
                .flatMap(p -> p.getPhdIndividualProgramProcessesSet().stream())
                .map(PhdIndividualProgramProcess::getRegistration)
                .filter(Objects::nonNull)
                .filter(r -> r.getActiveState().getStateType() == RegistrationStateType.REGISTERED)
                .filter(r -> r.getRegistrationStateLogSet().size() == 1)
                .filter(r -> r.getRegistrationStateLogSet().stream().anyMatch(state -> !state.getWhenDateTime().toLocalDate().isBefore(date)))
                .forEach(r -> {
                    switch (r.getPhdIndividualProgramProcess().getActiveState()) {
                        case ABANDON -> {
                            taskLog("%s\t%s\t%s%n", "Abandono", r.getPhdIndividualProgramProcess().getProcessNumber(), r.getPhdIndividualProgramProcess().getExternalId());
                            RegistrationState.createRegistrationState(r, user.getPerson(), new DateTime(), RegistrationStateType.EXTERNAL_ABANDON);
                            deleteFlow(r);
                        }
                        case CANCELLED -> {
                            taskLog("%s\t%s\t%s%n", "Cancelado", r.getPhdIndividualProgramProcess().getProcessNumber(), r.getPhdIndividualProgramProcess().getExternalId());
                            RegistrationState.createRegistrationState(r, user.getPerson(), new DateTime(), RegistrationStateType.CANCELED);
                            deleteFlow(r);
                        }
                        case CONCLUDED -> {
                            taskLog("%s\t%s\t%s%n", "Concluído", r.getPhdIndividualProgramProcess().getProcessNumber(), r.getPhdIndividualProgramProcess().getExternalId());
                            RegistrationState.createRegistrationState(r, user.getPerson(), new DateTime(), RegistrationStateType.SCHOOLPARTCONCLUDED);
                            deleteFlow(r);
                        }
                        case FLUNKED -> {
                            taskLog("%s\t%s\t%s%n", "Prescrito", r.getPhdIndividualProgramProcess().getProcessNumber(), r.getPhdIndividualProgramProcess().getExternalId());
                            RegistrationState.createRegistrationState(r, user.getPerson(), new DateTime(), RegistrationStateType.FLUNKED);
                            deleteFlow(r);
                        }
                        case TRANSFERRED -> {
                            taskLog("%s\t%s\t%s%n", "Transferido", r.getPhdIndividualProgramProcess().getProcessNumber(), r.getPhdIndividualProgramProcess().getExternalId());
                            RegistrationState.createRegistrationState(r, user.getPerson(), new DateTime(), RegistrationStateType.EXTERNAL_ABANDON);
                            deleteFlow(r);
                        }
                        default -> {
                            taskLog("##### %s\t%s\t%s%n", r.getPhdIndividualProgramProcess().getActiveState().toString(), r.getPhdIndividualProgramProcess().getProcessNumber(), r.getPhdIndividualProgramProcess().getExternalId());
                        }
                    }
                });
    }

    private void deleteFlow(Registration registration) {
        registration.getPerson().getUser().getSupervisorFlowSet().stream()
                .filter(flow -> {
                    final JsonObject data = flow.getData().get();
                    return data.has("registration") && data.get("registration").getAsString().equals(registration.getExternalId());
                })
                .forEach(Flow::delete);
    }
}
