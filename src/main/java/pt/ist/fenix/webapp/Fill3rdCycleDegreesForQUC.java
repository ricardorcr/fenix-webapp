package pt.ist.fenix.webapp;

import java.util.Set;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicInterval;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

import pt.ist.fenixedu.quc.domain.InquiriesRoot;

public class Fill3rdCycleDegreesForQUC extends CustomTask {

    @Override
    public void runTask() throws Exception {
        String[] degrees =
                new String[] { "DEBiom","DEC","DEEC","DEGest","DEIC","DEQuim","DETPT","DMat","DQuim","DSSE" };

        InquiriesRoot.getInstance().getDegreesAvailableForInquiriesSet().clear();
        Set<Degree> degreesAvailableForInquiriesSet = InquiriesRoot.getInstance().getDegreesAvailableForInquiriesSet();
//        for (String sigla : degrees) {
//            taskLog("Setting %s%n", sigla);
//            degreesAvailableForInquiriesSet.add(Degree.readBySigla(sigla));
//        }
//
        final AcademicInterval academicInterval = ExecutionSemester.readActualExecutionSemester().getAcademicInterval();
        Degree.readBolonhaDegrees().stream()
                .filter(Degree::isDEA)
                .filter(degree -> !degree.getExecutionDegrees(academicInterval).isEmpty())
                .forEach(degreesAvailableForInquiriesSet::add);
    }
}
