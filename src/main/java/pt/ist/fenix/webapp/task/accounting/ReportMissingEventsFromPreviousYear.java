package pt.ist.fenix.webapp.task.accounting;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.accounting.CustomEvent;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.EventTemplate;
import org.fenixedu.academic.domain.accounting.EventType;
import org.fenixedu.academic.domain.accounting.events.AdministrativeOfficeFeeEvent;
import org.fenixedu.academic.domain.accounting.events.insurance.InsuranceEvent;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import pt.ist.fenixframework.FenixFramework;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;

public class ReportMissingEventsFromPreviousYear extends CustomTask {

    private ExecutionYear executionYear = null;

    @Override
    public void runTask() throws Exception {
        executionYear = ExecutionYear.readExecutionYearByName("2022/2023");
        final Spreadsheet spreadsheet = new Spreadsheet("Eventos");
        //23/23
        final List<String> eventsIDs = Arrays.asList("571557067917367", "571557067917368", "571557067917369", "571557067917370", "571557067917371", "571557067917372",
                "571557067917373", "571557067917374", "571557067917375", "571557067917376", "571557067917377", "571557067917378", "571557067917379", "571557067917380",
                "571557067917381");

        //21/22
//        final List<String> eventsIDs = Arrays.asList("571557067917382", "571557067917383", "571557067917384", "571557067917385", "571557067917386", "571557067917387", "571557067917388", "571557067917389", "571557067917390", "571557067917391", "571557067917392", "571557067917393", "571557067917394", "571557067917395", "571557067917396", "571557067917397", "571557067917398", "571557067917399", "571557067917400", "571557067917401", "571557067917402", "571557067917403", "571557067917404", "571557067917405", "571557067917406", "571557067917407", "571557067917408", "571557067917409", "571557067917410", "571557067917411", "571557067917413", "571557067917414", "571557067917415", "571557067917416", "571557067917417", "571557067917418", "571557067917419", "571557067917420", "571557067917421", "571557067917422", "571557067917423", "571557067917424", "571557067917425", "571557067917426", "571557067917427", "571557067917428", "571557067917429", "571557067917430", "571557067917431", "571557067917432", "571557067917433", "571557067917434", "571557067917435", "571557067917436", "571557067917437", "571557067917438", "571557067917439", "571557067917440", "571557067917441", "571557067917442", "571557067917443", "571557067917444", "571557067917445", "571557067917446", "571557067917447", "571557067917448", "571557067917449", "571557067917450", "571557067917451", "571557067917452", "571557067917453", "571557067917454", "571557067917455", "571557067917456", "571557067917457", "571557067917458", "571557067917459", "571557067917460", "571557067917461", "571557067917462", "571557067917463", "571557067917464", "571557067917465", "571557067917466", "571557067917467", "571557067917468", "571557067917469", "571557067917470", "571557067917471", "571557067917472", "571557067917473", "571557067917474", "571557067917475", "571557067917476", "571557067917477", "571557067917478", "571557067917479", "571557067917480", "571557067917481", "571557067917482", "571557067917483", "571557067917484", "571557067917485", "571557067917486", "571557067917487", "571557067917488", "571557067917489", "571557067917490", "571557067917491", "571557067917492", "571557067917493", "571557067917494", "571557067917495", "571557067917496", "571557067917497", "571557067917498", "571557067917499", "571557067917500", "571557067917501", "571557067917502", "571557067917503", "571557067917504", "571557067917505", "571557067917506");

        eventsIDs.stream()
                .map(oid -> (Event) FenixFramework.getDomainObject(oid))
                .filter(this::doesNotHasOldEvent)
                .forEach(event -> report(event, spreadsheet));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(baos);
        output("dividas_por_gerar.xlsx", baos.toByteArray());
    }

    private boolean doesNotHasOldEvent(final Event event) {
        CustomEvent customEvent = (CustomEvent) event;
        if (EventTemplate.Type.TUITION.isType(customEvent)) {
            return customEvent.getPerson().getEventsSet().stream()
                    .filter(Event::isGratuity)
                    .filter(oldEvent -> oldEvent != customEvent)
                    .filter(oldEvent -> oldEvent.executionYearOf() == executionYear)
                    .filter(oldEvent -> {
                        if (customEvent.getConfigObject().has("curricularCourse")) {
                            if (oldEvent instanceof CustomEvent oldCustomEvent) {
                                if (oldCustomEvent.getConfigObject().has("curricularCourse")) {
                                    return oldCustomEvent.getConfigObject().get("curricularCourse").getAsString().equals(
                                            customEvent.getConfigObject().get("curricularCourse").getAsString());
                                }
                            }
                        }
                        return true;
                    })
                    .noneMatch(oldEvent -> oldEvent.getOriginalAmountToPay().equals(customEvent.getOriginalAmountToPay()));
        } else if (EventTemplate.Type.INSURANCE.isType(customEvent)) {
            final boolean customEventResult = customEvent.getPerson().getEventsSet().stream()
                    .filter(otherEvent -> otherEvent != customEvent)
                    .filter(CustomEvent.class::isInstance)
                    .map(CustomEvent.class::cast)
                    .filter(EventTemplate.Type.INSURANCE::isType)
                    .filter(otherEvent -> otherEvent.getExecutionYear() == executionYear)
                    .noneMatch(otherEvent -> otherEvent.getOriginalAmountToPay().equals(customEvent.getOriginalAmountToPay()));
            final boolean oldEventResult = customEvent.getPerson().getEventsByEventType(EventType.INSURANCE).stream()
                    .map(InsuranceEvent.class::cast)
                    .filter(oldEvent -> oldEvent.getExecutionYear() == executionYear)
                    .noneMatch(oldEvent -> oldEvent.getOriginalAmountToPay().equals(customEvent.getOriginalAmountToPay()));
            return customEventResult && oldEventResult;
        } else if (EventTemplate.Type.ADMIN_FEES.isType(customEvent)) {
            final boolean customEventResult = customEvent.getPerson().getEventsSet().stream()
                    .filter(otherEvent -> otherEvent != customEvent)
                    .filter(CustomEvent.class::isInstance)
                    .map(CustomEvent.class::cast)
                    .filter(EventTemplate.Type.ADMIN_FEES::isType)
                    .filter(otherEvent -> otherEvent.getExecutionYear() == executionYear)
                    .noneMatch(otherEvent -> otherEvent.getOriginalAmountToPay().equals(customEvent.getOriginalAmountToPay()));
            final boolean oldEventResult = customEvent.getPerson().getEventsByEventType(EventType.ADMINISTRATIVE_OFFICE_FEE).stream()
                    .map(AdministrativeOfficeFeeEvent.class::cast)
                    .filter(oldEvent -> oldEvent.getExecutionYear() == executionYear)
                    .noneMatch(oldEvent -> oldEvent.getOriginalAmountToPay().equals(customEvent.getOriginalAmountToPay()));
            return customEventResult && oldEventResult;
        }
        return true;
    }

    private void report(final Event event, final Spreadsheet spreadsheet) {
        final Spreadsheet.Row row = spreadsheet.addRow();
        row.setCell("istID", event.getPerson().getUsername());
        row.setCell("Nome", event.getPerson().getName());
        row.setCell("Evento", event.getDescriptionI18N().getContent());
        row.setCell("Valor", event.getTotalAmount().toString());
    }
}
