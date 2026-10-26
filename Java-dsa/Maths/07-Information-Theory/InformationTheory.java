/*
 * ============================================================================
 *  MATHS 07 : INFORMATION THEORY  (what loss functions measure)
 * ============================================================================
 *  Run:  java InformationTheory.java          (explanations: README.md in this folder)
 *
 *    1. information      surprise = -log2 p
 *    2. entropy          average surprise; fair vs biased coins; English text
 *    3. compression      Huffman codes sit between H and H + 1; zip can't beat entropy
 *    4. cross-entropy    log loss; why confident wrong answers cost so much
 *    5. KL divergence    >= 0, asymmetric, = cross-entropy - entropy
 *    6. mutual info      how much one variable tells you about another
 *    7. info gain        choosing a decision-tree split
 *    8. perplexity       "how many choices is the model torn between?"
 *    9. temperature      softmax temperature changes the entropy of LLM sampling
 * ============================================================================
 */

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.Deflater;

public class InformationTheory {

    static double log2(double x) { return Math.log(x) / Math.log(2); }
    static String f4(double x) { return String.format("%.4f", x); }

    static double entropy(double[] p) { double h = 0; for (double x : p) if (x > 0) h -= x * log2(x); return h; }
    static double crossEntropy(double[] p, double[] q) { double h = 0; for (int i = 0; i < p.length; i++) if (p[i] > 0) h -= p[i] * log2(q[i]); return h; }
    static double kl(double[] p, double[] q) { double d = 0; for (int i = 0; i < p.length; i++) if (p[i] > 0) d += p[i] * log2(p[i] / q[i]); return d; }

    // =====================================================================
    // 1. information content, 2. entropy
    // =====================================================================

    static void informationAndEntropy() {
        System.out.println("1. Information content  2. Entropy");
        check("surprise of a fair coin flip: -log2(1/2)", "1.0000", f4(-log2(0.5)));
        check("surprise of a die roll: log2 6", "2.5850", f4(log2(6)));
        check("surprise of a 1-in-1024 event", "10.0000", f4(-log2(1 / 1024.0)));
        check("entropy of a fair coin", "1.0000", f4(entropy(new double[]{0.5, 0.5})));
        check("entropy of a 90/10 coin (more predictable)", "0.4690", f4(entropy(new double[]{0.9, 0.1})));
        check("entropy of a coin that always lands heads", "0.0000", f4(entropy(new double[]{1, 0})));
        check("entropy of uniform over 8 outcomes = log2 8", "3.0000", f4(entropy(new double[]{.125, .125, .125, .125, .125, .125, .125, .125})));
        check("uniform has the MAX entropy for 4 outcomes", true, entropy(new double[]{.25, .25, .25, .25}) > entropy(new double[]{.4, .3, .2, .1}));
        String text = "the quick brown fox jumps over the lazy dog and then the dog sleeps while the fox runs away into the forest";
        Map<Character, Integer> counts = new TreeMap<>();
        for (char c : text.toCharArray()) counts.merge(c, 1, Integer::sum);
        double[] p = counts.values().stream().mapToDouble(c -> c / (double) text.length()).toArray();
        check("letter entropy of a sample sentence (bits/char)", "4.1836", f4(entropy(p)));
        check("vs. a fixed 5-bit code for 27 symbols (log2 27)", "4.7549", f4(log2(27)));
    }

    // =====================================================================
    // 3. compression
    // =====================================================================

    static Map<Character, String> huffman(Map<Character, Integer> freq) {
        record Node(int w, Character c, Node l, Node r) {}
        PriorityQueue<Node> pq = new PriorityQueue<>(Comparator.comparingInt(Node::w).thenComparing(n -> n.c == null ? '~' : n.c));
        freq.forEach((c, w) -> pq.add(new Node(w, c, null, null)));
        while (pq.size() > 1) { Node a = pq.poll(), b = pq.poll(); pq.add(new Node(a.w + b.w, null, a, b)); }   // merge the two rarest
        Map<Character, String> codes = new TreeMap<>();
        Deque<Object[]> stack = new ArrayDeque<>();
        stack.push(new Object[]{pq.poll(), ""});
        while (!stack.isEmpty()) {
            Object[] top = stack.pop();
            Node n = (Node) top[0];
            String code = (String) top[1];
            if (n.c != null) { codes.put(n.c, code.isEmpty() ? "0" : code); continue; }
            stack.push(new Object[]{n.l, code + "0"});
            stack.push(new Object[]{n.r, code + "1"});
        }
        return codes;
    }

