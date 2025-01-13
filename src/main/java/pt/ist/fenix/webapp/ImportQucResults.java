package pt.ist.fenix.webapp;

import com.google.common.io.CharStreams;
import org.fenixedu.bennu.core.signals.DomainObjectEvent;
import org.fenixedu.bennu.core.signals.Signal;
import org.fenixedu.bennu.io.domain.GroupBasedFile;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;
import pt.ist.fenixedu.quc.domain.InquiryResult;
import pt.ist.fenixedu.quc.domain.ResultsImportationProcess;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;

import java.io.InputStreamReader;

public class ImportQucResults extends CustomTask {

    @Override
    public TxMode getTxMode() {
        return TxMode.READ;
    }

    @Override
    public void runTask() throws Exception {
        InputStreamReader streamReader = null;
        try {
            GroupBasedFile qucResults = FenixFramework.getDomainObject("282093452162008");
            streamReader = new InputStreamReader(qucResults.getStream());
            String stringResults = CharStreams.toString(streamReader);

            DateTime resultDate = new DateTime(2024, 9, 20, 14, 15);
            InquiryResult.importResults(stringResults, resultDate);

            Signal.emit(ResultsImportationProcess.QUC_RESULTS_IMPORTED,new DomainObjectEvent<>(null));
        } finally {
            if (streamReader != null) {
                streamReader.close();
            }
        }
    }
}