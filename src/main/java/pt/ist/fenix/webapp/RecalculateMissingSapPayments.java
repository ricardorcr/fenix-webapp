package pt.ist.fenix.webapp;

import com.google.common.base.Strings;
import org.fenixedu.academic.domain.accounting.AccountingTransaction;
import org.fenixedu.academic.domain.accounting.AccountingTransactionDetail;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.ResidenceEvent;
import org.fenixedu.academic.domain.accounting.calculator.AccountingEntry;
import org.fenixedu.academic.domain.accounting.calculator.CreditEntry;
import org.fenixedu.academic.domain.accounting.calculator.DebtExemption;
import org.fenixedu.academic.domain.accounting.calculator.DebtInterestCalculator;
import org.fenixedu.academic.domain.accounting.calculator.ExcessRefund;
import org.fenixedu.academic.domain.accounting.calculator.Payment;
import org.fenixedu.academic.domain.accounting.calculator.Refund;
import org.fenixedu.academic.domain.accounting.events.EventExemptionJustificationType;
import org.fenixedu.academic.util.Money;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import pt.ist.esw.advice.pt.ist.fenixframework.AtomicInstance;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.giaf.invoices.ErrorLogConsumer;
import pt.ist.fenixedu.giaf.invoices.EventLogger;
import pt.ist.fenixedu.giaf.invoices.EventWrapper;
import pt.ist.fenixedu.giaf.invoices.SapEvent;
import pt.ist.fenixedu.giaf.invoices.Utils;
import pt.ist.fenixedu.giaf.invoices.task.SapCustomTask;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.CallableWithoutException;
import pt.ist.fenixframework.FenixFramework;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class RecalculateMissingSapPayments extends SapCustomTask {

    @Override
    public Atomic.TxMode getTxMode() {
        return Atomic.TxMode.READ;
    }

    @Override
    protected void runTask(ErrorLogConsumer errorLogConsumer, EventLogger elogger) {
//        Bennu.getInstance().getAccountingEventsSet().stream().parallel()
//                .forEach(e -> registerEventSapRequests(errorLogConsumer, elogger, e, false));


        final Event event = FenixFramework.getDomainObject("1688875630069298");
        registerEventSapRequests(errorLogConsumer, elogger, event, false);
    }

    public void registerEventSapRequests(ErrorLogConsumer consumer, EventLogger elogger, Event event, boolean offsetPayments) {
        registerEventSapRequests(consumer, elogger, () -> event, offsetPayments);
    }

    public void registerEventSapRequests(final ErrorLogConsumer consumer, final EventLogger elogger, final Supplier<Event> event, final boolean offsetPayments) {
        try {
            FenixFramework.getTransactionManager().withTransaction(new CallableWithoutException<Void>() {
                public Void call() {
                    hack(consumer, elogger, (Event)event.get(), offsetPayments);
                    return null;
                }
            }, new AtomicInstance(Atomic.TxMode.SPECULATIVE_READ, false));
        } catch (Throwable var5) {
            taskLog("Error: " + var5.getMessage());
        }

    }

    private void hack(ErrorLogConsumer errorLog, EventLogger elogger, Event event, boolean offsetPayments) {
        event.getAccountingTransactionsSet().stream().map(tx -> tx.getTransactionDetail())
                .filter(std -> std.getTransaction().getAdjustmentTransactionsSet().isEmpty())
                .filter(std -> std.getTransaction().getTransactionDetail().getWhenProcessed().getYear() >= 2018)
                .filter(std -> !(std.getEvent() instanceof ResidenceEvent))
                .forEach(std -> {
                    try {
                        final Money txAmount = std.getTransaction().getOriginalAmount();
                        final int year = std.getWhenRegistered().getYear();
                        final int month = std.getWhenRegistered().getMonthOfYear();
                        final int day = std.getWhenRegistered().getDayOfMonth();

                        final Money sapValue = std.getTransaction().getSapRequestSet().stream()
                                .filter(sr -> !sr.getIgnore())
//                              .filter(sr -> !sr.isInitialization())
                                .filter(sr -> sr.getOriginalRequest() == null)
                                .filter(sr -> !isInvoice(sr))
                                .filter(sr -> sr.getAdvancementRequest() == null || sr.getAdvancementRequest().getEvent() != std.getEvent())
                                .map(sr -> sr.getValue().add(sr.getAdvancement()))
                                .reduce(Money.ZERO, Money::add);

                        if (!sapValue.equals(txAmount)) {
                                taskLog("%s-%s-%s : %s %s : %s != %s%n", year, month, day, std.getEvent().getExternalId(), std.getExternalId(), txAmount.toPlainString(), sapValue.toPlainString());

                            fixX(std, errorLog, elogger);
                        }
                    } catch (final Throwable t) {
                        taskLog("Failed to process %s %s %s%n", t.getMessage(), std.getEvent().getExternalId(), std.getExternalId());
                    }
                });
    }

    private void fixX(AccountingTransactionDetail std, ErrorLogConsumer errorLogConsumer, EventLogger eventLogger) {
        try {
            FenixFramework.atomic(() -> fix(std, errorLogConsumer, eventLogger));
        } catch (Throwable t) {
            taskLog("Failed to process %s %s %s%n", t.getMessage(), std.getEvent().getExternalId(), std.getExternalId());
        }
    }

    private void fix(final AccountingTransactionDetail txd, ErrorLogConsumer errorLogConsumer, EventLogger eventLogger) {
        final LocalDate today = new LocalDate();
        final Event event = txd.getEvent();
        final AccountingTransaction tx = txd.getTransaction();

        final Set<SapRequest> existingRequests = new HashSet<>(tx.getSapRequestSet());

        if (event.getSapRequestSet().stream().anyMatch(sr -> sr.getSent() && !sr.getIntegrated())) {
            return;
        }

        if (event.getSapRequestSet().stream().anyMatch(sr -> !sr.getIntegrated() && sr.getOriginalRequest() != null)) {
            return;
        }

        if  (tx.getSapRequestSet().stream()
                .filter(sr -> !sr.getIgnore())
                .filter(sr -> sr.getRequestType() == SapRequestType.INVOICE_INTEREST)
                .count() > 1) {
            throw new Error("Unexpected number of invoice interest for " + event.getExternalId() + " " + tx.getExternalId());
        }

        if  (tx.getSapRequestSet().stream()
                .filter(sr -> !sr.getIgnore())
                .filter(sr -> sr.getRequestType() == SapRequestType.PAYMENT_INTEREST)
                .count() > 1) {
            throw new Error("Unexpected number of payment interest for " + event.getExternalId() + " " + tx.getExternalId());
        }

        if  (tx.getSapRequestSet().stream()
                .filter(sr -> !sr.getIgnore())
                .filter(sr -> sr.getRequestType() == SapRequestType.ADVANCEMENT)
                .count() > 1) {
            throw new Error("Unexpected number of advancements for " + event.getExternalId() + " " + tx.getExternalId());
        }

        final SapRequest invoiceInterest = tx.getSapRequestSet().stream()
                .filter(sr -> !sr.getIgnore())
                .filter(sr -> sr.getRequestType() == SapRequestType.INVOICE_INTEREST)
                .findAny().orElse(null);

        final SapRequest paymentInterest = tx.getSapRequestSet().stream()
                .filter(sr -> !sr.getIgnore())
                .filter(sr -> sr.getRequestType() == SapRequestType.PAYMENT_INTEREST)
                .findAny().orElse(null);

        final SapRequest advancement = tx.getSapRequestSet().stream()
                .filter(sr -> !sr.getIgnore())
                .filter(sr -> sr.getRequestType() == SapRequestType.ADVANCEMENT)
                .findAny().orElse(null);

        final Map<SapRequest, SapRequest> invoiceMap = tx.getSapRequestSet().stream()
                .filter(sr -> !sr.getIgnore())
                .filter(sr -> sr.getRequestType() == SapRequestType.PAYMENT)
                .collect(Collectors.toMap((sr) -> event.getSapRequestSet().stream()
                                .filter(osr -> !osr.getIgnore())
                                .filter(osr -> osr.getRequestType() == SapRequestType.INVOICE)
                                .filter(osr -> sr.refersToDocument(osr.getDocumentNumber()))
                                .findAny().orElseThrow(() -> new Error("No invoice found for " + event.getExternalId() + " " + sr.getDocumentNumber())),
                        (sr) -> sr));

        existingRequests.forEach(sr -> sr.setEvent(null));

        processSap(errorLogConsumer, eventLogger, event, false);

        if (invoiceInterest != null) {
            tx.getSapRequestSet().stream()
                    .filter(sr -> !sr.getSent())
                    .filter(sr -> !sr.getIgnore())
                    .filter((sr -> sr.getWhenCreated().toLocalDate().equals(today)))
                    .filter(sr -> sr.getRequestType() == SapRequestType.INVOICE_INTEREST)
                    .forEach(sr -> sr.delete());
        }

        if (paymentInterest != null) {
            tx.getSapRequestSet().stream()
                    .filter(sr -> !sr.getSent())
                    .filter(sr -> !sr.getIgnore())
                    .filter((sr -> sr.getWhenCreated().toLocalDate().equals(today)))
                    .filter(sr -> sr.getRequestType() == SapRequestType.PAYMENT_INTEREST)
                    .forEach(sr -> sr.delete());
        }

        if (advancement != null) {
            tx.getSapRequestSet().stream()
                    .filter(sr -> !sr.getSent())
                    .filter(sr -> !sr.getIgnore())
                    .filter((sr -> sr.getWhenCreated().toLocalDate().equals(today)))
                    .filter(sr -> sr.getRequestType() == SapRequestType.ADVANCEMENT)
                    .forEach(sr -> sr.delete());
        }

        tx.getSapRequestSet().stream()
                .filter(sr -> !sr.getSent())
                .filter(sr -> !sr.getIgnore())
                .filter(sr -> sr.getRequestType() == SapRequestType.PAYMENT)
                .forEach(sr -> {
                    final SapRequest invoice = event.getSapRequestSet().stream()
                            .filter(osr -> !osr.getIgnore())
                            .filter(osr -> osr.getRequestType() == SapRequestType.INVOICE)
                            .filter(osr -> sr.refersToDocument(osr.getDocumentNumber()))
                            .findAny().orElseThrow(() -> new Error("No invoice found for " + event.getExternalId() + " " + sr.getDocumentNumber()));
                    if (invoiceMap.containsKey(invoice)) {
                        sr.delete();
                    }
                });

        existingRequests.forEach(sr -> sr.setEvent(event));

        check(tx);
    }

    private void check(AccountingTransaction tx) {
        final Money txAmount = tx.getOriginalAmount();
        final Money sapValue = sapValueFor(tx);
        if (!sapValue.equals(txAmount)) {
            throw new Error("Fix failed for event: " + tx.getEvent().getExternalId());
        }
    }

    private Money sapValueFor(AccountingTransaction tx) {
        taskLog("%n%s%n", tx.getExternalId());
        return tx.getSapRequestSet().stream()
                .filter(sr -> !sr.getIgnore())
//                              .filter(sr -> !sr.isInitialization())
                .filter(sr -> sr.getOriginalRequest() == null)
                .filter(sr -> !isInvoice(sr))
                .filter(sr -> sr.getAdvancementRequest() == null || sr.getAdvancementRequest().getEvent() != tx.getEvent())
                .peek(sr -> taskLog("    %s : %s + %s%n", sr.getDocumentNumber(), sr.getValue(), sr.getAdvancement()))
                .map(sr -> sr.getValue().add(sr.getAdvancement()))
                .reduce(Money.ZERO, Money::add);
    }

    private boolean isInvoice(final SapRequest sr) {
        final SapRequestType requestType = sr.getRequestType();
        return requestType == SapRequestType.INVOICE || requestType == SapRequestType.INVOICE_INTEREST;
    }

    private void processSap(ErrorLogConsumer errorLog, EventLogger elogger, Event event, boolean offsetPayments) {
        if (shouldProcess(errorLog, event)) {
            SapEvent sapEvent = new SapEvent(event);
            if (!sapEvent.hasPendingDocumentCancelations()) {
                if (EventWrapper.needsProcessingSap(event)) {
                    EventWrapper eventWrapper = new EventWrapper(event, errorLog, true);
                    sapEvent.updateInvoiceWithNewClientData();
                    Money debtFenix = eventWrapper.debt;
                    Money invoiceSap = sapEvent.getInvoiceAmount();
                    if (debtFenix.isPositive()) {
                        if (invoiceSap.isZero()) {
                            sapEvent.registerInvoice(debtFenix, event, eventWrapper.isGratuity(), false);
                        } else if (invoiceSap.isNegative()) {
                            taskLog("A dívida no SAP é negativa");
                        }
                    }

                    DebtInterestCalculator calculator = event.getDebtInterestCalculator(new DateTime());
                    Iterator var9 = calculator.getAccountingEntries().iterator();

                    while(true) {
                        while(var9.hasNext()) {
                            AccountingEntry accountingEntry = (AccountingEntry)var9.next();
                            if (accountingEntry instanceof Payment && accountingEntry.getAmount().compareTo(BigDecimal.ZERO) > 0 && !sapEvent.hasPayment(accountingEntry.getId()) && accountingEntry.getCreated().isAfter(EventWrapper.SAP_TRANSACTIONS_THRESHOLD)) {
                                Payment payment = (Payment)accountingEntry;
                                if (offsetPayments && payment.getCreated().plusDays(15).isAfterNow()) {
                                    return;
                                }

                                if (Strings.isNullOrEmpty(payment.getRefundId())) {
                                    sapEvent.registerPayment((CreditEntry)accountingEntry);
                                } else {
                                    sapEvent.registerAdvancementInPayment(payment);
                                }
                            } else if (accountingEntry instanceof DebtExemption) {
                                if (accountingEntry.getAmount().compareTo(BigDecimal.ZERO) > 0 && !sapEvent.hasCredit(accountingEntry.getId()) && accountingEntry.getCreated().isAfter(EventWrapper.SAP_TRANSACTIONS_THRESHOLD)) {
                                    DebtExemption debtExemption = (DebtExemption)accountingEntry;
                                    if (EventExemptionJustificationType.CUSTOM_PAYMENT_PLAN.name().equals(debtExemption.getDescription())) {
                                        Money value = new Money(debtExemption.getAmount());
                                        sapEvent.fakeSapRequest(SapRequestType.INVOICE, "ND0", value, (String)null);
                                        sapEvent.fakeSapRequest(SapRequestType.CREDIT, "NA0", value, debtExemption.getId());
                                    } else {
//                                        sapEvent.registerCredit(event, debtExemption, eventWrapper.isGratuity());
                                    }
                                }
                            } else if (accountingEntry instanceof Refund && !sapEvent.hasRefund(accountingEntry.getId())) {
                                Refund refund = (Refund)accountingEntry;
                                DebtExemption debtExemption = findDebtExemptionfor(calculator, refund);
                                sapEvent.registerReimbursement(refund, debtExemption);
                            } else if (accountingEntry instanceof ExcessRefund && !sapEvent.hasRefund(accountingEntry.getId())) {
                                ExcessRefund excessRefund = (ExcessRefund)accountingEntry;
                                if (Strings.isNullOrEmpty(excessRefund.getTargetPaymentId())) {
                                    sapEvent.registerReimbursementAdvancement(excessRefund);
                                }
                            }
                        }

                        return;
                    }
                } else {
                    DebtInterestCalculator calculator = event.getDebtInterestCalculator(new DateTime());
                    calculator.getPayments().filter((p) -> {
                        return !sapEvent.hasPayment(p.getId()) && p.getCreated().isAfter(EventWrapper.SAP_TRANSACTIONS_THRESHOLD);
                    }).filter((p) -> {
                        return !offsetPayments || p.getCreated().plusDays(15).isBeforeNow();
                    }).forEach((p) -> {
                        sapEvent.registerPastPayment(p);
                    });
                }
            }
        }
    }

    private static DebtExemption findDebtExemptionfor(DebtInterestCalculator calculator, Refund refund) {
        AccountingEntry previousAccountingEntry = null;
        Iterator var3 = calculator.getAccountingEntries().iterator();

        while(var3.hasNext()) {
            AccountingEntry accountingEntry = (AccountingEntry)var3.next();
            if (accountingEntry instanceof DebtExemption && previousAccountingEntry == refund) {
                return (DebtExemption)accountingEntry;
            }

            if (accountingEntry instanceof Refund) {
                previousAccountingEntry = accountingEntry;
            }
        }

        return null;
    }

    public static boolean shouldProcess(ErrorLogConsumer consumer, Event event) {
        return allAdvancementFromRefundSapIntegration(event) && (EventWrapper.needsProcessingSap(event) || EventWrapper.needsToProcessPayments(event)) && Utils.validate(consumer, event);
    }

    private static boolean allAdvancementFromRefundSapIntegration(Event event) {
        return event.getAccountingTransactionsSet().stream().map((tx) -> {
            return tx.getRefund();
        }).filter((r) -> {
            return r != null;
        }).map((r) -> {
            return r.getEvent();
        }).flatMap((e) -> {
            return e.getNonAdjustingTransactionStream();
        }).allMatch((tx) -> {
            return !tx.getSapRequestSet().isEmpty() && tx.getSapRequestSet().stream().allMatch((sr) -> {
                return sr.getIntegrated();
            });
        });
    }

}