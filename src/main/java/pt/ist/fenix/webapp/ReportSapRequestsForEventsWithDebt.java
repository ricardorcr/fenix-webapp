package pt.ist.fenix.webapp;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.events.gratuity.exemption.penalty.FixedAmountInterestExemption;
import org.fenixedu.academic.domain.phd.debts.ExternalScholarshipPhdGratuityContribuitionEvent;
import org.fenixedu.academic.domain.phd.debts.PhdGratuityEvent;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.domain.SapRoot;
import pt.ist.fenixedu.giaf.invoices.EventWrapper;
import pt.ist.fenixedu.giaf.invoices.Utils;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ReportSapRequestsForEventsWithDebt extends ReadCustomTask {

    private LocalDate currentDate = null;
    private Map<Event, Set<SapRequest>> usedDebtCreditMap = null;
    private Set<SapRequest> relevantRequests = new HashSet<>();

    @Override
    public void runTask() throws Exception {
        currentDate = new LocalDate();
        usedDebtCreditMap = new HashMap<>();
        final Money[] values = new Money[] {Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO};
        Spreadsheet spreadsheet = new Spreadsheet("Documentos");
        SapRoot.getInstance().getSapRequestSet().stream()
//                .filter(sr -> sr.getRequestType() == SapRequestType.DEBT)
                .filter(sr -> sr.getIntegrated())
                .filter(sr -> !sr.isInitialization())
                .filter(sr -> sr.getDocumentDate().getYear() == 2021)
//                .filter(sr -> sr.getRequest().contains("2021/2022"))
                .map(SapRequest::getEvent)
                .collect(Collectors.toSet())
                .forEach(event -> report(event, spreadsheet, values));

        taskLog("Valor Total Facturas: %s%n", values[0]);
        taskLog("Valor Total Isenções: %s%n", values[1]);
        taskLog("Valor Total Zaziamentos: %s%n", values[2]);
        taskLog("Valor Total Dívidas: %s%n", values[3]);
        taskLog("Valor Total Abate: %s%n", values[4]);
        taskLog("Valor Total Isenções Fora Prazo: %s%n", values[5]);
        taskLog("Valor Total Isenções Fora Prazo request: %s%n", values[6]);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(baos);
        output("documentos_2021.xlsx", baos.toByteArray());
    }

    private void report(final Event event, final Spreadsheet spreadsheet, Money[] values) {
        try {
            check(event, values);

            final Set<SapRequest> pendingCredits = new HashSet<>();
            event.getSapRequestSet().stream()
                    .filter(sr -> !sr.isInitialization())
                    .filter(sr -> sr.getIntegrated())
                    .filter(sr -> sr.getDocumentDate().getYear() == 2021)
//                    .filter(sr -> sr.getRequestType() != SapRequestType.CLOSE_INVOICE)
                    .forEach(sr -> reportRequest(sr, spreadsheet, pendingCredits));

            if (pendingCredits.size() > 1) {
                final Money value = pendingCredits.stream()
                        .map(SapRequest::getValue)
                        .reduce(Money.ZERO, Money::add);
                final Optional<SapRequest> creditDebt = getCreditDebt(event, value, usedDebtCreditMap.get(event));
                if (creditDebt.isPresent()) {
                    for (SapRequest pendingCredit : pendingCredits) {
                        addRequestToReport(pendingCredit, spreadsheet, "");
                    }
                } else {
                    for (SapRequest pendingCredit : pendingCredits) {
                        addRequestToReport(pendingCredit, spreadsheet, "Sim");
                    }
                }
            } else {
                for (SapRequest pendingCredit : pendingCredits) {
                    addRequestToReport(pendingCredit, spreadsheet, "Sim");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            taskLog("%s\t%s%n", event.getExternalId(), e.getMessage());
        }
    }

    private void reportRequest(SapRequest request, Spreadsheet spreadsheet, Set<SapRequest> pendingCredits) {
        final Event event = request.getEvent();
        String wasOutOfDate = "";
        final boolean hasDebt = event.getSapRequestSet().stream()
                .filter(sr -> !sr.isInitialization())
                .filter(sr -> sr.getIntegrated())
                .anyMatch(sr -> sr.getRequestType() == SapRequestType.DEBT);
        if (request.getRequestType() == SapRequestType.CREDIT && hasDebt) {
            if ((!Strings.isNullOrEmpty(request.getCreditId())) || request.getRefund() != null) {
                if (!isZaziamento(request)) {
                    final Optional<SapRequest> creditDebt = getCreditDebt(request, usedDebtCreditMap.get(event));
                    if (creditDebt.isPresent()) {
                        //nothing to do
                    } else {
                        if (request.getDocumentNumber().startsWith("NR") && hasAnotherInvoice(request)) {
                            //nothing to do
                        } else {
                            pendingCredits.add(request);
                            wasOutOfDate = "Sim";
                        }
                    }
                }
            }
        }

        if (request.getRequestType() == SapRequestType.CREDIT && hasDebt && !Strings.isNullOrEmpty(wasOutOfDate)) {
            return;
        }
        addRequestToReport(request, spreadsheet, wasOutOfDate);
    }

    private void addRequestToReport(SapRequest request, Spreadsheet spreadsheet, String wasOutOfDate) {
        Spreadsheet.Row row = spreadsheet.addRow();
        String documentNumber = request.getDocumentNumber();
        String sapDocumentNumber = request.getSapDocumentNumber();
        if (request.getRequestType() == SapRequestType.CREDIT && request.getDocumentNumber().startsWith("NR")) {
            documentNumber = getDocumentNumberForType(request, "NA");
            sapDocumentNumber = documentNumber;
        }
        row.setCell("Nº Documento", documentNumber);
        row.setCell("Nº Documento SAP", sapDocumentNumber);
        row.setCell("Valor", request.getValue().toString());
        row.setCell("Document Date", request.getRequestType() == SapRequestType.DEBT ?
                getDebtDate(request) : request.getDocumentDate().toString("yyyy-MM-dd"));
        row.setCell("Data Envio", request.getWhenSent().toString("yyyy-MM-dd"));
        row.setCell("Data Despacho", getDispatchDate(request));
        row.setCell("Referente a", refersTo(request));
        String zaziamento = "";
        if (isZaziamento(request)) {
            zaziamento = "Sim";
        }
        row.setCell("Zaziamento", request.getRequestType() != SapRequestType.CREDIT ? "" : zaziamento);
        row.setCell("Fora Prazo", wasOutOfDate);
        row.setCell("Double Check", relevantRequests.contains(request) ? "Sim" : "");
        row.setCell("ClientID", request.getClientId());
        row.setCell("IstID", request.getEvent().getPerson().getUsername());
        row.setCell("Nome", request.getEvent().getPerson().getName());
        row.setCell("Ignored", String.valueOf(request.getIgnore()));
        row.setCell("Evento", "https://fenix.tecnico.ulisboa.pt/sap-invoice-viewer/" + request.getEvent().getExternalId());
        row.setCell("EventID", request.getEvent().getExternalId());
    }

    private boolean isZaziamento(SapRequest request) {
        return request.getPayment() != null && request.getPayment().getPaymentMethod().getExternalId().equals("852714217013252");
    }

    private String getRefundWorkingAmount(final SapRequest request) {
        final JsonObject json = request.getRequestAsJson();
        final JsonElement workingDocument = json.get("workingDocument");
        if (workingDocument != null) {
            final JsonObject workingJson = workingDocument.getAsJsonObject();
            return workingJson.get("workingAmount").getAsString();
        } else {
            return "0.00";
        }
    }

    private String getRefundWorkingDocument(final SapRequest request) {
        final JsonObject json = request.getRequestAsJson();
        final JsonElement workingDocument = json.get("workingDocument");
        final JsonObject workingJson = workingDocument.getAsJsonObject();
        return workingJson.get("workingAmount").getAsString();
    }

    private String refersTo(final SapRequest request) {
        switch (request.getRequestType()) {
            case ADVANCEMENT:
            case CREDIT:
            case PAYMENT_INTEREST:
            case PAYMENT:
                return getDocumentNumberForType(request, "ND");
            case DEBT_CREDIT:
                return getDocumentNumberForType(request, "NG");
            case REIMBURSEMENT:
                return getDocumentNumberForType(request, "NA");
            case INVOICE:
            case INVOICE_INTEREST:
            case DEBT:
            case CLOSE_INVOICE:
            default: return null;
        }
    }

    private String getDebtDate(final SapRequest debtRequest) {
        final JsonObject workingDocument = debtRequest.getRequestAsJson().get("workingDocument").getAsJsonObject();
        final String metadataString = workingDocument.get("metadata").getAsString().replace("\\", "");
        final JsonObject metadata = new JsonParser().parse(metadataString).getAsJsonObject();
        return metadata.get("START_DATE").getAsString();
    }

    private String getDispatchDate(final SapRequest sapRequest) {
        if (sapRequest.getRequestType() != SapRequestType.DEBT_CREDIT) {
            return "";
        }
        final JsonObject workingDocument = sapRequest.getRequestAsJson().get("workingDocument").getAsJsonObject();
        final String metadataString = workingDocument.get("metadata").getAsString().replace("\\", "");
        final JsonObject metadata = new JsonParser().parse(metadataString).getAsJsonObject();
        final JsonElement despacho = metadata.get("Despacho");
        return despacho != null ? despacho.getAsString() : "";
    }


    public String getDocumentNumberForType(final SapRequest sapRequest, final String typeCode){
        final JsonObject json = sapRequest.getRequestAsJson();
        final JsonElement paymentDocument = json.get("paymentDocument");
        if (paymentDocument != null && !paymentDocument.isJsonNull()) {
            final JsonObject paymentJson = paymentDocument.getAsJsonObject();
            final String paymentDocumentNumber = getDocumentNumber(paymentJson, "paymentDocumentNumber", typeCode);
            if(paymentDocumentNumber != null) {
                return paymentDocumentNumber;
            }
            final String workingDocumentNumber = getDocumentNumber(paymentJson, "workingDocumentNumber", typeCode);
            if (workingDocumentNumber != null) {
                return workingDocumentNumber;
            }
            final String originatingOnDocumentNumber = getDocumentNumber(paymentJson, "originatingOnDocumentNumber", typeCode);
            if (originatingOnDocumentNumber != null) {
                return originatingOnDocumentNumber;
            }
            final String paymentOriginDocNumber = getDocumentNumber(paymentJson, "paymentOriginDocNumber", typeCode);
            if (paymentOriginDocNumber != null) {
                return paymentOriginDocNumber;
            }
        }
        final JsonElement workingDocument = json.get("workingDocument");
        if (workingDocument != null && !workingDocument.isJsonNull()) {
            final JsonObject workingJson = workingDocument.getAsJsonObject();
            final String paymentOriginDocNumber = getDocumentNumber(workingJson, "paymentOriginDocNumber", typeCode);
            if (paymentOriginDocNumber != null) {
                return paymentOriginDocNumber;
            }
            final String workingDocumentNumber = getDocumentNumber(workingJson, "workingDocumentNumber", typeCode);
            if (workingDocumentNumber != null) {
                return workingDocumentNumber;
            }
            final String paymentDocumentNumber = getDocumentNumber(workingJson, "paymentDocumentNumber", typeCode);
            if(paymentDocumentNumber != null) {
                return paymentDocumentNumber;
            }
            final String workOriginDocNumber = getDocumentNumber(workingJson, "workOriginDocNumber", typeCode);
            if (workOriginDocNumber != null) {
                return workOriginDocNumber;
            }
        }
        return null;
    }

    private String getDocumentNumber(final JsonObject json, final String key, final String value){
        final JsonElement jsonElement = json.get(key);
        if(jsonElement != null && !jsonElement.isJsonNull() && jsonElement.getAsString().startsWith(value)){
            return jsonElement.getAsString();
        }
        return null;
    }


    private void check(final Event event, Money[] values) {
        final Money invoiceAmount = event.getSapRequestSet().stream()
                .filter(sr -> !sr.isInitialization())
                .filter(sr -> sr.getIntegrated())
                .filter(sr -> sr.getRequestType() == SapRequestType.INVOICE)
                .filter(sr -> sr.getAnulledRequest() == null && sr.getOriginalRequest() == null)
                .filter(sr -> sr.getDocumentDate().getYear() == 2021)
                .map(sr -> sr.getValue())
                .reduce(Money.ZERO, Money::add);

        final Money creditAmount = event.getSapRequestSet().stream()
                .filter(sr -> !sr.isInitialization())
                .filter(sr -> sr.getIntegrated())
                .filter(sr -> sr.getRequestType() == SapRequestType.CREDIT)
                .filter(sr -> !isZaziamento(sr))
                .filter(sr -> !sr.getDocumentNumber().startsWith("NR"))
                .filter(sr -> sr.getDocumentDate().getYear() == 2021)
                .map(sr -> sr.getValue())
                .reduce(Money.ZERO, Money::add);

        final Money zaziamentoAmount = event.getSapRequestSet().stream()
                .filter(sr -> !sr.isInitialization())
                .filter(sr -> sr.getIntegrated())
                .filter(sr -> sr.getRequestType() == SapRequestType.CREDIT)
                .filter(sr -> isZaziamento(sr))
                .filter(sr -> !sr.getDocumentNumber().startsWith("NR"))
                .filter(sr -> sr.getDocumentDate().getYear() == 2021)
                .map(sr -> sr.getValue())
                .reduce(Money.ZERO, Money::add);

        final Money reimbursementCreditAmount = event.getSapRequestSet().stream()
                .filter(sr -> !sr.isInitialization())
                .filter(sr -> sr.getIntegrated())
                .filter(sr -> sr.getRequestType() == SapRequestType.REIMBURSEMENT)
                .filter(sr -> sr.getDocumentDate().getYear() == 2021)
                .map(sr -> new Money(getRefundWorkingAmount(sr)))
                .reduce(Money.ZERO, Money::add);

        final Money debtAmount = event.getSapRequestSet().stream()
                .filter(sr -> !sr.isInitialization())
                .filter(sr -> sr.getIntegrated())
                .filter(sr -> sr.getRequestType() == SapRequestType.DEBT)
                .filter(sr -> sr.getDocumentDate().getYear() == 2021)
                .map(sr -> sr.getValue())
                .reduce(Money.ZERO, Money::add);

        final Money debtCreditAmount = event.getSapRequestSet().stream()
                .filter(sr -> !sr.isInitialization())
                .filter(sr -> sr.getIntegrated())
                .filter(sr -> sr.getRequestType() == SapRequestType.DEBT_CREDIT)
                .filter(sr -> sr.getDocumentDate().getYear() == 2021)
                .map(sr -> sr.getValue())
                .reduce(Money.ZERO, Money::add);

        values[0] = values[0].add(invoiceAmount);
        values[1] = values[1].add(creditAmount.add(reimbursementCreditAmount));
        values[2] = values[2].add(zaziamentoAmount);
        values[3] = values[3].add(debtAmount);
        values[4] = values[4].add(debtCreditAmount);

        final Money debtDifference = debtAmount.subtract(debtCreditAmount);
//        if (debtDifference.isNegative()) {
//            taskLog("Diferença Dívida negativa: %s\t%s%n", event.getExternalId(), debtDifference);
//        }
        final Money invoiceDifference = invoiceAmount.subtract(creditAmount).subtract(reimbursementCreditAmount);
//        if (invoiceDifference.isNegative()) {
//            taskLog("Diferença Factura negativa: %s\t%s%n", event.getExternalId(), invoiceDifference);
//        }

        if (event.getSapRequestSet().stream()
                .filter(sr -> !sr.isInitialization())
                .filter(sr -> sr.getIntegrated())
                .anyMatch(sr -> sr.getRequestType() == SapRequestType.DEBT)) {
            Set<SapRequest> creditRequests = new HashSet<>();
            Money outOfDate = event.getSapRequestSet().stream()
                    .filter(sr -> !sr.isInitialization())
                    .filter(sr -> sr.getIntegrated())
                    .filter(sr -> sr.getRequestType() == SapRequestType.CREDIT)
                    .filter(sr -> sr.getDocumentDate().getYear() == 2021)
                    .filter(sr -> !Strings.isNullOrEmpty(sr.getCreditId()) || sr.getRefund() != null)
                    .filter(sr -> !isZaziamento(sr))
                    .filter(sr -> {
                        final Optional<SapRequest> creditDebt = getCreditDebt(sr, usedDebtCreditMap.get(event));
                        if (creditDebt.isPresent()) {
                            usedDebtCreditMap.computeIfAbsent(event, (v) -> new HashSet<SapRequest>()).add(creditDebt.get());
                            return false;
                        } else {
                            if (sr.getDocumentNumber().startsWith("NR") && hasAnotherInvoice(sr)) {
                                return false;
                            } else {
                                creditRequests.add(sr);
                                return true;
                            }
                        }
                    })
                    .map(sr -> sr.getValue())
                    .reduce(Money.ZERO, Money::add);
            if (creditRequests.size() > 1) {
                final Money value = creditRequests.stream()
                        .map(SapRequest::getValue)
                        .reduce(Money.ZERO, Money::add);
                final Optional<SapRequest> creditDebt = getCreditDebt(event, value, usedDebtCreditMap.get(event));
                if (creditDebt.isPresent()) {
                    outOfDate = outOfDate.subtract(value);
                }
            }
            values[6] = values[6].add(outOfDate);
            if (outOfDate.isPositive()) {
//                taskLog("%s\t%s%n", event.getExternalId(), outOfDate);
                relevantRequests.addAll(creditRequests);
            }

            final Money outOfDateValue = event.getExemptionsSet().stream()
                    .filter(ex -> ex.getWhenCreated().getYear() == 2021)
                    .filter(exemption -> !(exemption instanceof FixedAmountInterestExemption))
                    .filter(ex -> !isToProcessDebt(true, false, ex.getWhenCreated().plusDays(1), event))
                    .map(ex -> ex.getExemptionAmount(null))
                    .reduce(Money.ZERO, Money::add);

            values[5] = values[5].add(outOfDateValue);
//            if (outOfDateValue.isPositive()) {
//                taskLog("%s\t%s%n", event.getExternalId(), outOfDateValue);
//            }
        }
//        if (debtDifference.compareTo(invoiceDifference) != 0) {
//            if (debtDifference.subtract(outOfDateValue).compareTo(invoiceDifference) != 0) {
//                taskLog("Não batem certo: %s\tDívida: %s\tFacturas: %s%n", event.getExternalId(), debtDifference, invoiceDifference);
//            }
//
//        }
    }

    private boolean hasAnotherInvoice(final SapRequest request) {
        final String invoiceNumber = getDocumentNumberForType(request, "ND");
        return request.getEvent().getSapRequestSet().stream()
                .filter(sr -> !sr.isInitialization())
                .filter(sr -> sr.getRequestType() == SapRequestType.INVOICE)
                .filter(sr -> !invoiceNumber.equals(sr.getDocumentNumber()))
                .filter(sr -> sr.getDocumentDate().getYear() == 2021)
                .anyMatch(sr -> sr.getValue().compareTo(request.getValue()) == 0);
    }

    private Optional<SapRequest> getCreditDebt(final SapRequest creditRequest, final Set<SapRequest> usedDebtCredits) {
        return creditRequest.getEvent().getSapRequestSet().stream()
                .filter(sr -> sr.getRequestType() == SapRequestType.DEBT_CREDIT)
                .filter(sr -> !sr.isInitialization())
                .filter(sr -> sr.getValue().compareTo(creditRequest.getValue()) == 0)
                .filter(sr -> sr.getDocumentDate().getYear() == 2021)
                .filter(sr -> usedDebtCredits == null || !usedDebtCredits.stream().anyMatch(dc -> dc == sr))
                .findAny();
    }

    private Optional<SapRequest> getCreditDebt(final Event event, final Money creditValue, final Set<SapRequest> usedDebtCredits) {
        return event.getSapRequestSet().stream()
                .filter(sr -> sr.getRequestType() == SapRequestType.DEBT_CREDIT)
                .filter(sr -> !sr.isInitialization())
                .filter(sr -> sr.getValue().compareTo(creditValue) == 0)
                .filter(sr -> sr.getDocumentDate().getYear() == 2021)
                .filter(sr -> usedDebtCredits == null || !usedDebtCredits.stream().anyMatch(dc -> dc == sr))
                .findAny();
    }

    private boolean isToProcessDebt(boolean isGratuity, final boolean isNewDate, final DateTime documentDate, final Event event) {
        return (isGratuity || event instanceof ExternalScholarshipPhdGratuityContribuitionEvent)
                && event.getWhenOccured().isAfter(EventWrapper.LIMIT)
                && isNotPastDebtEndDate(isNewDate, documentDate, event);
    }

    private boolean isNotPastDebtEndDate(final boolean isNewDate, final DateTime documentDate, final Event event) {
        return getDebtInterval(documentDate, isNewDate, event) != null;
    }

    private LocalDate[] getDebtInterval(final DateTime documentDate, final boolean isNewDate, final Event event) {
        LocalDate startDate = isNewDate ? currentDate : documentDate.toLocalDate();
        final LocalDate endDate;
        if (event instanceof PhdGratuityEvent) {
            PhdGratuityEvent phdEvent = (PhdGratuityEvent) event;
            final LocalDate localDate = phdEvent.getPhdGratuityDate().getYear() == phdEvent.getYear() ?
                    phdEvent.getPhdGratuityDate().toLocalDate() : phdEvent.getWhenOccured().toLocalDate();
            endDate = localDate.plusYears(1);
        } else {
            final ExecutionYear executionYear = Utils.executionYearOf(event);
            if (startDate.isBefore(executionYear.getBeginLocalDate())) {
                startDate = executionYear.getBeginLocalDate();
            }
            endDate = executionYear.getEndDateYearMonthDay().toLocalDate();
        }
        return startDate.isAfter(endDate) ? null : new LocalDate[]{startDate, endDate};
    }
}