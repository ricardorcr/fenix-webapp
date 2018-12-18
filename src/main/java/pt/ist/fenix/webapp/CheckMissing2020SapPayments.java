package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.accounting.AccountingTransaction;
import org.fenixedu.academic.domain.accounting.ResidenceEvent;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;

import java.io.ByteArrayOutputStream;

public class CheckMissing2020SapPayments extends CustomTask {

    @Override
    public void runTask() throws Exception {
        Spreadsheet spreadsheet = new Spreadsheet("Pagamentos_por_enviar_2020");
        Bennu.getInstance().getAccountingTransactionsSet().stream()
                .filter(at -> at.getWhenRegistered().getYear() == 2020)
                .filter(at -> !at.isAdjustingTransaction() && at.getAdjustmentTransactionsSet().isEmpty())
                .filter(at -> at.getSapRequestSet().isEmpty())
                .filter(at -> !(at.getEvent() instanceof ResidenceEvent))
                //.filter(at -> at.getEvent().getWhenOccured().getYear() == 2020)
                .forEach(at -> report(spreadsheet, at));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(outputStream);
        output("Pagamentos_por_enviar_2020.xls", outputStream.toByteArray());
    }

    private void report(Spreadsheet spreadsheet, AccountingTransaction at) {
        Spreadsheet.Row row = spreadsheet.addRow();
        row.setCell("Evento Id", at.getEvent().getExternalId());
        row.setCell("Transaction Id", at.getExternalId());
        row.setCell("Valor", at.getOriginalAmount().getAmountAsString());
        row.setCell("Data registo", at.getWhenRegistered().toString("dd-MM-yyyy HH:mm:ss"));
        row.setCell("Data processamento", at.getWhenProcessed().toString("dd-MM-yyyy HH:mm:ss"));
        row.setCell("Método pagamento", at.getTransactionDetail().getPaymentMethod().getLocalizedName());
    }
}
