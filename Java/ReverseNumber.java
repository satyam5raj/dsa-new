import java.util.Scanner;

public class ReverseNumber{
    public static void main(String[] args){
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter a number: ");
        int n = sc.nextInt();

        int reversed = 0;

        int temp = Math.abs(n);

        while(temp != 0){
            int lastDigit = temp % 10;
            reversed = reversed * 10 + lastDigit;
            temp = temp/10;
        }
        if(n<0){
            reversed = -reversed;
        }

        System.out.println("Reversed Number: " + reversed);
    }
}