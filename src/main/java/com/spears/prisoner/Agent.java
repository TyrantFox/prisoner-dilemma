package com.spears.prisoner;

public interface Agent extends Comparable<Agent> {
    /**
     * @return the cumulative score for the Agent
     */
    public int getScore();

    /**
     * @return a new game instance
     */
    public Game newGame();

    /**
     * Reset cumulative score
     */
    public void reset();

    @Override
    default int compareTo(Agent o) {
        return o.getScore() - this.getScore();
    }
}
