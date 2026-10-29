package DSA.Arrays;

import java.util.Arrays;

public class MergeSortedArrays {

    public static void main(String[] args) {
        int[] nums1 = { 1, 2, 3, 0, 0, 0 };
        int m = 3;
        int[] nums2 = { 2, 5, 6 };
        int n = 3;
        int[] result = mergeBackToFront(nums1, m, nums2, n);
        System.out.println("Result : " + Arrays.toString(result));
    }

    public static int[] mergeFrontToBack(int[] nums1, int m, int[] nums2, int n) {
        int[] copy = Arrays.copyOf(nums1, m);
        int i = 0, j = 0, w = 0;
        while (i < m && j < n) {
            if (copy[i] <= nums2[j]) {
                nums1[w] = copy[i];
                w++;
                i++;
            } else {
                nums1[w] = nums2[j];
                w++;
                j++;
            }
        }
        while (i < m) {
            nums1[w] = copy[i];
            w++;
            i++;
        }
        while (j < n) {
            nums1[w] = nums2[j];
            w++;
            j++;
        }
        return nums1;
    }

    public static int[] mergeBackToFront(int[] nums1, int m, int[] nums2, int n) {
        int i = m - 1, j = n - 1, w = m + n - 1;
        while (j >= 0) {
            if (i >= 0 && nums1[i] > nums2[j])
                nums1[w--] = nums1[i--];
            else
                nums1[w--] = nums2[j--];
        }
        return nums1;
    }

}
