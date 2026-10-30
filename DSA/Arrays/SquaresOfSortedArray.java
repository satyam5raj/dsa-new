package DSA.Arrays;

import java.util.Arrays;

public class SquaresOfSortedArray {

    public static void main(String[] args) {
        int[] nums = {-4, -1, 0, 3, 10};
        int[] result1 = squaresOfSortedArray(nums);
        int[] result2 = sortedSquares(nums);
        System.out.println("Result1 : " + Arrays.toString(result1));
        System.out.println("Result2 : " + Arrays.toString(result2));
    }

    public static int[] squaresOfSortedArray(int[] nums){
        int[] result = new int[nums.length];
        for(int i=0; i<nums.length; i++){
            result[i] = nums[i] * nums[i];
        }
        Arrays.sort(result);
        return result;
    }

    public static int[] sortedSquares(int[] nums) {
        int n = nums.length;
        int left = 0, right = n - 1;
        int[] result = new int[n];
        for (int i = n-1; i >= 0; i--) {
            int ls = nums[left] * nums[left];
            int rs = nums[right] * nums[right];
            if(ls>rs){
                result[i] = ls;
                left++;
            }
            else{
                result[i] = rs;
                right--;
            }
        }
        return result;
    }

}
