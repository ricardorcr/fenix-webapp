package pt.ist.fenix.webapp.task.documentSigning;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.jsonwebtoken.SignatureAlgorithm;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.fenixedu.bennu.core.util.CoreConfiguration;
import org.fenixedu.bennu.io.domain.DriveAPIStorage;
import org.fenixedu.bennu.io.domain.FileSupport;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.jwt.Tools;

public class SentToSignerFromDrive extends ReadCustomTask {

    final String USERNAME = "fenix";
    final String SLUG = "assuntos-academicos/uploadsigneraageral";

    private String accessToken = accessToken(USERNAME);

    @Override
    public void runTask() throws Exception {
        final JsonObject dir = readDirectory(SLUG);
        for (final JsonElement e : dir.getAsJsonArray("items")) {
            final JsonObject item = e.getAsJsonObject();
            final String name = item.get("name").getAsString();
            final String downloadLink = item.get("downloadLink").getAsString();
            final byte[] content = read(downloadLink);
            taskLog("%s = %s bytes%n", name, content.length);
        }
    }

    private JsonObject readDirectory(final String slug) {
        final HttpResponse<String> response = Unirest.get(getDriveUrl() + "/api/drive/directory/")
                .queryString("slug", slug)
                .header("Authorization", "Bearer " + accessToken)
                .header("X-Requested-With", "XMLHttpRequest")
                .asString();
        return new JsonParser().parse(response.getBody()).getAsJsonObject();
    }

    public byte[] read(final String downloadLink) {
        HttpResponse<byte[]> response = Unirest.get(downloadLink
                //getDriveUrl() + "/api/drive/file/" + file.getContentKey() + "/download"
                )
                .header("Authorization", "Bearer " + accessToken)
                .asBytes();
        if (response.getStatus() == 307) {
            response = Unirest.get(response.getHeaders().getFirst("Location")).asBytes();
        }
        return response.getBody();
    }
    public String getDriveUrl() {
        final DriveAPIStorage driveAPIStorage = FileSupport.getInstance().getFileStorageSet().stream()
                .filter(DriveAPIStorage.class::isInstance)
                .map(DriveAPIStorage.class::cast)
                .findAny().orElse(null);
        if (driveAPIStorage == null) {
            throw new Error("No DriveAPIStorage configured.");
        }
        return driveAPIStorage.getDriveUrl();
    }

    private String accessToken(final String username) {
        final JsonObject claim = new JsonObject();
        claim.addProperty("username", username);
        return Tools.sign(SignatureAlgorithm.RS256, CoreConfiguration.getConfiguration().jwtPrivateKeyPath(), claim);
    }

}