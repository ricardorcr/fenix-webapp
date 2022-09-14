package pt.ist.fenix.webapp;

import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.registration.process.domain.DeclarationTemplate;

import java.util.Locale;

public class Teste extends CustomTask {

    private Locale PT = Locale.forLanguageTag("pt-PT");
    private Locale EN = Locale.forLanguageTag("en-GB");

    @Override
    public void runTask() throws Exception {
        DeclarationTemplate ptDeclaration = getTemplateFor("declaracao-matricula", PT);
        DeclarationTemplate enDeclaration = getTemplateFor("declaracao-matricula", EN);

        taskLog("Declaração PT existe: %s%n", ptDeclaration.getExternalId());
        taskLog("Declaração EN existe: %s%n", enDeclaration.getExternalId());
    }

    private DeclarationTemplate getTemplateFor(String name, Locale locale) {
        return DeclarationTemplate.findByNameAndLocale(name, locale).get();
    }
}