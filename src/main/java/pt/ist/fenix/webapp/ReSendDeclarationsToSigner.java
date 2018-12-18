package pt.ist.fenix.webapp;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.bennu.spring.BennuSpringContextHelper;
import org.joda.time.YearMonthDay;
import pt.ist.registration.process.domain.RegistrationDeclarationFile;
import pt.ist.registration.process.domain.RegistrationDeclarationFileState;
import pt.ist.registration.process.ui.service.RegistrationProcessDeclarationsService;
import pt.ist.registration.process.ui.service.SignCertAndStoreService;

public class ReSendDeclarationsToSigner extends CustomTask {

    private final SignCertAndStoreService signCertAndStoreService = BennuSpringContextHelper.getBean(SignCertAndStoreService.class);

    @Override
    public void runTask() throws Exception {
        final YearMonthDay startDate = new YearMonthDay(2021,9,01);

        Bennu.getInstance().getRegistrationsSet().stream()
                .filter(r -> !r.getStartDate().isBefore(startDate))
                .flatMap(r -> r.getRegistrationDeclarationFileSet().stream())
                .filter(rd -> rd.getState() == RegistrationDeclarationFileState.CREATED)
                .forEach(this::sendToSigner);
    }

    private void sendToSigner(final RegistrationDeclarationFile declarationFile) {
        taskLog("Envio para %s\tde %s%n", declarationFile.getRegistration().getNumber(), declarationFile.getDisplayName());
        String filename = declarationFile.getFilename();
        String title = declarationFile.getDisplayName();
        RegistrationProcessDeclarationsService registrationProcessDeclarationsService = new RegistrationProcessDeclarationsService();
        String queue = registrationProcessDeclarationsService.getQueue(declarationFile.getRegistration());
        String externalIdentifier = declarationFile.getUniqueIdentifier();

        signCertAndStoreService.sendDocumentToBeSigned(declarationFile.getRegistration().getExternalId(), queue, title, title, filename,
                declarationFile.getStream(), externalIdentifier);

        if (declarationFile.getState() == RegistrationDeclarationFileState.CREATED) {
            declarationFile.updateState(RegistrationDeclarationFileState.PENDING);
        }
    }
}
