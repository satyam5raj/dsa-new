import java.util.Scanner;

public class SmallestDigit {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter a number: ");
        int n = sc.nextInt();

        int min;

        if (n == 0) {
            min = 0;
        } else {
            min = 9;
            int temp = Math.abs(n);

            while (temp != 0) {
                int lastDigit = temp % 10;
                if (lastDigit < min) {
                    min = lastDigit;
                }
                temp = temp / 10;
            }
        }

        System.out.println("Smallest digit: " + min);
    }
}
