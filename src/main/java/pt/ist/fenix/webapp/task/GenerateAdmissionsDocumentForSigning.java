package pt.ist.fenix.webapp.task;

import com.google.common.io.ByteStreams;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.forms.fields.PdfSignatureFormField;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.annot.PdfAnnotation;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.calculator.DebtInterestCalculator;
import org.fenixedu.academic.domain.candidacy.IngressionType;
import org.fenixedu.academic.domain.student.RegistrationProtocol;
import org.fenixedu.admissions.domain.AdmissionProcessTarget;
import org.fenixedu.admissions.domain.AdmissionsSystem;
import org.fenixedu.admissions.domain.Application;
import org.fenixedu.admissions.ist.domain.Utils;
import org.fenixedu.admissions.util.DynamicForm;
import org.fenixedu.bennu.RegistrationProcessConfiguration;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.rest.JsonBodyReaderWriter;
import org.fenixedu.bennu.core.util.CoreConfiguration;
import org.fenixedu.bennu.papyrus.domain.SignatureFieldSettings;
import org.fenixedu.bennu.papyrus.service.ITextQRCodeGenerator;
import org.fenixedu.bennu.scheduler.CronTask;
import org.fenixedu.bennu.scheduler.annotation.Task;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.commons.stream.StreamUtils;
import org.fenixedu.connect.domain.Account;
import org.fenixedu.connect.domain.ConnectSystem;
import org.fenixedu.connect.domain.Identity;
import org.fenixedu.connect.domain.identification.IdentificationDocument;
import org.fenixedu.connect.domain.identification.PersonalInformation;
import org.fenixedu.connect.domain.identification.TaxInformation;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.StreamDataBodyPart;
import org.joda.time.DateTime;
import pt.ist.fenix.webapp.Configuration;
import pt.ist.fenixframework.FenixFramework;
import pt.ist.papyrus.PapyrusClient;
import pt.ist.registration.process.handler.CandidacySignalHandler;
import pt.ist.standards.geographic.Country;
import pt.ist.standards.geographic.Planet;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;

@Task(englishTitle = "Auto Generate Declarations for International Students", readOnly = true)
public class GenerateAdmissionsDocumentForSigning extends CronTask implements Configuration {

    private static final String TEMPLATE_ID = "admissions-international-students-admitted";
    private static final String TEMPLATE_MOBILITY_ID = "admissions-mobility-students-admitted";
    private static final String SIGNING_QUEUE = "xNCC1IYd";
    private static final String LOG_FILE = "/afs/ist.utl.pt/ciist/fenix/fenix036/admissions-international-students-admitted.log";

    private static final Locale PT = new Locale("pt");
    private static final Locale EN = new Locale("en");

