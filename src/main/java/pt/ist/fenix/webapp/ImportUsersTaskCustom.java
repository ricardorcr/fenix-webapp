package pt.ist.fenix.webapp;

import com.google.common.base.Objects;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.jsonwebtoken.SignatureAlgorithm;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.domain.UserProfile;
import org.fenixedu.bennu.core.domain.exceptions.BennuCoreDomainException;
import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.bennu.core.util.CoreConfiguration;
import org.fenixedu.bennu.scheduler.CronTask;
import org.fenixedu.bennu.scheduler.annotation.Task;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
//import org.fenixedu.drive.domain.FileManagementSystem;
import org.fenixedu.jwt.Tools;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import pt.ist.fenixframework.FenixFramework;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ImportUsersTaskCustom extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        final LocalDate now = new LocalDate();
        HttpResponse<String> response = Unirest.get("https://fenix.tecnico.ulisboa.pt/profiles")
                .header("Authorization", "Bearer " + jwt())
                .asString();
        final JsonArray array = new JsonParser().parse(response.getBody()).getAsJsonArray();
        final List<JsonObject> list = new ArrayList<>();
        array.forEach(e -> list.add(e.getAsJsonObject()));
        list.stream().forEach(o -> process(now, o));
    }

    private void process(final LocalDate now, final JsonObject userObject) {

        final String username = get(userObject, "username");
        final String givenNames = get(userObject, "givenNames");
        final String familyNames = get(userObject, "familyNames");
        final String displayName = get(userObject, "displayName");
        final String email = get(userObject, "email");
        final LocalDate expiration = expiration(userObject);

        if (givenNames == null || familyNames == null || displayName == null) {
            return;
        }
        User user = User.findByUsername(username);
        if ("ist1101236".equals(username)) {
            taskLog("###");
//            FenixFramework.atomic(() -> {
//                FileManagementSystem.getOrCreateFileRepository(user);
//                if (user.getFavoritesFolder() == null) {
//                    FileManagementSystem.getOrCreateFavorites(user);
//                }
//            });
        } else {
            return;
        }
        if (user == null) {
            if (expiration == null || expiration.isAfter(now)) {
                taskLog("Ia criar user e directorio %s%n", username);
//                        user = create(username, givenNames, familyNames, displayName, email);
//                        FileManagementSystem.getOrCreateFileRepository(user);
//                        if (user.getFavoritesFolder() == null) {
//                            FileManagementSystem.getOrCreateFavorites(user);
//                        }
            }
        } else {
//            taskLog("Fazer update %s%n", username);
//            update(user, givenNames, familyNames, displayName, email);
//            if (expiration == null && user.getExpiration().isPresent() && user.isLoginExpired()) {
//                user.openLoginPeriod();
//            } else if (expiration != null && expiration.isBefore(now) && !user.isLoginExpired()
//                    && (!user.getExpiration().isPresent() || user.getExpiration().get().isAfter(expiration))) {
//                final LocalDate newExpiration = expiration.isAfter(now) ? expiration : now;
//                user.closeLoginPeriod(newExpiration);
//            }
        }

    }

    private String get(final JsonObject userObject, final String field) {
        final JsonElement e = userObject.get(field);
        return e == null || e.isJsonNull() ? null : e.getAsString();
    }

    private final static DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd");

    private LocalDate expiration(final JsonObject userObject) {
        final JsonElement e = userObject.get("expiration");
        final String s = e == null || e.isJsonNull() ? null : e.getAsString();
        return s == null ? null : LocalDate.parse(s, DATE_TIME_FORMATTER);
    }

    private User create(final String username, final String givenNames, final String familyNames,
                        final String displayName, final String email) {
        return new User(username, createUserProfile(username, givenNames, familyNames, displayName, email));
    }

    private void update(final User user, final String givenNames, final String familyNames, final String displayName,
                        final String email) {
        final UserProfile userProfile = user.getProfile();
        if (userProfile == null) {
            final UserProfile createdUserProfile = createUserProfile(user.getUsername(), givenNames, familyNames,
                    displayName, email);
            user.setProfile(createdUserProfile);
        } else {
            updateProfile(userProfile, givenNames, familyNames, displayName, email);
        }
    }

    public UserProfile createUserProfile(final String username, final String givenNames, final String familyNames,
                                         final String displayName, final String email) {
        final UserProfile userProfile = new UserProfile(givenNames, familyNames, displayName, email, Locale.getDefault());
        userProfile.setAvatarUrl(getAvatarUrl(username));
        return userProfile;
    }

    public String getAvatarUrl(final String username) {
        return "https://fenix.tecnico.ulisboa.pt/user/photo/" + username;
    }

    private void updateProfile(final UserProfile userProfile, final String givenNames, final String familyNames,
                               final String displayName, final String email) {
        if (hasNameChanged(userProfile, givenNames, familyNames, displayName)) {
            try {
                userProfile.changeName(givenNames, familyNames, displayName);
            } catch (final BennuCoreDomainException ex) {
            }
        }
        if (hasEmailChanged(userProfile, email)) {
            userProfile.setEmail(email);
        }
    }

    private boolean hasNameChanged(final UserProfile profile, final String givenNames, final String familyNames,
                                   final String displayName) {
        return hasChanged(profile.getDisplayName(), displayName) || hasChanged(profile.getGivenNames(), givenNames)
                || hasChanged(profile.getFamilyNames(), familyNames);
    }

    private boolean hasEmailChanged(final UserProfile profile, final String email) {
        return profile.getEmail() != null && hasChanged(profile.getEmail(), email);
    }

    private boolean hasChanged(final String origin, final String target) {
        return !Objects.equal(origin, target);
    }

    private String jwt() {
        final JsonObject claim = new JsonObject();
        claim.addProperty("username", managerUsername());
        return Tools.sign(SignatureAlgorithm.RS256, CoreConfiguration.getConfiguration().jwtPrivateKeyPath(), claim);
    }

    private String managerUsername() {
        return Group.dynamic("system").getMembers()
                .map(u -> u.getUsername())
                .findAny().orElseThrow(() -> new Error("No managers"));
    }

}
