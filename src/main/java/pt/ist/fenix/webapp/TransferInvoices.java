package pt.ist.fenix.webapp;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;

import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.calculator.CreditEntry;
import org.fenixedu.academic.util.Money;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.giaf.invoices.DebtCycleType;
import pt.ist.fenixedu.giaf.invoices.ErrorLogConsumer;
import pt.ist.fenixedu.giaf.invoices.EventLogger;
import pt.ist.fenixedu.giaf.invoices.EventWrapper;
import pt.ist.fenixedu.giaf.invoices.SapEvent;
import pt.ist.fenixedu.giaf.invoices.Utils;
import pt.ist.fenixedu.giaf.invoices.task.SapCustomTask;
import pt.ist.fenixframework.FenixFramework;

public class TransferInvoices extends SapCustomTask {

    private final String FILENAME = "/afs/ist.utl.pt/ciist/fenix/fenix036/InvoicesToTransfer.csv";
    //private final String FILENAME = "/tmp/sap/InvoicesToTransfer.csv";

    private class Entry {

        private final String[] parts;

        private Entry(final String line) {
            parts = line.split("\t");
        }

        private Event event() {
            return FenixFramework.getDomainObject(parts[0]);
        }

        private Money value() {
            return new Money(parts[1].replace(",", ""));
        }

        private String clientId() {
            return parts[2];
        }

        private String vatNumber() {
            return parts[3];
        }

        private String fiscalCountry() {
            return parts[4];
        }

        private String companyName() {
            return parts[5];
        }

        private String country() {
            return parts[6];
        }

        private String street() {
            return parts[7];
        }

        private String city() {
            return parts[8];
        }

        private String region() {
            return parts[9];
        }

        private String postalCode() {
            return parts[10];
        }

        private String nationality() {
            return parts[11];
        }

    }

    @Override
    protected void runTask(ErrorLogConsumer consumer, EventLogger logger) {
        try {
            Files.readAllLines(new File(FILENAME).toPath()).stream().map(l -> new Entry(l))
                    .forEach(e -> transfer(e, consumer, logger));
        } catch (final IOException e) {
            throw new Error(e);
        }
        throw new Error("Dry run.");
    }

    private void transfer(final Entry e, final ErrorLogConsumer consumer, final EventLogger logger) {
        final Event event = e.event();
        if (event == null || !FenixFramework.isDomainObjectValid(event)) {
            taskLog("No event found for: %s%n", e.parts[0]);
            return;
        }
        if (event.isCancelled()) {
            taskLog("%nProcessing event: %s %s %s %s%n", e.parts[0], event.getPerson().getUsername(),
                    event.getDescription().toString(), e.value().toPlainString());
            taskLog("   Event is canceled");
            event.getSapRequestSet().forEach(r -> taskLog("   %s %s %s%n", r.getDocumentNumber(), r.getRequestType().name(),
                    r.getValue().toPlainString()));
        } else if (payedFullValueLastYear(e, event, consumer, logger)) {
            taskLog("%nProcessing event: %s %s %s %s%n", e.parts[0], event.getPerson().getUsername(),
                    event.getDescription().toString(), e.value().toPlainString());
            taskLog("   Payed full value last year");
            event.getSapRequestSet().forEach(r -> taskLog("   %s %s %s%n", r.getDocumentNumber(), r.getRequestType().name(),
                    r.getValue().toPlainString()));
        } else {
            taskLog();
            taskLog();
            final SapEvent sapEvent = new SapEvent(event);
            final SapRequest invoiceToTransfer = event.getSapRequestSet().stream().filter(r -> r.getRequest().length() > 2)
                    .filter(r -> r.getRequestType() == SapRequestType.INVOICE).findAny().orElse(null);
            if (invoiceToTransfer == null) {
                taskLog("Processing event: %s %s %s %s%n", e.parts[0], event.getPerson().getUsername(),
                        event.getDescription().toString(), e.value().toPlainString());
                taskLog("   No proper invoice to transfer");
                event.getSapRequestSet().forEach(r -> taskLog("   %s %s %s%n", r.getDocumentNumber(), r.getRequestType().name(),
                        r.getValue().toPlainString()));
            } else if (invoiceToTransfer.getValue().lessThan(e.value())) {
                taskLog("Processing event: %s %s %s %s%n", e.parts[0], event.getPerson().getUsername(),
                        event.getDescription().toString(), e.value().toPlainString());
                taskLog("   Invoice to transfer does not have enough value.");
                event.getSapRequestSet().forEach(r -> taskLog("   %s %s %s%n", r.getDocumentNumber(), r.getRequestType().name(),
                        r.getValue().toPlainString()));
            } else {
//                taskLog("Ok Processing event: %s %s %s %s%n", e.parts[0], event.getPerson().getUsername(), event.getDescription().toString(), e.value().toPlainString());

            }
        }
    }

