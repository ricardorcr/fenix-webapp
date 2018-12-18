package pt.ist.fenix.webapp;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.calculator.CreditEntry;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.commons.spreadsheet.Spreadsheet.Row;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import pt.ist.fenixedu.giaf.invoices.DebtCycleType;
import pt.ist.fenixedu.giaf.invoices.ErrorLogConsumer;
import pt.ist.fenixedu.giaf.invoices.EventLogger;
import pt.ist.fenixedu.giaf.invoices.EventWrapper;
import pt.ist.fenixedu.giaf.invoices.SapEvent;
import pt.ist.fenixedu.giaf.invoices.Utils;
import pt.ist.fenixframework.FenixFramework;

public class RegisterInvoiceToSap extends CustomTask{

    @Override
    public void runTask() throws Exception {
        final Spreadsheet errors = new Spreadsheet("Errors");
        final ErrorLogConsumer errorLogConsumer = new ErrorLogConsumer() {

            @Override
            public void accept(final String oid, final String user, final String name, final String amount,
                    final String cycleType, final String error, final String args, final String type,
                    final String countryOfVatNumber, final String vatNumber, final String address, final String locality,
                    final String postCode, final String countryOfAddress, final String paymentMethod, final String documentNumber,
                    final String action) {

                final Row row = errors.addRow();
                row.setCell("OID", oid);
                row.setCell("user", user);
                row.setCell("name", name);
                row.setCell("amount", amount);
                row.setCell("cycleType", cycleType);
                row.setCell("error", error);
                row.setCell("args", args);
                row.setCell("type", type);
                row.setCell("countryOfVatNumber", countryOfVatNumber);
                row.setCell("vatNumber", vatNumber);
                row.setCell("address", address);
                row.setCell("locality", locality);
                row.setCell("postCode", postCode);
                row.setCell("countryOfAddress", countryOfAddress);
                row.setCell("paymentMethod", paymentMethod);
                row.setCell("documentNumber", documentNumber);
                row.setCell("action", action);

            }
        };
        final EventLogger elogger = (msg, args) -> taskLog(msg, args);

        getEventStream().forEach(event -> {
            try {
                registerInvoice(event, errorLogConsumer, elogger);
            } catch (Exception e) {
                logError(errorLogConsumer, elogger, event, e);
                e.printStackTrace();
            }
        });

        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        errors.exportToCSV(stream, "\t");
        output("Erros_registo_factura.xls", stream.toByteArray());
    }

    private Stream<Event> getEventStream() {
//        return Stream.of("1976574014390730"/*, "1976290546548750" este ID NAO EXISTE!!*/, "1132149084258850", "1132578580988150",
//                "281633890503501",
//                "1413624060969210", "1414053557698770", "281633890503405"/*, "1132149084258920" este ID NAO EXISTE!!*/,
//                "1132149084258820",
//                "1976574014390670", "286405599166693", "281633890503581", "1695099037680070", "1695528534409590",
//                "281633890503170", "569199130837922", "569199130837908", "1132149084258830", "1132578580988130",
//                "281633890503110", "1412850966855720", "1976574014390730", "1693780482719860", "1689008774054880",
//                "1976574014390550", "287724154127036", "288153650856321", "1976574014390690", "1413624060969160",
//                "1132149084258780", "1132578580988090", "281633890502798", "1976574014390500", "850674107548246",
//                "1976574014390690", "850674107548192", "568915662995484", "568915662995490", "1413624060969260",
//                "1975255459430550", "1970483750764800", "1413624060969270", "1975255459430580", "1970483750764840",
//                "569199130837953", "1412305506009220"/*, "1407533797343410" este ID NAO EXISTE!!*/, "568915662995492",
//                "850390639706117",
//                "567163316338788"/*, "288153650856434" estes IDs só exitem em produção!!, "281633890503620"*/, "567163316338789",
//                /*"288153650856436", -> só existe em produção! "281633890503623",*/
//                "567163316338790"/*, "288153650856437", "281633890503624" só em produção!*/,
//                "567163316338791"/*, "288153650856435", "281633890503621"só em produção!*/,
//                "567163316338792"/*, "288153650856438", "281633890503625" só em produção!*/, "850674107548230", "851103604277552",
//                "281633890503056",
//                "850674107548238", "850674107548248", "849355552587971", "844583843922190", "567528388559227", "1976574014390410",
//                "1977003511120010", "1689008774054470", "281633890503286", "1131375990145080", "850674107548228",
//                "851103604277550", "281633890503535", "1132149084258880", "1132578580988180", "281633890503430",
//                "1132149084258780", "849003365269708", "1413624060969230", "569628627567148", "281633890502741",
//                "1413624060969160", "1414053557698740", "281633890502826", "849901013434472", "287724154127134",
//                "288153650856419", "281633890503301", "1414053557698880", "1413503801884710", "1131375990145090",
//                "1976574014390730", "1977003511120270", "1407533797343490", "1130478341980400", "849901013434477",
//                /*"1976574014390720" NAO EXISTE!,*/ "849355552587979", "844583843922197", "287724154127086", "1975800920277080",
//                "1975800920277080").map(s -> (Event) FenixFramework.getDomainObject(s))
//                .filter(e -> e.getWhenOccured().getYear() == 2018);

        return Stream.of("567163316338788").map(s -> (Event) FenixFramework.getDomainObject(s))
                .filter(e -> e.getWhenOccured().getYear() == 2018);
    }

