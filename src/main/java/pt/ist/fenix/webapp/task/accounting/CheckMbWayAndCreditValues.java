package pt.ist.fenix.webapp.task.accounting;

import com.google.gson.JsonObject;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.core.json.JsonUtils;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.joda.time.LocalDate;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.payments.domain.SibsPayment;
import pt.ist.payments.domain.SibsPaymentProgressStatus;
import pt.ist.payments.domain.SibsPaymentSystem;

import java.util.Arrays;
import java.util.List;

public class CheckMbWayAndCreditValues extends ReadCustomTask {

    private Money total = new Money(0);
    @Override
    public void runTask() throws Exception {
        final Spreadsheet spreadsheet = new Spreadsheet("MbWays e CC");
        final LocalDate localDate = new LocalDate(2024,12,27);
        SibsPaymentSystem.getInstance().getSibsPaymentSet().stream()
                .filter(sibsPayment -> sibsPayment.getUsedMbWay() || sibsPayment.getUsedCreditOrDebitCard())
                .filter(sibsPayment -> sibsPayment.getStatus().equals(SibsPaymentProgressStatus.SUCCESSFUL))
                .filter(sibsPayment -> sibsPayment.getSettlement() != null)
                .filter(sibsPayment -> localDate.isEqual(sibsPayment.getSettlementDate()))
                .forEach(sibsPayment -> {
                    if (sibsPayment.getAccountingTransaction().getSapRequestSet().isEmpty()) {
                        taskLog("%s\t%s%n", sibsPayment.getEvent(), sibsPayment.getAccountingTransaction().getExternalId());
                    }
                    sibsPayment.getAccountingTransaction().getSapRequestSet().stream()
                            .filter(sr -> sr.getRequestType().equals(SapRequestType.PAYMENT) ||
                                    sr.getRequestType().equals(SapRequestType.PAYMENT_INTEREST) ||
                                    sr.getRequestType().equals(SapRequestType.ADVANCEMENT))
                            .forEach(sr -> report(sr, spreadsheet));
                    total = total.add(sibsPayment.getAccountingTransaction().getOriginalAmount());
                });

        taskLog("Total: %s", total.toString());
        output("mbways_e_cartoes.xlsx", spreadsheet.exportToXLSXSheet());
    }

    private void report(final SapRequest sr, final Spreadsheet spreadsheet) {
        Spreadsheet.Row row = spreadsheet.addRow();
        row.setCell("Evento", sr.getEvent().getExternalId());
        row.setCell("Doc Number", sr.getDocumentNumber());
        row.setCell("Data Valor", getSibsDate(sr));
        row.setCell("Tipo", sr.getRequestType().toString());
        row.setCell("Valor", sr.getValue().add(sr.getAdvancement()).toString());
        row.setCell("Tipo Pagamento", JsonUtils.get(sr.getRequestAsJson()
                .getAsJsonObject("paymentDocument"), "paymentMechanism"));
        row.setCell("Enviado", String.valueOf(sr.getSent()));
        row.setCell("Integrado", String.valueOf(sr.getIntegrated()));
        row.setCell("Data envio", sr.getWhenSent() != null ? sr.getWhenSent().toString("dd/MM/yyyy HH:mm:ss") : "");
    }

    private String getSibsDate(final SapRequest sr) {
        final JsonObject requestAsJson = sr.getRequestAsJson();
        final JsonObject paymentDocument = requestAsJson.get("paymentDocument").getAsJsonObject();
        if (paymentDocument.has("sibsDate")) {
            return paymentDocument.get("sibsDate").getAsString();
        }
        return "";
    }

}