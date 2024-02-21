package com.spears.prisoner.simple;

import java.util.function.BiPredicate;

import com.spears.prisoner.Agent;
import com.spears.prisoner.Game;

public class SimpleAgent<T extends BiPredicate<Integer, boolean[]>> implements Agent {
    private int score = 0;
    /**
     * How many historic events are tracked. This will be twice the number of rounds.
     */
    protected final int historyLength;
    /**
     * A strategy that determines the behavior for this move given the round number and the history. History is in reverse order, where index zero is the last move from the opponent, and index one is the last move from this agent, etc.
     */
    protected final T strategy;

    private final String name;

    public SimpleAgent(String name, int historyLength, T strategy) {
        this.name = name;
        this.historyLength = historyLength;
        this.strategy = strategy;
    }

    @Override
    public int getScore() {
        return score;
    }

    @Override
    public void reset() {
        score = 0;
    }

    @Override
    public Game newGame() {
        return new SimpleGame();
    }

    public T getStrategy() {
        return strategy;
    }

    @Override
    public String toString() {
        return name + ": " + score;
    }

    private class SimpleGame implements Game {
        /**
         * The last three plays from each player in reverse order (index 0 is the opponent's latest play)
         */
        private boolean[] history = new boolean[SimpleAgent.this.historyLength];
        private int round = 0;
        private int gameScore = 0;

        public SimpleGame() {
            for (int i = 0; i < history.length; i++) {
                history[i] = true;
            }
        }

        @Override
        public boolean play(boolean opponentLastPlay) {
            round++;
            if (SimpleAgent.this.historyLength > 0) {
                System.arraycopy(history, 0, history, 1, SimpleAgent.this.historyLength - 1);
                history[0] = opponentLastPlay;
            }
            boolean result = SimpleAgent.this.strategy.test(round, history);
            if (SimpleAgent.this.historyLength > 0) {
                System.arraycopy(history, 0, history, 1, SimpleAgent.this.historyLength - 1);
                history[0] = result;
            }

            return result;
        }

        @Override
        public void acceptPayment(int points) {
            SimpleAgent.this.score += points;
            gameScore += points;
        }

        @Override
        public int getGameScore() {
            return gameScore;
        }

        @Override
        public String toString() {
            return SimpleAgent.this.name + ": " + gameScore;
        }
    }
}
