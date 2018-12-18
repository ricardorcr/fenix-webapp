package pt.ist.fenix.webapp;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

public class DownloadCertifiedDeclaration extends CustomTask {

    @Override
    public void runTask() throws Exception {
        output("declaration.pdf",downloadCertified("29d626e0-cd81-4629-a1e7-d98b05da5c7c"));
    }

    byte[] downloadCertified(final String uuid) {
        HttpResponse<byte[]> response = Unirest.get("https://certifier.tecnico.ulisboa.pt/" + uuid + "/download")
                .asBytes();
        if (response.getStatus() == 307) {
            response = Unirest.get(response.getHeaders().getFirst("Location")).asBytes();
        }
        return response.getBody();
    }
}
