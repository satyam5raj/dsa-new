/*
 * ============================================================================
 *  AI 03 : EMBEDDINGS AND VECTOR SEARCH
 * ============================================================================
 *  Run:  java VectorSearch.java          (explanations: README.md in this folder)
 *
 *    1. keyword search    BM25 from scratch
 *    2. embeddings        toy "meaning" vectors: synonyms match, keywords don't need to
 *    3. hybrid search     reciprocal rank fusion (RRF) of BM25 + vectors
 *    4. similarity        cosine vs dot product; normalise once
 *    5. ANN index (IVF)   cluster, probe a few clusters: recall vs speed
 *    6. quantisation      int8 vectors: 4x smaller, recall barely drops
 * ============================================================================
 */

import java.util.*;

public class VectorSearch {

    // =====================================================================
    // the documents
    // =====================================================================

    static final String[] DOCS = {
            "car insurance covers accidents and theft of your vehicle",          // 0
            "home insurance protects your house against fire and flood",         // 1
            "error ERR-4521 means the payment gateway timed out",                // 2
            "our dog food is made with real chicken",                            // 3
            "how to reset your account password",                                // 4
            "automobile repair shops near the airport",                          // 5
    };

    static List<String> tokens(String s) { return Arrays.asList(s.toLowerCase().replaceAll("[^a-z0-9\\- ]", "").split("\\s+")); }

    // =====================================================================
    // 1. BM25
    // =====================================================================

    static double[] bm25(String query, String[] docs) {
        double k1 = 1.2, b = 0.75;
        int n = docs.length;
        List<List<String>> d = new ArrayList<>();
        double avgLen = 0;
        for (String doc : docs) { d.add(tokens(doc)); avgLen += tokens(doc).size() / (double) n; }
        double[] scores = new double[n];
        for (String term : new HashSet<>(tokens(query))) {
            int df = 0;
            for (List<String> doc : d) if (doc.contains(term)) df++;
            if (df == 0) continue;
            double idf = Math.log(1 + (n - df + 0.5) / (df + 0.5));            // rare terms count more
            for (int i = 0; i < n; i++) {
                int tf = Collections.frequency(d.get(i), term);
                if (tf == 0) continue;
                double len = d.get(i).size();
                scores[i] += idf * tf * (k1 + 1) / (tf + k1 * (1 - b + b * len / avgLen));   // saturating tf, length-normalised
            }
        }
        return scores;
    }

    static int[] rank(double[] scores) {
        Integer[] idx = new Integer[scores.length];
        for (int i = 0; i < idx.length; i++) idx[i] = i;
        Arrays.sort(idx, (a, c) -> Double.compare(scores[c], scores[a]));
        return Arrays.stream(idx).mapToInt(Integer::intValue).toArray();
    }

    static void keywordSearch() {
        System.out.println("1. Keyword search (BM25)");
        check("'ERR-4521' -> the error doc first", 2, rank(bm25("ERR-4521", DOCS))[0]);
        check("'home insurance' -> the home doc first", 1, rank(bm25("home insurance", DOCS))[0]);
        double[] syn = bm25("automobile insurance", DOCS);
        check("'automobile insurance': BM25 can't know automobile = car", true, rank(syn)[0] != 0);
        System.out.println("      BM25 top for 'automobile insurance' = doc " + rank(syn)[0] + " (doc 0 is the car-insurance doc)");
    }

    // =====================================================================
    // 2. embeddings (toy): each word has a vector over hidden "meaning" axes
    // =====================================================================

    // axes: [vehicle, insurance/finance, home, animal/food, account/tech, error/payment]
    static final Map<String, double[]> WORD = new HashMap<>();
    static {
        put(new double[]{1, 0, 0, 0, 0, 0}, "car", "automobile", "vehicle", "repair", "shops", "airport");
        put(new double[]{0, 1, 0, 0, 0, 0}, "insurance", "covers", "protects", "accidents", "theft");
        put(new double[]{0, 0, 1, 0, 0, 0}, "home", "house", "fire", "flood");
        put(new double[]{0, 0, 0, 1, 0, 0}, "dog", "food", "chicken", "real");
        put(new double[]{0, 0, 0, 0, 1, 0}, "account", "password", "reset", "login");
        put(new double[]{0, 0, 0, 0, 0, 1}, "error", "payment", "gateway", "timed", "out");
    }
    static void put(double[] v, String... words) { for (String w : words) WORD.put(w, v); }

    static double[] embed(String text) {                                          // average of word vectors, then unit length
        double[] e = new double[6];
        for (String t : tokens(text)) { double[] w = WORD.get(t); if (w != null) for (int i = 0; i < 6; i++) e[i] += w[i]; }
        return normalise(e);
    }

