package pt.ist.fenix.webapp;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.Country;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;

import eu.europa.ec.taxud.tin.algorithm.TINValid;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;

public class ValidateStudentData extends CustomTask {

    @Override
    public TxMode getTxMode() {
        return TxMode.READ;
    }
    
    int ok = 0;
    int fixable = 0;
    int problems = 0;

    @Override
    public void runTask() throws Exception {
        final Spreadsheet sheet = new Spreadsheet("NIFS");
        final Set<Person> people = eventStream()
            .map(e -> e.getParty())
            .filter(p -> p instanceof Person)
            .map(p -> (Person) p)
            .distinct()
            .collect(Collectors.toSet());

        taskLog("Completed collecting people. Running validation.");

        people.stream().parallel()
            .forEach(p -> validate(sheet, p));
        
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        sheet.exportToCSV(stream, "\t");
        output("nifs.xls", stream.toByteArray());

//        Bennu.getInstance().getAccountingEventsSet().stream()
//            .flatMap(e -> e.getSapRequestSet().stream())
//            .filter(sr -> sr.getRequest().length() > 2)
//            .filter(sr -> !sr.getIntegrated())
//            .filter(sr -> sr.getIntegrationMessage() != null)
//            .filter(sr -> sr.getIntegrationMessage().indexOf("\"Cliente\":") >= 0)
//            .forEach(sr -> fix(sr));
//            ;
//        
//
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
//                .filter(e -> needsProcessingSap(e));
    }

    private void fix(final Person person) {
        if (person != null && isValid(person)) {
            final String tin = validTin(person);
            person.setSocialSecurityNumber(tin);
//            taskLog("Setting valid TIN for %s - %s%n", person.getUsername(), person.getExternalId());
        }
    }

    private String validTin(Person p) {
        final String tin = p.getSocialSecurityNumber();
        if (tin == null || tin.trim().isEmpty()) {
            return null;
        } else {
            if (tin.length() > 2
                    && Character.isAlphabetic(tin.charAt(0))
                    && Character.isAlphabetic(tin.charAt(1))
                    && Character.isUpperCase(tin.charAt(0))
                    && Character.isUpperCase(tin.charAt(1))) {
                final String countryCode = tin.substring(0, 2);
                final String code = tin.substring(2);
                if (TINValid.checkTIN(countryCode, code) == 0) {
                    // all is ok
                    return tin;
                }
            }
            final Country country = getValidCountry(tin, p.getCountry(), p.getCountryOfResidence(), p.getCountryOfBirth());
            if (country != null) {
                return country.getCode() + tin;
            }
        }
        return null;
    }

    private boolean isValid(final Person p) {
        final String tin = p.getSocialSecurityNumber();
        if (tin == null || tin.trim().isEmpty()) {
            return false;
        } else {
            if (tin.length() > 2
                    && Character.isAlphabetic(tin.charAt(0))
                    && Character.isAlphabetic(tin.charAt(1))
                    && Character.isUpperCase(tin.charAt(0))
                    && Character.isUpperCase(tin.charAt(1))) {
                final String countryCode = tin.substring(0, 2);
                final String code = tin.substring(2);
                if (TINValid.checkTIN(countryCode, code) == 0) {
                    // all is ok
                    return true;
                }
            }
            if (hasValidTin(tin, p.getCountry(), p.getCountryOfResidence(), p.getCountryOfBirth())) {
                // all is ok
                return true;
            }
        }
        return false;
    }

    private Country getValidCountry(final String tin, final Country... countries) {
        for (int i = 0; i < countries.length; i++) {
            final Country country = countries[i];
            if (country != null && TINValid.checkTIN(country.getCode().toUpperCase(), tin) == 0) {
                return country;
            }
        }
        return null;
    }
    
    private void validate(final Spreadsheet sheet, final Person p) {
        FenixFramework.atomic(new Runnable() {
            @Override
            public void run() {
                if (FenixFramework.isDomainObjectValid(p)) {
                    final String tin = p.getSocialSecurityNumber();
                    if (tin == null || tin.trim().isEmpty()) {
                        report(sheet, p, "No TIN", tin);
                        problems++;
                        return;
                    } else {
                        if (tin.length() > 2
                                && Character.isAlphabetic(tin.charAt(0))
                                && Character.isAlphabetic(tin.charAt(1))
                                && Character.isUpperCase(tin.charAt(0))
                                && Character.isUpperCase(tin.charAt(1))) {
                            final String countryCode = tin.substring(0, 2);
                            final String code = tin.substring(2);
                            if (TINValid.checkTIN(countryCode, code) == 0) {
                                // all is ok
                                ok++;
                                return;
                            }
                        }
                        if (hasValidTin(tin, p.getCountry(), p.getCountryOfResidence(), p.getCountryOfBirth())) {
                            // all is ok
                            fixable++;
                            fix(p);
                            return;
                        }
                    }
                    problems++;
                    report(sheet, p, "No valid TIN", tin);
                }
            }
        });
    }

    private boolean hasValidTin(final String tin, final Country... countries) {
        for (int i = 0; i < countries.length; i++) {
            final Country country = countries[i];
            if (country != null && TINValid.checkTIN(country.getCode().toUpperCase(), tin) == 0) {
                return true;
            }
        }
        return false;
    }

    private void report(final Spreadsheet sheet, final Person p, final String error, final String tin) {
//        final Row row = sheet.addRow();
//        row.setCell("user", p.getUsername());
//        row.setCell("person", p.getExternalId());
//        row.setCell("name", p.getName());
//        row.setCell("error", error);
//        row.setCell("tin", tin);
//
//        final Money value = p.getEventsSet().stream()
//            .flatMap(e -> e.getSapRequestSet().stream())
//            .filter(sr -> sr.getRequest().length() > 2)
//            .map(sr -> sr.getValue())
//            .reduce(Money.ZERO, Money::add);
//        row.setCell("value", value.toPlainString());
//
//        final Country country = p.getCountry();
//        final Country countryOfResidence = p.getCountryOfResidence();
//        final Country countryIfBirth = p.getCountryOfBirth();
//
//        row.setCell("country", country == null ? " " : country.getCode());
//        row.setCell("countryOfResidence", countryOfResidence == null ? " " : countryOfResidence.getCode());
//        row.setCell("countryIfBirth", countryIfBirth == null ? " " : countryIfBirth.getCode());
    }

}