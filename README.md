\documentclass[a4paper,12pt]{article}
\usepackage{amsmath}
\usepackage{graphicx}
\usepackage{listings}
\usepackage{xcolor}
\usepackage{hyperref}

\title{Dokumentation für Bingo Spiel}
\author{}
\date{}

\begin{document}

\maketitle

\tableofcontents

\newpage

\section{Einleitung}
Dieses Dokument beschreibt die Implementierung eines Bingo-Spiels in Python. Das Spiel besteht aus einem Host und einem Spieler, wobei der Spieler auf einem generierten Bingo-Feld Wörter markiert, bis eine Gewinnbedingung erfüllt ist.

\section{Codebeschreibung}

\subsection{Funktion zur Generierung einer Bingokarte}
Diese Funktion generiert eine Bingokarte mit den gegebenen Wörtern.
\begin{lstlisting}[language=Python, caption=generate\_bingo\_card]
def generate_bingo_card(words, xaxis, yaxis):
    random.shuffle(words)
    card = [words[i * xaxis:(i + 1) * xaxis] for i in range(yaxis)]
    return card
\end{lstlisting}

\subsection{Funktion zur Anzeige der Bingokarte}
Diese Funktion zeigt die Bingokarte auf dem Bildschirm an und markiert bereits ausgewählte Wörter.
\begin{lstlisting}[language=Python, caption=display\_bingo\_card]
def display_bingo_card(screen, card, marked):
    screen.clear()
    for y, row in enumerate(card):
        for x, word in enumerate(row):
            if marked[y][x]:
                word = '\u0336'.join(word) + '\u0336'  # Durchstreichen des Wortes
                screen.print_at(word, x * 10, y * 2, colour=2)
            else:
                screen.print_at(word, x * 10, y * 2)
    screen.refresh()
\end{lstlisting}

\subsection{Funktion zur Überprüfung auf Gewinn}
Diese Funktion überprüft, ob eine Gewinnbedingung erfüllt ist.
\begin{lstlisting}[language=Python, caption=check\_winner]
def check_winner(card, marked):
    n = len(card)
    for row in marked:
        if all(row):
            return True
    for col in range(n):
        if all(row[col] for row in marked):
            return True
    if all(marked[i][i] for i in range(n)) or all(marked[i][n-i-1] for i in range(n)):
        return True
    return False
\end{lstlisting}

\subsection{Funktion zum Spiel eines Spielers}
Diese Funktion definiert das Verhalten eines Spielers, einschließlich des Markierens von Wörtern und des Loggens von Aktionen.
\begin{lstlisting}[language=Python, caption=player\_game]
def player_game(conn, player_id, card, xaxis, yaxis, log_file):
    screen = Screen.open()
    marked = [[False] * xaxis for _ in range(yaxis)]

    def mark_word(x, y):
        marked[y][x] = True
        log_action(log_file, f"Mark {card[y][x]} at ({x},{y})")
        if check_winner(card, marked):
            display_bingo_card(screen, card, marked)
            conn.send(f"Spieler {player_id} gewinnt mit dem Wort: {card[y][x]} bei ({x},{y})")
            return True
        return False

    def log_action(log_file, action):
        with open(log_file, 'a') as f:
            f.write(f"{datetime.now()} {action}\n")

    display_bingo_card(screen, card, marked)

    while True:
        event = screen.get_event()
        if event and isinstance(event, KeyboardEvent):
            display_bingo_card(screen, card, marked)
            if event.key_code == ord('q'):
                conn.send("quit")
                break
            elif event.key_code == ord('m'):
                x, y = random.randint(0, xaxis-1), random.randint(0, yaxis-1)

                if not marked[y][x]:
                    print(f"\nMarked word: {card[y][x]}")
                    if mark_word(x, y):
                        break

    screen.close()
\end{lstlisting}

\subsection{Funktion zum Spielhost}
Diese Funktion generiert die Bingokarte und startet das Spiel.
\begin{lstlisting}[language=Python, caption=host\_game]
def host_game(words, xaxis, yaxis, log_dir):
    card = generate_bingo_card(words, xaxis, yaxis)
    parent_conn, child_conn = Pipe()

    player_log = os.path.join(log_dir, f"{datetime.now().strftime('%Y-%m-%d-%H-%M-%S')}-bingo-Player.txt")
    with open(player_log, 'w') as f:
        f.write(f"{datetime.now()} Start des Spiels\n")
        f.write(f"{datetime.now()} Größe des Spielfelds: ({xaxis},{yaxis})\n")

    p = Process(target=player_game, args=(child_conn, 1, card, xaxis, yaxis, player_log))
    p.start()

    try:
        while True:
            if parent_conn.poll():
                message = parent_conn.recv()
                if message == "quit":
                    print("Der Spieler hat das Spiel beendet.")
                    break
                else:
                    print(message)
                    break
    finally:
        p.join()
        parent_conn.close()
        child_conn.close()
\end{lstlisting}

\subsection{Hauptprogramm}
Das Hauptprogramm liest die Eingaben des Benutzers ein und startet das Spiel.
\begin{lstlisting}[language=Python, caption=main]
def main(xaxis, yaxis, words_file, log_dir):
    try:
        with open(words_file, 'r') as f:
            words = [line.strip() for line in f.readlines()]

        if len(words) < xaxis * yaxis:
            print("Nicht genügend Wörter in der Datei.")
            exit(1)
        while True:
            print(f"xaxis: {xaxis}")
            print(f"yaxis: {yaxis}")
            print(f"wordfile: {words_file}")
            print(f"log_dir: {log_dir}")

            host_game(words, xaxis, yaxis, log_dir)

            again = input("Möchten Sie noch eine Runde spielen? (ja/nein): ").strip().lower()
            if again != 'ja':
                print("Spiel beendet.")
                break

    except Exception as e:
        print(f"Fehler: {e}")

if __name__ == "__main__":
    try:
        xaxis = int(input("Bitte eine Zahl für die x-Achse eingeben: "))
        yaxis = int(input("Bitte eine Zahl für die y-Achse eingeben: "))
    except ValueError:
        print("Bitte eine gültige Zahl eingeben.")
        exit(1)

    words_file = input("Bitte den Pfad zur Wortdatei eingeben: ")
    log_dir = input("Bitte das Verzeichnis für die Logs eingeben: ")

    if not os.path.exists(log_dir):
        print("Das angegebene Verzeichnis für die Logs existiert nicht.")
        exit(1)

    main(xaxis, yaxis, words_file, log_dir)
\end{lstlisting}

\end{document}
