import java.util.Scanner;

public class PalindromeNumber {
    public static void main(String[] args){
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter a number: ");
        int n = sc.nextInt();

        boolean isPalindrome;

        if(n<0){
            isPalindrome = false;
        }
        else{
            int original = n;
            int reversed = 0;
            int temp = n;

            while(temp != 0){
                int lastDigit = temp % 10;
                reversed = reversed * 10 + lastDigit;
                temp = temp / 10;
            }
            isPalindrome = (original == reversed);
        }
        System.out.println("Is Palindrome: " + isPalindrome);
    }
}