# Modifiche di epas-ccnl rispetto a ePAS

epas-ccnl è una modifica di ePAS 2.43.0 (commit di partenza `72d048e`, 19/11/2026). Questo file
elenca ogni modifica, la norma contrattuale che la motiva e i file toccati. Nel codice le parti
modificate sono marcate con il commento `epas-ccnl`.

Tutte le regole valgono solo per ricercatori e tecnologi (livelli I-III,
`Person.isTopQualification()`) e solo se `epas.ccnl.rt.enabled` non è `false` in
`conf/application.conf`. Il punto unico in cui sono definite è
`app/manager/ccnl/CcnlRules.java`.

## Fonti contrattuali

- CCNL Enti di Ricerca 1998-2001, firmato il 21/02/2002: artt. 5, 8, 26, 58, 59.
- CCNL Istruzione e Ricerca 2019-2021, firmato il 18/01/2024: art. 139 c. 3 ("All'art. 58,
  comma 1, del CCNL del 21/02/2002 le parole "nel trimestre" sono sostituite dalle parole "nel
  quadrimestre""), art. 140 (lavoro a distanza di ricercatori e tecnologi).
- CCNL Istruzione e Ricerca 2022-2024 (23/12/2025) e anticipazione economica 2025-2027
  (01/07/2026): non modificano gli articoli sopra. L'art. 3 del CCNL 01/07/2026 riguarda il
  buono pasto nel lavoro agile, già gestibile in ePAS con il codice L-AGILE con buono pasto.

## 1. Giornata senza presenza (art. 58 c. 1-2)

ePAS segnala come anomalia (`NO_ABS_NO_STAMP`) ogni giorno feriale senza timbrature né codici
e invia un'email al dipendente. Per i livelli I-III questa anomalia non viene più generata.

Il saldo del giorno (tempo a lavoro meno 7:12) resta nel conteggio: sommato sul periodo
coincide con la differenza tra ore svolte e 36 ore settimanali dovute, al netto di ferie e
assenze. Non serve quindi cambiare il calcolo del saldo, ma solo smettere di trattare il giorno
come un errore e verificare il saldo sul quadrimestre (punto 2).

File: `app/manager/PersonDayManager.java` (`checkForPersonDayInTrouble`).

## 2. Verifica sul quadrimestre (art. 58 c. 1 e c. 5)

Nuova pagina "Orario per quadrimestre (CCNL)" nel menu del dipendente, visibile ai livelli
I-III. Per gennaio-aprile, maggio-agosto e settembre-dicembre mostra tempo a lavoro, saldo
iniziale, variazione, saldo progressivo a fine periodo e assenze compensative. A periodo
concluso segnala se il difetto supera le 20 ore e quante ore vanno recuperate nel
quadrimestre successivo.

I dati sono quelli dei riepiloghi mensili di ePAS (`ContractMonthRecap`), gli stessi della
pagina "Riepilogo Orario".

File aggiunti: `app/manager/ccnl/QuadrimesterBalance.java` (calcolo),
`app/controllers/CcnlQuadrimesters.java`, `app/views/CcnlQuadrimesters/show.html`.
File modificati: `app/views/_employeeMenu.html`, `app/controllers/RequestInit.java`,
`conf/messages.it`, `conf/permissions.drl`.

## 3. Le ore accumulate non scadono (art. 58 c. 5)

Il parametro di sede `MONTH_EXPIRY_RECOVERY_DAYS_13` poteva azzerare il residuo dell'anno
precedente. Per i livelli I-III viene ignorato: le ore "vengono cumulate con quelle risultanti
dei periodi precedenti".

File: `app/manager/ContractMonthRecapManager.java`.

## 4. Assenze compensative: 22 giorni l'anno (art. 58 c. 5)

Il limite non dipende più dal parametro di sede `MAX_RECOVERY_DAYS_13`: è fisso a 22 giorni.

File: `app/manager/AbsenceManager.java`, `app/controllers/AbsenceRequests.java`.

## 5. Ferie, assenze compensative e permessi personali senza approvazione

