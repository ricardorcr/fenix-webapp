package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.accounting.AccountingTransactionDetail;
import org.fenixedu.academic.domain.accounting.DueDateAmountMap;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.EventType;
import org.fenixedu.academic.domain.accounting.events.AdministrativeOfficeFeeAndInsuranceEvent;
import org.fenixedu.academic.domain.accounting.events.AdministrativeOfficeFeeEvent;
import org.fenixedu.academic.domain.accounting.events.EnrolmentEvaluationEvent;
import org.fenixedu.academic.domain.accounting.events.ImprovementOfApprovedEnrolmentEvent;
import org.fenixedu.academic.domain.accounting.events.SpecialSeasonEnrolmentEvent;
import org.fenixedu.academic.domain.accounting.events.candidacy.SecondCycleIndividualCandidacyEvent;
import org.fenixedu.academic.domain.accounting.events.dfa.DFACandidacyEvent;
import org.fenixedu.academic.domain.accounting.events.gratuity.GratuityEvent;
import org.fenixedu.academic.domain.accounting.events.insurance.InsuranceEvent;
import org.fenixedu.academic.domain.candidacy.Candidacy;
import org.fenixedu.academic.domain.candidacyProcess.secondCycle.SecondCycleIndividualCandidacy;
import org.fenixedu.academic.domain.organizationalStructure.Party;
import org.fenixedu.academic.domain.phd.debts.ExternalScholarshipPhdGratuityContribuitionEvent;
import org.fenixedu.academic.domain.phd.debts.PhdGratuityEvent;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.GiafInvoiceConfiguration;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.generated.sources.saft.sap.SAFTPTPaymentType;
import org.fenixedu.generated.sources.saft.sap.SAFTPTSettlementType;
import org.fenixedu.generated.sources.saft.sap.SAFTPTSourcePayment;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import com.google.gson.JsonObject;

import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.domain.SapRoot;
import pt.ist.fenixedu.giaf.invoices.SapEvent;
import pt.ist.fenixframework.DomainObject;
import pt.ist.fenixframework.FenixFramework;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.stream.Stream;

public class CorrectCandidacyDebtValue extends CustomTask {

    public static final String PROCESS_ID = "006";
    public static final String IST_VAT_NUMBER = "501507930";

    @Override
    public void runTask() throws Exception {

//        Map<String, String> eventsMap = new HashMap<>();
//        eventsMap.put("1411184519548955","1411197404447514");
//        eventsMap.put("1129709542837133","1129722427736642");
//        eventsMap.put("1411184519546165","1411197404447124");
//        eventsMap.put("1411184519545646","1411197404447050");
//        eventsMap.put("1129709542835390","1129722427736430");
//        eventsMap.put("1129709542836799","1129722427736603");
//        eventsMap.put("1411184519548015","1411197404447381");
//        eventsMap.put("1411184519545692","1411197404447057");
//        eventsMap.put("1129709542837617","1129722427736698");
//        eventsMap.put("1129709542836647","1129722427736578");
//        eventsMap.put("1411184519548933","1411197404447507");
//        eventsMap.put("1411184519548072","1411197404447393");
//        eventsMap.put("1129709542834931","1129722427736370");

//        eventsMap.forEach((c, e) -> correct(c, e));

        correct(FenixFramework.getDomainObject("1411197404447514"));
    }

