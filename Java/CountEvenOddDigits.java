import java.util.Scanner;

public class CountEvenOddDigits{
    public static void main(String[] args){
        Scanner input = new Scanner(System.in);
        System.out.print("Enter a number : ");
        int num = input.nextInt();

        int evenCount = 0;
        int oddCount = 0;

        if(num == 0){
            evenCount = 1;
        }else{
            int temp = Math.abs(num);
            while (temp != 0){
                int lastDigit = temp % 10;
                if(lastDigit % 2 == 0){
                    evenCount += 1;
                }else{
                    oddCount += 1;
                }
                temp = temp / 10;
            }
        }
        System.out.println("Even digits : " + evenCount);
        System.out.println("Odd digits : " + oddCount);
    }
}