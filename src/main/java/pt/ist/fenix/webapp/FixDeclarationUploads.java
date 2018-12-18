package pt.ist.fenix.webapp;

import com.google.gson.JsonObject;
import io.jsonwebtoken.SignatureAlgorithm;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.util.CoreConfiguration;
import org.fenixedu.bennu.io.domain.DriveAPIStorage;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.bennu.spring.BennuSpringContextHelper;
import org.fenixedu.jwt.Tools;
import org.springframework.web.multipart.MultipartFile;
import pt.ist.fenixframework.FenixFramework;
import pt.ist.registration.process.domain.RegistrationDeclarationFile;
import pt.ist.registration.process.domain.RegistrationDeclarationFileState;
import pt.ist.registration.process.ui.service.SignCertAndStoreService;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class FixDeclarationUploads extends ReadCustomTask {

    private final SignCertAndStoreService signCertAndStoreService = BennuSpringContextHelper.getBean(SignCertAndStoreService.class);
    private static final String prefix = "https://drive.tecnico.ulisboa.pt/downloadFile/";

    @Override
    public void runTask() throws Exception {
        final DriveAPIStorage storage = Bennu.getInstance().getFileSupport().getFileStorageSet().stream()
                .filter(DriveAPIStorage.class::isInstance)
                .map(DriveAPIStorage.class::cast)
                .findAny().get();
        final String driveUrl = storage.getDriveUrl();

        ExecutionSemester.readActualExecutionSemester().getEnrolmentsSet().stream()
                .map(enrolment -> enrolment.getRegistration())
                .distinct()
                .flatMap(registration -> registration.getRegistrationDeclarationFileSet().stream())
                .filter(registrationDeclarationFile -> registrationDeclarationFile.getExecutionYear().isCurrent())
                .filter(registrationDeclarationFile -> registrationDeclarationFile.getState() == RegistrationDeclarationFileState.STORED)
                .forEach(registrationDeclaration -> fix(driveUrl, registrationDeclaration));
    }

    private void fix(final String driveUrl, final RegistrationDeclarationFile registrationDeclarationFile) {
        final String link = registrationDeclarationFile.getDownloadSignedFileLink();
        final int i = link.indexOf('/', prefix.length() + 1);
        final String driveOid = link.substring(prefix.length(), i);
        final User user = registrationDeclarationFile.getRegistration().getPerson().getUser();
        final byte[] content = download(driveUrl, user, driveOid);
        if (((int) registrationDeclarationFile.getSize().longValue() == content.length)) {
            taskLog("Fixing declaration for: %s = %s%n", user.getUsername(), registrationDeclarationFile.getDisplayName());

            final String uuid = registrationDeclarationFile.getUniqueIdentifier();
            final MultipartFile file = getMultipartFile(downloadCertified(uuid));
            FenixFramework.atomic(() -> {
                registrationDeclarationFile.setState(RegistrationDeclarationFileState.CERTIFIED);
            });
            signCertAndStoreService.sendDocumentToBeStoredWithJob(registrationDeclarationFile.getRegistration(),
                    registrationDeclarationFile, file);
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

    byte[] download(final String driveUrl, final User user, final String remoteFileId) {
        //HttpResponse<byte[]> response = Unirest.get(driveUrl + "/api/drive/file/" + remoteFileId + "/download")
        HttpResponse<byte[]> response = Unirest.get(driveUrl + "/downloadFile/" + remoteFileId + "/download.pdf")
                .header("Authorization", "Bearer " + accessToken(user))
                .asBytes();
        if (response.getStatus() == 307) {
            response = Unirest.get(response.getHeaders().getFirst("Location")).asBytes();
        }
        return response.getBody();
    }

    private String accessToken(final User user) {
        final JsonObject claim = new JsonObject();
        claim.addProperty("username", user.getUsername());
        return Tools.sign(SignatureAlgorithm.RS256, CoreConfiguration.getConfiguration().jwtPrivateKeyPath(), claim);
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