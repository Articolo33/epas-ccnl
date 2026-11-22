/*
 * Copyright (C) 2026  Consiglio Nazionale delle Ricerche
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

import models.Person;
import models.flows.enumerate.AbsenceRequestType;
import play.Play;

/**
 * epas-ccnl: regole del CCNL Enti di Ricerca per ricercatori e tecnologi (livelli I-III).
 *
 * <p>File aggiunto da epas-ccnl, modifica di ePAS. Raccoglie in un solo punto le regole
 * che il CCNL fissa per tutto il comparto e che in ePAS sono lasciate alla configurazione
 * delle singole sedi. Le regole si applicano solo ai livelli I-III e si possono disattivare
 * con la proprietà {@code epas.ccnl.rt.enabled=false} in application.conf.</p>
 *
 * <ul>
 *   <li>Art. 58 c. 1 CCNL 21/02/2002, come modificato dall'art. 139 c. 3 CCNL 18/01/2024:
 *       36 ore medie settimanali nel quadrimestre.</li>
 *   <li>Art. 58 c. 2: autonoma determinazione del proprio tempo di lavoro.</li>
 *   <li>Art. 58 c. 3: l'attività fuori sede si autocertifica mensilmente.</li>
 *   <li>Art. 58 c. 5: ore in eccesso o difetto cumulate con i periodi precedenti, difetto
 *       massimo 20 ore, al massimo 22 giorni di assenza compensativa all'anno.</li>
 *   <li>Art. 59 c. 1: le ferie le programma il ricercatore o tecnologo.</li>
 *   <li>Art. 5 c. 2 e c. 6: buono pasto oltre le sei ore con la relativa pausa, sulla
 *       base della dichiarazione del dipendente per i livelli III-I.</li>
 * </ul>
 */
public final class CcnlRules {

  /** Proprietà di application.conf che attiva le regole (default: attive). */
  public static final String ENABLED_PROPERTY = "epas.ccnl.rt.enabled";

  /** Art. 58 c. 5: numero massimo di giorni di assenza compensativa all'anno. */
  public static final int MAX_COMPENSATORY_DAYS_PER_YEAR = 22;

  /** Art. 58 c. 5: ore in difetto ammesse a fine periodo, in minuti (20 ore). */
  public static final int MAX_DEFICIT_MINUTES = 20 * 60;

  /** Art. 5 c. 2: soglia oraria del buono pasto, in minuti (sei ore). */
  public static final int MEAL_TICKET_MINUTES = 6 * 60;

  /** Art. 58 c. 1: ore medie settimanali, in minuti. */
  public static final int WEEKLY_MINUTES = 36 * 60;

  /** Mesi del periodo di riferimento (quadrimestre, art. 139 c. 3 CCNL 18/01/2024). */
  public static final int PERIOD_MONTHS = 4;

  private CcnlRules() {
  }

  /**
   * Indica se le regole CCNL per i livelli I-III sono attive nell'installazione.
   */
  public static boolean enabled() {
    if (Play.configuration == null) {
      return true;
    }
    return !"false".equalsIgnoreCase(
        Play.configuration.getProperty(ENABLED_PROPERTY, "true").trim());
  }

  /**
   * Indica se la persona è un ricercatore o tecnologo (livelli I-III) a cui si applicano
   * le regole dell'art. 58 CCNL.
   */
  public static boolean appliesTo(Person person) {
    return person != null && enabled() && person.isTopQualification();
  }

  /**
   * Indica se le regole si applicano a un livello (qualifica) espresso come numero.
   */
  public static boolean appliesToQualification(Integer qualification) {
    return qualification != null && qualification <= 3 && enabled();
  }

  /**
   * Indica se una richiesta di assenza del ricercatore o tecnologo è una semplice
   * comunicazione, senza approvazioni.
   *
   * <p>Ferie (art. 59 c. 1: il ricercatore programma e organizza le proprie ferie), assenze
   * compensative (art. 58 c. 2 e c. 5) e permessi personali (art. 8 c. 1: assenza sulla base
   * di autocertificazione o documentazione, con comunicazione tempestiva).</p>
   */
  public static boolean isSelfCertifiedRequest(Person person, AbsenceRequestType type) {
    if (type == null || !appliesTo(person)) {
      return false;
    }
    return type.canBeInsertedByTopLevelWithoutApproval
        || type == AbsenceRequestType.PERSONAL_PERMISSION;
  }

  /**
   * Primo mese (1-12) del quadrimestre che contiene il mese indicato.
   * Gennaio-aprile, maggio-agosto, settembre-dicembre.
   */
  public static int periodStartMonth(int month) {
    if (month < 1 || month > 12) {
      throw new IllegalArgumentException("Mese non valido: " + month);
    }
    return ((month - 1) / PERIOD_MONTHS) * PERIOD_MONTHS + 1;
  }

  /**
   * Numero del quadrimestre (1, 2 o 3) che contiene il mese indicato.
   */
  public static int periodIndex(int month) {
    return (periodStartMonth(month) - 1) / PERIOD_MONTHS + 1;
  }
}
