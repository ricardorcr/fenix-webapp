package pt.ist.fenix.webapp;

import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.signals.DomainObjectEvent;
import org.fenixedu.bennu.core.signals.Signal;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.connect.domain.Account;
import org.fenixedu.connect.domain.AccountNameIndex;
import org.fenixedu.connect.domain.DgesCnasCode;
import org.fenixedu.connect.domain.Identity;
import org.fenixedu.connect.domain.identification.PersonalInformation;
import org.fenixedu.connect.domain.identification.TaxInformation;
import org.fenixedu.connect.service.IdentityValidationService;
import pt.ist.fenixframework.FenixFramework;

import java.util.Objects;

public class Teste extends CustomTask {

    public static final String SIGNAL_IDENTITY_VALIDATED = "identity.validated";

    @Override
    public void runTask() throws Exception {
//        final User user = User.findByUsername("ist1104913");
//        Identity identity = user.getIdentity();
//        PersonalInformation personalInformation = identity == null ? null : identity.getPersonalInformation();
//        TaxInformation taxInformation = personalInformation == null ? null : personalInformation.getTaxInformation();
//        taskLog("Tax info: %s%n", taxInformation != null);
//        if (taxInformation != null) {
//            taskLog("Is it valid or not?? %s%n", !taxInformation.isValid());
//        }

        final Account account = FenixFramework.getDomainObject("1697512809876489");
        final Identity identity = Identity.getOrCreateIdentity(account);
//        AccountNameIndex.updateAccountNameIndex(account);
//        final Identity result = DgesCnasCode.cleanUpDgesCnasAccount(doAfterVerification(identity));
//        Signal.emit(SIGNAL_IDENTITY_VALIDATED, new DomainObjectEvent<>(result));
//
        final PersonalInformation pi = FenixFramework.getDomainObject("290086386142714");
        identity.setPersonalInformation(pi);
    }

    private static Identity doAfterVerification(final Identity identity) {
        identity.autoMerge();
        clearUnverifiedPersonalInformation(identity);
        return identity;
    }

    private static void clearUnverifiedPersonalInformation(final Identity identity) {
        identity.getAccountSet().stream()
                .map(Account::getPersonalInformation)
                .filter(Objects::nonNull)
                .forEach(PersonalInformation::delete);
    }
}