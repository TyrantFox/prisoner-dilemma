package com.spears.prisoner.genetic;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Random;
import java.util.function.BiPredicate;

import javax.swing.JPanel;

public class NeuralStrategy extends JPanel implements BiPredicate<Integer, boolean[]> {
    protected final int inputNodes;
    protected final int hiddenNodes;

    /*
    Fully interconnected two layer network with the last three moves of each player and the round number as inputs.
    Weights and biases initially randomized. Middle layer uses RELU activation function. Output is cooperate if positive, otherwise defect.
     */
    /**
     * Matrix as a single dimension array, row-first.
     * [i1]
     * [i2]
     * [ a11 a12 a13 a14 a15 a16 b1 ] [i3]
     * [ a21 a22 a23 a24 a25 a26 b2 ] [i4]
     * [ a31 a32 a33 a34 a35 a36 b3 ] [i5]
     * [ a41 a42 a43 a44 a45 a46 b4 ] [i6]
     * [1 ]
     * <p>
     * This is immediately followed by the hidden layer matrix
     */
    float[] wAndB;

    // Snapshots for drawing
    private int lastRound;
    private boolean[] lastHistory;
    private boolean training = false;

    public NeuralStrategy(int inputNodes, int hiddenNodes) {
        this.inputNodes = inputNodes;
        this.hiddenNodes = hiddenNodes;
        wAndB = new float[(inputNodes + 1) * (hiddenNodes) + hiddenNodes + 1];
        lastHistory = new boolean[inputNodes - 1];
    }

    public void randomize() {
        for (int i = 0; i < wAndB.length; i++) {
            wAndB[i] = (float) (Math.random() * 10F - 5F);
        }
    }

    public void mutate(float range) {
        // randomly select one
        int mutationIndex = new Random().nextInt(wAndB.length);
        // adjust by up to 5 in either direction
        wAndB[mutationIndex] += (float) ((Math.random() - 0.5) * range);
    }

    public NeuralStrategy clone() {
        NeuralStrategy result = new NeuralStrategy(inputNodes, hiddenNodes);
        System.arraycopy(this.wAndB, 0, result.wAndB, 0, this.wAndB.length);
        return result;
    }

    @Override
    public boolean test(Integer round, boolean[] history) {
        synchronized (this) {
            float[] hidden = new float[hiddenNodes];
            for (int row = 0; row < hiddenNodes; row++) {
                for (int col = 0; col < inputNodes; col++) {
                    float input = col == 0 ? round / 500f : history[col - 1] ? 1.0F : -1.0F;
                    hidden[row] += input * wAndB[row * (inputNodes + 1) + col];
                }
                // add the bias
                hidden[row] += wAndB[inputNodes];
                // apply RELU
                if (hidden[row] < 0) {
                    hidden[row] = 0;
                }
            }
            // Now determine the output
            float output = 0;
            for (int col = 0; col < hiddenNodes; col++) {
                output += hidden[col] * wAndB[(inputNodes + 1) * hiddenNodes + col];
            }
            // Add the bias
            output += wAndB[(inputNodes + 1) * hiddenNodes + hiddenNodes];

            if (getParent() != null) {
                System.arraycopy(history, 0, lastHistory, 0, history.length);
                lastRound = round;
                if (training) {
                    repaint();
                }
            }
            return output >= 0;
        }
    }

    public void setTraining(boolean training) {
        this.training = training;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        paintTraining((Graphics2D) g);
    }

