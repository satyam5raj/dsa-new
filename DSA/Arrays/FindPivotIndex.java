package DSA.Arrays;

public class FindPivotIndex {
    
    public static void main(String[] args) {
        int[] nums = {1, 7, 3, 6, 5, 6};
        int result = findPivotindex(nums);
        System.out.println("Index : " + result);
    }

    public static int findPivotindex(int[] nums){
        int total = 0;
        for(int x:nums) total += x;

        int leftSum = 0;
        for(int i=0; i<nums.length; i++){
            int rightSum = total-leftSum-nums[i];
            if(leftSum == rightSum) return i;
            leftSum = leftSum + nums[i];
        }
        return -1;
    }
}
