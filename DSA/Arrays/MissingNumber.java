package DSA.Arrays;

import java.util.HashSet;
import java.util.Set;

public class MissingNumber {

    public static void main(String[] args) {
        int[] nums = { 4, 2, 0, 5, 1 };
        int result = missingNumber(nums);
        System.out.println("Result : " + result);
    }

    public static int missingNumber(int[] nums) {
        Set<Integer> seen = new HashSet<>();
        for (int x : nums)
            seen.add(x);
        for (int j = 0; j < nums.length - 1; j++) {
            if (!seen.contains(j)) {
                return j;
            }
        }
        return -1;
    }

    static int missingNumberSum(int[] nums) {
        long n = nums.length;
        long expected = n * (n + 1) / 2;
        long actual = 0;
        for (int x : nums)
            actual += x;
        return (int) (expected - actual);
    }

}
