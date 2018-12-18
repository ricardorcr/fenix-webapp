package pt.ist.fenix.webapp;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.fenixedu.PostalCodeValidator;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.accounting.AccountingTransaction;
import org.fenixedu.academic.domain.accounting.AccountingTransactionDetail;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.EventType;
import org.fenixedu.academic.domain.accounting.calculator.ExcessRefund;
import org.fenixedu.academic.domain.accounting.calculator.PartialPayment;
import org.fenixedu.academic.domain.accounting.events.AdministrativeOfficeFeeAndInsuranceEvent;
import org.fenixedu.academic.domain.accounting.events.AdministrativeOfficeFeeEvent;
import org.fenixedu.academic.domain.accounting.events.EnrolmentEvaluationEvent;
import org.fenixedu.academic.domain.accounting.events.ImprovementOfApprovedEnrolmentEvent;
import org.fenixedu.academic.domain.accounting.events.SpecialSeasonEnrolmentEvent;
import org.fenixedu.academic.domain.accounting.events.dfa.DFACandidacyEvent;
import org.fenixedu.academic.domain.accounting.events.gratuity.GratuityEvent;
import org.fenixedu.academic.domain.accounting.events.insurance.InsuranceEvent;
import org.fenixedu.academic.domain.contacts.PhysicalAddress;
import org.fenixedu.academic.domain.organizationalStructure.Party;
import org.fenixedu.academic.domain.phd.debts.ExternalScholarshipPhdGratuityContribuitionEvent;
import org.fenixedu.academic.domain.phd.debts.PhdGratuityEvent;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.generated.sources.saft.sap.SAFTPTPaymentType;
import org.fenixedu.generated.sources.saft.sap.SAFTPTSettlementType;
import org.fenixedu.generated.sources.saft.sap.SAFTPTSourceBilling;
import org.fenixedu.generated.sources.saft.sap.SAFTPTSourcePayment;
import org.joda.time.DateTime;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.domain.SapRoot;
import pt.ist.fenixedu.giaf.invoices.SapEvent;
import pt.ist.fenixedu.giaf.invoices.Utils;
import pt.ist.fenixframework.FenixFramework;
import java.util.AbstractMap.SimpleImmutableEntry;

import java.math.BigDecimal;
import java.util.Date;

public class FixInterestPayments2018 extends CustomTask {

    private static final String DT_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String MORADA_DESCONHECIDO = "Desconhecido";
    private static final int MAX_SIZE_ADDRESS = 100;
    private static final int MAX_SIZE_CITY = 50;
    private static final int MAX_SIZE_REGION = 50;
    private static final int MAX_SIZE_POSTAL_CODE = 20;
    private static final int MAX_SIZE_VAT_NUMBER = 20;
    public static final String PROCESS_ID = "006";
    public static final String IST_VAT_NUMBER = "501507930";

    @Override
    public void runTask() throws Exception {
//        SapRequest adv = createAdvancement("852675562307768", "564470371890754", new Money(10));
//        useAdvancement(adv, "852675562307768", "564470371890754", "1134133359944051", new Money(10),SapRequestType.PAYMENT_INTEREST, false);
//
//        SapRequest adv1 = createAdvancement("844450699938085", "1408895302005899", new Money(1.29));
//        useAdvancement(adv1, "844450699938085", "1408895302005899", "1134133359953314", new Money(1.29), SapRequestType.PAYMENT_INTEREST, false);
//
//        SapRequest adv2 = createAdvancement("1407400653360416", "1127420325288301", new Money(163.47));
//        useAdvancement(adv2, "1407400653360416", "1127420325288301", "852658382442285", new Money(163.47), SapRequestType.PAYMENT, true);
////        hackInvoiceClosure(adv2.getReimbursementRequest(), "NP422844");
        SapRequest closeRequest = FenixFramework.getDomainObject("852658382535551");
        closeRequest.setRequest(closeRequest.getRequest().replace("NP422844", "NP422878"));
    }

    private void hackInvoiceClosure(final SapRequest reimbursementRequest, final String oldNumber) {
        SapRequest closeRequest = reimbursementRequest.getEvent().getSapRequestSet().stream().filter(sr -> sr.getRequestType() == SapRequestType.CLOSE_INVOICE).findAny().get();
        closeRequest.setRequest(closeRequest.getRequest().replace(oldNumber, reimbursementRequest.getDocumentNumber()));
    }