    static void compression() {
        System.out.println("3. Compression");
        Map<Character, Integer> freq = new TreeMap<>(Map.of('a', 45, 'b', 13, 'c', 12, 'd', 16, 'e', 9, 'f', 5));
        Map<Character, String> codes = huffman(freq);
        double avg = 0, total = 100;
        double[] p = new double[freq.size()];
        int i = 0;
        for (var e : freq.entrySet()) { avg += e.getValue() / total * codes.get(e.getKey()).length(); p[i++] = e.getValue() / total; }
        check("Huffman code lengths a..f", "1 3 3 3 4 4", codes.values().stream().map(c -> String.valueOf(c.length())).reduce((a, b) -> a + " " + b).get());
        check("average Huffman bits per symbol", "2.2400", f4(avg));
        check("entropy of the source (the limit)", "2.2199", f4(entropy(p)));
        check("H <= average length < H + 1", true, entropy(p) <= avg && avg < entropy(p) + 1);
        check("a fixed-length code needs ceil(log2 6) bits", 3, (int) Math.ceil(log2(6)));
        Random r = new Random(3);
        byte[] random = new byte[100_000], repetitive = new byte[100_000];
        r.nextBytes(random);
        byte[] pattern = "ABCABCABDABC".getBytes(StandardCharsets.US_ASCII);
        for (int k = 0; k < repetitive.length; k++) repetitive[k] = pattern[k % pattern.length];
        check("zip of 100 KB random bytes (no redundancy)", true, deflatedSize(random) > 99_000);
        check("zip of 100 KB repetitive text: tiny", true, deflatedSize(repetitive) < 1_000);
        System.out.printf("      deflate sizes: random %,d bytes, repetitive %,d bytes%n", deflatedSize(random), deflatedSize(repetitive));
    }

    static int deflatedSize(byte[] data) {
        Deflater d = new Deflater(Deflater.BEST_COMPRESSION);
        d.setInput(data);
        d.finish();
        byte[] buf = new byte[data.length + 1024];
        int n = d.deflate(buf);
        d.end();
        return n;
    }

    // =====================================================================
    // 4. cross-entropy, 5. KL divergence
    // =====================================================================

    static void crossEntropyAndKl() {
        System.out.println("4. Cross-entropy  5. KL divergence");
        check("log loss, true = 1, predicted 0.9 (nats)", "0.1054", f4(-Math.log(0.9)));
        check("log loss, true = 1, predicted 0.5", "0.6931", f4(-Math.log(0.5)));
        check("log loss, true = 1, predicted 0.1 (confident & wrong)", "2.3026", f4(-Math.log(0.1)));
        check("log loss, true = 1, predicted 0.001", "6.9078", f4(-Math.log(0.001)));
        double[] p = {0.7, 0.2, 0.1}, q = {0.5, 0.3, 0.2};
        check("H(p)", "1.1568", f4(entropy(p)));
        check("H(p, q) >= H(p)", "1.2796", f4(crossEntropy(p, q)));
        check("KL(p || q) = H(p, q) - H(p)", f4(crossEntropy(p, q) - entropy(p)), f4(kl(p, q)));
        check("KL(p || q)", "0.1228", f4(kl(p, q)));
        check("KL(q || p) (NOT the same: asymmetric)", "0.1328", f4(kl(q, p)));
        check("KL(p || p) = 0", "0.0000", f4(kl(p, p)));
        Random r = new Random(5);
        boolean nonNegative = true;
        for (int t = 0; t < 10_000; t++) { double[] a = randomDist(r, 4), b = randomDist(r, 4); nonNegative &= kl(a, b) >= -1e-12; }
        check("KL >= 0 for 10,000 random pairs (Gibbs' inequality)", true, nonNegative);
    }

    static double[] randomDist(Random r, int k) { double[] d = new double[k]; double s = 0; for (int i = 0; i < k; i++) { d[i] = r.nextDouble() + 1e-9; s += d[i]; } for (int i = 0; i < k; i++) d[i] /= s; return d; }

    // =====================================================================
    // 6. mutual information, 7. information gain
    // =====================================================================

    static double mutualInformation(double[][] joint) {
        int n = joint.length, m = joint[0].length;
        double[] px = new double[n], py = new double[m];
        for (int i = 0; i < n; i++) for (int j = 0; j < m; j++) { px[i] += joint[i][j]; py[j] += joint[i][j]; }
        double mi = 0;
        for (int i = 0; i < n; i++) for (int j = 0; j < m; j++) if (joint[i][j] > 0) mi += joint[i][j] * log2(joint[i][j] / (px[i] * py[j]));
        return mi;
    }

