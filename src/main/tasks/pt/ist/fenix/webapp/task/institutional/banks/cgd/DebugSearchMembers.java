package pt.ist.fenix.webapp.task.institutional.banks.cgd;

import com.microsoft.schemas._2003._10.serialization.arrays.ArrayOfstring;
import com.qubit.solution.fenixedu.integration.cgd.domain.configuration.CgdIntegrationConfiguration;
import com.qubit.solution.fenixedu.integration.cgd.services.form43.CgdForm43Sender;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.datacontract.schemas._2004._07.wingman_cgd_caixaiu_datacontract.School;
import org.fenixedu.academic.domain.Country;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.ProfessionalSituationConditionType;
import org.fenixedu.academic.domain.contacts.PhysicalAddress;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.person.Gender;
import org.fenixedu.academic.domain.person.IDDocumentType;
import org.fenixedu.academic.domain.student.PersonalIngressionData;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.bennu.papyrus.service.PapyrusPdfRendererService;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.spaces.domain.Space;
import org.joda.time.DateTime;
import org.joda.time.YearMonthDay;
import pt.ist.fenixedu.integration.domain.cgd.CgdCard;
import pt.ist.fenixedu.integration.ui.spring.service.RegistrationDeclarationForBanksService;
import pt.ist.fenixframework.FenixFramework;
import pt.ist.papyrus.PapyrusClient;
import pt.ist.papyrus.PapyrusConfiguration;
import pt.ist.papyrus.PapyrusSettings;
import pt.ist.registration.process.ui.service.RegistrationDeclarationDataProvider;
import services.caixaiu.cgd.wingman.iesservice.Client;
import services.caixaiu.cgd.wingman.iesservice.Form43Digital;
import services.caixaiu.cgd.wingman.iesservice.IIESService;
import services.caixaiu.cgd.wingman.iesservice.IdentificationCard;
import services.caixaiu.cgd.wingman.iesservice.ObjectFactory;
import services.caixaiu.cgd.wingman.iesservice.OperationResult;
import services.caixaiu.cgd.wingman.iesservice.SetForm43DigitalData;
import services.caixaiu.cgd.wingman.iesservice.SetForm43DigitalDataResponse;
import services.caixaiu.cgd.wingman.iesservice.ValidationResult;
import services.caixaiu.cgd.wingman.iesservice.Worker;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.ws.BindingProvider;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class DebugSearchMembers extends ReadCustomTask {

    private static services.caixaiu.cgd.wingman.iesservice.ObjectFactory objectFactory = new ObjectFactory();

    @Override
    public void runTask() throws Exception {
        System.setProperty("com.sun.xml.ws.transport.http.client.HttpTransportPipe.dump", "true");
        System.setProperty("com.sun.xml.internal.ws.transport.http.client.HttpTransportPipe.dump", "true");
        System.setProperty("com.sun.xml.ws.transport.http.HttpAdapter.dump", "true");
        System.setProperty("com.sun.xml.internal.ws.transport.http.HttpAdapter.dump", "true");
        System.setProperty("com.sun.xml.internal.ws.transport.http.HttpAdapter.dumpTreshold", "9999999999999999999999999999999999999");

/*
        final SearchMemberInput input = new SearchMemberInput();
        input.setDocumentID("230124674");
        input.setDocumentType(501);
        final SearchMemberOutput output = new CgdIntegrationService().searchMember(input);
        taskLog("%s = %s%n", output.getReplyCode(), output.getMemberInfo().stream()
                .map(info -> info.getName())
                .collect(Collectors.joining("; ")));
        final User user = User.findByUsername("ist423218");
 */
        final CgdCard cgdCard = FenixFramework.getDomainObject("851898173307065");
/*
        final RegistrationDeclarationForBanksService rservice = new RegistrationDeclarationForBanksService(
                new RegistrationDeclarationDataProvider(),
                new PapyrusPdfRendererService(new PapyrusClient(PapyrusConfiguration.getConfiguration().papyrusUrl(),
                        PapyrusConfiguration.getConfiguration().papyrusToken()), PapyrusSettings.newBuilder().build()));
        final SendCgdCardService service = new SendCgdCardService(rservice);
        service.sendCgdCard(cgdCard);
 */
        xpto(cgdCard);
    }

    public void xpto(final CgdCard cgdCard) {
        if (cgdCard == null) {
            taskLog("CGD: Não existe cartão para este pedido.");
            return;
        }
        final Person person = cgdCard.getUser().getPerson();
        final String username = cgdCard.getUser().getUsername();
        if (BooleanUtils.isTrue(cgdCard.getAllowSendBankDetails())) {
            if (person != null) {
                final Student student = person.getStudent();
                if (student != null) {
                    for (final Registration registration : student.getRegistrationsSet()) {
                        if (registration.isActive()) {
                            CgdForm43Sender sender = new CgdForm43Sender();
                            final IIESService service = sender.getClient();
                            try {
                                final Method method = sender.getClass().getDeclaredMethod("getService");
                                method.setAccessible(true);
                                final BindingProvider provider = (BindingProvider) method.invoke(sender);

                                final org.apache.cxf.endpoint.Client client = ClientProxy.getClient(provider);
                                LoggingInInterceptor loggingInInterceptor = new LoggingInInterceptor();
                                loggingInInterceptor.setPrettyLogging(true);
                                LoggingOutInterceptor loggingOutInterceptor = new LoggingOutInterceptor();
                                loggingOutInterceptor.setPrettyLogging(true);

                                client.getInInterceptors().add(loggingInInterceptor);
                                client.getOutInterceptors().add(loggingOutInterceptor);

                            } catch (final NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                                throw new Error(e);
                            }
                            boolean form = sendForm43For(registration, true, service);

                            final RegistrationDeclarationForBanksService rservice = new RegistrationDeclarationForBanksService(
                                    new RegistrationDeclarationDataProvider(),
                                    new PapyrusPdfRendererService(new PapyrusClient(PapyrusConfiguration.getConfiguration().papyrusUrl(),
                                            PapyrusConfiguration.getConfiguration().papyrusToken()), PapyrusSettings.newBuilder().build()));

                            boolean attachment = sender.uploadFormAttachment(registration, rservice
                                    .getRegistrationDeclarationFileForBanks(registration));
                            taskLog("Sent Form43 ({}) and registration declaration file ({}) for registration {}%n",
                                    form, attachment, registration.getExternalId() );
                            if (form && attachment) {
                                FenixFramework.atomic(() -> cgdCard.setSuccessfulSentData(new DateTime()));
                                taskLog(String.format("CGD: Comunicação efectuada à CGD com sucesso para o utilizador %s%n", username));
                                return;
                            } else {
                                taskLog(String.format("CGD: Comunicação falhou para o utilizador %s. Contactar a CGD.%n", username));
                                return;
                            }
                        }
                    }
                    taskLog(String.format("CGD: Não existe uma matrícula activa para o aluno %s%n", username));
                    return;
                }
                taskLog(String.format("CGD: Utilizador %s não é aluno%n", username));
                return;
            } else {
                taskLog(String.format("CGD: Utilizador %s não tem pessoa activa%n", username));
                return;
            }
        }
        taskLog(String.format("CGD: %s - É necessário autorização a cedência de dados à CGD para efeitos de abertura de conta%n", username));
    }

    public boolean sendForm43For(Registration registration, boolean requestCard, IIESService service) {
        boolean success = false;
        try {
            org.fenixedu.academic.domain.Person person = registration.getStudent().getPerson();
            Client clientData = createClient(person, service, registration);
            services.caixaiu.cgd.wingman.iesservice.Person personData = createPerson(person);
            Worker workerData = createWorker(person);
            services.caixaiu.cgd.wingman.iesservice.Student studentData = createStudent(registration);

            Form43Digital form43Digital = new Form43Digital();
            form43Digital.setClientData(clientData);
            form43Digital.setPersonData(personData);
            form43Digital.setProfessionalData(workerData);
            form43Digital.setStudentData(studentData);
            form43Digital.setIDCardProduction(requestCard);

            final SetForm43DigitalData input = new SetForm43DigitalData();
            input.setValue(new ObjectFactory().createForm43Digital(form43Digital));
            export("form", input);

            OperationResult setForm43DigitalData = service.setForm43DigitalData(form43Digital);

//            export("service", service);

            final SetForm43DigitalDataResponse output = new SetForm43DigitalDataResponse();
            output.setSetForm43DigitalDataResult(new ObjectFactory().createSetForm43DigitalDataResponseSetForm43DigitalDataResult(setForm43DigitalData));

            export("result", output);

            success = !setForm43DigitalData.isError();
            if (!success) {
                taskLog("Problems while trying to send form 43 to student with number: "
                        + registration.getStudent().getNumber() + " with message: "
                        + setForm43DigitalData.getFriendlyMessage().getValue() + "\nCode id: " + setForm43DigitalData.getCodeId()
                        + "\n Unique Error ID: " + setForm43DigitalData.getUEC()
                        + "\n In case there are violations they'll be present bellow ");
                for (ValidationResult validation : setForm43DigitalData.getViolations().getValue().getValidationResult()) {
                    taskLog("Validation error : " + validation.getErrorMessage().getValue() + " [member: "
                            + validation.getMemberNames().getValue().getString().toString() + "]");
                }
            } else {
                taskLog("Sent successfuly form 43 for student with number:" + registration.getStudent().getNumber());
            }
        } catch (Throwable t) {
            taskLog("Problems while trying to send form43 for student with number: " + registration.getStudent().getNumber(),
                    t);
            throw new java.lang.Error(t);
        }

        return success;
    }

    private void export(final String label, final Object o) {
        try {
            JAXBContext contextObj = JAXBContext.newInstance(o.getClass());
            Marshaller marshallerObj = contextObj.createMarshaller();
            marshallerObj.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            final ByteArrayOutputStream stream = new ByteArrayOutputStream();
            marshallerObj.marshal(o, stream);
            output(label + ".xml", stream.toByteArray());
        } catch (final JAXBException e) {
            throw new Error(e);
        }
    }

    private static Client createClient(org.fenixedu.academic.domain.Person person, IIESService service, final Registration registration) {
        Client client = new Client();
        String findIES = findIES(getInstitutionCode(registration), service);
        client.setIES(findIES);
        client.setGroup(objectFactory.createClientGroup("1")); // Fernando Nunes indicou que é o protocolo e neste caso será sempre 1
        client.setMemberCategoryCode("91"); // Resposta da Carla Récio a 19 do 6 indica que grande parte das escolas usam ALUNOS
        String retrieveMemberID = CgdIntegrationConfiguration.getInstance().getMemberIDStrategy().retrieveMemberID(person);
        client.setMemberNumber(retrieveMemberID);

        return client;
    }

    private static services.caixaiu.cgd.wingman.iesservice.Student createStudent(Registration registration) {
        services.caixaiu.cgd.wingman.iesservice.Student student = new services.caixaiu.cgd.wingman.iesservice.Student();
        student.setSchoolCode(getInstitutionCode(registration));
        student.setCourse(registration.getDegree().getIdCardName());
        String retrieveMemberID = CgdIntegrationConfiguration.getInstance().getMemberIDStrategy().retrieveMemberID(registration.getPerson());
        student.setStudentNumber(retrieveMemberID);
        student.setAcademicYear(registration.getCurricularYear());
        student.setAcademicDegreeCode(objectFactory.createStudentAcademicDegreeCode(getCodeForDegreeType(
                registration.getDegree().getDegreeType()).toString()));

        return student;
    }

    private static String getInstitutionCode(final Registration registration) {
        final Space campus = registration.getCampus();
        return campus.getName().equals("Taguspark") ? "808" : "807";
    }

    private static Integer getCodeForDegreeType(DegreeType degreeType) {

        Integer BASIC_STUDIES = 1;
        Integer SECUNDARY = 2;
        Integer BACHELHOR = 3;
        Integer DEGREE = 4;
        Integer MASTERS = 5;
        Integer PHD = 6;
        Integer NO_STUDIES = 7;
        Integer OTHER = 99;

        if (degreeType.isDegree() || degreeType.isBolonhaDegree()) {
            return DEGREE;
        }
        if (degreeType.isMasterDegree() || degreeType.isBolonhaMasterDegree()) {
            return MASTERS;
        }
        if (degreeType.isIntegratedMasterDegree()) {
            return MASTERS;
        }
        if (degreeType.isAdvancedSpecializationDiploma()) {
            return PHD;
        }
        if (degreeType.isAdvancedFormationDiploma()) {
            return OTHER;
        }
        if (degreeType.isSpecializationDegree()) {
            return OTHER;
        }
        if (degreeType.isEmpty()) {
            return NO_STUDIES;
        }
        throw new Error("Unknown degree type: " + degreeType);
        //return DEGREE;
    }

    private static String findIES(String ministryCode, IIESService service) {
        List<School> schools = service.getSchools().getSchool();
        for (School school : schools) {
            if (ministryCode.equals(school.getCode().getValue())) {
                return school.getPartnerCode().getValue();
            }
        }
        return null;
    }

    private static services.caixaiu.cgd.wingman.iesservice.Person createPerson(org.fenixedu.academic.domain.Person person) {
        services.caixaiu.cgd.wingman.iesservice.Person personData = new services.caixaiu.cgd.wingman.iesservice.Person();
        personData.setName(person.getName());
        personData.setEmail(objectFactory.createPersonEmail(person.getInstitutionalEmailAddressValue()));
        personData.setGenderCode(objectFactory.createPersonGenderCode(getCodeForGender(person.getGender())));
        try {
            YearMonthDay dateOfBirthYearMonthDay = person.getDateOfBirthYearMonthDay();
            if (dateOfBirthYearMonthDay != null) {
                personData.setBirthDate(DatatypeFactory.newInstance().newXMLGregorianCalendar(
                        dateOfBirthYearMonthDay.toDateTimeAtMidnight().toGregorianCalendar()));
            }
        } catch (DatatypeConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        personData.setFiscalNumber(objectFactory.createPersonFiscalNumber(person.getSocialSecurityNumber()));
        PersonalIngressionData personalIngressionDataByExecutionYear =
                person.getStudent().getPersonalIngressionDataByExecutionYear(ExecutionYear.readCurrentExecutionYear());
        if (personalIngressionDataByExecutionYear != null) {
            org.fenixedu.academic.domain.person.MaritalStatus maritalStatus =
                    personalIngressionDataByExecutionYear.getMaritalStatus();
            personData.setMaritalStatusCode(objectFactory.createPersonMaritalStatusCode(getCodeForMaritalStatus(maritalStatus)));
        }
        personData.setFather(objectFactory.createPersonFather(getShortNameFor(person.getNameOfFather())));
        personData.setMother(objectFactory.createPersonMother(getShortNameFor(person.getNameOfMother())));

        if (person.getCountryOfBirth() != null) {
            personData.setPlaceOfBirthCountryCode(objectFactory.createPersonPlaceOfBirthCountryCode(person.getCountryOfBirth()
                    .getCode()));
        }

        personData.setPlaceOfBirthDistrict(objectFactory.createPersonPlaceOfBirthDistrict(person.getDistrictOfBirth()));
        personData.setPlaceOfBirthCounty(objectFactory.createPersonPlaceOfBirthCounty(person.getDistrictSubdivisionOfBirth()));
        personData.setPlaceOfBirthParish(objectFactory.createPersonPlaceOfBirthParish(person.getParishOfBirth()));

        ArrayOfstring nationality = new ArrayOfstring();
        nationality.getString().add(person.getCountry().getCode());
        personData.setNationalities(nationality);

        PhysicalAddress defaultPhysicalAddress = person.getDefaultPhysicalAddress();
        if (defaultPhysicalAddress != null) {
            personData.setAddress(objectFactory.createPersonAddress(defaultPhysicalAddress.getAddress()));
            personData.setPlace(objectFactory.createPersonPlace(defaultPhysicalAddress.getArea().substring(0,Math.min
                    (defaultPhysicalAddress.getArea().length(),30))));
            personData.setPostalCode(objectFactory.createPersonPostalCode(defaultPhysicalAddress.getAreaCode()));
            personData.setDistrict(objectFactory.createPersonDistrict(defaultPhysicalAddress.getDistrictOfResidence()));
            personData.setCounty(objectFactory.createPersonCounty(defaultPhysicalAddress.getDistrictSubdivisionOfResidence()));
            personData.setParish(objectFactory.createPersonParish(defaultPhysicalAddress.getParishOfResidence()));
        }

        Country countryOfResidence = person.getCountryOfResidence();
        if (countryOfResidence != null) {
            personData.setCountryOfResidenceCode(objectFactory.createPersonCountryOfResidenceCode(countryOfResidence.getCode()));
        } else {
            personData.setCountryOfResidenceCode(objectFactory.createPersonCountryOfResidenceCode("PT"));
        }

        personData.setPhone(objectFactory.createPersonPhone(person.getDefaultPhoneNumber()));
        personData.setMobilePhone(objectFactory.createPersonMobilePhone(person.getDefaultMobilePhoneNumber()));

        IdentificationCard card = new IdentificationCard();

        // skipping issuer
        String codeForDocumentType = getCodeForDocumentType(person.getIdDocumentType());
        if (codeForDocumentType != null) {
            card.setTypeCode(objectFactory.createIdentificationCardTypeCode(codeForDocumentType));
        }
        card.setNumber(person.getDocumentIdNumber());
        // skipping issuer country code
        try {
            YearMonthDay expirationDateOfDocumentIdYearMonthDay = person.getExpirationDateOfDocumentIdYearMonthDay();
            if (expirationDateOfDocumentIdYearMonthDay != null) {
                card.setExpirationDate(objectFactory.createIdentificationCardExpirationDate(DatatypeFactory.newInstance()
                        .newXMLGregorianCalendar(
                                expirationDateOfDocumentIdYearMonthDay.toDateTimeAtMidnight().toGregorianCalendar())));
            }
        } catch (DatatypeConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        personData.setIdentificationCard(objectFactory.createIdentificationCard(card));

        return personData;
    }

    private static String getCodeForDocumentType(IDDocumentType idDocumentType) {
        if (idDocumentType != null) {
            switch (idDocumentType) {
                case IDENTITY_CARD:
                    return "101";
                case PASSPORT:
                    return "302";
                case FOREIGNER_IDENTITY_CARD:
                    return "301";
                case NATIVE_COUNTRY_IDENTITY_CARD:
                    return "301";
                case NAVY_IDENTITY_CARD:
                    return "203";
                case AIR_FORCE_IDENTITY_CARD:
                    return "202";
                case OTHER:
                    return null;
                case MILITARY_IDENTITY_CARD:
                    return "201";
                case EXTERNAL:
                    return null;
                case CITIZEN_CARD:
                    return "801";
                case RESIDENCE_AUTHORIZATION:
                    return "102";
            }
        }
        return null;
    }

    private static String getCodeForMaritalStatus(org.fenixedu.academic.domain.person.MaritalStatus maritalStatus) {
        if (maritalStatus == null) {
            maritalStatus = org.fenixedu.academic.domain.person.MaritalStatus.UNKNOWN;
        }
        switch (maritalStatus) {
            case SINGLE:
                return "001";
            case MARRIED:
                return "009";
            case DIVORCED:
                return "007";
            case WIDOWER:
                return "008";
            case SEPARATED:
                return "006";
            case CIVIL_UNION:
                return "005";
            case UNKNOWN:
                return "099";
            default:
                return "099";
        }
    }

    private static String getShortNameFor(String name) {
        String result = name;
        if (!StringUtils.isEmpty(name)) {
            String[] split = name.split(" ");
            result = split[0] + " " + split[split.length - 1];
        }
        return result;
    }

    private static String getCodeForGender(Gender gender) {
        if (gender != null) {
            switch (gender) {
                case MALE:
                    return "M";
                case FEMALE:
                    return "F";
                default:
                    return "X";
            }
        } else {
            return "X";
        }
    }

    private static Worker createWorker(org.fenixedu.academic.domain.Person person) {
        Worker worker = new Worker();
        worker.setIsWorker(person.getStudent().isWorkingStudent());
        PersonalIngressionData personalIngressionDataByExecutionYear =
                person.getStudent().getPersonalIngressionDataByExecutionYear(ExecutionYear.readCurrentExecutionYear());

        if (personalIngressionDataByExecutionYear != null) {
            // should we also skip this?
            worker.setSituationCode(objectFactory
                    .createWorkerSituationCode(getCodeForProfessionalCondition(personalIngressionDataByExecutionYear
                            .getProfessionalCondition())));
            // Skipping employeer
            // Skpping situationCode
            // Skipping fiscal country code
        }
        return worker;
    }

    private static String getCodeForProfessionalCondition(ProfessionalSituationConditionType professionalCondition) {
        if (professionalCondition == null) {
            professionalCondition = ProfessionalSituationConditionType.UNKNOWN;
        }
        switch (professionalCondition) {
            case WORKS_FOR_OTHERS:
                return "1";
            case EMPLOYEER:
            case INDEPENDENT_WORKER:
                return "2";
            case WORKS_FOR_FAMILY_WITHOUT_PAYMENT:
            case RETIRED:
            case UNEMPLOYED:
            case HOUSEWIFE:
            case STUDENT:
            case MILITARY_SERVICE:
            case OTHER:
            case UNKNOWN:
            default:
                return "3";
        }

    }

}