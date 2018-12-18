package pt.ist.fenix.webapp;

import com.google.common.io.CharStreams;
import org.fenixedu.bennu.io.domain.GroupBasedFile;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;
import pt.ist.fenixedu.quc.domain.InquiryResult;
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
            GroupBasedFile qucResults = FenixFramework.getDomainObject("282093452062616");
            streamReader = new InputStreamReader(qucResults.getStream());
            String stringResults = CharStreams.toString(streamReader);

            DateTime resultDate = new DateTime(2019, 9, 16, 14, 15);
            InquiryResult.importResults(stringResults, resultDate);

        } finally {
            if (streamReader != null) {
                streamReader.close();
            }
        }
    }
}