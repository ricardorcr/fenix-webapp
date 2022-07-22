package pt.ist.fenix.webapp;

import org.fenixedu.bennu.papyrus.domain.PapyrusTemplate;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixframework.FenixFramework;

import java.util.Locale;

public class CreatePapyrusTemplateEN extends CustomTask {

    @Override
    public void runTask() throws Exception {
        PapyrusTemplate papyrusTemplate = FenixFramework.getDomainObject("571342319517697"); //marksheet
        new PapyrusTemplate(papyrusTemplate.getName(), papyrusTemplate.getDisplayName(), papyrusTemplate.getTemplateHtml(), Locale.UK,
                null, null);

        papyrusTemplate = FenixFramework.getDomainObject("571342319517698"); //marksheet rectified
        new PapyrusTemplate(papyrusTemplate.getName(), papyrusTemplate.getDisplayName(), papyrusTemplate.getTemplateHtml(), Locale.UK,
                null, null);

    }
}