/**
 * Copyright © 2013 Instituto Superior Técnico
 * <p>
 * This file is part of FenixEdu IST Integration.
 * <p>
 * FenixEdu IST Integration is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * FenixEdu IST Integration is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Integration.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenix.webapp;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Locale;

import org.fenixedu.academic.domain.Attends;
import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.IEnrolment;
import org.fenixedu.academic.domain.OptionalEnrolment;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.candidacy.IngressionType;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.degree.degreeCurricularPlan.DegreeCurricularPlanState;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.academic.domain.degreeStructure.OptionalCurricularCourse;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.student.curriculum.ConclusionProcess;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationState;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationStateType;
import org.fenixedu.academic.domain.studentCurriculum.Credits;
import org.fenixedu.academic.domain.studentCurriculum.CreditsDismissal;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumGroup;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumGroupFactory;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumLine;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumModule;
import org.fenixedu.academic.domain.studentCurriculum.CycleCurriculumGroup;
import org.fenixedu.academic.domain.studentCurriculum.Dismissal;
import org.fenixedu.academic.domain.studentCurriculum.EnrolmentWrapper;
import org.fenixedu.academic.domain.studentCurriculum.Equivalence;
import org.fenixedu.academic.domain.studentCurriculum.OptionalDismissal;
import org.fenixedu.academic.domain.studentCurriculum.Substitution;
import org.fenixedu.academic.domain.studentCurriculum.TemporarySubstitution;
import org.fenixedu.academic.util.predicates.AndPredicate;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.i18n.I18N;
import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.messaging.core.domain.Message;

import org.joda.time.LocalDate;
import org.joda.time.YearMonthDay;
import pt.ist.fenix.webapp.service.EventTemplateService;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;

public class SeparateRegistrationsNew extends CustomTask {

    private static ExecutionSemester currentSemester = null;
    private static ExecutionYear currentYear = null;

    @Override
    public TxMode getTxMode() {
        return TxMode.READ;
    }

    @Override
    public void runTask() {
        currentSemester = ExecutionSemester.readActualExecutionSemester();
        currentYear = ExecutionYear.readCurrentExecutionYear();
        I18N.setLocale(new Locale("pt", "PT"));
        for (final DegreeCurricularPlan degreeCurricularPlan : getDegreeCurricularPlans()) {
            taskLog("Processing DCP: %s%n", degreeCurricularPlan.getName());

            for (final StudentCurricularPlan scp : degreeCurricularPlan.getStudentCurricularPlansSet()) {

//                if (scp.getRegistration().getNumber() == 96532 || scp.getRegistration().getNumber() == 96758) {
//                    taskLog("BUH!");
                if (canSeparate(scp)) {
                    taskLog("Separating Student: %s %s%n", scp.getRegistration().getStudent().getNumber(), scp.getRegistration().getExternalId());
                    try {
                        separateStudentProcedure(scp);

                        final Person person = scp.getPerson();
                        final boolean female = person.isFemale();
                        final String message = "Car" + (female ? "a" : "o") + " " + person.getName() +
                                "\n\nFoi agora criada a matrícula no curso de mestrado por ter aberto o ciclo externo durante a licenciatura " +
                                "que entretando foi dada como concluída. Pode agora proceder às respetivas inscrições." +
                                "\n\n" +
                                "Os melhores cumprimentos," +
                                "\nA Equipa FenixEdu";
//                    taskLog("%s%n", message);

                        Message.fromSystem()
                                .to(Group.users(person.getUser()))
                                .subject("Matrícula 2022/2023 - Separação Segundo ciclo")
                                .textBody(message)
                                .send();
                    } catch (Exception e) { //abort transaction and continue
                        taskLog("Separating students with rules %s %s%n",
                                scp.getRegistration().getStudent().getNumber(), e);
                        ByteArrayOutputStream errorOut = new ByteArrayOutputStream();
                        PrintStream error = new PrintStream(errorOut);
                        e.printStackTrace(error);
                        taskLog(new String(errorOut.toByteArray()));
                    }
                }
//                }
            }
        }
    }

    private List<DegreeCurricularPlan> getDegreeCurricularPlans() {
        return DegreeCurricularPlan.readByDegreeTypesAndState(
                DegreeType.oneOf(DegreeType::isBolonhaDegree, DegreeType::isIntegratedMasterDegree),
                DegreeCurricularPlanState.ACTIVE);
    }

    protected Registration createNewSecondCycle(final StudentCurricularPlan oldStudentCurricularPlan) {
        final Student student = oldStudentCurricularPlan.getRegistration().getStudent();
        final CycleCurriculumGroup oldSecondCycle = oldStudentCurricularPlan.getSecondCycle();
        final DegreeCurricularPlan degreeCurricularPlan = oldSecondCycle.getDegreeCurricularPlanOfDegreeModule();
        final ExecutionSemester conclusionSemester = getConclusionSemester(oldStudentCurricularPlan.getFirstCycle());

        final Registration newRegistration = createRegistration(student, oldStudentCurricularPlan, conclusionSemester);
        final StudentCurricularPlan newStudentCurricularPlan =
                createStudentCurricularPlan(newRegistration, degreeCurricularPlan, oldSecondCycle.getCycleType());
        final CycleCurriculumGroup newSecondCycle = newStudentCurricularPlan.getSecondCycle();
        final ExecutionSemester newRegistrationSemester = conclusionSemester.getNextExecutionPeriod();

        copyCycleCurriculumGroupsInformation(oldSecondCycle, newSecondCycle, newRegistrationSemester);
        moveAttends(oldStudentCurricularPlan, newStudentCurricularPlan);
        tryRemoveOldSecondCycle(oldSecondCycle);

        if (oldStudentCurricularPlan.getDegreeCurricularPlan().getDegreeType().isIntegratedMasterDegree()) {
            markOldRegistrationWithState(oldStudentCurricularPlan, RegistrationStateType.EXTERNAL_ABANDON, newRegistrationSemester);
        } else {
            markOldRegistrationWithState(oldStudentCurricularPlan, RegistrationStateType.CONCLUDED, newRegistrationSemester);
        }

        newRegistration.updateEnrolmentDate(newRegistrationSemester.getExecutionYear());
        EventTemplateService.initEventTemplate(newRegistration, newRegistrationSemester.getExecutionYear());
        return newRegistration;
    }

    private void moveAttends(final StudentCurricularPlan oldStudentCurricularPlan,
                             final StudentCurricularPlan newStudentCurricularPlan) {
        oldStudentCurricularPlan.getRegistration().getAssociatedAttendsSet().stream()
                .filter(attend -> !belongsTo(oldStudentCurricularPlan, attend))
                .filter(attend -> isToMoveAttendsFrom(oldStudentCurricularPlan, newStudentCurricularPlan, attend))
                .filter(attend -> !newStudentCurricularPlan.getRegistration().attends(attend.getExecutionCourse()))
                .forEach(attend -> attend.setRegistration(newStudentCurricularPlan.getRegistration()));
    }

    private boolean belongsTo(final StudentCurricularPlan studentCurricularPlan, final Attends attend) {
        return attend.getExecutionCourse().getAssociatedCurricularCoursesSet().stream()
                .anyMatch(curricularCourse -> studentCurricularPlan.getDegreeCurricularPlan().hasDegreeModule(curricularCourse));
    }

    private boolean isToMoveAttendsFrom(final StudentCurricularPlan oldStudentCurricularPlan,
                                        final StudentCurricularPlan newStudentCurricularPlan, final Attends attend) {

        if (attend.getEnrolment() != null) {
            return !oldStudentCurricularPlan.hasEnrolments(attend.getEnrolment())
                    && newStudentCurricularPlan.hasEnrolments(attend.getEnrolment());
        }
        return !attend.getExecutionPeriod().isBefore(newStudentCurricularPlan.getStartExecutionPeriod());
    }

    private void moveEnrolment(final Enrolment enrolment, final CurriculumGroup parent) {
        final CurriculumModule child = parent.getChildCurriculumModule(enrolment.getDegreeModule());
        if (child != null && child.isEnrolment()) {
            final Enrolment childEnrolment = (Enrolment) child;
            if (childEnrolment.getExecutionPeriod() == enrolment.getExecutionPeriod()) {
                throw new DomainException("error.SeparationCyclesManagement.enrolment.should.not.exist.for.same.executionPeriod");
            }
        }

        final Registration registration = parent.getStudentCurricularPlan().getRegistration();
        enrolment.setCurriculumGroup(parent);

        for (final Attends attend : enrolment.getAttendsSet()) {
            if (!registration.attends(attend.getExecutionCourse())) {
                attend.setRegistration(registration);
            }
        }
    }

    private Registration createRegistration(final Student student, final StudentCurricularPlan sourceStudentCurricularPlan,
                                            final ExecutionSemester conclusionSemester) {

        final CycleCurriculumGroup oldSecondCycle = sourceStudentCurricularPlan.getSecondCycle();
        Registration registration = student.getActiveRegistrationFor(oldSecondCycle.getDegreeCurricularPlanOfDegreeModule());

        if (registration != null) {
            return registration;
        }

        final ExecutionSemester periodAfterConclusion = conclusionSemester.getNextExecutionPeriod();
        final ExecutionYear newRegistrationYear = periodAfterConclusion.getExecutionYear();
        Degree degree = oldSecondCycle.getDegreeCurricularPlanOfDegreeModule().getDegree();
        registration = new Registration(student.getPerson(), student.getNumber(), degree);
        registration.setStartDate(periodAfterConclusion.getBeginDateYearMonthDay());
        RegistrationState activeState = registration.getActiveState();
        activeState.setStateDate(periodAfterConclusion.getBeginDateYearMonthDay());
        activeState.setResponsiblePerson(null);
        registration.setSourceRegistration(sourceStudentCurricularPlan.getRegistration());
        registration.setRegistrationProtocol(sourceStudentCurricularPlan.getRegistration().getRegistrationProtocol());
        registration.setRegistrationYear(newRegistrationYear);

        return registration;
    }

    private ExecutionSemester getConclusionSemester(final CycleCurriculumGroup firstCycle) {
        return firstCycle.getApprovedCurriculumLines().stream()
                .map(cl -> cl.getExecutionPeriod())
                .max(ExecutionSemester.COMPARATOR_BY_BEGIN_DATE)
                .orElseGet(() -> null);
    }

    private YearMonthDay getBeginDate(final StudentCurricularPlan sourceStudentCurricularPlan,
                                      final ExecutionSemester executionSemester) {

        if (!sourceStudentCurricularPlan.getFirstCycle().isConcluded()) {
            throw new DomainException("error.SeparationCyclesManagement.source.studentCurricularPlan.is.not.concluded");
        }

        final YearMonthDay conclusionDate = sourceStudentCurricularPlan.getFirstCycle().calculateConclusionDate();
        final YearMonthDay stateDate = conclusionDate != null ? conclusionDate.plusDays(1) : new YearMonthDay().plusDays(1);

        return executionSemester.getBeginDateYearMonthDay().isBefore(stateDate) ? stateDate : executionSemester
                .getBeginDateYearMonthDay();
    }

    private StudentCurricularPlan createStudentCurricularPlan(final Registration registration,
                                                              final DegreeCurricularPlan degreeCurricularPlan, CycleType cycleType) {

        StudentCurricularPlan result = registration.getStudentCurricularPlan(degreeCurricularPlan);
        if (result != null) {
            return result;
        }

        result =
                StudentCurricularPlan.createWithEmptyStructure(registration, degreeCurricularPlan, cycleType,
                        registration.getStartDate());

        // set ingression after create studentcurricularPlan
        registration.setIngressionType(IngressionType.findByPredicate(IngressionType::isDirectAccessFrom1stCycle).orElse(null));

        return result;
    }

    private void copyCycleCurriculumGroupsInformation(final CycleCurriculumGroup oldSecondCycle,
                                                      final CycleCurriculumGroup newSecondCycle,
                                                      final ExecutionSemester executionSemester) {
        for (final CurriculumModule curriculumModule : oldSecondCycle.getCurriculumModulesSet()) {
            if (curriculumModule.isLeaf()) {
                copyCurriculumLineInformation((CurriculumLine) curriculumModule, newSecondCycle, executionSemester);
            } else {
                copyCurriculumGroupsInformation((CurriculumGroup) curriculumModule, newSecondCycle, executionSemester);
            }
        }
    }

    private void copyCurriculumGroupsInformation(final CurriculumGroup source, final CurriculumGroup parent, final ExecutionSemester executionSemester) {
        final CurriculumGroup destination;
        //test if source group still exists as part of destination DCP
        if (!groupIsStillValid(source, executionSemester)) {
            return;
        }
        if (source.getName().getContent().equals("Minor")) {
            source.setCurriculumGroup(parent);
            return;
        }
        if (parent.hasChildDegreeModule(source.getDegreeModule())) {
            destination = (CurriculumGroup) parent.getChildCurriculumModule(source.getDegreeModule());
        } else {
            destination = CurriculumGroupFactory.createGroup(parent, source.getDegreeModule());
        }

        for (final CurriculumModule curriculumModule : source.getCurriculumModulesSet()) {
            if (curriculumModule.isLeaf()) {
                copyCurriculumLineInformation((CurriculumLine) curriculumModule, destination, executionSemester);
            } else {
                copyCurriculumGroupsInformation((CurriculumGroup) curriculumModule, destination, executionSemester);
            }
        }
    }

    private boolean groupIsStillValid(final CurriculumGroup source, final ExecutionSemester executionSemester) {
        if (source.getDegreeModule().getValidChildContexts(executionSemester).size() > 0) {
            return true;
        }
        return source.getChildCurriculumGroups().stream().anyMatch(source1 -> groupIsStillValid(source1, executionSemester));
    }

    private void copyCurriculumLineInformation(final CurriculumLine curriculumLine, final CurriculumGroup parent, final ExecutionSemester executionSemester) {
        if (curriculumLine.isEnrolment()) {
            final Enrolment enrolment = (Enrolment) curriculumLine;
            if (enrolment.isApproved()) {
                createSubstitutionForEnrolment((Enrolment) curriculumLine, parent, executionSemester);
            } else if ((enrolment.getExecutionPeriod() == currentSemester || enrolment.getExecutionPeriod() == currentSemester.getNextExecutionPeriod())
                    && enrolment.isActive()) { //they can enroll for both semester in the year
                moveEnrolment((Enrolment) curriculumLine, parent);
            }
        } else if (curriculumLine.isDismissal()) {
            createDismissal((Dismissal) curriculumLine, parent, executionSemester);
        } else {
            throw new DomainException("error.unknown.curriculumLine");
        }
    }

    private void tryRemoveOldSecondCycle(final CycleCurriculumGroup oldSecondCycle) {
        if (canRemoveOldSecondCycle(oldSecondCycle)) {
            deleteCurriculumModules(oldSecondCycle);
        }
    }

    protected void deleteCurriculumModules(final CurriculumModule curriculumModule) {
        if (curriculumModule == null) {
            return;
        }
        if (!curriculumModule.isLeaf()) {
            final CurriculumGroup curriculumGroup = (CurriculumGroup) curriculumModule;
            for (; !curriculumGroup.getCurriculumModulesSet().isEmpty();) {
                deleteCurriculumModules(curriculumGroup.getCurriculumModulesSet().iterator().next());
            }
            curriculumGroup.delete();
        } else if (curriculumModule.isDismissal()) {
            curriculumModule.delete();
        } else {
            throw new DomainException("error.can.only.remove.groups.and.dismissals");
        }
    }

    private boolean canRemoveOldSecondCycle(final CycleCurriculumGroup oldSecondCycle) {
        for (final CurriculumLine curriculumLine : oldSecondCycle.getAllCurriculumLines()) {
            if (curriculumLine.isEnrolment() || curriculumLine.isDismissal()) {
                return false;
            } else if (!curriculumLine.isDismissal()) {
                throw new DomainException("error.unknown.curriculum.line");
            }
        }
        return true;
    }

    private void createSubstitutionForEnrolment(final Enrolment enrolment, final CurriculumGroup parent, final ExecutionSemester executionSemester) {
        if (enrolment.getUsedInSeparationCycle() || parent.hasChildDegreeModule(enrolment.getDegreeModule())) {
            // TODO: temporary
            enrolment.setUsedInSeparationCycle(true);
            return;
        }

        enrolment.setUsedInSeparationCycle(true);

        if (enrolment.isOptional()) {
            final OptionalEnrolment optional = (OptionalEnrolment) enrolment;
            if (parent.hasChildDegreeModule(optional.getOptionalCurricularCourse())) {
                return;
            }
            final Substitution substitution = createSubstitution(enrolment, parent, executionSemester);
            createNewOptionalDismissal(substitution, parent, enrolment, optional.getOptionalCurricularCourse(),
                    optional.getEctsCredits());
        } else {
            createNewDismissal(createSubstitution(enrolment, parent, executionSemester), parent, enrolment);
        }
    }

    private Substitution createSubstitution(final Enrolment enrolment, final CurriculumGroup parent, final ExecutionSemester executionSemester) {
        final Substitution substitution = new Substitution();
        substitution.setStudentCurricularPlan(parent.getStudentCurricularPlan());
        substitution.setExecutionPeriod(executionSemester);
        EnrolmentWrapper.create(substitution, enrolment);
        return substitution;
    }

    private Dismissal createNewDismissal(final Credits credits, final CurriculumGroup parent, final CurriculumLine curriculumLine) {

        final CurricularCourse curricularCourse = curriculumLine.getCurricularCourse();

        if (!hasCurricularCourseToDismissal(parent, curricularCourse) && !hasResponsibleForCreation(curriculumLine)) {
            throw new DomainException("error.SeparationCyclesManagement.parent.doesnot.have.curricularCourse.to.dismissal");
        }

        final Dismissal dismissal = new Dismissal();
        dismissal.setCredits(credits);
        dismissal.setCurriculumGroup(parent);
        dismissal.setCurricularCourse(curricularCourse);

        return dismissal;
    }

    private OptionalDismissal createNewOptionalDismissal(final Credits credits, final CurriculumGroup parent,
                                                         final CurriculumLine curriculumLine, final OptionalCurricularCourse curricularCourse, final Double ectsCredits) {

        if (ectsCredits == null || ectsCredits == 0) {
            throw new DomainException("error.OptionalDismissal.invalid.credits");
        }

        if (!hasCurricularCourseToDismissal(parent, curricularCourse) && !hasResponsibleForCreation(curriculumLine)) {
            throw new DomainException("error.SeparationCyclesManagement.parent.doesnot.have.curricularCourse.to.dismissal");
        }

        final OptionalDismissal dismissal = new OptionalDismissal();
        dismissal.setCredits(credits);
        dismissal.setCurriculumGroup(parent);
        dismissal.setCurricularCourse(curricularCourse);
        dismissal.setEctsCredits(ectsCredits);

        return dismissal;
    }

    private boolean hasResponsibleForCreation(final CurriculumLine line) {
        return line.hasCreatedBy();
    }

    private boolean hasCurricularCourseToDismissal(final CurriculumGroup curriculumGroup, final CurricularCourse curricularCourse) {
        final CourseGroup degreeModule = curriculumGroup.getDegreeModule();
        return degreeModule.getChildContexts(CurricularCourse.class).stream()
                .map(context -> (CurricularCourse) context.getChildDegreeModule())
                .anyMatch(each -> each.isEquivalent(curricularCourse) && !curriculumGroup.hasChildDegreeModule(degreeModule));
    }

    private void createDismissal(final Dismissal dismissal, final CurriculumGroup parent, final ExecutionSemester executionSemester) {
        if (dismissal.getUsedInSeparationCycle() || curriculumGroupHasSimilarDismissal(parent, dismissal)) {
            // TODO: temporary
            dismissal.setUsedInSeparationCycle(true);
            return;
        }

        dismissal.setUsedInSeparationCycle(true);
        final Credits credits = dismissal.getCredits();

        final Credits newCredits;
        if (credits.isTemporary()) {
            newCredits = new TemporarySubstitution();

        } else if (credits.isSubstitution()) {
            newCredits = new Substitution();

        } else if (credits.isEquivalence()) {
            final Equivalence equivalence = (Equivalence) credits;
            final Equivalence newEquivalence = new Equivalence();
            newEquivalence.setGrade(equivalence.getGrade());
            newCredits = newEquivalence;

        } else {
            newCredits = new Credits();
        }

        newCredits.setStudentCurricularPlan(parent.getStudentCurricularPlan());
        newCredits.setExecutionPeriod(executionSemester);
        newCredits.setGivenCredits(credits.getGivenCredits());

        for (final IEnrolment enrolment : credits.getIEnrolments()) {
            EnrolmentWrapper.create(newCredits, enrolment);
        }

        if (dismissal.hasCurricularCourse()) {
            if (dismissal instanceof OptionalDismissal) {
                final OptionalDismissal optionalDismissal = (OptionalDismissal) dismissal;
                createNewOptionalDismissal(newCredits, parent, dismissal, optionalDismissal.getCurricularCourse(),
                        optionalDismissal.getEctsCredits());

            } else {
                createNewDismissal(newCredits, parent, dismissal);
            }
        } else if (dismissal.isCreditsDismissal()) {
            final CreditsDismissal creditsDismissal = (CreditsDismissal) dismissal;
            new CreditsDismissal(newCredits, parent, creditsDismissal.getNoEnrolCurricularCoursesSet());
        } else {
            throw new DomainException("error.unknown.dismissal.type");
        }

        dismissal.delete();
    }

    private boolean curriculumGroupHasSimilarDismissal(final CurriculumGroup curriculumGroup, final Dismissal dismissal) {
        return curriculumGroup.getChildDismissals().stream().anyMatch(each -> each.isSimilar(dismissal));
    }

    private void markOldRegistrationWithState(final StudentCurricularPlan oldStudentCurricularPlan, RegistrationStateType stateType,
                                              final ExecutionSemester executionSemester) {
        if (oldStudentCurricularPlan.getRegistration().hasState(stateType)) {
            return;
        }

        LocalDate stateDate = new LocalDate();
        if (stateDate.isAfter(executionSemester.getEndDateYearMonthDay())) {
            stateDate = executionSemester.getEndDateYearMonthDay().toLocalDate();
        }

        final RegistrationState state =
                RegistrationState.createRegistrationState(oldStudentCurricularPlan.getRegistration(), null,
                        stateDate.toDateTimeAtStartOfDay(), stateType);
        state.setResponsiblePerson(null);
    }

    private boolean studentAlreadyHasNewRegistration(final StudentCurricularPlan studentCurricularPlan) {
        final Student student = studentCurricularPlan.getRegistration().getStudent();
        return student.hasRegistrationFor(studentCurricularPlan.getSecondCycle().getDegreeCurricularPlanOfDegreeModule().getDegree());
    }

    private boolean canSeparate(final StudentCurricularPlan scp) {
        final RegistrationState lastState = scp.getRegistration().getLastState();
        if (lastState.isActive() || lastState.getStateType() == RegistrationStateType.CONCLUDED) {
            CycleCurriculumGroup firstCycle = scp.getFirstCycle();
            ConclusionProcess conclusionProcess = firstCycle != null ? firstCycle.getConclusionProcess() : null;

//            taskLog("Ciclo concluido: %s%n", hasFirstCycleConcluded(firstCycle));
            if (hasFirstCycleConcluded(firstCycle)) {
                final ExecutionSemester conclusionSemester = getConclusionSemester(firstCycle);
                if (conclusionSemester != null) {
//                taskLog("Semestre conclusão: %s%n", conclusionSemester.getQualifiedName());
                    final ExecutionYear conclusionYear = conclusionSemester.getExecutionYear();
                    final ExecutionYear newRegistrationYear = conclusionSemester.getNextExecutionPeriod().getExecutionYear();
                    if (conclusionYear.isCurrent() || currentYear.getPreviousExecutionYear() == conclusionYear) {
//                    taskLog("valid scp: %s%n", hasValidExternalSecondCycle(scp, newRegistrationYear));
//                    taskLog("does not already has: %s%n", !studentAlreadyHasNewRegistration(scp));
//                    taskLog("does not have another 2nd cycle: %s%n", !studentHasOtherSecondCyleRegistration(scp));
//                    taskLog("last one: %s%n", (scp.isActive() || (conclusionProcess != null && conclusionProcess.isActive())));
                        return hasValidExternalSecondCycle(scp, newRegistrationYear)
                                && !studentAlreadyHasNewRegistration(scp)
                                && !studentHasOtherSecondCyleRegistration(scp)
                                && (scp.isActive() || (conclusionProcess != null && conclusionProcess.isActive()));
                    }
                }
            }
        }
        return false;
    }

    private boolean studentHasOtherSecondCyleRegistration(final StudentCurricularPlan scp) {
        return scp.getRegistration().getStudent().getRegistrationsSet().stream()
                .anyMatch(r -> r.isActive() && r.getDegree().isSecondCycle());
    }

    private boolean hasFirstCycleConcluded(final CycleCurriculumGroup firstCycle) {
        return firstCycle != null && firstCycle.isConcluded();
    }

    private boolean hasValidExternalSecondCycle(final StudentCurricularPlan studentCurricularPlan, final ExecutionYear newRegistrationYear) {
        final CycleCurriculumGroup secondCycle = studentCurricularPlan.getSecondCycle();
        return secondCycle != null && secondCycle.isExternal()
                //TODO test without checking enrolments!!
//                && (secondCycle.hasEnrolment(ExecutionSemester.readActualExecutionSemester())
//                    || hasDismissal(ExecutionSemester.readActualExecutionSemester(), secondCycle))
                && hasActiveExecutionDegree(secondCycle, newRegistrationYear);
    }

    private boolean hasActiveExecutionDegree(final CycleCurriculumGroup secondCycle, final ExecutionYear newRegistrationYear) {
        return !secondCycle.getDegreeModule().getDegree()
                .getExecutionDegreesForExecutionYear(newRegistrationYear).isEmpty();
    }

    public boolean hasDismissal(final ExecutionSemester executionSemester, final CurriculumGroup group) {
        final AndPredicate<CurriculumModule> andPredicate = new AndPredicate<CurriculumModule>();
        andPredicate.add(new CurriculumModule.CurriculumModulePredicateByType(Dismissal.class));
        andPredicate.add(new CurriculumModule.CurriculumModulePredicateByExecutionSemester(executionSemester));

        return group.hasAnyCurriculumModules(andPredicate);
    }

    @Atomic(mode = TxMode.WRITE)
    private void separateStudentProcedure(StudentCurricularPlan studentCurricularPlan) {
        FenixFramework.atomic(() -> {
            createNewSecondCycle(studentCurricularPlan);
        });
    }
}