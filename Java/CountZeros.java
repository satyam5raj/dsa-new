import java.util.Scanner;

public class CountZeros {
    public static void main(String[] args){
        Scanner input = new Scanner(System.in);
        System.out.print("Enter a number : ");
        int n = input.nextInt();

        int count = 0;
        if(n==0){
            count = 1;
        }
        else{
            int temp = Math.abs(n);
            while(temp != 0){
                int lastDigit = temp % 10;
                if(lastDigit == 0){
                    count = count + 1;
                }
                temp = temp / 10;
            }
        }
        System.out.println("Number of zeros: " + count);
    }
}