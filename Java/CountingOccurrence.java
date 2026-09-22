// n = 1385757879
// How many times number 7 is occuring in this n so ans is 3.

import java.util.Scanner;

public class CountingOccurrence {
    public static void main(String[] args){
        Scanner input = new Scanner(System.in);
        System.out.print("Enter a number : ");
        long num = input.nextLong();
        System.out.print("Enter a occuring number : ");
        int occNum = input.nextInt();

        long temp = Math.abs(num);
        int count = 0;

        if (temp == 0 && occNum == 0) {
            count = 1;
        }else{
            while(temp != 0){
            int digit = (int) (temp % 10);
            if(digit == occNum){
                count++;
            }
            temp = temp/10;
        }
        }

        
        System.out.print("Occurence is : " + count + " times");
    }
}