package pt.ist.fenix.webapp.task.paperPusher.academic;

import org.fenixedu.academic.domain.accounting.AccountingTransaction;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.smartForms.domain.SmartFormsSystem;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.stream.Collectors;

public class ReportPendingPaymentRequests extends CustomTask {

    @Override
    public void runTask() throws Exception {
        Spreadsheet spreadsheet = new Spreadsheet("Pagamento pendente");
        SmartFormsSystem.getInstance().getRequestTypeSet().stream()
                .filter(rt -> rt.getName().getContent().contains("Carta de Curso") ||
                        rt.getName().getContent().contains("Certidão de Registo") ||
                        rt.getName().getContent().contains("Suplemento ao Diploma"))
                .flatMap(rt -> rt.getCurrentRequestTypeVersion().getRequestSet().stream())
                .filter(request -> request.getLockInstant() != null)
                .filter(request -> request.getRequestCost() != null && request.getRequestCost().getEvent() != null)
                .filter(request -> request.getRequestCost().getEvent().isOpen())
                .forEach(request -> {
                    final List<AccountingTransaction> adjustedTransactions = request.getRequestCost().getEvent().getAllAdjustedAccountingTransactions();
                    final Spreadsheet.Row row = spreadsheet.addRow();
                    final User user = request.getRequester().getUser();
                    row.setCell("IstID", user != null ? user.getUsername() : "");
                    row.setCell("Nome", user != null ? user.getPerson().getName() : "");
                    row.setCell("Tipo", request.getRequestTypeVersion().getRequestType().getName().getContent());
                    row.setCell("Concluído", request.getConcluded() ? "Sim" : "Não");
                    row.setCell("Data Criação", request.getCreationInstant().toString("dd/MM/yyyy HH:mm:ss"));
                    row.setCell("Fila", request.getRequestQueue().getName().getContent());
                    row.setCell("Evento", request.getRequestCost().getEvent().getExternalId());
                    String comments = "";
                    if (!adjustedTransactions.isEmpty()) {
                        row.setCell("Pagamento anulado", "Sim");
                        comments = adjustedTransactions.stream()
                                .map(tx -> tx.getTransactionDetail().getComments())
                                .collect(Collectors.joining(","));
                    } else {
                        row.setCell("Pagamento anulado", "Não");
                    }
                    row.setCell("Comentário", comments);
                });
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(baos);
        output("pagamentos_pendentes.xlsx", baos.toByteArray());
    }
}
