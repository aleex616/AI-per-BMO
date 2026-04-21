## AI-per-BMO

In questo progetto realizzeremo la parte software per un robot chiamato **BMO**, sviluppato da:
- [Simone Brancaglion](https://github.com/LeonixGamesIndustries)
- [Matteo Gravina](https://github.com/Gravi09)

---

## Obiettivo

L'idea è creare un'**applicazione Android** capace di mostrare la faccia di BMO e di utilizzare un LLM per rispondere a domande

![Imagine illustrativa](miscellaneous/imgREADME.png)

Utilizzeremo un modello Google con le seguenti caratteristiche:

### Modello AI
- **Modello:** `gemini-2.5-flash`
- **Contesto:** 1.000.000 token (~750.000 parole)
- **Knowledge cutoff:** 2023/2024
- **Limiti:**
  - 250 richieste al giorno (**RPD**)
  - 10 richieste al minuto (**RPM**)
  - 250.000 token al minuto (**TPM**)
- **API:** gratuita

---

## Funzionalità future

Successivamente verranno integrate:
- **Attivazione al richiamo** → alla pronuncia di "Hey BMO" inizierà ad ascoltare
- **Speech-to-Text** → trasformare la voce in testo  
- **Text-to-Speech** → trasformare il testo in audio  

---

## Materiali

- Smartphone
- PC
- Fortuna  

---

## 📅 Timeline

| Step | Descrizione                  | Data prevista   | Data di svolgimento |
|------|-----------------------------|------------------|---------------------|
| 1    | IA di base                  | 10/03/2026       |  10/03/2026         |
| 2    | Integrazione nell'app       | 24/03/2026       |  24/03/2026         |
| 3    | Speech-to-Text              | 07/04/2026       |  07/04/2026         |
| 4    | Text-to-Speech              | 14/04/2026       |  07/04/2026         |
| 5    | UI                          | 28/04/2026       |  10/04/2026         |
| 6    | Testing & Bug Fixing    **← siamo qui!** | 6/05/2026 |           |
| 7    | Creazione robot Luna        | 12/05/2026     |                     |
| 8    | Fine progetto               | 19/05/2026    |                     |

---

## Piano B

In caso gli altri componenti di questo progetto, elencati all'inizio, non riuscissero a creare la parte fisica per BMO, noi abbiamo pensato ad un piano B per il nostro progetto ed è quello di aggiungere la nostra AI in un altro robottino di lumodevice.com da cui abbiamo preso spunto.
Questo robottino verrà costruito usando la stampante 3D e lasceremo uno spazio dove posare il telefono che servirà per comunicare con la AI.
