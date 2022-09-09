package pt.ist.fenix.webapp.task.connect;

import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.connect.domain.Account;
import org.fenixedu.connect.domain.ConnectSystem;
import org.fenixedu.connect.domain.Identity;
import org.fenixedu.connect.domain.identification.PersonalInformation;
import pt.ist.standards.telecommunications.DailingCode;

public class FixAccountMobileNumbers extends ReadCustomTask {
    @Override
    public void runTask() throws Exception {
        ConnectSystem.getInstance().getAccountSet().stream().parallel().forEach(this::fix);
    }

    private void fix(final Account account) {
        final String mobile = account.getMobile();
        if (mobile != null) {
            final Identity identity = account.getIdentity();
            if (identity != null) {
                final PersonalInformation personalInformation = identity.getPersonalInformation();
                if (personalInformation != null) {
                    final String countryCode = personalInformation.getNationalityCountryCode();
                    if (countryCode != null) {
                        final String prefix = DailingCode.dailingPrefixFor(countryCode);
                        if (mobile.startsWith(prefix)) {
                            final String numberString = mobile.substring(prefix.length());
                            final int number = Integer.parseInt(numberString);
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
            }
        }
    }

}