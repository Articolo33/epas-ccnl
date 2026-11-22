/*
 * Copyright (C) 2026  contributori di epas-ccnl
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package manager.ccnl;

import com.google.common.collect.Lists;
import java.util.List;
import manager.ccnl.QuadrimesterBalance.MonthData;
import manager.ccnl.QuadrimesterBalance.Outcome;
import manager.ccnl.QuadrimesterBalance.Period;
import models.Person;
import models.Qualification;
import models.flows.enumerate.AbsenceRequestType;
import org.junit.Test;
import play.test.UnitTest;

/**
 * epas-ccnl: test del calcolo per quadrimestre (art. 58 c. 1 e c. 5 CCNL 21/02/2002).
 */
public class QuadrimesterBalanceTest extends UnitTest {

  @Test
  public void quadrimestersAreSolarFourMonthPeriods() {
    assertEquals(1, CcnlRules.periodStartMonth(1));
    assertEquals(1, CcnlRules.periodStartMonth(4));
    assertEquals(5, CcnlRules.periodStartMonth(5));
    assertEquals(5, CcnlRules.periodStartMonth(8));
    assertEquals(9, CcnlRules.periodStartMonth(9));
    assertEquals(9, CcnlRules.periodStartMonth(12));
    assertEquals(1, CcnlRules.periodIndex(3));
    assertEquals(2, CcnlRules.periodIndex(6));
    assertEquals(3, CcnlRules.periodIndex(11));
  }

  @Test
  public void contractualConstants() {
    assertEquals(22, CcnlRules.MAX_COMPENSATORY_DAYS_PER_YEAR);
    assertEquals(1200, CcnlRules.MAX_DEFICIT_MINUTES);
    assertEquals(360, CcnlRules.MEAL_TICKET_MINUTES);
    assertEquals(2160, CcnlRules.WEEKLY_MINUTES);
  }

  @Test
  public void rulesApplyOnlyToLevelsOneToThree() {
    assertTrue(CcnlRules.enabled());
    assertTrue(CcnlRules.appliesTo(personOfLevel(1)));
    assertTrue(CcnlRules.appliesTo(personOfLevel(3)));
    assertFalse(CcnlRules.appliesTo(personOfLevel(4)));
    assertFalse(CcnlRules.appliesTo(new Person()));
    assertFalse(CcnlRules.appliesTo(null));
    assertTrue(CcnlRules.appliesToQualification(2));
    assertFalse(CcnlRules.appliesToQualification(5));
  }

  @Test
  public void researcherRequestsAreCommunications() {
    Person researcher = personOfLevel(2);
    Person technician = personOfLevel(6);
    assertTrue(CcnlRules.isSelfCertifiedRequest(researcher, AbsenceRequestType.VACATION_REQUEST));
    assertTrue(CcnlRules.isSelfCertifiedRequest(researcher, AbsenceRequestType.COMPENSATORY_REST));
    assertTrue(
        CcnlRules.isSelfCertifiedRequest(researcher, AbsenceRequestType.PERSONAL_PERMISSION));
    assertFalse(
        CcnlRules.isSelfCertifiedRequest(technician, AbsenceRequestType.VACATION_REQUEST));
    assertFalse(CcnlRules.isSelfCertifiedRequest(researcher, null));
  }

  @Test
  public void completedYearOutcomes() {
    List<MonthData> months = Lists.newArrayList(
        new MonthData(1, 0, -300, 9000, 0),
        new MonthData(2, -300, -700, 9000, 0),
        new MonthData(3, -700, -1000, 9000, 1),
        new MonthData(4, -1000, -1500, 9000, 0),
        new MonthData(5, -1500, -1000, 9000, 0),
        new MonthData(6, -1000, -800, 9000, 2),
        new MonthData(7, -800, -700, 9000, 0),
        new MonthData(8, -700, -600, 9000, 0),
        new MonthData(9, -600, -100, 9000, 0),
        new MonthData(10, -100, 0, 9000, 0),
        new MonthData(11, 0, 50, 9000, 0),
        new MonthData(12, 50, 100, 9000, 3));

    List<Period> periods = QuadrimesterBalance.compute(2025, months, 2026, 11);
    assertEquals(3, periods.size());

    Period first = periods.get(0);
    assertTrue(first.isCompleted());
    assertEquals(Outcome.DEFICIT_OVER_LIMIT, first.getOutcome());
    assertEquals(Integer.valueOf(-1500), first.getFinalMinutes());
    assertEquals(Integer.valueOf(-1500), first.getBalanceMinutes());
    assertEquals(300, first.getToRecoverMinutes());
    assertEquals(36000, first.getWorkedMinutes());
    assertEquals(1, first.getCompensatoryDays());

    Period second = periods.get(1);
    assertEquals(Outcome.DEFICIT_WITHIN_LIMIT, second.getOutcome());
    assertEquals(Integer.valueOf(900), second.getBalanceMinutes());
    assertEquals(0, second.getToRecoverMinutes());

    Period third = periods.get(2);
    assertEquals(Outcome.EVEN_OR_SURPLUS, third.getOutcome());
    assertEquals(Integer.valueOf(100), third.getFinalMinutes());

    assertEquals(6, QuadrimesterBalance.compensatoryDaysUsed(periods));
  }

  @Test
  public void currentPeriodIsInProgress() {
    List<MonthData> months = Lists.newArrayList(
        new MonthData(9, 0, -2000, 8000, 0),
        new MonthData(10, -2000, -2500, 8000, 0));
    List<Period> periods = QuadrimesterBalance.compute(2026, months, 2026, 11);
    assertEquals(Outcome.NO_DATA, periods.get(0).getOutcome());
    assertEquals(Outcome.NO_DATA, periods.get(1).getOutcome());
    Period third = periods.get(2);
    assertFalse(third.isCompleted());
    assertEquals(Outcome.IN_PROGRESS, third.getOutcome());
    assertEquals(0, third.getToRecoverMinutes());
    assertEquals(Integer.valueOf(-2500), third.getFinalMinutes());
  }

  @Test
  public void periodWithoutLastMonthIsNotCompleted() {
    // Contratto terminato a marzo: il quadrimestre non arriva ad aprile.
    List<MonthData> months = Lists.newArrayList(
        new MonthData(1, 0, -2000, 8000, 0),
        new MonthData(3, -2000, -2400, 8000, 0));
    List<Period> periods = QuadrimesterBalance.compute(2025, months, 2026, 11);
    assertEquals(Outcome.IN_PROGRESS, periods.get(0).getOutcome());
    assertEquals(Integer.valueOf(-2400), periods.get(0).getBalanceMinutes());
  }

  private static Person personOfLevel(int level) {
    Qualification qualification = new Qualification();
    qualification.setQualification(level);
    Person person = new Person();
    person.setQualification(qualification);
    return person;
  }
}
