package pt.ist.fenix.webapp;

import org.fenixedu.bennu.portal.domain.MenuContainer;
import org.fenixedu.bennu.portal.domain.MenuFunctionality;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.i18n.LocalizedString;

import pt.ist.fenixframework.FenixFramework;

public class UpdateMenuFunctionality extends CustomTask {

    @Override
    public void runTask() throws Exception {
        org.fenixedu.bennu.portal.domain.MenuFunctionality oldMenuFunctionality =
                FenixFramework.getDomainObject("288875205361742");

        updateMenuFunctionality(oldMenuFunctionality, oldMenuFunctionality.getParent(),
                "https://fenix.tecnico.ulisboa.pt/fenixedu-external-teachers/", oldMenuFunctionality.getProvider(),
                oldMenuFunctionality.getAccessGroup().getExpression(), oldMenuFunctionality.getDescription(),
                oldMenuFunctionality.getTitle(), oldMenuFunctionality.getPath(), oldMenuFunctionality.getDocumentationUrl(),
                oldMenuFunctionality.getTarget());

    }

    private void updateMenuFunctionality(MenuFunctionality oldMenuFunctionality, MenuContainer parent, String string,
            String provider, String expression, LocalizedString description, LocalizedString title, String path,
            String documentationUrl, String target) {
        oldMenuFunctionality.delete();
        new MenuFunctionality(parent, true, string, provider, expression, description, title, path, documentationUrl, target);

    }

}