import java.util.Scanner;

public class SumOfDigits {
    public static void main(String[] args){
        Scanner input = new Scanner(System.in);
        System.out.print("Enter a number : ");
        int n = input.nextInt();

        int sum = 0;
        int temp = Math.abs(n);

        while(temp != 0) {
            int lastDigit = temp % 10;
            sum = sum + lastDigit;
            temp = temp / 10;
        }
        System.out.println("Sum of digits : " + sum);
    }
}


