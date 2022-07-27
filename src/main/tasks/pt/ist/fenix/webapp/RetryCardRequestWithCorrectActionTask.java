package pt.ist.fenix.webapp;

import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.custom.WriteCustomTask;
import org.fenixedu.bennu.spring.BennuSpringContextHelper;
import org.fenixedu.idcards.domain.SantanderCardState;
import org.fenixedu.idcards.domain.SantanderEntry;
import org.fenixedu.idcards.domain.SantanderUser;
import org.fenixedu.idcards.exception.SantanderCardNoPermissionException;
import org.fenixedu.idcards.service.IUserInfoService;
import org.fenixedu.idcards.service.SantanderIdCardsService;
import org.fenixedu.santandersdk.dto.CardPreviewBean;
import org.fenixedu.santandersdk.dto.CreateRegisterRequest;
import org.fenixedu.santandersdk.dto.CreateRegisterResponse;
import org.fenixedu.santandersdk.dto.RegisterAction;
import org.fenixedu.santandersdk.exception.SantanderMissingInformationException;
import org.fenixedu.santandersdk.exception.SantanderValidationException;
import org.fenixedu.santandersdk.service.SantanderSdkService;
import pt.ist.fenixframework.Atomic;

import java.util.Base64;

/**
 * @author Tiago Pinho
 */
public class RetryCardRequestWithCorrectActionTask extends WriteCustomTask {

    private static final SantanderSdkService sdkService = BennuSpringContextHelper.getBean(SantanderSdkService.class);
    private static final IUserInfoService userInfoService = BennuSpringContextHelper.getBean(IUserInfoService.class);
    private static final SantanderIdCardsService santanderIdCardsService = BennuSpringContextHelper.getBean(SantanderIdCardsService.class);

    @Override
    public void runTask() throws Exception {
        // Retry card request with correct action for user "ist155393" -> "NOVO"
        retryCardRequest(User.findByUsername("ist155393"), RegisterAction.NOVO);
    }

    @Atomic
    private void retryCardRequest(final User user, final RegisterAction registerAction) {
        if (user != null) {
            final SantanderEntry entry = user.getCurrentSantanderEntry();

            if (entry != null) {
                final SantanderCardState santanderCardState = entry.getSantanderCardInfo().getCurrentState();
                taskLog("User %s with current state = %s%n", user.getUsername(), santanderCardState);

                try {
                    final SantanderUser santanderUser = new SantanderUser(user, userInfoService);
                    final CreateRegisterRequest createRegisterRequest = santanderUser.toCreateRegisterRequest(registerAction);
                    final CardPreviewBean cardPreviewBean = sdkService.generateCardRequest(createRegisterRequest);
                    final CreateRegisterResponse response = sdkService.createRegister(cardPreviewBean);

                    taskLog("Card preview: %nName: %s%nPhoto: %s%nRole: %s%nCampus: %s%nPickup location: %s%n%n",
                            cardPreviewBean.getCardName(), Base64.getEncoder().encodeToString(cardPreviewBean.getPhoto()),
                            cardPreviewBean.getRole(), santanderUser.getCampus(),
                            santanderUser.getUserPickupLocation().getPickupLocation());

                    if (response.wasRegisterSuccessful()) {
                        try {
                            entry.reset(cardPreviewBean, santanderUser.getUserPickupLocation(),
                                    "Automatic task to fix previous request action.");
                            entry.saveResponse(response);
                        } catch (final SantanderCardNoPermissionException e) {
                            taskLog("User %s has no permission to request card.%n", user.getUsername());
                        }
                        taskLog("Request for user %s was successful! Response: %s%n", user.getUsername(), response.getResponseLine());
                    } else {
                        taskLog("Request for user %s failed! Error: %s%n", user.getUsername(), response.getErrorDescription());
                    }

                } catch (final SantanderMissingInformationException smie) {
                    taskLog("User %s has missing information: %s%n", user.getUsername(),
                            santanderIdCardsService.getErrorMessage(user.getProfile().getPreferredLocale(), smie.getMessage()));
                } catch (final SantanderValidationException sve) {
                    taskLog("Validation exception for user %s: %s%n", user.getUsername(), sve.getMessage());
                }

            } else {
                taskLog("Error: Could not find santander entry for user %s", user.getUsername());
            }
        }
    }

}