    private void useAdvancement(final SapRequest adv, final String eventID, final String txID, final String invoiceID, final Money amount, final SapRequestType requestType, final boolean isNewDate) {
        Event event = FenixFramework.getDomainObject(eventID);
        AccountingTransaction tx = FenixFramework.getDomainObject(txID);
        SapRequest invoice = FenixFramework.getDomainObject(invoiceID);
        SapRequest sapRequest = registerAdvancementInPayment(tx.getTransactionDetail(), adv, event, amount, invoice, requestType);
        JsonObject requestAsJson = sapRequest.getRequestAsJson();
        DateTime dateToRegister = invoice.getWhenCreated();
        if (isNewDate) {
            dateToRegister = new DateTime();
        }
        requestAsJson.get("paymentDocument").getAsJsonObject()
                .addProperty("paymentDate", dateToRegister.toString("yyyy-MM-dd HH:mm:ss"));
        sapRequest.setRequest(requestAsJson.toString());
    }

    private SapRequest registerAdvancementInPayment(final AccountingTransactionDetail payment, final SapRequest originalPayment,
                                              final Event event, final Money amountUsed, final SapRequest openInvoice, final SapRequestType requestType) {
        JsonObject advInPayment = toJsonUseAdvancementInPayment(payment, originalPayment, amountUsed, openInvoice.getDocumentNumber(), event);
        final SapRequest sapRequest = new SapRequest(event, originalPayment.getClientId(), amountUsed, getDocumentNumber(advInPayment, true),
                requestType, Money.ZERO, advInPayment);
        sapRequest.setPayment(payment.getTransaction());
//        sapRequest.setRefund(FenixFramework.getDomainObject(excessRefund.getId()));
        sapRequest.setAdvancementRequest(originalPayment);
        return sapRequest;
    }

    private JsonObject toJsonUseAdvancementInPayment(final AccountingTransactionDetail payment, final SapRequest originalPayment,
                                                     final Money amountToUse, final String invoiceNumber, final Event event) {
        JsonObject data =
                toJson(event, originalPayment.getClientJson(), payment.getWhenRegistered(), false, false, false, false, false);

        JsonObject paymentDocument = toJsonPaymentDocument(amountToUse, "NP", invoiceNumber, payment.getWhenRegistered(),
                "OU", getPaymentMethodReference(payment), SAFTPTSettlementType.NN.toString(), true);
        paymentDocument.addProperty("excessPayment", amountToUse.negate().toPlainString());//the payment amount must be zero
        paymentDocument.addProperty("isToCreditTotal", true);

        final JsonArray documents = new JsonArray();
        JsonObject line = new JsonObject();
        line.addProperty("amount", amountToUse.getAmountAsString());
        line.addProperty("isToDebit", false);
        line.addProperty("originDocNumber", originalPayment.getDocumentNumberForType("NA"));
        documents.add(line);
        paymentDocument.add("documents", documents);

        data.add("paymentDocument", paymentDocument);

        return data;
    }

    private SapRequest createAdvancement(final String eventID, final String txID, final Money money) {
        Event event = FenixFramework.getDomainObject(eventID);
        AccountingTransaction tx = FenixFramework.getDomainObject(txID);
        String clientID = tx.getSapRequestSet().iterator().next().getClientId();
        return registerAdvancementOnly(clientID, tx.getTransactionDetail(), money, event);
    }

    private SapRequest registerAdvancementOnly(final String clientId, final AccountingTransactionDetail transactionDetail,
                                         final Money payedAmount, final Event event) {
        final JsonObject data = toJsonAdvancementOnly(clientId, transactionDetail, payedAmount, event);
        String documentNumber = getDocumentNumber(data, true);
        SapRequest sapRequest = new SapRequest(event, clientId, Money.ZERO, documentNumber,
                SapRequestType.ADVANCEMENT, payedAmount, data);
        sapRequest.setPayment(transactionDetail.getTransaction());
        return sapRequest;
    }

