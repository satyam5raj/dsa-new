package DSA.Arrays;
import java.util.Arrays;

public class MoveZeros {

    public static void main(String[] args) {
        int[] nums = {0, 1, 0, 3, 12};
        int[] result = moveZeros(nums);
        System.out.println("Result : " + Arrays.toString(result));
    }

    public static int[] moveZeros(int[] nums) {
        int insert = 0;
        for (int i = 0; i < nums.length; i++) {
            if (nums[i] != 0) {
                int temp = nums[insert];
                nums[insert] = nums[i];
                nums[i] = temp;
                insert++;
            }
        }
        return nums;
    }

}
