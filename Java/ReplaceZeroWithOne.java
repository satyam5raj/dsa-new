import java.util.Scanner;

public class ReplaceZeroWithOne {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter a number: ");
        int n = sc.nextInt();

        int result;

        if (n == 0) {
            result = 1;
        } else {
            int temp = Math.abs(n);
            int step1 = 0;

            while (temp != 0) {
                int lastDigit = temp % 10;
                if (lastDigit == 0) {
                    lastDigit = 1;
                }
                step1 = step1 * 10 + lastDigit;
                temp = temp / 10;
            }

            result = 0;
            temp = step1;
            while (temp != 0) {
                int lastDigit = temp % 10;
                result = result * 10 + lastDigit;
                temp = temp / 10;
            }

            if (n < 0) {
                result = -result;
            }
        }

        System.out.println("After replacing 0 with 1: " + result);
    }
}
