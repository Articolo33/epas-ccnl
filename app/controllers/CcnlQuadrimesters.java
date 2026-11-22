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

package controllers;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperContractMonthRecap;
import dao.wrapper.IWrapperFactory;
import java.util.List;
import javax.inject.Inject;
import manager.ccnl.CcnlRules;
import manager.ccnl.QuadrimesterBalance;
import manager.ccnl.QuadrimesterBalance.MonthData;
import manager.ccnl.QuadrimesterBalance.Period;
import models.Contract;
import models.ContractMonthRecap;
import models.Person;
import models.User;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;
import play.mvc.Controller;
import play.mvc.With;

/**
 * epas-ccnl: riepilogo per quadrimestre dell'orario di ricercatori e tecnologi.
 *
 * <p>File aggiunto da epas-ccnl, modifica di ePAS. Mostra al ricercatore o tecnologo il
 * saldo delle 36 ore medie settimanali nel quadrimestre (art. 58 c. 1 CCNL 21/02/2002, come
 * modificato dall'art. 139 c. 3 CCNL 18/01/2024) e la verifica del limite di 20 ore in
 * difetto (art. 58 c. 5).</p>
 */
@With({Resecure.class})
public class CcnlQuadrimesters extends Controller {

  @Inject
  private static IWrapperFactory wrapperFactory;

  /**
   * Riepilogo per quadrimestre dell'anno indicato, per l'utente collegato.
   *
   * @param year l'anno
   */
  public static void show(int year) {
    Optional<User> user = Security.getUser();
    if (!user.isPresent() || user.get().getPerson() == null) {
      flash.error("Accesso negato.");
      renderTemplate("Application/indexAdmin.html");
    }
    final LocalDate today = LocalDate.now();
    if (year <= 0) {
      year = today.getYear();
    }
    if (year > today.getYear()) {
      flash.error("Impossibile richiedere il riepilogo di un anno futuro.");
      renderTemplate("Application/indexAdmin.html");
    }

    final Person person = user.get().getPerson();
    final boolean applicable = CcnlRules.appliesTo(person);

    List<MonthData> months = Lists.newArrayList();
    Optional<Contract> contract = wrapperFactory.create(person).getCurrentContract();
    if (contract.isPresent()) {
      IWrapperContract con = wrapperFactory.create(contract.get());
      YearMonth actual = new YearMonth(year, 1);
      final YearMonth last = new YearMonth(year, 12);
      while (!actual.isAfter(last)) {
        Optional<ContractMonthRecap> recap = con.getContractMonthRecap(actual);
        if (recap.isPresent()) {
          IWrapperContractMonthRecap wrRecap = wrapperFactory.create(recap.get());
          ContractMonthRecap value = recap.get();
          int initial = wrRecap.getResidualLastYearInit() + value.getInitMonteOreAnnoCorrente();
          int fin = value.getRemainingMinutesLastYear() + value.getRemainingMinutesCurrentYear();
          int recoveryDays = value.getRecoveryDayUsed() == null ? 0 : value.getRecoveryDayUsed();
          months.add(new MonthData(value.getMonth(), initial, fin, value.getOreLavorate(),
              recoveryDays));
        }
        actual = actual.plusMonths(1);
      }
    }

    List<Period> periods = QuadrimesterBalance.compute(year, months,
        today.getYear(), today.getMonthOfYear());
    int compensatoryDaysUsed = QuadrimesterBalance.compensatoryDaysUsed(periods);
    int compensatoryDaysMax = CcnlRules.MAX_COMPENSATORY_DAYS_PER_YEAR;
    int maxDeficitMinutes = CcnlRules.MAX_DEFICIT_MINUTES;

    render(person, year, applicable, periods, compensatoryDaysUsed, compensatoryDaysMax,
        maxDeficitMinutes);
  }
}
