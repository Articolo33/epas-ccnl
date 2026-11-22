# epas-ccnl

**epas-ccnl è una modifica di [ePAS](https://github.com/consiglionazionaledellericerche/epas),**
il sistema di rilevazione delle presenze del Consiglio Nazionale delle Ricerche sviluppato
dall'Istituto di Informatica e Telematica (IIT-CNR). Non è una versione ufficiale di ePAS e non
è prodotta né sostenuta dal CNR o da IIT-CNR.

Il codice di partenza è ePAS 2.43.0, commit
[`72d048e`](https://github.com/consiglionazionaledellericerche/epas/commit/72d048e7a81ff7449ed6f6163514a63d085dbd8c)
del 19 novembre 2026. La storia completa di ePAS è conservata in questo repository; le modifiche
di epas-ccnl sono nei commit successivi. Il README originale di ePAS è in
[README_EPAS.md](README_EPAS.md).

## Perché

Il CCNL Enti di Ricerca prevede per ricercatori e tecnologi (livelli I-III) un orario di 36 ore
medie settimanali nel quadrimestre e l'autonoma determinazione del proprio tempo di lavoro
(art. 58 CCNL 21/02/2002, come modificato dall'art. 139 c. 3 CCNL 18/01/2024). ePAS invece
ragiona per giornate, non verifica il quadrimestre e lascia alle singole sedi decisioni che il
contratto fissa per tutto il comparto: approvazione delle ferie, lavoro fuori sede, soglia del
buono pasto, scadenza delle ore accumulate.

epas-ccnl applica le regole del contratto ai livelli I-III. Per i livelli IV-IX il comportamento
resta quello di ePAS.

## Cosa cambia

| Area | ePAS | epas-ccnl (livelli I-III) | Norma |
| --- | --- | --- | --- |
| Giornata senza presenza | Segnalata come anomalia, con email | Non è un'anomalia | Art. 58 c. 1-2 |
| Periodo di riferimento | Solo mese e anno | Nuova pagina "Orario per quadrimestre" con il saldo progressivo e il limite di 20 ore | Art. 58 c. 1 e 5; art. 139 c. 3 CCNL 2024 |
| Ore accumulate | La sede può farle scadere | Non scadono | Art. 58 c. 5 |
| Assenze compensative | Limite annuo deciso dalla sede | 22 giorni, fisso | Art. 58 c. 5 |
| Ferie, assenze compensative, permessi personali | Approvazione del direttore se la sede lo prevede | Comunicazione, senza approvazione | Art. 59 c. 1; art. 58 c. 5; art. 8 c. 1 |
| Lavoro fuori sede | Solo se la sede lo abilita, persona per persona | Sempre disponibile, con stampa mensile dell'autocertificazione | Art. 58 c. 3 |
| Autocertificazione delle timbrature | Solo se la sede la abilita caricando un atto di autorizzazione | Sempre disponibile | Art. 5 c. 6; art. 26 c. 1 lett. e); art. 58 c. 2 |
| Soglia del buono pasto | Decisa dal tipo orario della sede | 6 ore con la pausa, uguale ovunque | Art. 5 c. 2 |

Il dettaglio, con i file modificati e i limiti noti, è in [MODIFICHE_CCNL.md](MODIFICHE_CCNL.md).

Le regole si possono disattivare con `epas.ccnl.rt.enabled=false` in `conf/application.conf`.

## Installazione e test

Valgono le istruzioni di ePAS ([README_EPAS.md](README_EPAS.md) e
[README_DEVELOPERS.md](README_DEVELOPERS.md)). Il workflow `.github/workflows/test.yml` esegue
`play autotest` a ogni push sul ramo `main`, compresi i test aggiunti da epas-ccnl
(`test/manager/ccnl/QuadrimesterBalanceTest.java`, `test/persondays/CcnlMealTicketTest.java`).
I workflow di pubblicazione dell'immagine Docker di ePAS sono stati rimossi perché richiedono le
credenziali del progetto originale.

## Licenza

Come ePAS, epas-ccnl è distribuito con la licenza GNU Affero General Public License v3 indicata
nelle intestazioni dei file (il file [LICENSE](LICENSE) ereditato da ePAS riporta il testo della
GNU GPL v3). I diritti sul codice originale restano del
Consiglio Nazionale delle Ricerche; le modifiche sono indicate nei file con la dicitura
"epas-ccnl". Chi usa epas-ccnl in rete deve rendere disponibile il codice sorgente agli utenti,
come richiede la AGPL.


