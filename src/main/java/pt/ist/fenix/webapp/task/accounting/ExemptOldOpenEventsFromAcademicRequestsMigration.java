package pt.ist.fenix.webapp.task.accounting;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.accounting.CustomEvent;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.EventType;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import pt.ist.fenixframework.FenixFramework;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;

public class ExemptOldOpenEventsFromAcademicRequestsMigration extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final List<String> toExempt = Arrays.asList("1978931951439059","1978931951439055","1978931951439054","1978931951439053","1978931951439052","1978931951439050",
                "1978931951439047","1978931951439046","1978931951439045","1978931951439043","1978931951439042","1978931951439041","1978931951439039","1978931951439038",
                "1978931951439031","1978931951439030","1978931951439029","1978931951439028","1978931951439026","1978931951439025","1978931951439024","1978931951439022",
                "1978931951439020","1978931951439017","1978931951439016","1978931951439012","1978931951439009","1978931951439008","1978931951439006","1978931951439005",
                "1978931951439004","1978931951439002","1978931951439001","1978931951439000","1978931951438994","1978931951438993","1978931951438990","1978931951438988",
                "1978931951438987","1978931951438986","1978931951438982","1978931951438978","1978931951438977","1978931951438976","1978931951438975","1978931951438973",
                "1978931951438969","1978931951438966","1978931951438965","1978931951438963","1978931951438962","1978931951438961","1978931951438960","1978931951438959",
                "1978931951438958","1978931951438955","1978931951438953","1978931951438952","1978931951438951","1978931951438950","1978931951438946","1978931951438945",
                "1978931951438941","1978931951438940","1978931951438934","1978931951438931","1978931951438930","1978931951438927");

        final Person responsible = User.findByUsername("ist24616").getPerson();
        final Spreadsheet spreadsheet = new Spreadsheet("Eventos isentados");
        toExempt.stream()
                .map(eventID -> (CustomEvent) FenixFramework.getDomainObject(eventID))
                .filter(event -> event.getRequestCost() != null)
                .forEach(event -> {
                    if (event.getRequestCost().getRequest().getConcluded()) {
                        final Spreadsheet.Row row = spreadsheet.addRow();
                        row.setCell("Aluno", event.getPerson().getUsername());
                        row.setCell("Evento", event.getExternalId());
                        row.setCell("Data", event.getWhenOccured().toString("dd/MM/yyyy HH:mm:ss"));
                        row.setCell("Descrição", event.getDescription().toLocalizedString().getContent());
                        event.exempt(responsible, "Dívida já paga e sem dados de pagamento para migrar");
                    } else {
                        taskLog("Request: %s\tEvent: %s%n", event.getRequestCost().getRequest().getExternalId(), event.getExternalId());
                    }
                });

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        spreadsheet.exportToXLSXSheet(baos);
        output("eventos_isentados.xlsx", baos.toByteArray());
    }
}
