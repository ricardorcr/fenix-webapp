package pt.ist.fenix.webapp;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.fenixedu.PostalCodeValidator;
import org.fenixedu.academic.domain.Country;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.ResidenceEvent;
import org.fenixedu.academic.domain.accounting.events.gratuity.ExternalScholarshipGratuityContributionEvent;
import org.fenixedu.academic.domain.contacts.PhysicalAddress;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.organizationalStructure.Party;
import org.fenixedu.academic.domain.phd.debts.ExternalScholarshipPhdGratuityContribuitionEvent;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.commons.spreadsheet.Spreadsheet.Row;
import org.fenixedu.generated.sources.saft.sap.SAFTPTSourceBilling;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import com.google.gson.JsonObject;

import eu.europa.ec.taxud.tin.algorithm.TINValid;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.domain.SapRoot;
import pt.ist.fenixedu.giaf.invoices.ClientMap;
import pt.ist.fenixedu.giaf.invoices.DebtCycleType;
import pt.ist.fenixedu.giaf.invoices.ErrorLogConsumer;
import pt.ist.fenixedu.giaf.invoices.EventLogger;
import pt.ist.fenixedu.giaf.invoices.SapEvent;
import pt.ist.fenixedu.giaf.invoices.Utils;
import pt.ist.fenixframework.FenixFramework;

public class RegisterGiafDebtToSAP extends CustomTask {

    private static final String FCT_NIF = "503904040";
    private static final String DT_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String MORADA_DESCONHECIDO = "Desconhecido";
    private static final int MAX_SIZE_ADDRESS = 100;
    private static final int MAX_SIZE_CITY = 50;
    private static final int MAX_SIZE_REGION = 50;
    private static final int MAX_SIZE_POSTAL_CODE = 20;
    private static final int MAX_SIZE_VAT_NUMBER = 20;
    public LocalDate currentDate = new LocalDate();

    @Override
    public void runTask() throws Exception {

        final Spreadsheet errors = new Spreadsheet("Errors");
        final ErrorLogConsumer errorLogConsumer = new ErrorLogConsumer() {

            @Override
            public void accept(final String oid, final String user, final String name, final String amount,
                    final String cycleType, final String error, final String args, final String type,
                    final String countryOfVatNumber, final String vatNumber, final String address, final String locality,
                    final String postCode, final String countryOfAddress, final String paymentMethod, final String documentNumber,
                    final String action) {

                final Row row = errors.addRow();
                row.setCell("OID", oid);
                row.setCell("user", user);
                row.setCell("name", name);
                row.setCell("amount", amount);
                row.setCell("cycleType", cycleType);
                row.setCell("error", error);
                row.setCell("args", args);
                row.setCell("type", type);
                row.setCell("countryOfVatNumber", countryOfVatNumber);
                row.setCell("vatNumber", vatNumber);
                row.setCell("address", address);
                row.setCell("locality", locality);
                row.setCell("postCode", postCode);
                row.setCell("countryOfAddress", countryOfAddress);
                row.setCell("paymentMethod", paymentMethod);
                row.setCell("documentNumber", documentNumber);
                row.setCell("action", action);
                Event event = FenixFramework.getDomainObject(oid);
                row.setCell("year", Utils.executionYearOf(event).getName());

            }
        };
        final EventLogger elogger = (msg, args) -> taskLog(msg, args);

        final File file = new File("/afs/ist.utl.pt/ciist/fenix/fenix036/dividas_maio_giaf.csv");
        List<String> allLines = Files.readAllLines(file.toPath());

        //SapEvent.documentDatePreviousYear = (currentDate) -> new DateTime(currentDate.getYear() - 1, 12, 25, 23, 59);

        allLines.stream().forEach(l -> {
            process(l, errorLogConsumer, elogger);
        });

        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            errors.exportToCSV(stream, "\t");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        output("FenixErrorLog.xls", stream.toByteArray());
        totalDebt.forEach((k, v) -> taskLog("%s - %s\n", k.getName(), v));
        taskLog("##############");
        totalDebtWithErrors.forEach((k, v) -> taskLog("%s - %s\n", k.getName(), v));
        taskLog("##############");
        totalDebtOnlyErrors.forEach((k, v) -> taskLog("%s - %s\n", k.getName(), v));
    }

