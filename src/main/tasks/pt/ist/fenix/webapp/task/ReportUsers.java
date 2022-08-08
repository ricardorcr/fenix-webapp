package pt.ist.fenix.webapp.task;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.accessControl.ActiveStudentsGroup;
import org.fenixedu.academic.domain.accessControl.ActiveTeachersGroup;
import org.fenixedu.academic.domain.accessControl.AllAlumniGroup;
import org.fenixedu.academic.domain.contacts.EmailAddress;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;

import java.nio.file.Files;

import kong.unirest.Unirest;
import pt.ist.fenixedu.contracts.domain.accessControl.ActiveEmployees;
import pt.ist.fenixedu.contracts.domain.accessControl.ActiveGrantOwner;
import pt.ist.fenixedu.contracts.domain.accessControl.ActiveResearchers;

public class ReportUsers extends ReadCustomTask {

    final Spreadsheet spreadsheet = new Spreadsheet("report");
    final Spreadsheet duplicates = spreadsheet.addSpreadsheet("Duplicates");

    @Override
    public void runTask() throws Exception {
        final ActiveStudentsGroup activeStudentsGroup = new ActiveStudentsGroup();
        final ActiveEmployees activeEmployees = new ActiveEmployees();
        final ActiveTeachersGroup activeTeachersGroup = new ActiveTeachersGroup();
        final ActiveResearchers activeResearchers = new ActiveResearchers();
        final ActiveGrantOwner activeGrantOwner = new ActiveGrantOwner();
        final Group alumniGroup = new AllAlumniGroup();

        final String lfilename = "/afs/ist.utl.pt/ciist/fenix/fenix015/ist/google/ldap.csv";
        final Map<String, User> ldap =
                Files.readAllLines(new File(lfilename).toPath()).stream().skip(1l).map(line -> line.split(",")).peek(line -> {
                    if (User.findByUsername(line[1]) == null) {
                        taskLog("No user for %s%n", line[0], line[1]);
                    }
                }).filter(l -> User.findByUsername(l[1]) != null)
                        .collect(Collectors.toMap(line -> line[0].toLowerCase(), l -> User.findByUsername(l[1])));

        final Map<String, User> userMap = Bennu.getInstance().getPartysSet().stream()
                .flatMap(party -> party.getPartyContactsSet().stream()).filter(partyContact -> partyContact.isEmailAddress())
                .map(EmailAddress.class::cast).filter(emailAddress -> emailAddress.isInstitutionalType())
                .collect(Collectors.toMap(e -> e.getValue().toLowerCase(), e -> ((Person) e.getParty()).getUser(), (k1, k2) -> {
                    duplicates.addRow().setCell("username", k1.getUsername()).setCell("email",
                            k1.getPerson().getInstitutionalEmailAddressValue());
                    return k1;
                }));

        taskLog("....");
        final String filename = "/afs/ist.utl.pt/ciist/fenix/fenix015/ist/google/email_list_02082022_145754.csv";
        String body = Unirest.get(filename).asString().getBody();
        Stream.of(body.split("\r\n")).forEach(email -> {
            User user = find(email.toLowerCase(), userMap, ldap);
            if (user == null && email.endsWith("@ist.utl.pt")) {
                user = find(email.replaceAll("ist.utl.pt", "tecnico.ulisboa.pt"), userMap, ldap);
            }
            if (user == null) {
                final Spreadsheet.Row row = spreadsheet.addRow();
                row.setCell("Email", email);
                row.setCell("User", "?");
            } else {
                final Spreadsheet.Row row = spreadsheet.addRow();
                row.setCell("Email", email);
                row.setCell("User", user.getUsername());
                row.setCell("Student", Boolean.toString(activeStudentsGroup.isMember(user)));
                row.setCell("Employee", Boolean.toString(activeEmployees.isMember(user)));
                row.setCell("Teaacher", Boolean.toString(activeTeachersGroup.isMember(user)));
                row.setCell("Researcher", Boolean.toString(activeResearchers.isMember(user)));
                row.setCell("GrantOwner", Boolean.toString(activeGrantOwner.isMember(user)));
                row.setCell("Active",
                        Boolean.toString(activeStudentsGroup.isMember(user) || activeEmployees.isMember(user)
                                || activeTeachersGroup.isMember(user) || activeResearchers.isMember(user)
                                || activeGrantOwner.isMember(user)));
                row.setCell("Alumni", Boolean.toString(alumniGroup.isMember(user)));
                final String role = activeTeachersGroup.isMember(user) ? "Teacher/Researcher" : activeResearchers
                        .isMember(user) ? "Teacher/Researcher" : activeEmployees.isMember(user) ? "Employee" : activeStudentsGroup
                                .isMember(user) ? "Student" : activeGrantOwner.isMember(user) ? "GrantOwner" : "";
                row.setCell("Role", role);
            }
        });

        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(stream);
        output("report.xlsx", stream.toByteArray());
    }

    private User find(final String email, final Map<String, User> userMap, final Map<String, User> ldap) {
        if (ldap.containsKey(email)) {
            return ldap.get(email);
        }

        final User user = userMap.get(email);
        if (user != null) {
            return user;
        }
        return Bennu.getInstance().getPartysSet().stream().flatMap(party -> party.getPartyContactsSet().stream())
                .filter(partyContact -> partyContact.isEmailAddress()).map(EmailAddress.class::cast)
                .filter(emailAddress -> emailAddress.getValue().equals(email))
                .map(emailAddress -> ((Person) emailAddress.getParty()).getUser()).findAny().orElse(null);
    }

}