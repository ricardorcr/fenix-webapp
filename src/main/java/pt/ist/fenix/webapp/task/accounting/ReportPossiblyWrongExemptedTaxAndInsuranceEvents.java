package pt.ist.fenix.webapp.task.accounting;

import com.google.gson.JsonObject;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.accounting.CustomEvent;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.EventTemplate;
import org.fenixedu.academic.domain.accounting.events.EventExemptionJustificationType;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;

import java.io.ByteArrayOutputStream;

public class ReportPossiblyWrongExemptedTaxAndInsuranceEvents extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        final Spreadsheet spreadsheet = new Spreadsheet("Eventos");
        final ExecutionYear currentExecutionYear = ExecutionYear.readCurrentExecutionYear();
        Bennu.getInstance().getAccountingEventsSet().stream()
                .filter(CustomEvent.class::isInstance)
                .map(CustomEvent.class::cast)
                .filter(event -> event.getExecutionYear() == currentExecutionYear)
                .filter(ReportPossiblyWrongExemptedTaxAndInsuranceEvents::isInsurance)
                .filter(ReportPossiblyWrongExemptedTaxAndInsuranceEvents::isExemptedFromListener)
                .filter(ReportPossiblyWrongExemptedTaxAndInsuranceEvents::hasUCIEvent)
                .forEach(event -> report(spreadsheet, event));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(baos);
        output("possiveis_taxas_por_pagar.xlsx", baos.toByteArray());
    }

    private static boolean isInsurance(CustomEvent event) {
        final JsonObject configObject = event.getConfigObject();
        final String type = configObject.has("type") ? configObject.get("type").getAsString() : null;
        return EventTemplate.Type.INSURANCE.toString().equals(type);
    }

    private static boolean hasUCIEvent(CustomEvent event) {
        return event.getPerson().getEventsSet().stream()
                .filter(CustomEvent.class::isInstance)
                .map(CustomEvent.class::cast)
                .anyMatch(otherEvent -> otherEvent.getConfigObject().has("curricularCourse"));
    }

    private static boolean isExemptedFromListener(CustomEvent event) {
        return event.getExemptionsStream()
                .anyMatch(exemption -> {
                    if (exemption.getResponsible() == event.getPerson()
                            && exemption.getExemptionJustification().getJustificationType() == EventExemptionJustificationType.CANCELLED
                            && exemption.getExemptionJustification().getReason().equals("Changed payment plan")) {
                        return true;
                    } else {
                        return false;
                    }
                });
    }

    private void report(final Spreadsheet spreadsheet, final Event event) {
        final Spreadsheet.Row row = spreadsheet.addRow();
        row.setCell("EventoID", event.getExternalId());
        row.setCell("User", event.getPerson().getUsername());
        row.setCell("Evento", "https://fenix.tecnico.ulisboa.pt/accounting-management/" + event.getExternalId() + "/details");
    }
}