    public void paintTraining(Graphics2D g2) {
        int left = getWidth() / 20;
        int right = getWidth() * 19 / 20;
        int center = getWidth() / 2;
        int top = getHeight() / 20;
        int bottom = getHeight() * 19 / 20;
        int mid = (top + bottom) / 2;
        g2.setStroke(new BasicStroke(3));

        float inputScale = 0;
        // iterate the input nodes, but also include the bias in this scaling calculation
        for (int inputIndex = 0; inputIndex < inputNodes + 1; inputIndex++) {
            for (int hiddenIndex = 0; hiddenIndex < hiddenNodes; hiddenIndex++) {
                inputScale = Math.max(inputScale, Math.abs(wAndB[hiddenIndex * (inputNodes + 1) + inputIndex] * wAndB[(inputNodes + 1) * hiddenNodes + hiddenIndex]));
            }
        }
        // now draw the lines
        for (int inputIndex = 0; inputIndex < inputNodes; inputIndex++) {
            for (int hiddenIndex = 0; hiddenIndex < hiddenNodes; hiddenIndex++) {
                float normalizedWeight = (wAndB[hiddenIndex * (inputNodes + 1) + inputIndex] * Math.abs(wAndB[(inputNodes + 1) * hiddenNodes + hiddenIndex])) / inputScale;
                g2.setColor(getColor(normalizedWeight));
                g2.drawLine(left, nodeY(inputIndex, inputNodes, top, bottom), center, nodeY(hiddenIndex, hiddenNodes, top, bottom));
            }
        }
        // scale the output weights
        float hiddenScale = 0;
        for (int hiddenIndex = 0; hiddenIndex < hiddenNodes; hiddenIndex++) {
            hiddenScale = Math.max(hiddenScale, Math.abs(wAndB[(inputNodes + 1) * hiddenNodes + hiddenIndex]));
        }
        // now draw the lines
        for (int hiddenIndex = 0; hiddenIndex < hiddenNodes; hiddenIndex++) {
            float normalizedWeight = wAndB[(inputNodes + 1) * hiddenNodes + hiddenIndex] / hiddenScale;
            g2.setColor(getColor(normalizedWeight));
            g2.drawLine(center, nodeY(hiddenIndex, hiddenNodes, top, bottom), right, mid);
        }

        // draw the nodes
        for (int inputIndex = 0; inputIndex < inputNodes + 1; inputIndex++) {
            if (inputIndex == 0) {
                drawRound(g2, left, nodeY(inputIndex, inputNodes, top, bottom), 0f);
            } else {
                drawNode(g2, left, nodeY(inputIndex, inputNodes, top, bottom), 0f);
            }
        }
        for (int hiddenIndex = 0; hiddenIndex < hiddenNodes; hiddenIndex++) {
            // color based on bias
            drawNode(g2, center, nodeY(hiddenIndex, hiddenNodes, top, bottom), wAndB[hiddenIndex * (inputNodes + 1) + inputNodes] * Math.abs(wAndB[(inputNodes + 1) * hiddenNodes + hiddenIndex]) / inputScale);
        }
        drawNode(g2, right, mid, wAndB[(inputNodes + 1) * hiddenNodes + hiddenNodes]);
    }

    private Color getColor(float activation) {
        return activation < 0 ? new Color(Math.min(1f, -activation), 0.2f, 0.2f) : new Color(0.2f, Math.min(1f, activation), 0.2f);
    }

