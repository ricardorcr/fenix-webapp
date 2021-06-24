package pt.ist.fenixedu;

import static org.fenixedu.bennu.core.i18n.BundleUtil.getLocalizedString;

import java.util.List;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.cms.domain.CMSTemplate;
import org.fenixedu.cms.domain.CMSTheme;
import org.fenixedu.cms.domain.Menu;
import org.fenixedu.cms.domain.MenuItem;
import org.fenixedu.cms.domain.Page;
import org.fenixedu.cms.domain.Site;
import org.fenixedu.cms.domain.component.CMSComponent;
import org.fenixedu.cms.domain.component.Component;
import org.fenixedu.cms.domain.component.ComponentDescriptor;
import org.fenixedu.commons.i18n.LocalizedString;
import org.joda.time.DateTime;

import pt.ist.fenixedu.quc.domain.TeacherInquiryTemplate;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;

/**
 * Created by diutsu on 16/09/16.
 */
public class CreateQUCPage extends CustomTask {

    public static final String BUNDLE = "resources.FenixEduQucResources";

    public static final LocalizedString QUC_TITLE = getLocalizedString(BUNDLE, "link.coordinator.QUCResults");

    @Override
    public TxMode getTxMode() {
        return TxMode.READ;
    }

    @Override
    public void runTask() throws Exception {
        ExecutionSemester oldQucExecutionSemester = ExecutionSemester.readBySemesterAndExecutionYear(2, "2019/2020");

        Bennu.getInstance().getCMSThemesSet().forEach(t -> taskLog(t.getType()));
        CMSTemplate quc_template =
                CMSTheme.forType("fenixedu-learning-theme").getTemplatesSet().stream().filter(p -> p.getType().contains("QUC"))
                        .findAny().get();

        List<Site> missingQUC =
                Bennu.getInstance().getExecutionCoursesSet().stream().filter(ec -> ec.getExecutionPeriod() != null)
                        .filter(ec -> ec.getExecutionPeriod().isAfter(oldQucExecutionSemester)).filter(ec -> ec.getSite() != null)
                        .filter(this::hasQUQResults).filter(
                        ec -> ec.getSite().getPagesSet().stream().filter(p -> p.getTemplate() != null)
                                .noneMatch(p -> p.getTemplate().getType().contains("QUC"))).map(ec -> ec.getSite())
                        .collect(Collectors.toList());

        taskLog("before: " + missingQUC.size());

        FenixFramework.atomic(
                () -> missingQUC.stream().forEach(site -> {
                    Page page = site.getPagesSet().stream().filter(p -> p.getName().equals(QUC_TITLE)).findAny()
                            .orElseGet(() -> new Page(site, QUC_TITLE));

                    if (page.getTemplate() != quc_template) {
                        page.setTemplate(quc_template);
                    }

                    addQUCComponent(page);
                    page.setPublished(true);

                    Menu menu = site.getMenusSet().stream().filter(m -> m.getPrivileged()).findAny().get();

                    if (menu.getItemsSet().stream().noneMatch(item -> item.getPage() == page)) {
                        MenuItem menuItem = new MenuItem(menu);
                        menuItem.setPage(page);
                        menuItem.setName(page.getName());
                        menuItem.setFolder(false);
                        menuItem.setUrl(null);
                        menu.putAt(menuItem, menu.getToplevelItemsSet().size());
                    }
                }));

        taskLog("after: " + Bennu.getInstance().getExecutionCoursesSet().stream().filter(ec -> ec.getExecutionPeriod() != null)
                .filter(ec -> ec.getExecutionPeriod().isAfter(oldQucExecutionSemester))
                .filter(ec -> ec.getSite() != null).filter(this::hasQUQResults).filter(
                        ec -> ec.getSite().getPagesSet().stream().filter(p -> p.getTemplate() != null)
                                .noneMatch(p -> p.getTemplate().getType().contains("QUC"))).map(ec -> ec.getSite()).count());
    }

    private void addQUCComponent(Page page) {
        String componentType = "pt.ist.fenixedu.cmscomponents.domain.executionCourse.ExecutionCourseQUCComponent";
        ComponentDescriptor descriptor = Component.forType(componentType);
        if (descriptor == null) {
            throw new IllegalArgumentException("Component '" + componentType + "' is unknown!");
        }
        if (descriptor.isStateless()) {
            @SuppressWarnings("unchecked") Class<? extends CMSComponent> type =
                    (Class<? extends CMSComponent>) descriptor.getType();
            if (page.getComponentsSet().stream().noneMatch(type::isInstance)) {
                page.addComponents(Component.forType(type));
            }
        }
    }

    private boolean hasQUQResults(ExecutionCourse ec) {
        TeacherInquiryTemplate teacherInquiryTemplate =
                TeacherInquiryTemplate.getTemplateByExecutionPeriod(ec.getExecutionPeriod());
        return !(teacherInquiryTemplate == null || teacherInquiryTemplate.getResponsePeriodBegin().plusDays(7)
                .isAfter(DateTime.now()) || ec.getInquiryResultsSet().isEmpty());
    }

}
