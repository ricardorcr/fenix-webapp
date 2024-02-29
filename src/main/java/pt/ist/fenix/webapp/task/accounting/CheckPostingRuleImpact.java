package pt.ist.fenix.webapp.task.accounting;

import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;

import java.io.ByteArrayOutputStream;

public class CheckPostingRuleImpact extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        final Spreadsheet spreadsheet = new Spreadsheet("Eventos");
        final Spreadsheet erros = spreadsheet.addSpreadsheet("Erros");
        Bennu.getInstance().getAccountingEventsSet().stream()
                .filter(event -> event.getEventStartDate().getYear() == 2004 || event.getEventStartDate().getYear() == 2005
                || event.getEventStartDate().getYear() == 2003 || event.getEventStartDate().getYear() == 2006)
                .forEach(event -> {
                    try {
                        final Money amountToPay = event.getAmountToPay();
                        final Spreadsheet.Row row = spreadsheet.addRow();
                        row.setCell("Evento", event.getExternalId());
                        row.setCell("Valor", amountToPay.toString());
                    } catch (Exception e) {
                        final Spreadsheet.Row row = erros.addRow();
                        row.setCell("Evento", event.getExternalId());
                        row.setCell("Erro", e.getMessage());
                    }
                });
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(baos);
        output("check_events.xlsx", baos.toByteArray());
    }
}