    public void paintOperation(Graphics2D g2) {
        synchronized (this) {
            // three layers; allow 5% on the outsides, so 5%, 50%, 95% horizontal
            // similarly allow 5% marging top and bottom
            int left = getWidth() / 20;
            int right = getWidth() * 19 / 20;
            int center = getWidth() / 2;
            int top = getHeight() / 20;
            int bottom = getHeight() * 19 / 20;

            // first draw the lines with their weights
            // normalize the outputs by their relative contributions - scale to the greater of positive and negative contributions
            float[] hidden = new float[hiddenNodes];
            for (int row = 0; row < hiddenNodes; row++) {
                for (int col = 0; col < inputNodes; col++) {
                    float input = row == 0 ? lastRound / 500f : lastHistory[row - 1] ? 1.0F : -1.0F;
                    hidden[row] += input * wAndB[row * (inputNodes + 1) + col];
                }
                // add the bias
                hidden[row] += wAndB[inputNodes];
            }

            // Now determine the output
            float output = 0;
            float negSum = 0;
            float posSum = 0;
            for (int col = 0; col < hiddenNodes; col++) {
                float contribution = hidden[col] * wAndB[(inputNodes + 1) * hiddenNodes + col];
                if (contribution < 0) {
                    negSum += contribution;
                } else {
                    posSum += contribution;
                }
            }
            float outScale = Math.max(-negSum, posSum);
            g2.setStroke(new BasicStroke(3));
            for (int col = 0; col < hiddenNodes; col++) {
                float contribution = hidden[col] * wAndB[(inputNodes + 1) * hiddenNodes + col];
                g2.setColor(getColor(contribution / outScale));
                g2.drawLine(center, nodeY(col, hiddenNodes, top, bottom), right, (bottom + top) / 2);
            }

            output += negSum + posSum;
            // Add the bias
            output += wAndB[(inputNodes + 1) * hiddenNodes + hiddenNodes];

            for (int row = 0; row < hiddenNodes; row++) {
                float nodeScale = 0;
                for (int col = 0; col < inputNodes; col++) {
                    float input = col == 0 ? lastRound / 500f : lastHistory[col - 1] ? 1.0F : -1.0F;
                    float contribution = input * wAndB[row * (inputNodes + 1) + col];
                    nodeScale = Math.max(Math.abs(contribution), nodeScale);
                }
                for (int col = 0; col < inputNodes; col++) {
                    float input = col == 0 ? lastRound / 500f : lastHistory[col - 1] ? 1.0F : -1.0F;
                    float contribution = input * wAndB[row * (inputNodes + 1) + col];
                    // draw the lines for the first layers
                    g2.setColor(getColor(contribution / nodeScale));
                    g2.drawLine(left, nodeY(col, inputNodes, top, bottom), center, nodeY(row, hiddenNodes, top, bottom));
                }
                // apply RELU
                if (hidden[row] < 0) {
                    hidden[row] = 0;
                }
            }

            // draw the nodes
            for (int index = 0; index < inputNodes; index++) {
                float activation = index == 0 ? lastRound / 500f : (lastHistory[index - 1] ? 1f : -1f);
                drawNode(g2, left, nodeY(index, inputNodes, top, bottom), activation);
            }
            for (int index = 0; index < hiddenNodes; index++) {
                float activation = hidden[index] / 10f;
                drawNode(g2, center, nodeY(index, hiddenNodes, top, bottom), activation);
            }
            drawNode(g2, right, (top + bottom) / 2, Math.signum(output));
        }
    }

    private void drawNode(Graphics2D g2, int x, int y, float activation) {
        g2.setColor(getColor(activation));
        int diameter = Math.min(getWidth(), getHeight()) / 25;
        g2.fillOval(x - diameter / 2, y - diameter / 2, diameter, diameter);
    }

    private void drawRound(Graphics2D g2, int x, int y, float activation) {
        g2.setColor(getColor(activation));
        int diameter = Math.min(getWidth(), getHeight()) / 25;
        g2.fillRect(x - diameter / 2, y - diameter / 2, diameter, diameter);
    }

    private int nodeY(int index, int count, int minY, int maxY) {
        return minY + (maxY - minY) * index / (count - 1);
    }

    @Override
    public Dimension getPreferredSize() {
        Container parent = getParent();
        if (parent != null) {
            return parent.getSize();
        } else {
            return new Dimension(400, 300); // Default size if no parent
        }
    }

    @Override
    public String toString() {
        return "NeuralStrategy{" +
               "inputNodes=" + inputNodes +
               ", hiddenNodes=" + hiddenNodes +
               ", wAndB =" + Arrays.toString(wAndB) +
               '}';
    }

    public void writeCSV(Writer writer) throws IOException {
        boolean first = true;
        for (int i=0; i<wAndB.length; i++) {
            if (!first) {
                writer.write(',');
            } else {
                first = false;
            }
            writer.write(Float.toString(wAndB[i]));
        }
        writer.write('\n');
    }
}
