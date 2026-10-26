/*
 * ============================================================================
 *  MATHS 02 : LINEAR ALGEBRA  (a tiny matrix library written from scratch)
 * ============================================================================
 *  Run:  java LinearAlgebra.java          (explanations: README.md in this folder)
 *
 *    1. vectors          add, scale, dot, norms, angle, cosine similarity, projection
 *    2. matrices         multiply (not commutative!), transpose, identity, as transformations
 *    3. linear systems   Gaussian elimination, rank, singular systems
 *    4. determinant      area scaling, det = 0 means "squashes space"
 *    5. inverse          A * A^-1 = I
 *    6. eigen            Av = lambda v, power iteration, trace/det facts
 *    7. orthogonality    Gram-Schmidt, Q^T Q = I
 *    8. SVD              singular values, best low-rank approximation
 *    9. definiteness     x^T A x > 0
 *   10. least squares    fitting a line = solving A^T A w = A^T y
 *   11. PCA              the direction of most variance = top eigenvector of the covariance
 *   12. tensors          shapes and the cost of matrix multiplication
 * ============================================================================
 */

import java.util.*;

public class LinearAlgebra {

    // =====================================================================
    // the "library"
    // =====================================================================

    static double dot(double[] a, double[] b) { double s = 0; for (int i = 0; i < a.length; i++) s += a[i] * b[i]; return s; }
    static double norm(double[] a) { return Math.sqrt(dot(a, a)); }
    static double[] add(double[] a, double[] b) { double[] r = a.clone(); for (int i = 0; i < a.length; i++) r[i] += b[i]; return r; }
    static double[] scale(double c, double[] a) { double[] r = a.clone(); for (int i = 0; i < a.length; i++) r[i] *= c; return r; }
    static double[] sub(double[] a, double[] b) { return add(a, scale(-1, b)); }

