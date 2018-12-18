package pt.ist.fenix.webapp;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.bennu.spring.BennuSpringContextHelper;
import org.joda.time.DateTime;
import org.springframework.web.multipart.MultipartFile;

import pt.ist.fenixframework.Atomic;
import pt.ist.registration.process.domain.RegistrationDeclarationFile;
import pt.ist.registration.process.domain.RegistrationDeclarationFileState;
import pt.ist.registration.process.ui.service.SignCertAndStoreService;

public class SendCertifiedDocToDrive extends CustomTask {

    private final SignCertAndStoreService signCertAndStoreService = BennuSpringContextHelper.getBean(SignCertAndStoreService.class);
    private MultipartFile file = null;

    @Override
    public Atomic.TxMode getTxMode() {
        return Atomic.TxMode.READ;
    }

    @Override
    public void runTask() throws Exception {

        final ExecutionYear currentYear = ExecutionYear.readCurrentExecutionYear();
        final DateTime yesterday = new DateTime().withHourOfDay(03).minusDays(2);

        Bennu.getInstance().getRegistrationsSet().stream()
                .filter(r -> r.getStartExecutionYear() == currentYear)
                .flatMap(r -> r.getRegistrationDeclarationFileSet().stream())
//                .filter(rd -> rd.getCreationDate().isBefore(yesterday))
                .filter(rd -> rd.getState() == RegistrationDeclarationFileState.CERTIFIED)
//                .filter(rd -> rd.getRegistration().getNumber().equals(100408))
                .forEach(this::sendDocumentToDrive);
//                .forEach(rd -> taskLog("Registration Declaration %s of student %s will be sent to Drive%n",
//                        rd.getUniqueIdentifier(), rd.getRegistration().getNumber()));
    }

    private void sendDocumentToDrive(final RegistrationDeclarationFile declarationFile) {
        if (declarationFile.getState() != RegistrationDeclarationFileState.STORED) {
            taskLog("Sending Registration Declaration %s of student %s to Drive%n",
                    declarationFile.getUniqueIdentifier(), declarationFile.getRegistration().getNumber());
            file = getMultipartFile(declarationFile);
            signCertAndStoreService.sendDocumentToBeStoredWithJob(declarationFile.getRegistration(), declarationFile, file);
        }
    }

    private MultipartFile getMultipartFile(final RegistrationDeclarationFile declarationFile) {
        return new MultipartFile() {
            @Override
            public String getName() {
                return null;
            }

            @Override
            public String getOriginalFilename() {
                return null;
            }

            @Override
            public String getContentType() {
                return "application/pdf";
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public long getSize() {
                return 0;
            }

            @Override
            public byte[] getBytes() throws IOException {
                return new byte[0];
            }

            @Override
            public InputStream getInputStream() throws IOException {
                return declarationFile.getStream();
            }

            @Override
            public void transferTo(final File file) throws IOException, IllegalStateException {

            }
        };
    }
}