    private JsonObject toJsonAdvancementOnly(final String clientId, final AccountingTransactionDetail transactionDetail, final Money amount, final Event event) {
        final JsonObject clientData = toJsonClient(event.getParty(), clientId);
        JsonObject data = toJson(event, clientData, transactionDetail.getWhenRegistered(), false, false, false, true, false);
        JsonObject paymentDocument = toJsonPaymentDocument(Money.ZERO, "NP", "", transactionDetail.getWhenRegistered(),
                getPaymentMechanism(transactionDetail), getPaymentMethodReference(transactionDetail),
                SAFTPTSettlementType.NL.toString(), true);
        paymentDocument.addProperty("isWithoutLines", true);
        paymentDocument.addProperty("noPaymentTotals", true);
        paymentDocument.addProperty("isAdvancedPayment", true);
        paymentDocument.addProperty("excessPayment", amount.getAmountAsString());

        JsonObject workingDocument = toJsonWorkDocument(getDocumentDate(transactionDetail.getWhenRegistered(), false),
                new DateTime(), amount, "NA", false, transactionDetail.getWhenRegistered());
        workingDocument.addProperty("isAdvancedPayment", true);
        workingDocument.addProperty("paymentDocumentNumber", paymentDocument.get("paymentDocumentNumber").getAsString());
        //the payment document has to refer the working document credit note number
        paymentDocument.addProperty("originatingOnDocumentNumber", workingDocument.get("workingDocumentNumber").getAsString());

        data.add("workingDocument", workingDocument);
        data.add("paymentDocument", paymentDocument);

        return data;
    }

    private DateTime getDocumentDate(DateTime documentDate, boolean isNewDate) {
        if (isNewDate) {
            return new DateTime();
        }
        return documentDate;
    }

    private JsonObject toJsonWorkDocument(DateTime documentDate, DateTime entryDate, Money amount, String documentType,
                                          boolean isToDebit, DateTime dueDate) {
        JsonObject workDocument = new JsonObject();
        workDocument.addProperty("documentDate", documentDate.toString(DT_FORMAT));
        workDocument.addProperty("entryDate", entryDate.toString(DT_FORMAT));
        workDocument.addProperty("dueDate", dueDate.toString(DT_FORMAT));
        workDocument.addProperty("workingDocumentNumber", documentType + getDocumentNumber());
        workDocument.addProperty("sourceBilling", SAFTPTSourceBilling.P.toString());
        workDocument.addProperty("workingAmount", amount.getAmountAsString());
        workDocument.addProperty("taxPayable", BigDecimal.ZERO);
        workDocument.addProperty("workType", "DC");
        workDocument.addProperty("workStatus", "N");

        workDocument.addProperty("isToDebit", isToDebit);
        workDocument.addProperty("isToCredit", !isToDebit);

        workDocument.addProperty("taxExemptionReason", "M99");
        workDocument.addProperty("unitOfMeasure", "UNID");

        return workDocument;
    }

    public JsonObject toJson(final Event event, final JsonObject clientData, DateTime documentDate, boolean isDebtRegistration,
                             boolean isNewDate, boolean isInterest, boolean isAdvancement, boolean isPastPayment) {
        final JsonObject json = toJsonCommon(documentDate, isNewDate);

        final String description = event.getDescription().toString();
        final SimpleImmutableEntry<String, String> product = mapToProduct(event, description, isDebtRegistration, isInterest, isAdvancement, isPastPayment);
        json.addProperty("productCode", product.getKey());
        json.addProperty("productDescription", detailedDescription(product.getValue(), event));

        json.add("clientData", clientData);

        return json;
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
        json.addProperty("fromDate", isNewDate ? new DateTime().toString(DT_FORMAT) : documentDate.toString(DT_FORMAT));
        json.addProperty("toDate", new DateTime().toString(DT_FORMAT)); //tem impacto no ano fiscal!!!
        json.addProperty("productCompanyTaxId", "999999999");
        json.addProperty("productId", "FenixEdu/FenixEdu");
        json.addProperty("productVersion", "5.0.0.0");
        json.addProperty("softwareCertificateNumber", 0);
        json.addProperty("taxAccountingBasis", "P");
        json.addProperty("taxEntity", "Global");
        json.addProperty("taxRegistrationNumber", IST_VAT_NUMBER);
        return json;
    }

