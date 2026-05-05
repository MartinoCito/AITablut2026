# Terrut — Minimax + Alpha-Beta + Genetic Training

## Introduction  
Progetto nato per il corso di **Intelligenza Artificiale (UniBo)**: l’obiettivo è creare un agente capace di giocare a **Tablut** usando una combinazione di:

- 🧠 **Minimax** con **Alpha–Beta pruning**
- 🧩 **Euristica a feature** (valutazione dello stato tramite caratteristiche del dominio)
- 🧬 **Genetic Algorithm (GA)** per ottimizzare i pesi dell’euristica
- ⚡ **Parallelizzazione delle partite** durante il training

### Team 👥
- **Martino Cito**
- **Roberto Iervese**

---

### Architettura (high-level) 🏗️
- `MyAIPlayerLogic` 🧠  
  Implementa **Minimax + AlphaBeta**, iterative deepening, move ordering, gestione ripetizioni nel path.
- `SimulationEngine` ⚖️  
  Simulatore per validare mosse, catture, terminal states e gestione draw.
- `FeaturesExtractor` 🧩  
  Estrae un vettore di **feature normalizzate** per valutare uno stato.
- `GATuner` 🧬  
  Allena i **pesi** dell’euristica tramite GA e partite simulate in parallelo.
- `MatchEvaluator` 🕹️  
  Valuta singole partite come task indipendenti.

---

## How to train 

```bat
java -jar MyTrainer.jar
```

## How to Run (Player)

```bat
java -jar "TerrutPlayer.jar" WHITE/BLACK timeout <ipAddress>
```
