package pt.ist.fenix.webapp;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.fenixedu.PostalCodeValidator;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.accounting.CustomEvent;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.EventType;
import org.fenixedu.academic.domain.accounting.calculator.CreditEntry;
import org.fenixedu.academic.domain.accounting.events.AdministrativeOfficeFeeAndInsuranceEvent;
import org.fenixedu.academic.domain.accounting.events.AdministrativeOfficeFeeEvent;
import org.fenixedu.academic.domain.accounting.events.EnrolmentEvaluationEvent;
import org.fenixedu.academic.domain.accounting.events.EventExemption;
import org.fenixedu.academic.domain.accounting.events.EventExemptionJustification;
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
import org.fenixedu.bennu.GiafInvoiceConfiguration;
import org.fenixedu.generated.sources.saft.sap.SAFTPTSourceBilling;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import pt.ist.fenixedu.domain.ExternalClient;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.domain.SapRoot;
import pt.ist.fenixedu.giaf.invoices.ClientMap;
import pt.ist.fenixedu.giaf.invoices.ErrorLogConsumer;
import pt.ist.fenixedu.giaf.invoices.EventLogger;
import pt.ist.fenixedu.giaf.invoices.SapEvent;
import pt.ist.fenixedu.giaf.invoices.Utils;
import pt.ist.fenixedu.giaf.invoices.task.SapCustomTask;
import pt.ist.fenixframework.FenixFramework;