    static double[] normalise(double[] v) { double n = Math.sqrt(dot(v, v)); if (n == 0) return v; double[] r = v.clone(); for (int i = 0; i < r.length; i++) r[i] /= n; return r; }
    static double dot(double[] a, double[] b) { double s = 0; for (int i = 0; i < a.length; i++) s += a[i] * b[i]; return s; }

    static double[] vectorScores(String query) {
        double[] q = embed(query), s = new double[DOCS.length];
        for (int i = 0; i < DOCS.length; i++) s[i] = dot(q, embed(DOCS[i]));      // cosine (both unit length)
        return s;
    }

    static void embeddings() {
        System.out.println("2. Embeddings (meaning, not exact words)");
        check("'automobile insurance' -> car-insurance doc first", 0, rank(vectorScores("automobile insurance"))[0]);
        check("cos(car, automobile) = 1 (same meaning axis)", "1.00", String.format("%.2f", dot(embed("car"), embed("automobile"))));
        check("cos(car, dog) = 0 (unrelated)", "0.00", String.format("%.2f", dot(embed("car"), embed("dog"))));
        double[] err = vectorScores("ERR-4521");
        check("but 'ERR-4521' is unknown to the embedding: all scores 0", true, Arrays.stream(err).allMatch(x -> x == 0));
    }

    // =====================================================================
    // 3. hybrid search with reciprocal rank fusion
    // =====================================================================

    static int[] rrf(int[]... rankings) {
        double[] score = new double[DOCS.length];
        for (int[] r : rankings) for (int pos = 0; pos < r.length; pos++) score[r[pos]] += 1.0 / (60 + pos + 1);   // k = 60
        return rank(score);
    }

    static void hybrid() {
        System.out.println("3. Hybrid search (BM25 + vectors, fused with RRF)");
        String q1 = "automobile insurance", q2 = "payment error ERR-4521";
        check("hybrid wins the synonym query", 0, rrf(rank(bm25(q1, DOCS)), rank(vectorScores(q1)))[0]);
        check("hybrid wins the exact-code query", 2, rrf(rank(bm25(q2, DOCS)), rank(vectorScores(q2)))[0]);
    }

    // =====================================================================
    // 4. similarity metrics
    // =====================================================================

    static void similarity() {
        System.out.println("4. Cosine vs dot product");
        double[] a = {1, 1}, longA = {10, 10}, b = {1, 0};
        check("cosine ignores length: cos(a, b) = cos(10a, b)", true, Math.abs(dot(normalise(a), normalise(b)) - dot(normalise(longA), normalise(b))) < 1e-12);
        check("dot product does not: a.b vs 10a.b", "1.0 vs 10.0", dot(a, b) + " vs " + dot(longA, b));
        check("on unit vectors, dot == cosine (so normalise at index time)", true, Math.abs(dot(normalise(a), normalise(b)) - Math.cos(Math.PI / 4)) < 1e-12);
    }

    // =====================================================================
    // 5. an approximate index: IVF (inverted file of clusters)
    // =====================================================================

    static void annIndex() {
        System.out.println("5. Approximate nearest neighbour index (IVF)");
        Random r = new Random(5);
        int n = 20_000, dim = 32, clusters = 100, k = 10;
        double[][] centres = new double[50][dim];
        for (double[] c : centres) for (int i = 0; i < dim; i++) c[i] = r.nextGaussian() * 3;
        double[][] data = new double[n][];
        for (int i = 0; i < n; i++) { double[] c = centres[r.nextInt(50)], v = new double[dim]; for (int j = 0; j < dim; j++) v[j] = c[j] + r.nextGaussian(); data[i] = normalise(v); }
        double[][] cent = kmeans(data, clusters, 8, r);
        List<List<Integer>> lists = new ArrayList<>();
        for (int c = 0; c < clusters; c++) lists.add(new ArrayList<>());
        for (int i = 0; i < n; i++) lists.get(nearest(cent, data[i], 1)[0]).add(i);
        double[] recallAt = new double[4];
        long[] work = new long[4];
        int[] probes = {1, 4, 10, 30};
        int queries = 200;
        for (int qi = 0; qi < queries; qi++) {
            double[] q = data[r.nextInt(n)].clone();
            for (int j = 0; j < dim; j++) q[j] += r.nextGaussian() * 0.05;
            q = normalise(q);
            Set<Integer> truth = new HashSet<>();
            for (int i : topK(data, q, k, null)) truth.add(i);                    // exact: compare against everything
            for (int p = 0; p < probes.length; p++) {
                List<Integer> cand = new ArrayList<>();
                for (int c : nearest(cent, q, probes[p])) cand.addAll(lists.get(c));   // only look inside the closest clusters
                work[p] += cand.size() + clusters;
                int hit = 0;
                for (int i : topK(data, q, k, cand)) if (truth.contains(i)) hit++;
                recallAt[p] += hit / (double) k / queries;
            }
        }
        StringBuilder s = new StringBuilder();
        for (int p = 0; p < probes.length; p++) s.append(String.format("nprobe=%d: recall@10=%.2f, vectors compared=%,d | ", probes[p], recallAt[p], work[p] / queries));
        System.out.println("      brute force compares " + String.format("%,d", n) + " vectors per query");
        System.out.println("      " + s);
        check("more probes -> higher recall", true, recallAt[0] <= recallAt[1] && recallAt[1] <= recallAt[2] && recallAt[2] <= recallAt[3]);
        check("nprobe=10 reaches recall >= 0.9", true, recallAt[2] >= 0.9);
        check("...while comparing < 25% of the vectors", true, work[2] / queries < n / 4);
    }

