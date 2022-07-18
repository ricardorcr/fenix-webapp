package pt.ist.fenix.webapp.servlet;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.SchoolLevelType;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.admissions.ist.util.QualificationLevelUtil;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.json.JsonUtils;
import org.fenixedu.bennu.core.util.CoreConfiguration;
import org.fenixedu.bennu.core.util.TransactionalThread;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.connect.domain.Account;
import org.fenixedu.connect.domain.ConnectSystem;
import org.fenixedu.connect.domain.Identity;
import org.fenixedu.connect.domain.identification.PersonalInformation;
import org.fenixedu.connect.domain.identification.TaxInformation;
import org.fenixedu.git.Repository;
import org.fenixedu.ulisboa.integration.sas.service.process.AbstractFillScholarshipService;
import pt.ist.fenix.webapp.Configuration;
import pt.ist.fenixedu.giaf.invoices.SapEvent;
import pt.ist.fenixedu.giaf.invoices.Utils;
import pt.ist.fenixframework.FenixFramework;
import pt.ist.standards.geographic.Planet;
import pt.ist.standards.geographic.PostalCode;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@WebListener
public class FenixIstWebAppListener implements ServletContextListener, Configuration {

    private SchoolLevelType toSchoolLevelType(final String qualificationLevel) {
        if (qualificationLevel == null) {
            return SchoolLevelType.UNKNOWN;
        }
        if (qualificationLevel.equals("qualificationLevel1")) {
            return SchoolLevelType.FIRST_CYCLE_BASIC_SCHOOL;
        }
        if (qualificationLevel.equals("qualificationLevel2")) {
            return SchoolLevelType.THIRD_CYCLE_BASIC_SCHOOL;
        }
        if (qualificationLevel.equals("qualificationLevel3")) {
            return SchoolLevelType.HIGH_SCHOOL_OR_EQUIVALENT;
        }
        if (qualificationLevel.equals("qualificationLevel4")) {
            return SchoolLevelType.TECHNICAL_SPECIALIZATION;
        }
        if (qualificationLevel.equals("qualificationLevel5")) {
            return SchoolLevelType.MEDIUM_EDUCATION;
        }
        if (qualificationLevel.equals("qualificationLevel6")) {
            return SchoolLevelType.BACHELOR_DEGREE;
        }
        if (qualificationLevel.equals("qualificationLevel7")) {
            return SchoolLevelType.MASTER_DEGREE;
        }
        if (qualificationLevel.equals("qualificationLevel8")) {
            return SchoolLevelType.DOCTORATE_DEGREE;
        }
        return null;
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        FenixFramework.atomic(() -> {
            AbstractFillScholarshipService.completedQualificationsSchoolLevelTypeSupplier = (person) -> {
                final Student student = person.getStudent();
                return Stream.concat(
                        student.getPersonalIngressionsDataSet().stream()
                                .flatMap(x -> x.getPrecedentDegreesInformationsSet().stream())
                                .map(x -> x.getSchoolLevel()),
                        person.getUser().getIdentity().getAccountSet().stream()
                                .flatMap(account -> account.getApplicationSet().stream())
                                .map(QualificationLevelUtil::maxCompletedQualificationLevel)
                                .map(this::toSchoolLevelType)
                ).collect(Collectors.toSet());
            };
        });

        if (!CoreConfiguration.getConfiguration().developmentMode()) {
            final Properties properties = loadProperties();
            final String url = properties.getProperty("scheduler.git.repo.url");
            if (url != null && !url.trim().isEmpty()) {
                final Repository repository = new Repository(url,
                        properties.getProperty("scheduler.git.repo.dir"),
                        properties.getProperty("scheduler.git.repo.username"),
                        properties.getProperty("scheduler.git.repo.password"));

                CustomTask.registerHandler(customTask -> {
                    final Path path = toPath(repository, customTask);
                    final byte[] content = customTask.getSourceCode().getBytes();
                    final byte[] current = read(path);
                    if (!Arrays.equals(current, content)) {
                        final String[] info = new String[2];
                        TransactionalThread.runTx(true, () -> {
                            final User user = User.findByUsername(customTask.getTaskRunner());
                            info[0] = user.getDisplayName();
                            info[1] = user.getEmail();
                        });

                        write(path, content);
                        repository.addCommitAndPush("Run CustomTask " + customTask.getClass().getSimpleName(),
                                info[0], info[1]);
                    }
                });
            }
        }

        Event.registerPersonalInformationCheck(person -> {
            final PersonalInformation personalInformation = personalInformationFor(person);
            if (personalInformation == null) {
                throw new Error("Unable to get valid address information. No personal information found for person.");
            }

            final TaxInformation taxInformation = personalInformation.getTaxInformation();
            if (taxInformation == null) {
                throw new Error("Unable to find tax information for person.");
            }

            if (!taxInformation.isValid()) {
                throw new Error("Tax information for person is not valid.");
            }
        });

        SapEvent.ADDRESS_FILLER = (party, clientData) -> {
            final String clientId = clientData.get("clientId").getAsString();
            final String countryCode = clientId.substring(0, 2);

            final PersonalInformation personalInformation = personalInformationFor((Person) party);
            if (personalInformation == null) {
                throw new Error("Unable to get valid address information. No personal information found for person.");
            }

            final TaxInformation taxInformation = personalInformation.getTaxInformation();
            if (taxInformation == null) {
                throw new Error("Unable to find tax information for person.");
            }

            if (!taxInformation.isValid()) {
                throw new Error("Tax information for person is not valid.");
            }

            final JsonObject address = new JsonParser().parse(taxInformation.getAddressData()).getAsJsonObject();
            final String line1 = JsonUtils.get(address, "firstLine");
            final String line2 = JsonUtils.get(address, "secondLine");
            final String street = line2 == null ? line1 : (line1 + ", " + line2);
            final String location = JsonUtils.get(address, "location");
            final String zipCode = JsonUtils.get(address, "zipCode");
            final String district = districtFor(countryCode, zipCode);

            clientData.addProperty("street", Utils.limitFormat(SapEvent.MAX_SIZE_ADDRESS, street));
            clientData.addProperty("city", Utils.limitFormat(SapEvent.MAX_SIZE_CITY, location));
            clientData.addProperty("region", district == null ? SapEvent.MORADA_DESCONHECIDO
                    : Utils.limitFormat(SapEvent.MAX_SIZE_REGION, district));
            clientData.addProperty("postalCode", zipCode);
        };
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }

    private Path toPath(final Repository repository, final CustomTask customTask) {
        final String dirPath = repository.localDir().getAbsolutePath()
                + "/src/main/tasks/".replace('/', File.separatorChar)
                + customTask.getClass().getPackage().getName().replace('.', File.separatorChar);
        final File dir = new File(dirPath);
        dir.mkdirs();
        final String classname = customTask.getClass().getSimpleName();
        final String filePath = dirPath + File.separatorChar + classname + ".java";
        return new File(filePath).toPath();
    }

    private byte[] read(final Path path) {
        if (!path.toFile().exists()) {
            return null;
        }
        try {
            return Files.readAllBytes(path);
        } catch (final IOException ex) {
            throw new Error(ex);
        }
    }

    private void write(final Path path, final byte[] content) {
        try {
            Files.write(path, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (final IOException e) {
            throw new Error(e);
        }
    }

    private PersonalInformation personalInformationFor(final Person person) {
        final User user = person.getUser();
        final Identity identity = user == null ? null : user.getIdentity();
        Account account = identity == null ? null : ConnectSystem.getMostRelevantAccount(identity);
        if (account == null && user != null) {
            account = user.getAccount();
        }
        if (account == null) {
            account = findByEvent(person);
        }
        return account == null ? null : ConnectSystem.getPersonalInformationFor(account);
    }

    private Account findByEvent(final Person person) {
        return person.getEventsSet().stream()
                .flatMap(event -> event.getApplicationSet().stream())
                .filter(application -> application != null)
                .map(application -> application.getAccount())
                .findAny().orElse(null);
    }

    private String districtFor(final String countryCode, final String zipCode) {
        final PostalCode postalCode = Planet.getEarth().getByAlfa2(countryCode).getPostalCode(zipCode);
        final JsonObject details = postalCode.getDetails();
        final String distrito = details == null ? null : JsonUtils.get(details, "Distrito");
        return distrito;
    }

}