    private void correct(final String candidacyId, final String eventId) {
        SecondCycleIndividualCandidacy candidacy = FenixFramework.getDomainObject(candidacyId);
        Event event = FenixFramework.getDomainObject(eventId);
        Money whatShouldBe = new Money(candidacy.getSelectedDegreesSet().size() * 100.00);

        taskLog("Going to change due map for event %s %s%n", event.getExternalId(),
                event.getDueDateAmountMap().toJson().toString());
        // martelar dueDateValueMap
        Method[] methodsDue = getAllDueMethodsInHierarchy(SecondCycleIndividualCandidacyEvent.class);
        Map<LocalDate, Money> dueMap = new HashMap<>();
        LocalDate dueDate = event.getDueDateByPaymentCodes().toLocalDate();
        dueMap.put(dueDate, whatShouldBe);
        DueDateAmountMap dueDateAmountMap = new DueDateAmountMap(dueMap);
        try {
            for (Method method : methodsDue) {
                if (method.getName().contains("Map")) {
                    method.setAccessible(true);
                    method.invoke(event, dueDateAmountMap);
                } else {
                    method.setAccessible(true);
                    method.invoke(event, dueDate);
                }
            }
        } catch (IllegalAccessException e) {
            taskLog("Wrong 1");
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            taskLog("Wrong 2");
            e.printStackTrace();
        }
        taskLog("Evento %s corrigido para aluno %s %s - dueMap: %s%n", event.getExternalId(),
                event.getPerson().getName(), event.getPerson().getUsername(), event.getDueDateAmountMap().toJson());
    }

    private void correct(final Event event) {
        taskLog("Going to change due map for event %s %s%n", event.getExternalId(),
                event.getDueDateAmountMap().toJson().toString());
        // martelar dueDateValueMap
//        Method[] methodsDue = getAllDueMethodsInHierarchy(SecondCycleIndividualCandidacyEvent.class);
//        Map<LocalDate, Money> dueMap = new HashMap<>();
//        LocalDate dueDate = new LocalDate(2020, 1, 7); // data de fim igual ao que era
//        dueMap.put(dueDate, new Money(200));
//        DueDateAmountMap dueDateAmountMap = new DueDateAmountMap(dueMap);
//        try {
//            for (Method method : methodsDue) {
//                if (method.getName().contains("Map")) {
//                    method.setAccessible(true);
//                    method.invoke(event, dueDateAmountMap);
//                } else {
//                    method.setAccessible(true);
//                    method.invoke(event, dueDate);
//                }
//            }
//        } catch (IllegalAccessException e) {
//            taskLog("Wrong 1");
//            e.printStackTrace();
//        } catch (InvocationTargetException e) {
//            taskLog("Wrong 2");
//            e.printStackTrace();
//        }

        SapEvent sapEvent = new SapEvent(event);
        SapRequest invoice = sapEvent.registerInvoice(new Money(100), event, false, true);
        registerAdvancementInPayment(event, FenixFramework.getDomainObject("289708429178690"), invoice.getDocumentNumber());

        taskLog("Evento %s corrigido para aluno %s%n", event.getExternalId(), event.getPerson().getUsername());
    }

    private void registerAdvancementInPayment(Event event, SapRequest advancementRequest, String invoiceNumber) {
        AccountingTransactionDetail payment = FenixFramework.getDomainObject("1407490847710708");
        SapRequest originalPayment = advancementRequest;
        Money amountUsed = new Money(100);
        JsonObject advInPayment = toJsonUseAdvancementInPayment(payment, originalPayment, amountUsed, invoiceNumber,
                true, event);
        final SapRequest sapRequest = new SapRequest(event, originalPayment.getClientId(), amountUsed,
                getDocumentNumber(advInPayment, true), SapRequestType.PAYMENT, Money.ZERO, advInPayment);
        sapRequest.setPayment(payment.getTransaction());
        sapRequest.setAdvancementRequest(originalPayment);

    }

    private String getDocumentNumber(JsonObject data, boolean paymentDocument) {
        if (paymentDocument) {
            return data.get("paymentDocument").getAsJsonObject().get("paymentDocumentNumber").getAsString();
        } else {
            return data.get("workingDocument").getAsJsonObject().get("workingDocumentNumber").getAsString();
        }
    }

    private JsonObject toJsonUseAdvancementInPayment(final AccountingTransactionDetail payment,
                                                     final SapRequest originalPayment, final Money amountToUse, final String invoiceNumber,
                                                     final boolean isNewDate, final Event event) {
        DateTime documentDate = payment.getWhenRegistered();
        if (isNewDate) {
            documentDate = new DateTime();
        }
        JsonObject data = toJson(event, originalPayment.getClientJson(), documentDate, false, false, false, false,
                false);

        JsonObject paymentDocument = toJsonPaymentDocument(amountToUse, "NP", invoiceNumber, documentDate, "OU",
                getPaymentMethodReference(payment), SAFTPTSettlementType.NN.toString(), true);
        paymentDocument.addProperty("excessPayment", amountToUse.negate().toPlainString());// the payment amount must be
        // zero
        paymentDocument.addProperty("isToCreditTotal", true);
        paymentDocument.addProperty("isAdvancedPayment", true);
        paymentDocument.addProperty("originatingOnDocumentNumber", originalPayment.getDocumentNumberForType("NA"));

        data.add("paymentDocument", paymentDocument);

        return data;
    }

