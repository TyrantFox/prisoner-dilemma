package com.spears.prisoner;

import java.awt.BorderLayout;
import java.awt.HeadlessException;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiPredicate;

import javax.swing.JFrame;

import com.spears.prisoner.genetic.NeuralStrategy;
import com.spears.prisoner.simple.SimpleAgent;

public class Competition extends JFrame {
    private static final BiPredicate<Integer, boolean[]> titForTat = (turn, history) -> history[0];
    private static final BiPredicate<Integer, boolean[]> titForTwoTat = (turn, history) -> history[0] || history[2];
    private static final BiPredicate<Integer, boolean[]> alwaysDefect = (turn, history) -> false;
    private static final BiPredicate<Integer, boolean[]> alwaysCooperate = (turn, history) -> true;
    private static final BiPredicate<Integer, boolean[]> random = (turn, history) -> Math.random() > 0.2;

    public static void main(String[] args) throws IOException {
        Competition competition = new Competition();
        competition.setVisible(true);
        competition.writer = new FileWriter("/Users/tfs/scratch/prisoner.csv");
        try {
            competition.runSimulation();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        competition.writer.close();
    }

    boolean sloMo = false;
    private Writer writer;
    private NeuralStrategy bestStrategy = null;

    public Competition() throws HeadlessException {
        setTitle("Prisoner's Dilemma Simulation");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null); // Center the frame
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // Write the output
                // write the weights and balances to file for the winner
                if (writer != null && bestStrategy != null) {
                    try {
                        bestStrategy.writeCSV(writer);
                        boolean[] history = new boolean[6];
                        for (int round = 0; round < 500; round += 100) {
                            for (int state = 0; state < 64; state++) {
                                writer.write(Integer.toString(round));
                                int x = state;
                                for (int i = 0; i < 6; i++) {
                                    history[i] = (x & 0x01) == 1;
                                    x >>= 1;
                                    writer.write(',');
                                    writer.write(history[i] ? '1' : '0');
                                }
                                boolean output = bestStrategy.test(round, history);
                                writer.write(',');
                                writer.write(output ? '1' : '0');
                                writer.write('\n');
                            }
                        }

                        // Compete against the standard strategies
                        List<Agent> competitors = new ArrayList<>();
                        // Add some others for them to compete against
                        competitors.add(new SimpleAgent<>("Neural Agent", 6, bestStrategy));
                        competitors.add(new SimpleAgent<>("Always Cooperate", 0, alwaysCooperate));
                        competitors.add(new SimpleAgent<>("Always Defect", 0, alwaysDefect));
                        competitors.add(new SimpleAgent<>("Random1", 0, random));
                        competitors.add(new SimpleAgent<>("Tit for tat", 1, titForTat));
                        competitors.add(new SimpleAgent<>("Tit for two tat", 3, titForTwoTat));

                        for (int i = 0; i < 10; i++) {
                            for (Agent player1 : competitors) {
                                for (Agent player2 : competitors) {
                                    compete(player1, player2, 500, false);
                                }
                            }
                        }
                        Collections.sort(competitors);
                        for (Agent agent : competitors) {
                            writer.write(agent.toString());
                            writer.write('\n');
                        }
                        writer.flush();
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }

                // Finally, call the setDefaultCloseOperation to close the frame
                setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            }
        });

    }

    public void runSimulation() throws InterruptedException, IOException {
        int agentNumber = 0;

        // Make some neural agents
        List<SimpleAgent<NeuralStrategy>> neuralAgents = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            NeuralStrategy s = new NeuralStrategy(7, 4);
            s.randomize();
            neuralAgents.add(new SimpleAgent<>("N" + agentNumber++, 6, s));
        }

        for (int generation = 0; generation < 500000; generation++) {
            // Add the first NeuralAgent strategy to the frame
            this.add(neuralAgents.get(0).getStrategy(), BorderLayout.CENTER);
            this.pack();
            // Put them all in the competitors pool
            List<Agent> competitors = new ArrayList<>(neuralAgents);
//            // Add some others for them to compete against
//            competitors.add(new SimpleAgent<>("Always Cooperate", 0, alwaysCooperate));
//            competitors.add(new SimpleAgent<>("Always Defect", 0, alwaysDefect));
//            competitors.add(new SimpleAgent<>("Random1", 0, random));
//            competitors.add(new SimpleAgent<>("Random2", 0, random));
//            competitors.add(new SimpleAgent<>("Tit for tat", 1, titForTat));
//            competitors.add(new SimpleAgent<>("Tit for two tat", 3, titForTwoTat));

            for (int i = 0; i < 5; i++) {
                for (Agent player1 : competitors) {
                    for (Agent player2 : competitors) {
                        compete(player1, player2, 500, false);
                    }
                }
            }

            // Remove the panel to be replaced by the new winner
            this.remove(neuralAgents.get(0).getStrategy());
            Collections.sort(competitors);
            Collections.sort(neuralAgents);

            System.out.println("Generation: " + generation);
            System.out.println(neuralAgents.get(0) + ";  " + competitors.get(0));

            // keep the top performer, use the top two to mutate and repopulate
            bestStrategy = neuralAgents.get(0).getStrategy();
            List<SimpleAgent<NeuralStrategy>> priorPopulation = neuralAgents;
            neuralAgents = new ArrayList<>(neuralAgents.size());
            neuralAgents.add(priorPopulation.get(0));
            priorPopulation.get(0).reset();
            int index = 0;
            while (neuralAgents.size() < 20) {
                NeuralStrategy s = priorPopulation.get(index).getStrategy().clone();
                s.mutate(2F + 8F * Math.max(0F, 0.002F * (500 - generation)));
                neuralAgents.add(new SimpleAgent<>("N" + agentNumber++, 6, s));
                // Use the most successful 5 neural strategies to seed the next generation
                index = (index + 1 % 8);
            }
            // mutate the last one a bit more for some entropy
            neuralAgents.get(neuralAgents.size() - 1).getStrategy().mutate(10F);
        }

        System.out.println(neuralAgents.get(0).getStrategy());
        // compete the first 2 players against the manual versions
        List<Agent> agents = new ArrayList<>();
        agents.add(new SimpleAgent<NeuralStrategy>("Neural 1", 6, neuralAgents.get(0).getStrategy()));
        agents.add(new SimpleAgent<NeuralStrategy>("Neural 2", 6, neuralAgents.get(1).getStrategy()));
        agents.add(new SimpleAgent<>("Tit for tat", 1, titForTat));
        agents.add(new SimpleAgent<>("Tit for two tat", 3, titForTwoTat));
        agents.add(new SimpleAgent<>("Always Defect", 0, alwaysDefect));
        agents.add(new SimpleAgent<>("Always Cooperate", 0, alwaysCooperate));
        agents.add(new SimpleAgent<>("Random", 0, random));

        add(neuralAgents.get(0).getStrategy(), BorderLayout.CENTER);
        for (int i = 0; i < 10; i++) {
            for (Agent player1 : agents) {
                for (Agent player2 : agents) {
                    sloMo = (player1 == agents.get(0) && player2 == agents.get(6));
                    compete(player1, player2, 500, true);
                }
            }
        }

        Collections.sort(agents);

        for (Agent agent : agents) {
            System.out.println(agent);
        }
    }

    public int compete(Agent agent1, Agent agent2, int rounds, boolean log) throws InterruptedException {
        Game game1 = agent1.newGame();
        Game game2 = agent2.newGame();
        // 500 rounds
        boolean last1 = true;
        boolean last2 = true;
        for (int round = 0; round < rounds; round++) {
            boolean cooperate1 = game1.play(last2);
            boolean cooperate2 = game2.play(last1);
            if (log) {
                System.out.print((cooperate1 ? "1" : "0") + (cooperate2 ? "1" : "0") + " ");
            }
            if (cooperate1) {
                if (cooperate2) {
                    game1.acceptPayment(3);
                    game2.acceptPayment(3);
                } else {
                    game2.acceptPayment(5);
                }
            } else {
                if (cooperate2) {
                    game1.acceptPayment(5);
                }
            }
            last1 = cooperate1;
            last2 = cooperate2;
        }
        if (log) {
            System.out.println(game1 + "  vs  " + game2);
        }
        if (sloMo) {
            Thread.sleep(100);
        }
        return Integer.compare(game1.getGameScore(), game2.getGameScore());
    }
}
// Winning strategy: NeuralStrategy{inputNodes=7, hiddenNodes=4, layer1WandB=[-8.386028, 2.093851, -9.931549, 2.3337495, -0.1290679, 3.4237962, -9.897608, -6.9329114, -1.7207811, -2.6283493, 15.543184, 0.26271844, 7.7571774, 4.561391, 10.918468, 3.8788776, -8.366383, 3.1482666, 2.3929732, 10.429932, -3.0819666, -0.5745237, 3.5739381, 9.627931, 6.8680105, -5.475805, -2.5376437, 0.8376126, 3.454391, -8.30924, 5.101409, -8.181271], layer2WandB=[-2.1129003, 3.048812, -7.6071596, 0.76300097, -8.712442]}
//                   NeuralStrategy{inputNodes=7, hiddenNodes=4, layer1WandB=[-8.116583, 1.0033467, -1.618475, -7.9823327, 5.605843, -1.5227392, 10.368262, 2.6374893, 10.648161, 1.3634453, -1.3503288, 5.077832, -0.18515348, 2.6057484, 1.258893, 1.9781802, -1.5378143, -2.6082523, -2.890381, -0.4948194, 2.75598, 7.8397236, -2.0933523, 3.6165514, 7.0422688, -2.0270076, 3.9829428, -2.50707, -0.84087604, 0.905385, -3.1964173, 1.8861244], layer2WandB=[-5.284036, 8.6118145, -4.4409685, 0.24980445, 6.0517597]}