- Ferie: "Costituisce specifica responsabilità del ricercatore e tecnologo programmare e
  organizzare le proprie ferie" (art. 59 c. 1).
- Assenze compensative: art. 58 c. 2 e c. 5.
- Permessi personali: il dipendente si assenta "sulla base di apposita autocertificazione o
  documentazione, da presentare con comunicazione tempestiva" (art. 8 c. 1).

Per i livelli I-III queste richieste diventano comunicazioni: si completano subito e il
responsabile riceve solo la notifica, qualunque sia la configurazione della sede
(`VACATION_REQUEST_I_III_*`, `COMPENSATORY_REST_REQUEST_I_III_*`,
`PERSONAL_PERMISSION_REQUEST_I_III_*`, `enableAbsenceTopLevelAuthorization`).

File: `app/manager/flows/AbsenceRequestManager.java`, `app/controllers/AbsenceRequests.java`.

## 6. Lavoro fuori sede e autocertificazione delle timbrature

- "Lo svolgimento dell'attività al di fuori della sede di servizio deve essere autocertificato
  mensilmente" (art. 58 c. 3): nessuna autorizzazione.
- Il buono pasto dei livelli III-I si consegna "sulla base di apposite dichiarazioni del
  dipendente" (art. 5 c. 6).
- L'obbligo di rispettare le formalità di rilevazione delle presenze vale "salvo quanto
  previsto dall'art. 58, comma 2" (art. 26 c. 1 lett. e).

Per i livelli I-III la causale "lavoro fuori sede", l'autocertificazione delle proprie
timbrature e la stampa mensile del fuori sede sono sempre disponibili, senza i parametri
`WORKING_OFF_SITE`, `OFF_SITE_STAMPING` e `TR_AUTOCERTIFICATION` e senza l'atto di
autorizzazione da caricare. Restano i limiti di ePAS sui mesi già inviati agli attestati e
sui due mesi precedenti. Le timbrature autocertificate non generano notifiche agli
amministratori, come già avveniva con `TR_AUTOCERTIFICATION`.

File: `conf/permissions.drl` (regole `ccnlResearcher*`), `app/dao/UserDao.java`,
`app/controllers/Stampings.java`, `app/manager/NotificationManager.java`.

## 7. Buono pasto: soglia contrattuale (art. 5 c. 2)

Per i livelli I-III la soglia è sempre di 6 ore con la relativa pausa, indipendentemente dal
tipo orario della sede. Non si applica la soglia pomeridiana, che il CCNL non prevede. Un tipo
orario della sede senza buono pasto non esclude il buono nei giorni lavorativi.

File: `app/manager/PersonDayManager.java` (`mealTicketHandlerAndDecurtedMeal`).

## 8. Avviso nella pagina dei parametri di sede

La pagina di configurazione della sede avvisa che per i livelli I-III le regole CCNL
prevalgono sui parametri.

File: `app/views/Configurations/show.html`.

## Limiti noti e lavoro da fare

- **Codice dell'assenza compensativa.** I livelli I-III usano ancora il codice 91 "Riposo
  compensativo", lo stesso dei riposi da straordinario dei livelli IV-IX (art. 49). Un codice
  distinto va concordato con il sistema Attestati del CNR, altrimenti l'invio mensile fallisce.
- **160 ore dell'art. 58 c. 4.** Non c'è ancora una causale per le attività di arricchimento
  professionale, per lo stesso motivo.
- **Permessi dell'art. 8 per i livelli IV-IX.** Il CCNL li prevede con autocertificazione e
  comunicazione anche per questi livelli; epas-ccnl per ora interviene solo sui livelli I-III.
- **Pagina del quadrimestre.** Usa il contratto in corso: per un anno con più contratti mostra
  solo i mesi del contratto attuale.
- **Verifica.** Le modifiche non sono state compilate nell'ambiente in cui sono state scritte,
  che non aveva accesso alle dipendenze Maven. Il calcolo del quadrimestre è stato provato a
  parte; la compilazione completa e i test avvengono con il workflow `test.yml` su GitHub.
  Prima di usare epas-ccnl in produzione serve una prova su una copia della base dati.
