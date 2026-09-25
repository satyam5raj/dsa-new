package DSA;
import java.util.Scanner;

public class ArrayInput {
    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);
        System.out.println("Enter array size : ");
        int n = input.nextInt();
        int[] nums = new int[n];
        for (int i = 0; i < nums.length; i++) {
            System.out.print("Enter element " + i + ": ");
            nums[i] = input.nextInt();
        }

        System.out.println("Arrays :");
        for(int num : nums){
            System.out.println(num);
        }
        input.close();
    }
}