    private void registerInvoice(Event event, ErrorLogConsumer errorLogConsumer, EventLogger elogger) throws Exception {
        final EventWrapper eventWrapper = new EventWrapper(event, errorLogConsumer, true);
        final SapEvent sapEvent = new SapEvent(event);

        if (!event.getSapRequestSet().isEmpty()) {
            throw new Exception("There should not be anything registered for this event");
        }

        //this are new debts, there shouldn't be anything registered yet
//        sapEvent.updateInvoiceWithNewClientData();

        final Money debtFenix = eventWrapper.debt;
        final Money invoiceSap = sapEvent.getInvoiceAmount();

        if (debtFenix.isPositive()) {
            if (invoiceSap.isZero()) {
                sapEvent.registerInvoice(debtFenix, event, eventWrapper.isGratuity(), false);
            } else if (invoiceSap.isNegative()) {
                logError(event, errorLogConsumer, elogger, "A dívida no SAP é negativa");
            } else if (!debtFenix.equals(invoiceSap)) {
                logError(event, errorLogConsumer, elogger, "A dívida no SAP é: " + invoiceSap.getAmountAsString()
                        + " e no Fénix é: " + debtFenix.getAmountAsString());
                if (debtFenix.greaterThan(invoiceSap)) {
                    // criar invoice com a diferença entre debtFenix e invoiceDebtSap (se for propina aumentar a dívida no sap)
                    // passar data actual (o valor do evento mudou, não dá para saber quando, vamos assumir que mudou quando foi detectada essa diferença)
                    logError(event, errorLogConsumer, elogger, "A dívida no Fénix é superior à dívida registada no SAP");
//                    sapEvent.registerInvoice(debtFenix.subtract(invoiceSap), eventWrapper.event,
//                            eventWrapper.isGratuity(), true, errorLogConsumer, elogger);
                } else {
                    // diminuir divida no sap e registar credit note da diferença na última factura existente
                    logError(event, errorLogConsumer, elogger, "A dívida no SAP é superior à dívida registada no Fénix");
//                    CreditEntry creditEntry = getCreditEntry(invoiceSap.subtract(debtFenix));
//                    sapEvent.registerCredit(eventWrapper.event, creditEntry, eventWrapper.isGratuity(), errorLogConsumer,
//                            elogger);
                }
            }
        } else {
            logError(event, errorLogConsumer, elogger, "A dívida no Fénix não é positiva: " + debtFenix.getAmountAsString());
        }
    }

    private static void logError(final ErrorLogConsumer errorLog, final EventLogger elogger, final Event event,
            final Exception e) {
        final String errorMessage = e.getMessage();

        BigDecimal amount;
        DebtCycleType cycleType;

        try {
            amount = event.getOriginalAmountToPay().getAmount();
            cycleType = Utils.cycleType(event);
        } catch (Exception ex) {
            amount = null;
            cycleType = null;
        }

        errorLog.accept(event.getExternalId(), Utils.getUserIdentifier(event.getParty()), event.getParty().getName(),
                amount == null ? "" : amount.toPlainString(), cycleType == null ? "" : cycleType.getDescription(), errorMessage,
                "", "", "", "", "", "", "", "", "", "", "");
        elogger.log("%s: %s%n", event.getExternalId(), errorMessage);
        elogger.log(
                "Unhandled SAP error for event " + event.getExternalId() + " : " + e.getClass().getName() + " - " + errorMessage);
    }

    private static void logError(Event event, ErrorLogConsumer errorLog, EventLogger elogger, String errorMessage) {
        BigDecimal amount;
        DebtCycleType cycleType;
        try {
            amount = event.getOriginalAmountToPay().getAmount();
            cycleType = Utils.cycleType(event);
        } catch (Exception ex) {
            amount = null;
            cycleType = null;
        }

        errorLog.accept(event.getExternalId(), Utils.getUserIdentifier(event.getParty()), event.getParty().getName(),
                amount == null ? "" : amount.toPlainString(), cycleType == null ? "" : cycleType.getDescription(), errorMessage,
                "", "", "", "", "", "", "", "", "", "", "");
        elogger.log("%s: %s %s %s %n", event.getExternalId(), errorMessage, "", "");
    }

    static CreditEntry getCreditEntry(final Money creditAmount) {
        return new CreditEntry("", new DateTime(), new LocalDate(), "", creditAmount.getAmount()) {
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