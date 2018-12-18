package pt.ist.fenix.webapp;

import org.fenixedu.bennu.scheduler.custom.CustomTask;

public class ValidateCompanyData extends CustomTask {

    int ok = 0;
    int fixable = 0;
    int problems = 0;

    @Override
    public void runTask() throws Exception {
//        final Spreadsheet sheet = new Spreadsheet("NIFS");
//        eventStream()
//            .map(e -> e.getParty())
//                .filter(p -> !(p instanceof Person))
//            .distinct()
//            .forEach(p -> validate(sheet, p));
//        
//        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
//        sheet.exportToCSV(stream, "\t");
//        output("nifs.xls", stream.toByteArray());
//
//        taskLog("Ok: %s   Fixable: %s   Problems: %s%n", ok, fixable, problems);
    }

//    private Stream<Event> eventStream() {
//        return Stream.concat(Bennu.getInstance().getAccountingEventsSet().stream(),
//                Bennu.getInstance().getAccountingTransactionDetailsSet().stream().map(t -> t.getEvent()))
//                .filter(e -> EventWrapper.needsProcessingSap(e));
//    }
//
//    private void fix(final Party party) {
//        if (party != null && isValid(party)) {
//            final String tin = validTin(party);
//            party.setSocialSecurityNumber(tin);
//            taskLog("Setting valid TIN for %s - %s%n", Utils.getUserIdentifier(party), party.getExternalId());
//        }
//    }
//
//    private String validTin(Party p) {
//        final String tin = p.getSocialSecurityNumber();
//        if (tin == null || tin.trim().isEmpty()) {
//            return null;
//        } else {
//            if (tin.length() > 2
//                    && Character.isAlphabetic(tin.charAt(0))
//                    && Character.isAlphabetic(tin.charAt(1))
//                    && Character.isUpperCase(tin.charAt(0))
//                    && Character.isUpperCase(tin.charAt(1))) {
//                final String countryCode = tin.substring(0, 2);
//                final String code = tin.substring(2);
//                if (TINValid.checkTIN(countryCode, code) == 0) {
//                    // all is ok
//                    return tin;
//                }
//            }
//            final Country country = getValidCountry(tin, p.getCountry(), p.getCountryOfResidence());
//            if (country != null) {
//                return country.getCode() + tin;
//            }
//        }
//        return null;
//    }
//
//    private boolean isValid(final Party p) {
//        final String tin = p.getSocialSecurityNumber();
//        if (tin == null || tin.trim().isEmpty()) {
//            return false;
//        } else {
//            if (tin.length() > 2
//                    && Character.isAlphabetic(tin.charAt(0))
//                    && Character.isAlphabetic(tin.charAt(1))
//                    && Character.isUpperCase(tin.charAt(0))
//                    && Character.isUpperCase(tin.charAt(1))) {
//                final String countryCode = tin.substring(0, 2);
//                final String code = tin.substring(2);
//                if (TINValid.checkTIN(countryCode, code) == 0) {
//                    // all is ok
//                    return true;
//                }
//            }
//            if (hasValidTin(tin, p.getCountry(), p.getCountryOfResidence())) {
//                // all is ok
//                return true;
//            }
//        }
//        return false;
//    }
//
//    private Country getValidCountry(final String tin, final Country... countries) {
//        for (int i = 0; i < countries.length; i++) {
//            final Country country = countries[i];
//            if (country != null && TINValid.checkTIN(country.getCode().toUpperCase(), tin) == 0) {
//                return country;
//            }
//        }
//        return null;
//    }
//    
//    private void validate(final Spreadsheet sheet, final Party p) {
//        final String tin = p.getSocialSecurityNumber();
//        if (tin == null || tin.trim().isEmpty()) {
//            report(sheet, p, "No TIN", tin);
//            problems++;
//            return;
//        } else {
//            if (tin.length() > 2
//                    && Character.isAlphabetic(tin.charAt(0))
//                    && Character.isAlphabetic(tin.charAt(1))
//                    && Character.isUpperCase(tin.charAt(0))
//                    && Character.isUpperCase(tin.charAt(1))) {
//                final String countryCode = tin.substring(0, 2);
//                final String code = tin.substring(2);
//                if (TINValid.checkTIN(countryCode, code) == 0) {
//                    // all is ok
//                    ok++;
//                    return;
//                }
//            }
//            if (hasValidTin(tin, p.getCountry(), p.getCountryOfResidence())) {
//                // all is ok
//                fixable++;
//                fix(p);
//                return;
//            }
//        }
//        problems++;
//        report(sheet, p, "No valid TIN", tin);
//    }
//
//    private boolean hasValidTin(final String tin, final Country... countries) {
//        for (int i = 0; i < countries.length; i++) {
//            final Country country = countries[i];
//            if (country != null && TINValid.checkTIN(country.getCode().toUpperCase(), tin) == 0) {
//                return true;
//            }
//        }
//        return false;
//    }
//
//    private void report(final Spreadsheet sheet, final Party p, final String error, final String tin) {
//        final Row row = sheet.addRow();
//        row.setCell("user", Utils.getUserIdentifier(p));
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
//
//        row.setCell("country", country == null ? " " : country.getCode());
//        row.setCell("countryOfResidence", countryOfResidence == null ? " " : countryOfResidence.getCode());
//    }

}