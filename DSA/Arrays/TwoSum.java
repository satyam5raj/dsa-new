package DSA.Arrays;
import java.util.Arrays;
import java.util.Scanner;
import java.util.Map;
import java.util.HashMap;

// public class TwoSum {
//     public static void main(String[] args) {
//         int[] nums = {2, 7, 11, 15};
//         int target = 9;

//         int[] result = twoSum(nums, target);
//         System.out.println("Arrays: " + Arrays.toString(nums));
//         System.out.println("Target: " + target);
//         System.out.println("Indices: " + Arrays.toString(result));

//         if (result.length == 2) {
//             System.out.println(
//                 "Numbers: " + nums[result[0]] + " and " + nums[result[1]]
//             );
//         } else {
//             System.out.println("No valid pair found.");
//         }
//     }

//     public static int[] twoSum(int[] nums, int target){
//         for(int i=0; i<nums.length; i++){
//             for(int j=i+1; j<nums.length; j++){
//                 if(nums[i] + nums[j] == target){
//                     return new int[]{i, j};
//                 }
//             }
//         }
//         return new int[]{};
//     }
// }



public class TwoSum{
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter size of an array: ");
        int n = sc.nextInt();

        int[] nums = new int[n];
        System.out.println("Enter " + n + " elements (space seperated): ");
        for(int i=0; i<n; i++){
            nums[i] = sc.nextInt();
        }

        System.out.println("Enter target: ");
        int target = sc.nextInt();

        sc.close();

        int[] result = twoSum(nums, target);
        System.out.println("Arrays: " + Arrays.toString(nums));
        System.out.println("Target: " + target);
        System.out.println("Indices: " + Arrays.toString(result));

        if (result.length == 2) {
            System.out.println(
                "Numbers: " + nums[result[0]] + " and " + nums[result[1]]
            );
        } else {
            System.out.println("No valid pair found.");
        }
    }

    public static int[] twoSum(int[] nums, int target){
        Map<Integer, Integer> seen = new HashMap<>();

        for(int i=0; i<nums.length; i++){
            int complement = target - nums[i];

            if(seen.containsKey(complement)){
                return new int[]{seen.get(complement), i};
            }
            seen.put(nums[i], i);
        }
        return new int[]{};
    }
}