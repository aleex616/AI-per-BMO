# AI-per-BMO

In questo progetto realizzeremo la parte software per un robot chiamato **BMO** realizzato da [SimoneBrancaglion](https://github.com/LeonixGamesIndustries) e da [MatteoGravina](https://github.com/Gravi09)

L'idea è quella di realizzare un **applicazione per Android** capace di eseguire in background uno **script in Python** che ci permette di avere le risposte dall'AI. Lo script usa un modello di Google con le seguenti caratteristiche:
- **Modello:** gemini-2.5-flash 
- **Contesto** di 1.000.000 token (circa 750.000 parole)
- **Knowledge** 2023/2024
- **250 MAX** richieste al giorno **(RPD)**
- **10 MAX** richieste al minuto **(RPM)**
- **250.000 MAX** token al minuto **(TPM)**
- **API:** gratuita

Successivamente useremo qualcosa che non so ancora cosa, per trasformare le parole pronunciate dall'utente in testo e trasformare poi, il testo generato dall' AI in audio.
