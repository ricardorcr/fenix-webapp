package pt.ist.fenix.webapp.task;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.jsonwebtoken.SignatureAlgorithm;
import kong.unirest.HttpResponse;
import kong.unirest.MultipartBody;
import kong.unirest.Unirest;
import org.fenixedu.academic.domain.Country;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.DistrictSubdivision;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.EntryPhase;
import org.fenixedu.academic.domain.ExecutionDegree_Base;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.GrantOwnerType;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.SchoolPeriodDuration;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.accounting.EventTemplate;
import org.fenixedu.academic.domain.candidacy.IngressionType;
import org.fenixedu.academic.domain.candidacy.PersonalInformationBean;
import org.fenixedu.academic.domain.candidacyProcess.erasmus.ErasmusApplyForSemesterType;
import org.fenixedu.academic.domain.candidacyProcess.mobility.MobilityAgreement;
import org.fenixedu.academic.domain.candidacyProcess.mobility.MobilityApplicationProcess;
import org.fenixedu.academic.domain.candidacyProcess.mobility.MobilityIndividualApplication;
import org.fenixedu.academic.domain.candidacyProcess.mobility.MobilityIndividualApplicationProcess;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.academic.domain.mobility.outbound.OutboundMobilityCandidacySubmission;
import org.fenixedu.academic.domain.raides.DegreeDesignation;
import org.fenixedu.academic.domain.reports.RaidesCommonReportFieldsWrapper;
import org.fenixedu.academic.domain.student.PrecedentDegreeInformation;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationDataByExecutionYear_Base;
import org.fenixedu.academic.domain.student.RegistrationProtocol;
import org.fenixedu.academic.domain.student.RegistrationRegimeType;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.student.StudentStatute;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationState;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationStateType;
import org.fenixedu.academic.domain.studentCurriculum.BranchCurriculumGroup;
import org.fenixedu.academic.domain.studentCurriculum.Credits;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumLine;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumModule;
import org.fenixedu.academic.domain.studentCurriculum.CycleCurriculumGroup;
import org.fenixedu.academic.domain.studentCurriculum.ExtraCurriculumGroup;
import org.fenixedu.academic.dto.student.RegistrationConclusionBean;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.admissions.domain.AdmissionProcessTarget;
import org.fenixedu.admissions.domain.AdmissionsLog;
import org.fenixedu.admissions.domain.AdmissionsSystem;
import org.fenixedu.admissions.domain.Application;
import org.fenixedu.admissions.ist.domain.Utils;
import org.fenixedu.admissions.service.GenerateStatistics;
import org.fenixedu.bennu.SapSdkConfiguration;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.bennu.core.json.JsonUtils;
import org.fenixedu.bennu.core.util.CoreConfiguration;
import org.fenixedu.bennu.io.domain.DriveAPIStorage;
import org.fenixedu.bennu.io.domain.FileSupport;
import org.fenixedu.bennu.scheduler.annotation.Task;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.connect.domain.Account;
import org.fenixedu.connect.domain.ConnectSystem;
import org.fenixedu.connect.domain.Identity;
import org.fenixedu.connect.domain.identification.IdentificationDocument;
import org.fenixedu.connect.domain.identification.PersonalInformation;
import org.fenixedu.connect.domain.identification.PortugueseCitizenCard;
import org.fenixedu.connect.domain.identification.PortugueseIdentityCard;
import org.fenixedu.connect.domain.identification.TaxInformation;
import org.fenixedu.dynamicForms.DynamicForm;
import org.fenixedu.jwt.Tools;
import org.fenixedu.messaging.core.domain.Message;
import org.jetbrains.annotations.Nullable;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.IllegalFieldValueException;
import org.joda.time.LocalDate;
import org.joda.time.YearMonthDay;
import org.joda.time.format.ISODateTimeFormat;
import pt.ist.fenixedu.contracts.domain.Employee;
import pt.ist.fenixedu.integration.domain.student.importation.DegreeCandidateDTO;
import pt.ist.fenixframework.FenixFramework;
import pt.ist.standards.geographic.Planet;
import pt.ist.standards.geographic.PostalCode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DumpRawData extends CustomTask {

    private static final String REPO_NODE_ID = "1414452990237579";

    @Override
    public void runTask() throws Exception {
        try {
//            dumpIdentifiers();
//            dumpDegrees();
//            dumpRegistrations();
//            dumpEmails();
//            dumpContactInfo();
//            dumpAdmissionProcesses();
            dumpRaidsReports();
        } finally {
            new Thread(() -> FenixFramework.atomic(() -> {
                Message.fromSystem()
                        .singleTos("marta.graca@tecnico.ulisboa.pt", "saragalhoz@tecnico.ulisboa.pt")
                        .subject("Exportação dados do Fénix " + new LocalDate().toString("yyyy-MM-dd"))
                        .textBody("A tarefa de exportação de todos os dados foi agora concluída.")
                        .send();
            })).start();
        }
    }

    private void dumpIdentifiers() {
        final Spreadsheet spreadsheetUsernames = new Spreadsheet("Usernames");
        final Spreadsheet spreadsheetRegistrations = spreadsheetUsernames.addSpreadsheet("Registrations");
        final Spreadsheet spreadsheetOtherUsernames = spreadsheetRegistrations.addSpreadsheet("OtherUsernames");
        final Spreadsheet spreadsheetAccounts = spreadsheetRegistrations.addSpreadsheet("Accounts");

        final Spreadsheet spreadsheetIdentificationDocuments = new Spreadsheet("IdDocuments");
        final Spreadsheet spreadsheetTaxInformation = new Spreadsheet("TaxInformation");
        // final Spreadsheet spreadsheetAddressData = new Spreadsheet("AddressData");
        final Spreadsheet spreadsheetDateOfBirth = new Spreadsheet("DateOfBirth");

        process(ConnectSystem.getInstance().getIdentitySet(), identity -> {
            final User user = identity.getUser();
            final Person person = user == null ? null : user.getPerson();
            final Student student = person == null ? null : person.getStudent();
            final Employee employee = person == null ? null : person.getEmployee();
            final String username = user == null ? null : user.getUsername();
            final String[] sap = employee == null ? null : SapSdkConfiguration.usernameProvider()
                    .toSapNumbers(username);

            final Spreadsheet.Row rowUsernames = row(spreadsheetUsernames);
            rowUsernames.setCell("identity", identity.getExternalId());
            rowUsernames.setCell("username", username);
            rowUsernames.setCell("student", student == null ? null : student.getNumber());
            rowUsernames.setCell("collaborator", employee == null ? null : employee.getEmployeeNumber());
            rowUsernames.setCell("sapIST", sap == null ? null : sap[0]);
            rowUsernames.setCell("sapIST-ID", sap == null ? null : sap[1]);
            rowUsernames.setCell("sapADIST", sap == null ? null : sap[2]);
            rowUsernames.setCell("sapIDMEC", sap == null ? null : sap[3]);

            if (student != null) {
                student.getRegistrationsSet().forEach(registration -> {
                    final Spreadsheet.Row rowRegistrations = row(spreadsheetRegistrations);
                    rowRegistrations.setCell("username", username);
                    rowRegistrations.setCell("student", student.getNumber());
                    rowRegistrations.setCell("registration", registration.getNumber());
                });
            }

            identity.getAccountSet().stream()
                    .map(Account::getUser)
                    .filter(Objects::nonNull)
                    .map(User::getUsername)
                    .filter(u -> !u.equals(username))
                    .forEach(otherUsername -> {
                        final Spreadsheet.Row rowOthers = row(spreadsheetOtherUsernames);
                        rowOthers.setCell("username", username);
                        rowOthers.setCell("otherUsername", otherUsername);
                    });

            identity.getAccountSet()
                    .forEach(account -> {
                        final Spreadsheet.Row rowOthers = row(spreadsheetAccounts);
                        rowOthers.setCell("Account", account.getExternalId());
                        rowOthers.setCell("Identity", identity.getExternalId());
                    });

            final PersonalInformation personalInformation = identity.getPersonalInformation();
            final IdentificationDocument identificationDocument = personalInformation == null ? null
                    : personalInformation.getIdentificationDocument();
            if (identificationDocument != null) {
                final Spreadsheet.Row rowDocument = row(spreadsheetIdentificationDocuments);
                rowDocument.setCell("identity", identity.getExternalId());
                final LocalizedString nationality = personalInformation.getLocalizedNationalityCountry();
                rowDocument.setCell("nationality", nationality != null ? nationality.getContent() : "");
                rowDocument.setCell("country", identificationDocument.getCountryCode());
                rowDocument.setCell("type", identificationDocument.getIdentificationDocumentName().getContent());
                rowDocument.setCell("number", identificationDocument.getDocumentNumber());
                if (identificationDocument instanceof PortugueseIdentityCard identityCard) {
                    rowDocument.setCell("extraDigit", identityCard.getExtraDigit());
                    if (identityCard instanceof PortugueseCitizenCard citizenCard) {
                        rowDocument.setCell("versionNumber", citizenCard.getVersionNumber());
                        rowDocument.setCell("secondExtraDigit", citizenCard.getSecondExtraDigit());
                    } else {
                        rowDocument.setCell("versionNumber", "");
                        rowDocument.setCell("secondExtraDigit", "");
                    }
                } else {
                    rowDocument.setCell("extraDigit", "");
                }
            }

            final TaxInformation taxInformation = personalInformation == null ? null
                    : personalInformation.getTaxInformation();
            if (taxInformation != null) {
                final Spreadsheet.Row rowTaxInfo = row(spreadsheetTaxInformation);
                rowTaxInfo.setCell("identity", identity.getExternalId());
                final String tin = taxInformation.getTin();
                if (tin == null || tin.isEmpty()) {
                    rowTaxInfo.setCell("country", "");
                    rowTaxInfo.setCell("tin", "");
                } else {
                    rowTaxInfo.setCell("country", tin.substring(0, 2));
                    rowTaxInfo.setCell("tin", tin.substring(2));
                }
                if (taxInformation.getAddressData() != null && !taxInformation.getAddressData().isEmpty()) {
                    final JsonObject addressData = JsonUtils.parse(taxInformation.getAddressData());
                    if (addressData != null) {
                        final String countryCode = JsonUtils.get(addressData, "countryCode");
                        final Country addressCountry = countryCode == null
                                ? null : Country.readByTwoLetterCode(countryCode);
                        if (addressCountry != null) {
                            final String zipCode = JsonUtils.get(addressData, "zipCode");
                            if (zipCode != null) {
                                final String line1 = JsonUtils.get(addressData, "firstLine");
                                if (line1 != null && !line1.isEmpty()) {
                                    final String line2 = JsonUtils.get(addressData, "secondLine");
                                    final String address = line2 == null ? line1 : (line1 + ", " + line2);
                                    final String location = JsonUtils.get(addressData, "location");

                                    rowTaxInfo.setCell("address", address);
                                    rowTaxInfo.setCell("zipCode", zipCode);
                                    rowTaxInfo.setCell("location", location == null ? "" : location);
                                    rowTaxInfo.setCell("countryCode", countryCode);

                                    if ("PT".equals(countryCode)) {
                                        final PostalCode postalCode = Planet.getEarth().getByAlfa2(countryCode)
                                                .getPostalCode(zipCode);
                                        final JsonObject info = postalCode == null ? null : postalCode.getDetails();
                                        if (info != null) {
                                            rowTaxInfo.setCell("freguesia", JsonUtils.get(info, "Freguesia"));
                                            rowTaxInfo.setCell("concelho", JsonUtils.get(info, "Concelho"));
                                            rowTaxInfo.setCell("distrito", JsonUtils.get(info, "Distrito"));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            final LocalDate dateOfBirth = personalInformation != null ? personalInformation.getDateOfBirth() : null;
            if (dateOfBirth != null) {
                final Spreadsheet.Row row = row(spreadsheetDateOfBirth);
                row.setCell("identity", identity.getExternalId());
                row.setCell("dateOfBirth", dateOfBirth.toString("yyyy-MM-dd"));
            }

/*
            if (person != null) {
                person.getPartyContactsSet().stream()
                        .filter(PhysicalAddress.class::isInstance)
                        .map(PhysicalAddress.class::cast)
                        .forEach(physicalAddress -> {
                            final Spreadsheet.Row row = spreadsheetAddressData.addRow();
                            row.setCell("Identity", user.getIdentity().getExternalId());
                            row.setCell("active", physicalAddress.getActive().toString());
                            row.setCell("type", physicalAddress.getType().getName());
                            row.setCell("defaultContact", physicalAddress.getDefaultContact().toString());
                            row.setCell("lastModifiedDate", physicalAddress.getLastModifiedDate() == null ? ""
                                    : physicalAddress.getLastModifiedDate().toString("yyyy-MM-dd HH:mm:ss"));
                            row.setCell("address", physicalAddress.getAddress());
                            row.setCell("areaCode", physicalAddress.getAreaCode());
                            row.setCell("areaOfAreaCode", physicalAddress.getAreaOfAreaCode() == null ? "" : physicalAddress.getAreaOfAreaCode());
                            row.setCell("countryOfResidence", physicalAddress.getCountryOfResidence() == null ? ""
                                    : physicalAddress.getCountryOfResidence().getCode());
                            row.setCell("area", physicalAddress.getArea());
                            row.setCell("parishOfResidence", physicalAddress.getParishOfResidence());
                            row.setCell("districtSubdivisionOfResidence", physicalAddress.getDistrictSubdivisionOfResidence());
                            row.setCell("districtOfResidence", physicalAddress.getDistrictOfResidence());
                        });
            }
 */
        });

        upload(spreadsheetUsernames, "identifiers.xlsx");
        upload(spreadsheetIdentificationDocuments, "identificationDocument.xlsx");
        upload(spreadsheetTaxInformation, "taxInformation.xlsx");
        upload(spreadsheetDateOfBirth, "dateOfBirth.xlsx");
        //upload(spreadsheetAddressData, "addressData.xlsx");
    }

    private void dumpDegrees() {
        final Spreadsheet spreadsheet = new Spreadsheet("Degrees");

        process(Bennu.getInstance().getDegreesSet(), degree -> {
            final Spreadsheet.Row row = row(spreadsheet);
            row.setCell("Code", degree.getSigla());
            row.setCell("Tyoe", degree.getDegreeType().getName().getContent());
            row.setCell("Name", degree.getNameI18N().getContent());
            row.setCell("MinistryCode", degree.getMinistryCode());
        });

        upload(spreadsheet, "degrees.xlsx");
    }

    private void dumpRegistrations() {
        final Spreadsheet spreadsheet = new Spreadsheet("Registrations");
        final Spreadsheet byYear = spreadsheet.addSpreadsheet("ByYear");
        final Spreadsheet states = byYear.addSpreadsheet("States");
        final Spreadsheet plans = states.addSpreadsheet("Plans");

        process(Bennu.getInstance().getRegistrationsSet(), registration -> {
            final Student student = registration.getStudent();
            final Person person = student.getPerson();
            final User user = person.getUser();
            final RegistrationProtocol protocol = registration.getRegistrationProtocol();
            final IngressionType ingressionType = registration.getIngressionType();
            final EntryPhase entryPhase = registration.getEntryPhase();

            final Spreadsheet.Row row = row(spreadsheet);
            row.setCell("registrationId", registration.getExternalId());
            row.setCell("username", user.getUsername());
            row.setCell("student", student.getNumber());
            row.setCell("registration", registration.getNumber());
            row.setCell("degree", registration.getDegree().getSigla());
            row.setCell("protocolCode", protocol == null ? "" : protocol.getCode());
            row.setCell("protocol", protocol == null ? "" : protocol.getDescription().getContent());
            row.setCell("ingressionTypeCode", ingressionType == null ? "" : ingressionType.getCode());
            row.setCell("ingressionType", ingressionType == null ? "" : ingressionType.getDescription().getContent());
            row.setCell("entryPhase", entryPhase == null ? "" : entryPhase.getLocalizedName());
            row.setCell("startDate", registration.getStartDate() == null
                    ? "" : registration.getStartDate().toString("yyyy-MM-dd"));
            row.setCell("studiesStartDate", registration.getStudiesStartDate() == null
                    ? "" : registration.getStudiesStartDate().toString("yyyy-MM-dd"));
            row.setCell("eventTemplate", registration.getEventTemplate() == null ? ""
                    : registration.getEventTemplate().getTitle().getContent());

            registration.getRegistrationDataByExecutionYearSet().forEach(dataByExecutionYear -> {
                final ExecutionYear executionYear = dataByExecutionYear.getExecutionYear();
                final Spreadsheet.Row rowByYear = row(byYear);
                rowByYear.setCell("registrationId", registration.getExternalId());
                rowByYear.setCell("executionYear", executionYear.getYear());
                rowByYear.setCell("enrolmentDate", dataByExecutionYear.getEnrolmentDate() == null ? ""
                        : dataByExecutionYear.getEnrolmentDate().toString("yyyy-MM-dd"));
                rowByYear.setCell("maxCreditsPerYear", dataByExecutionYear.getMaxCreditsPerYear());
                rowByYear.setCell("allowedSemesterForEnrolments",
                        dataByExecutionYear.getAllowedSemesterForEnrolments() == null
                                ? "" : dataByExecutionYear.getAllowedSemesterForEnrolments().getQualifiedName());
                rowByYear.setCell("eventTemplate", dataByExecutionYear.getEventTemplate() == null ? ""
                        : dataByExecutionYear.getEventTemplate().getTitle().getContent());
                rowByYear.setCell("enrolmentModel", dataByExecutionYear.getEnrolmentModel() == null ? ""
                        : dataByExecutionYear.getEnrolmentModel().getLocalizedName());
                rowByYear.setCell("isReingression", Boolean.toString(dataByExecutionYear.getReingression()));
                rowByYear.setCell("reingressionDate", dataByExecutionYear.getReingressionDate() == null ? ""
                        : dataByExecutionYear.getReingressionDate().toString("yyyy-MM-dd"));
                rowByYear.setCell("enrolledCourses", Long.toString(registration.getStudentCurricularPlansSet().stream()
                        .flatMap(StudentCurricularPlan::getEnrolmentStream)
                        .filter(enrolment -> enrolment.getExecutionYear() == executionYear)
                        .count()));
                rowByYear.setCell("approvedCourses", Long.toString(registration.getStudentCurricularPlansSet().stream()
                        .flatMap(StudentCurricularPlan::getEnrolmentStream)
                        .filter(enrolment -> enrolment.getExecutionYear() == executionYear)
                        .filter(Enrolment::isApproved)
                        .count()));
                rowByYear.setCell("enrolledCredits", Double.toString(registration.getStudentCurricularPlansSet()
                        .stream()
                        .flatMap(StudentCurricularPlan::getEnrolmentStream)
                        .filter(enrolment -> enrolment.getExecutionYear() == executionYear)
                        .mapToDouble(enrolment -> enrolment.getEctsCreditsForCurriculum().doubleValue())
                        .sum()));
                rowByYear.setCell("approvedCredits", Double.toString(registration.getStudentCurricularPlansSet()
                        .stream()
                        .flatMap(StudentCurricularPlan::getEnrolmentStream)
                        .filter(enrolment -> enrolment.getExecutionYear() == executionYear)
                        .filter(Enrolment::isApproved)
                        .mapToDouble(enrolment -> enrolment.getEctsCreditsForCurriculum().doubleValue())
                        .sum()));
            });

            registration.getRegistrationStatesSet().forEach(registrationState -> {
                final Spreadsheet.Row rowState = row(states);
                rowState.setCell("registrationId", registration.getExternalId());
                rowState.setCell("executionYear", registrationState.getExecutionYear() == null
                        ? "" : registrationState.getExecutionYear().getYear());
                rowState.setCell("stateDate", registrationState.getStateDate() == null
                        ? "" : registrationState.getStateDate().toString("yyyy-MM-dd"));
                rowState.setCell("endDate", registrationState.getEndDate() == null
                        ? "" : registrationState.getEndDate().toString("yyyy-MM-dd"));
                rowState.setCell("stateType", registrationState.getStateType().getName());
                rowState.setCell("state", registrationState.getStateType().getDescription());
                rowState.setCell("isActive", Boolean.toString(registrationState.isActive()));
            });

            registration.getStudentCurricularPlansSet().forEach(studentCurricularPlan -> {
                final DegreeCurricularPlan degreeCurricularPlan = studentCurricularPlan.getDegreeCurricularPlan();

                final Spreadsheet.Row rowPlan = row(plans);
                rowPlan.setCell("registrationId", registration.getExternalId());
                rowPlan.setCell("degreeCurricularPlan", degreeCurricularPlan.getName());
                rowPlan.setCell("startDate", studentCurricularPlan.getStartDateYearMonthDay() == null ? ""
                        : studentCurricularPlan.getStartDateYearMonthDay().toString("yyyy-MM-dd"));
                rowPlan.setCell("dismissalCredits", Double.toString(studentCurricularPlan.getRoot()
                        .getCurriculumLineStream()
                        .filter(curriculumLine -> !curriculumLine.isEnrolment())
                        .mapToDouble(line -> line.getEctsCreditsForCurriculum().doubleValue())
                        .sum()));
            });
        });

        upload(spreadsheet, "registrations.xlsx");
    }

    private void dumpEmails() throws IOException {
        final Spreadsheet spreadsheetEmails = new Spreadsheet("Emails");
        process(Bennu.getInstance().getUserSet(), user -> {
            final Spreadsheet.Row row = row(spreadsheetEmails);
            row.setCell("Username", user.getUsername());
            row.setCell("Main Email", user.getEmail());
            row.setCell("Institutional", user.getPerson() == null
                    ? "" : user.getPerson().getInstitutionalEmailAddressValue());
        });

        upload(spreadsheetEmails, "Emails.xlsx");
    }

    private void dumpContactInfo() {
        final Spreadsheet spreadsheetEmails = new Spreadsheet("Contacts");
        process(Bennu.getInstance().getUserSet(), user -> {
            final Person person = user.getPerson();
            if (person != null) {
                person.getPartyContactsSet().forEach(partyContact -> {
                    final Spreadsheet.Row row = row(spreadsheetEmails);
                    row.setCell("Username", user.getUsername());
                    row.setCell("Meio", partyContact.getClass().getSimpleName());
                    row.setCell("Type", partyContact.getPartyContactTypeString());
                    row.setCell("Value", partyContact.getPresentationValue());
                    row.setCell("VisibleToPublic", toString(partyContact.getVisibleToPublic()));
                    row.setCell("VisibleToStudents", toString(partyContact.getVisibleToStudents()));
                    row.setCell("VisibleToStaff", toString(partyContact.getVisibleToStaff()));
                });
            }
        });

        upload(spreadsheetEmails, "Contacts.xlsx");
    }

    private String toString(final Boolean b) {
        return b == null ? "" : b.toString();
    }

    private void dumpAdmissionProcesses() {
        final ExecutionYear executionYear = ExecutionYear.readCurrentExecutionYear();
        AdmissionsSystem.getInstance().getAdmissionProcessSet().stream()
                .filter(admissionProcess -> Utils.isDegreeType(admissionProcess)
                        || Utils.isFirstTimeInCycle(admissionProcess))
                .filter(admissionProcess -> executionYearFor(admissionProcess) == executionYear)
                .forEach(admissionProcess -> {
                    final byte[] xls = new GenerateStatistics().calculate(admissionProcess, null).spreadsheet();
                    upload(executionYear.getName().replace('/', '_'), admissionProcess.getTitle().getContent()
                            + ".xlsx", xls);
                });
        {
            final GenerateStatistics statisticsDoubleDergee = new GenerateStatistics();
            AdmissionsSystem.getInstance().getAdmissionProcessSet().stream()
                    .filter(Utils::isMobilityDoubleDegreeType)
                    .filter(admissionProcess -> executionYearFor(admissionProcess) == executionYear)
                    .forEach(admissionProcess -> statisticsDoubleDergee.calculate(admissionProcess, null));
            final byte[] xls = statisticsDoubleDergee.spreadsheet();
            upload(executionYear.getName().replace('/', '_'), "Mobility In DoubleDegree " + executionYear.getYear()
                    + ".xlsx", xls);
        }
        {
            final GenerateStatistics statisticsMobility = new GenerateStatistics();
            AdmissionsSystem.getInstance().getAdmissionProcessSet().stream()
                    .filter(Utils::isMobilityType)
                    .filter(process -> !Utils.isMobilityDoubleDegreeType(process))
                    .filter(admissionProcess -> executionYearFor(admissionProcess) == executionYear)
                    .forEach(admissionProcess -> statisticsMobility.calculate(admissionProcess, null));
            final byte[] xls = statisticsMobility.spreadsheet();
            upload(executionYear.getName().replace('/', '_'), "Mobility In " + executionYear.getYear()
                    + ".xlsx", xls);
        }
        {
            AdmissionsSystem.getInstance().getAdmissionProcessSet().stream()
                    .filter(Utils::isOutboundMobilityType)
                    .filter(admissionProcess -> executionYearFor(admissionProcess) == executionYear)
                    .forEach(admissionProcess -> {
                        final byte[] xls = new GenerateStatistics().calculate(admissionProcess, null).spreadsheet();
                        upload(executionYear.getName().replace('/', '_'), "Mobility Out " + admissionProcess.getTitle().getContent() + executionYear.getYear() + ".xlsx", xls);
                    });
        }
        {
            final GenerateStatistics specificRegime = new GenerateStatistics();
            AdmissionsSystem.getInstance().getAdmissionProcessSet().stream()
                    .filter(Utils::isDegreeSpecificRegimentType)
                    .filter(admissionProcess -> executionYearFor(admissionProcess) == executionYear)
                    .forEach(admissionProcess -> specificRegime.calculate(admissionProcess, null));
            final byte[] xls = specificRegime.spreadsheet();
            upload(executionYear.getName().replace('/', '_'), "Special Regiments " + executionYear.getYear()
                    + ".xlsx", xls);
        }
        {
            AdmissionsSystem.getInstance().getAdmissionProcessSet().stream()
                    .filter(Utils::isReinstatement)
                    .filter(admissionProcess -> executionYearFor(admissionProcess) == executionYear)
                    .forEach(admissionProcess -> {
                        final byte[] xls = new GenerateStatistics().calculate(admissionProcess, null).spreadsheet();
                        upload(executionYear.getName().replace('/', '_'), "Reinstatement " + admissionProcess.getTitle().getContent() + executionYear.getYear() + ".xlsx", xls);
                    });
        }
    }

    private void dumpRaidsReports() {
        Bennu.getInstance().getDegreeTypeSet().stream()
                .filter(degreeType -> degreeType.isBolonhaDegree() || degreeType.isBolonhaMasterDegree()
                        || degreeType.isIntegratedMasterDegree())
                .forEach(this::dumpRaidsReports);
    }

    private void dumpRaidsReports(final DegreeType degreeType) {
        final ExecutionYear executionYear = ExecutionYear.readCurrentExecutionYear();
        final String name = "RAIDES_" + degreeType.getName().getContent().replace(' ', '_') + "_"
                + executionYear.getBeginLocalDate().getYear() + "_" + executionYear.getEndLocalDate().getYear();
        final Spreadsheet spreadsheet = new Spreadsheet(name);
        RaidesCommonReportFieldsWrapper.createHeaders(spreadsheet);

        final ExecutionYear previousYear = executionYear.getPreviousExecutionYear();
        getRegistrationsToProcess(executionYear, degreeType)
                .forEach(registration -> process(spreadsheet, executionYear, previousYear, registration));

        final byte[] xlsx = spreadsheet.exportToXLSXSheet();
        upload(executionYear.getName().replace('/', '_'), name + ".xlsx", xlsx);
    }

    private Stream<Registration> getRegistrationsToProcess(final ExecutionYear executionYear,
                                                           final DegreeType degreeType) {
        final Stream<Registration> stream = registrationStreamFor(executionYear, degreeType);
        final ExecutionYear previous = executionYear.getPreviousExecutionYear();
        final Stream<Registration> result = previous == null ? stream : Stream.concat(stream,
                registrationStreamFor(previous, degreeType));
        return result.distinct();
    }

    private Stream<Registration> registrationStreamFor(final ExecutionYear executionYear, final DegreeType degreeType) {
        return executionYear.getExecutionDegreesSet().stream()
                .map(ExecutionDegree_Base::getDegreeCurricularPlan)
                .filter(dcp -> dcp.getDegreeType() == degreeType)
                .flatMap(dcp -> dcp.getStudentCurricularPlansSet().stream())
                .filter(scp -> !scp.getStartDateYearMonthDay().isAfter(executionYear.getEndDateYearMonthDay()))
                .map(StudentCurricularPlan::getRegistration);
    }


    private void process(final Spreadsheet spreadsheet, final ExecutionYear executionYear,
                         final ExecutionYear previousYear, final Registration registration) {
        FenixFramework.atomic(() -> {
            if (registration != null && !registration.isTransition() && !registration.isSchoolPartConcluded()) {
                for (final CycleType cycleType : registration.getDegreeType().getCycleTypes()) {
                    final StudentCurricularPlan studentCurricularPlan = registration
                            .getStudentCurricularPlan(cycleType);
                    final CycleCurriculumGroup cycleCGroup = studentCurricularPlan.getRoot()
                            .getCycleCurriculumGroup(cycleType);
                    if (cycleCGroup != null && !cycleCGroup.isExternal()) {

                        final RegistrationConclusionBean registrationConclusionBean =
                                new RegistrationConclusionBean(registration, cycleCGroup);

                        if (cycleCGroup.isConcluded()) {
                            final ExecutionYear conclusionYear = registrationConclusionBean.getConclusionYear();

                            if (conclusionYear != executionYear && conclusionYear != previousYear) {
                                continue;
                            }

                        }

                        boolean isToAddRegistration = registration.getRegistrationStates(executionYear).stream()
                                .anyMatch(state -> state.isActive()
                                        || state.getStateType() == RegistrationStateType.CONCLUDED);

                        if (isToAddRegistration
                                && (cycleCGroup.isConcluded(previousYear)
                                == CurriculumModule.ConclusionValue.CONCLUDED)) {
                            reportRaidesGraduate(spreadsheet, registration, studentCurricularPlan,
                                    getFullRegistrationPath(registration), executionYear,
                                    cycleType, true, registrationConclusionBean.getConclusionDate(),
                                    registrationConclusionBean.getRawGrade().getNumericValue());
                        } else if (isToAddRegistration
                                && (registration.getLastDegreeCurricularPlan().hasExecutionDegreeFor(executionYear)
                                || registration.hasAnyCurriculumLines(executionYear))) {
                            reportRaidesGraduate(spreadsheet, registration, studentCurricularPlan,
                                    getFullRegistrationPath(registration), executionYear,
                                    cycleType, false, null, registrationConclusionBean.getRawGrade().getNumericValue());
                        }
                    }
                }
            }
        });
    }

    private void reportRaidesGraduate(final Spreadsheet sheet, final Registration registration,
                                      StudentCurricularPlan studentCurricularPlan, List<Registration> registrationPath,
                                      ExecutionYear executionYear, final CycleType cycleType, final boolean concluded,
                                      final YearMonthDay conclusionDate, BigDecimal average) {
        final Spreadsheet.Row row = reportRaidesFields(sheet, registration, studentCurricularPlan, registrationPath,
                executionYear, cycleType, concluded, conclusionDate, average, true);

        registerMobility(registration, executionYear, row);
    }

    private Spreadsheet.Row reportRaidesFields(final Spreadsheet sheet, final Registration registration,
                                               StudentCurricularPlan studentCurricularPlan,
                                               List<Registration> registrationPath, ExecutionYear executionYear,
                                               final CycleType cycleType, final boolean concluded,
                                               final YearMonthDay conclusionDate, BigDecimal average,
                                               boolean graduation) {
        final Spreadsheet.Row row = sheet.addRow();
        final Person graduate = registration.getPerson();
        //List<Registration> registrationPath = getFullRegistrationPath(registration);
        final Registration sourceRegistration = registrationPath.iterator().next();
        final PersonalInformationBean personalInformationBean = registration.getPersonalInformationBean(executionYear);

        // Ciclo
        row.setCell("ciclo", cycleType.getDescription());

        // Concluído
        row.setCell("concluído (ano anterior)?", String.valueOf(concluded));

        // Média do Ciclo
        if (graduation) {
            row.setCell("média do ciclo", concluded
                    ? printBigDecimal(average.setScale(0, RoundingMode.HALF_EVEN)) : printBigDecimal(average));
        } else {
            row.setCell("média do ciclo", concluded
                    ? studentCurricularPlan.getCycle(cycleType).getCurriculum().getRawGrade().getValue() : "n/a");
        }

        // Data de Conclusão
        row.setCell("Data de conclusão", conclusionDate != null ? conclusionDate.toString("dd-MM-yyyy") : "");

        // Data de Início
        row.setCell("Data de início", registration.getStartDate() != null
                ? registration.getStartDate().toString("dd-MM-yyyy") : "");

        // Nº de aluno
        row.setCell("número aluno", registration.getNumber());

        final User user = graduate.getUser();

        // Nome de Utilizador
        row.setCell("nome de utilizador", user.getUsername());

        final Identity identity = Identity.identityFor(user);
        if (identity == null) {
            row.setCell("Erro", "Procedimentos de registo do aluno violados. Preenchimento de informação impossível.");
            return row;
        }
        final PersonalInformation personalInformation = identity.getPersonalInformation();
        final IdentificationDocument identificationDocument = personalInformation.getIdentificationDocument();

        // Tipo Identificação
        row.setCell("tipo identificação", identificationDocument == null
                ? graduate.getIdDocumentType().getLocalizedName() : identificationDocument
                .getIdentificationDocumentName().getContent());

        // N.º de Identificação
        row.setCell("número identificação", identificationDocument == null
                ? graduate.getDocumentIdNumber() : identificationDocument.getDocumentNumber());

        // Dígitos de Controlo
        row.setCell("digitos controlo", identificationDocument == null
                ? graduate.getIdentificationDocumentExtraDigitValue()
                : identificationDocument instanceof PortugueseIdentityCard
                ? ((PortugueseIdentityCard) identificationDocument).getExtraDigit() : "");

        // Versão Doc. Identificação
        row.setCell("versão doc identificação", identificationDocument == null
                ? graduate.getIdentificationDocumentSeriesNumberValue()
                : identificationDocument instanceof PortugueseCitizenCard
                ? (((PortugueseIdentityCard) identificationDocument).getExtraDigit()
                + ((PortugueseCitizenCard) identificationDocument).getVersionNumber()
                + ((PortugueseCitizenCard) identificationDocument).getSecondExtraDigit()) : "");

        // Nome
        row.setCell("nome", personalInformation == null ? registration.getName() : personalInformation.getFullName());

        // Sexo
        row.setCell("género", personalInformation == null || personalInformation.getGender() == null
                ? graduate.getGender().toString() : personalInformation.getGender().getLocalizedString().getContent());

        // Data de Nascimento
        row.setCell("data nascimento", graduate.getDateOfBirthYearMonthDay() != null
                ? graduate.getDateOfBirthYearMonthDay().toString("dd-MM-yyyy") : "n/a");

        // País de Nascimento
        row.setCell("país nascimento", graduate.getCountryOfBirth() != null
                ? graduate.getCountryOfBirth().getName() : "n/a");

        // País de Nacionalidade
        row.setCell("país nacionalidade", graduate.getCountry() != null ? graduate.getCountry().getName() : "n/a");

        // Tipo Curso
        row.setCell("tipo curso", registration.getDegreeType().getName().getContent());

        // Nome Curso
        row.setCell("nome curso", registration.getDegree().getNameI18N().getContent());

        // Sigla Curso
        row.setCell("sigla curso", registration.getDegree().getSigla());

        // Ramos do currículo do aluno
        final StringBuilder majorBranches = new StringBuilder();
        final StringBuilder minorBranches = new StringBuilder();
        for (final BranchCurriculumGroup group : studentCurricularPlan.getBranchCurriculumGroups()) {
            if (group.isMajor()) {
                majorBranches.append(group.getName().getContent()).append(",");
            } else if (group.isMinor()) {
                minorBranches.append(group.getName().getContent()).append(",");
            }
        }
        studentCurricularPlan.getAllCurriculumGroups().forEach(curriculumGroup -> {
            final String name = curriculumGroup.getName().getContent();
            if (name.indexOf("Principal") > 0 || name.contains("Major ")) {
                majorBranches.append(name);
            } else if (name.indexOf("Secundário") > 0 || name.contains("Minor ")) {
                minorBranches.append(name);
            }
        });

        // Ramo Principal
        if (!majorBranches.isEmpty()) {
            row.setCell("Ramo Principal", majorBranches.deleteCharAt(majorBranches.length() - 1).toString());
        } else {
            row.setCell("Ramo Principal", "");
        }

        // Ramo Secundáro
        if (!minorBranches.isEmpty()) {
            row.setCell("Ramo Secundáro", minorBranches.deleteCharAt(minorBranches.length() - 1).toString());
        } else {
            row.setCell("Ramo Secundáro", "");
        }

        // Ano Curricular
        row.setCell("ano curricular", registration.getCurricularYear(executionYear));

        // Ano de Ingresso no Curso Actual
        row.setCell("ano ingresso curso actual", (sourceRegistration == null
                ? registration : sourceRegistration).getStartExecutionYear().getName());

        // N.º de anos letivos de inscrição no Curso atual
        int numberOfEnrolmentYears = 0;
        for (Registration current : registrationPath) {
            numberOfEnrolmentYears += current.getNumberOfYearsEnrolledUntil(executionYear);
        }
        row.setCell("nº. anos lectivos inscrição curso actual", numberOfEnrolmentYears);

        // Último ano em que esteve inscrito
        row.setCell("Último ano inscrito neste curso", registration.getLastEnrolmentExecutionYear() != null
                ? registration.getLastEnrolmentExecutionYear().getName() : "");

        // Regime de frequência curso: Tempo integral/Tempo Parcial
        final EventTemplate eventTemplate = registration.getEventTemplate();
        final EventTemplate eventTemplateForYear = registration.getRegistrationDataByExecutionYearSet().stream()
                .filter(data -> data.getExecutionYear() == executionYear)
                .map(RegistrationDataByExecutionYear_Base::getEventTemplate)
                .filter(Objects::nonNull)
                .findAny().orElse(null);
        final RegistrationRegimeType regimeType = eventTemplate == null || eventTemplateForYear == null
                || eventTemplate == eventTemplateForYear
                ? RegistrationRegimeType.FULL_TIME : RegistrationRegimeType.PARTIAL_TIME;
        row.setCell("regime frequência curso", regimeType.getName());

        // Tipo de Aluno (AFA, AM, ERASMUS, etc)
        row.setCell("tipo aluno", registration.getRegistrationProtocol() != null
                ? registration.getRegistrationProtocol().getCode() : "");

        // Regime de Ingresso no Curso Actual (código)
        IngressionType ingressionType = sourceRegistration.getIngressionType();
        if (ingressionType == null && sourceRegistration.getStudentCandidacy() != null) {
            ingressionType = sourceRegistration.getStudentCandidacy().getIngressionType();
        }
        row.setCell("regime ingresso (código)", ingressionType != null ? ingressionType.getCode() : "");

        // Regime de Ingresso no Curso Atual (designação)
        row.setCell("regime ingresso (designação)", ingressionType != null
                ? ingressionType.getDescription().getContent() : "");

        final Application application = findApplicationFor(identity, registration);
        final DegreeCandidateDTO dgesLine = dgesLineFor(application);
        final PrecedentDegreeInformation precedentDegreeInformation = registration.getPrecedentDegreeInformation();

        final Set<DynamicForm.Field> mostRecentPreviousQualification = application == null
                ? null : findMostRecentPreviousQualification(application);
        final Set<DynamicForm.Field> mostRecentCompleteQualification = application == null
                ? null : findMostRecentCompleteQualification(application);
        final Set<DynamicForm.Field> highSchoolQualification = application == null
                ? null : findHighSchoolQualification(application);
        final Set<DynamicForm.Field> otherQualification = application == null
                ? null : findOtherQualification(application,
                toString(mostRecentCompleteQualification),
                toString(mostRecentCompleteQualification),
                toString(highSchoolQualification));
        final Set<DynamicForm.Field> otherCompleteQualification = application == null
                ? null : findOtherCompleteQualification(application,
                toString(mostRecentCompleteQualification),
                toString(mostRecentCompleteQualification),
                toString(highSchoolQualification));

        // estabelecimento do grau preced.: Instituição onde esteve
        // inscrito, mas não obteve grau, (e.g: transferencias, mudanças de
        // curso...)
        if (dgesLine == null) {
            if (mostRecentPreviousQualification == null) {
                row.setCell("estabelecimento do grau preced. (qd aplicável)", personalInformationBean
                        .getPrecedentInstitution() != null ? personalInformationBean.getPrecedentInstitution()
                        .getName() : precedentDegreeInformation != null
                        ? precedentDegreeInformation.getInstitutionName() : "");
            } else {
                row.setCell("estabelecimento do grau preced. (qd aplicável)",
                        institutionFor(mostRecentPreviousQualification));
            }
        } else {
            row.setCell("estabelecimento do grau preced. (qd aplicável)", dgesLine.getHighSchoolName());
        }

        // curso grau preced.
        if (dgesLine == null) {
            if (mostRecentPreviousQualification == null) {
                row.setCell("curso grau preced. (qd aplicável)", personalInformationBean
                        .getPrecedentDegreeDesignation() != null
                        ? personalInformationBean.getPrecedentDegreeDesignation()
                        : precedentDegreeInformation != null ? precedentDegreeInformation.getDegreeDesignation() : "");
            } else {
                row.setCell("curso grau preced. (qd aplicável)", programFor(mostRecentPreviousQualification));
            }
        } else {
            row.setCell("curso grau preced. (qd aplicável)", dgesLine.getHighSchoolDegreeDesignation());
        }

        // estabelec. curso habl anterior compl (se o aluno ingressou por uma via
        // diferente CNA, e deve ser IST caso o aluno tenha estado matriculado noutro curso do IST)
        if (dgesLine == null) {
            if (mostRecentCompleteQualification == null) {
                row.setCell("estabelec. curso habl anterior compl", personalInformationBean.getInstitution() != null
                        ? personalInformationBean.getInstitution().getName() :
                        precedentDegreeInformation != null && precedentDegreeInformation.getConclusionGrade() != null
                                ? precedentDegreeInformation.getInstitutionName() : "");
            } else {
                row.setCell("estabelec. curso habl anterior compl", institutionFor(mostRecentCompleteQualification));
            }
        } else {
            row.setCell("estabelec. curso habl anterior compl", dgesLine.getHighSchoolName());
        }

        // curso habl anterior compl (se o aluno ingressou por uma via diferente CNA, e
        // deve ser IST caso o aluno tenha estado matriculado noutro curso do IST)
        if (dgesLine == null) {
            if (mostRecentCompleteQualification == null) {
                final String degreeDesignation = personalInformationBean.getDegreeDesignation();
                row.setCell("curso habl anterior compl", degreeDesignation != null && (!degreeDesignation.isEmpty())
                        ? degreeDesignation : precedentDegreeInformation != null
                        && precedentDegreeInformation.getConclusionGrade() != null
                        ? precedentDegreeInformation.getDegreeDesignation() : "");
            } else {
                row.setCell("curso habl anterior compl", programFor(mostRecentCompleteQualification));
            }
        } else {
            row.setCell("curso habl anterior compl", dgesLine.getHighSchoolDegreeDesignation());
        }

        // n.º inscrições no curso preced. (conta uma por cada ano)
        row.setCell("nº inscrições no curso preced.", personalInformationBean
                .getNumberOfPreviousYearEnrolmentsInPrecedentDegree() != null
                ? personalInformationBean.getNumberOfPreviousYearEnrolmentsInPrecedentDegree().toString()
                : sourceRegistration != registration && sourceRegistration != null
                ? Integer.toString(sourceRegistration.getRegistrationDataByExecutionYearSet().size()) : "");

        // Nota de Ingresso
        if (dgesLine != null) {
            row.setCell("nota ingresso", printDouble(dgesLine.getEntryGrade()));
        } else if (application != null && application.getGrade() != null) {
            row.setCell("nota ingresso", application.getGrade().toPlainString().replace('.', ','));
        } else {
            Double entryGrade = getEntryGrade(registration, sourceRegistration);
            row.setCell("nota ingresso", printDouble(entryGrade));
        }

        // Opção de Ingresso
        if (dgesLine != null) {
            row.setCell("opção ingresso", dgesLine.getPlacingOption());
        } else {
            Integer placingOption = null;
            if (registration.getStudentCandidacy() != null) {
                placingOption = registration.getStudentCandidacy().getPlacingOption();
            }
            row.setCell("opção ingresso", placingOption);
        }

        // Estado Civil
        final DynamicForm.Select maritalStatus = application == null ? null : fieldFor(application, "maritalStatus");
        if (maritalStatus != null && maritalStatus.valueLabel() != null) {
            row.setCell("estado civil", maritalStatus.valueLabel().stream().map(LocalizedString::getContent)
                    .collect(Collectors.joining(", ")));
        } else {
            row.setCell("estado civil", personalInformationBean.getMaritalStatus() != null
                    ? personalInformationBean.getMaritalStatus().toString()
                    : registration.getPerson().getMaritalStatus().toString());
        }

        // País de Residência Permanente
        final DynamicForm.AsyncSelect residenceCountry = application == null
                ? null : fieldFor(application, "residenceCountry");
        if (residenceCountry != null && residenceCountry.valueLabel() != null) {
            row.setCell("país residência permanente", residenceCountry.valueLabel().getContent());
        } else if (personalInformationBean.getCountryOfResidence() != null) {
            row.setCell("país residência permanente", personalInformationBean.getCountryOfResidence().getName());
        } else {
            row.setCell("país residência permanente", registration.getStudent().getPerson()
                    .getCountryOfResidence() != null
                    ? registration.getStudent().getPerson().getCountryOfResidence().getName() : "");
        }

        // Distrito de Residência Permanente
        final DynamicForm.AsyncSelect residenceMunicipality = application == null
                ? null : fieldFor(application, "residenceMunicipality");
        final DistrictSubdivision districtSubdivision = residenceMunicipality == null
                ? null : FenixFramework.getDomainObject(residenceMunicipality.value());
        if (districtSubdivision != null) {
            row.setCell("distrito residência permanente", districtSubdivision.getDistrict().getName());
        } else if (personalInformationBean.getDistrictSubdivisionOfResidence() != null) {
            row.setCell("distrito residência permanente", personalInformationBean.getDistrictSubdivisionOfResidence()
                    .getDistrict().getName());
        } else {
            row.setCell("distrito residência permanente", registration.getStudent().getPerson()
                    .getDistrictOfResidence());
        }

        // Concelho de Residência Permanente
        if (districtSubdivision != null) {
            row.setCell("concelho residência permanente", districtSubdivision.getName());
        } else if (personalInformationBean.getDistrictSubdivisionOfResidence() != null) {
            row.setCell("concelho residência permanente", personalInformationBean.getDistrictSubdivisionOfResidence()
                    .getName());
        } else {
            row.setCell("concelho residência permanente", registration.getStudent().getPerson()
                    .getDistrictSubdivisionOfResidence());
        }

        // Deslocado da Residência Permanente
        final DynamicForm.Boolean displaced = application == null ? null : fieldFor(application, "displaced");
        if (displaced != null && displaced.value() != null) {
            row.setCell("deslocado residência permanente", displaced.value().toString());
        } else if (personalInformationBean.getDislocatedFromPermanentResidence() != null) {
            row.setCell("deslocado residência permanente", personalInformationBean
                    .getDislocatedFromPermanentResidence().toString());
        } else {
            row.setCell("deslocado residência permanente", "");
        }

        // Nível de Escolaridade do Pai
        final DynamicForm.Select fatherQualification = application == null
                ? null : fieldFor(application, "fatherQualification");
        if (fatherQualification != null && fatherQualification.valueLabel() != null) {
            row.setCell("nível escolaridade pai", fatherQualification.valueLabel().stream()
                    .map(LocalizedString::getContent).collect(Collectors.joining(", ")));
        } else if (personalInformationBean.getFatherSchoolLevel() != null) {
            row.setCell("nível escolaridade pai", personalInformationBean.getFatherSchoolLevel().getName());
        } else {
            row.setCell("nível escolaridade pai", "");
        }

        // Nível de Escolaridade da Mãe
        final DynamicForm.Select motherQualification = application == null
                ? null : fieldFor(application, "motherQualification");
        if (motherQualification != null && motherQualification.valueLabel() != null) {
            row.setCell("nível escolaridade mãe", motherQualification.valueLabel().stream()
                    .map(LocalizedString::getContent).collect(Collectors.joining(", ")));
        } else if (personalInformationBean.getMotherSchoolLevel() != null) {
            row.setCell("nível escolaridade mãe", personalInformationBean.getMotherSchoolLevel().getName());
        } else {
            row.setCell("nível escolaridade mãe", "");
        }

        // Condição perante a situação na profissão/Ocupação do
        // Pai
        final DynamicForm.Select fatherProfessionalStatus = application == null
                ? null : fieldFor(application, "fatherProfessionalStatus");
        if (fatherProfessionalStatus != null && fatherProfessionalStatus.valueLabel() != null) {
            row.setCell("condição perante profissão pai", fatherProfessionalStatus.valueLabel().stream()
                    .map(LocalizedString::getContent).collect(Collectors.joining(", ")));
        } else if (personalInformationBean.getFatherProfessionalCondition() != null) {
            row.setCell("condição perante profissão pai", personalInformationBean.getFatherProfessionalCondition()
                    .getName());
        } else {
            row.setCell("condição perante profissão pai", "");
        }

        // Condição perante a situação na profissão/Ocupação da
        // Mãe
        final DynamicForm.Select motherProfessionalStatus = application == null
                ? null : fieldFor(application, "motherProfessionalStatus");
        if (motherProfessionalStatus != null && motherProfessionalStatus.valueLabel() != null) {
            row.setCell("condição perante profissão mãe", motherProfessionalStatus.valueLabel().stream()
                    .map(LocalizedString::getContent).collect(Collectors.joining(", ")));
        } else if (personalInformationBean.getMotherProfessionalCondition() != null) {
            row.setCell("condição perante profissão mãe", personalInformationBean.getMotherProfessionalCondition()
                    .getName());
        } else {
            row.setCell("condição perante profissão mãe", "");
        }

        // Profissão do Pai
        final DynamicForm.Select fatherOccupation = application == null
                ? null : fieldFor(application, "fatherOccupation");
        if (fatherOccupation != null && fatherOccupation.valueLabel() != null) {
            row.setCell("profissão pai", fatherOccupation.valueLabel().stream().map(LocalizedString::getContent)
                    .collect(Collectors.joining(", ")));
        } else if (personalInformationBean.getFatherProfessionType() != null) {
            row.setCell("profissão pai", personalInformationBean.getFatherProfessionType().getName());
        } else {
            row.setCell("profissão pai", "");
        }

        // Profissão da Mãe
        final DynamicForm.Select motherOccupation = application == null
                ? null : fieldFor(application, "motherOccupation");
        if (motherOccupation != null && motherOccupation.valueLabel() != null) {
            row.setCell("profissão mãe", motherOccupation.valueLabel().stream().map(LocalizedString::getContent)
                    .collect(Collectors.joining(", ")));
        } else if (personalInformationBean.getMotherProfessionType() != null) {
            row.setCell("profissão mãe", personalInformationBean.getMotherProfessionType().getName());
        } else {
            row.setCell("profissão mãe", "");
        }

        // Profissão do Aluno
        final DynamicForm.Select occupation = application == null ? null : fieldFor(application, "occupation");
        if (occupation != null && occupation.valueLabel() != null) {
            row.setCell("profissão aluno", occupation.valueLabel().stream().map(LocalizedString::getContent)
                    .collect(Collectors.joining(", ")));
        } else if (personalInformationBean.getProfessionType() != null) {
            row.setCell("profissão aluno", personalInformationBean.getProfessionType().getName());
        } else {
            row.setCell("profissão aluno", "");
        }

        // Data preenchimento dados RAIDES
        if (personalInformationBean.getLastModifiedDate() != null) {
            DateTime dateTime = personalInformationBean.getLastModifiedDate();
            row.setCell("Data preenchimento dados RAIDES", dateTime.toString("yyyy-MM-dd"));
        } else {
            final AdmissionsLog admissionsLog = application == null ? null : application.getLogSet().stream()
                    .filter(log -> identity.getAccountSet().stream()
                            .anyMatch(a -> a.getEmail().equals(log.getAccountEmail())))
                    .max(Comparator.comparing(AdmissionsLog::getWhen))
                    .orElse(null);
            if (admissionsLog == null) {
                row.setCell("Data preenchimento dados RAIDES", "");
            } else {
                row.setCell("Data preenchimento dados RAIDES", admissionsLog.getWhen().toString("yyyy-MM-dd"));
            }
        }

        // Estatuto de Trabalhador Estudante introduzido pelo aluno
        if (personalInformationBean.getProfessionalCondition() != null) {
            row.setCell("estatuto trabalhador estudante introduzido (info. RAIDES)", personalInformationBean
                    .getProfessionalCondition().getName());
        } else {
            row.setCell("estatuto trabalhador estudante introduzido (info. RAIDES)", "");
        }

        // Estatuto de Trabalhador Estudante 1º semestre do ano a que se
        // referem
        // os dados
        boolean working1Found = false;
        for (StudentStatute statute : registration.getStudent().getStudentStatutesSet()) {
            if (statute.getType().isWorkingStudentStatute()
                    && statute.isValidInExecutionPeriod(executionYear.getFirstExecutionPeriod())) {
                working1Found = true;
                break;
            }
        }
        row.setCell("estatuto trabalhador 1º semestre ano (info. oficial)", String.valueOf(working1Found));

        // Estatuto de Trabalhador Estudante 1º semestre do ano a que se
        // referem
        // os dados
        boolean working2Found = false;
        for (StudentStatute statute : registration.getStudent().getStudentStatutesSet()) {
            if (statute.getType().isWorkingStudentStatute()
                    && statute.isValidInExecutionPeriod(executionYear.getLastExecutionPeriod())) {
                working2Found = true;
                break;
            }
        }
        row.setCell("estatuto trabalhador 2º semestre ano (info. oficial)", String.valueOf(working2Found));

        // Bolseiro (info. RAIDES)
        final DynamicForm.Select scholarshipType = application == null ? null : fieldFor(application, "scholarshipType");
        if (scholarshipType != null && scholarshipType.valueLabel() != null) {
            row.setCell("bolseiro (info. RAIDES)", scholarshipType.valueLabel().stream()
                    .map(LocalizedString::getContent).collect(Collectors.joining(", ")));
        } else if (personalInformationBean.getGrantOwnerType() != null) {
            row.setCell("bolseiro (info. RAIDES)", personalInformationBean.getGrantOwnerType().getName());
        } else {
            row.setCell("bolseiro (info. RAIDES)", "");
        }

        // Instituição que atribuiu a bolsa
        final DynamicForm.Text scholarshipInstitution = application == null
                ? null : fieldFor(application, "scholarshipInstitution");
        if (scholarshipInstitution != null && scholarshipInstitution.value() != null) {
            row.setCell("instituição que atribuiu a bolsa (qd aplicável)", scholarshipInstitution.value());
        } if (personalInformationBean.getGrantOwnerType() != null
                && personalInformationBean.getGrantOwnerType().equals(GrantOwnerType.OTHER_INSTITUTION_GRANT_OWNER)) {
            row.setCell("instituição que atribuiu a bolsa (qd aplicável)",
                    personalInformationBean.getGrantOwnerProviderName());
        } else {
            row.setCell("instituição que atribuiu a bolsa (qd aplicável)", "");
        }

        // Bolseiro (info. oficial)
        boolean sasFound = false;
        for (StudentStatute statute : registration.getStudent().getStudentStatutesSet()) {
            if (statute.getType().isGrantOwnerStatute()
                    && statute.isValidInExecutionPeriod(executionYear.getFirstExecutionPeriod())) {
                sasFound = true;
                break;
            }
        }
        row.setCell("bolseiro (info. oficial)", String.valueOf(sasFound));

        // Grau Precedente
        if (dgesLine == null) {
            if (mostRecentPreviousQualification == null) {
                row.setCell("Grau Precedente", personalInformationBean.getPrecedentSchoolLevel() != null
                        ? personalInformationBean.getPrecedentSchoolLevel().getName() : "");
            } else {
                row.setCell("Grau Precedente", qualificationLevelFor(mostRecentPreviousQualification));
            }
        } else {
            row.setCell("Grau Precedente", "Ensino secundário (12.º ano de escolaridade completo) ou equivalente");
        }

        // Outro Grau Precedente
        if (otherQualification == null) {
            row.setCell("Outro Grau Precedente", personalInformationBean.getOtherPrecedentSchoolLevel());
        } else {
            row.setCell("Outro Grau Precedente", qualificationLevelFor(otherQualification));
        }

        // grau da habl anterior compl
        if (dgesLine == null) {
            if (mostRecentCompleteQualification == null) {
                row.setCell("grau habl anterior compl", personalInformationBean.getSchoolLevel() != null
                        ? personalInformationBean.getSchoolLevel().getName() : "");
            } else {
                row.setCell("grau habl anterior compl", qualificationLevelFor(mostRecentCompleteQualification));
            }
        } else {
            row.setCell("grau habl anterior compl",
                    "Ensino secundário (12.º ano de escolaridade completo) ou equivalente");
        }

        // Codigo do grau habl anterior
        if (dgesLine == null) {
            DegreeDesignation designation =
                    DegreeDesignation.readByNameAndSchoolLevel(personalInformationBean.getDegreeDesignation(),
                            personalInformationBean.getPrecedentSchoolLevel());
            row.setCell("Codigo do grau habl anterior", designation != null
                    ? designation.getDegreeClassification().getCode() : "");
        } else {
            row.setCell("Codigo do grau habl anterior", "ES");
        }

        // Outro grau da habl anterior compl
        if (otherCompleteQualification == null) {
            row.setCell("Outro grau habl anterior compl", personalInformationBean.getOtherSchoolLevel());
        } else {
            row.setCell("Outro grau habl anterior compl", qualificationLevelFor(otherCompleteQualification));
        }

        // País de Habilitação Anterior ao Curso Atual
        if (dgesLine == null) {
            if (mostRecentPreviousQualification == null) {
                row.setCell("país habilitação anterior", personalInformationBean
                        .getCountryWhereFinishedPreviousCompleteDegree() != null
                        ? personalInformationBean.getCountryWhereFinishedPreviousCompleteDegree().getName() : "");
            } else {
                row.setCell("país habilitação anterior", countryFor(mostRecentPreviousQualification));
            }
        } else {
            row.setCell("país habilitação anterior", "PT");
        }

        // País de Habilitação do 12.º ano ou equivalente
        if (dgesLine == null) {
            if (highSchoolQualification == null) {
                row.setCell("país habilitação 12º ano ou equivalente", personalInformationBean
                        .getCountryWhereFinishedHighSchoolLevel() != null
                        ? personalInformationBean.getCountryWhereFinishedHighSchoolLevel().getName() : "");
            } else {
                row.setCell("país habilitação 12º ano ou equivalente", countryFor(highSchoolQualification));
            }
        } else {
            row.setCell("país habilitação 12º ano ou equivalente", "PT");
        }

        // Ano de conclusão da habilitação anterior
        if (dgesLine == null) {
            if (mostRecentCompleteQualification == null) {
                row.setCell("ano de conclusão da habilitação anterior", personalInformationBean.getConclusionYear());
            } else {
                row.setCell("ano de conclusão da habilitação anterior",
                        qualificationEndFor(mostRecentCompleteQualification).getYear());
            }
        } else {
            row.setCell("ano de conclusão da habilitação anterior", application.getAdmissionProcessTarget()
                    .getAdmissionProcess().getStartApplicationSubmissionPeriod().getYear());
        }

        // Nota de conclusão da habilitação anterior
        if (dgesLine == null) {
            if (mostRecentCompleteQualification == null) {
                row.setCell("nota da habilitação anterior", personalInformationBean.getConclusionGrade() != null
                        ? personalInformationBean.getConclusionGrade() : "");
            } else {
                row.setCell("nota da habilitação anterior", gradeFors(mostRecentCompleteQualification));
            }
        } else {
            row.setCell("nota da habilitação anterior", dgesLine.getHighSchoolFinalGrade());
        }

        MobilityAgreement mobilityAgreement = null;
        ExecutionInterval chosenCandidacyInterval = null;
        //getting the last mobility program done
        for (OutboundMobilityCandidacySubmission outboundCandidacySubmission : registration
                .getOutboundMobilityCandidacySubmissionSet()) {
            if (outboundCandidacySubmission.getSelectedCandidacy() != null
                    && outboundCandidacySubmission.getSelectedCandidacy().getSelected()) {
                ExecutionInterval candidacyInterval =
                        outboundCandidacySubmission.getOutboundMobilityCandidacyPeriod().getExecutionInterval();
                //the candidacies are made in the previous year
                if (candidacyInterval.getAcademicInterval().isBefore(executionYear.getAcademicInterval())) {
                    if (mobilityAgreement != null) {
                        if (!candidacyInterval.getAcademicInterval().isAfter(chosenCandidacyInterval
                                .getAcademicInterval())) {
                            continue;
                        }
                    }
                    mobilityAgreement =
                            outboundCandidacySubmission.getSelectedCandidacy().getOutboundMobilityCandidacyContest()
                                    .getMobilityAgreement();
                    chosenCandidacyInterval = candidacyInterval;
                }
            }
        }
        if (mobilityAgreement == null) {
            final Application mobilityApplication = findMobilityApplicationFor(identity, registration);
            if (mobilityApplication == null) {
                row.setCell("Programa mobilidade", "");
                row.setCell("País mobilidade", "");
                row.setCell("Duração programa mobilidade", "");
            } else {
                row.setCell("Programa mobilidade", mobilityApplication.getAdmissionProcessTarget()
                        .getAdmissionProcess().getTitle().getContent());
                row.setCell("País mobilidade", extractName(mobilityApplication.getAdmissionProcessTarget()));
                row.setCell("Duração programa mobilidade", studeiesInterval(mobilityApplication));
            }
        } else {
            // Programa de mobilidade
            row.setCell("Programa mobilidade", mobilityAgreement != null
                    ? mobilityAgreement.getMobilityProgram().getName().getContent() : "");

            // País de mobilidade
            row.setCell("País mobilidade", mobilityAgreement != null
                    ? mobilityAgreement.getUniversityUnit().getCountry().getName() : "");

            // Duração do programa de mobilidade
            row.setCell("Duração programa mobilidade", personalInformationBean.getMobilityProgramDuration() != null
                    ? BundleUtil.getString(Bundle.ENUMERATION,
                    personalInformationBean.getMobilityProgramDuration().name()) : "");
        }

        // Tipo de Estabelecimento Frequentado no Ensino Secundário
        if (personalInformationBean.getHighSchoolType() != null) {
            row.setCell("tipo estabelecimento ensino secundário", personalInformationBean.getHighSchoolType()
                    .getName());
        } else {
            row.setCell("tipo estabelecimento ensino secundário", "");
        }

        int totalEnrolmentsInPreviousYear = 0;
        int totalEnrolmentsApprovedInPreviousYear = 0;
        //int totalEnrolmentsInFirstSemester = 0;
        double totalEctsConcludedUntilPreviousYear = 0d;
        for (final CycleCurriculumGroup cycleCurriculumGroup : studentCurricularPlan.getInternalCycleCurriculumGrops()) {

            totalEctsConcludedUntilPreviousYear +=
                    cycleCurriculumGroup.getCreditsConcluded(executionYear.getPreviousExecutionYear());

            totalEnrolmentsInPreviousYear +=
                    cycleCurriculumGroup.getEnrolmentsBy(executionYear.getPreviousExecutionYear()).size();

            for (final Enrolment enrolment : cycleCurriculumGroup.getEnrolmentsBy(executionYear
                    .getPreviousExecutionYear())) {
                if (enrolment.isApproved()) {
                    totalEnrolmentsApprovedInPreviousYear++;
                }
            }

            //	    totalEnrolmentsInFirstSemester += cycleCurriculumGroup.getEnrolmentsBy(executionYear.getFirstExecutionPeriod())
            //		    .size();
        }

        // Total de ECTS inscritos no total do ano
        double totalCreditsEnrolled = 0d;
        for (Enrolment enrollment : studentCurricularPlan.getEnrolmentsByExecutionYear(executionYear)) {
            if (!enrollment.isAnnulled()) {
                totalCreditsEnrolled += enrollment.getEctsCredits();
            }
        }
        row.setCell("total ECTS inscritos no ano", printDouble(totalCreditsEnrolled));

        // Total de ECTS concluídos até ao fim do ano lectivo anterior ao
        // que se
        // referem os dados (neste caso até ao fim de 2007/08) no curso actual
        double totalCreditsDismissed = 0d;
        for (Credits credits : studentCurricularPlan.getCreditsSet()) {
            if (credits.isEquivalence()) {
                totalCreditsDismissed += credits.getEnrolmentsEcts();
            }
        }
        row.setCell("total ECTS concluídos fim ano lectivo anterior", printDouble(totalEctsConcludedUntilPreviousYear));

        // Nº de Disciplinas Inscritos no ano lectivo anterior ao que se
        // referem
        // os dados
        row.setCell("nº. disciplinas inscritas ano lectivo anterior dados", totalEnrolmentsInPreviousYear);

        // Nº de Disciplinas Aprovadas no ano lectivo anterior ao que se
        // referem
        // os dados
        row.setCell("nº. disciplinas aprovadas ano lectivo anterior dados", totalEnrolmentsApprovedInPreviousYear);

        // N.º de Inscrições Externas no ano a que se referem os dados
        ExtraCurriculumGroup extraCurriculumGroup = studentCurricularPlan.getExtraCurriculumGroup();
        int extraCurricularEnrolmentsCount =
                extraCurriculumGroup != null ? extraCurriculumGroup.getEnrolmentsBy(executionYear).size() : 0;

        for (final CycleCurriculumGroup cycleCurriculumGroup : studentCurricularPlan.getExternalCurriculumGroups()) {
            extraCurricularEnrolmentsCount += cycleCurriculumGroup.getEnrolmentsBy(executionYear).size();
        }

        if (studentCurricularPlan.hasPropaedeuticsCurriculumGroup()) {
            extraCurricularEnrolmentsCount +=
                    studentCurricularPlan.getPropaedeuticCurriculumGroup().getEnrolmentsBy(executionYear).size();
        }

        row.setCell("nº. inscrições externas ano dados", extraCurricularEnrolmentsCount);

        // Estados de matrícula
        SortedSet<RegistrationState> states = new TreeSet<>(RegistrationState.DATE_COMPARATOR);
        for (Registration current : registrationPath) {
            states.addAll(current.getRegistrationStatesSet());
        }
        RegistrationState previousYearState = null;
        RegistrationState currentYearState = null;
        for (RegistrationState state : states) {
            if (!state.getStateDate().isAfter(
                    executionYear.getPreviousExecutionYear().getEndDateYearMonthDay().toDateTimeAtMidnight())) {
                previousYearState = state;
            }
            if (!state.getStateDate().isAfter(executionYear.getEndDateYearMonthDay().toDateTimeAtMidnight())) {
                currentYearState = state;
            }
        }

        // Estado da matrícula no ano lectivo anterior ao que se referem os
        // dados
        row.setCell("estado matrícula ano anterior dados", previousYearState != null
                ? previousYearState.getStateType().getDescription() : "n/a");

        // Estado (da matrícula) no ano a que se referem os dados
        row.setCell("estado matrícula ano dados", currentYearState != null
                ? currentYearState.getStateType().getDescription() : "n/a");

        // Data do estado de matrícula
        row.setCell("data do estado de matrícula", currentYearState != null
                ? currentYearState.getStateDate().toString("dd-MM-yyyy") : "n/a");

        // Nº ECTS do 1º Ciclo concluídos até ao fim do ano lectivo
        // anterior ao que se referem os dados
        final CycleCurriculumGroup firstCycleCurriculumGroup =
                getStudentCurricularPlan(registration, CycleType.FIRST_CYCLE).getCycle(CycleType.FIRST_CYCLE);
        row.setCell("nº. ECTS 1º ciclo concluídos fim ano lectivo anterior", firstCycleCurriculumGroup != null
                ? printBigDecimal(firstCycleCurriculumGroup.getCurriculum(executionYear).getSumEctsCredits()) : "");

        // Nº ECTS do 2º Ciclo concluídos até ao fim do ano lectivo
        // anterior ao que se referem os dados
        final CycleCurriculumGroup secondCycleCurriculumGroup =
                getStudentCurricularPlan(registration, CycleType.SECOND_CYCLE).getCycle(CycleType.SECOND_CYCLE);
        row.setCell("nº. ECTS 2º ciclo concluídos fim ano lectivo anterior", secondCycleCurriculumGroup != null
                && !secondCycleCurriculumGroup.isExternal()
                ? printBigDecimal(secondCycleCurriculumGroup.getCurriculum(executionYear).getSumEctsCredits()) : "");

        // Nº ECTS do 2º Ciclo Extra primeiro ciclo concluídos até ao fim do ano
        // lectivo anterior ao que se referem os dados
        Double extraFirstCycleEcts = 0d;
        for (final CycleCurriculumGroup cycleCurriculumGroup : studentCurricularPlan.getExternalCurriculumGroups()) {
            for (final CurriculumLine curriculumLine : cycleCurriculumGroup.getAllCurriculumLines()) {
                if (!curriculumLine.getExecutionYear().isAfter(executionYear.getPreviousExecutionYear())) {
                    extraFirstCycleEcts += curriculumLine.getCreditsConcluded(executionYear.getPreviousExecutionYear());
                }
            }
        }
        row.setCell("nº. ECTS extra 1º ciclo concluídos fim ano lectivo anterior", printDouble(extraFirstCycleEcts));

        // Nº ECTS Extracurriculares concluídos até ao fim do ano lectivo
        // anterior que ao se referem os dados
        double extraCurricularEcts = 0d;
        double allExtraCurricularEcts = 0d;
        if (extraCurriculumGroup != null) {
            for (final CurriculumLine curriculumLine : extraCurriculumGroup.getAllCurriculumLines()) {
                if (curriculumLine.isApproved() && curriculumLine.hasExecutionPeriod()
                        && !curriculumLine.getExecutionYear().isAfter(executionYear.getPreviousExecutionYear())) {
                    extraCurricularEcts += curriculumLine.getEctsCreditsForCurriculum().doubleValue();
                }
                if (curriculumLine.hasExecutionPeriod() && curriculumLine.getExecutionYear() == executionYear) {
                    allExtraCurricularEcts += curriculumLine.getEctsCreditsForCurriculum().doubleValue();
                }
            }
        }
        row.setCell("nº. ECTS extracurriculares concluídos fim ano lectivo anterior", printDouble(extraCurricularEcts));

        // Nº ECTS Propedeutic concluídos até ao fim do ano lectivo
        // anterior que ao se referem os dados
        double propaedeuticEcts = 0d;
        double allPropaedeuticEcts = 0d;
        if (studentCurricularPlan.getPropaedeuticCurriculumGroup() != null) {
            for (final CurriculumLine curriculumLine : studentCurricularPlan.getPropaedeuticCurriculumGroup()
                    .getAllCurriculumLines()) {
                if (curriculumLine.isApproved() && curriculumLine.hasExecutionPeriod()
                        && !curriculumLine.getExecutionYear().isAfter(executionYear.getPreviousExecutionYear())) {
                    propaedeuticEcts += curriculumLine.getEctsCreditsForCurriculum().doubleValue();
                }
                if (curriculumLine.hasExecutionPeriod() && curriculumLine.getExecutionYear() == executionYear) {
                    allPropaedeuticEcts += curriculumLine.getEctsCreditsForCurriculum().doubleValue();
                }
            }
        }
        row.setCell("nº. ECTS Propedeuticas concluídos fim ano lectivo anterior", printDouble(propaedeuticEcts));

        // N.º ECTS inscritos em unidades curriculares propedêuticas e em
        // extracurriculares
        row.setCell("nº. ECTS inscritos em Propedeut e extra-curriculares", printDouble(allPropaedeuticEcts
                + allExtraCurricularEcts));

        // N.º ECTS equivalência/substituição/dispensa
        row.setCell("nº. ECTS equivalência/substituição/dispensa", printDouble(totalCreditsDismissed));

        // Tem situação de propinas no letivo dos dados
        row.setCell("Tem situação de propinas no lectivo dos dados?", String.valueOf(studentCurricularPlan
                .hasAnyGratuityEventFor(executionYear)));

        return row;
    }

    private static @Nullable Double getEntryGrade(final Registration registration,
                                                  final Registration sourceRegistration) {
        Double entryGrade = registration.getEntryGrade();
        if (entryGrade == null && registration.getStudentCandidacy() != null) {
            entryGrade = registration.getStudentCandidacy().getEntryGrade();
        }
        if (entryGrade == null && sourceRegistration != null) {
            entryGrade = sourceRegistration.getEntryGrade();
            if (entryGrade == null && sourceRegistration.getStudentCandidacy() != null) {
                entryGrade = sourceRegistration.getStudentCandidacy().getEntryGrade();
            }
        }
        return entryGrade;
    }

    private <T extends DynamicForm.Field> T fieldFor(final Application application, final String field) {
        final DynamicForm dynamicForm = formFor(application, field);
        return dynamicForm == null ? null : dynamicForm.get(field);
    }

    private String studeiesInterval(final Application application) {
        try {
            final JsonObject form = application.getAdmissionProcessTarget().getAdmissionProcess().getFormDataJson();
            final DynamicForm dynamicForm = new DynamicForm(form).overwriteReadonly(true)
                    .withData(application.getDataObject().getAsJsonObject("formData"));
            final DateTime arrivalDate = dynamicForm.get("arrivalDate").value();
            final DateTime departureDate = dynamicForm.get("departureDate").value();
            final int days = Days.daysBetween(arrivalDate, departureDate).getDays();
            return days > 200 ? SchoolPeriodDuration.YEAR.name() : SchoolPeriodDuration.SEMESTER.name();
        } catch (IllegalArgumentException ex) {
            return SchoolPeriodDuration.SEMESTER.name();
        }
    }

    private String extractName(final AdmissionProcessTarget target) {
        final String content = target.getName().getContent();
        final int i = content.indexOf('(');
        if (i > 0) {
            final int j = content.indexOf(')', i);
            if (j > 0) {
                return content.substring(i + 1, j);
            }
        }
        return "";
    }

    private Set<DynamicForm.Field> findMostRecentPreviousQualification(final Application application) {
        final DynamicForm form = formFor(application, "qualifications");
        if (form != null) {
            final DynamicForm.Array array = form.get("qualifications");
            final Stream<Set<DynamicForm.Field>> qualifications = extractQualifications(array);
            return qualifications
                    .filter(fields -> qualificationDateFor(fields) != null)
                    .max(Comparator.comparing(this::qualificationDateFor))
                    .orElse(null);
        }
        return null;
    }

    private Set<DynamicForm.Field> findMostRecentCompleteQualification(final Application application) {
        final DynamicForm form = formFor(application, "qualifications");
        if (form != null) {
            final DynamicForm.Array array = form.get("qualifications");
            final Stream<Set<DynamicForm.Field>> qualifications = extractQualifications(array);
            return qualifications
                    .filter(fields -> qualificationEndFor(fields) != null)
                    .max(Comparator.comparing(this::qualificationEndFor))
                    .orElse(null);
        }
        return null;
    }

    private Set<DynamicForm.Field> findOtherQualification(final Application application, final String... exclude) {
        final DynamicForm form = formFor(application, "qualifications");
        if (form != null) {
            final DynamicForm.Array array = form.get("qualifications");
            final Stream<Set<DynamicForm.Field>> qualifications = extractQualifications(array);
            return qualifications
                    .filter(fields -> !contains(fields, exclude))
                    .findAny()
                    .orElse(null);
        }
        return null;
    }

    private Set<DynamicForm.Field> findOtherCompleteQualification(final Application application,
                                                                  final String... exclude) {
        final DynamicForm form = formFor(application, "qualifications");
        if (form != null) {
            final DynamicForm.Array array = form.get("qualifications");
            final Stream<Set<DynamicForm.Field>> qualifications = extractQualifications(array);
            return qualifications
                    .filter(fields -> qualificationEndFor(fields) != null)
                    .filter(fields -> !contains(fields, exclude))
                    .max(Comparator.comparing(this::qualificationEndFor))
                    .orElse(null);
        }
        return null;
    }

    private boolean contains(final Set<DynamicForm.Field> fields, final String[] exclude) {
        final String fieldsString = toString(fields);
        for (final String s : exclude) {
            if (s != null && s.equals(fieldsString)) {
                return true;
            }
        }
        return false;
    }

    private String toString(final Set<DynamicForm.Field> fields) {
        return fields == null ? null : fields.stream()
                .filter(Objects::nonNull)
                .map(DynamicForm.Field::toString).collect(Collectors.joining(", "));
    }

    private Set<DynamicForm.Field> findHighSchoolQualification(final Application application) {
        final DynamicForm form = formFor(application, "qualifications");
        if (form != null) {
            final DynamicForm.Array array = form.get("qualifications");
            final Stream<Set<DynamicForm.Field>> qualifications = extractQualifications(array);
            return qualifications
                    .filter(fields -> qualificationLevelFor(fields) != null
                            && qualificationLevelFor(fields).contains("12."))
                    .findAny()
                    .orElse(null);
        }
        return null;
    }

    private String qualificationLevelFor(final Set<DynamicForm.Field> fields) {
        return fields.stream()
                .filter(field -> field.getName().equals("qualificationLevel"))
                .map(this::toValue)
                .filter(Objects::nonNull)
                .findAny().orElse(null);
    }

    private DateTime qualificationEndFor(final Set<DynamicForm.Field> fields) {
        return fields.stream()
                .filter(field -> field.getName().equals("qualificationEnd"))
                .map(this::toDateTime)
                .filter(Objects::nonNull)
                .findAny()
                .orElse(null);
    }

    private DateTime toDateTime(final DynamicForm.Field field) {
        //return ((DynamicForm.DateTime) field).value();
        final JsonElement element = field.getData();
        if (element != null && !element.isJsonNull()) {
            String value = element.getAsString();
            try {
                return ISODateTimeFormat.date().parseDateTime(value);
            } catch (final IllegalArgumentException ex) {
                value = value.replace(" ", "").replace("-", "/");
                if (value.length() == 4) {
                    value = "01/01" + value;
                }
                if (value.equals("2020/21")) {
                    value = "01/01/2021";
                }
                final int i = value.indexOf('/');
                if (i >= 0) {
                    final int j = value.indexOf('/', i + 1);
                    if (j > 0) {
                        try {
                            String year = value.substring(j + 1);
                            if (year.length() == 2) {
                                year = "20" + year;
                            }
                            return new LocalDate(new BigDecimal(year).intValue(),
                                    new BigDecimal(value.substring(i + 1, j)).intValue(),
                                    new BigDecimal(value.substring(0, i)).intValue())
                                    .toDateTimeAtStartOfDay();
                        } catch (IllegalFieldValueException | NumberFormatException ignored) {
                        }
                    }
                }
                if (value.length() == 8) {
                    try {
                        return new LocalDate(new BigDecimal(value.substring(4)).intValue(),
                                new BigDecimal(value.substring(2, 4)).intValue(),
                                new BigDecimal(value.substring(0, 2)).intValue())
                                .toDateTimeAtStartOfDay();
                    } catch (IllegalFieldValueException | NumberFormatException ignored) {
                    }
                }
            }
        }
        return null;
    }

    private DateTime qualificationStartFor(final Set<DynamicForm.Field> fields) {
        return fields.stream()
                .filter(field -> field.getName().equals("qualificationStart"))
                .map(this::toDateTime)
                .filter(Objects::nonNull)
                .findAny()
                .orElse(null);
    }

    private DateTime qualificationDateFor(final Set<DynamicForm.Field> fields) {
        final DateTime end = qualificationEndFor(fields);
        return end == null ? qualificationStartFor(fields) : end;
    }

    private String countryFor(final Set<DynamicForm.Field> fields) {
        return fields.stream()
                .filter(field -> field.getName().equals("qualificationCountry"))
                .map(field -> ((DynamicForm.AsyncSelect) field).value())
                .filter(Objects::nonNull)
                .findAny().orElse(null);
    }

    private String gradeFors(final Set<DynamicForm.Field> fields) {
        return fields.stream()
                .filter(field -> field.getName().equals("qualificationGrade"))
                .map(this::toValue)
                .filter(Objects::nonNull)
                .findAny()
                .orElse(null);
    }

    private String institutionFor(Set<DynamicForm.Field> fields) {
        final String r1 = fields.stream()
                .filter(field -> field.getName().equals("qualificationInstitutionK12"))
                .map(this::toValue)
                .filter(Objects::nonNull)
                .findAny()
                .orElse(null);
        final String r2 = fields.stream()
                .filter(field -> field.getName().equals("qualificationInstitution"))
                .map(this::toValue)
                .filter(Objects::nonNull)
                .findAny()
                .orElse(null);
        return r1 == null || r1.isEmpty() ? r1 : r2;
    }

    private String programFor(Set<DynamicForm.Field> fields) {
        return fields.stream()
                .filter(field -> field.getName().equals("qualificationCourse"))
                .map(this::toValue)
                .filter(Objects::nonNull)
                .findAny()
                .orElse(null);
    }

    private String toValue(final DynamicForm.Field field) {
        return field instanceof DynamicForm.Quantity ? ((DynamicForm.Quantity) field).value().toPlainString()
                : field instanceof DynamicForm.Text ? ((DynamicForm.Text) field).value()
                : field instanceof DynamicForm.Select ? toValueSelect((DynamicForm.Select) field) : null;
    }

    private String toValueSelect(final DynamicForm.Select field) {
        final JsonElement data = field.getData();
        return data == null || data.isJsonNull()
                ? null : LocalizedString.fromJson(data.getAsJsonObject().get("label")).getContent();
    }

    private Stream<Set<DynamicForm.Field>> extractQualifications(final DynamicForm.Array array) {
        try {
            final Field field = DynamicForm.Array.class.getDeclaredField("items");
            field.setAccessible(true);
            final List<Set<DynamicForm.Field>> list = (List<Set<DynamicForm.Field>>) field.get(array);
            return list.stream();
        } catch (final NoSuchFieldException | IllegalAccessException e) {
            throw new Error(e);
        }
    }

    private DynamicForm formFor(final Application application, final String field) {
        final AdmissionProcessTarget target = application.getAdmissionProcessTarget();
        final AdmissionProcess process = target.getAdmissionProcess();
        if (process.getFormData().indexOf(field) > 0) {
            final JsonObject form = process.getFormDataJson();
            return new DynamicForm(form).withData(application.getDataObject().getAsJsonObject("formData"));
        }
        if (process.getOutcomeConfig().indexOf(field) > 0) {
            final JsonObject outcomeConfig = process.getOutcomeConfigJson();
            final JsonObject forms = outcomeConfig.getAsJsonObject("forms");
            if (forms != null) {
                final JsonObject before = forms.getAsJsonObject("beforeOutcome");
                if (before != null && before.toString().indexOf(field) > 0) {
                    return new DynamicForm(before).withData(application.getDataObject()
                            .getAsJsonObject("outcomeFormData").getAsJsonObject("beforeOutcome"));
                }
                final JsonObject after = forms.getAsJsonObject("afterOutcome");
                if (after != null && after.toString().indexOf(field) > 0) {
                    return new DynamicForm(after).withData(application.getDataObject()
                            .getAsJsonObject("outcomeFormData").getAsJsonObject("afterOutcome"));
                }
            }
        }
        return null;
    }

    private DegreeCandidateDTO dgesLineFor(final Application application) {
        if (application != null) {
            final String dgesLine = JsonUtils.get(application.getDataObject(), "dgesLine");
            if (dgesLine != null && !dgesLine.isEmpty()) {
                try {
                    final DegreeCandidateDTO dto = new DegreeCandidateDTO();
                    dto.fillWithFileLineData(dgesLine);
                    return dto;
                } catch (Throwable ignored) {
                }
            }
        }
        return null;
    }

    private Application findApplicationFor(final Identity identity, final Registration registration) {
        return identity.getAccountSet().stream()
                .flatMap(account -> account.getApplicationSet().stream())
                .filter(Utils::isDegreeType)
                .filter(application -> Utils.registrationFor(application) == registration)
                .findAny().orElse(null);
    }

    private Application findMobilityApplicationFor(final Identity identity, final Registration registration) {
        return identity.getAccountSet().stream()
                .flatMap(account -> account.getApplicationSet().stream())
                .filter(Utils::isMobilityType)
                .filter(application -> Utils.registrationFor(application) == registration)
                .findAny().orElse(null);
    }

    private void registerMobility(Registration registration, ExecutionYear executionYear, Spreadsheet.Row row) {
        boolean inMobility = registration.getIndividualCandidacy() != null
                && registration.getIndividualCandidacy().isErasmus();
        if (inMobility) {
            final MobilityIndividualApplication mia = (MobilityIndividualApplication) registration
                    .getIndividualCandidacy();
            final MobilityIndividualApplicationProcess applicationProcess = mia.getCandidacyProcess();
            final ExecutionYear startExecutionYear = applicationProcess.getCandidacyExecutionInterval();
            final RegistrationState lastState = registration.getLastState();
            DateTime concludedStudiesDate = null;
            if (lastState.getStateType() == RegistrationStateType.SCHOOLPARTCONCLUDED) {
                concludedStudiesDate = lastState.getStateDate();
            }
            final MobilityApplicationProcess process = (MobilityApplicationProcess) applicationProcess
                    .getCandidacyProcess();
            final ErasmusApplyForSemesterType forSemester = process.getForSemester();
            final Country precedentCountry = mia.getRefactoredPrecedentDegreeInformation().getPrecedentCountry();

            row.setCell("Ano lectivo MobilidadeIN", startExecutionYear.getName());
            row.setCell("Semestre MobilidadeIN", forSemester.getLocalizedName());
            row.setCell("Fim MobilidadeIN", concludedStudiesDate != null
                    ? concludedStudiesDate.toString("dd-MM-yyyy") : "");
            row.setCell("País MobilidadeIN", precedentCountry.getName());
        }
        Optional<RegistrationState> optionalRegistrationState = registration.getRegistrationStates(executionYear).stream()
                .filter(rs -> rs.getStateType() == RegistrationStateType.MOBILITY)
                .findAny();
        boolean outMobility = optionalRegistrationState.isPresent();
        if (outMobility) {
            final RegistrationState registrationState = optionalRegistrationState.get();
            final ExecutionYear startExecutionYear = registrationState.getExecutionYear();
            DateTime concludedMobility = registrationState.getEndDate();
            final Optional<Country> anyCountry = registration.getSortedExternalEnrolments().stream()
                    .filter(e -> e.getExecutionYear() == executionYear)
                    .filter(e -> e.getExternalCurricularCourse().getUnit().getCountry() != Country.readDefault())
                    .map(e -> e.getExternalCurricularCourse().getUnit().getCountry())
                    .filter(Objects::nonNull)
                    .findAny();
            Country country = null;
            if (anyCountry.isEmpty()) {
                final Set<OutboundMobilityCandidacySubmission> candidacies = registration
                        .getOutboundMobilityCandidacySubmissionSet().stream()
                        .filter(oc -> oc.getSelectedCandidacy() != null)
                        .collect(Collectors.toSet());
                if (candidacies.size() == 1) {
                    country = candidacies.iterator().next().getSelectedCandidacy()
                            .getOutboundMobilityCandidacyContest().getMobilityAgreement()
                            .getUniversityUnit().getCountry();
                }
            } else {
                country = anyCountry.get();
            }

            row.setCell("Ano lectivo MobilidadeOUT", startExecutionYear.getName());
            row.setCell("Semestre MobilidadeIN", ExecutionSemester.readByDateTime(registrationState
                    .getStateDate()).getName());
            row.setCell("Fim MobilidadeOUT", concludedMobility != null
                    ? concludedMobility.toString("dd-MM-yyyy") : "");
            row.setCell("País MobilidadeOUT", country != null ? country.getName() : "");
        }
    }

    protected List<Registration> getFullRegistrationPath(final Registration current) {
        if (current.getDegreeType().isBolonhaDegree() || current.getDegreeType().isIntegratedMasterDegree()) {
            List<Registration> path = new ArrayList<>();
            path.add(current);
            Registration source;
            if (current.getSourceRegistration() != null
                    && (!(source = current.getSourceRegistration()).isBolonha() || isValidSourceLink(source))) {
                path.addAll(getFullRegistrationPath(source));
            } else if ((source = findSourceRegistrationByEquivalencePlan(current)) != null) {
                path.addAll(getFullRegistrationPath(source));
            }
            path.sort(Registration.COMPARATOR_BY_START_DATE);
            return path;
        } else {
            return Collections.singletonList(current);
        }
    }

    protected boolean isValidSourceLink(Registration source) {
        return Stream.of(RegistrationStateType.TRANSITED, RegistrationStateType.FLUNKED,
                        RegistrationStateType.INTERNAL_ABANDON, RegistrationStateType.EXTERNAL_ABANDON,
                        RegistrationStateType.INTERRUPTED)
                .anyMatch(registrationStateType -> source.getActiveStateType() == registrationStateType);
    }

    private Registration findSourceRegistrationByEquivalencePlan(Registration targetRegistration) {
        final DegreeCurricularPlan targetDegreeCurricularPlan = targetRegistration.getLastDegreeCurricularPlan();
        if (targetDegreeCurricularPlan.getEquivalencePlan() != null) {
            for (Registration sourceRegistration : targetRegistration.getStudent().getRegistrationsSet()) {
                final DegreeCurricularPlan sourceDegreeCurricularPlan = sourceRegistration
                        .getLastDegreeCurricularPlan();
                if (sourceRegistration != targetRegistration
                        && sourceRegistration.getActiveStateType() == RegistrationStateType.TRANSITED
                        && targetDegreeCurricularPlan.getEquivalencePlan().getSourceDegreeCurricularPlan()
                        .equals(sourceDegreeCurricularPlan)) {
                    return sourceRegistration;
                }
            }
        }
        return null;
    }

    private String printBigDecimal(BigDecimal value) {
        return value == null ? "" : value.toPlainString().replace('.', ',');
    }

    private String printDouble(Double value) {
        return value == null ? "" : value.toString().replace('.', ',');
    }

    private StudentCurricularPlan getStudentCurricularPlan(Registration registration, CycleType cycleType) {
        return registration.getStudentCurricularPlan(cycleType);
    }

    private ExecutionYear executionYearFor(final AdmissionProcess admissionProcess) {
        return admissionProcess.getAdmissionProcessTargetSet().stream()
                .map(AdmissionProcessTarget::getOutcomeConfigJson)
                .map(config -> FenixFramework.getDomainObject(config.get("year").getAsString()))
                .map(ExecutionYear.class::cast)
                .findAny().orElse(null);
    }

    private Spreadsheet.Row row(final Spreadsheet spreadsheet) {
        synchronized (spreadsheet) {
            return spreadsheet.addRow();
        }
    }

    private <T> void process(final Set<T> set, final Consumer<T> consumer) {
        set.forEach(t -> processAtomic(t, consumer));
    }

    private <T> void processAtomic(final T t, final Consumer<T> consumer) {
        try {
            FenixFramework.atomic(() -> consumer.accept(t));
        } catch (final Exception e) {
            throw new Error(e);
        }
    }

    private void upload(final Spreadsheet spreadsheet, final String filename) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            spreadsheet.exportToXLSSheet(baos);
        } catch (final IOException e) {
            throw new Error(e);
        }
        upload("", filename, baos.toByteArray());
    }

    private void upload(final String path, final String filename, final byte[] content) {
        final FileSupport fileSupport = FileSupport.getInstance();
        final DriveAPIStorage driveAPIStorage = fileSupport.getFileStorageSet().stream()
                .filter(DriveAPIStorage.class::isInstance)
                .map(DriveAPIStorage.class::cast)
                .findAny().orElseThrow(Error::new);

        final MultipartBody request = Unirest.post(driveAPIStorage.getDriveUrl() + "/api/drive/directory/"
                        + REPO_NODE_ID)
                .header("Authorization", "Bearer " + getAccessToken(driveAPIStorage))
                .header("X-Requested-With", "XMLHttpRequest")
                .field("path", path);
        final Function<MultipartBody, MultipartBody> fileSetter = b -> b.field("file", content, filename);
        final HttpResponse<String> response = fileSetter.apply(request).asString();
        final JsonObject result = JsonParser.parseString(response.getBody()).getAsJsonObject();
        final JsonElement id = result.get("id");
        if (id == null || id.isJsonNull()) {
            throw new Error(result.toString());
        }
    }

    private transient String accessToken = null;
    private transient long accessTokenValidUnit = System.currentTimeMillis() - 1;

    private String getAccessToken(final DriveAPIStorage driveAPIStorage) {
        if (accessToken == null || System.currentTimeMillis() >= accessTokenValidUnit) {
            synchronized (this) {
                if (accessToken == null || System.currentTimeMillis() >= accessTokenValidUnit) {
                    final JsonObject claim = new JsonObject();
                    claim.addProperty("username", driveAPIStorage.getRemoteUsername());
                    accessToken = Tools.sign(SignatureAlgorithm.RS256, CoreConfiguration.getConfiguration()
                            .jwtPrivateKeyPath(), claim);
                }
            }
        }
        return accessToken;
    }

}