import java.math.BigDecimal;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CreateMissingNADebtRequest extends SapCustomTask {

    private static final String MORADA_DESCONHECIDO = "Desconhecido";
    private static final int MAX_SIZE_ADDRESS = 100;
    private static final int MAX_SIZE_CITY = 50;
    private static final int MAX_SIZE_REGION = 50;
    private static final int MAX_SIZE_POSTAL_CODE = 20;
    private static final int MAX_SIZE_VAT_NUMBER = 20;
    public static final String PROCESS_ID = "006";
    public static final String IST_VAT_NUMBER = "501507930";
    public LocalDate currentDate = new LocalDate();

    @Override
    protected void runTask(ErrorLogConsumer errorLogConsumer, EventLogger elogger) {

        final Map<Event, Set<SapRequest>> usedDebtCreditMap = new HashMap<>();
        final Money[] values = new Money[] {Money.ZERO};
        final Set<SapRequest> relevantRequests = new HashSet<>();
        final Set<SapRequest> creditDebtRequests = new HashSet<>();
        SapRoot.getInstance().getSapRequestSet().stream()
                .filter(sr -> sr.getIntegrated())
                .filter(sr -> !sr.isInitialization())
                .filter(sr -> sr.getDocumentDate().getYear() == 2021)
                .map(SapRequest::getEvent)
                .collect(Collectors.toSet())
                .forEach(event -> process(event, values, relevantRequests, usedDebtCreditMap));

        taskLog("Valor abater: %s%n", values[0].toString());
        final Money requestsAmount = relevantRequests.stream()
                .map(SapRequest::getValue)
                .reduce(Money.ZERO, Money::add);
        taskLog("Valor requests: %s%n", requestsAmount.toString());

        relevantRequests.stream()
                .forEach(request -> registerCreditDebt(request, creditDebtRequests));

        final Money creditDebtAmount = creditDebtRequests.stream()
                .map(SapRequest::getValue)
                .reduce(Money.ZERO, Money::add);
        taskLog("Valor requests Abate: %s%n", creditDebtAmount.toString());
    }

    private void registerCreditDebt(final SapRequest creditRequest, final Set<SapRequest> creditDebtRequests) {
        final Event event = creditRequest.getEvent();
        final CreditEntry creditEntry = getCreditEntry(creditRequest.getValue(), creditRequest.getCreditId());
        final SapRequest debtCredit = registerDebtCredit(creditEntry, event, true);
        debtCredit.setRequest(debtCredit.getRequest().replace("2022-02-18", "2021-12-31"));

        creditDebtRequests.add(debtCredit);
    }

    private void process(final Event event, final Money[] values, final Set<SapRequest> relevantRequests, final Map<Event, Set<SapRequest>> usedDebtCreditMap) {
        if (event.getSapRequestSet().stream()
                .filter(sr -> !sr.isInitialization())
                .filter(sr -> sr.getIntegrated())
                .anyMatch(sr -> sr.getRequestType() == SapRequestType.DEBT)) {
            Set<SapRequest> creditRequests = new HashSet<>();
            Money outOfDate = event.getSapRequestSet().stream()
                    .filter(sr -> !sr.isInitialization())
                    .filter(sr -> sr.getIntegrated())
                    .filter(sr -> sr.getRequestType() == SapRequestType.CREDIT)
                    .filter(sr -> sr.getDocumentDate().getYear() == 2021)
                    .filter(sr -> !Strings.isNullOrEmpty(sr.getCreditId()) || sr.getRefund() != null)
                    .filter(sr -> !isZaziamento(sr))
                    .filter(sr -> {
                        final Optional<SapRequest> creditDebt = getCreditDebt(sr, usedDebtCreditMap.get(event));
                        if (creditDebt.isPresent()) {
                            usedDebtCreditMap.computeIfAbsent(event, (v) -> new HashSet<SapRequest>()).add(creditDebt.get());
                            return false;
                        } else {
                            if (sr.getDocumentNumber().startsWith("NR") && hasAnotherInvoice(sr)) {
                                return false;
                            } else {
                                creditRequests.add(sr);
                                return true;
                            }
                        }
                    })
                    .map(sr -> sr.getValue())
                    .reduce(Money.ZERO, Money::add);
            if (creditRequests.size() > 1) {
                final Money value = creditRequests.stream()
                        .map(SapRequest::getValue)
                        .reduce(Money.ZERO, Money::add);
                final Optional<SapRequest> creditDebt = getCreditDebt(event, value, usedDebtCreditMap.get(event));
                if (creditDebt.isPresent()) {
                    outOfDate = outOfDate.subtract(value);
                }
            }
            values[0] = values[0].add(outOfDate);
            if (outOfDate.isPositive()) {
//                taskLog("%s\t%s%n", event.getExternalId(), outOfDate);
                relevantRequests.addAll(creditRequests);
            }
        }
    }

    private boolean hasAnotherInvoice(final SapRequest request) {
        final String invoiceNumber = request.getDocumentNumberForType("ND");
        return request.getEvent().getSapRequestSet().stream()
                .filter(sr -> !sr.isInitialization())
                .filter(sr -> sr.getRequestType() == SapRequestType.INVOICE)
                .filter(sr -> !invoiceNumber.equals(sr.getDocumentNumber()))
                .filter(sr -> sr.getDocumentDate().getYear() == 2021)
                .anyMatch(sr -> sr.getValue().compareTo(request.getValue()) == 0);
    }

    private Optional<SapRequest> getCreditDebt(final SapRequest creditRequest, final Set<SapRequest> usedDebtCredits) {
        return creditRequest.getEvent().getSapRequestSet().stream()
                .filter(sr -> sr.getRequestType() == SapRequestType.DEBT_CREDIT)
                .filter(sr -> !sr.isInitialization())
                .filter(sr -> sr.getValue().compareTo(creditRequest.getValue()) == 0)
                .filter(sr -> sr.getDocumentDate().getYear() == 2021)
                .filter(sr -> usedDebtCredits == null || !usedDebtCredits.stream().anyMatch(dc -> dc == sr))
                .findAny();
    }

    private Optional<SapRequest> getCreditDebt(final Event event, final Money creditValue, final Set<SapRequest> usedDebtCredits) {
        return event.getSapRequestSet().stream()
                .filter(sr -> sr.getRequestType() == SapRequestType.DEBT_CREDIT)
                .filter(sr -> !sr.isInitialization())
                .filter(sr -> sr.getValue().compareTo(creditValue) == 0)
                .filter(sr -> sr.getDocumentDate().getYear() == 2021)
                .filter(sr -> usedDebtCredits == null || !usedDebtCredits.stream().anyMatch(dc -> dc == sr))
                .findAny();
    }

    private boolean isZaziamento(SapRequest request) {
        return request.getPayment() != null && request.getPayment().getPaymentMethod().getExternalId().equals("852714217013252");
    }

    private SapRequest registerDebtCredit(CreditEntry creditEntry, Event event, boolean isNewDate) {

        String clientId = ClientMap.uVATNumberFor(event.getParty());
        AbstractMap.SimpleImmutableEntry<List<SapRequest>, Money> openDebtsAndRemainingValue = getOpenDebtsAndRemainingValue(event);
        List<SapRequest> openDebts = openDebtsAndRemainingValue.getKey();
        Money remainingAmount = openDebtsAndRemainingValue.getValue();
        final Money amountToRegister = new Money(creditEntry.getUsedAmountInDebts());
        if (amountToRegister.greaterThan(remainingAmount)) {
            if (openDebts.size() > 1) {
                // dividir o valor da isenção pelas várias dívidas
                registerDebtCreditList(event, openDebts, amountToRegister, creditEntry, remainingAmount,
                        clientId, isNewDate);
            } else {
                throw new Error("There is no open debt to credit exemption: " + creditEntry.getId() + " for event: " + event.getExternalId());
            }
        } else {
            //tudo normal
            return registerDebtCredit(clientId, event, amountToRegister, creditEntry, openDebts.get(0), isNewDate);
        }
        return null;
    }

    private SapRequest registerDebtCredit(String clientId, Event event, Money amountToRegister, CreditEntry creditEntry,
                                          SapRequest debtRequest, boolean isNewDate) {
        checkValidDocumentNumber(debtRequest.getDocumentNumber(), event);

        final EventExemption exemption = FenixFramework.getDomainObject(creditEntry.getId());
        final DateTime documentDate = getDocumentDate(creditEntry.getCreated(), isNewDate);
        LocalDate dispatchDate = documentDate.toLocalDate();
        if (exemption != null) {
            EventExemptionJustification justification = (EventExemptionJustification) exemption.getExemptionJustification();
            if (justification.getDispatchDate() != null) {
                dispatchDate = justification.getDispatchDate();
            }
        }
        JsonObject data = toJsonDebtCredit(event, amountToRegister, clientId,
                documentDate, new DateTime(), dispatchDate, true, "NJ", false, isNewDate, debtRequest);
        String documentNumber = getDocumentNumber(data, false);
        SapRequest sapRequest =
                new SapRequest(event, clientId, amountToRegister, documentNumber, SapRequestType.DEBT_CREDIT, Money.ZERO, data);
        sapRequest.setCreditId(creditEntry.getId());
        return sapRequest;
    }

    private JsonObject toJsonDebtCredit(Event event, Money debtFenix, String clientId, DateTime documentDate, DateTime entryDate,
                                        LocalDate dispatchDate, boolean isDebtRegistration, String docType, boolean isToDebit, boolean isNewDate, SapRequest debtRequest) {
        JsonObject request = new JsonParser().parse(debtRequest.getRequest()).getAsJsonObject();
        String metadata = request.get("workingDocument").getAsJsonObject().get("metadata").getAsString();

        String stripMetadata = metadata.replace("\\", "");
        JsonObject metadataJson = new JsonParser().parse(stripMetadata).getAsJsonObject();
        final LocalDate startDate = LocalDate.parse(metadataJson.get("START_DATE").getAsString());
        if (dispatchDate.isBefore(startDate)) { //some debts are created before the new year starts and exempted before the new year
            dispatchDate = startDate;
        }

        metadata = metadata.replace("}", ", \"Despacho\":\"" + dispatchDate.toString("yyyy-MM-dd") + "\"}");
        JsonObject json = toJsonDebt(event, debtFenix, clientId, documentDate, entryDate, isDebtRegistration, docType, isToDebit,
                isNewDate, metadata);
        JsonObject workingDocument = json.get("workingDocument").getAsJsonObject();
        workingDocument.addProperty("workOriginDocNumber", debtRequest.getDocumentNumber());
        return json;
    }


    private JsonObject toJsonDebt(Event event, Money debtFenix, String clientId, DateTime documentDate, DateTime entryDate,
                                  boolean isDebtRegistration, String docType, boolean isToDebit, boolean isNewDate, String originalMetadata) {
        final JsonObject clientData = toJsonClient(event.getParty(), clientId);
        JsonObject json = toJson(event, clientData, documentDate, isDebtRegistration, isNewDate, false, false, false);
        JsonObject workDocument =
                toJsonWorkDocument(documentDate, entryDate, debtFenix, docType, isToDebit, new DateTime(Utils.getDueDate(event)));

        if (originalMetadata == null) {
            final LocalDate[] debtInterval = getDebtInterval(documentDate, isNewDate, event);
            final ExecutionYear executionYear = Utils.executionYearOf(event);
            String metadata = String.format("{\"ANO_LECTIVO\":\"%s\", \"START_DATE\":\"%s\", \"END_DATE\":\"%s\"}",
                    executionYear.getName(), debtInterval[0].toString("yyyy-MM-dd"), debtInterval[1].toString("yyyy-MM-dd"));
            workDocument.addProperty("metadata", metadata);
        } else {
            workDocument.addProperty("metadata", originalMetadata);
        }

        json.add("workingDocument", workDocument);
        return json;
    }

    private LocalDate[] getDebtInterval(final DateTime documentDate, final boolean isNewDate, final Event event) {
        LocalDate startDate = isNewDate ? currentDate : documentDate.toLocalDate();
        final LocalDate endDate;
        if (event instanceof PhdGratuityEvent) {
            PhdGratuityEvent phdEvent = (PhdGratuityEvent) event;
            final LocalDate localDate = phdEvent.getPhdGratuityDate().getYear() == phdEvent.getYear() ?
                    phdEvent.getPhdGratuityDate().toLocalDate() : phdEvent.getWhenOccured().toLocalDate();
            endDate = localDate.plusYears(1);
        } else {
            final ExecutionYear executionYear = Utils.executionYearOf(event);
            if (startDate.isBefore(executionYear.getBeginLocalDate())) {
                startDate = executionYear.getBeginLocalDate();
            }
            endDate = executionYear.getEndDateYearMonthDay().toLocalDate();
        }
        return startDate.isAfter(endDate) ? null : new LocalDate[]{startDate, endDate};
    }

    private void checkValidDocumentNumber(String documentNumber, Event event) {
        if ('0' == documentNumber.charAt(2)) {
            throw new Error("Houve uma tentativa de efectuar uma operação sobre o documento: " + documentNumber
                    + " - evento: " + event.getExternalId());
        }
    }

    private void registerDebtCreditList(Event event, List<SapRequest> openDebts, Money amountToRegister, CreditEntry creditEntry,
                                        Money remainingAmount, String clientId, boolean isNewDate) {
        if (amountToRegister.greaterThan(remainingAmount)) {
            if (openDebts.size() > 1) {
                registerDebtCredit(clientId, event, remainingAmount, creditEntry, openDebts.get(0), isNewDate);
                registerDebtCreditList(event, openDebts.subList(1, openDebts.size()), amountToRegister.subtract(remainingAmount),
                        creditEntry, openDebts.get(1).getValue(), clientId, isNewDate);
            } else {
                throw new Error("There is no open debt to credit exemption: " + creditEntry.getId() + " for event: " + event.getExternalId());
            }
        } else {
            registerDebtCredit(clientId, event, amountToRegister, creditEntry, openDebts.get(0), isNewDate);
        }
    }

    /**
     * Returns the open debts and the remaining value of the first open debt
     * The list is ordered, the first open debt is the first of the list
     *
     * @return
     */
    private AbstractMap.SimpleImmutableEntry<List<SapRequest>, Money> getOpenDebtsAndRemainingValue(final Event event) {
        List<SapRequest> debtEntries = getDebtEntries(event).sorted(SapRequest.COMPARATOR_BY_ORDER).collect(Collectors.toList());
        Money debtAmount = Money.ZERO;
        Money firstRemainingValue = Money.ZERO;
        Money totalAmount = getDebtCreditAmount(event);
        List<SapRequest> openDebtEntries = new ArrayList<SapRequest>();
        for (SapRequest debtEntry : debtEntries) {
            debtAmount = debtAmount.add(debtEntry.getValue());
            if (debtAmount.greaterOrEqualThan(totalAmount)) {
                if (firstRemainingValue.isZero()) {
                    firstRemainingValue = debtAmount.subtract(totalAmount);
                }
                openDebtEntries.add(debtEntry);
            }
        }
        return new AbstractMap.SimpleImmutableEntry<>(openDebtEntries, firstRemainingValue);
    }

    public Money getDebtCreditAmount(final Event event) {
        return addAll(event, SapRequestType.DEBT_CREDIT);
    }

    private Money addAll(final Event event, final SapRequestType sapRequestType) {
        return getFilteredSapRequestStream(event).filter(sr -> sr.getRequestType().equals(sapRequestType))
                .map(SapRequest::getValue).reduce(Money.ZERO, Money::add);
    }

    private Stream<SapRequest> getDebtEntries(final Event event) {
        return getFilteredSapRequestStream(event).filter(sr -> sr.getRequestType() == SapRequestType.DEBT)
                .filter(sr -> sr.getValue().isPositive());
    }

    public Stream<SapRequest> getFilteredSapRequestStream(final Event event) {
        return event.getSapRequestSet().stream().filter(r -> !r.getIgnore());
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

    private LocalDate[] getDebtInterval(final Event event, final DateTime documentDate, final boolean isNewDate) {
        LocalDate startDate = isNewDate ? currentDate : documentDate.toLocalDate();
        final LocalDate endDate;
        if (event instanceof PhdGratuityEvent) {
            PhdGratuityEvent phdEvent = (PhdGratuityEvent) event;
            final LocalDate localDate = phdEvent.getPhdGratuityDate().getYear() == phdEvent.getYear() ?
                    phdEvent.getPhdGratuityDate().toLocalDate() : phdEvent.getWhenOccured().toLocalDate();
            endDate = localDate.plusYears(1);
        } else {
            final ExecutionYear executionYear = Utils.executionYearOf(event);
            if (startDate.isBefore(executionYear.getBeginLocalDate())) {
                startDate = executionYear.getBeginLocalDate();
            }
            endDate = executionYear.getEndDateYearMonthDay().toLocalDate();
        }
        return startDate.isAfter(endDate) ? null : new LocalDate[]{startDate, endDate};
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
        json.addProperty("toDate", new DateTime().toString(GiafInvoiceConfiguration.DT_FORMAT)); //tem impacto no ano fiscal!!!
        json.addProperty("productCompanyTaxId", "999999999");
        json.addProperty("productId", "FenixEdu/FenixEdu");
        json.addProperty("productVersion", "5.0.0.0");
        json.addProperty("softwareCertificateNumber", 0);
        json.addProperty("taxAccountingBasis", "P");
        json.addProperty("taxEntity", "Global");
        json.addProperty("taxRegistrationNumber", IST_VAT_NUMBER);
        return json;
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

    private JsonObject toJsonClient(final ExternalClient externalClient) {
        final JsonObject clientData = new JsonObject();
        clientData.addProperty("accountId", externalClient.getAccountId());
        clientData.addProperty("companyName", externalClient.getCompanyName());
        clientData.addProperty("clientId", externalClient.getClientId());
        clientData.addProperty("country", externalClient.getCountry());
        clientData.addProperty("street", externalClient.getStreet());
        clientData.addProperty("city", externalClient.getCity());
        clientData.addProperty("region", externalClient.getRegion());
        clientData.addProperty("postalCode", externalClient.getPostalCode());
        clientData.addProperty("vatNumber", externalClient.getFiscalCountry() + externalClient.getVatNumber());
        clientData.addProperty("fiscalCountry", externalClient.getFiscalCountry());
        clientData.addProperty("nationality", externalClient.getNationality());
        clientData.addProperty("billingIndicator", externalClient.getBillingIndicator());
        return clientData;
    }

    public static AbstractMap.SimpleImmutableEntry<String, String> mapToProduct(Event event, String eventDescription,
                                                                                boolean isDebtRegistration, boolean isInterest,
                                                                                boolean isAdvancement, boolean isPastEvent) {
        if (isPastEvent) {
            return new AbstractMap.SimpleImmutableEntry<String, String>("0063", "REGULARIZAÇAO ANOS ANTERIORES");
        }
        if (isInterest) {
            return new AbstractMap.SimpleImmutableEntry<String, String>("0036", "MULTAS");
        }
        if (isAdvancement) {
            return new AbstractMap.SimpleImmutableEntry<String, String>("0056", "ADIANTAMENTO");
        }
        if (event instanceof CustomEvent) {
            final CustomEvent customEvent = (CustomEvent) event;
            final JsonObject config = customEvent.getConfigObject();
            String productCode = config.get("productCode").getAsString();
            String productDescription = config.get("productDescription").getAsString();
            if (isDebtRegistration) {
                productCode = "E" + productCode;
                productDescription = "ESP " + productDescription;
            }
            return new AbstractMap.SimpleImmutableEntry<String, String>(productCode, productDescription);
        }
        if (event.isGratuity() && !(event instanceof PhdGratuityEvent)) {
            final GratuityEvent gratuityEvent = (GratuityEvent) event;
            final StudentCurricularPlan scp = gratuityEvent.getStudentCurricularPlan();
            final Degree degree = scp.getDegree();
            if (scp.getRegistration().getRegistrationProtocol().isAlien()) {
                if (isDebtRegistration) {
                    return new AbstractMap.SimpleImmutableEntry<String, String>("E0075", "ESP PROPINAS INTERNACIONAL");
                } else {
                    return new AbstractMap.SimpleImmutableEntry<String, String>("0075", "PROPINAS INTERNACIONAL");
                }
            }
            if (degree.isFirstCycle() && degree.isSecondCycle()) {
                if (isDebtRegistration) {
                    return new AbstractMap.SimpleImmutableEntry<String, String>("E0030", "ESP PROPINAS MESTRADO INTEGRADO");
                } else {
                    return new AbstractMap.SimpleImmutableEntry<String, String>("0030", "PROPINAS MESTRADO INTEGRADO");
                }
            }
            if (degree.isFirstCycle()) {
                if (isDebtRegistration) {
                    return new AbstractMap.SimpleImmutableEntry<String, String>("E0027", "ESP PROPINAS 1 CICLO");
                } else {
                    return new AbstractMap.SimpleImmutableEntry<String, String>("0027", "PROPINAS 1 CICLO");
                }
            }
            if (degree.isSecondCycle()) {
                if (isDebtRegistration) {
                    return new AbstractMap.SimpleImmutableEntry<String, String>("E0028", "ESP PROPINAS 2 CICLO");
                } else {
                    return new AbstractMap.SimpleImmutableEntry<String, String>("0028", "PROPINAS 2 CICLO");
                }
            }
            if (degree.isThirdCycle()) {
                if (degree.getDegreeType().isAdvancedFormationDiploma()) {
                    if (isDebtRegistration) {
                        return new AbstractMap.SimpleImmutableEntry<String, String>("E0088", "ESP PROPINAS POS-GRADUACOES");
                    } else {
                        return new AbstractMap.SimpleImmutableEntry<String, String>("0088", "PROPINAS POS-GRADUACOES");
                    }
                } else {
                    if (isDebtRegistration) {
                        return new AbstractMap.SimpleImmutableEntry<String, String>("E0029", "ESP PROPINAS 3 CICLO");
                    } else {
                        return new AbstractMap.SimpleImmutableEntry<String, String>("0029", "PROPINAS 3 CICLO");
                    }
                }
            }
            if (isDebtRegistration) {
                return new AbstractMap.SimpleImmutableEntry<String, String>("E0076", "ESP PROPINAS OUTROS");
            } else {
                return new AbstractMap.SimpleImmutableEntry<String, String>("0076", "PROPINAS OUTROS");
            }
        }
        if (event instanceof PhdGratuityEvent) {
            if (isDebtRegistration) {
                return new AbstractMap.SimpleImmutableEntry<String, String>("E0029", "ESP PROPINAS 3 CICLO");
            } else {
                return new AbstractMap.SimpleImmutableEntry<String, String>("0029", "PROPINAS 3 CICLO");
            }
        }
        if (event instanceof ExternalScholarshipPhdGratuityContribuitionEvent) {
            return new AbstractMap.SimpleImmutableEntry<String, String>("0029", "PROPINAS 3 CICLO");
        }
        if (event.isResidenceEvent()) {
            return null;
        }
        if (event.isFctScholarshipPhdGratuityContribuitionEvent()) {
            return null;
        }
        if (event.isAcademicServiceRequestEvent()) {
            if (eventDescription.indexOf(" Reingresso") >= 0) {
                return new AbstractMap.SimpleImmutableEntry<String, String>("0035", "OUTRAS TAXAS");
            }
            return new AbstractMap.SimpleImmutableEntry<String, String>("0037", "EMOLUMENTOS");
        }
        if (event.isDfaRegistrationEvent()) {
            return new AbstractMap.SimpleImmutableEntry<String, String>("0031", "TAXAS DE MATRICULA");
        }
        if (event.isIndividualCandidacyEvent()) {
            return new AbstractMap.SimpleImmutableEntry<String, String>("0031", "TAXAS DE MATRICULA");
        }
        if (event.isEnrolmentOutOfPeriod()) {
            return new AbstractMap.SimpleImmutableEntry<String, String>("0035", "OUTRAS TAXAS");
        }
        if (event instanceof AdministrativeOfficeFeeAndInsuranceEvent) {
            return new AbstractMap.SimpleImmutableEntry<String, String>("0031", "TAXAS DE MATRICULA");
        }
        if (event instanceof AdministrativeOfficeFeeEvent) {
            return new AbstractMap.SimpleImmutableEntry<String, String>("0031", "TAXAS DE MATRICULA");
        }
        if (event instanceof InsuranceEvent) {
            return new AbstractMap.SimpleImmutableEntry<String, String>("0034", "SEGURO ESCOLAR");
        }
        if (event.isSpecializationDegreeRegistrationEvent()) {
            return new AbstractMap.SimpleImmutableEntry<String, String>("0031", "TAXAS DE MATRICULA");
        }
        if (event instanceof ImprovementOfApprovedEnrolmentEvent || (event instanceof EnrolmentEvaluationEvent
                && event.getEventType() == EventType.IMPROVEMENT_OF_APPROVED_ENROLMENT)) {
            return new AbstractMap.SimpleImmutableEntry<String, String>("0033", "TAXAS DE MELHORIAS DE NOTAS");
        }
        if (event instanceof DFACandidacyEvent) {
            return new AbstractMap.SimpleImmutableEntry<String, String>("0031", "TAXAS DE MATRICULA");
        }
        if (event instanceof SpecialSeasonEnrolmentEvent
                || (event instanceof EnrolmentEvaluationEvent && event.getEventType() == EventType.SPECIAL_SEASON_ENROLMENT)
                || (event instanceof EnrolmentEvaluationEvent && event.getEventType() == EventType.EXTRAORDINARY_SEASON_ENROLMENT)) {
            return new AbstractMap.SimpleImmutableEntry<String, String>("0032", "TAXAS  DE EXAMES");
        }
        if (event.isPhdEvent()) {
            if (eventDescription.indexOf("Taxa de Inscri") >= 0) {
                return new AbstractMap.SimpleImmutableEntry<String, String>("0031", "TAXAS DE MATRICULA");
            }
            if (eventDescription.indexOf("Requerimento de provas") >= 0) {
                return new AbstractMap.SimpleImmutableEntry<String, String>("0032", "TAXAS  DE EXAMES");
            }
            return new AbstractMap.SimpleImmutableEntry<String, String>("0031", "TAXAS DE MATRICULA");
        }
        throw new Error("not.supported: " + event.getExternalId());
    }

    public JsonObject toJson(final Event event, final JsonObject clientData, DateTime documentDate, boolean isDebtRegistration,
                             boolean isNewDate, boolean isInterest, boolean isAdvancement, boolean isPastPayment) {
        final JsonObject json = toJsonCommon(documentDate, isNewDate);

        final String description = event.getDescription().toString();
        final AbstractMap.SimpleImmutableEntry<String, String> product = mapToProduct(event, description, isDebtRegistration, isInterest, isAdvancement, isPastPayment);
        json.addProperty("productCode", product.getKey());
        json.addProperty("productDescription", detailedDescription(product.getValue(), event));

        json.add("clientData", clientData);

        return json;
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

    private Long getDocumentNumber() {
        return SapRoot.getInstance().getAndSetNextDocumentNumber();
    }

    CreditEntry getCreditEntry(final Money creditAmount, final String creditID) {
        return new CreditEntry(creditID, new DateTime(), new LocalDate(), "", creditAmount.getAmount()) {
            @Override
            public BigDecimal getUsedAmountInDebts() {
                return getAmount();
            }

            @Override
            public boolean isToApplyInterest() {
                return false;
            }

            @Override
            public boolean isToApplyFine() {
                return false;
            }

            @Override
            public boolean isForInterest() {
                return false;
            }

            @Override
            public boolean isForFine() {
                return false;
            }

            @Override
            public boolean isForDebt() {
                return false;
            }
        };
    }
}