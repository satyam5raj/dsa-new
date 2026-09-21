import java.util.Scanner;

public class ContainsDigit {
    public static void main(String[] args){
        Scanner input = new Scanner(System.in);
        System.out.println("Enter a number : ");
        int n = input.nextInt();
        System.out.println("Enter digit k to search for : ");
        int k = input.nextInt();

        boolean found = false;
        int temp = Math.abs(n);

        while(temp != 0){
            int lastDigit = temp % 10;
            if(lastDigit == k){
                found = true;
                break;
            }
            temp = temp / 10;
        }
        System.out.println("Contains digit " + k + ": " + found);
    }
}