import java.util.Scanner;

public class CountPrimes {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter n: ");
        int n = sc.nextInt();

        int count = 0;

        for (int i = 2; i <= n; i++) {
            if (isPrime(i)) {
                count = count + 1;
            }
        }

        System.out.println("Count of primes: " + count);
    }

    static boolean isPrime(int num) {
        if (num <= 1) {
            return false;
        }
        int c = 2;
        while (c * c <= num) {
            if (num % c == 0) {
                return false;
            }
            c++;
        }
        return true;
    }
}
