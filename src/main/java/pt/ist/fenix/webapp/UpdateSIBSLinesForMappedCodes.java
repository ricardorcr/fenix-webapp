package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.accounting.PaymentCode;
import org.fenixedu.academic.domain.accounting.PaymentCodeMapping;
import org.fenixedu.academic.domain.accounting.accountingTransactions.detail.SibsTransactionDetail;
import org.fenixedu.academic.util.sibs.incomming.SibsIncommingPaymentFile;
import org.fenixedu.academic.util.sibs.incomming.SibsIncommingPaymentFileDetailLine;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

public class UpdateSIBSLinesForMappedCodes extends CustomTask {

    private final String DATE_PATTERN = "yyyyMMddHHmm";
    private final String DATE_DAY_PATTERN = "yyyyMMdd";

    @Override
    public void runTask() throws Exception {
        Bennu.getInstance().getAccountingTransactionDetailsSet().stream()
                .filter(SibsTransactionDetail.class::isInstance)
                .map(atd -> (SibsTransactionDetail) atd)
                .filter(atd -> atd.getSibsLine() == null)
                .filter(atd -> atd.getWhenRegistered().getYear() >= 2018)
                .filter(this::doesNotExistOtherTransaction)
                .filter((this::codeIsMapped))
                .forEach(this::fix);
    }

    private void fix(final SibsTransactionDetail sibsTransactionDetail) {
        taskLog("TransactionID: %s - Code: %s%n", sibsTransactionDetail.getExternalId(), sibsTransactionDetail.getSibsCode());
        PaymentCode paymentCode = PaymentCode.readByCode(sibsTransactionDetail.getSibsCode());
        PaymentCodeMapping codeMapping = paymentCode.getNewPaymentCodeMappingsSet().iterator().next();
        PaymentCode oldPaymentCode = codeMapping.getOldPaymentCode();
        if (paymentCode != oldPaymentCode) {
            DateTime whenRegistered = sibsTransactionDetail.getWhenRegistered();
            DateTime dayDate = whenRegistered;
            if (whenRegistered.getHourOfDay() > 20 || (whenRegistered.getHourOfDay() == 20 && whenRegistered.getMinuteOfDay() > 0)) {
                dayDate = dayDate.plusDays(1);
            }
            String dayDateKey = dayDate.toString(DATE_DAY_PATTERN);
            File file = getFile(dayDateKey);
            FileInputStream fileInputStream = null;
            try {
                fileInputStream = new FileInputStream(file);
                final SibsIncommingPaymentFile sibsFile = SibsIncommingPaymentFile.parse(file.getName(), fileInputStream);
                String bdCode = oldPaymentCode.getCode() + whenRegistered.toString(DATE_PATTERN);
                for (final SibsIncommingPaymentFileDetailLine detailLine : sibsFile.getDetailLines()) {
                    String fileCode = detailLine.getCode() + detailLine.getWhenOccuredTransaction().toString(DATE_PATTERN);
                    if (bdCode.equals(fileCode)) {
                        sibsTransactionDetail.setSibsLine(detailLine);
                    }
                }
            } catch (FileNotFoundException e) {
                taskLog("ERROR - File %s%n", file.getName());
                e.printStackTrace();
            } finally {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            taskLog("ERRO - algum muito errado aqui");
        }
    }

    private File getFile(String dayDate) {
        ///afs/ist.utl.pt/ciist/fenix/fenix015/sibs
        ///home/rcro/DocumentsSSD/fenix/pagamentos/sibs/MEPS
        final File dir = new File("/home/rcro/DocumentsSSD/fenix/pagamentos/sibs/MEPS");
        File[] files = dir.listFiles();
        return Arrays.asList(files).stream().filter(f -> f.getName().contains(dayDate))
                .filter(f -> f.getName().endsWith(".INP")).findAny().get();
    }

    private boolean codeIsMapped(final SibsTransactionDetail sibsTransactionDetail) {
        PaymentCode paymentCode = PaymentCode.readByCode(sibsTransactionDetail.getSibsCode());
        return !paymentCode.getNewPaymentCodeMappingsSet().isEmpty() || !paymentCode.getOldPaymentCodeMappingsSet().isEmpty();
    }

    private boolean doesNotExistOtherTransaction(final SibsTransactionDetail sibsTransactionDetail) {
        return !Bennu.getInstance().getAccountingTransactionDetailsSet().stream()
                .filter(SibsTransactionDetail.class::isInstance)
                .map(atd -> (SibsTransactionDetail) atd)
                .filter(atd -> atd.getSibsLine() != null)
                .filter((atd -> atd.getSibsCode().equals(sibsTransactionDetail.getSibsCode())))
                .findAny().isPresent();
    }
}
