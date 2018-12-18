package pt.ist.fenix.webapp;

import com.google.common.base.Strings;
import org.fenixedu.academic.domain.accounting.AccountingTransaction;
import org.fenixedu.academic.domain.accounting.AccountingTransactionDetail;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.EventState;
import org.fenixedu.academic.domain.accounting.Exemption;
import org.fenixedu.academic.domain.accounting.accountingTransactions.detail.SibsTransactionDetail;
import org.fenixedu.academic.domain.accounting.calculator.DebtInterestCalculator;
import org.fenixedu.academic.domain.accounting.events.EventExemptionJustificationType;
import org.fenixedu.academic.domain.accounting.events.gratuity.exemption.penalty.FixedAmountInterestExemption;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.util.Money;
import org.fenixedu.academic.util.sibs.incomming.SibsIncommingPaymentFileDetailLine;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.signals.DomainObjectEvent;
import org.fenixedu.bennu.core.signals.Signal;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.giaf.invoices.EventProcessor;
import pt.ist.fenixedu.giaf.invoices.SapEvent;
import pt.ist.fenixframework.DomainObject;
import pt.ist.fenixframework.FenixFramework;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class CheckAndCorrectDuplicateSibsPayments extends CustomTask {

    private static User responsible = null;

    @Override
    public void runTask() throws Exception {
        try {
            Signal.clear(AccountingTransaction.SIGNAL_ANNUL);

            responsible = User.findByUsername("ist24616");
//            Map<SibsIncommingPaymentFileDetailLine, Set<AccountingTransactionDetail>> sibsMap = new HashMap<>();
//
//            Bennu.getInstance().getAccountingTransactionDetailsSet().stream()
//                    .filter(SibsTransactionDetail.class::isInstance)
//                    .map(atd -> (SibsTransactionDetail) atd)
//                    .filter(atd -> atd.getSibsLine() != null /*&& atd.getTransaction().getAdjustmentTransactionsSet().isEmpty()*/)
//                    .forEach(atd -> sibsMap.computeIfAbsent(atd.getSibsLine(), k -> new HashSet<>()).add(atd));
//
//            sibsMap.forEach((k, v) -> {
//                if (v.size() > 1 && k.getHeader().getWhenProcessedBySibs().getYear() == 2018) {
//                    Money txValue = v.stream().map(atd -> atd.getTransaction().getOriginalAmount())
//                            .reduce(Money.ZERO, Money::add);
//                    if (!k.getAmount().equals(txValue)) {
//                        StringBuilder sb = new StringBuilder();
//                        AccountingTransactionDetail lastAtd = null;
//                        for (AccountingTransactionDetail atd : v) {
//                            if (atd.getWhenProcessed().getDayOfMonth() == 21) {
//                                lastAtd = atd;
//                            }
//                            sb.append(atd.getExternalId()).append(" # ");
//                            sb.append(atd.getWhenRegistered().toString("dd-MM-yyyy HH:mm")).append(" # ");
//                            sb.append(atd.getWhenProcessed().toString("dd-MM-yyyy HH:mm")).append(" --- ");
//                        }
//                        String wasRefunded = "";
//                        if (lastAtd != null && lastAtd.getTransaction().getAdjustmentTransactionsSet().isEmpty()) {
//                            Money refundValue = lastAtd.getEvent().getRefundSet().stream().map(r -> r.getAmount()).reduce(Money.ZERO, Money::add);
//                            boolean allRefundsIntegrated = lastAtd.getEvent().getRefundSet().stream().allMatch(
//                                    r -> r.getAccountingTransaction().getSapRequestSet().stream().allMatch(sr -> sr.getIntegrated()));
//                            if (lastAtd.getTransaction().getOriginalAmount().equals(refundValue)) {
//                                wasRefunded = "wasRefunded";
////                            taskLog("Tudo integrado: %s%n", allRefundsIntegrated);
//                            } else if (refundValue.isPositive()) {
//                                wasRefunded = "foi mas os valores são diferentes";
////                            taskLog("Tudo integrado: %s%n", allRefundsIntegrated);
//                            } else {
//                                wasRefunded = "sem reembolso";
//                                allRefundsIntegrated = false;
//                            }
//                            final String username = lastAtd.getEvent().getPerson().getUsername();
//                            String eventID = lastAtd.getEvent().getExternalId();
//                            String refundEvents = lastAtd.getEvent().getRefundSet().stream()
//                                    .map(r -> r.getAccountingTransaction().getEvent().getExternalId())
//                                    .collect(Collectors.joining(", "));
//                            String refundPayments = lastAtd.getEvent().getRefundSet().stream()
//                                    .map(r -> r.getAccountingTransaction().getExternalId())
//                                    .collect(Collectors.joining(", "));
//                            String advancementsRequests = lastAtd.getTransaction().getSapRequestSet().stream()
//                                    .map(sr -> sr.getDocumentNumberForType("NA"))
//                                    .collect(Collectors.joining(", "));
//                            if (Strings.isNullOrEmpty(advancementsRequests)) {
//                                taskLog("Last é: %s\t%s%n", lastAtd.getExternalId(), lastAtd.getWhenProcessed());
//                            }
//
//                            if (!allRefundsIntegrated) {
//                                //taskLog("%s\t%s\t%s%n", username, eventID, sb.toString());
//                                fixDuplicateOrigin(lastAtd);
//                            }
//
//                            String a = "istID, evento c/ duplicados, evento reembolso, integrado, transaccoes duplicadas, transacções reembolso, NP Adiantamento";
//                            taskLog("%s\t%s\t%s\t%s\t%s\t%s\t%s%n", username, eventID, refundEvents, allRefundsIntegrated,
//                                    sb.toString(), refundPayments, advancementsRequests);
//
////                        final DateTime lastAtdWhenRegistered = lastAtd.getWhenRegistered();
////                        lastAtd.getEvent().getRefundSet().stream()
////                                .filter(r -> r.getAccountingTransaction().getWhenRegistered().equals(lastAtdWhenRegistered))
////                                .forEach(r -> {
////                                    AccountingTransaction at = r.getAccountingTransaction();
////                                    boolean integrated = at.getSapRequestSet().stream().allMatch(sr -> sr.getIntegrated());
////                                    Event event = at.getEvent();
////                                    taskLog("%s\t%s\t%s\t%s\t%s\t%s\t%s%n", username, event.getExternalId(), event.getDescription(),
////                                            r.getAmount(), at.getWhenProcessed().toString("dd/MM/yyyy HH:mm"), integrated, at.getExternalId());
////                                });
//                        }
//
////                    taskLog("Está repetido: OID whenRegistered whenProcessed %s%n", wasRefunded);
////                    taskLog(sb.toString() + "\n");
//                    }
//                }
//            });
//
//            fixUsedDuplicate("286491498514946", "564470371893692");
//            fixUsedDuplicate("286641822369729", "564470371892704");
//            fixUsedDuplicate("568116799080594", "564470371893500");
//            fixUsedDuplicate("1125925676647822", "564470371886783");
//            fixUsedDuplicate("1126058820639172", "1408895302010555");
//            fixUsedDuplicate("1130916428646151", "1127420325301304");
//            fixUsedDuplicate("1134124769220005", "1408895302010554");
//            fixUsedDuplicate("1134150539019113", "1127420325299719");
//            fixUsedDuplicate("1134154833986319", "564470371894553");
//            fixUsedDuplicate("1407400653357974", "564470371885747");
//            fixUsedDuplicate("1407400653358105", "564470371886834");
//            fixUsedDuplicate("1407400653358686", "564470371886664");
//            fixUsedDuplicate("1407400653359111", "564470371886619");
//            fixUsedDuplicate("1407400653361665", "564470371885936");
//            fixUsedDuplicate("1407400653362422", "564470371886558");
//            fixUsedDuplicate("1407400653363265", "564470371886585");
//            fixUsedDuplicate("1407400653363363", "564470371886648");
//            fixUsedDuplicate("1407400653363437", "564470371886471");
//            fixUsedDuplicate("1407400653364412", "564470371886671");
//            fixUsedDuplicate("1407400653364588", "564470371886595");
//            fixUsedDuplicate("1407400653364707", "564470371886521");
//            fixUsedDuplicate("1693866382067388", "564470371885751");
//            fixUsedDuplicate("1978575469150263", "1971845255429188");
//            fixUsedDuplicate("1978579764117703", "564470371888569");
//
//            fixUsedMultipleDuplicates("1407400653360579", "564470371888883");
//            fixUsedMultipleDuplicates("1407400653360579", "564470371886789");
//            fixUsedDuplicate("1407400653360579", "564470371888882");

//            fixDuplicateOrigin("1127420325284437");
//            fixDuplicateOrigin("1127420325284444");
//            fixDuplicateOrigin("1127420325284445");
//            fixDuplicateOrigin("1127420325284451");
//            fixDuplicateOrigin("1127420325284452");
//            fixDuplicateOrigin("1127420325284453");

//            fixDuplicateOrigin("1971845255470498");


            fixUsedMultipleDuplicates("1125925676653037","1690370278740830");
//        throw new Error("Dry run!");
        } finally {
            Signal.register(AccountingTransaction.SIGNAL_ANNUL, this::handlerAccountingTransactionAnnulment);
        }
    }

    private void fixUsedDuplicate(final String eventId, final String paymentId) {

        Event event = FenixFramework.getDomainObject(eventId);
        AccountingTransaction accountingTransaction = FenixFramework.getDomainObject(paymentId);
        fixUsedMultipleDuplicates(event, accountingTransaction);

        SapRequest lastPayment = getLastPayment(accountingTransaction);
        if (lastPayment != null) {
            SapEvent sapEvent = new SapEvent(lastPayment.getEvent());
            sapEvent.cancelDocument(lastPayment);

            lastPayment.setIgnore(true); //TODO for tests only -> em qualidade os IK's não foram estornados
        }
        //o pagamento final pode ter sido cancelado e é necessário criar o request certo para esse pagamento
        EventProcessor.calculate(() -> event);
    }

    private void fixUsedMultipleDuplicates(final String eventId, final String paymentId) {
        Event event = FenixFramework.getDomainObject(eventId);
        AccountingTransaction accountingTransaction = FenixFramework.getDomainObject(paymentId);
        fixUsedMultipleDuplicates(event, accountingTransaction);
    }

    private void fixUsedMultipleDuplicates(final Event event, final AccountingTransaction accountingTransaction) {

        final Map<String, BigDecimal> paymentsInterests = new HashMap<>();
        DebtInterestCalculator calculatorBefore = event.getDebtInterestCalculator(new DateTime());
        calculatorBefore.getPayments().forEach(p -> {
            paymentsInterests.put(p.getId(), p.getUsedAmountInInterests());
        });

        Set<AccountingTransaction> accountingTransactionsSet = new TreeSet<>(AccountingTransaction.COMPARATOR_BY_WHEN_REGISTERED);
        accountingTransactionsSet.addAll(event.getAccountingTransactionsSet());
        Set<Exemption> exemptionSet = new HashSet<>(event.getExemptionsSet());

        Set<AccountingTransaction> originAccountingTransactions = new TreeSet<>(AccountingTransaction.COMPARATOR_BY_WHEN_REGISTERED.reversed());
        accountingTransaction.getRefund().getSapRequestSet().forEach(sr -> originAccountingTransactions.add(sr.getAdvancementRequest().getPayment()));
        //this requests are going to be canceled and the refund deleted
        accountingTransaction.getRefund().getSapRequestSet().clear();

        //disconnecting objects from event
        event.getAccountingTransactionsSet().forEach(at -> at.setEvent(null));
        event.getExemptionsSet().forEach(ex -> ex.setEvent(null));

        //canceling duplicated payment
        accountingTransaction.setEvent(event);
        Set<SapRequest> requestsToRevertIgnore = accountingTransaction.getSapRequestSet().stream()
                .filter(sr -> !sr.getIgnore())
                .map(sr -> {
                    SapEvent sapEvent = new SapEvent(event);
                    sapEvent.cancelDocument(sr);
                    sr.setIgnore(true);
                    sr.getAnulledRequest().setIgnore(true);
                    return sr;
                }).collect(Collectors.toSet());
        try {
            accountingTransaction.annul(responsible, "Esta utilização do adiantamento foi indevida, pois o adiantamento foi mal registado.");
        } catch (Throwable de) {
            taskLog("-------------Tentei cancelar %s para evento %s%n", accountingTransaction.getExternalId(), accountingTransaction.getEvent().getExternalId());
            throw de;
        }
        requestsToRevertIgnore.forEach(sr -> {
            sr.setIgnore(false);
            if (sr.getAnulledRequest() != null) {
                sr.getAnulledRequest().setIgnore(false);
            }
        });

        accountingTransactionsSet.forEach(at -> {
            at.setEvent(event);
            if (at.getAdjustedTransaction() == null && at.getAdjustmentTransactionsSet().isEmpty() && at != accountingTransaction) {
                //TODO se existirem 2 pagamentos exactamente na mesma data HH:mm ddMMYYYY isto pode esbroncar os juros...
                DateTime beforePayment = at.getWhenRegistered().minusSeconds(1);
                DebtInterestCalculator calculator = event.getDebtInterestCalculator(beforePayment);
                if (paymentsInterests.get(at.getExternalId()) == null) {
                    taskLog("############Transaccao %s não consta no mapa%n", at.getExternalId());
                }
                BigDecimal interestToExempt = calculator.getDueInterestAmount().subtract(paymentsInterests.get(at.getExternalId()));

                if (interestToExempt.signum() == 1) {
                    FixedAmountInterestExemption exemption = new FixedAmountInterestExemption(event, responsible.getPerson(),
                            new Money(interestToExempt), EventExemptionJustificationType.FINE_EXEMPTION, beforePayment,
                            "Foi cancelado um pagamento que estava duplicado e foi necessário isentar os juros daí decorrentes.");
                    exemption.setWhenCreated(beforePayment);
                }
            }
        });
        exemptionSet.forEach(ex -> ex.setEvent(event));

        originAccountingTransactions.forEach(at -> fixDuplicateOrigin(at.getTransactionDetail()));
    }

    private SapRequest getLastPayment(final AccountingTransaction transaction) {
        SapEvent sapEvent = new SapEvent(transaction.getEvent());

        if (!transaction.getSapRequestSet().isEmpty()) {
            SapRequest paymentRequest = transaction.getSapRequestSet().stream()
                    .filter(sr -> sr.getRequestType().equals(SapRequestType.ADVANCEMENT) ||
                            sr.getRequestType().equals(SapRequestType.PAYMENT))
                    .findAny().orElseGet(() -> null);
            if (paymentRequest == null) {
                return null;
            }
            String invoiceNumber = paymentRequest.getDocumentNumberForType("ND");
            SapRequest invoiceRequest = sapEvent.getFilteredSapRequestStream().filter(sr -> sr.getDocumentNumber().equals(invoiceNumber)).findAny().get();
            if (!invoiceRequest.openInvoiceValue().isPositive()) {
                final Set<SapRequest> invoiceDocs = new TreeSet<>(SapRequest.COMPARATOR_BY_ORDER.reversed());
                sapEvent.getFilteredSapRequestStream()
                        .filter(sr -> sr.refersToDocument(invoiceNumber) && sr.getOriginalRequest() == null)
                        .forEach(sr -> invoiceDocs.add(sr));
                if (!invoiceDocs.isEmpty()) {
                    SapRequest lastRequest = invoiceDocs.iterator().next();
                    if (lastRequest.getRequestType().equals(SapRequestType.PAYMENT) || lastRequest.getRequestType().equals(SapRequestType.ADVANCEMENT)) {
                        return lastRequest.getPayment() != transaction ? lastRequest : null;
                    }
                }
            }
        }
        return null;
    }

    private void fixDuplicateOrigin(final String transactionId) {
        AccountingTransaction accountingTransaction = FenixFramework.getDomainObject(transactionId);
        fixDuplicateOrigin(accountingTransaction.getTransactionDetail());
    }

    private void fixDuplicateOrigin(final AccountingTransactionDetail lastAtd) {
        Event event = lastAtd.getEvent();
        Set<AccountingTransaction> accountingTransactionsSet = new HashSet<>(lastAtd.getEvent().getAccountingTransactionsSet());
        Set<Exemption> exemptionSet = new HashSet<>(lastAtd.getEvent().getExemptionsSet());

        accountingTransactionsSet.forEach(at -> at.setEvent(null));
        exemptionSet.forEach(ex -> ex.setEvent(null));

        lastAtd.getTransaction().setEvent(event);
        lastAtd.getTransaction().getSapRequestSet().stream()
                .forEach(sr -> {
                    SapEvent sapEvent = new SapEvent(lastAtd.getEvent());
                    sapEvent.cancelDocument(sr);

                    SapRequest anulledRequest = sr.getAnulledRequest();
                    sr.setIgnore(true);
                    anulledRequest.setIgnore(true);
                });
        try {
            lastAtd.getTransaction().annul(responsible, "Transacção sibs carregada em duplicado.");
        } catch (Throwable de) {
            taskLog("##########Tentei cancelar %s para evento %s%n", lastAtd.getExternalId(), lastAtd.getEvent().getExternalId());
            throw de;
        }
        lastAtd.getTransaction().getSapRequestSet().stream()
                .forEach(sr -> {
                    SapRequest anulledRequest = sr.getAnulledRequest();
                    sr.setIgnore(false);
                    anulledRequest.setIgnore(false);
                });

        accountingTransactionsSet.forEach(at -> at.setEvent(event));
        exemptionSet.forEach(ex -> ex.setEvent(event));
    }

    private void handlerAccountingTransactionAnnulment(final DomainObjectEvent<AccountingTransaction> domainEvent) {
        final AccountingTransaction transaction = domainEvent.getInstance();
        final Event event = transaction.getEvent();
        if (new SapEvent(event).hasPayment(transaction.getExternalId())) {
            throw new DomainException(Optional.of("resources.GiafInvoicesResources"), "error.first.undo.transaction.in.sap");
        }
    }

}
