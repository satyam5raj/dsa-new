import java.util.Scanner;

public class RotateDigits{
    public static void main(String[] args){
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter a number : ");
        int n = sc.nextInt();

        int num = Math.abs(n);

        if(num ==0){
            System.out.println("Rotate Left : 0");
            System.out.println("Rotate Right : 0");
        }else {
            int digitCount = 0;
            int temp = num;
            while(temp != 0){
                temp = temp / 10;
                digitCount = digitCount+1;
            }

            int power = (int) Math.pow(10, digitCount-1);

            int firstDigit = num / power;
            int rotateLeft = (num - firstDigit * power) * 10 + firstDigit;

            int lastDigit = num % 10;
            int rotateRight = lastDigit * power + num / 10;

            System.out.println("Rotate Left : " + rotateLeft);
            System.out.println("Rotate Right : " + rotateRight);
        }
    }
}