    private String detailedDescription(final String description, final Event event) {
        final Party party = event.getParty();
        return party == null ? description : description + " : " + party.getName();
    }

    private String getPaymentMethodReference(AccountingTransactionDetail transactionDetail) {
        return transactionDetail.getPaymentReference();
    }

    private String getPaymentMechanism(AccountingTransactionDetail transactionDetail) {
        return transactionDetail.getPaymentMethod().getCode();
    }


    public static SimpleImmutableEntry<String, String> mapToProduct(Event event, String eventDescription,
                                                                    boolean isDebtRegistration, boolean isInterest,
                                                                    boolean isAdvancement, boolean isPastEvent) {
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
        if (event instanceof SpecialSeasonEnrolmentEvent
                || (event instanceof EnrolmentEvaluationEvent && event.getEventType() == EventType.SPECIAL_SEASON_ENROLMENT)) {
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

    private JsonObject toJsonClient(final Party party, final String clientId) {
        final JsonObject clientData = new JsonObject();
        clientData.addProperty("accountId", "STUDENT");
        clientData.addProperty("companyName", party.getName());
        clientData.addProperty("clientId", clientId);
        //country must be the same as the fiscal country
        final String countryCode = clientId.substring(0, 2);
        clientData.addProperty("country", countryCode);

        PhysicalAddress physicalAddress = Utils.toAddress(party, countryCode);
        clientData.addProperty("street",
                physicalAddress != null && physicalAddress.getAddress() != null && !Strings.isNullOrEmpty(physicalAddress.getAddress().trim()) ?
                        Utils.limitFormat(MAX_SIZE_ADDRESS, physicalAddress.getAddress()) : MORADA_DESCONHECIDO);

        String city = Utils.limitFormat(MAX_SIZE_CITY, party.getDistrictSubdivisionOfResidence()).trim();
        clientData.addProperty("city", !Strings.isNullOrEmpty(city) ? city : MORADA_DESCONHECIDO);

        String region = Utils.limitFormat(MAX_SIZE_REGION, party.getDistrictOfResidence()).trim();
        clientData.addProperty("region", !Strings.isNullOrEmpty(region) ? region : MORADA_DESCONHECIDO);

        String postalCode =
                physicalAddress == null ? null : Utils.limitFormat(MAX_SIZE_POSTAL_CODE, physicalAddress.getAreaCode()).trim();
        //sometimes the address is correct but the vatNumber doesn't exists and a random one was generated from the birth country
        //in that case we must send a valid postal code for that country, even if it is not the address country
        if (physicalAddress.getCountryOfResidence() != null
                && !physicalAddress.getCountryOfResidence().getCode().equals(countryCode)) {
            postalCode = PostalCodeValidator.examplePostCodeFor(countryCode);
        }
        if (!PostalCodeValidator.isValidAreaCode(countryCode, postalCode)) {
            postalCode = PostalCodeValidator.examplePostCodeFor(countryCode);
        }
        clientData.addProperty("postalCode",
                !Strings.isNullOrEmpty(postalCode) ? postalCode : PostalCodeValidator.examplePostCodeFor(countryCode));

        clientData.addProperty("vatNumber", Utils.limitFormat(MAX_SIZE_VAT_NUMBER, clientId));
        clientData.addProperty("fiscalCountry", countryCode);
        clientData.addProperty("nationality", party.getCountry().getCode());
        clientData.addProperty("billingIndicator", 0);

        return clientData;
    }

    private JsonObject toJsonPaymentDocument(Money amount, String documentType, String workingDocumentNumber,
                                             DateTime paymentDate, String paymentMechanism, String paymentMethodReference, String settlementType,
                                             boolean isDebit) {
        JsonObject paymentDocument = new JsonObject();
        paymentDocument.addProperty("paymentDocumentNumber", documentType + getDocumentNumber());
        paymentDocument.addProperty("paymentDate", paymentDate.toString(DT_FORMAT));
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

    private String getDocumentNumber(JsonObject data, boolean paymentDocument) {
        if (paymentDocument) {
            return data.get("paymentDocument").getAsJsonObject().get("paymentDocumentNumber").getAsString();
        } else {
            return data.get("workingDocument").getAsJsonObject().get("workingDocumentNumber").getAsString();
        }
    }
}
