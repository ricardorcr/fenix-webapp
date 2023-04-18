package pt.ist.fenix.webapp;

import com.google.gson.JsonObject;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.accounting.CustomEvent;
import org.fenixedu.academic.domain.accounting.EventTemplate;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationDataByExecutionYear;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumModule;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.json.JsonUtils;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import pt.ist.fenixframework.FenixFramework;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class FixDuplicateTuitionEvents extends ReadCustomTask {


    @Override
    public void runTask() throws Exception {
        final List studentsList = Arrays.asList("96849", "95852", "96466", "96402", "96202", "96552", "95755", "92936", "93235", "93546", "95618", "96951", "95566",
                "95675", "96503", "92865", "96462", "96508", "93294", "95902", "87509", "90440", "96818", "95623", "96651", "93034", "96800", "96907", "93230",
                "96834", "95821", "94753", "95848", "96421", "96760", "96316", "96406", "95834", "95795", "95599", "96425", "96249", "96373", "98530", "95960",
                "96798", "95783", "96531", "96414", "95782", "96499", "96388", "95770", "95807", "95668", "95847", "96650", "95535", "95842", "95799", "95844",
                "96480", "95811", "95850", "95543", "96213", "96344", "96910", "96520", "95803", "96309", "95851", "95765", "87195", "96420", "95836", "93601",
                "95843", "92622", "96380", "95643", "96746", "96898", "95893", "95567", "96869", "96518", "90528", "96750", "95637", "95581", "94188", "95686",
                "96334", "95867", "93605", "96810", "96115", "96132", "95532", "95662", "95629", "97247", "96908", "95544", "95555", "96088", "95650", "95613",
                "95600", "96432", "95572", "96833", "96878", "96413", "95628", "96232", "96872", "95861", "95676", "95625", "95788", "95896", "95533", "89861",
                "95574", "92559", "90631", "96009", "93156", "96125", "95575", "95659", "90939", "95612", "89726", "96657", "92536", "96740", "96848", "93514",
                "96273", "96339", "95787", "94774", "95800", "95815", "95688", "70982", "96415", "95849", "96553", "96512", "92903", "95825", "96535", "96409",
                "93571", "95775", "95840", "95814", "96363", "96836", "85008", "96539", "95856", "94166", "96103", "96546", "96225", "96173", "96501", "96796",
                "96536", "95774", "96565", "95796", "95854", "96563", "96871", "96890", "94287", "95678", "95568", "96542", "96228", "96879", "95551", "95674",
                "95579", "96638", "82374", "96701", "95680", "98839", "96349", "95562", "96464", "96124", "96117", "95923", "96912", "96494", "96291", "96696",
                "96114", "96112", "89949", "95590", "95627", "96390", "95526", "96329", "95866", "96816", "95617", "95649", "97190", "95670", "95911", "95609",
                "96025", "95682", "96069", "95546", "93480");

        final ExecutionSemester actualExecutionSemester = ExecutionSemester.readActualExecutionSemester();
        actualExecutionSemester.getEnrolmentsSet().stream()
                .map(CurriculumModule::getRegistration)
                .distinct()
                .filter(r -> r.getSourceRegistration() != null)
                .filter(r -> r.getSourceRegistration().isConcluded())
                .filter(r -> r.getDegree().isSecondCycle())
                .filter(r -> studentsList.contains(r.getStudent().getNumber().toString()))
                .filter(r -> !r.getSourceRegistration().hasAnyEnrolmentsIn(actualExecutionSemester.getExecutionYear()))
                .forEach(this::fix);
    }

    private void fix(final Registration registration) {
        FenixFramework.atomic(() -> {
            final Registration sourceRegistration = registration.getSourceRegistration();
            final String dcpName = registration.getDegreeCurricularPlanName();
            final ExecutionYear currentYear = ExecutionYear.readCurrentExecutionYear();
            taskLog("Trying to fix Tuition Event %s\t%s for %s%n", registration.getNumber(), sourceRegistration.getDegreeCurricularPlanName(), currentYear.getName());

            getGratuityEventsFor(sourceRegistration, currentYear)
                    .forEach(event -> {
                        if (event.canBeCanceled() && !sourceRegistration.hasAnyEnrolmentsIn(currentYear)) {
                            taskLog("Vou cancelar dívida: %s\t%s\t%s%n", event.getExternalId(), event.getPerson().getUsername(), event.getDescription().toString());
                            event.cancel(User.findByUsername("ist24616").getPerson(), "Dívida já criada no mestrado " + dcpName);
                        }
                    });
        });
    }

    private Stream<CustomEvent> getGratuityEventsFor(final Registration registration, final ExecutionYear executionYear) {
        return registration.getPerson().getEventsSet().stream()
                .filter(CustomEvent.class::isInstance)
                .map(CustomEvent.class::cast)
                .filter(event -> !event.isCancelled())
                .filter(event -> EventTemplate.Type.TUITION.isType(event))
                .filter(event -> {
                    final JsonObject configObject = event.getConfigObject();
                    final ExecutionYear eventExecutionYear = JsonUtils.toDomainObject(configObject, "executionYear");
                    Registration eventRegistration = JsonUtils.toDomainObject(configObject, "registration");
                    if (eventRegistration == null) {
                        final RegistrationDataByExecutionYear dataByExecutionYear = JsonUtils.toDomainObject(configObject, "registrationDataByExecutionYear");
                        if (dataByExecutionYear != null && FenixFramework.isDomainObjectValid(dataByExecutionYear)) {
                            eventRegistration = dataByExecutionYear.getRegistration();
                        } else {
                            if (event.canBeCanceled()) {
                                final String eventName = event.getDescription().toString();
                                if (eventName.contains(executionYear.getName()) && eventName.contains(registration.getDegree().getSigla())) {
                                    taskLog("Has no valid data registration: %s\t%s\t%s%n", event.getExternalId(), eventName, registration.getDegree().getSigla());
                                    return eventExecutionYear == executionYear;
                                }
                            }
                        }
                    }
                    return eventExecutionYear == executionYear && eventRegistration == registration;
                });
    }
}
