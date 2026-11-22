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

package persondays;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import common.injection.StaticInject;
import java.util.List;
import manager.PersonDayManager;
import models.Person;
import models.PersonDay;
import models.Qualification;
import models.Stamping;
import models.Stamping.WayType;
import models.WorkingTimeTypeDay;
import org.junit.Test;
import play.test.UnitTest;

/**
 * epas-ccnl: buono pasto dei livelli I-III con la soglia dell'art. 5 c. 2 CCNL 21/02/2002.
 */
@StaticInject
public class CcnlMealTicketTest extends UnitTest {

  @Inject
  private static PersonDayManager personDayManager;

  /**
   * Una sede con un orario che fissa la soglia del buono a 7 ore: il ricercatore che lavora
   * 6:30 con mezz'ora di pausa matura il buono (soglia contrattuale di 6 ore), il tecnico no.
   */
  @Test
  public void researcherUsesContractualThreshold() {
    assertTrue(ticketFor(3, 420, 9, 0, 12, 30, 13, 0, 16, 0));
    assertFalse(ticketFor(4, 420, 9, 0, 12, 30, 13, 0, 16, 0));
  }

  /**
   * Sotto le sei ore il buono non matura nemmeno per il ricercatore.
   */
  @Test
  public void researcherBelowSixHoursHasNoTicket() {
    assertFalse(ticketFor(2, 360, 9, 0, 12, 0, 12, 30, 15, 0));
  }

  /**
   * Un tipo orario senza buono (soglia zero) non esclude il buono del ricercatore che lavora
   * più di sei ore con la pausa.
   */
  @Test
  public void researcherIgnoresOfficeDisabledTicket() {
    assertTrue(ticketFor(1, 0, 8, 30, 12, 30, 13, 0, 16, 0));
    assertFalse(ticketFor(5, 0, 8, 30, 12, 30, 13, 0, 16, 0));
  }

  private static boolean ticketFor(int level, int officeMealTicketMinutes,
      int in1h, int in1m, int out1h, int out1m, int in2h, int in2m, int out2h, int out2m) {
    PersonDaysTest support = new PersonDaysTest();
    Qualification qualification = new Qualification();
    qualification.setQualification(level);
    Person person = new Person();
    person.setQualification(qualification);

    PersonDay personDay = new PersonDay(person, PersonDaysTest.second);
    List<Stamping> stampings = Lists.newArrayList();
    stampings.add(support.stampings(personDay, in1h, in1m, WayType.in, null, null));
    stampings.add(support.stampings(personDay, out1h, out1m, WayType.out, null, null));
    stampings.add(support.stampings(personDay, in2h, in2m, WayType.in, null, null));
    stampings.add(support.stampings(personDay, out2h, out2m, WayType.out, null, null));
    personDay.setStampings(stampings);

    WorkingTimeTypeDay wttd = support.normalDay();
    wttd.setMealTicketTime(officeMealTicketMinutes);

    personDayManager.updateTimeAtWork(personDay, wttd, false,
        PersonDaysTest.startLunch, PersonDaysTest.endLunch,
        PersonDaysTest.startWork, PersonDaysTest.endWork, Optional.absent());
    return personDay.isTicketAvailable();
  }
}
