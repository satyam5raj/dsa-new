import java.util.Scanner;

public class LargestDigit{
    public static void main(String[] args){
        Scanner input = new Scanner(System.in);
        System.out.print("Enter a number : ");
        int num = input.nextInt();

        int max = 0;
        int temp = Math.abs(num);
        while(temp != 0){
            int lastDigit = temp % 10;
            if(lastDigit > max){
                max = lastDigit;
            }
            temp = temp / 10;
        }
        System.out.println("Largest Digit : " + max);
    }
}