import java.util.Scanner;

public class ExtractFirstDigit {
    public static void main(String[] args){
        Scanner input = new Scanner(System.in);
        System.out.print("Enter a number: ");
        int n = input.nextInt();

        int temp = Math.abs(n);

        while (temp >= 10){
            temp = temp / 10;
        }

        System.out.println("First Digit : " + temp);
    }
}