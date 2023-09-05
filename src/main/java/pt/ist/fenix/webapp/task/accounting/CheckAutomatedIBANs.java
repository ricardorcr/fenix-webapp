package pt.ist.fenix.webapp.task.accounting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.jsonwebtoken.SignatureAlgorithm;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.ehcache.shadow.org.terracotta.statistics.TableSkeleton;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.core.util.CoreConfiguration;
import org.fenixedu.bennu.io.domain.DriveAPIStorage;
import org.fenixedu.bennu.io.domain.FileSupport;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.jwt.Tools;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class CheckAutomatedIBANs extends ReadCustomTask {

    final String USERNAME = "fenix";
    final String INPUT_DIRECTORY = "santander/pagamentos-iban-dedicado/processados";
    private String accessToken = accessToken(USERNAME);
    private List<Money> possibleValues = new ArrayList<>();

    @Override
    public void runTask() throws Exception {
        Map<String, Integer> statistics = new HashMap<>();
        statistics.put("automatic", 0);
        statistics.put("manual", 0);

        possibleValues = Arrays.asList(new Money(69.70),new Money(2.03),new Money(30),new Money(15),new Money(100),
                new Money(120),new Money(350),new Money(7000),new Money(3500),new Money(1200),new Money(700));

        final JsonObject dir = readDirectory(INPUT_DIRECTORY);
        for (final JsonElement e : dir.getAsJsonArray("items")) {
            final JsonObject item = e.getAsJsonObject();
            final String name = item.get("name").getAsString();
            final String downloadLink = item.get("downloadLink").getAsString();
            if (name.endsWith(".txt")) {
                final String content = read(downloadLink);
                final List<String> allLines = Arrays.asList(content.replaceAll("\r", "").split("\n"));
                processFile(allLines, statistics);
            }
        }
        statistics.forEach((k,v) -> taskLog("%s\t%s%n", k, v));
    }

    private void processFile(final List<String> content, final Map<String, Integer> statistics) {
        FenixFramework.atomic(() -> {
                checkIBANs(content, statistics);
        });
    }

    private JsonObject readDirectory(final String slug) {
        final HttpResponse<String> response = Unirest.get(getDriveUrl() + "/api/drive/directory/")
                .queryString("slug", slug)
                .header("Authorization", "Bearer " + accessToken)
                .header("X-Requested-With", "XMLHttpRequest")
                .asString();
        return new JsonParser().parse(response.getBody()).getAsJsonObject();
    }

    public String read(final String downloadLink) {
        HttpResponse<String> response = Unirest.get(downloadLink
                        //getDriveUrl() + "/api/drive/file/" + file.getContentKey() + "/download"
                )
                .header("Authorization", "Bearer " + accessToken)
                .asString();
        if (response.getStatus() == 307) {
            response = Unirest.get(response.getHeaders().getFirst("Location")).asString();
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

    @Atomic
    private List<String> checkIBANs(final List<String> allLines, Map<String, Integer> statistics) {
        final Iterator<String> iter = allLines.iterator();
        List<String> notFoundReferences = new ArrayList<>();
        while (iter.hasNext()) {
            final String line = iter.next();
            if (line.startsWith(":25:")) {
                final String[] split = line.split(":25:");
                continue;
            }
            if (line.startsWith(":61:")) {
                final String secondLine = iter.next();
                if (secondLine.startsWith(":86:TRF REF:")) {
                    statistics.put("automatic", statistics.get("automatic") + 1);
                    continue;
                }
                final int indexOf = line.indexOf("NMSCNONREF");
                final Money amount = new Money(line.substring(15, indexOf).replace(",", "."));

                if (isProbable(amount)) {
                    statistics.put("manual", statistics.get("manual") + 1);
//                    taskLog("%s%n%s%n", amount.toPlainString(), line);
                }
            }
        }
        return notFoundReferences;
    }

    private boolean isProbable(final Money amount) {
        for (Money possibleValue : possibleValues) {
            if (possibleValue.equals(amount) || amount.getAmount().remainder(possibleValue.getAmount()) == BigDecimal.ZERO) {
                return true;
            }
        }
        return false;
    }
}
