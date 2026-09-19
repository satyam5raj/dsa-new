import java.util.Scanner;

public class CountDigits{
    public static void main(String[] args){
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter a number: ");
        int n = sc.nextInt();

        int count = 0;

        if(n == 0){
            count = 1;
        }
        else{
            int temp = Math.abs(n);
            while(temp != 0){
                temp = temp / 10;
                count = count + 1;
            }
        }
        System.out.println("Number of digits: " + count);
    }
}