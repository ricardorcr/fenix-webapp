package pt.ist.fenix.webapp;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Stream;

import org.fenixedu.PostalCodeValidator;
import org.fenixedu.academic.domain.Country;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.contacts.PartyContact;
import org.fenixedu.academic.domain.contacts.PhysicalAddress;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.commons.spreadsheet.Spreadsheet.Row;

import eu.europa.ec.taxud.tin.algorithm.TINValid;
import pt.ist.fenixframework.FenixFramework;

public class ValidatePostCodes extends CustomTask {

    int ok = 0;
    int fixable = 0;
    int problems = 0;

    @Override
    public void runTask() throws Exception {
        final Spreadsheet sheet = new Spreadsheet("PostCodes");

        eventStream()
            .map(e -> e.getParty())
            .filter(p -> p instanceof Person)
            .map(p -> (Person) p)
            .distinct()
            .forEach(p -> validate(sheet, p))
            ;
        
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        sheet.exportToCSV(stream, "\t");
        output("postCodes.xls", stream.toByteArray());
        
        taskLog("Ok: %s   Fixable: %s   Problems: %s%n", ok, fixable, problems);
    }

    private Stream<Event> eventStream() {
        final File file = new File("/home/rcro/Documents/fenix/sap/dividas_fenix_28_maio/dividas_maio_giaf.csv");
        List<String> allLines = null;
        try {
            allLines = Files.readAllLines(file.toPath());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return allLines.stream().map(s -> FenixFramework.getDomainObject(s.split("\t")[0]));
//        return Stream.concat(Bennu.getInstance().getAccountingEventsSet().stream(),
//                Bennu.getInstance().getAccountingTransactionDetailsSet().stream().map(t -> t.getEvent()))
//                .filter(e -> EventWrapper.needsProcessingSap(e));
    }

    private String[] validate(final Spreadsheet sheet, final Person p) {
        final PhysicalAddress physicalAddress = toAddress(p);
        final String areaCode = physicalAddress == null ? null : physicalAddress.getAreaCode();
        if (areaCode == null || areaCode.trim().isEmpty()) {
            final String[] fix = generate(countryFromNif(p), p.getCountry(), p.getCountryOfResidence(), p.getCountryOfBirth());
                if (fix == null) {
                    report(sheet, p, "No area code", areaCode);
                } else {
                    taskLog("Fixing areaCode for %s : %s : %s%n", p.getUsername(), fix[0], fix[1]);
                    physicalAddress.setCountryOfResidence(Country.readByTwoLetterCode(fix[0]));
                    physicalAddress.setAreaCode(fix[1]);
                    return fix;
                }
            return null;
        } else {
            String[] result = vaalidate(areaCode.trim(), p.getCountry(), p.getCountryOfResidence(), p.getCountryOfBirth());
            if (result == null) {
                final String[] fix = generate(countryFromNif(p), p.getCountry(), p.getCountryOfResidence(), p.getCountryOfBirth());
                if (fix == null) {
                    report(sheet, p, "No valid AreaCode", areaCode);
                } else {
                    taskLog("Fixing areaCode for %s : %s : %s%n", p.getUsername(), fix[0], fix[1]);
                    physicalAddress.setCountryOfResidence(Country.readByTwoLetterCode(fix[0]));
                    physicalAddress.setAreaCode(fix[1]);
                    return fix;
                }
            }
            return result;
        }
    }

    public static PhysicalAddress toAddress(final Person person) {
        PhysicalAddress address = person.getDefaultPhysicalAddress();
        if (address == null) {
            for (final PartyContact contact : person.getPartyContactsSet()) {
                if (contact instanceof PhysicalAddress) {
                    address = (PhysicalAddress) contact;
                    break;
                }
            }
        }
        return address;
    }

    private Country countryFromNif(final Person p) {
        final String ssn = p.getSocialSecurityNumber();
        if (ssn != null && ssn.length() > 2 && TINValid.checkTIN(ssn.substring(0, 2), ssn.substring(2)) == 0) {
            final Country country = Country.readByTwoLetterCode(ssn.substring(0, 2));
            return country;
        }
        return null;
    }

    private String[] generate(final Country... countries) {
        for (int i = 0; i < countries.length; i++) {
            final Country country = countries[i];
            if (country != null) {
                final String countryCode = country.getCode();
                try {
                    final String examplePostalCode = PostalCodeValidator.examplePostCodeFor(countryCode);
                    if (examplePostalCode != null) {
                        return new String[] { countryCode, examplePostalCode };
                    }
                } catch (final Error e) {
                    if (e.getMessage().startsWith("No validator for")) {
                        return null;
                    }
                    throw e;
                }
            }
        }
        return null;
    }

    private String[] vaalidate(final String areaCode, final Country... countries) {
        for (int i = 0; i < countries.length; i++) {
            final Country country = countries[i];
            try {
                if (country != null && PostalCodeValidator.isValidAreaCode(country.getCode(), areaCode)) {
                    return new String[] { country.getCode(), areaCode };
                }
            } catch (final Error e) {
                if (e.getMessage().startsWith("No validator for")) {
                    taskLog("%s%n", e.getMessage());
                    return null;
                }
                throw e;
            }
        }
        return null;
    }

    private void report(final Spreadsheet sheet, final Person p, final String error, final String areaCode) {
        final Row row = sheet.addRow();
        row.setCell("user", p.getUsername());
        row.setCell("person", p.getExternalId());
        row.setCell("name", p.getName());
        row.setCell("error", error);
        row.setCell("areaCode", areaCode);

        final Money value = p.getEventsSet().stream()
            .flatMap(e -> e.getSapRequestSet().stream())
            .filter(sr -> sr.getRequest().length() > 2)
            .map(sr -> sr.getValue())
            .reduce(Money.ZERO, Money::add);
        row.setCell("value", value.toPlainString());

        final Country country = p.getCountry();
        final Country countryOfResidence = p.getCountryOfResidence();
        final Country countryIfBirth = p.getCountryOfBirth();

        row.setCell("country", country == null ? " " : country.getCode());
        row.setCell("countryOfResidence", countryOfResidence == null ? " " : countryOfResidence.getCode());
        row.setCell("countryIfBirth", countryIfBirth == null ? " " : countryIfBirth.getCode());
    }

}