    private JsonObject toJsonPaymentDocument(Money amount, String documentType, String workingDocumentNumber,
                                             DateTime paymentDate, String paymentMechanism, String paymentMethodReference, String settlementType,
                                             boolean isDebit) {
        JsonObject paymentDocument = new JsonObject();
        paymentDocument.addProperty("paymentDocumentNumber", documentType + getDocumentNumber());
        paymentDocument.addProperty("paymentDate", paymentDate.toString(GiafInvoiceConfiguration.DT_FORMAT));
        paymentDocument.addProperty("paymentType", SAFTPTPaymentType.RG.toString());
        paymentDocument.addProperty("paymentStatus", "N");
        paymentDocument.addProperty("sourcePayment", SAFTPTSourcePayment.P.toString());
        paymentDocument.addProperty("paymentAmount", amount.getAmountAsString());
        paymentDocument.addProperty("paymentMechanism", paymentMechanism);
        paymentDocument.addProperty("paymentMethodReference", paymentMethodReference);
        paymentDocument.addProperty("settlementType", settlementType);

        paymentDocument.addProperty("isToDebit", isDebit);
        paymentDocument.addProperty("workingDocumentNumber", workingDocumentNumber);

        paymentDocument.addProperty("paymentGrossTotal", BigDecimal.ZERO);
        paymentDocument.addProperty("paymentNetTotal", BigDecimal.ZERO);
        paymentDocument.addProperty("paymentTaxPayable", BigDecimal.ZERO);
        return paymentDocument;
    }

    private Long getDocumentNumber() {
        return SapRoot.getInstance().getAndSetNextDocumentNumber();
    }

    private String getPaymentMethodReference(AccountingTransactionDetail transactionDetail) {
        return transactionDetail.getPaymentReference();
    }

    public JsonObject toJson(final Event event, final JsonObject clientData, DateTime documentDate,
                             boolean isDebtRegistration, boolean isNewDate, boolean isInterest, boolean isAdvancement,
                             boolean isPastPayment) {
        final JsonObject json = toJsonCommon(documentDate, isNewDate);

        final String description = event.getDescription().toString();
        final SimpleImmutableEntry<String, String> product = mapToProduct(event, description, isDebtRegistration,
                isInterest, isAdvancement, isPastPayment);
        json.addProperty("productCode", product.getKey());
        json.addProperty("productDescription", detailedDescription(product.getValue(), event));

        json.add("clientData", clientData);

        return json;
    }

    private String detailedDescription(final String description, final Event event) {
        final Party party = event.getParty();
        return party == null ? description : description + " : " + party.getName();
    }

    private JsonObject toJsonCommon(DateTime documentDate, boolean isNewDate) {
        final JsonObject json = new JsonObject();
        json.addProperty("finantialInstitution", "IST");
        json.addProperty("taxType", "IVA");
        json.addProperty("taxCode", "ISE");
        json.addProperty("taxCountry", "PT");
        json.addProperty("taxPercentage", "0");
        json.addProperty("auditFileVersion", "1.0.3");
        json.addProperty("processId", PROCESS_ID);
        json.addProperty("businessName", "Técnico Lisboa");
        json.addProperty("companyName", "Instituto Superior Técnico");
        json.addProperty("companyId", "256241256");
        json.addProperty("currencyCode", "EUR");
        json.addProperty("country", "PT");
        json.addProperty("addressDetail", "Avenida Rovisco Pais, 1");
        json.addProperty("city", "Lisboa");
        json.addProperty("postalCode", "1049-001");
        json.addProperty("region", "Lisboa");
        json.addProperty("street", "Avenida Rovisco Pais, 1");
        json.addProperty("fromDate", isNewDate ? new DateTime().toString(GiafInvoiceConfiguration.DT_FORMAT)
                : documentDate.toString(GiafInvoiceConfiguration.DT_FORMAT));
        json.addProperty("toDate", new DateTime().toString(GiafInvoiceConfiguration.DT_FORMAT)); // tem impacto no ano
        // fiscal!!!
        json.addProperty("productCompanyTaxId", "999999999");
        json.addProperty("productId", "FenixEdu/FenixEdu");
        json.addProperty("productVersion", "5.0.0.0");
        json.addProperty("softwareCertificateNumber", 0);
        json.addProperty("taxAccountingBasis", "P");
        json.addProperty("taxEntity", "Global");
        json.addProperty("taxRegistrationNumber", IST_VAT_NUMBER);
        return json;
    }

