package pt.ist.fenix.webapp;

import java.util.Set;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

import pt.ist.fenixedu.quc.domain.InquiriesRoot;

public class Fill3rdCycleDegreesForQUC extends CustomTask {

    @Override
    public void runTask() throws Exception {
        String[] degrees =
                new String[] { "CEEST", "CELSSBB", "Darq", "Dbioeng", "DBiotec", "POSI", "DEC", "DEEC", "DEFT", "DEGest",
                        "DEQuim", "DFAERM", "DMat", "DQuim", "DSSE" };

        InquiriesRoot.getInstance().getDegreesAvailableForInquiriesSet().clear();
        Set<Degree> degreesAvailableForInquiriesSet = InquiriesRoot.getInstance().getDegreesAvailableForInquiriesSet();
        for (String sigla : degrees) {
            degreesAvailableForInquiriesSet.add(Degree.readBySigla(sigla));
        }
    }
}