    Map<ExecutionYear, Money> totalDebt = new TreeMap<>();
    Map<ExecutionYear, Money> totalDebtWithErrors = new TreeMap<>();
    Map<ExecutionYear, Money> totalDebtOnlyErrors = new TreeMap<>();

    private void process(String line, ErrorLogConsumer errorLogConsumer, EventLogger elogger) {
        String[] content = line.split("\t");
        Event event = FenixFramework.getDomainObject(content[0]);
        if (event.getExternalId().equals("1978055778107411")) {

            if (isToProcess(event)) {
                final Money amount = new Money(content[1]);
                totalDebtWithErrors.compute(Utils.executionYearOf(event), (k, v) -> (v == null) ? amount : v.add(amount));
                if (validate(errorLogConsumer, event, amount)) {
                    process(event, amount, errorLogConsumer, elogger);
                } else {
                    totalDebtOnlyErrors.compute(Utils.executionYearOf(event), (k, v) -> (v == null) ? amount : v.add(amount));
                }
            }
        }
    }

    private boolean isToProcess(Event event) {
//        if (event instanceof ExternalScholarshipGratuityContributionEvent) {
//            return false;
//        }
        if (event instanceof ResidenceEvent) {
            return false;
        }
        if (event instanceof ExternalScholarshipPhdGratuityContribuitionEvent) {
            return false;
        }
        return true;
    }

    private void process(Event event, Money amount, ErrorLogConsumer errorLogConsumer, EventLogger elogger) {
        totalDebt.compute(Utils.executionYearOf(event), (k, v) -> (v == null) ? amount : v.add(amount));
        final SapEvent sapEvent = new SapEvent(event);
        try {
            registerInvoice(sapEvent, amount, event, event.isGratuity(), false, errorLogConsumer, elogger);
        } catch (Exception e) {
            taskLog("Event %s deu erro: %s.\n", event.getExternalId(), e.getMessage());
            e.printStackTrace();
        }
    }

    private void registerInvoice(SapEvent sapEvent, Money amount, Event event, boolean isGratuity, boolean isNewDate,
            ErrorLogConsumer errorLogConsumer, EventLogger elogger) throws Exception {
        String clientId = ClientMap.uVATNumberFor(event.getParty());
        JsonObject data = toJsonInvoice(event, amount, getDocumentDate(event.getWhenOccured(), isNewDate), new DateTime(),
                clientId, false, isNewDate, false, false);

        String documentNumber = getDocumentNumber(data, false);
        new SapRequest(event, clientId, amount, documentNumber, SapRequestType.INVOICE, Money.ZERO, data);
    }

    private DateTime getDocumentDate(DateTime documentDate, boolean isNewDate) {
        //return SapEvent.documentDatePreviousYear.apply(currentDate);
        return new DateTime();
    }

    private String getDocumentNumber(JsonObject data, boolean paymentDocument) {
        if (paymentDocument) {
            return data.get("paymentDocument").getAsJsonObject().get("paymentDocumentNumber").getAsString();
        } else {
            return data.get("workingDocument").getAsJsonObject().get("workingDocumentNumber").getAsString();
        }
    }

    private JsonObject toJsonInvoice(Event event, Money debtFenix, DateTime documentDate, DateTime entryDate, String clientId,
            boolean isDebtRegistration, boolean isNewDate, boolean isInterest, boolean isPastPayment) throws Exception {
        JsonObject json = toJson(event, clientId, documentDate, isDebtRegistration, true, isInterest, isPastPayment);
        JsonObject workDocument =
                toJsonWorkDocument(documentDate, entryDate, debtFenix, "ND", true, new DateTime(Utils.getDueDate(event)));

        json.add("workingDocument", workDocument);
        return json;
    }

