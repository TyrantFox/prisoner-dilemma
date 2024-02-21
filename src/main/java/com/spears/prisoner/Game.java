package com.spears.prisoner;

public interface Game {
    /**
     *
     * @param opponentLastPlay whether the opponent cooperated in the last round
     * @return whether to cooperate in this round
     */
    boolean play(boolean opponentLastPlay);

    /**
     * Following a play this provides the udpate to the score (the payout for the round)
     * @param points
     */
    void acceptPayment(int points);

    int getGameScore();
}
