import java.util.Scanner;

public class ArmstrongNumber {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter a number: ");
        int n = sc.nextInt();

        int original = n;

        int digitCount = 0;
        int temp = n;
        while (temp != 0) {
            temp = temp / 10;
            digitCount = digitCount + 1;
        }

        int sum = 0;
        temp = n;
        while (temp != 0) {
            int lastDigit = temp % 10;
            sum = sum + (int) Math.pow(lastDigit, digitCount);
            temp = temp / 10;
        }

        boolean isArmstrong = (sum == original);
        System.out.println("Is Armstrong: " + isArmstrong);
    }
}
