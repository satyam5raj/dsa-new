import java.util.Scanner;

public class PrimeFactors {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter a number: ");
        int n = sc.nextInt();

        int num = n;

        for (int factor = 2; factor * factor <= num; factor++) {
            while (num % factor == 0) {
                System.out.println(factor);
                num = num / factor;
            }
        }

        if (num > 1) {
            System.out.println(num);
        }
    }
}