    static void mutualInfoAndGain() {
        System.out.println("6. Mutual information  7. Information gain");
        double[][] weatherUmbrella = {{0.05, 0.55}, {0.30, 0.10}};               // rows sunny/rainy, cols umbrella yes/no
        check("I(weather; umbrella)", "0.3613", f4(mutualInformation(weatherUmbrella)));
        double hWeather = entropy(new double[]{0.6, 0.4});
        double hWeatherGivenUmbrella = 0.35 * entropy(new double[]{0.05 / 0.35, 0.30 / 0.35}) + 0.65 * entropy(new double[]{0.55 / 0.65, 0.10 / 0.65});
        check("= H(weather) - H(weather | umbrella)", f4(mutualInformation(weatherUmbrella)), f4(hWeather - hWeatherGivenUmbrella));
        check("independent variables: I = 0", "0.0000", f4(Math.abs(mutualInformation(new double[][]{{0.24, 0.36}, {0.16, 0.24}}))));
        // decision tree: 14 days, 9 "play" / 5 "don't". Split on outlook: sunny (2/3), overcast (4/0), rain (3/2)
        double parent = entropy(new double[]{9 / 14.0, 5 / 14.0});
        double children = 5 / 14.0 * entropy(new double[]{2 / 5.0, 3 / 5.0}) + 4 / 14.0 * entropy(new double[]{1, 0}) + 5 / 14.0 * entropy(new double[]{3 / 5.0, 2 / 5.0});
        check("entropy before splitting (9 yes, 5 no)", "0.9403", f4(parent));
        check("information gain of splitting on 'outlook'", "0.2467", f4(parent - children));
        double windy = 8 / 14.0 * entropy(new double[]{6 / 8.0, 2 / 8.0}) + 6 / 14.0 * entropy(new double[]{3 / 6.0, 3 / 6.0});
        check("information gain of 'windy' (worse split)", "0.0481", f4(parent - windy));
    }

    // =====================================================================
    // 8. perplexity, 9. temperature
    // =====================================================================

    static void perplexityAndTemperature() {
        System.out.println("8. Perplexity  9. Softmax temperature");
        double uniformCE = -log2(1 / 10.0);
        check("model uniform over 10 tokens: perplexity", "10.0000", f4(Math.pow(2, uniformCE)));
        double[] probsOfActualTokens = {0.5, 0.25, 0.8, 0.1, 0.4};                // what a model gave the real next tokens
        double ce = 0;
        for (double q : probsOfActualTokens) ce -= log2(q) / probsOfActualTokens.length;
        check("cross-entropy of a 5-token sentence (bits/token)", "1.5932", f4(ce));
        check("perplexity = 2^cross-entropy", "3.0171", f4(Math.pow(2, ce)));
        check("= geometric mean of 1/q", "3.0171", f4(Math.pow(1 / (0.5 * 0.25 * 0.8 * 0.1 * 0.4), 1 / 5.0)));
        double[] logits = {3, 2, 1, 0};
        StringBuilder s = new StringBuilder();
        for (double temp : new double[]{0.25, 1, 4}) s.append(String.format("T=%.2f: H=%.3f top=%.3f | ", temp, entropy(softmax(logits, temp)), softmax(logits, temp)[0]));
        check("lower T -> sharper (low entropy), higher T -> flatter", "T=0.25: H=0.134 top=0.982 | T=1.00: H=1.367 top=0.644 | T=4.00: H=1.945 top=0.350 | ", s.toString());
    }

    static double[] softmax(double[] z, double temp) {
        double max = Arrays.stream(z).max().getAsDouble(), sum = 0;
        double[] e = new double[z.length];
        for (int i = 0; i < z.length; i++) { e[i] = Math.exp((z[i] - max) / temp); sum += e[i]; }
        for (int i = 0; i < z.length; i++) e[i] /= sum;
        return e;
    }

    public static void main(String[] args) {
        informationAndEntropy();
        compression();
        crossEntropyAndKl();
        mutualInfoAndGain();
        perplexityAndTemperature();
        summary();
    }

    // ---------------- tiny test helper (same in every file) ----------------
    static int passed = 0, total = 0;

    static void check(String label, Object expected, Object actual) {
        String e = String.valueOf(expected), a = String.valueOf(actual);
        total++;
        if (e.equals(a)) passed++;
        System.out.printf("   %s  %-54s -> %s%s%n", e.equals(a) ? "PASS" : "FAIL", label, a, e.equals(a) ? "" : "   (expected " + e + ")");
    }

    static void summary() {
        System.out.println("   " + passed + "/" + total + " checks passed");
        if (passed != total) System.exit(1);
    }
}
