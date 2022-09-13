package pt.ist.fenix.webapp;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.fenixedu.academic.domain.Country;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.organizationalStructure.CountryUnit;
import org.fenixedu.academic.domain.organizationalStructure.PartyTypeEnum;
import org.fenixedu.academic.domain.organizationalStructure.UniversityUnit;
import org.fenixedu.academic.domain.student.RegistrationProtocol;
import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.admissions.util.AdmissionsError;
import org.fenixedu.bennu.AdmissionsISTConfiguration;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.bennu.core.json.JsonUtils;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.i18n.LocalizedString;
import org.springframework.http.HttpStatus;
import pt.ist.fenixframework.FenixFramework;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ChangeOutgoingMobilityOptions extends CustomTask {

    private static final Locale EN = new Locale("en", "GB");
    private static final Locale PT = new Locale("pt", "PT");

    private static final String LOCAL = "/home/rcro/DocumentsHDD/fenix/candidaturas/";
    private static final String PROD = "/afs/ist.utl.pt/ciist/fenix/fenix015/ist/";
    private static final String OUTGOING_CHANGES_FILENAME = PROD + "changes_outgoing_mobility.xlsx";

    @Override
    public void runTask() throws Exception {
        final Stream<Row> rowStream = xlsxRowStream(OUTGOING_CHANGES_FILENAME, "Dados");
        AdmissionProcess outgoing = FenixFramework.getDomainObject("571432513830937"); //PROD
//        AdmissionProcess outgoing = FenixFramework.getDomainObject("289957537120275"); //LOCAL
        final JsonObject form = outgoing.getFormDataJson();
        final Set<LocalizedString> errors = new HashSet<>();
        Set<Set<Degree>> possibleDegrees = new HashSet<>();
        rowStream.forEach(row -> {
            if (row.getCell(0) != null) {
                final RegistrationProtocol protocol = registrationProtocol(row);
                boolean lineWithError = false;
                if (protocol == null) {
                    errors.add(BundleUtil.getLocalizedString(AdmissionsISTConfiguration.BUNDLE,
                            "error.template.create.mobility.process.invalid.protocol", "#" + row.getCell(0).getStringCellValue() + "#"));
                    lineWithError = true;
                }
                final CountryUnit country = country(row);
                UniversityUnit universityUnit = null;
                if (country == null) {
                    errors.add(BundleUtil.getLocalizedString(AdmissionsISTConfiguration.BUNDLE,
                            "error.template.create.mobility.process.invalid.country", "#" + row.getCell(1).getStringCellValue() + "#"));
                    lineWithError = true;
                } else {
                    universityUnit = universityUnit(row);
                    if (universityUnit == null) {
                        errors.add(BundleUtil.getLocalizedString(AdmissionsISTConfiguration.BUNDLE,
                                "error.template.create.mobility.process.invalid.university", "#" + row.getCell(2).getStringCellValue() + "#"));
                        lineWithError = true;
                    }
                }
                Operation operation = operation(row);
                if (!lineWithError) {
                    final Set<Degree> degrees = degrees(row);
                    String degreesID = getDegreesID(degrees);
                    final double vacancies = vacancies(row);

                    switch (operation) {
                        case ADD_LINE: addLine(form, possibleDegrees, protocol, country, universityUnit, degrees, degreesID, vacancies);
                            break;
                        case REMOVE_LINE: removeLine(form, protocol, country, universityUnit, degreesID, vacancies);
                            break;
                        case CHANGE_VACANCY: changeVacancy(form, protocol, country, universityUnit, degreesID, vacancies);
                            break;
                        default: throw new Error("Operation not recognized");
                    }
                }
            }
        });

        if (!errors.isEmpty()) {
            final LocalizedString errorMessage = errors.stream().reduce(new LocalizedString(), (acc, ls) -> acc.append(ls, "\n"));
            throw new AdmissionsError(AdmissionsISTConfiguration.BUNDLE, HttpStatus.BAD_REQUEST,
                    "error.template.create.mobility.process.invalid.file", errorMessage.getContent());
        }

        addDegrees(form, possibleDegrees);
        outgoing.setFormData(form.toString());
    }

    private void addLine(final JsonObject form, final Set<Set<Degree>> possibleDegrees, final RegistrationProtocol protocol, final CountryUnit country,
                         final UniversityUnit universityUnit, final Set<Degree> degrees, final String degreesID, double vacancies) {
        possibleDegrees.add(degrees);
        addCountry(form, country, degreesID);
        addProtocol(form, country, degreesID, protocol);
        addUniversity(form, country, degreesID, protocol, universityUnit, String.valueOf(vacancies));
    }

    private void removeLine(final JsonObject form, final RegistrationProtocol protocol, final CountryUnit country, final UniversityUnit universityUnit,
                            final String degreesID, double vacancies) {
        removeUniversity(form, country, degreesID, protocol, universityUnit, String.valueOf(vacancies));
    }

    private void changeVacancy(final JsonObject form, final RegistrationProtocol protocol, final CountryUnit country, final UniversityUnit universityUnit,
                            final String degreesID, double vacancies) {
        changeVacancy(form, country, degreesID, protocol, universityUnit, String.valueOf(vacancies));
    }

    private void addDegrees(final JsonObject form, final Set<Set<Degree>> possibleDegrees) {
        LocalizedString or = new LocalizedString(PT, " ou ").with(EN, " or ");
        for (Set<Degree> degrees : possibleDegrees) {
            LocalizedString degreesLabel = new LocalizedString();
            for (Degree degree : degrees) {
                String degreeCode = degree.getSigla();
                if (!degreesLabel.isEmpty()) {
                    degreesLabel = degreesLabel.append(or);
                }
                LocalizedString degreeDisplay = new LocalizedString(PT, degreeCode).with(EN, degreeCode);
                degreesLabel = degreesLabel.append(degreeDisplay);
            }
            addIfNot(form, "degree", degreesLabel, getDegreesID(degrees), null, null);
        }
    }

    private void addCountry(final JsonObject form, final CountryUnit country, final String degreesID) {
        final String countryCombinedCode = country.getCountry().getExternalId() + ":" + degreesID;
        addIfNot(form, "country", country.getCountry().getLocalizedName(), countryCombinedCode, degreesID, null);
    }

    private void addProtocol(final JsonObject form, final CountryUnit country, final String degreesID, final RegistrationProtocol protocol) {
        final String countryCombinedCode = country.getCountry().getExternalId() + ":" + degreesID;
        final String protocolCombinedCode = protocol.getExternalId() + ":" + countryCombinedCode;
        addIfNot(form, "protocol", protocol.getDescription(), protocolCombinedCode, countryCombinedCode, null);
    }

    private void addUniversity(final JsonObject form, final CountryUnit country, final String degreesID, final RegistrationProtocol protocol,
                               final UniversityUnit universityUnit, final String slots) {
        final String protocolCombinedCode = protocol.getExternalId() + ":" + country.getCountry().getExternalId() + ":" + degreesID;
        final String universityCombinedCode = universityUnit.getExternalId() + ":" + protocolCombinedCode;
        addIfNot(form, "university", universityUnit.getNameI18n(), universityCombinedCode, protocolCombinedCode, slots);
    }

    private void removeUniversity(final JsonObject form, final CountryUnit country, final String degreesID, final RegistrationProtocol protocol,
                                  final UniversityUnit universityUnit, final String slots) {
        final String protocolCombinedCode = protocol.getExternalId() + ":" + country.getCountry().getExternalId() + ":" + degreesID;
        final String universityCombinedCode = universityUnit.getExternalId() + ":" + protocolCombinedCode;
        removeIfExists(form, "university", universityUnit.getNameI18n(), universityCombinedCode, protocolCombinedCode, slots);
    }

    private void changeVacancy(final JsonObject form, final CountryUnit country, final String degreesID, final RegistrationProtocol protocol,
                               final UniversityUnit universityUnit, final String slots) {
        final String protocolCombinedCode = protocol.getExternalId() + ":" + country.getCountry().getExternalId() + ":" + degreesID;
        final String universityCombinedCode = universityUnit.getExternalId() + ":" + protocolCombinedCode;
        changeVacancyIfExists(form, "university", universityUnit.getNameI18n(), universityCombinedCode, protocolCombinedCode, slots);
    }

    private void addIfNot(final JsonObject form, final String fieldName, final LocalizedString label, final String value, final String optionFor,
                          final String slots) {
        final JsonArray pages = form.get("pages").getAsJsonArray();
        final JsonObject page = pages.get(0).getAsJsonObject();
        final JsonArray sections = page.get("sections").getAsJsonArray();
        final JsonObject section = sections.get(0).getAsJsonObject();
        final JsonArray properties = section.get("properties").getAsJsonArray();
        final JsonObject arrayField = properties.get(0).getAsJsonObject();
        final JsonArray arrayProperties = arrayField.get("properties").getAsJsonArray();

        for (final JsonElement field : arrayProperties) {
            if (field.isJsonObject()) {
                final JsonObject jsonObject = field.getAsJsonObject();
                if (jsonObject.get("field").getAsString().equals(fieldName)) {
                    JsonObject newOption = new JsonObject();
                    newOption.add("label", label.json());
                    newOption.addProperty("value", value);
                    JsonUtils.addIf(newOption,"slots", slots);
                    if (optionFor != null) {
                        newOption.addProperty("optionFor", optionFor);
                    }
                    final JsonArray options = jsonObject.get("options").getAsJsonArray();
                    boolean hasOption = false;
                    for (JsonElement option : options) {
                        if (option.toString().equals(newOption.toString())) {
                            hasOption = true;
                            break;
                        }
                    }
                    if (!hasOption) {
                        options.add(newOption);
                    }
                }
            }
        }
    }

    private void removeIfExists(final JsonObject form, final String fieldName, final LocalizedString label, final String value, final String optionFor,
                                final String slots) {
        final JsonArray pages = form.get("pages").getAsJsonArray();
        final JsonObject page = pages.get(0).getAsJsonObject();
        final JsonArray sections = page.get("sections").getAsJsonArray();
        final JsonObject section = sections.get(0).getAsJsonObject();
        final JsonArray properties = section.get("properties").getAsJsonArray();
        final JsonObject arrayField = properties.get(0).getAsJsonObject();
        final JsonArray arrayProperties = arrayField.get("properties").getAsJsonArray();

        for (final JsonElement field : arrayProperties) {
            if (field.isJsonObject()) {
                final JsonObject jsonObject = field.getAsJsonObject();
                if (jsonObject.get("field").getAsString().equals(fieldName)) {
                    JsonObject optionToRemove = new JsonObject();
                    optionToRemove.add("label", label.json());
                    optionToRemove.addProperty("value", value);
                    JsonUtils.addIf(optionToRemove,"slots", slots);
                    if (optionFor != null) {
                        optionToRemove.addProperty("optionFor", optionFor);
                    }
                    final JsonArray options = jsonObject.get("options").getAsJsonArray();
                    for (JsonElement option : options) {
                        if (option.toString().equals(optionToRemove.toString())) {
                            options.remove(option);
                            break;
                        }
                    }
                }
            }
        }
    }

    private void changeVacancyIfExists(final JsonObject form, final String fieldName, final LocalizedString label, final String value, final String optionFor,
                                       final String slots) {
        final JsonArray pages = form.get("pages").getAsJsonArray();
        final JsonObject page = pages.get(0).getAsJsonObject();
        final JsonArray sections = page.get("sections").getAsJsonArray();
        final JsonObject section = sections.get(0).getAsJsonObject();
        final JsonArray properties = section.get("properties").getAsJsonArray();
        final JsonObject arrayField = properties.get(0).getAsJsonObject();
        final JsonArray arrayProperties = arrayField.get("properties").getAsJsonArray();

        for (final JsonElement field : arrayProperties) {
            if (field.isJsonObject()) {
                final JsonObject jsonObject = field.getAsJsonObject();
                if (jsonObject.get("field").getAsString().equals(fieldName)) {
                    JsonObject optionToChange = new JsonObject();
                    optionToChange.add("label", label.json());
                    optionToChange.addProperty("value", value);
                    optionToChange.addProperty("slots", slots);
                    if (optionFor != null) {
                        optionToChange.addProperty("optionFor", optionFor);
                    }
                    final JsonArray options = jsonObject.get("options").getAsJsonArray();
                    for (JsonElement option : options) {
                        JsonObject joption = option.getAsJsonObject();
                        if (joption.has("slots")) {
                            final String originalVacancy = joption.get("slots").getAsString();
                            optionToChange.addProperty("slots", originalVacancy);
                            if (option.toString().equals(optionToChange.toString())) {
                                joption.addProperty("slots", slots);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    private RegistrationProtocol registrationProtocol(final Row row) {
        final Cell cell = row.getCell(0);
        final String protocolCode = cell.getStringCellValue().trim();
        return Bennu.getInstance().getRegistrationProtocolsSet().stream()
                .filter(p -> p.getCode().equalsIgnoreCase(protocolCode))
                .findAny().orElse(null);
    }

    private CountryUnit country(final Row row) {
        final Cell cell = row.getCell(1);
        final String countryCode = cell.getStringCellValue().trim();
        return CountryUnit.getCountryUnitByCountry(Country.readByTwoLetterCode(countryCode));
    }

    private UniversityUnit universityUnit(final Row row) {
        final Cell cell = row.getCell(2);
        final String universityName = cell.getStringCellValue();
        return country(row).getSubUnits(PartyTypeEnum.UNIVERSITY).stream()
                .map(UniversityUnit.class::cast)
                .filter(unit -> matchName(unit, universityName))
                .findAny().orElse(null);
    }

    private Set<Degree> degrees(final Row row) {
        final Cell cell = row.getCell(3);
        final Set<Degree> degrees = new HashSet<Degree>();
        final String[] degreeCodes = cell.getStringCellValue().split(",");
        for (int iter = 0; iter < degreeCodes.length; iter++) {
            String degreeCode = degreeCodes[iter].trim();
            degrees.add(Degree.readBySigla(degreeCode));
        }
        return degrees;
    }

    private double vacancies(final Row row) {
        final Cell cell = row.getCell(4);
        return cell.getNumericCellValue();
    }

    private Operation operation(Row row) {
        final Cell cell = row.getCell(6);
        return Operation.getOperation((int)cell.getNumericCellValue());
    }

    private boolean matchName(final UniversityUnit universityUnit, final String name) {
        String content = universityUnit.getName();
        if (content == null) {
            content = universityUnit.getNameI18n().getContent();
        }
        return content.replace("\u00a0"," ").replaceAll("  ", " ").trim()
                .equals(name.replace("\u00a0"," ").replaceAll("  ", " ").trim());
    }

    private String getDegreesID(final Set<Degree> degrees) {
        return degrees.stream()
                .map(Degree::getExternalId)
                .collect(Collectors.joining("#"));
    }

    protected Stream<Row> xlsxRowStream(final String fileUrl, final String sheetName) {
        try {
            final byte[] content = Files.readAllBytes(new File(fileUrl).toPath());
            final ByteArrayInputStream inputStream = new ByteArrayInputStream(content);
            final Workbook workbook = new XSSFWorkbook(inputStream);
            final Sheet sheet = workbook.getSheet(sheetName);
            return sheet == null ? Stream.empty() : StreamSupport.stream(Spliterators.spliteratorUnknownSize(sheet.rowIterator(), Spliterator.ORDERED), false);
        } catch (final NotOfficeXmlFileException | IOException ex) {
            throw new Error(ex);
        }
    }

    private enum Operation {
        CHANGE_VACANCY(0),
        ADD_LINE(1),
        REMOVE_LINE(2);

        private int operation;

        Operation(int operation) {
            this.operation = operation;
        }

        public static Operation getOperation(int operation) {
            for (int iter=0; iter < values().length; iter++) {
                if (values()[iter].operation == operation) {
                    return values()[iter];
                }
            }
            return null;
        }
    }
}