    private boolean payedFullValueLastYear(final Entry e, final Event event, final ErrorLogConsumer consumer,
            final EventLogger logger) {
        processSap(consumer, logger, event);
        final boolean valueMatch = event.getSapRequestSet().stream().filter(r -> r.getRequest().length() == 2)
                .filter(r -> r.getRequestType() == SapRequestType.PAYMENT || r.getRequestType() == SapRequestType.CREDIT)
                .map(r -> r.getValue()).reduce(Money.ZERO, Money::add).equals(e.value());
        return valueMatch && event.getSapRequestSet().stream().noneMatch(r -> r.getRequest().length() > 2);
    }

    private static void processSap(final ErrorLogConsumer errorLog, final EventLogger elogger, final Event event) {
        try {
            if (EventWrapper.needsProcessingSap(event)) {
                final SapEvent sapEvent = new SapEvent(event);
                if (sapEvent.hasPendingDocumentCancelations()) {
                    return;
                }

                final EventWrapper eventWrapper = new EventWrapper(event, errorLog, true);

                sapEvent.updateInvoiceWithNewClientData();

                final Money debtFenix = eventWrapper.debt;
                final Money invoiceSap = sapEvent.getInvoiceAmount();

                if (debtFenix.isPositive()) {
                    if (invoiceSap.isZero()) {
                        sapEvent.registerInvoice(debtFenix, event, eventWrapper.isGratuity(), false);
                    } else if (invoiceSap.isNegative()) {
                        logError(event, errorLog, elogger, "A dívida no SAP é negativa");
                    } else if (!debtFenix.equals(invoiceSap)) {
                        logError(event, errorLog, elogger, "A dívida no SAP é: " + invoiceSap.getAmountAsString()
                                + " e no Fénix é: " + debtFenix.getAmountAsString());
                        if (debtFenix.greaterThan(invoiceSap)) {
                            // criar invoice com a diferença entre debtFenix e invoiceDebtSap (se for propina aumentar a dívida no sap)
                            // passar data actual (o valor do evento mudou, não dá para saber quando, vamos assumir que mudou quando foi detectada essa diferença)
                            logError(event, errorLog, elogger, "A dívida no Fénix é superior à dívida registada no SAP");
                            sapEvent.registerInvoice(debtFenix.subtract(invoiceSap), eventWrapper.event,
                                    eventWrapper.isGratuity(), true);
                        } else {
                            // diminuir divida no sap e registar credit note da diferença na última factura existente
                            logError(event, errorLog, elogger, "A dívida no SAP é superior à dívida registada no Fénix");
                            CreditEntry creditEntry = getCreditEntry(invoiceSap.subtract(debtFenix));
//                            sapEvent.registerCredit(eventWrapper.event, creditEntry, eventWrapper.isGratuity());
                        }
                    }
                }
            } else {
                //processing payments of past events
//                eventWrapper.paymentsSap().filter(d -> !sapEvent.hasPayment(d)).peek(
//                    d -> elogger.log("Processing past payment %s : %s%n", eventWrapper.event.getExternalId(), d.getExternalId()))
//                    .forEach(d -> sapEvent.registerInvoiceAndPayment(clientMap, d, errorLog, elogger));
            }
        } catch (final Exception e) {
            logError(errorLog, elogger, event, e);
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
        e.printStackTrace();
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