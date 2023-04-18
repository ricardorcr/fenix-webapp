package pt.ist.fenix.webapp;

import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.payments.domain.SibsPayment;
import pt.ist.payments.util.SibsOperationType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class CheckMissingSIBSTransactionsFromFile extends CustomTask {

    private final String BASE_DIR = "/afs/ist.utl.pt/ciist/fenix/sibs/sibs001/processed/";
    private final String FILE_NAME = "EXTO_V00_20221018_0261369_00000002_0000_132.csv";

    @Override
    public void runTask() throws Exception {
        final File input = new File(BASE_DIR /*+ FILE_NAME*/);
        for (final File file : input.listFiles()) {
            if (file.getName().endsWith(".csv") && file.getName().contains("202210")) {
                importFromReport(file.toPath());
            }
        }
    }

    public void importFromReport(final Path path) throws IOException {
        for (final String line : Files.readAllLines(path)) {
            if (!line.startsWith("Tipo_reg_inicio")) {
                final String[] parts = line.split(",");
                final String operationTypeCode = parts[32];
                if (SibsOperationType.isBoolean(operationTypeCode)) {
                    final String merchantReference = parts[48];
                    final SibsPayment sibsPayment = SibsPayment.findWithApiId(merchantReference);
                    if (sibsPayment != null) {
                        if (sibsPayment.getSettlement() == null) {
                            taskLog("Transação sem Settlement: %s\n%s\n%n", sibsPayment.getExternalId(), line);
                        }
//                        sibsPayment.setSettlement(line);
                    } else {
                        taskLog("Transação inexistente: %s%n", line);
                    }
                }
            }
        }
    }
}
