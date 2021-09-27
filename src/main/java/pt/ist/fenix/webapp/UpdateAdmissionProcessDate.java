package pt.ist.fenix.webapp;

import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.admissions.domain.AdmissionsSystem;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;
import pt.ist.fenixframework.FenixFramework;

import java.util.Locale;

public class UpdateAdmissionProcessDate extends CustomTask {

    @Override
    public void runTask() throws Exception {
//        AdmissionsSystem.getInstance().getAdmissionProcessSet().stream()
//                .filter(ap -> {
//                    final String title = ap.getTitle().getContent(Locale.forLanguageTag("pt-PT"));
//                    return title.contains("Internacionais 2021/2022 - 2ª Fase");
//                })
//                .forEach(ap -> {
//                    ap.setStartOutcomePeriod(new DateTime(2021,06,30,0,0,0));
//                    ap.setEndOutcomePeriod(new DateTime(2021,07,14,23,59,59));
//                });

        AdmissionProcess kicRENE = FenixFramework.getDomainObject("1415857443963207");
        kicRENE.setEndOutcomePeriod(new DateTime(2021,9,7,23,59,59));
        kicRENE.setEndApplicationSubmissionPeriod(new DateTime(2022, 05, 05, 05, 05));
    }
}