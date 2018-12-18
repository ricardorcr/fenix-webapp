package pt.ist.fenix.webapp;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.Year;
import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.Country;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.calculator.CreditEntry;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.core.domain.Bennu;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import com.google.common.base.Strings;

import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.giaf.invoices.ClientMap;
import pt.ist.fenixedu.giaf.invoices.DebtCycleType;
import pt.ist.fenixedu.giaf.invoices.ErrorLogConsumer;
import pt.ist.fenixedu.giaf.invoices.EventLogger;
import pt.ist.fenixedu.giaf.invoices.EventWrapper;
import pt.ist.fenixedu.giaf.invoices.SapEvent;
import pt.ist.fenixedu.giaf.invoices.Utils;
import pt.ist.fenixedu.giaf.invoices.task.SapCustomTask;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;

public class CalculateNewSapInvoices extends SapCustomTask {

    private final String FILENAME = "/afs/ist.utl.pt/ciist/fenix/fenix036/InvoicesToTransfer.csv";
    //private final String FILENAME = "/tmp/sap/InvoicesToTransfer.csv";
    private final int currentYear = Year.now().getValue();

    @Override
    public TxMode getTxMode() {
        return TxMode.READ;
    }

    @Override
    protected void runTask(ErrorLogConsumer consumer, EventLogger logger) {
        if (EventWrapper.SAP_THRESHOLD == null) {
            throw new Error();
        }
        try {
            final Set<String> eventIds = Files.readAllLines(new File(FILENAME).toPath()).stream().map(l -> l.split("\t")[0])
                    .collect(Collectors.toSet());
            Bennu.getInstance().getAccountingEventsSet().stream()
                .parallel()
                .filter(e -> !eventIds.contains(e.getExternalId()))
                .forEach(e -> processSapTx(consumer, logger, e));
        } catch (final IOException e) {
            throw new Error(e);
        }
    }

    public boolean shouldProcess(final ErrorLogConsumer consumer, final EventLogger logger, final Event event) {
        return event.getWhenOccured().getYear() >= currentYear
                && event.getSapRequestSet().stream().allMatch(r -> r.getIntegrated())
                && (EventWrapper.needsProcessingSap(event)
                        || event.getAccountingTransactionsSet().stream()
                                .anyMatch(tx -> tx.getWhenRegistered().getYear() == currentYear))
                && Utils.validate(consumer, event)
                && hasValidCountryCode(consumer, logger, event);
    }

    private boolean hasValidCountryCode(final ErrorLogConsumer consumer, EventLogger logger, final Event event) {
        final String clientId = ClientMap.uVATNumberFor(event.getParty());
        final boolean result = clientId != null && clientId.length() > 2 && Country.readByTwoLetterCode(clientId.substring(0, 2)) != null;
        if (!result) {
            logError(event, consumer, logger, "No valid country code for: " + clientId);
        }
        return result;
    }

    private void processSapTx(final ErrorLogConsumer consumer, final EventLogger logger, final Event event) {
        FenixFramework.atomic(() -> {
            processSap(consumer, logger, event);
        });
    }

    private void processSap(final ErrorLogConsumer errorLog, final EventLogger elogger, final Event event) {
        if (!shouldProcess(errorLog, elogger, event)) {
            return;
        }

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
        } catch (final Error er) {
            logError(errorLog, elogger, event, new Exception(er.getMessage()));
        }
    }

    private SapRequest getSentInvoice(Event event) {
        Set<SapRequest> invoices =
                event.getSapRequestSet().stream().filter(sr -> !sr.getIgnore()).filter(sr -> sr.getRequest().length() > 2)
                .filter(sr -> sr.getRequestType().equals(SapRequestType.INVOICE)).collect(Collectors.toSet());
        if (invoices.isEmpty()) {
            throw new Error(
                    "Não existe nenhuma factura enviada - " + event.getExternalId() + " - " + event.getPerson().getUsername());
        }
        if (invoices.size() > 1) {
            throw new Error("It has more than one!! " + event.getExternalId() + " - " + event.getPerson().getUsername());
        }
        return invoices.iterator().next();
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
                amount == null ? "" : amount.toPlainString(), cycleType == null ? "" : cycleType.getDescription(),
                Strings.isNullOrEmpty(errorMessage) ? e.getClass().getName() : errorMessage,
                "", "", "", "", "", "", "", "", "", "", "");
        elogger.log(
                "Unhandled error for event " + event.getExternalId() + " : " + e.getClass().getName() + " - " + errorMessage);
//        e.printStackTrace();
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