    public static SimpleImmutableEntry<String, String> mapToProduct(Event event, String eventDescription,
                                                                    boolean isDebtRegistration, boolean isInterest, boolean isAdvancement, boolean isPastEvent) {
        if (isInterest) {
            return new SimpleImmutableEntry<String, String>("0036", "MULTAS");
        }
        if (isAdvancement) {
            return new SimpleImmutableEntry<String, String>("0056", "ADIANTAMENTO");
        }
        if (isPastEvent) {
            return new SimpleImmutableEntry<String, String>("0063", "REGULARIZAÇAO ANOS ANTERIORES");
        }
        if (event.isGratuity() && !(event instanceof PhdGratuityEvent)) {
            final GratuityEvent gratuityEvent = (GratuityEvent) event;
            final StudentCurricularPlan scp = gratuityEvent.getStudentCurricularPlan();
            final Degree degree = scp.getDegree();
            if (scp.getRegistration().getRegistrationProtocol().isAlien()) {
                if (isDebtRegistration) {
                    return new SimpleImmutableEntry<String, String>("E0075", "ESP PROPINAS INTERNACIONAL");
                } else {
                    return new SimpleImmutableEntry<String, String>("0075", "PROPINAS INTERNACIONAL");
                }
            }
            if (degree.isFirstCycle() && degree.isSecondCycle()) {
                if (isDebtRegistration) {
                    return new SimpleImmutableEntry<String, String>("E0030", "ESP PROPINAS MESTRADO INTEGRADO");
                } else {
                    return new SimpleImmutableEntry<String, String>("0030", "PROPINAS MESTRADO INTEGRADO");
                }
            }
            if (degree.isFirstCycle()) {
                if (isDebtRegistration) {
                    return new SimpleImmutableEntry<String, String>("E0027", "ESP PROPINAS 1 CICLO");
                } else {
                    return new SimpleImmutableEntry<String, String>("0027", "PROPINAS 1 CICLO");
                }
            }
            if (degree.isSecondCycle()) {
                if (isDebtRegistration) {
                    return new SimpleImmutableEntry<String, String>("E0028", "ESP PROPINAS 2 CICLO");
                } else {
                    return new SimpleImmutableEntry<String, String>("0028", "PROPINAS 2 CICLO");
                }
            }
            if (degree.isThirdCycle()) {
                if (isDebtRegistration) {
                    return new SimpleImmutableEntry<String, String>("E0029", "ESP PROPINAS 3 CICLO");
                } else {
                    return new SimpleImmutableEntry<String, String>("0029", "PROPINAS 3 CICLO");
                }
            }
            if (isDebtRegistration) {
                return new SimpleImmutableEntry<String, String>("E0076", "ESP PROPINAS OUTROS");
            } else {
                return new SimpleImmutableEntry<String, String>("0076", "PROPINAS OUTROS");
            }
        }
        if (event instanceof PhdGratuityEvent) {
            if (isDebtRegistration) {
                return new SimpleImmutableEntry<String, String>("E0029", "ESP PROPINAS 3 CICLO");
            } else {
                return new SimpleImmutableEntry<String, String>("0029", "PROPINAS 3 CICLO");
            }
        }
        if (event instanceof ExternalScholarshipPhdGratuityContribuitionEvent) {
            return new SimpleImmutableEntry<String, String>("0029", "PROPINAS 3 CICLO");
        }
        if (event.isResidenceEvent()) {
            return null;
        }
        if (event.isFctScholarshipPhdGratuityContribuitionEvent()) {
            return null;
        }
        if (event.isAcademicServiceRequestEvent()) {
            if (eventDescription.indexOf(" Reingresso") >= 0) {
                return new SimpleImmutableEntry<String, String>("0035", "OUTRAS TAXAS");
            }
            return new SimpleImmutableEntry<String, String>("0037", "EMOLUMENTOS");
        }
        if (event.isDfaRegistrationEvent()) {
            return new SimpleImmutableEntry<String, String>("0031", "TAXAS DE MATRICULA");
        }
        if (event.isIndividualCandidacyEvent()) {
            return new SimpleImmutableEntry<String, String>("0031", "TAXAS DE MATRICULA");
        }
        if (event.isEnrolmentOutOfPeriod()) {
            return new SimpleImmutableEntry<String, String>("0035", "OUTRAS TAXAS");
        }
        if (event instanceof AdministrativeOfficeFeeAndInsuranceEvent) {
            return new SimpleImmutableEntry<String, String>("0031", "TAXAS DE MATRICULA");
        }
        if (event instanceof AdministrativeOfficeFeeEvent) {
            return new SimpleImmutableEntry<String, String>("0031", "TAXAS DE MATRICULA");
        }
        if (event instanceof InsuranceEvent) {
            return new SimpleImmutableEntry<String, String>("0034", "SEGURO ESCOLAR");
        }
        if (event.isSpecializationDegreeRegistrationEvent()) {
            return new SimpleImmutableEntry<String, String>("0031", "TAXAS DE MATRICULA");
        }
        if (event instanceof ImprovementOfApprovedEnrolmentEvent || (event instanceof EnrolmentEvaluationEvent
                && event.getEventType() == EventType.IMPROVEMENT_OF_APPROVED_ENROLMENT)) {
            return new SimpleImmutableEntry<String, String>("0033", "TAXAS DE MELHORIAS DE NOTAS");
        }
        if (event instanceof DFACandidacyEvent) {
            return new SimpleImmutableEntry<String, String>("0031", "TAXAS DE MATRICULA");
        }
        if (event instanceof SpecialSeasonEnrolmentEvent || (event instanceof EnrolmentEvaluationEvent
                && event.getEventType() == EventType.SPECIAL_SEASON_ENROLMENT)) {
            return new SimpleImmutableEntry<String, String>("0032", "TAXAS  DE EXAMES");
        }
        if (event.isPhdEvent()) {
            if (eventDescription.indexOf("Taxa de Inscri") >= 0) {
                return new SimpleImmutableEntry<String, String>("0031", "TAXAS DE MATRICULA");
            }
            if (eventDescription.indexOf("Requerimento de provas") >= 0) {
                return new SimpleImmutableEntry<String, String>("0032", "TAXAS  DE EXAMES");
            }
            return new SimpleImmutableEntry<String, String>("0031", "TAXAS DE MATRICULA");
        }
        throw new Error("not.supported: " + event.getExternalId());
    }

    private Method[] getAllDueMethodsInHierarchy(Class<?> objectClass) {
        Set<Method> allMethods = new HashSet<Method>();
        Method[] declaredMethods = objectClass.getDeclaredMethods();
        Method[] methods = objectClass.getMethods();
        if (objectClass.getSuperclass() != null) {
            Class<?> superClass = objectClass.getSuperclass();
            Method[] superClassMethods = getAllDueMethodsInHierarchy(superClass);
            allMethods.addAll(Arrays.asList(superClassMethods));
        }
        for (Method method : declaredMethods) {
            if (method.getName().contains("setDue")) {
                allMethods.add(method);
            }
        }
        for (Method method : methods) {
            if (method.getName().contains("setDue")) {
                allMethods.add(method);
            }
        }
        return allMethods.toArray(new Method[allMethods.size()]);
    }

}