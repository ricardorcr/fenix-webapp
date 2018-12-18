package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.Professorship;
import org.fenixedu.academic.domain.Summary;
import org.fenixedu.academic.util.LocaleUtils;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.cms.domain.Category;
import org.fenixedu.cms.domain.Menu;
import org.fenixedu.cms.domain.Page;
import org.fenixedu.cms.domain.Site;
import org.fenixedu.cms.domain.component.ListCategoryPosts;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.learning.domain.executionCourse.ExecutionCourseSiteBuilder;
import org.fenixedu.learning.domain.executionCourse.SummaryListener;
import pt.ist.fenixframework.FenixFramework;

import java.util.Locale;

import static org.fenixedu.bennu.core.i18n.BundleUtil.getLocalizedString;

public class RegenerateSummariesAndAnnouncementsPages extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final LocalizedString SUMMARIES_TITLE = getLocalizedString(ExecutionCourseSiteBuilder.BUNDLE, "label.summaries");
        final LocalizedString ANNOUNCEMENTS_TITLE = getLocalizedString(ExecutionCourseSiteBuilder.BUNDLE, "label.announcements");

        Site site = null;
        Professorship professorship = FenixFramework.getDomainObject("1691044588555696"); //ist14182 Protecções e Automação em Sistemas de Energia
        for (Summary s : professorship.getAssociatedSummariesSet()) {
            if (s.getPost().getActive()) {
                if (site == null) {
                    site = s.getPost().getSite();

                    site.getPagesSet().stream()
                            .filter(p -> p.getName().getContent().contains("Sumários")
                                    || p.getName().getContent().contains("Anúncios"))
                            .forEach(p -> p.delete());
                    Menu menu = site.getSystemMenu();

                    Category announcementsCategory = site.getOrCreateCategoryForSlug("announcement", ANNOUNCEMENTS_TITLE);
                    ListCategoryPosts announcementsComponent = new ListCategoryPosts(announcementsCategory);
                    Page.create(site, menu, null, ANNOUNCEMENTS_TITLE, true, "category", User.findByUsername("ist24616"), announcementsComponent);

                    Category summariesCategory = site.getOrCreateCategoryForSlug("summary", SUMMARIES_TITLE);
                    ListCategoryPosts summariesComponent = new ListCategoryPosts(summariesCategory);
                    Page.create(site, menu, null, SUMMARIES_TITLE, true, "category", User.findByUsername("ist24616"), summariesComponent);
                }
                taskLog("Vou regerar o post para o sumário %s\t%s%n", s.getExternalId(), s.getTitle().getContent());
                SummaryListener.updatePost(s.getPost(), s);
            }
        }
    }
}