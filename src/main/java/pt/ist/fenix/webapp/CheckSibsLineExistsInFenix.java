package pt.ist.fenix.webapp;

import com.google.api.client.util.Strings;
import org.fenixedu.academic.domain.accounting.PaymentCode;
import org.fenixedu.academic.domain.accounting.accountingTransactions.detail.SibsTransactionDetail;
import org.fenixedu.academic.domain.accounting.paymentCodes.AccountingEventPaymentCode;
import org.fenixedu.academic.domain.accounting.paymentCodes.IndividualCandidacyPaymentCode;
import org.fenixedu.academic.domain.accounting.paymentCodes.InstallmentPaymentCode;
import org.fenixedu.academic.util.Money;
import org.fenixedu.academic.util.sibs.incomming.SibsIncommingPaymentFile;
import org.fenixedu.academic.util.sibs.incomming.SibsIncommingPaymentFileDetailLine;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixframework.Atomic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CheckSibsLineExistsInFenix extends CustomTask {

    private final Map<SibsIncommingPaymentFileDetailLine, Set<SibsTransactionDetail>> detailsMap = new HashMap<>();
    private final Set<SibsIncommingPaymentFileDetailLine> sibsLines = new HashSet<>();
    private final String DATE_PATTERN = "yyyyMMddHHmm";

    @Override
    public Atomic.TxMode getTxMode() {
        return Atomic.TxMode.READ;
    }

    @Override
    public void runTask() throws Exception {
        Bennu.getInstance().getAccountingTransactionDetailsSet().stream()
                .filter(SibsTransactionDetail.class::isInstance)
                .map(atd -> (SibsTransactionDetail) atd)
                .filter(atd -> atd.getSibsLine() != null)
                .forEach(atd -> sibsLines.add(atd.getSibsLine()));

        Bennu.getInstance().getAccountingTransactionDetailsSet().stream()
                .filter(SibsTransactionDetail.class::isInstance)
                .map(atd -> (SibsTransactionDetail) atd)
                .filter(atd -> atd.getSibsLine() != null)
                .forEach(atd -> fillMap(atd));

        ///afs/ist.utl.pt/ciist/fenix/fenix015/sibs
//        final File dir = new File("/afs/ist.utl.pt/ciist/fenix/fenix015/sibs");
//        File[] files = dir.listFiles();
//        for (int iter = 0; iter < files.length; iter++) {
//            processFile(files[iter]);
//        }
        final File file = new File("/afs/ist.utl.pt/ciist/fenix/fenix015/sibs/20180425$3265.INP");
        processFile(file);
//        throw new Error("Dry run");
    }

    private void processFile(final File file) throws IOException {
        if (file.getName().endsWith(".INP") && file.getName().startsWith("2018")) {
            FileInputStream fileInputStream = null;
            final Money[] total = new Money[]{Money.ZERO};
            try {
                fileInputStream = new FileInputStream(file);
                final SibsIncommingPaymentFile sibsFile = SibsIncommingPaymentFile.parse(file.getName(), fileInputStream);
                taskLog(sibsFile.getFooter().getTransactionsTotalAmount().getAmountAsString());
                for (final SibsIncommingPaymentFileDetailLine detailLine : sibsFile.getDetailLines()) {
                    if (!sibsLines.contains(detailLine)) {
                        String sibsLine = detailLine.export().split("\n")[1];
                        PaymentCode paymentCode = PaymentCode.readByCode(detailLine.getCode());
                        if (paymentCode != null) {
                            if (!paymentCode.getOldPaymentCodeMappingsSet().isEmpty() || !paymentCode.getNewPaymentCodeMappingsSet().isEmpty()) {
                                taskLog("Não encontrei, mas código está mapeado\t%s\tdo ficheiro: %s\t%s\t%s%n",
                                        sibsLine, file.getName(), getStudent(paymentCode), getEvent(paymentCode));
                            } else {
                                taskLog("Não encontrei\t%s\tdo ficheiro: %s\t%s\t%s%n",
                                        sibsLine, file.getName(), getStudent(paymentCode), getEvent(paymentCode));
                            }
                        } else {
                            taskLog("Não encontrei\t%s\tdo ficheiro: %s\tNão existe o código de pagamento\t%s%n",
                                    sibsLine, file.getName(), detailLine.getCode());
                        }
                    } else {
//                        taskLog("Vou processar a linha: %s%n", detailLine.getCode());
                        detailsMap.get(detailLine).stream()
                                .filter(atd -> atd.getTransaction().getAmountWithAdjustment().isPositive())
                                .filter(atd -> !hasSameNPsValue(atd))
                                .forEach(atd -> taskLog(atd.getExternalId()));
//                                .forEach(atd -> total[0] = total[0].add(atd.getTransaction().getAmountWithAdjustment()/*OriginalAmount()*/));
                    }
                }
//                taskLog("Total das transacções: %s%n", total[0]);
            } catch (FileNotFoundException e) {
                taskLog("ERROR - File not found %s%n", file.getName());
                e.printStackTrace();
            } finally {
                fileInputStream.close();
            }
        }
    }

    private boolean hasSameNPsValue(final SibsTransactionDetail sibsTransactionDetail) {
//        taskLog("Vou processar a txd: %s%n", sibsTransactionDetail.getExternalId());
        Money npsValue = sibsTransactionDetail.getTransaction().getSapRequestSet().stream()
                .filter(sr -> !sr.getIgnore())
                .filter(sr -> sr.getAnulledRequest() == null && sr.getOriginalRequest() == null)
                .filter(sr -> sr.getRequestType() == SapRequestType.PAYMENT || sr.getRequestType() == SapRequestType.PAYMENT_INTEREST
                        || sr.getRequestType() == SapRequestType.ADVANCEMENT)
//                .peek(sr -> taskLog("%s\t%s\t%s\t%s%n", sr.getDocumentNumber(), sr.getValue().add(sr.getAdvancement()), sr.getRequestType().name(), sr.getPayment().getExternalId()))
                .map(sr -> sr.getValue().add(sr.getAdvancement()))
                .reduce(Money.ZERO, Money::add);
        return npsValue.equals(sibsTransactionDetail.getTransaction().getOriginalAmount());
    }

    private String getEvent(final PaymentCode paymentCode) {
        try {
            if (paymentCode instanceof AccountingEventPaymentCode) {
                return ((AccountingEventPaymentCode) paymentCode).getAccountingEvent().getExternalId();
            } else if (paymentCode instanceof IndividualCandidacyPaymentCode) {
                return ((IndividualCandidacyPaymentCode) paymentCode).getAccountingEvent().getExternalId();
            } else if (paymentCode instanceof InstallmentPaymentCode) {
                return ((InstallmentPaymentCode) paymentCode).getAccountingEvent().getExternalId();
            } else {
                return "";
            }
        } catch (Exception e) {
            taskLog("ERROR - paymentCode: %s - %s%n", paymentCode.getExternalId(), e.getMessage());
            return "";
        }
    }

    private String getStudent(final PaymentCode paymentCode) {
        try {
            if (paymentCode instanceof AccountingEventPaymentCode) {
                return ((AccountingEventPaymentCode) paymentCode).getAccountingEvent().getPerson().getUsername();
            } else if (paymentCode instanceof IndividualCandidacyPaymentCode) {
                return ((IndividualCandidacyPaymentCode) paymentCode).getAccountingEvent().getPerson().getUsername();
            } else if (paymentCode instanceof InstallmentPaymentCode) {
                return ((InstallmentPaymentCode) paymentCode).getAccountingEvent().getPerson().getUsername();
            } else {
                return "";
            }
        } catch (Exception e) {
            taskLog("ERROR - paymentCode: %s - %s%n", paymentCode.getExternalId(), e.getMessage());
            return "";
        }
    }

    private void fillMap(final SibsTransactionDetail atd) {
        if (Strings.isNullOrEmpty(atd.getSibsCode())) {
            taskLog("%s não tem o código da referência%n", atd.getSibsCode());
        } else {
            detailsMap.computeIfAbsent(atd.getSibsLine(), k -> new HashSet<>()).add(atd);
        }
    }
}
