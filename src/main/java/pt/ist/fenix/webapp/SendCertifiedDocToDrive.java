package pt.ist.fenix.webapp;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.bennu.spring.BennuSpringContextHelper;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.web.multipart.MultipartFile;
import pt.ist.fenixframework.Atomic;
import pt.ist.registration.process.domain.RegistrationDeclarationFile;
import pt.ist.registration.process.domain.RegistrationDeclarationFileState;
import pt.ist.registration.process.ui.service.SignCertAndStoreService;

public class SendCertifiedDocToDrive extends CustomTask {

    private final SignCertAndStoreService signCertAndStoreService = BennuSpringContextHelper.getBean(SignCertAndStoreService.class);

    @Override
    public Atomic.TxMode getTxMode() {
        return Atomic.TxMode.READ;
    }

    @Override
    public void runTask() throws Exception {

        ExecutionSemester.readActualExecutionSemester().getEnrolmentsSet().stream()
                .map(enrolment -> enrolment.getRegistration())
                .distinct()
//                .filter(r -> r.getNumber() == 104659)
                .flatMap(registration -> registration.getRegistrationDeclarationFileSet().stream())
//                .filter(rd -> rd.getCreationDate().isBefore(yesterday))
                .filter(registrationDeclarationFile -> registrationDeclarationFile.getExecutionYear().isCurrent())
                .filter(rd -> rd.getState() == RegistrationDeclarationFileState.CERTIFIED)
                .forEach(this::sendDocumentToDrive);
//                .forEach(rd -> taskLog("Registration Declaration %s of student %s will be sent to Drive%n",
//                        rd.getUniqueIdentifier(), rd.getRegistration().getNumber()));
    }

    private void sendDocumentToDrive(final RegistrationDeclarationFile declarationFile) {
        if (declarationFile.getState() != RegistrationDeclarationFileState.STORED) {
            taskLog("Sending Registration Declaration %s of student %s to Drive%n",
                    declarationFile.getUniqueIdentifier(), declarationFile.getRegistration().getNumber());

            final String uuid = declarationFile.getUniqueIdentifier();
            final MultipartFile file = getMultipartFile(downloadCertified(uuid));
            signCertAndStoreService.sendDocumentToBeStoredWithJob(declarationFile.getRegistration(), declarationFile, file);
        }
    }

    byte[] downloadCertified(final String uuid) {
        HttpResponse<byte[]> response = Unirest.get("https://certifier.tecnico.ulisboa.pt/" + uuid + "/download")
                .asBytes();
        if (response.getStatus() == 307) {
            response = Unirest.get(response.getHeaders().getFirst("Location")).asBytes();
        }
        return response.getBody();
    }

    private MultipartFile getMultipartFile(final byte[] content) {
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
                return new ByteArrayInputStream(content);
            }

            @Override
            public void transferTo(final File file) throws IOException, IllegalStateException {

            }
        };
    }
}

