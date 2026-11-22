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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * epas-ccnl: verifica dell'orario di ricercatori e tecnologi sul quadrimestre.
 *
 * <p>File aggiunto da epas-ccnl, modifica di ePAS. Calcolo puro, senza accesso al database:
 * riceve i riepiloghi mensili e restituisce i tre quadrimestri dell'anno.</p>
 *
 * <p>Art. 58 c. 1 CCNL 21/02/2002 (come modificato dall'art. 139 c. 3 CCNL 18/01/2024):
 * 36 ore medie settimanali nel quadrimestre. Il saldo giornaliero di ePAS (tempo a lavoro
 * meno 7:12 per ogni giorno feriale non festivo, zero nei giorni di ferie e di assenza
 * giustificata) sommato sul quadrimestre coincide con la differenza tra le ore svolte e le
 * 36 ore settimanali dovute, al netto di ferie e assenze.</p>
 *
 * <p>Art. 58 c. 5: le ore in eccesso o in difetto al termine del periodo sono cumulate con
 * quelle dei periodi precedenti; il difetto non può superare 20 ore e la parte oltre le 20
 * ore va recuperata nel periodo successivo. Il saldo considerato è quindi quello progressivo
 * a fine quadrimestre.</p>
 */
public final class QuadrimesterBalance {

  private QuadrimesterBalance() {
  }

  /**
   * Esito della verifica di un quadrimestre.
   */
  public enum Outcome {
    /** Nessun riepilogo mensile disponibile nel periodo. */
    NO_DATA("Nessun dato"),
    /** Periodo non ancora concluso: il saldo è provvisorio. */
    IN_PROGRESS("In corso"),
    /** Saldo progressivo a fine periodo positivo o nullo. */
    EVEN_OR_SURPLUS("In regola"),
    /** Saldo negativo entro le 20 ore ammesse dall'art. 58 c. 5. */
    DEFICIT_WITHIN_LIMIT("Difetto entro 20 ore"),
    /** Saldo negativo oltre le 20 ore: l'eccedenza va recuperata nel periodo successivo. */
    DEFICIT_OVER_LIMIT("Difetto oltre 20 ore");

    private final String label;

    Outcome(String label) {
      this.label = label;
    }

    public String getLabel() {
      return label;
    }
  }

  /**
   * Dati di un mese, presi dal riepilogo mensile del contratto.
   */
  public static final class MonthData {
    private final int month;
    private final int initialMinutes;
    private final int finalMinutes;
    private final int workedMinutes;
    private final int compensatoryDays;

    /**
     * Costruttore.
     *
     * @param month il mese (1-12)
     * @param initialMinutes saldo progressivo a inizio mese, in minuti
     * @param finalMinutes saldo progressivo a fine mese, in minuti
     * @param workedMinutes tempo a lavoro registrato nel mese, in minuti
     * @param compensatoryDays giorni di assenza compensativa usati nel mese
     */
    public MonthData(int month, int initialMinutes, int finalMinutes, int workedMinutes,
        int compensatoryDays) {
      if (month < 1 || month > 12) {
        throw new IllegalArgumentException("Mese non valido: " + month);
      }
      this.month = month;
      this.initialMinutes = initialMinutes;
      this.finalMinutes = finalMinutes;
      this.workedMinutes = workedMinutes;
      this.compensatoryDays = compensatoryDays;
    }

    public int getMonth() {
      return month;
    }

    public int getInitialMinutes() {
      return initialMinutes;
    }

    public int getFinalMinutes() {
      return finalMinutes;
    }

    public int getWorkedMinutes() {
      return workedMinutes;
    }

    public int getCompensatoryDays() {
      return compensatoryDays;
    }
  }

  /**
   * Risultato di un quadrimestre.
   */
  public static final class Period {
    private final int index;
    private final int firstMonth;
    private final int lastMonth;
    private final Integer initialMinutes;
    private final Integer finalMinutes;
    private final int workedMinutes;
    private final int compensatoryDays;
    private final boolean completed;
    private final Outcome outcome;
    private final int toRecoverMinutes;

    Period(int index, int firstMonth, int lastMonth, Integer initialMinutes,
        Integer finalMinutes, int workedMinutes, int compensatoryDays, boolean completed,
        Outcome outcome, int toRecoverMinutes) {
      this.index = index;
      this.firstMonth = firstMonth;
      this.lastMonth = lastMonth;
      this.initialMinutes = initialMinutes;
      this.finalMinutes = finalMinutes;
      this.workedMinutes = workedMinutes;
      this.compensatoryDays = compensatoryDays;
      this.completed = completed;
      this.outcome = outcome;
      this.toRecoverMinutes = toRecoverMinutes;
    }

    /** Numero del quadrimestre (1, 2, 3). */
    public int getIndex() {
      return index;
    }

    public int getFirstMonth() {
      return firstMonth;
    }

    public int getLastMonth() {
      return lastMonth;
    }

    /** Saldo progressivo a inizio periodo (null se non ci sono dati). */
    public Integer getInitialMinutes() {
      return initialMinutes;
    }

    /** Saldo progressivo a fine periodo, o all'ultimo mese disponibile. */
    public Integer getFinalMinutes() {
      return finalMinutes;
    }

    /** Variazione del saldo nel periodo (ore svolte meno ore dovute). */
    public Integer getBalanceMinutes() {
      if (initialMinutes == null || finalMinutes == null) {
        return null;
      }
      return finalMinutes - initialMinutes;
    }

    public int getWorkedMinutes() {
      return workedMinutes;
    }

    public int getCompensatoryDays() {
      return compensatoryDays;
    }

    public boolean isCompleted() {
      return completed;
    }

    public Outcome getOutcome() {
      return outcome;
    }

    /** Minuti in difetto oltre le 20 ore, da recuperare nel periodo successivo. */
    public int getToRecoverMinutes() {
      return toRecoverMinutes;
    }
  }

  /**
   * Calcola i tre quadrimestri dell'anno.
   *
   * @param year l'anno richiesto
   * @param months i riepiloghi mensili disponibili dell'anno (in qualsiasi ordine)
   * @param currentYear l'anno corrente
   * @param currentMonth il mese corrente (1-12)
   * @return i tre quadrimestri, in ordine
   */
  public static List<Period> compute(int year, List<MonthData> months, int currentYear,
      int currentMonth) {
    List<MonthData> sorted = new ArrayList<>(months);
    Collections.sort(sorted, (a, b) -> Integer.compare(a.getMonth(), b.getMonth()));

    List<Period> periods = new ArrayList<>();
    for (int index = 1; index <= 12 / CcnlRules.PERIOD_MONTHS; index++) {
      final int firstMonth = (index - 1) * CcnlRules.PERIOD_MONTHS + 1;
      final int lastMonth = firstMonth + CcnlRules.PERIOD_MONTHS - 1;

      MonthData first = null;
      MonthData last = null;
      int worked = 0;
      int compensatory = 0;
      for (MonthData m : sorted) {
        if (m.getMonth() < firstMonth || m.getMonth() > lastMonth) {
          continue;
        }
        if (first == null) {
          first = m;
        }
        last = m;
        worked += m.getWorkedMinutes();
        compensatory += m.getCompensatoryDays();
      }

      final boolean periodOver = year < currentYear
          || (year == currentYear && lastMonth < currentMonth);
      final boolean completed = periodOver && last != null && last.getMonth() == lastMonth;

      if (first == null) {
        periods.add(new Period(index, firstMonth, lastMonth, null, null, 0, 0, false,
            Outcome.NO_DATA, 0));
        continue;
      }

      final int initial = first.getInitialMinutes();
      final int fin = last.getFinalMinutes();
      Outcome outcome;
      int toRecover = 0;
      if (!completed) {
        outcome = Outcome.IN_PROGRESS;
      } else if (fin >= 0) {
        outcome = Outcome.EVEN_OR_SURPLUS;
      } else if (-fin <= CcnlRules.MAX_DEFICIT_MINUTES) {
        outcome = Outcome.DEFICIT_WITHIN_LIMIT;
      } else {
        outcome = Outcome.DEFICIT_OVER_LIMIT;
        toRecover = -fin - CcnlRules.MAX_DEFICIT_MINUTES;
      }
      periods.add(new Period(index, firstMonth, lastMonth, initial, fin, worked, compensatory,
          completed, outcome, toRecover));
    }
    return periods;
  }

  /**
   * Giorni di assenza compensativa usati nell'anno.
   */
  public static int compensatoryDaysUsed(List<Period> periods) {
    int used = 0;
    for (Period p : periods) {
      used += p.getCompensatoryDays();
    }
    return used;
  }
}