    public JsonObject toJson(final Event event, final String clientId, DateTime documentDate, boolean isDebtRegistration,
            boolean isNewDate, boolean isInterest, boolean isPastPayment) {
        final JsonObject json = new JsonObject();

        json.addProperty("finantialInstitution", "IST");
        json.addProperty("taxType", "IVA");
        json.addProperty("taxCode", "ISE");
        json.addProperty("taxCountry", "PT");
        json.addProperty("taxPercentage", "0");
        json.addProperty("auditFileVersion", "1.0.3");
        json.addProperty("processId", "006");
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
        json.addProperty("taxRegistrationNumber", "501507930");
        SimpleImmutableEntry<String, String> product =
                SapEvent.mapToProduct(event, event.getDescription().toString(), isDebtRegistration, isInterest, isPastPayment, false);
        json.addProperty("productDescription", product.getValue());
        json.addProperty("productCode", product.getKey());

        final JsonObject clientData = toJsonClient(event.getParty(), clientId);
        json.add("clientData", clientData);

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
                physicalAddress != null && !Strings.isNullOrEmpty(physicalAddress.getAddress().trim()) ? Utils
                        .limitFormat(MAX_SIZE_ADDRESS, physicalAddress.getAddress()) : MORADA_DESCONHECIDO);

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

    private JsonObject toJsonWorkDocument(DateTime documentDate, DateTime entryDate, Money amount, String documentType,
            boolean isToDebit, DateTime dueDate) throws Exception {
        JsonObject workDocument = new JsonObject();
        workDocument.addProperty("documentDate", documentDate.toString(DT_FORMAT));
        workDocument.addProperty("entryDate", entryDate.toString(DT_FORMAT));
        workDocument.addProperty("dueDate", dueDate.toString(DT_FORMAT));
        workDocument.addProperty("workingDocumentNumber", documentType + SapRoot.getInstance().getAndSetNextDocumentNumber());
        workDocument.addProperty("sourceBilling", SAFTPTSourceBilling.P.toString());
        workDocument.addProperty("workingAmount", amount.getAmountAsString());
        workDocument.addProperty("taxPayable", BigDecimal.ZERO);
        workDocument.addProperty("workType", "DC");
        workDocument.addProperty("workStatus", "N");

        workDocument.addProperty("isToDebit", isToDebit);
        workDocument.addProperty("isToCredit", !isToDebit);

//        workDocument.addProperty("compromiseMetadata", "");

        workDocument.addProperty("taxExemptionReason", "M99");
        workDocument.addProperty("unitOfMeasure", "UNID");

        return workDocument;
    }

