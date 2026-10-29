package DSA.Arrays;

import java.util.LinkedHashSet;
import java.util.Set;

public class RemoveDuplicatesFromSortedArrays {
    
    public static void main(String[] args) {
        int[] nums = {1,1,2,2,3};
        int result = removeDuplicatesSet(nums);
        System.out.println("Result : " + result);
    }

    public static int removeDuplicates(int[] nums){
        int k = 1;
        for(int i=1; i<nums.length; i++){
            if(nums[i] != nums[k-1]){
                nums[k] = nums[i];
                k++;
            }
        }
        return k;
    }

    public static int removeDuplicatesSet(int[] nums){
        Set<Integer> seen = new LinkedHashSet<>();
        for(int num: nums) seen.add(num);
        int k = 0;
        for(int x: seen){
            nums[k] = x;
            k++;
        }
        return k;
    }

}
