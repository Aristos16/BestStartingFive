/*
 * HungarianAlgorithm solves the assignment problem using the Hungarian algorithm.
 *
 * In this project, the matrix has:
 * - rows    -> basketball positions
 * - columns -> players
 *
 * The algorithm returns the best one-to-one assignment that minimizes total cost.
 * Since the rest of the project works with scores that should be maximized,
 * those scores are converted into costs before this class is used.
 */
import java.util.Arrays;

public class HungarianAlgorithm {

    // Cost matrix: rows are tasks/positions, columns are candidates/players
    private final double[][] cost;
    private final int rows;
    private final int cols;

    public HungarianAlgorithm(double[][] costMatrix) {
        this.cost = costMatrix;
        this.rows = costMatrix.length;
        this.cols = costMatrix[0].length;
    }

    /**
     * Solves the assignment problem and returns the final matching.
     *
     * @return an array where assignment[row] = col, meaning that the given row
     *         is assigned to the given column.
     *         If a row cannot be matched to a real column, its value is -1.
     */
    public int[] execute() {

        if (rows <= cols) {
            return hungarianRowsLeqCols(cost, rows, cols);
        } else {
            double[][] transposed = transpose(cost, rows, cols);

            // In the transposed version, the returned array maps:
            // transposedRow -> transposedCol
            // which corresponds to:
            // originalCol -> originalRow
            int[] colToRow = hungarianRowsLeqCols(transposed, cols, rows);

            // Convert back to the original format:
            // originalRow -> originalCol
            int[] rowToCol = new int[rows];
            Arrays.fill(rowToCol, -1);

            for (int c = 0; c < cols; c++) {
                int r = colToRow[c];
                if (r >= 0 && r < rows) {
                    rowToCol[r] = c;
                }
            }

            return rowToCol;
        }
    }

    /**
     * This implementation uses the classic potential-based / augmenting-path
     * version of the Hungarian algorithm.
     */
    private static int[] hungarianRowsLeqCols(double[][] a, int nRows, int nCols) {
        final int N = nRows;
        final int M = Math.max(nCols, nRows);

        // Create a padded matrix if needed.
        // Extra columns represent dummy assignments with very large cost.
        double[][] b = new double[N][M];

        //One position at the time
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < M; j++) {
                if (j < nCols) {
                    b[i][j] = a[i][j];
                } else {
                    b[i][j] = 1e12; //not enough players
                }
            }
        }

        // 1-indexed helper arrays used by the algorithm:
        // u, v   -> dual variables / potentials
        // p      -> current matching, p[j] = row matched to column j
        // way    -> previous column in the augmenting path
        double[] u = new double[N + 1];
        double[] v = new double[M + 1];


        int[] p = new int[M + 1];
        int[] way = new int[M + 1];

        for (int i = 1; i <= N; i++) {
            p[0] = i;
            int j0 = 0;

            double[] minv = new double[M + 1];
            boolean[] used = new boolean[M + 1];
            Arrays.fill(minv, Double.POSITIVE_INFINITY);
            Arrays.fill(used, false);

            do {
                used[j0] = true;
                int i0 = p[j0];
                double delta = Double.POSITIVE_INFINITY;
                int j1 = 0;

                for (int j = 1; j <= M; j++) {
                    if (used[j]) {
                        continue;
                    }

                    // Reduced cost for edge (i0, j)
                    double cur = b[i0 - 1][j - 1] - u[i0] - v[j];

                    if (cur < minv[j]) {
                        minv[j] = cur;
                        way[j] = j0;
                    }

                    if (minv[j] < delta) {
                        delta = minv[j];
                        j1 = j;
                    }
                }

                // Update potentials so that the search can continue
                for (int j = 0; j <= M; j++) {
                    if (used[j]) {
                        u[p[j]] += delta;
                        v[j] -= delta;
                    } else {
                        minv[j] -= delta;
                    }
                }

                j0 = j1;
            } while (p[j0] != 0);

            // Reconstruct and apply the augmenting path
            do {
                int j1 = way[j0];
                p[j0] = p[j1];
                j0 = j1;
            } while (j0 != 0);
        }

        // Build the final answer in the format assignment[row] = col
        int[] assignment = new int[nRows];
        Arrays.fill(assignment, -1);

        for (int j = 1; j <= M; j++) {
            int i = p[j];
            if (i >= 1 && i <= nRows) {
                int col = j - 1;

                // If the chosen column is a fill padded column,
                // treat that row as unmatched.
                assignment[i - 1] = (col < nCols) ? col : -1;
            }
        }

        return assignment;
    }

    /**
     * Returns the transpose of a matrix.
     */
    private static double[][] transpose(double[][] x, int r, int c) {
        double[][] t = new double[c][r];
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < c; j++) {
                t[j][i] = x[i][j];
            }
        }
        return t;
    }
}