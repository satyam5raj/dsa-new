import java.util.Scanner;

public class ProductOfDigits {
    public static void main(String[] args){
        Scanner input = new Scanner(System.in);
        System.out.print("Enter a number : ");
        int n = input.nextInt();

        int product = 1;
        int temp = Math.abs(n);

        while(temp != 0) {
            int lastDigit = temp % 10;
            product = product * lastDigit;
            temp = temp / 10;
        }
        System.out.println("Product of digits : " + product);
    }
}