    static double[][] mul(double[][] a, double[][] b) {
        int n = a.length, m = b[0].length, k = b.length;
        double[][] c = new double[n][m];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < m; j++)
                for (int t = 0; t < k; t++) c[i][j] += a[i][t] * b[t][j];       // row i of A . column j of B
        return c;
    }

    static double[] mulVec(double[][] a, double[] x) { double[] r = new double[a.length]; for (int i = 0; i < a.length; i++) r[i] = dot(a[i], x); return r; }

    static double[][] transpose(double[][] a) {
        double[][] t = new double[a[0].length][a.length];
        for (int i = 0; i < a.length; i++) for (int j = 0; j < a[0].length; j++) t[j][i] = a[i][j];
        return t;
    }

    static double[][] identity(int n) { double[][] I = new double[n][n]; for (int i = 0; i < n; i++) I[i][i] = 1; return I; }
    static double[][] copy(double[][] a) { double[][] c = new double[a.length][]; for (int i = 0; i < a.length; i++) c[i] = a[i].clone(); return c; }

    /** Solve Ax = b by Gaussian elimination with partial pivoting. */
    static double[] solve(double[][] A, double[] b) {
        int n = b.length;
        double[][] M = new double[n][n + 1];                                   // augmented matrix [A | b]
        for (int i = 0; i < n; i++) { System.arraycopy(A[i], 0, M[i], 0, n); M[i][n] = b[i]; }
        for (int col = 0; col < n; col++) {
            int pivot = col;
            for (int r = col + 1; r < n; r++) if (Math.abs(M[r][col]) > Math.abs(M[pivot][col])) pivot = r;
            double[] t = M[col]; M[col] = M[pivot]; M[pivot] = t;              // biggest pivot first: numerically safer
            for (int r = col + 1; r < n; r++) {
                double f = M[r][col] / M[col][col];
                for (int c = col; c <= n; c++) M[r][c] -= f * M[col][c];      // eliminate below the pivot
            }
        }
        double[] x = new double[n];
        for (int i = n - 1; i >= 0; i--) {                                      // back substitution
            double s = M[i][n];
            for (int j = i + 1; j < n; j++) s -= M[i][j] * x[j];
            x[i] = s / M[i][i];
        }
        return x;
    }

    static int rank(double[][] A) {
        double[][] M = copy(A);
        int rows = M.length, cols = M[0].length, r = 0;
        for (int c = 0; c < cols && r < rows; c++) {
            int p = r;
            for (int i = r + 1; i < rows; i++) if (Math.abs(M[i][c]) > Math.abs(M[p][c])) p = i;
            if (Math.abs(M[p][c]) < 1e-10) continue;                           // no pivot in this column
            double[] t = M[r]; M[r] = M[p]; M[p] = t;
            for (int i = r + 1; i < rows; i++) { double f = M[i][c] / M[r][c]; for (int j = c; j < cols; j++) M[i][j] -= f * M[r][j]; }
            r++;
        }
        return r;
    }

    static double det(double[][] A) {
        double[][] M = copy(A);
        int n = M.length;
        double d = 1;
        for (int c = 0; c < n; c++) {
            int p = c;
            for (int i = c + 1; i < n; i++) if (Math.abs(M[i][c]) > Math.abs(M[p][c])) p = i;
            if (Math.abs(M[p][c]) < 1e-12) return 0;
            if (p != c) { double[] t = M[c]; M[c] = M[p]; M[p] = t; d = -d; }   // a row swap flips the sign
            d *= M[c][c];
            for (int i = c + 1; i < n; i++) { double f = M[i][c] / M[c][c]; for (int j = c; j < n; j++) M[i][j] -= f * M[c][j]; }
        }
        return d;                                                               // product of the pivots
    }

    static double[][] inverse(double[][] A) {                                   // solve A x = e_i for each column
        int n = A.length;
        double[][] inv = new double[n][n];
        for (int i = 0; i < n; i++) {
            double[] e = new double[n];
            e[i] = 1;
            double[] col = solve(A, e);
            for (int r = 0; r < n; r++) inv[r][i] = col[r];
        }
        return inv;
    }

    /** All eigenvalues/vectors of a SYMMETRIC matrix (Jacobi rotations). Returns {values[], vectors as columns}. */
    static Object[] eigenSymmetric(double[][] S) {
        int n = S.length;
        double[][] A = copy(S), V = identity(n);
        for (int sweep = 0; sweep < 100; sweep++) {
            double off = 0;
            for (int i = 0; i < n; i++) for (int j = i + 1; j < n; j++) off += A[i][j] * A[i][j];
            if (off < 1e-20) break;
            for (int p = 0; p < n; p++)
                for (int q = p + 1; q < n; q++) {
                    if (Math.abs(A[p][q]) < 1e-15) continue;
                    double theta = (A[q][q] - A[p][p]) / (2 * A[p][q]);
                    double t = Math.signum(theta) / (Math.abs(theta) + Math.sqrt(theta * theta + 1));
                    if (theta == 0) t = 1;
                    double c = 1 / Math.sqrt(t * t + 1), s = t * c;
                    for (int k = 0; k < n; k++) {                               // rotate columns p, q
                        double akp = A[k][p], akq = A[k][q];
                        A[k][p] = c * akp - s * akq; A[k][q] = s * akp + c * akq;
                    }
                    for (int k = 0; k < n; k++) {                               // rotate rows p, q
                        double apk = A[p][k], aqk = A[q][k];
                        A[p][k] = c * apk - s * aqk; A[q][k] = s * apk + c * aqk;
                    }
                    for (int k = 0; k < n; k++) {
                        double vkp = V[k][p], vkq = V[k][q];
                        V[k][p] = c * vkp - s * vkq; V[k][q] = s * vkp + c * vkq;
                    }
                }
        }
        double[] values = new double[n];
        for (int i = 0; i < n; i++) values[i] = A[i][i];
        return new Object[]{values, V};
    }

    static String v(double[] a) { StringBuilder s = new StringBuilder("["); for (int i = 0; i < a.length; i++) s.append(i > 0 ? ", " : "").append(fmt(a[i])); return s.append(']').toString(); }
    static String m(double[][] a) { StringBuilder s = new StringBuilder("["); for (int i = 0; i < a.length; i++) s.append(i > 0 ? ", " : "").append(v(a[i])); return s.append(']').toString(); }
    static String fmt(double x) { double r = Math.round(x * 10000) / 10000.0; if (r == -0.0) r = 0; return r == Math.rint(r) ? String.valueOf((long) r) : String.valueOf(r); }

    // =====================================================================
    // 1. vectors
    // =====================================================================

    static void vectors() {
        System.out.println("1. Vectors");
        double[] a = {3, 4}, b = {1, 2};
        check("a + b", "[4, 6]", v(add(a, b)));
        check("2a", "[6, 8]", v(scale(2, a)));
        check("a . b = 3*1 + 4*2", 11.0, dot(a, b));
        check("L2 norm |a| = sqrt(9+16)", 5.0, norm(a));
        check("L1 norm |3|+|4|", 7.0, Math.abs(a[0]) + Math.abs(a[1]));
        check("unit vector a/|a|", "[0.6, 0.8]", v(scale(1 / norm(a), a)));
        double[] x = {1, 0}, y = {0, 1}, d = {1, 1};
        check("perpendicular: (1,0).(0,1) = 0", 0.0, dot(x, y));
        check("angle between (1,0) and (1,1) in degrees", "45.0", String.format("%.1f", Math.toDegrees(Math.acos(dot(x, d) / (norm(x) * norm(d))))));
        // cosine similarity of toy "word vectors": [royalty, femininity, food]
        double[] king = {0.9, 0.1, 0.0}, queen = {0.9, 0.9, 0.0}, pizza = {0.0, 0.1, 0.9};
        check("cos(king, queen)", "0.78", String.format("%.2f", cos(king, queen)));
        check("cos(king, pizza)", "0.01", String.format("%.2f", cos(king, pizza)));
        double[] proj = scale(dot(a, x) / dot(x, x), x);                       // shadow of a on the x-axis
        check("projection of (3,4) onto (1,0)", "[3, 0]", v(proj));
        check("leftover is perpendicular", 0.0, dot(sub(a, proj), x));
    }

    static double cos(double[] a, double[] b) { return dot(a, b) / (norm(a) * norm(b)); }

    // =====================================================================
    // 2. matrices
    // =====================================================================

    static void matrices() {
        System.out.println("2. Matrices");
        double[][] A = {{1, 2}, {3, 4}}, B = {{0, 1}, {1, 0}};
        check("AB", "[[2, 1], [4, 3]]", m(mul(A, B)));
        check("BA (different: AB != BA)", "[[3, 4], [1, 2]]", m(mul(B, A)));
        check("A I = A", m(A), m(mul(A, identity(2))));
        check("A^T", "[[1, 3], [2, 4]]", m(transpose(A)));
        check("(AB)^T = B^T A^T", m(transpose(mul(A, B))), m(mul(transpose(B), transpose(A))));
        double[][] rot90 = {{0, -1}, {1, 0}}, stretch = {{2, 0}, {0, 3}};
        check("rotate (1,0) by 90 degrees", "[0, 1]", v(mulVec(rot90, new double[]{1, 0})));
        check("stretch (1,1) by 2 and 3", "[2, 3]", v(mulVec(stretch, new double[]{1, 1})));
        check("rotate twice = rotate 180", "[[-1, 0], [0, -1]]", m(mul(rot90, rot90)));
        double[][] X = {{1, 2, 3}, {4, 5, 6}};                                 // 2x3 times 3x1
        check("2x3 matrix times a 3-vector", "[14, 32]", v(mulVec(X, new double[]{1, 2, 3})));
    }

    // =====================================================================
    // 3. linear systems
    // =====================================================================

    static void systems() {
        System.out.println("3. Linear systems");
        double[][] A = {{2, 1, -1}, {-3, -1, 2}, {-2, 1, 2}};
        double[] b = {8, -11, -3};
        double[] x = solve(A, b);
        check("solve 3 equations -> (x, y, z)", "[2, 3, -1]", v(x));
        check("plug back in: A x = b", v(b), v(mulVec(A, x)));
        check("rank of an invertible 3x3", 3, rank(A));
        double[][] dependent = {{1, 2, 3}, {2, 4, 6}, {1, 0, 1}};              // row 2 = 2 * row 1
        check("rank when one row is a multiple of another", 2, rank(dependent));
        check("rank of a 3x5 data matrix is at most 3", 3, rank(new double[][]{{1, 0, 2, 0, 1}, {0, 1, 1, 0, 0}, {1, 1, 0, 1, 2}}));
    }

    // =====================================================================
    // 4. determinant, 5. inverse
    // =====================================================================

    static void determinantAndInverse() {
        System.out.println("4. Determinant  5. Inverse");
        check("det [[3,1],[2,4]] = 3*4 - 1*2", "10", fmt(det(new double[][]{{3, 1}, {2, 4}})));
        check("det of stretch(2,3) = area x6", "6", fmt(det(new double[][]{{2, 0}, {0, 3}})));
        check("det of a rotation = 1 (area kept)", "1", fmt(det(new double[][]{{0, -1}, {1, 0}})));
        check("det = 0 when rows are dependent", "0", fmt(det(new double[][]{{1, 2}, {2, 4}})));
        check("det of the 3x3 system", "-1", fmt(det(new double[][]{{2, 1, -1}, {-3, -1, 2}, {-2, 1, 2}})));
        double[][] A = {{4, 7}, {2, 6}};
        double[][] inv = inverse(A);
        check("inverse of [[4,7],[2,6]] = 1/10 [[6,-7],[-2,4]]", "[[0.6, -0.7], [-0.2, 0.4]]", m(inv));
        check("A * A^-1 = I", "[[1, 0], [0, 1]]", m(mul(A, inv)));
        check("det(AB) = det(A) det(B)", fmt(det(A) * 2), fmt(det(mul(A, new double[][]{{2, 0}, {0, 1}}))));
    }

    // =====================================================================
    // 6. eigenvalues and eigenvectors
    // =====================================================================

    static void eigen() {
        System.out.println("6. Eigenvalues and eigenvectors");
        double[][] A = {{2, 1}, {1, 2}};
        check("A (1,1) = 3 (1,1)  -> eigenvalue 3", "[3, 3]", v(mulVec(A, new double[]{1, 1})));
        check("A (1,-1) = 1 (1,-1) -> eigenvalue 1", "[1, -1]", v(mulVec(A, new double[]{1, -1})));
        check("A (1,0) is NOT a multiple of (1,0)", "[2, 1]", v(mulVec(A, new double[]{1, 0})));
        double[] x = {1, 0};                                                    // power iteration
        double lambda = 0;
        for (int i = 0; i < 50; i++) { double[] y = mulVec(A, x); lambda = norm(y) / norm(x); x = scale(1 / norm(y), y); }
        check("power iteration -> biggest eigenvalue", "3", fmt(lambda));
        check("...and its eigenvector (normalised)", "[0.7071, 0.7071]", v(x));
        double[][] S = {{4, 1, 2}, {1, 3, 0}, {2, 0, 5}};
        double[] vals = (double[]) eigenSymmetric(S)[0];
        double sum = 0, prod = 1;
        for (double e : vals) { sum += e; prod *= e; }
        check("sum of eigenvalues = trace (4+3+5)", "12", fmt(sum));
        check("product of eigenvalues = det", fmt(det(S)), fmt(prod));
        // Fibonacci by matrix power: [[1,1],[1,0]]^n
        double[][] F = {{1, 1}, {1, 0}}, P = identity(2);
        for (int i = 0; i < 30; i++) P = mul(P, F);
        check("[[1,1],[1,0]]^30 contains F(30)", "832040", fmt(P[0][1]));
        double[] fe = (double[]) eigenSymmetric(F)[0];
        check("its biggest eigenvalue is the golden ratio", "1.618", String.format("%.3f", Math.max(fe[0], fe[1])));
    }

    // =====================================================================
    // 7. orthogonality
    // =====================================================================

    static void orthogonality() {
        System.out.println("7. Orthogonality (Gram-Schmidt)");
        double[][] vs = {{1, 1, 0}, {1, 0, 1}, {0, 1, 1}};
        double[][] q = new double[3][];
        for (int i = 0; i < 3; i++) {
            double[] w = vs[i].clone();
            for (int j = 0; j < i; j++) w = sub(w, scale(dot(w, q[j]), q[j]));  // remove the parts along earlier q's
            q[i] = scale(1 / norm(w), w);
        }
        double[][] Q = transpose(q);                                            // q's as columns
        check("Q^T Q = I (orthonormal)", "[[1, 0, 0], [0, 1, 0], [0, 0, 1]]", m(mul(transpose(Q), Q)));
        check("|Q x| = |x| (Q keeps lengths)", "5", fmt(norm(mulVec(Q, new double[]{3, 4, 0}))));
    }

    // =====================================================================
    // 8. SVD
    // =====================================================================

    static void svd() {
        System.out.println("8. SVD (singular value decomposition)");
        double[][] A = {{3, 0}, {4, 5}};
        Object[] eig = eigenSymmetric(mul(transpose(A), A));                   // A^T A = V Sigma^2 V^T
        double[] lam = (double[]) eig[0];
        double s1 = Math.sqrt(Math.max(lam[0], lam[1])), s2 = Math.sqrt(Math.min(lam[0], lam[1]));
        check("singular values of [[3,0],[4,5]] = sqrt(45), sqrt(5)", "6.7082, 2.2361", fmt(s1) + ", " + fmt(s2));
        check("product of singular values = |det|", "15", fmt(s1 * s2));
        // rank-1 approximation of a nearly rank-1 matrix (a "user x movie ratings" table)
        double[][] R = {{5, 4, 1}, {4, 4, 1}, {1, 1, 5}, {2, 1, 4}};
        Object[] e = eigenSymmetric(mul(transpose(R), R));
        double[] vals = (double[]) e[0];
        double[][] V = (double[][]) e[1];
        Integer[] order = {0, 1, 2};
        Arrays.sort(order, (i, j) -> Double.compare(vals[j], vals[i]));
        double[] sig = new double[3];
        for (int i = 0; i < 3; i++) sig[i] = Math.sqrt(Math.max(0, vals[order[i]]));
        double[][] approx = new double[4][3];
        for (int k = 0; k < 2; k++) {                                           // keep the top 2 of 3 directions
            double[] vk = new double[3];
            for (int r = 0; r < 3; r++) vk[r] = V[r][order[k]];
            double[] uk = scale(1 / sig[k], mulVec(R, vk));
            for (int i = 0; i < 4; i++) for (int j = 0; j < 3; j++) approx[i][j] += sig[k] * uk[i] * vk[j];
        }
        double err = 0;
        for (int i = 0; i < 4; i++) for (int j = 0; j < 3; j++) err += Math.pow(R[i][j] - approx[i][j], 2);
        check("singular values of the ratings table", "9.7185, 5.2982, 0.6924", fmt(sig[0]) + ", " + fmt(sig[1]) + ", " + fmt(sig[2]));
        double frob = 0;
        for (double[] row : R) for (double x : row) frob += x * x;
        check("sum of sigma^2 = sum of all entries^2 (123)", "123", fmt(sig[0] * sig[0] + sig[1] * sig[1] + sig[2] * sig[2]));
        check("...the top direction alone keeps", "77%", String.format("%.0f%%", 100 * sig[0] * sig[0] / frob));
        check("rank-2 error = the dropped singular value", fmt(sig[2]), fmt(Math.sqrt(err)));
    }

    // =====================================================================
    // 9. positive definite
    // =====================================================================

    static void definiteness() {
        System.out.println("9. Positive definite matrices");
        double[][] P = {{2, -1}, {-1, 2}}, N = {{1, 2}, {2, 1}};
        Random rnd = new Random(9);
        boolean allPositive = true, someNegative = false;
        for (int t = 0; t < 1000; t++) {
            double[] x = {rnd.nextGaussian(), rnd.nextGaussian()};
            allPositive &= dot(x, mulVec(P, x)) > 0;
            someNegative |= dot(x, mulVec(N, x)) < 0;
        }
        check("x^T P x > 0 for 1,000 random x", true, allPositive);
        check("eigenvalues of P are 1 and 3 (both > 0)", "1, 3", eigStr(P));
        check("N has a negative eigenvalue (-1, 3)", "-1, 3", eigStr(N));
        check("...so x^T N x < 0 for some x", true, someNegative);
    }

    static String eigStr(double[][] S) { double[] e = ((double[]) eigenSymmetric(S)[0]).clone(); Arrays.sort(e); return fmt(e[0]) + ", " + fmt(e[1]); }

    // =====================================================================
    // 10. least squares
    // =====================================================================

    static void leastSquares() {
        System.out.println("10. Least squares (linear regression)");
        Random rnd = new Random(10);
        int n = 200;
        double[][] X = new double[n][2];
        double[] y = new double[n];
        for (int i = 0; i < n; i++) { double x = rnd.nextDouble() * 10; X[i] = new double[]{1, x}; y[i] = 1 + 2 * x + rnd.nextGaussian() * 0.5; }
        double[][] Xt = transpose(X);
        double[] w = solve(mul(Xt, X), mulVec(Xt, y));                          // normal equations
        check("recovered intercept ~ 1", "1.0", String.format("%.1f", w[0]));
        check("recovered slope ~ 2", "2.0", String.format("%.1f", w[1]));
        double[] residual = sub(y, mulVec(X, w));
        check("residual is perpendicular to the columns (X^T r = 0)", "[0, 0]", v(mulVec(Xt, residual)));
    }

    // =====================================================================
    // 11. PCA
    // =====================================================================

    static void pca() {
        System.out.println("11. PCA");
        Random rnd = new Random(11);
        int n = 5000;
        double[][] data = new double[n][2];
        for (int i = 0; i < n; i++) { double t = rnd.nextGaussian() * 3, s = rnd.nextGaussian() * 0.5; data[i] = new double[]{t + s, t - s}; }
        double[] mean = new double[2];
        for (double[] p : data) { mean[0] += p[0] / n; mean[1] += p[1] / n; }
        double[][] cov = new double[2][2];
        for (double[] p : data) for (int i = 0; i < 2; i++) for (int j = 0; j < 2; j++) cov[i][j] += (p[i] - mean[i]) * (p[j] - mean[j]) / (n - 1);
        Object[] e = eigenSymmetric(cov);
        double[] vals = (double[]) e[0];
        double[][] V = (double[][]) e[1];
        int top = vals[0] > vals[1] ? 0 : 1;
        double[] dir = {Math.abs(V[0][top]), Math.abs(V[1][top])};
        check("top principal direction ~ (0.707, 0.707)", "0.7, 0.7", String.format("%.1f, %.1f", dir[0], dir[1]));
        check("...i.e. within 1 degree of 45 degrees", true, Math.abs(Math.toDegrees(Math.atan2(dir[1], dir[0])) - 45) < 1);
        double explained = vals[top] / (vals[0] + vals[1]);
        check("variance explained by 1 component", "97%", String.format("%.0f%%", explained * 100));
    }

    // =====================================================================
    // 12. tensors
    // =====================================================================

    static void tensors() {
        System.out.println("12. Tensors and cost");
        int[] shape = {32, 3, 224, 224};                                        // batch, channels, height, width
        long elements = 1;
        for (int s : shape) elements *= s;
        check("elements in a 32x3x224x224 image batch", 4_816_896L, elements);
        check("MB as float32 (4 bytes each)", "18.4", String.format("%.1f", elements * 4 / 1_048_576.0));
        long n = 1024;
        check("multiply two 1024x1024 matrices: n^3 multiply-adds", 1_073_741_824L, n * n * n);
    }

    public static void main(String[] args) {
        vectors();
        matrices();
        systems();
        determinantAndInverse();
        eigen();
        orthogonality();
        svd();
        definiteness();
        leastSquares();
        pca();
        tensors();
        summary();
    }

    // ---------------- tiny test helper (same in every file) ----------------
    static int passed = 0, total = 0;

    static void check(String label, Object expected, Object actual) {
        String e = String.valueOf(expected), a = String.valueOf(actual);
        total++;
        if (e.equals(a)) passed++;
        System.out.printf("   %s  %-50s -> %s%s%n", e.equals(a) ? "PASS" : "FAIL", label, a, e.equals(a) ? "" : "   (expected " + e + ")");
    }

    static void summary() {
        System.out.println("   " + passed + "/" + total + " checks passed");
        if (passed != total) System.exit(1);
    }
}
