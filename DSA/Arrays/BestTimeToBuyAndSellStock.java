package DSA.Arrays;

public class BestTimeToBuyAndSellStock {
    
    public static void main(String[] args) {
        int[] prices = {7, 1, 5, 3, 6, 4};
        int result = buyAndSellStock(prices);
        System.out.println("Result : " + result);
    }

    public static int buyAndSellStock(int[] prices){
        int minPrice = Integer.MAX_VALUE;
        int maxProfit = 0;
        for(int price: prices){
            minPrice = Math.min(price, minPrice);
            maxProfit = Math.max(maxProfit, price - minPrice);
        }
        return maxProfit;
    }
}