    public static boolean validate(final ErrorLogConsumer consumer, final Event event, final Money amount) {
//        if (event.isCancelled()) {
//            logError(consumer, amount, "Event is canceled", event, null, "", null, null, null, null, event);
//            return false;
//        }
        if (event instanceof ExternalScholarshipGratuityContributionEvent) {
            logError(consumer, amount, "External scholarship", event, null, "", null, null, null, null, event);
            return false;
        }
        if (event instanceof ResidenceEvent) {
            logError(consumer, amount, "Residence Event", event, null, "", null, null, null, null, event);
            return false;
        }
        if (!event.getParty().isPerson()) {
            if (isToIgnoreNIF(event.getParty())) {
                logError(consumer, amount, "FCT", event, null, "", null, null, null, null, event);
                return false;
            }
        }
        final String eventDescription;
        try {
            eventDescription = event.getDescription().toString();
        } catch (final NullPointerException ex) {
            logError(consumer, amount, "No Description Available", event, null, "", null, null, null, null, event);
            return false;
        }
        final Money originalAmountToPay;
        try {
            originalAmountToPay = event.getOriginalAmountToPay();
        } catch (final DomainException ex) {
            logError(consumer, amount, "Unable to Determine Amount: " + ex.getMessage(), event, null, "", null, null, null, null,
                    event);
            return false;
        } catch (final NullPointerException ex) {
            logError(consumer, amount, "Unable to Determine Amount: " + ex.getMessage(), event, null, "", null, null, null, null,
                    event);
            return false;
        }

        final Party party = event.getParty();
        final Country country = party.getCountry();

        final SimpleImmutableEntry<String, String> articleCode = SapEvent.mapToProduct(event, eventDescription, false, false, false, false);
        if (articleCode == null) {
            if (eventDescription.indexOf("Pagamento da resid") >= 0) {
                logError(consumer, amount, "No Article Code - Residence", event, null, "", null, null, null, null, event);
            }
            logError(consumer, amount, "No Article Code", event, null, "", null, null, null, null, event);
            return false;
        }

        String tin = party.getSocialSecurityNumber();
        if (tin != null && tin.length() > 2 && TINValid.checkTIN(tin.substring(0, 2), tin.substring(2)) == 0) {

        } else if (country != null) {
            tin = ClientMap.uVATNumberFor(party);
        } else {
            logError(consumer, amount, "No Country", event, Utils.getUserIdentifier(party), "", country, party, null, null,
                    event);
            return false;
        }

        final PhysicalAddress address = Utils.toAddress(party, tin.substring(0, 2));
        if (address == null) {
            logError(consumer, amount, "No Address", event, Utils.getUserIdentifier(party), "", country, party, address, null,
                    event);
            return false;
        }
        final Country countryOfAddress = address.getCountryOfResidence();
        if (countryOfAddress == null) {
//            logError(consumer, "No Valid Country for Address", event, getUserIdentifier(party), "", country, party, address,
//                    countryOfAddress, event);
//            return false;
        } else if ("PT".equals(countryOfAddress.getCode()) /* || "PT".equals(country.getCode()) */) {
            if (!isValidPostCode(hackAreaCodePT(address.getAreaCode(), countryOfAddress))) {
                logError(consumer, amount, "No Valid Post Code For Address For", event, Utils.getUserIdentifier(party), "",
                        country, party, address, countryOfAddress, event);
                return false;
            }
        }

//        if ("PT".equals(country.getCode())) {
//            if (!ClientMap.isVatValidForPT(tin.substring(2))) {
//                logError(consumer, amount, "Not a Valid PT VAT Number", event, Utils.getUserIdentifier(party), tin, country,
//                        party, address, countryOfAddress, event);
//                return false;
//            }
//        }
        if ("PT999999990".equals(tin) && originalAmountToPay.greaterThan(new Money(100))) {
            logError(consumer, amount, "No VAT Number", event, Utils.getUserIdentifier(party), tin, country, party, address,
                    countryOfAddress, event);
            return false;
        }

        final BigDecimal amount2 = originalAmountToPay.getAmount();
        //final BigDecimal amount = event.getOriginalAmountToPay().getAmount();
        if (amount2.signum() <= 0) {
            if (event.isCancelled()) {
                // consumer.accept(detail, "Canceled Transaction", detail.getExternalId());
                logError(consumer, amount, "Negative amount for canceled event", event, null, "", null, null, null, null, event);
                return false;
            } else {
                //consumer.accept(t, "Zero Value For Transaction", event.getExternalId());
                logError(consumer, amount, "Negative amount", event, null, "", null, null, null, null, event);
                return false;
            }
        }
        return true;
    }

    private static void logError(final ErrorLogConsumer consumer, final Money amount, final String error, final Event event,
            final String user, final String vat, final Country country, final Party party, final PhysicalAddress address,
            final Country countryOfAddress, final Event e) {

        if (consumer == null) {
            return;
        }

        DebtCycleType cycleType;

        try {
            cycleType = Utils.cycleType(e);
        } catch (Throwable t) {
            cycleType = null;
        }

        consumer.accept(event.getExternalId(), user, party == null ? "" : party.getName(),
                amount == null ? "" : amount.toPlainString(), cycleType == null ? "" : cycleType.getDescription(), error, vat, "",
                country == null ? "" : country.getCode(), vat, address == null ? "" : address.getAddress(), "",
                address == null ? "" : address.getPostalCode(), countryOfAddress == null ? "" : countryOfAddress.getCode(), "",
                "", "");
    }

    private static boolean isToIgnoreNIF(Party party) {
        return FCT_NIF.equalsIgnoreCase(party.getSocialSecurityNumber())
                || ("PT" + FCT_NIF).equals(party.getSocialSecurityNumber());
    }

    private static String hackAreaCodePT(final String areaCode, final Country countryOfResidence) {
        if (countryOfResidence != null && "PT".equals(countryOfResidence.getCode())) {
            if (areaCode == null || areaCode.isEmpty()) {
                return "0000-000";
            }
            if (areaCode.length() == 4) {
                return areaCode + "-001";
            }
        }
        return areaCode;
    }

    private static boolean isValidPostCode(final String postalCode) {
        return false;
//        if (postalCode != null) {
//            final String v = postalCode.trim();
//            return v.length() == 8 && v.charAt(4) == '-' && CharMatcher.DIGIT.matchesAllOf(v.substring(0, 4))
//                    && CharMatcher.DIGIT.matchesAllOf(v.substring(5));
//        }
//        return false;
    }

}