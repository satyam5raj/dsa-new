import java.util.Scanner;

public class RemoveLastDigit {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter a number: ");
        int n = sc.nextInt();

        int result = n / 10;

        System.out.println("After removing last digit: " + result);
    }
}
