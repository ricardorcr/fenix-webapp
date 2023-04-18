package pt.ist.fenix.webapp;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.Exemption;
import org.fenixedu.academic.domain.accounting.calculator.CreditEntry;
import org.fenixedu.academic.domain.accounting.events.EventExemptionJustificationType;
import org.fenixedu.academic.domain.accounting.events.PenaltyExemption;
import org.fenixedu.academic.domain.accounting.events.gratuity.exemption.penalty.FixedAmountInterestExemption;
import org.fenixedu.academic.domain.accounting.events.gratuity.exemption.penalty.InstallmentPenaltyExemption;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.domain.SapRoot;
import pt.ist.fenixedu.giaf.invoices.SapEvent;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class CheckMissingDebtCredit extends CustomTask {

    public Predicate<SapRequest> yearsCondition;
    public Predicate<Exemption> exemptionPredicate;
    private LocalDate firstDay2023 = new LocalDate(2023,01,01);
    private LocalDate now = new LocalDate();

    @Override
    public void runTask() throws Exception {
        //TODO VER TODO MAIS ABAIXO FEITO PARA 2018!!!!
        Spreadsheet spreadsheet = new Spreadsheet("Documentos");
        yearsCondition = sr -> !sr.getRequest().contains("2017/2018"); //sr.getRequest().contains("2020/2021") || sr.getRequest().contains("2019/2020") || sr.getRequest().contains("2018/2019");
        exemptionPredicate = exemption -> exemption.getWhenCreated().getYear() < 2023;
        SapRoot.getInstance().getSapRequestSet().stream()
                .filter(sr -> sr.getRequestType() == SapRequestType.DEBT)
                .filter(sr -> !sr.isInitialization())
                .filter(sr -> sr.getIntegrated())
//                .filter(sr -> getStartDate(sr).getYear() > 2018)
//                .filter(sr -> yearsCondition.test(sr))
                .map(SapRequest::getEvent)
                .distinct()
                .forEach(event -> check(event, spreadsheet));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(baos);
        output("dividas_por_lancar.xls", baos.toByteArray());
    }

    private void check(final Event event, final Spreadsheet spreadsheet) {
        final Money exemptionAmount = event.getExemptionsSet().stream()
                .filter(exemption -> !(exemption instanceof FixedAmountInterestExemption))
                .filter(exemption -> !(exemption instanceof PenaltyExemption))
                .filter(exemption -> exemption.getExemptionJustification().getJustificationType() != EventExemptionJustificationType.CUSTOM_PAYMENT_PLAN)
                .filter(exemptionPredicate)
                .map(exemption -> exemption.getExemptionAmount(event.getOriginalAmountToPay()))
                .reduce(Money.ZERO, Money::add);

        final Money discountsAmount = event.getDiscountsSet().stream().map(d -> d.getAmount()).reduce(Money.ZERO, Money::add);
        final Money allExemptionsAmount = exemptionAmount.add(discountsAmount);

        if (allExemptionsAmount.isPositive()) {
            final BigDecimal debtExemptionAmount = event.getDebtInterestCalculator(firstDay2023.toDateTimeAtStartOfDay()).getDebtExemptionAmount();
            final Money debtExemptionValue = new Money(debtExemptionAmount);
            if (!hasRequestType(event, SapRequestType.DEBT_CREDIT)) {
                if (debtExemptionAmount.compareTo(allExemptionsAmount.getAmount()) != 0) {
                    taskLog("Look into: Isenção: %s\tCalculadora: %s\t%s%n", allExemptionsAmount.toString(), debtExemptionAmount.toBigInteger(), event.getExternalId());
                }
                report(event, spreadsheet, debtExemptionValue, Money.ZERO);
            } else {
                final Money creditDebt = event.getSapRequestSet().stream()
                        .filter(sr -> sr.getRequestType() == SapRequestType.DEBT_CREDIT)
                        .filter(sr -> !sr.getIgnore())
                        .filter(sr -> sr.getDocumentDate().getYear() < 2023)
                        .map(sr -> sr.getValue())
                        .reduce(Money.ZERO, Money::add);
//                final Money exemptionAmount = event.getExemptionsSet().stream()
//                        .filter(exemption -> !(exemption instanceof FixedAmountInterestExemption))
//                        .map(exemption -> exemption.getExemptionAmount(event.getOriginalAmountToPay()))
//                        .reduce(Money.ZERO, Money::add);
                if (creditDebt.compareTo(allExemptionsAmount) != 0) {
                    if (creditDebt.compareTo(debtExemptionValue) != 0) {
//                        taskLog("Falta só parte: %s\tvalor isenção:%s\tvalor abate:%s\tvalor evento:%s%n", event.getExternalId(), exemptionAmount.toString(), creditDebt.toString(), event.getOriginalAmountToPay().toString());
                        report(event, spreadsheet, allExemptionsAmount.subtract(creditDebt), creditDebt);
                    } else {
                        taskLog("Isenção não bate certo com NJ mas bate certo com calculadora: %s%n", event.getExternalId());
                    }
                }
            }
        }
    }

    private boolean hasRequestType(final Event event, final SapRequestType requestType) {
        return event.getSapRequestSet().stream()
                .filter(sr -> !sr.isInitialization())
                .anyMatch(sr -> sr.getRequestType() == requestType);
    }

    private void report(final Event event, final Spreadsheet spreadsheet, final Money debtCreditMissing, final Money creditDebtSent) {
        final Set<SapRequest> debtSet = event.getSapRequestSet().stream()
                .filter(sr -> sr.getRequestType() == SapRequestType.DEBT)
                .filter(sr -> !sr.isInitialization())
                .filter(sr -> sr.getIntegrated())
                .collect(Collectors.toSet());
        if (debtSet.size() > 1) {
            taskLog("hummmm: %s%n", event.getExternalId());
        } else {
            final Spreadsheet.Row row = spreadsheet.addRow();
            final SapRequest debtRequest = debtSet.iterator().next();
            final int debtYear = getStartDate(debtRequest).getYear();
            row.setCell("Doc. Dívida", debtRequest.getDocumentNumber());
            row.setCell("Valor Dívida", debtRequest.getValue().toString());
            row.setCell("Cliente", debtRequest.getClientData().getClientId());
            row.setCell("NIF", debtRequest.getClientData().getVatNumber());
            row.setCell("Data envio", debtRequest.getWhenSent().toString("dd/MM/yyyy HH:mm:ss"));
            row.setCell("Ano Dívida", debtYear);
            row.setCell("Abate por enviar", debtCreditMissing.toString());
            row.setCell("Abate enviado", creditDebtSent.toString());
            row.setCell("Evento", "https://fenix.tecnico.ulisboa.pt/sap-invoice-viewer/" + event.getExternalId());

            if (debtYear == 2018) {
                if (debtCreditMissing.isPositive()) {
//                    registerDebtCredit(event, debtCreditMissing);
                    taskLog("Request criado para Evento: %s\t%s%n", event.getExternalId(), debtCreditMissing.toString());
                } else {
                    taskLog("Valor enviado a mais criado para Evento: %s\t%s%n", event.getExternalId(), debtCreditMissing.toString());
                }
            } else {
                taskLog("Should not exist: %s%n", event.getExternalId());
            }
        }
    }

    private void registerDebtCredit(Event event, Money debtCreditMissing) {
        final SapEvent sapEvent = new SapEvent(event);
        sapEvent.registerDebtCredit(getCreditEntry(debtCreditMissing), event, true);
        event.getSapRequestSet().stream()
                .filter(sr -> sr.getRequestType() == SapRequestType.DEBT_CREDIT)
                .filter(sr -> sr.getWhenCreated().toLocalDate().isEqual(now))
                .forEach(sr -> {
                    final String newRequest = sr.getRequest().replace(now.toString("yyyy-MM-dd"), "2022-12-30");
                    sr.setRequest(newRequest);
                    sr.setSent(true); //TODO FEITO APENAS PARA 2018!!!
                    sr.setIntegrated(true);
                });
    }

    private LocalDate getStartDate(final SapRequest debt) {
        String metadata = debt.getRequestAsJson().get("workingDocument").getAsJsonObject().get("metadata").getAsString();
        String stripMetadata = metadata.replace("\\", "");
        JsonObject metadataJson = new JsonParser().parse(stripMetadata).getAsJsonObject();
        return LocalDate.parse(metadataJson.get("START_DATE").getAsString());
    }

    CreditEntry getCreditEntry(final Money creditAmount) {
        return new CreditEntry("", new DateTime(), new LocalDate(), "", creditAmount.getAmount()) {
            @Override
            public BigDecimal getUsedAmountInDebts() {
                return getAmount();
            }

            @Override
            public boolean isToApplyInterest() {
                return false;
            }

            @Override
            public boolean isToApplyFine() {
                return false;
            }

            @Override
            public boolean isForInterest() {
                return false;
            }

            @Override
            public boolean isForFine() {
                return false;
            }

            @Override
            public boolean isForDebt() {
                return false;
            }
        };
    }
}