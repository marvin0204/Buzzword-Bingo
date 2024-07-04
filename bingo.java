import random
import os
from datetime import datetime
from multiprocessing import Process, Pipe
from asciimatics.screen import Screen
from asciimatics.event import KeyboardEvent
import time
import sys

# Funktion zur Generierung einer Bingokarte
def generate_bingo_card(words, xaxis, yaxis):
    random.shuffle(words)
    card = [words[i * xaxis:(i + 1) * xaxis] for i in range(yaxis)]
    return card

# Funktion zur Anzeige der Bingokarten
def display_bingo_cards(screen, card1, marked1, card2, marked2):
    screen.clear()
    screen.print_at("Spieler 1", 0, 0, colour=7)
    screen.print_at("Spieler 2", 40, 0, colour=7)

    for y, row in enumerate(card1):
        for x, word in enumerate(row):
            if marked1[y][x]:
                word = '\u0336'.join(word) + '\u0336'  # Durchstreichen des Wortes
                screen.print_at(word, x * 10, y * 2 + 2, colour=2)
            else:
                screen.print_at(word, x * 10, y * 2 + 2)

    for y, row in enumerate(card2):
        for x, word in enumerate(row):
            if marked2[y][x]:
                word = '\u0336'.join(word) + '\u0336'  # Durchstreichen des Wortes
                screen.print_at(word, x * 10 + 40, y * 2 + 2, colour=2)
            else:
                screen.print_at(word, x * 10 + 40, y * 2 + 2)

    screen.refresh()

# Funktion zur Überprüfung auf Gewinn
def check_winner(marked):
    n = len(marked)
    for row in marked:
        if all(row):
            return True
    for col in range(n):
        if all(row[col] for row in marked):
            return True
    if all(marked[i][i] for i in range(n)) or all(marked[i][n-i-1] for i in range(n)):
        return True
    return False