    @Override
    public void runTask() throws Exception {
//        final Set<String> processed = load(LOG_FILE);
//        try {
//            AdmissionsSystem.getInstance().getAdmissionProcessSet().stream()
//                    .flatMap(admissionProcess -> admissionProcess.getAdmissionProcessTargetSet().stream())
//                    .flatMap(admissionProcessTarget -> admissionProcessTarget.getApplicationSet().stream())
//                    .filter(application -> application.getAdmitted())
//                    .forEach(application -> {
//                        final boolean isPayed = isPayed(application);
//                        final boolean isMobility = isMobilityApplication(application);
//                        if (isMobility || isPayed) {
//                            final String hash = calculateHashFor(application);
//                            if (!processed.contains(hash)) {
//                                try {
//                                    process(application, isMobility ? TEMPLATE_MOBILITY_ID : TEMPLATE_ID);
//                                    processed.add(hash);
//                                } catch (final Throwable t) {
//                                    final ByteArrayOutputStream stream = new ByteArrayOutputStream();
//                                    final PrintStream printStream = new PrintStream(stream);
//                                    t.printStackTrace(printStream);
//                                    taskLog("Failled to process: %s : %s%n    %s%n", application.getExternalId(), t.getMessage(), new String(stream.toByteArray()));
//                                }
//                            }
//                        }
//                    });
//        } finally {
//            write(LOG_FILE, processed);
//        }
//    }
//
//    private boolean isMobilityApplication(final Application application) {
//        final String outcomeType = application.getAdmissionProcessTarget().getAdmissionProcess().getOutcomeTypeJson().get("name").getAsString();
//        if (outcomeType.equalsIgnoreCase("mobilityInbound") || outcomeType.equalsIgnoreCase("mobilityInboundDoubleDegree")) {
//            final JsonObject data = application.getDataObject();
//            final JsonElement element = data.get("registration");
//            return element != null && element.isJsonNull()
//                    && !isSchengen(ConnectSystem.getPersonalInformationFor(application.getAccount()).getNationalityCountryCode());
//        }
//        return false;
//    }
//
//    private final Set<String> schengen = new HashSet<>();
//
//    {
//        schengen.add("AT");
//        schengen.add("BE");
//        schengen.add("BG");
//        schengen.add("CH");
//        schengen.add("CY");
//        schengen.add("CZ");
//        schengen.add("DE");
//        schengen.add("DK");
//        schengen.add("EE");
//        schengen.add("EL");
//        schengen.add("ES");
//        schengen.add("FI");
//        schengen.add("FR");
//        schengen.add("HR");
//        schengen.add("HU");
//        schengen.add("IE");
//        schengen.add("IS");
//        schengen.add("IT");
//        schengen.add("LI");
//        schengen.add("LT");
//        schengen.add("LU");
//        schengen.add("LV");
//        schengen.add("MT");
//        schengen.add("NL");
//        schengen.add("NO");
//        schengen.add("PL");
//        schengen.add("PT");
//        schengen.add("RO");
//        schengen.add("SE");
//        schengen.add("SI");
//        schengen.add("SK");
//        schengen.add("UK");
//    }
//
//    private boolean isSchengen(final String country) {
//        return schengen.contains(country);
//    }
//
//    private String calculateHashFor(final Application application) {
//        final PersonalInformation personalInformation = ConnectSystem.getPersonalInformationFor(application.getAccount());
//        final IdentificationDocument identificationDocument = personalInformation.getIdentificationDocument();
//        final StringBuilder builder = new StringBuilder();
//        builder.append(application.getExternalId());
//        builder.append(personalInformation.getFullName());
//        builder.append(personalInformation.getDateOfBirth().toString("yyyy-MM-dd"));
//        builder.append(identificationDocument.getDocumentNumber());
//        builder.append(personalInformation.getNationalityCountryCode());
//        return Base64.getEncoder().encodeToString(builder.toString().getBytes());
//    }
//
//    private boolean isPayed(final Application application) {
//        final JsonObject data = application.getDataObject();
//        final JsonElement e = data.get("gratuityEvent");
//        if (e != null && !e.isJsonNull()) {
//            final Event event = FenixFramework.getDomainObject(e.getAsString());
//            final DebtInterestCalculator calculator = event.getDebtInterestCalculator(new DateTime());
//            final BigDecimal totalAmount = calculator.getTotalAmount();
//            final BigDecimal paidDebtAmount = calculator.getPaidDebtAmount();
//            final double ratio = paidDebtAmount.divide(totalAmount, 2, RoundingMode.HALF_EVEN).doubleValue();
//            return ratio > 0.28d && Utils.registrationFor(application).getRegistrationProtocol().isAlien();
//        }
//        return false;
//    }
//
//    private void process(final Application application, final String templateId) {
//        final String uuid = UUID.randomUUID().toString();
//        final byte[] document = generate(templateId, data(application, uuid), PT);
//        final String title = titleFor(application);
//        final User user = user(application);
//        sendDocumentToBeSigned(SIGNING_QUEUE, title, title, title + ".pdf", new ByteArrayInputStream(document), uuid, user == null
//                ? "ist24439" : user.getUsername());
//        FenixFramework.atomic(() -> {
//            final JsonObject data = application.getDataObject();
//            JsonArray documents = data.getAsJsonArray("documents");
//            if (documents == null || documents.isJsonNull()) {
//                documents = new JsonArray();
//                data.add("documents", documents);
//            }
//            final JsonObject jdoc = new JsonObject();
//            documents.add(jdoc);
//            jdoc.addProperty("url", "https://certifier.tecnico.ulisboa.pt/" + uuid + "/download");
//            jdoc.addProperty("title", "Admission Declaration");
//            application.setData(data.toString());
//        });
//    }
//
//    private User user(final Application application) {
//        final Identity identity = application.getAccount().getIdentity();
//        return identity.getUser();
//    }
//
//    private String titleFor(final Application application) {
//        final User user = user(application);
//        final String id = user == null ? "nau" : user.getUsername();
//        return id + "_" + application.getExternalId();
//    }
//
//    private JsonObject data(final Application application, final String uuid) {
//        final Account account = application.getAccount();
//        final PersonalInformation personalInformation = ConnectSystem.getPersonalInformationFor(account);
//        final IdentificationDocument identificationDocument = personalInformation.getIdentificationDocument();
//        final TaxInformation taxInformation = personalInformation.getTaxInformation();
//        final Country nationality = Planet.getEarth().getByAlfa2(personalInformation.getNationalityCountryCode());
//        final AdmissionProcessTarget target = application.getAdmissionProcessTarget();
//        final JsonObject outcomeConfigJson = target.getOutcomeConfigJson();
//        final Degree degree = FenixFramework.getDomainObject(outcomeConfigJson.get("degree").getAsString());
//        final ExecutionYear executionYear = FenixFramework.getDomainObject(outcomeConfigJson.get("year").getAsString());
//        final LocalizedString degreeName = degree.getPresentationNameI18N(executionYear);
//        final RegistrationProtocol protocol = FenixFramework.getDomainObject(outcomeConfigJson.get("protocol").getAsString());
//        final IngressionType ingressionType = ingressionTypeFor(outcomeConfigJson, application);
//
//        final JsonObject result = new JsonObject();
//        result.addProperty("degreePT", degreeName.getContent(PT));
//        result.addProperty("degreeEN", degreeName.getContent(EN));
//        result.addProperty("name", personalInformation.getFullName());
//        result.addProperty("dateOfBirth", personalInformation.getDateOfBirth().toString("yyyy-MM-dd"));
//        result.addProperty("gender", personalInformation.getGender() == null ? "" : personalInformation.getGender().name());
//        result.addProperty("nationalityPT", nationality.getNationality(PT));
//        result.addProperty("nationalityEN", nationality.getNationality(EN));
//        result.addProperty("documentTypePT", getIdentificationDocumentName(identificationDocument, PT));
//        result.addProperty("documentTypeEN", getIdentificationDocumentName(identificationDocument, EN));
//        result.addProperty("docUmentCountry", Planet.getEarth().getByAlfa2(identificationDocument.getCountryCode()).getLocalizedName(EN));
//        result.addProperty("documentNumber", identificationDocument.getDocumentNumber());
//        result.addProperty("documenExpirationDate", identificationDocument.getExpirationDate().toString("yyyy-MM-dd"));
//        result.addProperty("tin", taxInformation == null ? "n/a" : taxInformation.getTin());
//        result.addProperty("protocolPT", protocol == null ? "-" : protocol.getDescription().getContent(PT));
//        result.addProperty("protocolEN", protocol == null ? "-" : protocol.getDescription().getContent(EN));
//        result.addProperty("ingressionTypePT", ingressionType == null ? "-" : ingressionType.getLocalizedName(PT));
//        result.addProperty("ingressionTypeEN", ingressionType == null ? "-" : ingressionType.getLocalizedName(EN));
//        result.addProperty("ingressionTypeEN", ingressionType == null ? "-" : ingressionType.getLocalizedName(EN));
//        result.addProperty("executionYear", executionYear == null ? "" : executionYear.getYear());
//        result.addProperty("executionYearStartDate", executionYear == null || executionYear.getBeginDateYearMonthDay() == null
//                ? "" : executionYear.getBeginDateYearMonthDay().toString("yyyy-MM-dd"));
//        result.addProperty("executionYearEndtDate", executionYear == null || executionYear.getEndDateYearMonthDay() == null
//                ? "" : executionYear.getEndDateYearMonthDay().toString("yyyy-MM-dd"));
//
//        final DynamicForm form = new DynamicForm(application.getAdmissionProcessTarget().getAdmissionProcess().getFormDataJson());
//        form.withData(application.getDataObject().getAsJsonObject("formData"));
//        final DynamicForm.DateTime arrivalDate = form.get("arrivalDate");
//        if (arrivalDate != null && arrivalDate.value() != null) {
//            final DateTime departureDate = ((DynamicForm.DateTime) form.get("departureDate")).value();
//            final Country homeInstitutionCountry = Planet.getEarth().getByAlfa2(((DynamicForm.AsyncSelect) form.get("homeInstitutionCountry")).value());
//            final String homeInstitutionUniversity = ((DynamicForm.Text) form.get("homeInstitutionUniversity")).value();
//
//            result.addProperty("arrivalDate", arrivalDate.value().toString("yyyy-MM-dd"));
//            result.addProperty("departureDate", departureDate.toString("yyyy-MM-dd"));
//            result.addProperty("homeInstitutionCountryPT", homeInstitutionCountry.getLocalizedName(PT));
//            result.addProperty("homeInstitutionCountryEN", homeInstitutionCountry.getLocalizedName(EN));
//            result.addProperty("homeInstitutionUniversity", homeInstitutionUniversity);
//
//
///*
//
//
//                if (outcomeConfigJson != null) {
//                    final ExecutionYear executionYear = FenixFramework.getDomainObject(outcomeConfigJson.get("year").getAsString());
//                    final Degree degree = getDegree(application.getAdmissionProcessTarget());
//
//
//                    final DegreeCurricularPlan dcp = findBestPlan(degree, executionYear);
//                    final JsonElement cycleElement = outcomeConfigJson.get("cycleType");
//                    CycleType cycleType = null;
//                    if (cycleElement != null && !cycleElement.isJsonNull()) {
//                        cycleType = CycleType.valueOf(cycleElement.getAsString());
//                    }
//                    final Registration registration = new Registration(person, dcp, protocol, cycleType, executionYear);
//
//                    if (outcomeConfigJson.get("ingressionType") != null) {
//                        final IngressionType ingressionType = FenixFramework.getDomainObject(outcomeConfigJson.get("ingressionType").getAsString());
//
// */
//
//        }
//
//        result.addProperty("uuid", uuid);
//        result.addProperty("qrcodeImage", generateURIBase64QRCode(uuid));
//
//        return result;
//    }
//
//    private IngressionType ingressionTypeFor(final JsonObject outcomeConfigJson, final Application application) {
//        if (outcomeConfigJson.get("ingressionType") != null) {
//            return FenixFramework.getDomainObject(outcomeConfigJson.get("ingressionType").getAsString());
//        } else {
//            final JsonObject data = application.getDataObject();
//            final JsonObject formData = data == null ? null : data.getAsJsonObject("formData");
//            return formData == null ? null : formData.entrySet().stream()
//                    .flatMap(page -> page.getValue().getAsJsonObject().entrySet().stream())
//                    .flatMap(section -> section.getValue().getAsJsonObject().entrySet().stream())
//                    .filter(property -> property.getKey().equals("ingressionType"))
//                    .map(property -> property.getValue().getAsJsonObject().get("value").getAsString())
//                    .map(id -> (IngressionType) FenixFramework.getDomainObject(id))
//                    .findAny().orElse(null);
//        }
//    }
//
//    public String getIdentificationDocumentName(final IdentificationDocument identificationDocument, final Locale locale) {
//        return identificationDocument.getIdentificationDocumentName().getContent(locale);
//    }
//
//    private String generateURIBase64QRCode(final String uuid) {
//        return "data:image/png;base64," + Base64.getEncoder().encodeToString((generateQRCode(uuid, 300, 300)));
//    }
//
//    private byte[] generateQRCode(final String identifier, final int width, final int height) {
//        return new ITextQRCodeGenerator().generate("https://certifier.tecnico.ulisboa.pt/" + identifier, width, height);
//    }
//
//    private JsonObject toJson(final AdmissionProcessTarget target, final Locale locale) {
//        final JsonObject result = new JsonObject();
//        result.addProperty("target", target.getName().getContent(locale));
//        result.add("admitted", target.getApplicationSet().stream()
//                .filter(application -> application.getAdmitted())
//                .map(application -> toJson(application))
//                .collect(StreamUtils.toJsonArray()));
//        result.add("notAdmitted", target.getApplicationSet().stream()
//                .filter(application -> application.getLockInstant() != null)
//                .filter(application -> application.getAccepted() != null && application.getAccepted().booleanValue())
//                .filter(application -> !application.getAdmitted())
//                .map(application -> toJson(application))
//                .collect(StreamUtils.toJsonArray()));
//        result.add("rejected", target.getApplicationSet().stream()
//                .filter(application -> application.getLockInstant() != null)
//                .filter(application -> application.getAccepted() != null && !application.getAccepted().booleanValue())
//                .map(application -> toJson(application))
//                .collect(StreamUtils.toJsonArray()));
//        return result;
//    }
//
//    private JsonObject toJson(final Application application) {
//        final JsonObject result = new JsonObject();
//        result.addProperty("name", application.getAccount().getIdentity().getPersonalInformation().getFullName());
//        return result;
//    }
//
//    private byte[] generate(final String templateID, final JsonObject data, final Locale locale) {
//        final PapyrusClient papyrusClient = new PapyrusClient();
//        final InputStream inputStream = papyrusClient.render(templateID, locale, data);
//        try {
//            final byte[] document = ByteStreams.toByteArray(inputStream);
//            final SignatureFieldSettings settings = new SignatureFieldSettings(150, 320, 550, 220, "signatureField", 1);
//            return generateDocumentWithSignatureField(new ByteArrayInputStream(document), settings);
//        } catch (final IOException ex) {
//            throw new Error(ex);
//        }
//    }
//
//    public byte[] generateDocumentWithSignatureField(final InputStream fileStream, final SignatureFieldSettings settings) {
//        if (fileStream == null) {
//            return null;
//        }
//        final Rectangle rectangle = new Rectangle(settings.getLlx(), settings.getLly(), settings.getUrx(), settings.getUry());
//        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
//        try (final PdfWriter writer = new PdfWriter(bos);
//             final PdfReader original = new PdfReader(fileStream);
//             final PdfDocument pdfDoc = new PdfDocument(original, writer)) {
//            final PdfAcroForm pdfAcroForm = PdfAcroForm.getAcroForm(pdfDoc, true);
//
//            final PdfSignatureFormField field = com.itextpdf.forms.fields.PdfFormField.createSignature(pdfDoc, rectangle);
//            field.setFieldName(settings.getName());
//            field.setPage(settings.getPage());
//            field.setFieldFlag(PdfAnnotation.PRINT);
//            field.put(PdfName.DA, new PdfString("/Helv 0 Tf 0 g"));
//            pdfAcroForm.addField(field, pdfDoc.getPage(settings.getPage()));
//
//            return bos.toByteArray();
//        } catch (final IOException e) {
//            throw new Error(e);
//        }
//    }
//
//    public void sendDocumentToBeSigned(final String queue, final String title, final String description,
//                                       final String filename, final InputStream contentStream,
//                                       final String uuid, final String username) {
//        final String compactJws = Jwts.builder()
//                .setSubject(RegistrationProcessConfiguration.getConfiguration().signerJwtUser())
//                .setExpiration(DateTime.now().plusHours(6).toDate())
//                .signWith(SignatureAlgorithm.HS512, RegistrationProcessConfiguration.signerJwtSecret()).compact();
//
//        try (final FormDataMultiPart formDataMultiPart = new FormDataMultiPart()) {
//            final StreamDataBodyPart streamDataBodyPart = new StreamDataBodyPart("file", contentStream, filename, new MediaType("application", "pdf"));
//            formDataMultiPart.bodyPart(streamDataBodyPart);
//            formDataMultiPart.bodyPart(new FormDataBodyPart("queue", queue));
//            formDataMultiPart.bodyPart(new FormDataBodyPart("creator", "Sistema FenixEdu"));
//            formDataMultiPart.bodyPart(new FormDataBodyPart("filename", filename));
//            formDataMultiPart.bodyPart(new FormDataBodyPart("title", title));
//            formDataMultiPart.bodyPart(new FormDataBodyPart("description", description));
//            formDataMultiPart.bodyPart(new FormDataBodyPart("externalIdentifier", uuid));
//            formDataMultiPart.bodyPart(new FormDataBodyPart("signatureField", CandidacySignalHandler.SIGNATURE_FIELD));
//
//            final String nounce = Jwts.builder().setSubject(uuid).signWith(SignatureAlgorithm.HS512, RegistrationProcessConfiguration.signerJwtSecret()).compact();
//
//            formDataMultiPart.bodyPart(new FormDataBodyPart("callbackUrl", CoreConfiguration.getConfiguration().applicationUrl()
//                    + "/adhock-document/store/" + username + "/" + filename + "?nounce=" + nounce));
//            final Client client = ClientBuilder.newClient();
//            client.register(MultiPartFeature.class);
//            client.register(JsonBodyReaderWriter.class);
//            client.target(RegistrationProcessConfiguration.getConfiguration().signerUrl()).path("sign-requests")
//                    .request().header("Authorization", "Bearer " + compactJws)
//                    .post(Entity.entity(formDataMultiPart, MediaType.MULTIPART_FORM_DATA_TYPE), String.class);
//        } catch (final IOException e) {
//            throw new Error(e);
//        }
//    }
//
//    private Set<String> load(final String logFilename) {
//        final Set<String> result = new HashSet<>();
//        final File file = new File(logFilename);
//        if (file.exists()) {
//            try {
//                for (final String line : Files.readAllLines(file.toPath())) {
//                    result.add(line);
//                }
//            } catch (final IOException e) {
//                throw new Error(e);
//            }
//        }
//        taskLog("Read %s users from file: %s%n", result.size(), logFilename);
//        return result;
//    }
//
//    private void write(final String logFilename, final Set<String> processed) {
//        final StringBuilder builder = new StringBuilder();
//        processed.forEach(username -> builder.append(username).append("\n"));
//        try {
//            Files.write(new File(logFilename).toPath(), builder.toString().getBytes(),
//                    StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
//        } catch (final IOException e) {
//            throw new Error(e);
//        }
    }

}
