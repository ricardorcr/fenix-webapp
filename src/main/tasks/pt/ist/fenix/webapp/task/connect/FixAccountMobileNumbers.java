package pt.ist.fenix.webapp.task.connect;

import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.connect.domain.Account;
import org.fenixedu.connect.domain.ConnectSystem;
import org.fenixedu.connect.domain.Identity;
import org.fenixedu.connect.domain.identification.PersonalInformation;
import pt.ist.fenixframework.FenixFramework;
import pt.ist.standards.telecommunications.DailingCode;
import pt.ist.standards.util.ResourceReader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FixAccountMobileNumbers extends ReadCustomTask {

    private static final String RESOURCE_PATH = "/telecommunications/";

    @Override
    public void runTask() throws Exception {
        final List<String> prefixes = new ArrayList<>();
        final String[] content = ResourceReader.readLines(RESOURCE_PATH + "dailingcode.txt");
        for (final String line : content) {
            final String[] ss = line.split("\t");
            final String alpha2 = ss[0];
            final String code = ss[1];
            prefixes.add(code);
        }

        Collections.sort(prefixes, (s1, s2) -> {
            final int i = Integer.compare(s1.length(), s2.length());
            return i == 0 ? s1.compareTo(s2) : i;
        });

        prefixes.forEach(s -> taskLog("%s%n", s));

        ConnectSystem.getInstance().getAccountSet().stream().parallel().forEach(a -> fix(prefixes, a));
    }

    private void fix(final List<String> prefixes, final Account account) {
        FenixFramework.atomic(() -> {
            final String mobile = account.getMobile();
            if (mobile != null) {
                final Identity identity = account.getIdentity();
                if (identity != null) {
                    final String prefix = findPrefix(prefixes, mobile);
                    if (prefix == null) {
                        taskLog("No prefix found for: %s of account: %sn", prefix, mobile);
                        throw new Error();
                    }
                    if (mobile.startsWith(prefix)) {
                        final String numberString = mobile.substring(prefix.length());
                        final long number = Long.parseLong(numberString);
                        final String fix = prefix + number;
                        if (!mobile.equals(fix)) {
                            taskLog("Fixing account: %s : %s -> %s %s%n",
                                    account.getEmail(),
                                    mobile,
                                    prefix,
                                    number);
                        }
                    }
                }
            }
        });
    }

    private String findPrefix(final List<String> prefixes, final String mobile) {
        for (final String p : prefixes) {
            if (mobile.startsWith(p)) {
                return p;
            }
        }
        return null;
    }

}