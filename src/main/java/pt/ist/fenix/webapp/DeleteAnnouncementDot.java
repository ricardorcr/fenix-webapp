package pt.ist.fenix.webapp;

import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.cms.domain.Site;
import org.fenixedu.commons.i18n.LocalizedString;
import pt.ist.fenixframework.FenixFramework;

import java.util.Locale;

public class DeleteAnnouncementDot extends CustomTask {

    @Override
    public void runTask() throws Exception {
        Site site = FenixFramework.getDomainObject("563860486496813");
        site.getPostSet().stream()
                .filter(p -> p.getName().getContent().equals("."))
                .filter(p -> p.getCreationDate().getYear() == 2020 && p.getCreationDate().getMonthOfYear() == 2
                                && p.getCreationDate().getDayOfMonth() == 10)
                .forEach(p -> {
                    taskLog("Este é para apagar %s %s%n", p.getName(), p.getCreationDate());
                    p.delete();
                });
    }
}
