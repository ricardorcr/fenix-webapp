package pt.ist.fenix.webapp.task.accounting;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.accounting.CustomEvent;
import org.fenixedu.academic.domain.accounting.EventType;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;

import java.io.ByteArrayOutputStream;

public class FixDuplicateEventsFromAcademicRequestsMigration extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final Person responsible = User.findByUsername("ist24616").getPerson();
        final Spreadsheet spreadsheet = new Spreadsheet("Eventos cancelados");
        Bennu.getInstance().getAccountingEventsSet().stream()
                .filter(event -> event.getEventType() == EventType.CUSTOM)
                .map(CustomEvent.class::cast)
                .filter(event -> event.getConfig().contains("Pedido de Carta de Curso")
                        || event.getConfig().contains("Pedido de Certidão de Registo"))
                .filter(event -> event.getRequestCost() == null)
                .filter(event -> !event.isCancelled())
                .filter(this::hasAnotherEqual)
                .forEach(event -> {
                    final Spreadsheet.Row row = spreadsheet.addRow();
                    row.setCell("Aluno", event.getPerson().getUsername());
                    row.setCell("Evento", event.getExternalId());
                    row.setCell("Data", event.getWhenOccured().toString("dd/MM/yyyy HH:mm:ss"));
                    row.setCell("Descrição", event.getDescription().toLocalizedString().getContent());
                    event.cancel(responsible, "Evento duplicado");
                });

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        spreadsheet.exportToXLSXSheet(baos);
        output("eventos_cancelados.xlsx", baos.toByteArray());
    }

    private boolean hasAnotherEqual(final CustomEvent customEvent) {
        return customEvent.getPerson().getEventsSet().stream()
                .filter(event -> event != customEvent)
                .filter(event -> event.getEventType() == customEvent.getEventType())
                .map(CustomEvent.class::cast)
                .filter(event -> event.getRequestCost() != null)
                .filter(event -> event.getConfig().equals(customEvent.getConfig()))
                .anyMatch(event -> event.getTotalAmount().equals(customEvent.getTotalAmount()));
    }
}
