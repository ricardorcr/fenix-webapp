package pt.ist.fenix.webapp;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.accounting.AccountingTransaction;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.Refund;
import org.fenixedu.academic.domain.accounting.accountingTransactions.detail.SibsTransactionDetail;
import org.fenixedu.academic.util.Money;
import org.fenixedu.academic.util.sibs.incomming.SibsIncommingPaymentFileDetailLine;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.commons.spreadsheet.Spreadsheet.Row;
import org.joda.time.YearMonthDay;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.domain.SapRoot;
import pt.ist.fenixedu.giaf.invoices.Utils;

public class DumpPaymentsWithSapInfo extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        final String name = "DocumentosPagamento";
        final Spreadsheet sheet = new Spreadsheet(name);
        final Spreadsheet sheetByDay = new Spreadsheet(name + "Day");

        final Map<String, Money> valueMap = new HashMap<>();

        SapRoot.getInstance().getSapRequestSet().stream()
                .filter(sr -> isPayment(sr))
                .filter(sr -> !sr.isInitialization())
//                .filter(sr -> !sr.getIgnore())
                .filter(sr -> sr.getOriginalRequest() == null && sr.getAnulledRequest() == null)
                .filter(sr -> !isIgnoredAndTxAlreadySent(sr))
                .forEach(sr -> process(sheet, valueMap, sr));

        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        sheet.exportToXLSSheet(stream);
        output(name + ".xls", stream.toByteArray());

        valueMap.forEach((k, v) -> {
            final Row row = sheetByDay.addRow();
            row.setCell("SIBS Date", k);
            row.setCell("Value", v.toPlainString());
        });
        final ByteArrayOutputStream streamDay = new ByteArrayOutputStream();
        sheetByDay.exportToXLSSheet(streamDay);
        output(name + "Day.xls", streamDay.toByteArray());
    }

    private boolean isIgnoredAndTxAlreadySent(final SapRequest sapRequest) {
        if (sapRequest.getIgnore() && sapRequest.getPayment() != null
                && sapRequest.getAnulledRequest() == null && sapRequest.getOriginalRequest() == null) {
            final Money originalValue = sapRequest.getPayment().getOriginalAmount();
            final Money requestValue = sapRequest.getValue().add(sapRequest.getAdvancement());
            final Money totalOtherValue = sapRequest.getPayment().getSapRequestSet().stream()
                    .filter(osr -> sapRequest != osr)
                    .filter(osr -> isPayment(osr))
                    .filter(osr -> !osr.getIgnore())
                    .map(osr -> osr.getValue().add(osr.getAdvancement()))
                    .reduce(Money.ZERO, Money::add);
            return requestValue.add(totalOtherValue).greaterOrEqualThan(originalValue) &&
                    sapRequest.getPayment().getSapRequestSet().stream()
                            .filter(sr -> sr != sapRequest)
                            .filter(sr -> isPayment(sr))
                            .filter(sr -> !sr.getIgnore())
                            .anyMatch(sr -> {
                                Money srValue = sr.getValue().add(sr.getAdvancement());
                                return srValue.equals(requestValue);
                            });
        }
        return false;
    }

    private void process(final Spreadsheet sheet, final Map<String, Money> valueMap, final SapRequest request) {
        final Row row = sheet.addRow();
        row.setCell("Document Number", request.getDocumentNumber());
        row.setCell("SAP Document Number", request.getSapDocumentNumber());
        final Money value = request.getValue().add(request.getAdvancement());
        row.setCell("Value", value.toPlainString());

        row.setCell("User", user(request));
        row.setCell("Event", request.getEvent().getDescription().toString());

        final JsonObject json = request.getRequestAsJson();

        final JsonObject paymentDocument = json.get("paymentDocument").getAsJsonObject();
        final String paymentDate = paymentDocument.get("paymentDate").getAsString().substring(0,10);
        row.setCell("paymentDate", paymentDate);
        //       row.setCell("paymentType", paymentDocument.get("paymentType").getAsString());
        final String whenSent = request.getWhenSent() != null ? request.getWhenSent().toString("yyyy-MM-dd") : "";
        row.setCell("sentDate", whenSent);
        final String registeredDate = request.getPayment().getWhenProcessed().toString("yyyy-MM-dd");
        row.setCell("registeredDate", registeredDate);
        final String status = paymentDocument.get("paymentStatus").getAsString();
        row.setCell("paymentStatus", status);
        final String paymentMechanism = paymentDocument.get("paymentMechanism").getAsString();
        row.setCell("paymentMechanism", paymentMechanism);
        final JsonElement paymentMethodReference = paymentDocument.get("paymentMethodReference");
        row.setCell("paymentMethodReference", paymentMethodReference.isJsonNull() ? "" : paymentMethodReference.getAsString());
        YearMonthDay sibsFileDate = getSIBSFileDate(request, paymentDocument);
        final String sibsDate  = sibsFileDate != null ? sibsFileDate.toString("yyyy-MM-dd") : "";
        row.setCell("sibsFileDate", sibsDate);
//        row.setCell("settlementType", paymentDocument.get("settlementType").getAsString());

        final JsonObject clientData = json.get("clientData").getAsJsonObject();
        row.setCell("accountId", clientData.get("accountId").getAsString());
        row.setCell("clientId", clientData.get("clientId").getAsString());
        row.setCell("companyName", clientData.get("companyName").getAsString());
        row.setCell("vatNumber", clientData.get("vatNumber").getAsString());

        row.setCell("academicYear", Utils.executionYearOf(request.getEvent()).getYear());
        row.setCell("eventId", request.getEvent().getExternalId());
        row.setCell("ValueForDebt", request.getValue().toPlainString());
        row.setCell("ValueAdvancement", request.getAdvancement().toPlainString());
        if (request.getAdvancement().isPositive()) {
            final Refund refund = request.getRefund();
            row.setCell("isRefunded", Boolean.toString(refund != null));
            if (refund == null) {
                row.setCell("refundDocuments", "");
            } else {
                final AccountingTransaction tx = refund.getAccountingTransaction();
                final String refundDocuments = tx.getSapRequestSet().stream()
                        .filter(sr -> sr != request)
                        .map(SapRequest::getDocumentNumber)
                        .distinct()
                        .collect(Collectors.joining(", "));
                row.setCell("refundDocuments", refundDocuments);
            }
        } else {
            row.setCell("isRefunded", "");
            row.setCell("refundDocuments", "");
        }

        if ("SI".equals(paymentMechanism)) {
            Money currentValue = valueMap.get(sibsDate);
            if (currentValue == null) {
                currentValue = Money.ZERO;
            }
            if ("A".equals(status)) {
                currentValue = currentValue.subtract(value);
            } else {
                currentValue = currentValue.add(value);
            }
            valueMap.put(sibsDate, currentValue);
        }
    }

    private boolean isPayment(final SapRequest request) {
        final SapRequestType type = request.getRequestType();
        return type == SapRequestType.PAYMENT || type == SapRequestType.PAYMENT_INTEREST
                || type == SapRequestType.ADVANCEMENT;
    }

    private String user(final SapRequest request) {
        final Event event = request.getEvent();
        final Person person = event.getPerson();
        return person == null ? " " : person.getUsername();
    }

    private YearMonthDay getSIBSFileDate(final SapRequest request, final JsonObject paymentDocument) {
        try {
            if ("SI".equals(paymentDocument.get("paymentMechanism").getAsString())
                    && "N".equals(paymentDocument.get("paymentStatus").getAsString())) {
                if (request.getPayment() != null && request.getPayment().getTransactionDetail() instanceof SibsTransactionDetail) {
                    SibsTransactionDetail sibsTransactionDetail = (SibsTransactionDetail) request.getPayment().getTransactionDetail();
                    SibsIncommingPaymentFileDetailLine sibsLine = sibsTransactionDetail.getSibsLine();
                    if (sibsLine != null) {
                        return sibsLine.getHeader().getWhenProcessedBySibs();
                    }
                }
                return null;
            } else {
                return null;
            }
        } catch (Exception e) {
            taskLog("#####%s - %s%n", request.getDocumentNumber(), request.getExternalId());
            throw e;
        }
    }
}