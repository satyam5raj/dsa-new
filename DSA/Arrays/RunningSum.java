package DSA.Arrays;

import java.util.Arrays;

public class RunningSum {
    public static void main(String[] args) {
        int[] nums = { 1, 2, 3, 4 };
        int[] result_stored = runningSumOptimal(nums);
        System.out.println("Result : " + Arrays.toString(result_stored));
    }

    public static int[] runningSumBrute(int[] nums) {
        int n = nums.length;
        int[] result = new int[n];
        for (int i = 0; i < n; i++) {
            int sum = 0;
            for (int j = 0; j <= i; j++) {
                sum = sum + nums[j];
            }
            result[i] = sum;
        }
        return result;
    }

    public static int[] runningSumOptimal(int[] nums) {
        int[] result = new int[nums.length];
        int sum = 0;
        for (int i = 0; i < nums.length; i++) {
            sum += nums[i];
            result[i] = sum;
        }
        return result;
    }

}
