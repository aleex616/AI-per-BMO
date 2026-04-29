# LUNA

In questo progetto realizzeremo un'app per smartphone dotata di IA chiamata LUNA.

Quest'app ha le seguenti funzionalità:
- Orologio
- Hub di ricarica di dispositivi e dello smartphone usato
- Modello IA in cloud
- Calendario/promemoria
- Sveglia/timer

---

## Obiettivo

L'idea è creare un'**applicazione Android** che mostri l'orario e data e al pronunciare del suo nome "LUNA" o alla pressione del pulsante per parlare si attivi l'LLM.
Insieme al modello 3D che gli dà la forma di un robottino, è perfetto da lasciare sul comodino e da usare come assistente digitale, hub di ricarica e sveglia.

Utilizzeremo un modello Google in cloud con le seguenti caratteristiche:

### Modello AI

| Parametro | Valore |
|-----------|--------|
| Modello | `gemini-2.5-flash` |
| Contesto | 1.000.000 token (~750.000 parole) |
| Knowledge cutoff | 2023/2024 |
| Limite giornaliero | 250 richieste (**RPD**) |
| Limite al minuto | 10 richieste (**RPM**) |
| Token al minuto | 250.000 (**TPM**) |

> API gratuita per tutti da Google AI Studio.

---

## Materiali

**Hardware**
- Smartphone (Android 9.0 (pie) o superiore)
- PC
- Stampante 3D
- Filamento di qualsiasi colore
- Hub USB
- Caricatore Qi MagSafe
- Fortuna

**Software**
- VS Code
- Android Studio
- Flash Print 5 (software di slicing)
- Blender
- PowerPoint

---

## Come funziona la nostra app 
![schemaApp](miscellaneous/schemaApp.png)


---

## Come iniziare

1. Scarica tutto il codice dalla repository
2. vai su google AI studio e crea la tua API key
3. estrai la cartella zip che hai scaricato e cerca il file local.properties e aprilo con VS code o un altro editor di testo
4. incolla la tua API key nella variabile presente nel file
5. Apri android studio e crea la build
6. Installa sullo smartphone
7. enjoy

---

## Timeline


| Step | Descrizione                           | Data prevista | Data di svolgimento |
|:----:|---------------------------------------|:-------------:|:-------------------:|
| 1    | IA di base                            | 10/03/2026    | 10/03/2026          |
| 2    | Integrazione nell'app                 | 24/03/2026    | 24/03/2026          |
| 3    | Speech-to-Text                        | 07/04/2026    | 07/04/2026          |
| 4    | Text-to-Speech                        | 14/04/2026    | 07/04/2026          |
| 5    | UI                                    | 28/04/2026    | 12/04/2026          |
| 6    | Realizzazione parte orologio          | 30/04/2026    | 24/04/2026          |
| 7    | Opzioni di personalizzazione          | 03/05/2026    | 24/04/2026          |
| 8    | Integrazione sveglia e calendario     | 06/05/2026    | 25-26/04/2026       |
| 9    | Testing & Bug Fixing **← siamo qui!** | 08/05/2026    | 29/04/2026          |
| 10   | Design esterno del robot              | 12/05/2026    | 17/04/2026          |
| 11   | Creazione dei modelli 3D su Blender   | 12/05/2026    | 24/04/2026          |
| 12   | Stampa 3D                             | 19/05/2026    | ??/05/2026          |
| 13   | Fine progetto                         | 19/05/2026    |                     |