    static double[][] kmeans(double[][] data, int k, int iters, Random r) {
        double[][] c = new double[k][];
        for (int i = 0; i < k; i++) c[i] = data[r.nextInt(data.length)].clone();
        for (int it = 0; it < iters; it++) {
            double[][] sum = new double[k][data[0].length];
            int[] cnt = new int[k];
            for (double[] v : data) { int a = nearest(c, v, 1)[0]; cnt[a]++; for (int j = 0; j < v.length; j++) sum[a][j] += v[j]; }
            for (int i = 0; i < k; i++) if (cnt[i] > 0) c[i] = normalise(sum[i]);
        }
        return c;
    }

    static int[] nearest(double[][] pts, double[] q, int m) {
        Integer[] idx = new Integer[pts.length];
        double[] s = new double[pts.length];
        for (int i = 0; i < pts.length; i++) { idx[i] = i; s[i] = dot(pts[i], q); }
        Arrays.sort(idx, (a, b) -> Double.compare(s[b], s[a]));
        int[] out = new int[m];
        for (int i = 0; i < m; i++) out[i] = idx[i];
        return out;
    }

    static List<Integer> topK(double[][] data, double[] q, int k, List<Integer> candidates) {
        PriorityQueue<double[]> pq = new PriorityQueue<>(Comparator.comparingDouble(x -> x[1]));   // min-heap of the best k
        Iterable<Integer> it = candidates != null ? candidates : () -> java.util.stream.IntStream.range(0, data.length).iterator();
        for (int i : it) { pq.add(new double[]{i, dot(data[i], q)}); if (pq.size() > k) pq.poll(); }
        List<Integer> out = new ArrayList<>();
        for (double[] x : pq) out.add((int) x[0]);
        return out;
    }

    // =====================================================================
    // 6. scalar quantisation
    // =====================================================================

    static void quantisation() {
        System.out.println("6. Vector quantisation (float32 -> int8)");
        Random r = new Random(6);
        int n = 5000, dim = 64;
        double[][] data = new double[n][];
        byte[][] q8 = new byte[n][dim];
        for (int i = 0; i < n; i++) { double[] v = new double[dim]; for (int j = 0; j < dim; j++) v[j] = r.nextGaussian(); data[i] = normalise(v); for (int j = 0; j < dim; j++) q8[i][j] = (byte) Math.round(data[i][j] * 127 / 0.6); }
        int agree = 0, queries = 100;
        for (int t = 0; t < queries; t++) {
            double[] q = data[r.nextInt(n)];
            int best = -1, bestQ = -1;
            double bs = -9, bq = -9;
            for (int i = 0; i < n; i++) {
                double s = dot(data[i], q);
                double sq = 0;
                for (int j = 0; j < dim; j++) sq += q8[i][j] * q[j];
                if (s > bs) { bs = s; best = i; }
                if (sq > bq) { bq = sq; bestQ = i; }
            }
            if (best == bestQ) agree++;
        }
        check("memory per vector: 64 x 4 bytes vs 64 x 1 byte", "256 vs 64", (dim * 4) + " vs " + dim);
        check("int8 search finds the same top-1 in >= 95% of queries", true, agree >= 95);
        System.out.println("      top-1 agreement: " + agree + "/" + queries);
    }

    public static void main(String[] args) {
        keywordSearch();
        embeddings();
        hybrid();
        similarity();
        annIndex();
        quantisation();
        summary();
    }

    // ---------------- tiny test helper (same in every file) ----------------
    static int passed = 0, total = 0;

    static void check(String label, Object expected, Object actual) {
        String e = String.valueOf(expected), a = String.valueOf(actual);
        total++;
        if (e.equals(a)) passed++;
        System.out.printf("   %s  %-58s -> %s%s%n", e.equals(a) ? "PASS" : "FAIL", label, a, e.equals(a) ? "" : "   (expected " + e + ")");
    }

    static void summary() {
        System.out.println("   " + passed + "/" + total + " checks passed");
        if (passed != total) System.exit(1);
    }
}
