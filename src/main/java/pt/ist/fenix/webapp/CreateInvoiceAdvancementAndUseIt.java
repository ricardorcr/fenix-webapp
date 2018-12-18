package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.accounting.AccountingTransactionDetail;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.EventType;
import org.fenixedu.academic.domain.accounting.PaymentMethod;
import org.fenixedu.academic.domain.accounting.accountingTransactions.detail.SibsTransactionDetail;
import org.fenixedu.academic.domain.accounting.events.AdministrativeOfficeFeeAndInsuranceEvent;
import org.fenixedu.academic.domain.accounting.events.AdministrativeOfficeFeeEvent;
import org.fenixedu.academic.domain.accounting.events.EnrolmentEvaluationEvent;
import org.fenixedu.academic.domain.accounting.events.ImprovementOfApprovedEnrolmentEvent;
import org.fenixedu.academic.domain.accounting.events.SpecialSeasonEnrolmentEvent;
import org.fenixedu.academic.domain.accounting.events.dfa.DFACandidacyEvent;
import org.fenixedu.academic.domain.accounting.events.gratuity.GratuityEvent;
import org.fenixedu.academic.domain.accounting.events.insurance.InsuranceEvent;
import org.fenixedu.academic.domain.organizationalStructure.Party;
import org.fenixedu.academic.domain.phd.debts.ExternalScholarshipPhdGratuityContribuitionEvent;
import org.fenixedu.academic.domain.phd.debts.PhdGratuityEvent;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.GiafInvoiceConfiguration;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.generated.sources.saft.sap.SAFTPTPaymentType;
import org.fenixedu.generated.sources.saft.sap.SAFTPTSettlementType;
import org.fenixedu.generated.sources.saft.sap.SAFTPTSourceBilling;
import org.fenixedu.generated.sources.saft.sap.SAFTPTSourcePayment;
import org.joda.time.DateTime;

import com.google.gson.JsonObject;

import org.joda.time.YearMonthDay;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.domain.SapRoot;
import pt.ist.fenixedu.giaf.invoices.SapEvent;
import pt.ist.fenixframework.FenixFramework;

import java.math.BigDecimal;
import java.util.AbstractMap.SimpleImmutableEntry;

public class CreateInvoiceAdvancementAndUseIt extends CustomTask {

    public static final String PROCESS_ID = "006";
    public static final String IST_VAT_NUMBER = "501507930";
    private static final String SIBS_DATE_FORMAT = "yyyy-MM-dd";

    @Override
    public void runTask() throws Exception {

        correct("1975341358777897", "ND378046", "NP383232");
        correct("1975341358777896", "ND378045", "NP383233");
    }

    private void correct(final String eventId, final String invoiceNumber, final String paymentNumber) {

        Event event = FenixFramework.getDomainObject(eventId);
        SapRequest invoice = event.getSapRequestSet().stream().filter(sr -> sr.getDocumentNumber().equals(invoiceNumber)).findAny().get();
        invoice.setIgnore(true);
        SapRequest payment = event.getSapRequestSet().stream().filter(sr -> sr.getDocumentNumber().equals(paymentNumber)).findAny().get();

        SapEvent sapEvent = new SapEvent(event);
        SapRequest advancement = registerAdvancementOnly(invoice, payment);
        SapRequest newInvoice = sapEvent.registerInvoice(invoice.getValue(), event, false, true);
        registerAdvancementInPayment(event, advancement, newInvoice.getDocumentNumber());
        payment.delete();

        taskLog("Evento %s corrigido para aluno %s%n", event.getExternalId(), event.getPerson().getUsername());
    }

    private SapRequest registerAdvancementOnly(final SapRequest invoice, final SapRequest oldPayment) {
        final JsonObject data = toJsonAdvancementOnly(invoice.getClientJson(), oldPayment.getPayment().getTransactionDetail(), oldPayment.getValue());
        String documentNumber = getDocumentNumber(data, true);
        SapRequest sapRequest = new SapRequest(invoice.getEvent(), invoice.getClientId(), Money.ZERO, documentNumber,
                SapRequestType.ADVANCEMENT, oldPayment.getValue(), data);
        sapRequest.setPayment(oldPayment.getPayment());
        return sapRequest;
    }

    private JsonObject toJsonAdvancementOnly(final JsonObject clientData, final AccountingTransactionDetail transactionDetail, final Money amount) {
        JsonObject data = toJson(transactionDetail.getEvent(), clientData, transactionDetail.getWhenRegistered(), false, false, false, true, false);
        JsonObject paymentDocument = toJsonPaymentDocument(Money.ZERO, "NP", "", transactionDetail.getWhenRegistered(),
                getPaymentMechanism(transactionDetail), getPaymentMethodReference(transactionDetail),
                SAFTPTSettlementType.NL.toString(), true);
        paymentDocument.addProperty("isWithoutLines", true);
        paymentDocument.addProperty("noPaymentTotals", true);
        paymentDocument.addProperty("isAdvancedPayment", true);
        paymentDocument.addProperty("excessPayment", amount.getAmountAsString());
        addSibsMetadata(paymentDocument, transactionDetail);

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

    private JsonObject toJsonWorkDocument(DateTime documentDate, DateTime entryDate, Money amount, String documentType,
                                          boolean isToDebit, DateTime dueDate) {
        JsonObject workDocument = new JsonObject();
        workDocument.addProperty("documentDate", documentDate.toString(GiafInvoiceConfiguration.DT_FORMAT));
        workDocument.addProperty("entryDate", entryDate.toString(GiafInvoiceConfiguration.DT_FORMAT));
        workDocument.addProperty("dueDate", dueDate.toString(GiafInvoiceConfiguration.DT_FORMAT));
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

    private void registerAdvancementInPayment(Event event, SapRequest advancementRequest, String invoiceNumber) {
        SapRequest originalPayment = advancementRequest;
        Money amountUsed = advancementRequest.getAdvancement();
        JsonObject advInPayment = toJsonUseAdvancementInPayment(advancementRequest.getPayment().getTransactionDetail(), originalPayment, amountUsed, invoiceNumber,
                true, event);
        final SapRequest sapRequest = new SapRequest(event, originalPayment.getClientId(), amountUsed,
                getDocumentNumber(advInPayment, true), SapRequestType.PAYMENT, Money.ZERO, advInPayment);
        sapRequest.setPayment(advancementRequest.getPayment());
        sapRequest.setAdvancementRequest(originalPayment);

    }

    private void addSibsMetadata(final JsonObject json, final AccountingTransactionDetail transactionDetail) {
        if (PaymentMethod.getSibsPaymentMethod() == transactionDetail.getPaymentMethod()) {
            SibsTransactionDetail sibsTx = (SibsTransactionDetail) transactionDetail;
            YearMonthDay sibsDate = sibsTx.getSibsLine().getHeader().getWhenProcessedBySibs();
            json.addProperty("sibsDate", sibsDate.toString(SIBS_DATE_FORMAT));
        }
    }

    private String getDocumentNumber(JsonObject data, boolean paymentDocument) {
        if (paymentDocument) {
            return data.get("paymentDocument").getAsJsonObject().get("paymentDocumentNumber").getAsString();
        } else {
            return data.get("workingDocument").getAsJsonObject().get("workingDocumentNumber").getAsString();
        }
    }

    private DateTime getDocumentDate(DateTime documentDate, boolean isNewDate) {
        if (isNewDate) {
            return new DateTime();
        }
        return documentDate;
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

    private String getPaymentMechanism(AccountingTransactionDetail transactionDetail) {
        return transactionDetail.getPaymentMethod().getCode();
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
}