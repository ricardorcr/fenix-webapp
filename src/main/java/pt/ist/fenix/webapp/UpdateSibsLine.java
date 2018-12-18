package pt.ist.fenix.webapp;

import com.google.api.client.util.Strings;
import org.fenixedu.academic.domain.accounting.PaymentCode;
import org.fenixedu.academic.domain.accounting.accountingTransactions.detail.SibsTransactionDetail;
import org.fenixedu.academic.domain.accounting.paymentCodes.AccountingEventPaymentCode;
import org.fenixedu.academic.domain.accounting.paymentCodes.IndividualCandidacyPaymentCode;
import org.fenixedu.academic.domain.accounting.paymentCodes.InstallmentPaymentCode;
import org.fenixedu.academic.util.sibs.incomming.SibsIncommingPaymentFile;
import org.fenixedu.academic.util.sibs.incomming.SibsIncommingPaymentFileDetailLine;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixframework.Atomic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class UpdateSibsLine extends CustomTask {

    private final Map<String, Set<SibsTransactionDetail>> detailsMap = new HashMap<>();
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
                .filter(atd -> atd.getSibsLine() == null)
                .forEach(this::fillMap);

        ///afs/ist.utl.pt/ciist/fenix/fenix015/sibs
        final File dir = new File("/home/rcro/DocumentsSSD/fenix/pagamentos/sibs/MEPS");
        File[] files = dir.listFiles();
        for (int iter = 0; iter < files.length; iter++) {
            processFile(files[iter]);
        }
//        final File file = new File("/home/rcro/DocumentsSSD/fenix/pagamentos/sibs/MEPS/20180406$3246.INP");
//        processFile(file);
//        throw new Error("Dry run");
    }

    private void processFile(final File file) throws IOException {
        if (file.getName().endsWith(".INP")) {
            FileInputStream fileInputStream = null;
            try {
                fileInputStream = new FileInputStream(file);
                final SibsIncommingPaymentFile sibsFile = SibsIncommingPaymentFile.parse(file.getName(), fileInputStream);
                for (final SibsIncommingPaymentFileDetailLine detailLine : sibsFile.getDetailLines()) {
                    if (!sibsLines.contains(detailLine)) {
                        String codeForMap = detailLine.getCode() + detailLine.getWhenOccuredTransaction().toString(DATE_PATTERN);
                        Set<SibsTransactionDetail> transactionDetails = detailsMap.get(codeForMap);
                        if (transactionDetails != null) {
                            transactionDetails.stream()
                                    .filter(td -> td.getSibsLine() == null)
                                    .forEach(td -> td.setSibsLine(detailLine)/*td.getSibsLine()*/);
                        } else {
                            String sibsLine = detailLine.export().split("\n")[1];
                            PaymentCode paymentCode = PaymentCode.readByCode(detailLine.getCode());
                            if (paymentCode != null) {
                                taskLog("Não encontrei\t%s\tdo ficheiro: %s\t%s\t%s%n",
                                        sibsLine, file.getName(), getStudent(paymentCode), getEvent(paymentCode));
                            } else {
                                taskLog("Não encontrei\t%s\tdo ficheiro: %s\tNão existe o código de pagamento\t%s%n",
                                        sibsLine, file.getName(), detailLine.getCode());
                            }
                        }
                    }
                }
            } catch (FileNotFoundException e) {
                taskLog("ERROR - File %s%n", file.getName());
                e.printStackTrace();
            } finally {
                fileInputStream.close();
            }
        }
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
            detailsMap.computeIfAbsent(atd.getSibsCode() + atd.getWhenRegistered().toString(DATE_PATTERN),
                    k -> new HashSet<>()).add(atd);
        }
    }
}
